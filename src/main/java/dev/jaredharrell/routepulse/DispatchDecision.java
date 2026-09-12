package dev.jaredharrell.routepulse;

public record DispatchDecision(
        long tick,
        String jobId,
        int basePriority,
        long waitedTicks,
        double effectivePriority,
        String explanation) {
}
