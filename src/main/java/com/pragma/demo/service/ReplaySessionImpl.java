package com.pragma.demo.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.time.DurationFormatUtils;
import com.pragma.demo.models.MarketDataEvent;

@Slf4j
public class ReplaySessionImpl implements ReplaySession {

    private final Scheduler SCHEDULER = Schedulers.boundedElastic();

    private final String sessionId;
    private final Date created = new Date(); // session creation timestamp
    private final long publishTimerMillis; // determines how often events are published
    private double replayClockMillis; // double because we need to multiply by replaySpeed and allow
                                      // clock to in sub-millisec increments

    // Event stream
    private final List<MarketDataEvent> events;
    private final Map<Integer, Integer> eventIndexById = new HashMap<>(); // for fast lookup during jumpToEvent()

    // Current index into event stream
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    // Is session in running state. Stopped session does not publish events
    private AtomicBoolean isRunning = new AtomicBoolean(false);

    // I ssession terminated. Terminated session cannot be restarted.
    private AtomicBoolean isTerminated = new AtomicBoolean(false);


    // Replay speed. Speed: 1.0 => normal speed, 2.0 => double normal speed, 0.5 => half normal
    // speed
    private AtomicReference<Double> replaySpeed = new AtomicReference<>(1.0);

    private final Sinks.Many<MarketDataEvent> eventSink;
    private final Flux<MarketDataEvent> eventFlux;

    public ReplaySessionImpl(String sessionId, List<MarketDataEvent> events,
            long publishTimerMillis) {
        this.sessionId = sessionId;

        this.events = events;
        IntStream.range(0, events.size())   // create map of eventId to index
                .forEach(idx -> eventIndexById.put(events.get(idx).id(), idx));

        this.publishTimerMillis = publishTimerMillis;

        this.eventSink = Sinks.many().unicast().onBackpressureBuffer();
        this.eventFlux = eventSink.asFlux().doOnComplete(() -> {
            log.trace("Session: {} - event stream completed", sessionId);
        }).doOnCancel(() -> {
            log.trace("Session: {} - event stream cancelled", sessionId);
        });
        doRewind();
    }

    @Override
    public void start() {
        if (isTerminated.get()) {
            throw new ReplayException("Session is already terminated: " + sessionId);
        }

        log.trace("start session: {}, subscriber count: {}", sessionId,
                eventSink.currentSubscriberCount());
        long startMillis = System.currentTimeMillis();
        isRunning.set(true);
        Flux.interval(Duration.ofMillis(publishTimerMillis), SCHEDULER).takeWhile(
                tick -> isRunning() && !isTerminated() && currentIndex.get() < events.size())
                .subscribe(tick -> {
                    // Publish all events with timestamp <= simulationClockMillis
                    while (currentIndex.get() < events.size()
                            && events.get(currentIndex.get()).timestamp() <= replayClockMillis) {

                        MarketDataEvent event = events.get(currentIndex.getAndIncrement());
                        Sinks.EmitResult result = eventSink.tryEmitNext(event);
                        if (result.isFailure()) {
                            log.error("Failed to emit event: {}, session: {}, result: {}", event,
                                    sessionId, result);
                            // TODO: What to do in case of failure?
                        }
                        else {
                            log.trace("replay event: {} on session: {}", event, sessionId);
                        }
                    }

                    // Advance the simulation clock based on the publishing speed
                    replayClockMillis += (replaySpeed.get() * publishTimerMillis);

                    if (currentIndex.get() >= events.size()) {

                        log.trace("Completing eventSink for session: {}", sessionId);
                        eventSink.tryEmitComplete();

                        log.trace("stop and terminate session: {}", sessionId);
                        isRunning.set(false);
                        isTerminated.set(true);

                        if (log.isTraceEnabled()) {
                            long endMillis = System.currentTimeMillis();
                            String fmtDuration = DurationFormatUtils.formatDuration(
                                    Duration.ofMillis(endMillis - startMillis).toMillis(),
                                    "H:mm:ss:SSS");
                            log.trace("Session: {}, started: {}, end: {}, duration: {}", sessionId,
                                    new Date(startMillis), new Date(endMillis), fmtDuration);
                        }
                    }
                });
    }

    @Override
    public void stop() {
        if (isTerminated.get()) {
            throw new ReplayException("Session is already terminated: " + sessionId);
        }

        log.trace("stop session: {}", sessionId);
        isRunning.set(false);
    }

    @Override
    public void rewind() {
        if (isTerminated.get()) {
            throw new ReplayException("Session is already terminated: " + sessionId);
        }

        log.trace("rewind session: {}", sessionId);
        doRewind();
    }

    private void doRewind() {
        currentIndex.set(0);
        if (!events.isEmpty())
            this.replayClockMillis = events.get(0).timestamp(); // Reset clock to first
                                                                // event's
        else
            this.replayClockMillis = 0;
    }

    @Override
    public void jumpToEvent(int eventId) {
        if (isTerminated.get()) {
            throw new ReplayException("Session is already terminated: " + sessionId);
        }

        log.trace("jump to eventId: {}, session: {}", eventId, sessionId);
        Integer idx = eventIndexById.get(eventId);
        if (idx == null) {
            throw new ReplayException(String.format("Invalid event ID:: {}", eventId));
        }
        jumpToEventByIndex(idx);
    }

    @Override
    public void forward(int skipCount) {
        if (isTerminated.get()) {
            throw new ReplayException("Session is already terminated: " + sessionId);
        }

        log.trace("forward: {}, session: {}", skipCount, sessionId);
        int targetIndex = currentIndex.get() + skipCount;
        if (targetIndex >= events.size()) {
            log.trace("forward: {}, session: {} - reached end of events", skipCount, sessionId);
            jumpToEventByIndex(events.size());
        }
        else {
            jumpToEventByIndex(targetIndex);
        }
    }

    private void jumpToEventByIndex(int index) {
        currentIndex.set(index);
        if (index < events.size())
            replayClockMillis = events.get(index).timestamp();
    }

    @Override
    public void replaySpeed(double replaySpeed) {
        if (isTerminated.get()) {
            throw new ReplayException("Session is already terminated: " + sessionId);
        }

        log.trace("set replay speed: {} on session: {}", replaySpeed, sessionId);
        this.replaySpeed.set(replaySpeed);
    }

    @Override
    public Flux<MarketDataEvent> subscribe() {
        if (isTerminated.get()) {
            throw new ReplayException("Session is already terminated: " + sessionId);
        }

        log.trace("subscribe to session: {}", sessionId);
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

    public Double getReplaySpeed() {
        return replaySpeed.get();
    }

    public int getCurrentIndex() {
        return currentIndex.get();
    }

    @Override
    public String toString() {
        return String.format("Session: {}, Created: {}", sessionId, created);
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public boolean isTerminated() {
        return isTerminated.get();
    }
}
