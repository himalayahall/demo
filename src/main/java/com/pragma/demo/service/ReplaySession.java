package com.pragma.demo.service;

import java.util.Date;
import reactor.core.publisher.Flux;

/**
 * Replay session.
 *
 */
public interface ReplaySession {
    /**
     * Start session. Subscriber, if any, to session event stream will start receiving events.
     * 
     */
    void start();

    /**
     * Stop session. Subscriber, if any, to session event stream will stop receiving events.
     */
    void stop();

    /**
     * Rewind session. Session is first stopped and then rewound to beginning.
     */
    void rewind();

    /**
     * Set replay speed.
     *
     * @param speed Replace speed. Must be POSITIVE (> 0.0)
     */
    void replaySpeed(double speed);

    /**
     * Jump to event.
     * @param eventId 
     */
    void jumpToEvent(int eventId);

    /**
     * Subscribe to session event stream.
     * 
     * @return Session event flux.
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
}
