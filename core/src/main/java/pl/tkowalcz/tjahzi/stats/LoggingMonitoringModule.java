package pl.tkowalcz.tjahzi.stats;

import java.util.function.Consumer;

/**
 * A MonitoringModule implementation that logs internal errors and connection errors
 * to a provided logger consumer in addition to the standard monitoring functionality.
 */
public class LoggingMonitoringModule extends StandardMonitoringModule {

    private final Consumer<String> logger;

    public LoggingMonitoringModule(Consumer<String> logger) {
        this.logger = logger;
    }

    @Override
    public void addAgentError(Throwable throwable) {
        super.addAgentError(throwable);
        logger.accept("[Tjahzi] Internal error occurred: " + throwable.getMessage() + ".");
    }

    @Override
    public void addPipelineError(Throwable cause) {
        super.addPipelineError(cause);
        logger.accept("[Tjahzi] Pipeline error occurred: " + cause.getMessage() + ".");
    }

    @Override
    public void incrementDroppedPuts(Throwable throwable) {
        super.incrementDroppedPuts(throwable);
        logger.accept("[Tjahzi] Dropped log entries due to error: " + throwable.getMessage() + ".");
    }

    @Override
    public void incrementHttpErrors(int status, String message) {
        super.incrementHttpErrors(status, message);
        logger.accept("[Tjahzi] HTTP error occurred - status: " + status + ", message: " + message + ".");
    }

    @Override
    public void incrementFailedHttpRequests() {
        super.incrementFailedHttpRequests();
        logger.accept("[Tjahzi] HTTP request failed.");
    }

    @Override
    public void incrementChannelInactive() {
        super.incrementChannelInactive();
        logger.accept("[Tjahzi] Connection channel became inactive.");
    }

    @Override
    public void onClose() {
        super.onClose();
        logger.accept("[Tjahzi] Appender is being closed.");
    }
}
