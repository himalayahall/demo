package com.pragma.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * MarketDataService - manages replay sessions, starts, stops, rewinds, jumps to events, sets replay
 * speed, and provides event stream for a given session. It uses MarketDataSessionImpl to manage
 * sessions. It reads and caches market data events from a CSV file at initialization - all sessions
 * see the exact same set of events and modifying the CSV file will not affect until the service is
 * restarted.
 * 
 * During replay of market data events, replayClockIncMillis if used to to schedule publications -
 * e.g. if this set to 10 then events are published in 10 millisec publication windows. The
 * publishing scheduler sleeps for 10 millisec between publications.
 */
@Slf4j
@Service
public class ReplayService {
    private final Map<String, ReplaySessionImpl> sessions = new HashMap<>();
    private final CSVReaderService csvReader;
    private List<MarketDataEvent> events;

    @Value("${app.market.replay.replayClockIncMillis}")
    private long replayClockIncMillis;

    @Value("${app.market.replay.pathToFile}")
    private String pathToFile;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    public ReplayService(ApplicationContext context,
            @Value("${app.csvReader}") String implementation) throws IOException {
        this.csvReader = context.getBean(implementation, CSVReaderService.class);
    }

    public boolean isSession(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, new ReplaySessionImpl(sessionId, events, replayClockIncMillis));
        log.info("create session: {}", sessionId);
        return sessionId;
    }

    public void start(String sessionId) {
        Optional<ReplaySessionImpl> session = Optional.of(sessions.get(sessionId));
        if (session.isPresent()) {
            session.get().start();
        }
    }

    public void stop(String sessionId) {
        Optional<ReplaySessionImpl> session = Optional.of(sessions.get(sessionId));
        if (session.isPresent()) {
            session.get().stop();
        }
    }

    public void rewind(String sessionId) {
        Optional<ReplaySessionImpl> session = Optional.of(sessions.get(sessionId));
        if (session.isPresent()) {
            session.get().rewind();
        }
    }

    public void jumpToEvent(String sessionId, int eventId) {
        Optional<ReplaySessionImpl> session = Optional.of(sessions.get(sessionId));
        if (session.isPresent()) {
            session.get().jumpToEvent(eventId);
        }
    }

    public void replaySpeed(String sessionId, double speed) {
        Optional<ReplaySessionImpl> session = Optional.of(sessions.get(sessionId));
        if (session.isPresent()) {
            session.get().replaySpeed(speed);
        }
    }

    public Flux<MarketDataEvent> subscribe(String sessionId) {
        Optional<ReplaySessionImpl> session = Optional.of(sessions.get(sessionId));
        if (session.isPresent()) {
            return session.get().subscribe();
        }
        return Flux.empty();
    }

    @PostConstruct
    public void init() {
        log.debug("Path to file: {}", pathToFile);
        try {
            Resource resource = resourceLoader.getResource("classpath:" + pathToFile);
            this.events = csvReader.readMarketDataEvents(resource);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss:SSS");
            log.info("Events: {}, First: {}, Last: {}", this.events.size(),
                    sdf.format(new Date(this.events.get(0).timestamp())),
                    sdf.format(new Date(this.events.get(this.events.size() - 1).timestamp())));
        }
        catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
