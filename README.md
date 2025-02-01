# Market Data Replay Service 

## Tech Stack

- Java, Spring Boot, Spring WebFlux, Google Guava (cache), Gradle, VS Code

## Assumptions

1. I will assume that the simulator will be used by a small number of clients (e.g. >= 0, <= 20). Replay sessions for a small number of clients can be handled on a single server using native Java threads (or threadpool). For a greater number of clients on a single server, virtual threads would work well but they are not quite production grade in Java 17. In any case, one could always spin up additional replay servers along with a load balancer to fan out sessions among servers
  - [sree] ok
 
2. I will assume that CSV file contents can fit into replay process memory (all data is cached)
  - [sree] ok
 
3. I will assume that simple streaming of data - e.g. server side events or other streaming strategy - is adequate (instead of using pub/sub to offload streaming responsibility to a message broker)
  -[ sree] correct
 
4. I will assume that a replay session that is interrupted midstream will not require automatic session reconnect/restart capability
  - [sree] correct
 
5. There will be two 'clocks' - a Replay clock and a Simulation clock. The Replay clock (per session) will control the frequency (how often) of data publication to a replay session. The Simulation clock (also per session) will control the quantity (how much) data is published to a replay session
  - [sree] ok. Had to read the next 2 to understand this. So your ‘Replay clock’ specifies the publishing cadence. Yes, that works.
 
6. Replay clock granularity will be 1 second (might be configurable) - i.e. data events will be published to each session at 1 second intervals 
  - [sree]  1 sec would likely need some buffering and smoothening out by the UI. Try for an approach that does not require UI to buffer/smooth out. Assume a reasonably powerful server for your app, and the limited number of clients that you noted above to keep the design simple.
 
7. Simulation clock granularity will be governed by the replay rate (default: 1.0, may be changed by client). For example, a replay rate of 1.0 would advance the simulation 
clock in lockstep with the replay clock. And a replay rate of 1.5 would advance the simulation clock at 1.5X the Replay rate - e.g. each time the replay clock advances by 1 second (1000 milliseconds), the simulation clock will advance by 1.5 seconds (1500 millisecs)
  - [sree] yes. Support both speed up and slow down.
 
8. During each publishing interval (see 6) all unpublished events within the session simulation clock window will be published
  - [sree] ok
 
9. Data will be published as JSON (CSV row data -> Record -> JSON conversion done on replay server)
  - [sree] ok

10. Security (Https) and authentication out of scope

## Design

- Reactive Spring Flux application, REST API for replay controls
- Data -> read from CSV file. FYI - The file had invisible BOM which caused a lot of head scratching before I pinpointed the cause and fixed it (see below)
- Sliding window -> virtual sliding window moves over cached events, during each publishing cycle ALL events under sliding window are published.
- Two settings control the sliding window and event publication
  - publishTimerMillis ->  controls how often the sliding window is moved. Default: 10 ms, configurable via **application.properties**.
  - replayClockMillis -> how far time has progressed in a replay session. It effectively controls the sliding window size. When session is created or rewound
replayClockMillis is set to the timestamp of first data event. At each publishing cycle all *unpublished* events with timestamp <= replayClockMillis are published,
and replayClockMillis advances, depending on the *replaySpeed* (see below).

  - replaySpeed -> controls how rapidly the replay clock advances. For example, suppose  publishTimerMillis = 10 , and replaySpeed = 1.0. Then at each publishing cycle
     replayClockMillis will advance by 10 ms (publishTimerMillis * replaySpeed). Now, suppose  replaySpeed is bumped up to 2.0. At the next publishing cycle, replayClockMillis 
will advance by 20 ms, even though only 10 ms have passed on the system clock (publishTimerMillis = 10). This works both for speeding up (> 1.0) and slowing down (< 1.0) replay. 

## Main Artifacts
- MarketDataController -> entry point for the REST API
- ReplayService -> provides services for managing the lifecycle of sessions (create, start, stop, rewind, etc.)
- ReplaySession -> replace session
- MarketDataEvent -> record with data from the market data CSV file
- CSVReaderService -> CSV reader service interface
  - JacksonCSVReader -> Jackson implementation
  - ApacheCSVReaderService -> Apache Commons implementation. Tried this first but was not clean code (deprecated API, dealing with BOM was cumbersome)

## Running

- Clone project, load in VSCode (or your favorite IDE), launch application

## API
- Documentation is [here](/himalayahall/demo/blob/f0d0e7bf3b3ee5e3bf8b4e38404bc64c2865d09a/api-documentation.pdf)
- Below are 2 ways of headless interaction with this service:

  1. Spring OpenAPI browser interface is baked in. Navigate to http://localhost:8080/swagger-ui.html on your browser. All controls work through the OpenAPI interface, **except** 
the streaming of market data events is not rendered on the browser.

  3. Use CURL to access the API. For example, *curl -X GET http://localhost:8080/mktdata/session/stream/e8cc93be-3723-4c37-8681-b3fa6d3b7a79* to subscribe for events on session 
e8cc93be-3723-4c37-8681-b3fa6d3b7a79.
