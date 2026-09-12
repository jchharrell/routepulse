package dev.jaredharrell.routepulse;

import java.util.*;

/**
 * Replays one deterministic arrival stream through the baseline and adaptive
 * policies. The comparison is useful because scheduling claims are easier to
 * defend when both policies see exactly the same workload.
 */
public final class ScenarioComparison {
    public record Snapshot(int jobs, double baselineAverageWait, double adaptiveAverageWait,
                           int adaptiveMaxWait, int adaptiveDecisions, String lastExplanation) {}

    public static Snapshot run(long seed, int ticks) {
        var rng = new Random(seed);
        var arrivals = new ArrayList<List<DispatchJob>>();
        int nextId = 1;
        for (int t = 0; t < ticks; t++) {
            boolean rush = t >= ticks / 3 && t <= (ticks * 2) / 3;
            int count = rush ? 2 : (rng.nextDouble() < 0.6 ? 1 : 0);
            var atTick = new ArrayList<DispatchJob>();
            for (int i = 0; i < count; i++) {
                int priority = 1 + rng.nextInt(5);
                int service = 1 + rng.nextInt(4);
                atTick.add(new DispatchJob("J" + nextId++, priority, t, service));
            }
            arrivals.add(atTick);
        }

        var baseline = new Dispatcher();
        var adaptive = new AdaptiveDispatcher(0.25);
        for (int t = 0; t < ticks; t++) {
            for (var job : arrivals.get(t)) {
                baseline.submit(job);
                adaptive.submit(job);
            }
            baseline.tick();
            adaptive.tick();
        }
        // Drain both queues so wait statistics represent the whole generated workload.
        int guard = ticks * 20 + 1000;
        while ((baseline.queued() > 0 || baseline.activeJobId().isPresent()) && guard-- > 0) baseline.tick();
        guard = ticks * 20 + 1000;
        while ((adaptive.queued() > 0 || adaptive.activeJobId().isPresent()) && guard-- > 0) adaptive.tick();

        var decisions = adaptive.decisions();
        String last = decisions.isEmpty() ? "no dispatches" : decisions.get(decisions.size() - 1).explanation();
        return new Snapshot(nextId - 1, baseline.averageWait(), adaptive.averageWait(), adaptive.maxObservedWait(), decisions.size(), last);
    }

    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 42L;
        int ticks = args.length > 1 ? Integer.parseInt(args[1]) : 120;
        var result = run(seed, ticks);
        System.out.printf(Locale.ROOT,
                "jobs=%d baselineAvgWait=%.2f adaptiveAvgWait=%.2f adaptiveMaxWait=%d decisions=%d%n",
                result.jobs(), result.baselineAverageWait(), result.adaptiveAverageWait(), result.adaptiveMaxWait(), result.adaptiveDecisions());
        System.out.println("last decision: " + result.lastExplanation());
    }
}
