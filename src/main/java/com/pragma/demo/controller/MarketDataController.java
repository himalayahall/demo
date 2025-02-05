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
@Tag(name = "Market Data Replay",
                description = "APIs for streaming pre-recorded market data. Provides APIs to create session, start, stop, rewind, jump, forward, and set replay speed.")
public class MarketDataController {

        @Autowired
        private ReplayService marketDataService;

        @PostMapping("/session")
        @Operation(summary = "Create replay session",
                        description = "Create new replay session. Make sure to subscribe to session by calling stream.")
        @ApiResponse(responseCode = "200", description = "Successfully created session")
        public Mono<String> createSession() {  
                return Mono.just(marketDataService.createSession());
        }

        @PutMapping("/session/start/{sessionId}")
        @Operation(summary = "Start replay session.",
                        description = "Start streaming events from replay session. A stopped replay session will start from previous saved state. Once a session has published all events it is stopped. It is a no-op for running sessions.")
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
        @Operation(summary = "Stop replay session.",
                        description = "Stop streaming events from replay session. Current state is saved. Call start to resume streaming from saved state. It is a no-op for stopped sessions.")
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
        @Operation(summary = "Rewind session to beginning.",
                        description = "Rewind session to beginning. Running replay sessions will resume streaming from beginning.")
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
        @Operation(summary = "Jump to session event.",
                        description = "Jump to specific event in the replay session. Running sessions will resume streaming from specified event.")
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
        @Operation(summary = "Forward replay session by number of events.",
                        description = "Jump forward by skipping specified number of events. If number of skip events is greater than remaining events, session will stop (does not wrap around).")
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
        @Operation(summary = "Set replay speed.",
                        description = "Set replay session speed. Default speed is 1.0"
                                        + " (real-time). Value less than 1.0 will slow down replay. Value greater than 1.0 will speed up replay.")
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

        @GetMapping(value = "/session/subscribe/{sessionId}", produces = "text/event-stream")
        @Operation(summary = "Subscribe to replay session events.",
                        description = "Subscribe to replay session events. After subscription, replay session must be started to actually begin streaming.")
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

        @GetMapping(value = "/session/subscribe_start/{sessionId}", produces = "text/event-stream")
        @Operation(summary = "Subscribe to replay session events and start session.",
                        description = "Subscribe to replay session events and then start session.")
        @ApiResponse(responseCode = "200", description = "Successfully subscribed and started")
        @ApiResponse(responseCode = "400", description = "Bad request")
        public Flux<MarketDataEvent> subscribeStart(@PathVariable @Parameter(name = "sessionId",
                        description = "Session Id (UUID)", required = true) String sessionId) {
                return Mono.fromCallable(() -> {
                        validateUUID(sessionId);
                        return sessionId;
                }).thenMany(doSubscribeStart(sessionId))
                                .onErrorResume(ResponseStatusException.class, e -> Flux.error(e))
                                .onErrorResume(ReplayException.class,
                                                e -> Flux.error(new ResponseStatusException(
                                                                HttpStatus.BAD_REQUEST,
                                                                e.getMessage())));
        }

        private Flux<MarketDataEvent> doSubscribeStart(String sessionId) {
                Flux<MarketDataEvent> flux = marketDataService.subscribe(sessionId);
                marketDataService.start(sessionId);
                return flux;
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
