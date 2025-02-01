package com.pragma.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
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

@Slf4j
@Service
public class MarketDataService {
    private final Map<String, MarketDataSessionImpl> sessions = new HashMap<>();
    private final List<MarketDataEvent> events;

    private final CSVReaderService csvReader;

    @Value("${app.market.replay.replayClockIncMillis}")
    private long replayClockIncMillis;

    @Value("${app.market.replay.pathToFile}")
    private String pathToFile =
            "/Users/jawaidhakim/Jobs/MarketAxess/demo/src/main/resources/marketdata-for-coding-challenge.csv";

    @Autowired
    public MarketDataService(ApplicationContext context,
            @Value("${app.csvReader}") String implementation) throws IOException {
        this.csvReader = context.getBean(implementation, CSVReaderService.class);
        this.events = csvReader.readMarketDataEvents(pathToFile);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss:SSS");
        log.info("Events: {}, First: {}, Last: {}", this.events.size(),
                sdf.format(new Date(this.events.get(0).timestamp())),
                sdf.format(new Date(this.events.get(this.events.size() - 1).timestamp())));
    }

    public boolean isSession(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, new MarketDataSessionImpl(sessionId, events, replayClockIncMillis));
        log.info("create session: {}", sessionId);
        return sessionId;
    }

    public void start(String sessionId) {
        MarketDataSessionImpl session = sessions.get(sessionId);
        if (session != null) {
            session.start();
        }
    }

    public void stop(String sessionId) {
        Optional<MarketDataSessionImpl> session = Optional.of(sessions.get(sessionId));
        if (session.isPresent()) {
            session.get().stop();
        }
    }

    public void rewind(String sessionId) {
        Optional<MarketDataSessionImpl> session = Optional.of(sessions.get(sessionId));
        if (session.isPresent()) {
            session.get().rewind();
        }
    }

    public void jumpToEvent(String sessionId, int eventId) {
        Optional<MarketDataSessionImpl> session = Optional.of(sessions.get(sessionId));
        if (session.isPresent()) {
            session.get().jumpToEvent(eventId);
        }
    }

    public void setReplaySpeed(String sessionId, double speed) {
        Optional<MarketDataSessionImpl> session = Optional.of(sessions.get(sessionId));
        if (session.isPresent()) {
            session.get().setReplaySpeed(speed);
        }
    }

    public Flux<MarketDataEvent> getEventStream(String sessionId) {
        Optional<MarketDataSessionImpl> session = Optional.of(sessions.get(sessionId));
        if (session.isPresent()) {
            return session.get().subscribe();
        }
        return Flux.empty();
    }
}
