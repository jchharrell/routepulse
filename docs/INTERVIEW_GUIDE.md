# RoutePulse interview guide

## 30-second version

RoutePulse is a Java scheduling simulator that compares strict-priority dispatch with a starvation-aware aging policy on the same deterministic arrival stream. The adaptive scheduler records a decision explanation each time it chooses a job, so I can talk about both algorithmic complexity and operational fairness.

## Five-minute walkthrough

1. **Problem:** strict priority works well until urgent traffic is sustained. Then lower-priority jobs may never run.
2. **`DispatchJob`:** immutable job record with priority, creation tick, and service duration.
3. **`Dispatcher`:** baseline `PriorityQueue` implementation.
4. **`AdaptiveDispatcher`:** compute `base priority + wait-time credit` at dispatch time so aging changes ordering correctly.
5. **`DispatchDecision`:** preserve the inputs to the scheduling choice and a human-readable explanation.
6. **`ScenarioComparison`:** generate arrivals once, replay them into both policies, then compare metrics.
7. **Tests:** explicitly create sustained urgent traffic and verify that an old low-priority job eventually runs.

## Questions I should be able to answer

### What is starvation?
A task is ready to run but can be postponed indefinitely because other tasks keep receiving preference. Strict priority scheduling can starve low-priority work if higher-priority work arrives continuously.

### Why does aging help?
Waiting time gradually increases a job's effective priority. Even if new high-priority work keeps arriving, a sufficiently old low-priority job eventually becomes competitive.

### What is the time complexity?
The baseline `PriorityQueue` inserts/removes in `O(log n)`. The adaptive implementation uses a list and scans for the maximum current score at dispatch time, so selection is `O(n)`. I chose correctness and inspectability for the simulation because priorities change as time advances. For a large scheduler I would consider bucketed priorities, indexed heaps with explicit score refreshes, or deadline-based structures.

### Why can't you leave jobs in a Java PriorityQueue and just change the scoring function?
`PriorityQueue` assumes an element's ordering stays stable while it is in the heap. If effective priority changes because time changed, the heap does not magically reheapify every element. You must remove/reinsert/update or use a structure that matches dynamic priorities.

### Could aging make low-priority work beat truly urgent work?
Yes if configured too aggressively. Aging is a policy parameter. A production design might cap credits, use deadlines, create separate urgency classes, or reserve worker capacity so fairness does not violate critical-service guarantees.

### Why average wait is not enough?
A low average can hide a small set of extremely delayed jobs. I would add p95/p99 wait, maximum wait, starvation count, and deadline misses before tuning a real scheduler.

### Why deterministic seeds?
They make comparisons reproducible. Both schedulers must see the exact same arrivals; otherwise random workload differences can be mistaken for algorithm improvement.

## Memorable angle

The project is not “I used a priority queue.” It is **what happens when a simple algorithm meets an unfair workload, and how do you make the correction explainable?**

## What I would not claim

- Aging is always the best scheduling policy.
- The simulation models a specific company's production queue.
- Average wait alone proves one policy is superior.
- The current single-worker simulation captures distributed-systems failure modes.
