package com.pragma.demo.service;

import reactor.core.publisher.Flux;

/**
 * Replay session.
 *
 */
public interface MarketDataSession {
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
    void setReplaySpeed(double speed);

    /**
     * Subscribe to session event stream.
     * 
     * @return Session event flux.
     */
    Flux<MarketDataEvent> subscribe();
}
