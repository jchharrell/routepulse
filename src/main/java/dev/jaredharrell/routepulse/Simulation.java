package dev.jaredharrell.routepulse;

import java.util.Random;

public final class Simulation {
    public static void main(String[] args) {
        var rng = new Random(42);
        var dispatcher = new Dispatcher();
        int id = 1;
        for (int t = 0; t < 100; t++) {
            boolean rush = t >= 35 && t <= 60;
            int arrivals = rush ? 2 : (rng.nextDouble() < .55 ? 1 : 0);
            for (int i = 0; i < arrivals; i++) {
                dispatcher.submit(new DispatchJob("J" + id++, 1 + rng.nextInt(5), dispatcher.currentTick(), 1 + rng.nextInt(4)));
            }
            dispatcher.tick();
            if (t % 10 == 0) System.out.printf("tick=%d queued=%d active=%s avgWait=%.2f%n",
                    dispatcher.currentTick(), dispatcher.queued(), dispatcher.activeJobId().orElse("idle"), dispatcher.averageWait());
        }
    }
}
