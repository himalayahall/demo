package com.pragma.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReplaySessionImplTest {

    private ReplaySessionImpl replaySession;
    private List<MarketDataEvent> events;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Sample market data events
        events = List.of(
                new MarketDataEvent(1, 1000, "Event1", 100.0, 10, "XCHG1", 101.0, 5, "XCHG2"),
                new MarketDataEvent(2, 2000, "Event2", 101.0, 15, "XCHG1", 102.0, 10, "XCHG2"),
                new MarketDataEvent(3, 3000, "Event3", 102.0, 20, "XCHG1", 103.0, 15, "XCHG2")
        );

        replaySession = new ReplaySessionImpl("session1", events, 100);
    }

    @Test
    void testStart() {
        // Verify that events are emitted
        Flux<MarketDataEvent> eventFlux = replaySession.subscribe();
        replaySession.start();
        StepVerifier.create(eventFlux)
                .expectNext(events.get(0))
                .expectNext(events.get(1))
                .expectNext(events.get(2))
                .thenCancel() // Cancel to avoid infinite stream
                .verify();
    }

    @Test
    void testStop() {
        replaySession.start();
        replaySession.stop();

        // Verify that no events are emitted after stopping
        Flux<MarketDataEvent> eventFlux = replaySession.subscribe();
        StepVerifier.create(eventFlux)
                .expectNextCount(0)
                .thenCancel()
                .verify();
    }

    @Test
    void testRewind() {
        replaySession.start();
        replaySession.rewind();

        // Verify that the session starts from the beginning
        Flux<MarketDataEvent> eventFlux = replaySession.subscribe();
        StepVerifier.create(eventFlux)
                .expectNext(events.get(0))
                .expectNext(events.get(1))
                .expectNext(events.get(2))
                .thenCancel()
                .verify();
    }

    @Test
    void testJumpToEvent() {
        replaySession.start();
        replaySession.jumpToEvent(2); // Jump to event with ID 2

        // Verify that the session starts from the specified event
        Flux<MarketDataEvent> eventFlux = replaySession.subscribe();
        StepVerifier.create(eventFlux)
                .expectNext(events.get(1)) // Event with ID 2
                .expectNext(events.get(2)) // Next event
                .thenCancel()
                .verify();
    }

    @Test
    void testForward() {
        replaySession.start();
        replaySession.forward(1); // Skip 1 event

        // Verify that the session skips the specified number of events
        Flux<MarketDataEvent> eventFlux = replaySession.subscribe();
        StepVerifier.create(eventFlux)
                .expectNext(events.get(1)) // Skipped first event
                .expectNext(events.get(2)) // Next event
                .thenCancel()
                .verify();
    }

    @Test
    void testReplaySpeed() {
        replaySession.start();
        replaySession.replaySpeed(2.0); // Double the replay speed

        // Verify that the replay speed is updated
        assertEquals(2.0, replaySession.getReplaySpeed());
    }

    @Test
    void testSubscribe() {
        replaySession.start();

        // Verify that the event stream is correctly subscribed
        Flux<MarketDataEvent> eventFlux = replaySession.subscribe();
        StepVerifier.create(eventFlux)
                .expectNext(events.get(0))
                .expectNext(events.get(1))
                .expectNext(events.get(2))
                .thenCancel()
                .verify();
    }

    @Test
    void testSessionId() {
        assertEquals("session1", replaySession.sessionId());
    }

    @Test
    void testCreated() {
        assertNotNull(replaySession.created());
    }
}