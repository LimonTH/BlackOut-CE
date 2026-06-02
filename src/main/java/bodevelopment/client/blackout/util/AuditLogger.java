package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.Internal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Foundation for action audit logging (MISS-02).
 * <p>
 * Records high-risk actions with module attribution to a rotating log file.
 * Helps users determine which module triggered a ban.
 * <p>
 * <b>Current status:</b> Logger infrastructure created. Integration with
 * individual modules (AutoCrystal, Surround, etc.) is future work.
 * Call {@link #log(String, String)} from module action methods.
 */
@Internal
public class AuditLogger {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final File LOG_FILE =
            new File(BlackOut.RUN_DIRECTORY, "blackout/audit.log");

    private static boolean enabled = false;

    public static void enable() {
        enabled = true;
    }

    public static void disable() {
        enabled = false;
    }

    /**
     * Records an auditable action.
     *
     * @param module the module that performed the action
     * @param action description of what was done (e.g., "placed obsidian at 10,64,-5")
     */
    public static void log(String module, String action) {
        if (!enabled) return;
        try {
            LOG_FILE.getParentFile().mkdirs();
            try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_FILE, true))) {
                pw.printf("[%s] [%s] %s%n",
                        LocalDateTime.now().format(FORMATTER), module, action);
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * Records an outbound packet with module attribution.
     */
    public static void logPacket(String module, String packetType) {
        if (!enabled) return;
        log(module, "sent " + packetType);
    }

    /**
     * Returns the size of the log file in bytes, or -1 if unreadable.
     */
    public static long getLogSize() {
        return LOG_FILE.exists() ? LOG_FILE.length() : 0;
    }

    /**
     * Truncates the audit log, keeping only the last N bytes.
     */
    public static void rotate(long maxBytes) {
        if (LOG_FILE.exists() && LOG_FILE.length() > maxBytes) {
            File rotated = new File(LOG_FILE.getParentFile(), "audit.log.old");
            rotated.delete();
            LOG_FILE.renameTo(rotated);
        }
    }
}
