package pl.tkowalcz.tjahzi.stats;

import com.google.common.util.concurrent.Uninterruptibles;

import java.util.concurrent.TimeUnit;

public class StatsDumpingThread extends Thread {

    private static final boolean LAUNCH_STATS_DUMP_THREAD = Boolean.getBoolean("tjahzi.monitoring.dumpStats.enabled");

    private static final int DEFAULT_LAUNCH_DUMP_INTERVAL_SECONDS = 60;
    private static final int LAUNCH_DUMP_INTERVAL_SECONDS = Integer.getInteger(
            "tjahzi.monitoring.dumpStats.intervalSeconds",
            DEFAULT_LAUNCH_DUMP_INTERVAL_SECONDS
    );

    private final MonitoringModule monitoringModule;

    public StatsDumpingThread(MonitoringModule monitoringModule) {
        super("tjahzi.monitoring.dumpStats.thread");
        setDaemon(true);

        this.monitoringModule = monitoringModule;
    }

    public boolean isEnabled() {
        return LAUNCH_STATS_DUMP_THREAD;
    }

    @Override
    public void run() {
        while (isEnabled()) {
            System.out.println(monitoringModule.toString());
            Uninterruptibles.sleepUninterruptibly(LAUNCH_DUMP_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
    }
}
