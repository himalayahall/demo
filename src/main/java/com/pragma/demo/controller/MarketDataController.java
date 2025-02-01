package com.pragma.demo.controller;

import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.pragma.demo.service.MarketDataEvent;
import com.pragma.demo.service.ReplayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.pragma.demo.service.ReplayException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/mktdata")
@Tag(name = "Market Data", description = "APIs for streaming market data")
public class MarketDataController {

        @Autowired
        private ReplayService marketDataService;

        @PostMapping("/session")
        @Operation(summary = "Create session", description = "Create a new session. Call start")
        @ApiResponse(responseCode = "200", description = "Successfully created session")
        public Mono<String> createSession() {
                return Mono.just(marketDataService.createSession());
        }

        @PutMapping("/session/start/{sessionId}")
        @Operation(summary = "Start replay", description = "Start streaming.")
        @ApiResponse(responseCode = "200", description = "Successfully started")
        @ApiResponse(responseCode = "400", description = "Bad request")
        public Mono<String> start(@PathVariable @Parameter(name = "sessionId",
                        description = "Session Id (UUID)", required = true) String sessionId) {
                return Mono.fromCallable(() -> {
                        validateUUID(sessionId); // Runs in a reactive-safe way
                        return sessionId;
                }).then(Mono.fromRunnable(() -> marketDataService.start(sessionId))) // Runs
                                                                                     // non-blocking
                                .thenReturn("Replay started for session " + sessionId)
                                .onErrorResume(ReplayException.class,
                                                e -> Mono.error(new ResponseStatusException(
                                                                HttpStatus.NOT_FOUND,
                                                                e.getMessage())));
        }

        @PutMapping("/session/stop/{sessionId}")
        @Operation(summary = "Stop replay", description = "Stop session streaming.")
        @ApiResponse(responseCode = "200", description = "Successfully stopped")
        @ApiResponse(responseCode = "400", description = "Bad request")
        public Mono<String> stop(@PathVariable @Parameter(name = "sessionId",
                        description = "Session Id (UUID)", required = true) String sessionId) {
                return Mono.fromCallable(() -> {
                        validateUUID(sessionId); // Runs in a reactive-safe way
                        return sessionId;
                }).then(Mono.fromRunnable(() -> marketDataService.stop(sessionId))) // Runs
                                                                                    // non-blocking
                                .thenReturn("Replay stopped for session " + sessionId)
                                .onErrorResume(ReplayException.class,
                                                e -> Mono.error(new ResponseStatusException(
                                                                HttpStatus.NOT_FOUND,
                                                                e.getMessage())));
        }

        @PutMapping("/session/rewind/{sessionId}")
        @Operation(summary = "Rewind session", description = "Rewind session.")
        @ApiResponse(responseCode = "200", description = "Successfully rewound")
        @ApiResponse(responseCode = "400", description = "Bad request")
        public Mono<String> rewind(@PathVariable @Parameter(name = "sessionId",
                        description = "Session Id (UUID)", required = true) String sessionId) {
                return Mono.fromCallable(() -> {
                        validateUUID(sessionId); // Runs in a reactive-safe way
                        return sessionId;
                }).then(Mono.fromRunnable(() -> marketDataService.rewind(sessionId))) // Runs
                                                                                      // non-blocking
                                .thenReturn("Rewound session " + sessionId)
                                .onErrorResume(ReplayException.class,
                                                e -> Mono.error(new ResponseStatusException(
                                                                HttpStatus.NOT_FOUND,
                                                                e.getMessage())));
        }

        @PutMapping("/session/jump/{sessionId}/{eventId}")
        @Operation(summary = "Jump to session event",
                        description = "Jump to specific event in session.")
        @ApiResponse(responseCode = "200", description = "Successfully jumped to event")
        @ApiResponse(responseCode = "400", description = "Bad request")
        public Mono<String> jumpToEvent(@PathVariable @Parameter(name = "sessionId",
                        description = "Session Id (UUID)", required = true) String sessionId,
                        @PathVariable @Parameter(name = "eventId",
                                        description = "Event Id. Must be positive (> 0)",
                                        required = true) @Positive int eventId) {
                return Mono.fromCallable(() -> {
                        validateUUID(sessionId); // Runs in a reactive-safe way
                        return sessionId;
                }).then(Mono.fromRunnable(() -> marketDataService.jumpToEvent(sessionId, eventId))) // Runs
                                                                                                    // non-blocking
                                .thenReturn("Jumped to event " + eventId + " for session "
                                                + sessionId)
                                .onErrorResume(ReplayException.class,
                                                e -> Mono.error(new ResponseStatusException(
                                                                HttpStatus.NOT_FOUND,
                                                                e.getMessage())));
        }

