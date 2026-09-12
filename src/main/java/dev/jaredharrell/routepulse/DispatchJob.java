package dev.jaredharrell.routepulse;

public record DispatchJob(String id, int priority, long createdAt, int serviceTicks) implements Comparable<DispatchJob> {
    public DispatchJob {
        if (priority < 1 || priority > 5) throw new IllegalArgumentException("priority must be 1..5");
        if (serviceTicks < 1) throw new IllegalArgumentException("serviceTicks must be positive");
    }

    @Override public int compareTo(DispatchJob other) {
        int byPriority = Integer.compare(other.priority, priority);
        if (byPriority != 0) return byPriority;
        int byAge = Long.compare(createdAt, other.createdAt);
        return byAge != 0 ? byAge : id.compareTo(other.id);
    }
}
