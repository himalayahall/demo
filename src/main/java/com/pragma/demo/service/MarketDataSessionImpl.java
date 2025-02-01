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
public class MarketDataSessionImpl implements MarketDataSession {

    private final Date created = new Date();    // session creation timestamp
    private final long replayClockIncMillis;    // determines how often events are published
    private long simulationClockMillis;         // determines how many events are published
    private final String sessionId;    
    
    // Event stream
    private final List<MarketDataEvent> events;

    // Current index into event stream
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    // Is session in running state. Stopped session does not publish events
    private volatile AtomicBoolean isRunning = new AtomicBoolean(false);


    // Replay speed. Speed: 1.0 => normal speed, 2.0 => double  normal speed, 0.5 => half normal speed
    private AtomicReference<Double> replaySpeed = new AtomicReference<>(1.0);

    private final Sinks.Many<MarketDataEvent> eventSink =
            Sinks.many().unicast().onBackpressureBuffer();
    private final Flux<MarketDataEvent> eventFlux = eventSink.asFlux();


    public MarketDataSessionImpl(String sessionId, List<MarketDataEvent> events,
            long replayClockIncMillis) {
        this.sessionId = sessionId;
        this.events = events;
        this.replayClockIncMillis = replayClockIncMillis;

        rewind();
    }

    public Date getCreated() {
        return created;
    }

    @Override
    public void start() {
        log.info("start session: {}", sessionId);

        isRunning.set(true);
        Flux.interval(Duration.ofMillis(replayClockIncMillis), Schedulers.boundedElastic())
                .takeWhile(tick -> isRunning.get() && currentIndex.get() < events.size())
                .subscribe(tick -> {
                    // Publish all events with timestamp <= simulationClockMillis
                    while (currentIndex.get() < events.size()
                            && events.get(currentIndex.get()).timestamp() <= simulationClockMillis) {
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

        // Assuming number of clients is small - no need to use virtual threads, normal
        // threads will do fine
        // new Thread(() -> {
        //     while (isRunning.get() && currentIndex.get() < events.size()) {
        //         // Publish all events with timestamp <= simulationClockMillis
        //         while (currentIndex.get() < events.size()
        //                 && events.get(currentIndex.get()).timestamp() <= simulationClockMillis) {
        //             MarketDataEvent event = events.get(currentIndex.getAndIncrement());
        //             log.info("replay event: {} on session: {}", event, sessionId);
        //             eventSink.tryEmitNext(event);
        //         }

        //         // Advance the simulation clock based on the publishing speed
        //         simulationClock += (long) (replaySpeed.get() * replayClockIncMillis);

        //         // Sleep for 1 second to mimic real-time progression
        //         try {
        //             Thread.sleep(replayClockIncMillis);
        //         }
        //         catch (InterruptedException e) {
        //             Thread.currentThread().interrupt();
        //         }
        //     }
        // }).start();
    }

    public void stop() {
        log.info("stop session: {}", sessionId);
        isRunning.set(false);
        eventSink.tryEmitComplete();
    }

    public void rewind() {
        log.info("rewind session: {}", sessionId);
        currentIndex.set(0);
        if (!events.isEmpty())
            this.simulationClockMillis = events.get(0).timestamp(); // Reset clock to the first event's
                                                              // timestamp
        else
            this.simulationClockMillis = 0;
    }

    public void jumpToEvent(int eventId) {
        log.info("jump to eventId: {} in session: {}", eventId, sessionId);
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).id() == eventId) {
                currentIndex.set(i);
                simulationClockMillis = events.get(i).timestamp(); // Set clock to the timestamp of the
                                                             // target event
                return;
            }
        }
        throw new ReplayException(String.format("Invalid event ID:: {}", eventId));
    }

    public void setReplaySpeed(double replaySpeed) {
        log.info("set replay speed: {} on session: {}", replaySpeed, sessionId);
        this.replaySpeed.set(replaySpeed);
    }

    public Flux<MarketDataEvent> subscribe() {
        return eventFlux;
    }

    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String toString() {
        return String.format("Session: {}, Created: {}", sessionId, created);
    }
}