        @PutMapping("/session/forward/{sessionId}/{skipCount}")
        @Operation(summary = "Forward session by number of events",
                        description = "Forward session by skipping a number of events.")
        @ApiResponse(responseCode = "200", description = "Successfully forwarded session")
        @ApiResponse(responseCode = "400", description = "Bad request")
        public Mono<String> forward(@PathVariable @Parameter(name = "sessionId",
                        description = "Session Id (UUID)", required = true) String sessionId,
                        @PathVariable @Parameter(name = "skipCount",
                                        description = "Number of events to skip. Must be positive (> 0)",
                                        required = true) @Positive int skipCount) {
                return Mono.fromCallable(() -> {
                        validateUUID(sessionId); // Runs in a reactive-safe way
                        return sessionId;
                }).then(Mono.fromRunnable(() -> marketDataService.forward(sessionId, skipCount))) // Runs
                                                                                                  // non-blocking
                                .thenReturn("Skip " + skipCount + " events for session "
                                                + sessionId)
                                .onErrorResume(ReplayException.class,
                                                e -> Mono.error(new ResponseStatusException(
                                                                HttpStatus.NOT_FOUND,
                                                                e.getMessage())));
        }

        @PutMapping("/session/speed/{sessionId}/{speed}")
        @Operation(summary = "Set replay speed", description = "Set replay speed for a session.")
        @ApiResponse(responseCode = "200", description = "Successfully set replay speed")
        @ApiResponse(responseCode = "400", description = "Bad request")
        public Mono<String> replaySpeed(@PathVariable @Parameter(name = "sessionId",
                        description = "Session Id (UUID)", required = true) String sessionId,
                        @PathVariable @Parameter(name = "speed",
                                        description = "Must be positive (> 0.0). Default is 1.0. Value less than 1.0 will slow down replay. Value greater than 1.0 will speed up replay.",
                                        required = true) @Positive double speed) {
                return Mono.fromCallable(() -> {
                        validateUUID(sessionId); // Runs in a reactive-safe way
                        return sessionId;
                }).then(Mono.fromRunnable(() -> marketDataService.replaySpeed(sessionId, speed))) // Runs
                                                                                                  // non-blocking
                                .thenReturn("Replay speed set to " + speed + " for session "
                                                + sessionId)
                                .onErrorResume(ReplayException.class,
                                                e -> Mono.error(new ResponseStatusException(
                                                                HttpStatus.NOT_FOUND,
                                                                e.getMessage())));
        }

        @GetMapping(value = "/session/stream/{sessionId}", produces = "text/event-stream")
        @Operation(summary = "Subscribe to market data events",
                        description = "Subscribe to market data events for a session. Call start to start streaming")
        @ApiResponse(responseCode = "200", description = "Successfully subscribed")
        @ApiResponse(responseCode = "400", description = "Bad request")
        public Flux<MarketDataEvent> subscribe(@PathVariable @Parameter(name = "sessionId",
                        description = "Session Id (UUID)", required = true) String sessionId) {
                return Mono.fromCallable(() -> {
                        validateUUID(sessionId); // This may throw exceptions
                        return sessionId;
                }).thenMany(marketDataService.subscribe(sessionId)) // Continue if validation passes
                                .onErrorResume(ResponseStatusException.class, e -> Flux.error(e))
                                .onErrorResume(ReplayException.class,
                                                e -> Flux.error(new ResponseStatusException(
                                                                HttpStatus.BAD_REQUEST,
                                                                e.getMessage())));
        }

        private void validateUUID(String sessionId) {
                try {
                        UUID.fromString(sessionId); // Throws exception if invalid
                        if (!marketDataService.isSession(sessionId))
                                throw new ReplayException("Session " + sessionId + " not found");
                }
                catch (IllegalArgumentException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Invalid UUID format");
                }
        }
}
