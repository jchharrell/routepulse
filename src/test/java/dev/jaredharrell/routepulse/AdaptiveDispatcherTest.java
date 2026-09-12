package dev.jaredharrell.routepulse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdaptiveDispatcherTest {

    @Test
    void old_low_priority_job_eventually_beats_newer_high_priority_work() {
        var dispatcher = new AdaptiveDispatcher(1.0);
        dispatcher.submit(new DispatchJob("old", 1, 0, 1));

        // Keep introducing fresh priority-5 work. Aging should eventually rescue "old".
        for (int t = 0; t < 8; t++) {
            dispatcher.submit(new DispatchJob("urgent-" + t, 5, dispatcher.currentTick(), 1));
            dispatcher.tick();
        }

        assertTrue(dispatcher.decisions().stream().anyMatch(d -> d.jobId().equals("old")));
        var decision = dispatcher.decisions().stream().filter(d -> d.jobId().equals("old")).findFirst().orElseThrow();
        assertTrue(decision.waitedTicks() >= 4);
        assertTrue(decision.explanation().contains("starvation"));
    }

    @Test
    void decision_trace_matches_selected_job() {
        var dispatcher = new AdaptiveDispatcher(0.25);
        dispatcher.submit(new DispatchJob("A", 2, 0, 1));
        dispatcher.submit(new DispatchJob("B", 5, 0, 1));
        dispatcher.tick();

        assertEquals("B", dispatcher.decisions().get(0).jobId());
        assertEquals(5, dispatcher.decisions().get(0).basePriority());
        assertTrue(dispatcher.decisions().get(0).effectivePriority() >= 5.0);
    }

    @Test
    void comparison_is_deterministic_for_same_seed() {
        var first = ScenarioComparison.run(99, 80);
        var second = ScenarioComparison.run(99, 80);
        assertEquals(first.jobs(), second.jobs());
        assertEquals(first.baselineAverageWait(), second.baselineAverageWait(), 1e-9);
        assertEquals(first.adaptiveAverageWait(), second.adaptiveAverageWait(), 1e-9);
        assertEquals(first.adaptiveMaxWait(), second.adaptiveMaxWait());
    }
}
