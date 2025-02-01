package com.pragma.demo.controller;

import jakarta.validation.constraints.Positive;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.pragma.demo.service.MarketDataEvent;
import com.pragma.demo.service.MarketDataService;
import com.pragma.demo.service.ReplayException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/mktdata")
public class MarketDataController {

    @Autowired
    private MarketDataService marketDataService;

    @PostMapping("/session")
    public Mono<String> createSession() {
        return Mono.just(marketDataService.createSession());
    }

    @PutMapping("/session/start/{sessionId}")
    public Mono<String> start(@PathVariable String sessionId) {
        return Mono.fromCallable(() -> {
            validateUUID(sessionId); // Runs in a reactive-safe way
            return sessionId;
        }).then(Mono.fromRunnable(() -> marketDataService.start(sessionId))) // Runs
                                                                             // non-blocking
                .thenReturn("Replay started for session " + sessionId)
                .onErrorResume(ReplayException.class, e -> Mono
                        .error(new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage())));
    }

    @PutMapping("/session/stop/{sessionId}")
    public Mono<String> stop(@PathVariable String sessionId) {
        return Mono.fromCallable(() -> {
            validateUUID(sessionId); // Runs in a reactive-safe way
            return sessionId;
        }).then(Mono.fromRunnable(() -> marketDataService.stop(sessionId))) // Runs
                                                                            // non-blocking
                .thenReturn("Replay stopped for session " + sessionId)
                .onErrorResume(ReplayException.class, e -> Mono
                        .error(new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage())));
    }

    @PutMapping("/session/rewind/{sessionId}")
    public Mono<String> rewind(@PathVariable String sessionId) {
        return Mono.fromCallable(() -> {
            validateUUID(sessionId); // Runs in a reactive-safe way
            return sessionId;
        }).then(Mono.fromRunnable(() -> marketDataService.rewind(sessionId))) // Runs
                                                                              // non-blocking
                .thenReturn("Rewound session " + sessionId)
                .onErrorResume(ReplayException.class, e -> Mono
                        .error(new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage())));
    }

    @PutMapping("/session/jump/{sessionId}/{eventId}")
    public Mono<String> jumpToEvent(@PathVariable String sessionId, @PathVariable @Positive int eventId) {
        return Mono.fromCallable(() -> {
            validateUUID(sessionId); // Runs in a reactive-safe way
            return sessionId;
        }).then(Mono.fromRunnable(() -> marketDataService.jumpToEvent(sessionId, eventId))) // Runs
                                                                                            // non-blocking
                .thenReturn("Jumped to event " + eventId + " for session " + sessionId)
                .onErrorResume(ReplayException.class, e -> Mono
                        .error(new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage())));
    }

    @PutMapping("/session/speed/{sessionId}")
    public Mono<String> setReplaySpeed(@PathVariable String sessionId,
            @RequestParam @Positive double speed) {
        return Mono.fromCallable(() -> {
            validateUUID(sessionId); // Runs in a reactive-safe way
            return sessionId;
        }).then(Mono.fromRunnable(() -> marketDataService.setReplaySpeed(sessionId, speed))) // Runs
                                                                                             // non-blocking
                .thenReturn("Replay speed set to " + speed + " for session " + sessionId)
                .onErrorResume(ReplayException.class, e -> Mono
                        .error(new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage())));
    }

    @GetMapping(value = "/session/stream/{sessionId}", produces = "text/event-stream")
    public Flux<MarketDataEvent> streamEvents(@PathVariable String sessionId) {
        return Mono.fromCallable(() -> {
            validateUUID(sessionId); // This may throw exceptions
            return sessionId;
        }).thenMany(marketDataService.getEventStream(sessionId)) // Continue if validation passes
                .onErrorResume(ResponseStatusException.class, e -> Flux.error(e))
                .onErrorResume(ReplayException.class, e -> Flux
                        .error(new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage())));
    }

    private void validateUUID(String sessionId) {
        try {
            UUID.fromString(sessionId); // Throws exception if invalid
            if (!marketDataService.isSession(sessionId))
                throw new ReplayException("Session " + sessionId + " not found");
        }
        catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid UUID format");
        }
    }
}
