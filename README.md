# RoutePulse

A Java dispatch/queue simulation for exploring scheduling behavior under normal and rush-load conditions. The browser UI is the visual demo; the Java core models the queue and worker behavior independently.

## What it demonstrates

- Java records and validation
- `PriorityQueue` with deterministic ordering
- priority-first scheduling with FIFO tie breaking
- explicit simulation clock and worker state
- queue depth and average-wait observability
- deterministic load simulation using a seeded RNG
- JUnit tests and GitHub Actions CI

## Run

Requires Java 17 + Maven:

```bash
mvn test
mvn -q package
java -cp target/classes dev.jaredharrell.routepulse.Simulation
```

Or open `index.html` for the browser visualization.

## Architecture

- `DispatchJob` — immutable job, validation, ordering contract
- `Dispatcher` — priority queue, worker state, wait metrics, clock
- `Simulation` — repeatable traffic/rush scenario
- `DispatcherTest` — priority, FIFO, completion, and validation behavior

## Decisions I can explain

I used a priority queue because selecting the next highest-priority job should be cheaper and clearer than repeatedly sorting an entire list. Ties use creation time and then ID so ordering is deterministic. The simulator uses ticks instead of wall-clock time, which makes tests repeatable and lets load behavior be explored without sleeps or threads.

The current core has one worker on purpose. A production version with multiple workers introduces concurrency questions: reservation/locking, duplicate delivery, retries, idempotency, and ordering guarantees.

## Production direction

Durable message broker, worker pool, retry/dead-letter queues, idempotency keys, persistence, service-level objectives, OpenTelemetry traces/metrics, and load tests that compare scheduling policies.

## Scope

Synthetic simulation only. The small domain is designed to be explainable in interviews while still giving room to discuss distributed-systems tradeoffs.
