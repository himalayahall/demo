package com.pragma.demo.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReplaySessionImpl implements ReplaySession {

    private final Date created = new Date(); // session creation timestamp
    private final long replayClockIncMillis; // determines how often events are published
    private long simulationClockMillis; // determines how many events are published
    private final String sessionId;

    // Event stream
    private final List<MarketDataEvent> events;

    // Current index into event stream
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    // Is session in running state. Stopped session does not publish events
    private volatile AtomicBoolean isRunning = new AtomicBoolean(false);


    // Replay speed. Speed: 1.0 => normal speed, 2.0 => double normal speed, 0.5 => half normal
    // speed
    private AtomicReference<Double> replaySpeed = new AtomicReference<>(1.0);

    private final Sinks.Many<MarketDataEvent> eventSink =
            Sinks.many().unicast().onBackpressureBuffer();
    private final Flux<MarketDataEvent> eventFlux = eventSink.asFlux();


    public ReplaySessionImpl(String sessionId, List<MarketDataEvent> events,
            long replayClockIncMillis) {
        this.sessionId = sessionId;
        this.events = events;
        this.replayClockIncMillis = replayClockIncMillis;

        rewind();
    }

    @Override
    public void start() {
        log.info("start session: {}", sessionId);

        isRunning.set(true);
        Flux.interval(Duration.ofMillis(replayClockIncMillis), Schedulers.boundedElastic())
                .takeWhile(tick -> isRunning.get() && currentIndex.get() < events.size())
                .subscribe(tick -> {
                    // Publish all events with timestamp <= simulationClockMillis
                    while (currentIndex.get() < events.size() && events.get(currentIndex.get())
                            .timestamp() <= simulationClockMillis) {
                        MarketDataEvent event = events.get(currentIndex.getAndIncrement());
                        log.info("replay event: {} on session: {}", event, sessionId);
                        Sinks.EmitResult result = eventSink.tryEmitNext(event);
                        if (result.isFailure()) {
                            log.error("Failed to emit event: {}, session: {}, result: {}", event,
                                    sessionId, result);
                            // TODO: Handle the failure (e.g., retry, stop the session, etc.)
                        }
                    }

                    // Advance the simulation clock based on the publishing speed
                    simulationClockMillis += (long) (replaySpeed.get() * replayClockIncMillis);
                });
    }

    @Override
    public void stop() {
        log.info("stop session: {}", sessionId);
        isRunning.set(false);
        eventSink.tryEmitComplete();
    }

    @Override
    public void rewind() {
        log.info("rewind session: {}", sessionId);
        currentIndex.set(0);
        if (!events.isEmpty())
            this.simulationClockMillis = events.get(0).timestamp(); // Reset clock to the first
                                                                    // event's
        // timestamp
        else
            this.simulationClockMillis = 0;
    }

    @Override
    public void jumpToEvent(int eventId) {
        log.info("jump to eventId: {}, session: {}", eventId, sessionId);
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).id() == eventId) {
                currentIndex.set(i);
                simulationClockMillis = events.get(i).timestamp(); // Set clock to the timestamp of
                                                                   // the
                // target event
                return;
            }
        }
        throw new ReplayException(String.format("Invalid event ID:: {}", eventId));
    }

    @Override
    public void replaySpeed(double replaySpeed) {
        log.info("set replay speed: {} on session: {}", replaySpeed, sessionId);
        this.replaySpeed.set(replaySpeed);
    }

    @Override
    public Flux<MarketDataEvent> subscribe() {
        log.info("subscribe session: {}", sessionId);
        return eventFlux;
    }

    @Override
    public String sessionId() {
        return sessionId;
    }

    @Override
    public Date created() {
        return created;
    }

    @Override
    public String toString() {
        return String.format("Session: {}, Created: {}", sessionId, created);
    }
}
