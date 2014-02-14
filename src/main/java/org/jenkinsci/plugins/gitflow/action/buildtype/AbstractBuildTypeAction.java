package org.jenkinsci.plugins.gitflow.action.buildtype;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

/**
 * Abstract base class for the different build-type-specific actions.
 *
 * @param <T> the build type
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public abstract class AbstractBuildTypeAction<T extends AbstractBuild> {

    protected final T build;
    protected final Launcher launcher;
    protected final BuildListener listener;
    protected final PrintStream consoleLogger;

    /**
     * Initialises a new build-type-specific action.
     *
     * @param build    the <i>Gitflow</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @throws IOException          if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    protected AbstractBuildTypeAction(final T build, final Launcher launcher, final BuildListener listener) {
        this.build = build;
        this.launcher = launcher;
        this.listener = listener;
        this.consoleLogger = listener.getLogger();
    }

    /**
     * Update the version numbers in the project files to the provided version.
     *
     * @param version the version to be set in the project files.
     * @return the files that were modified during the update.
     * @throws IOException          if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public abstract List<String> updateVersion(final String version) throws IOException, InterruptedException;
}
