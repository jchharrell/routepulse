# RoutePulse

**An explainable dispatch simulator for studying priority queues, starvation, fairness, and the tradeoff between “serve the most urgent job now” and “never let ordinary work wait forever.”**

RoutePulse started as a priority-queue exercise and grew into a systems project about a real scheduling problem: strict priority is simple, but under sustained high-priority traffic it can starve lower-priority work indefinitely.

The project now includes two schedulers that can replay the **same deterministic workload**:

- `Dispatcher` — strict priority baseline
- `AdaptiveDispatcher` — priority plus waiting-time aging credits

The adaptive policy records an explanation every time it chooses a job, so the scheduling decision is inspectable rather than hidden inside a comparator.

> The workload is synthetic. The project is intended to demonstrate scheduling and systems-design reasoning, not a real logistics network.

## What makes this project different

### Starvation-aware priority aging

A job's effective score is:

```text
effective priority = base priority + aging_per_tick * waited_ticks
```

Fresh urgent work still wins early. But every tick gives waiting jobs more credit, so an old priority-1 job can eventually outrank newer priority-5 jobs. This is a small version of a fairness mechanism used in operating systems and queue schedulers.

### Explainable decisions

Each adaptive dispatch produces a `DispatchDecision` containing:

- selected job ID
- base priority
- wait time
- effective priority
- a plain-English explanation

When aging materially changes the outcome, the explanation explicitly says that aging prevented starvation.

### Same-workload policy comparison

`ScenarioComparison` generates one deterministic arrival stream and runs both policies against it. This avoids a common simulation mistake: comparing schedulers on different random workloads and then attributing the difference to the algorithm.

It reports baseline average wait, adaptive average wait, adaptive max observed wait, and the number of explainable decisions.

## Architecture

```text
synthetic arrival stream
          |
          +-------------------------+
          |                         |
          v                         v
   Dispatcher                AdaptiveDispatcher
 strict PriorityQueue        dynamic aging score
          |                         |
          v                         v
 average wait              wait + decision traces
          \                         /
           \                       /
             ScenarioComparison
          same workload / same seed
```

## Quick start

Requires Java 17+ and Maven.

```bash
git clone https://github.com/jchharrell/routepulse.git
cd routepulse
mvn test
```

Compile and run the comparison:

```bash
mvn -q -DskipTests package
java -cp target/classes dev.jaredharrell.routepulse.ScenarioComparison
```

Optional arguments are seed and simulation ticks:

```bash
java -cp target/classes dev.jaredharrell.routepulse.ScenarioComparison 99 200
```

The existing `Simulation` class is also available if you want to watch queue depth/average wait over time.

## Core classes

| Class | Responsibility |
| --- | --- |
| `DispatchJob` | immutable job model and baseline priority ordering |
| `Dispatcher` | strict-priority baseline scheduler |
| `AdaptiveDispatcher` | dynamic priority aging and decision tracing |
| `DispatchDecision` | explainable scheduling record |
| `ScenarioComparison` | deterministic A/B replay of the same arrivals |
| `Simulation` | lightweight interactive console simulation |

## Tests

```bash
mvn test
```

The test suite checks:

- strict priority ordering
- deterministic tie behavior
- wait-time metrics
- starvation rescue under sustained urgent arrivals
- correctness of decision traces
- deterministic policy comparisons for a fixed seed

GitHub Actions runs Maven tests on every push and pull request.

## Design decisions I can explain

**Why not just use `PriorityQueue` for the adaptive scheduler?** The effective priority changes every tick as a job ages. Java's `PriorityQueue` does not automatically reorder existing elements when an external scoring function changes. The adaptive scheduler keeps queued jobs in a list and chooses the maximum current score at dispatch time. That trades `O(log n)` removal for an `O(n)` selection, which is acceptable for a small simulation and keeps the dynamic policy correct.

**Why aging instead of round-robin?** Round-robin is fair but ignores business urgency. Aging keeps priority meaningful while placing a bound on how long low-priority work can be ignored.

**Why compare the same arrival stream?** It isolates the policy as the independent variable. If both policies see different random jobs, the experiment is harder to interpret.

**Why explain scheduling decisions?** Queueing algorithms affect who waits. A trace makes the policy debuggable and lets operators understand why the system selected one job over another.

## Production directions

A larger implementation could use multiple workers, resource/skill constraints, durable queues, leases, idempotency keys, cancellation, deadlines, preemption, backpressure, distributed tracing, event storage, workload replay, and policy tuning against service-level objectives.

A particularly interesting extension would optimize not only average wait but also **tail wait and starvation debt**, because a low average can hide a small group of jobs waiting far too long.

## Interview walkthrough

See [`docs/INTERVIEW_GUIDE.md`](docs/INTERVIEW_GUIDE.md) for the five-minute project story, algorithm questions, complexity tradeoffs, and what I would build next.
