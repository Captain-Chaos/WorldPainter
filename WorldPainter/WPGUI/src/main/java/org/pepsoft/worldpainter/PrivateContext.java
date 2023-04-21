package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.vo.UsageVO;

/**
 * Created by pepijn on 8-2-2015.
 */
public interface PrivateContext {
    /**
     * Perform any necessary initialisations. This is called on the main thread during the initialisation of the
     * {@link Main} class, before the GUI has been instantiated or initialised.
     */
    void init();

    /**
     * Initiate a check for updates. May show popups and exit the program to install a new version at its option. This
     * is called on the main thread during the initialisation of the {@link Main} class, before the GUI has been
     * instantiated or initialised.
     */
    void checkForUpdates();

    /**
     * Submit usage data to the server, in either a blocking fashion or in the background. Upon successful submission
     * the submitted events should be removed from the persisten event log. It is not specified on which thread this
     * will be called.
     *
     * @param usageData The usage data to submit.
     * @param blocking  When {@code true} the method should block until finished and throw a runtime exception in case
     *                  of error. When {@code false} the method should return immediately and perform the submission in
     *                  the background, logging an error in case of failure.
     */
    void submitUsageData(UsageVO usageData, boolean blocking);

    /**
     * Perform any desired modifications of the app. This is called on the event dispatch thread, when the {@link App}
     * has been constructed, but not yet been made visible.
     *
     * @param app The WorldPainter app to modify.
     */
    void install(App app);
}