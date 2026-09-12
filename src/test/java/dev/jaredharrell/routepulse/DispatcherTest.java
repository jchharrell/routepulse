package dev.jaredharrell.routepulse;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DispatcherTest {
    @Test void higher_priority_job_is_dispatched_first() {
        var d = new Dispatcher();
        d.submit(new DispatchJob("low", 1, 0, 2));
        d.submit(new DispatchJob("critical", 5, 0, 2));
        d.tick();
        assertEquals("critical", d.activeJobId().orElseThrow());
    }

    @Test void fifo_breaks_priority_ties() {
        var d = new Dispatcher();
        d.submit(new DispatchJob("older", 3, 0, 2));
        d.submit(new DispatchJob("newer", 3, 1, 2));
        d.tick();
        assertEquals("older", d.activeJobId().orElseThrow());
    }

    @Test void completed_work_releases_worker() {
        var d = new Dispatcher();
        d.submit(new DispatchJob("one", 2, 0, 1));
        d.tick();
        assertTrue(d.activeJobId().isEmpty());
    }

    @Test void invalid_priority_is_rejected() {
        assertThrows(IllegalArgumentException.class, () -> new DispatchJob("bad", 8, 0, 1));
    }
}
