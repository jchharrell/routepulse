package dev.jaredharrell.routepulse;

import java.util.*;

public final class Dispatcher {
    private final PriorityQueue<DispatchJob> queue = new PriorityQueue<>();
    private final List<Integer> completedWaits = new ArrayList<>();
    private long tick;
    private Active active;

    private record Active(DispatchJob job, int remaining) {}

    public void submit(DispatchJob job) { queue.add(job); }

    public void tick() {
        tick++;
        if (active == null && !queue.isEmpty()) {
            var job = queue.remove();
            completedWaits.add((int)Math.max(0, tick - job.createdAt()));
            active = new Active(job, job.serviceTicks());
        }
        if (active != null) {
            int remaining = active.remaining() - 1;
            active = remaining == 0 ? null : new Active(active.job(), remaining);
        }
    }

    public int queued() { return queue.size(); }
    public Optional<String> activeJobId() { return active == null ? Optional.empty() : Optional.of(active.job().id()); }
    public double averageWait() { return completedWaits.stream().mapToInt(Integer::intValue).average().orElse(0); }
    public long currentTick() { return tick; }
}
