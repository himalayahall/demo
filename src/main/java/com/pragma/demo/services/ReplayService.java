package com.pragma.demo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import com.google.common.cache.Cache;
import com.pragma.demo.models.MarketDataEvent;
import com.pragma.demo.services.data.CSVReaderService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


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

    @Value("${app.market.replay.data_file}")
    private String dataFile;

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
        log.trace("create session: {}", sessionId);
        return sessionId;
    }

    /**
     * Start session.
     * 
     * @param sessionId Session id.
     */
    public void start(String sessionId) {
        Optional<ReplaySession> session = Optional.ofNullable(cache.getIfPresent(sessionId));
        session.ifPresentOrElse(s -> {
            if (s.isTerminated()) {
                String msg = String.format("cannot start terminated session: %s", sessionId);
                log.trace(msg);
                throw new ReplayException(msg);
            }
            s.start();
        }, () -> {
            String msg = String.format("session not found: %s", sessionId);
            log.trace(msg);
            throw new ReplayException(msg);
        });
    }

    /**
     * Stop session.
     * 
     * @param sessionId Session id.
     */
    public void stop(String sessionId) {
        Optional<ReplaySession> session = Optional.ofNullable(cache.getIfPresent(sessionId));
        session.ifPresentOrElse(s -> {
            if (s.isTerminated()) {
                String msg = String.format("cannot stop terminated session: %s", sessionId);
                log.trace(msg);
                throw new ReplayException(msg);
            }
            s.stop();
        }, () -> {
            String msg = String.format("session not found: %s", sessionId);
            log.trace(msg);
            throw new ReplayException(msg);
        });
    }

    /**
     * Rewind session.
     * 
     * @param sessionId Session id.
     */
    public void rewind(String sessionId) {
        Optional<ReplaySession> session = Optional.ofNullable(cache.getIfPresent(sessionId));
        session.ifPresentOrElse(s -> {
            if (s.isTerminated()) {
                String msg = String.format("cannot rewind terminated session: %s", sessionId);
                log.trace(msg);
                throw new ReplayException(msg);
            }
            s.rewind();
        }, () -> {
            String msg = String.format("session not found: %s", sessionId);
            log.trace(msg);
            throw new ReplayException(msg);
        });
    }

    /**
     * Jump to session event.
     * 
     * @param sessionId Session id.
     * @param eventId Event id.
     */
    public void jumpToEvent(String sessionId, int eventId) {
        Optional<ReplaySession> session = Optional.ofNullable(cache.getIfPresent(sessionId));
        session.ifPresentOrElse(s -> {
            if (s.isTerminated()) {
                String msg =
                        String.format("cannot jum to event in terminated session: %s", sessionId);
                log.trace(msg);
                throw new ReplayException(msg);
            }
            s.jumpToEvent(eventId);
        }, () -> {
            String msg = String.format("session not found: %s", sessionId);
            log.trace(msg);
            throw new ReplayException(msg);
        });
    }

    public void forward(String sessionId, int skipCount) {
        Optional<ReplaySession> session = Optional.ofNullable(cache.getIfPresent(sessionId));
        session.ifPresentOrElse(s -> {
            if (s.isTerminated()) {
                String msg = String.format("cannot forward terminated session: %s", sessionId);
                log.trace(msg);
                throw new ReplayException(msg);
            }
            s.forward(skipCount);
        }, () -> {
            String msg = String.format("session not found: %s", sessionId);
            log.trace(msg);
            throw new ReplayException(msg);
        });
    }

    /**
     * Set replay speed.
     * 
     * @param sessionId Session id.
     * @param speed Replay speed. Must be Positive (> 0.0).
     */
    public void replaySpeed(String sessionId, double speed) {
        Optional<ReplaySession> session = Optional.ofNullable(cache.getIfPresent(sessionId));
        session.ifPresentOrElse(s -> {
            if (s.isTerminated()) {
                String msg =
                        String.format("cannot set speed for terminated session: %s", sessionId);
                log.trace(msg);
                throw new ReplayException(msg);
            }
            s.replaySpeed(speed);
        }, () -> {
            String msg = String.format("session not found: %s", sessionId);
            log.trace(msg);
            throw new ReplayException(msg);
        });
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
            if (session.get().isTerminated()) {
                String msg = String.format("cannot subscribe to terminated session: %s", sessionId);
                log.trace(msg);
                throw new ReplayException(msg);
            }
            return session.get().subscribe();
        }
        return Flux.empty();
    }

    @PostConstruct
    public void init() {
        log.info("Data file: {}", dataFile);
        try {
            Resource resource = resourceLoader.getResource("classpath:" + dataFile);
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
