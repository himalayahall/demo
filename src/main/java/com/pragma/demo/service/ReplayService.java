package com.pragma.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import com.google.common.cache.Cache;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;


/**
 * MarketDataService - manages replay sessions, starts, stops, rewinds, jumps to events, sets replay
 * speed, and provides event stream for a given session. It uses MarketDataSessionImpl to manage
 * sessions. It reads and caches market data events from a CSV file at initialization - all sessions
 * see the exact same set of events and modifying the CSV file will not affect until the service is
 * restarted.
 * 
 * During replay of market data events, publishTimerMillis if used to to schedule publications -
 * e.g. if this set to 10 then events are published in 10 millisec publication windows. The
 * publishing scheduler sleeps for 10 millisec between publications.
 */
@Slf4j
@Service
public class ReplayService {

    private final CSVReaderService csvReader;
    private List<MarketDataEvent> events;

    @Value("${app.market.replay.publishTimerMillis}")
    private long publishTimerMillis;

    @Value("${app.market.replay.pathToFile}")
    private String pathToFile;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private Cache<String, ReplaySession> cache;

    @Autowired
    public ReplayService(ApplicationContext context,
            @Value("${app.csvReader}") String implementation) throws IOException {
        this.csvReader = context.getBean(implementation, CSVReaderService.class);
    }

    /**
     * Check if session exists.
     * 
     * @param sessionId Session id.
     * @return True if session exists, false otherwise.
     */
    public boolean isSession(String sessionId) {
        return cache.getIfPresent(sessionId) != null;
    }

    /**
     * Create a new session.
     * 
     * @return
     */
    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        cache.put(sessionId, new ReplaySessionImpl(sessionId, events, publishTimerMillis));
        log.info("create session: {}", sessionId);
        return sessionId;
    }

    /**
     * Start session.
     * 
     * @param sessionId Session id.
     */
    public void start(String sessionId) {
        Optional<ReplaySession> session = Optional.ofNullable(cache.getIfPresent(sessionId));
        if (session.isPresent()) {
            session.get().start();
        }
    }

    /**
     * Stop session.
     * 
     * @param sessionId Session id.
     */
    public void stop(String sessionId) {
        Optional<ReplaySession> session = Optional.ofNullable(cache.getIfPresent(sessionId));
        if (session.isPresent()) {
            session.get().stop();
        }
    }

    /**
     * Rewind session.
     * 
     * @param sessionId Session id.
     */
    public void rewind(String sessionId) {
        Optional<ReplaySession> session = Optional.ofNullable(cache.getIfPresent(sessionId));
        if (session.isPresent()) {
            session.get().rewind();
        }
    }

    /**
     * Jump to session event.
     * 
     * @param sessionId Session id.
     * @param eventId Event id.
     */
    public void jumpToEvent(String sessionId, int eventId) {
        Optional<ReplaySession> session = Optional.ofNullable(cache.getIfPresent(sessionId));
        if (session.isPresent()) {
            session.get().jumpToEvent(eventId);
        }
    }

    public void forward(String sessionId, int skipCount) {
        Optional<ReplaySession> session = Optional.ofNullable(cache.getIfPresent(sessionId));
        if (session.isPresent()) {
            session.get().forward(skipCount);
        }
    }

    /**
     * Set replay speed.
     * 
     * @param sessionId Session id.
     * @param speed Replay speed. Must be Positive (> 0.0).
     */
    public void replaySpeed(String sessionId, double speed) {
        Optional<ReplaySession> session = Optional.ofNullable(cache.getIfPresent(sessionId));
        if (session.isPresent()) {
            session.get().replaySpeed(speed);
        }
    }

    /**
     * Subscribe to session event stream.
     * 
     * @param sessionId Session id.
     * @return Session event flux.
     */
    public Flux<MarketDataEvent> subscribe(String sessionId) {
        Optional<ReplaySession> session = Optional.ofNullable(cache.getIfPresent(sessionId));
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
