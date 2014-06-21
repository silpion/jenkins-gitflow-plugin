package org.jenkinsci.plugins.gitflow.action.buildtype;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

/**
 * This class implements the different actions, that are required to apply the <i>Gitflow</i> to projects of unknown of unsupported build types - which in most
 * cases might mean executing no operations.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class UnknownBuildTypeAction extends AbstractBuildTypeAction<AbstractBuild<?, ?>> {

    private static final String MSG_CANNOT_CHANGE_VERSION = "[WARNING] Gitflow: Unsupported project type. Cannot change version number in project files.";

    /**
     * Initialises a new action for an unknown build type.
     *
     * @param build the <i>Gitflow</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     */
    public UnknownBuildTypeAction(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) {
        super(build, launcher, listener);
    }

    @Override
    public String getCurrentVersion() {
        return "unknown";
    }

    @Override
    public List<String> updateVersion(final String version) throws IOException, InterruptedException {
        this.consoleLogger.println(MSG_CANNOT_CHANGE_VERSION);
        return Collections.emptyList();
    }

    @Override
    public void preventArchivePublication(final Map<String, String> buildEnvVars) {
        // Nothing to do.
    }
}
