package ua.edu.digitaldean.service;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
public final class DiagnosticLog {
    private DiagnosticLog() {
    }
    public static Logger create() {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.INFO);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.INFO);
        handler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                Throwable failure = record.getThrown();
                String details = "";
                if (failure != null) {
                    Throwable cause = failure.getCause();
                    details = " | " + failure.getClass().getSimpleName()
                            + " | причина: " + (cause == null
                            ? "НЕМАЄ" : cause.getClass().getSimpleName())
                            + " | пригнічених: " + failure.getSuppressed().length;
                }
                return record.getLevel().getName() + " | "
                        + formatMessage(record) + details + System.lineSeparator();
            }
        });
        logger.addHandler(handler);
        return logger;
    }
}
