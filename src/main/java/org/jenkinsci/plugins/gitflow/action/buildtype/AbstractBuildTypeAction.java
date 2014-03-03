package org.jenkinsci.plugins.gitflow.action.buildtype;

import java.io.IOException;
import java.util.List;

import org.jenkinsci.plugins.gitflow.action.AbstractActionBase;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

/**
 * Abstract base class for the different build-type-specific actions.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public abstract class AbstractBuildTypeAction<T extends AbstractBuild<?, ?>> extends AbstractActionBase<T> {

    protected final Launcher launcher;

    /**
     * Initialises a new build-type-specific action.
     *
     * @param build the <i>Gitflow</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     */
    protected AbstractBuildTypeAction(final T build, final Launcher launcher, final BuildListener listener) {
        super(build, listener);
        this.launcher = launcher;
    }

    /**
     * Returns the current version number of the project files.
     *
     * @return the current version number of the project files.
     */
    public abstract String getCurrentVersion();

    /**
     * Update the version numbers in the project files to the provided version.
     *
     * @param version the version to be set in the project files.
     * @return the files that were modified during the update.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public abstract List<String> updateVersion(final String version) throws IOException, InterruptedException;
}
