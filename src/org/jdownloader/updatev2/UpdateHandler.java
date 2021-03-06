package org.jdownloader.updatev2;

import java.awt.Window;
import java.io.IOException;
import java.util.List;

import org.appwork.utils.logging2.LogSource;

public interface UpdateHandler {
    void startIntervalChecker();

    void runUpdateCheck(boolean manually);

    void setGuiVisible(boolean b, boolean toFront);

    boolean isGuiVisible();

    String getAppID();

    // List<File> getFileList(File awfFile) throws InterruptedException, IOException;
    Window getGuiFrame();

    void installPendingUpdates(InstallLog log);

    boolean hasPendingUpdates();

    boolean hasPendingClientUpdates();

    boolean hasPendingSelfupdate();

    InstallLog createAWFInstallLog() throws InterruptedException, IOException;

    void setGuiFinished(String message);

    boolean isExtensionInstalled(String id);

    void uninstallExtension(String... ids) throws InterruptedException;

    void installExtension(String... ids) throws InterruptedException;

    void waitForUpdate() throws InterruptedException;

    List<String> getRequestedInstalls();

    List<String> getRequestedUnInstalls();

    void stopIntervalChecker();

    LogSource getLogger();

    List<InstallLog> clearInstallLogs();

    void requestFullExtensionUpdate(String... parameters);

    void setGuiAlwaysOnTop(boolean enabled);
}
