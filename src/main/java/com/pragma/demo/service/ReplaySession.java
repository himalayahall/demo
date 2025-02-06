package com.pragma.demo.service;

import java.util.Date;
import com.pragma.demo.models.MarketDataEvent;
import reactor.core.publisher.Flux;

/**
 * Replay session.
 *
 */
public interface ReplaySession {
    /**
     * Start session. Subscriber, if any, to session event stream will start receiving events. When
     * event stream has been exhausted the session is terminated.
     * 
     * 
     * @throws ReplayException if session is terminated.
     */
    void start();

    /**
     * Stop session. Subscriber, if any, to session event stream will stop receiving events.
     * 
     * @throws ReplayException if session is terminated.
     */
    void stop();

    /**
     * Rewind session. Session is rewound (without stopping) to beginning of event stream.
     * 
     * @throws ReplayException if session is terminated.
     */
    void rewind();

    /**
     * Set replay speed. No-op for terminated streams.
     *
     * @param speed Replay speed. Must be POSITIVE (> 0.0)
     * 
     * @throws ReplayException if session is terminated.
     */
    void replaySpeed(double speed);

    /**
     * Jump to event. No-op if eventId is not found. No-op for terminated streams.
     * 
     * @param eventId
     * 
     * @throws ReplayException if session is terminated.
     */
    void jumpToEvent(int eventId);

    /**
     * Forward session by numEvents. Moving past the end of event stream will stop the stream (if it
     * is currently running) but will not terminate it. No-op for terminated streams.
     * 
     * @param numEvents Number of events to forward.
     * 
     * @throws ReplayException if session is terminated.
     */
    void forward(int numEvents);

    /**
     * Subscribe to session event stream. No-op for terminated streams.
     * 
     * @return Session event flux.
     * 
     * @throws ReplayException if session is terminated.
     */
    Flux<MarketDataEvent> subscribe();

    /**
     * Get session creation timestamp.
     * 
     * @return Session creation timestamp.
     */
    Date created();

    /**
     * Get session id.
     * 
     * @return
     */
    String sessionId();

    /**
     * Check if session is running. Stopped session can be restarted.
     * 
     * @return True if session is running, false otherwise.
     */
    boolean isRunning();

    /**
     * Check if session is terminated. Session are terminated once they reach end of market data
     * stream. Terminated session cannot be restarted.
     * 
     * @return True if session is terminated, false otherwise.
     */
    boolean isTerminated();
}
