package dev.jaredharrell.routepulse;

import java.util.*;

/**
 * Starvation-aware dispatcher.
 *
 * Jobs start with their business priority, but waiting time earns an "aging credit".
 * That lets an old low-priority job eventually outrank a stream of newer urgent work.
 * Every dispatch emits a human-readable decision trace so the scheduler is inspectable.
 */
public final class AdaptiveDispatcher {
    private final List<DispatchJob> queue = new ArrayList<>();
    private final List<Integer> completedWaits = new ArrayList<>();
    private final List<DispatchDecision> decisions = new ArrayList<>();
    private final double agingPerTick;
    private long tick;
    private Active active;

    private record Active(DispatchJob job, int remaining) {}

    public AdaptiveDispatcher() { this(0.20); }

    public AdaptiveDispatcher(double agingPerTick) {
        if (agingPerTick < 0) throw new IllegalArgumentException("agingPerTick must be non-negative");
        this.agingPerTick = agingPerTick;
    }

    public void submit(DispatchJob job) { queue.add(job); }

    public void tick() {
        tick++;
        if (active == null && !queue.isEmpty()) {
            DispatchJob selected = queue.stream()
                    .max(Comparator.comparingDouble(this::score)
                            .thenComparingLong(job -> -job.createdAt())
                            .thenComparing(DispatchJob::id, Comparator.reverseOrder()))
                    .orElseThrow();
            queue.remove(selected);
            int waited = (int)Math.max(0, tick - selected.createdAt());
            completedWaits.add(waited);
            double effective = score(selected);
            decisions.add(new DispatchDecision(
                    tick,
                    selected.id(),
                    selected.priority(),
                    waited,
                    effective,
                    explain(selected, waited, effective)));
            active = new Active(selected, selected.serviceTicks());
        }
        if (active != null) {
            int remaining = active.remaining() - 1;
            active = remaining == 0 ? null : new Active(active.job(), remaining);
        }
    }

    private double score(DispatchJob job) {
        long waited = Math.max(0, tick - job.createdAt());
        return job.priority() + agingPerTick * waited;
    }

    private String explain(DispatchJob job, long waited, double effective) {
        double agingCredit = agingPerTick * waited;
        if (agingCredit >= 1.0) {
            return String.format(
                    Locale.ROOT,
                    "%s selected: base priority %d + %.2f aging credit after %d ticks = %.2f. Aging prevented starvation.",
                    job.id(), job.priority(), agingCredit, waited, effective);
        }
        return String.format(
                Locale.ROOT,
                "%s selected: base priority %d + %.2f aging credit after %d ticks = %.2f.",
                job.id(), job.priority(), agingCredit, waited, effective);
    }

    public int queued() { return queue.size(); }
    public long currentTick() { return tick; }
    public Optional<String> activeJobId() { return active == null ? Optional.empty() : Optional.of(active.job().id()); }
    public double averageWait() { return completedWaits.stream().mapToInt(Integer::intValue).average().orElse(0); }
    public int maxObservedWait() { return completedWaits.stream().mapToInt(Integer::intValue).max().orElse(0); }
    public List<DispatchDecision> decisions() { return List.copyOf(decisions); }
}
