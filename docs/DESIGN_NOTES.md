# RoutePulse Design Notes

RoutePulse is a Java scheduling simulator for studying the tradeoff between strict priority and starvation-aware dispatch under the same deterministic workload.

## Design goals

- make scheduling policy behavior reproducible
- compare policies against identical arrivals
- surface fairness failures that average metrics can hide
- keep dispatch decisions explainable
- separate baseline and adaptive policies cleanly

## Baseline scheduler

`Dispatcher` uses Java's `PriorityQueue` with a stable ordering based on priority, age, and identifier tie-breaks. Insert and removal operations are `O(log n)` and the implementation provides a useful baseline for normal priority scheduling.

## Adaptive scheduler

`AdaptiveDispatcher` adds waiting-time credit to a job's base priority. The effective score is calculated at dispatch time so old low-priority work can eventually compete with continuously arriving urgent work.

The implementation intentionally scans the current queue when selecting work because effective priority changes as time advances. A normal `PriorityQueue` cannot safely represent dynamic ordering without explicit reheapification or reinsertion.

## Explainable decisions

`DispatchDecision` records the inputs that caused a scheduling choice and preserves a human-readable explanation. This makes the policy inspectable rather than exposing only final queue statistics.

## Scenario comparison

`ScenarioComparison` generates a workload once and replays the same arrival stream into both scheduling policies. A fixed seed prevents random differences from being mistaken for algorithm improvements.

The comparison focuses on queue length, waiting time, dispatch order, and starvation behavior. A production study would also include tail latency, deadline misses, throughput, worker pools, and failure recovery.

## Complexity tradeoff

The baseline heap has efficient `O(log n)` operations. The adaptive implementation currently chooses clarity and correctness over asymptotic optimization and scans the queue in `O(n)` when dispatching. For larger systems, alternatives include bucketed priorities, indexed heaps with explicit refresh, or deadline-oriented structures.

## Verification

JUnit tests cover ordering, deterministic behavior, aging, and a sustained high-priority workload that verifies older low-priority work eventually receives service. GitHub Actions runs the Maven test suite, packages the project, and executes the simulation as a smoke test.

## Scope

RoutePulse is a deterministic single-worker simulation, not a model of a specific production queue. The aging policy is an experimental scheduling strategy, not a universal recommendation.