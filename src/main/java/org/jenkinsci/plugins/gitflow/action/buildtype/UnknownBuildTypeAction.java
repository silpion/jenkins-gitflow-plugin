package org.jenkinsci.plugins.gitflow.action.buildtype;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

/**
 * This class implements the different actions, that are required to apply the <i>Gitflow</i> to projects of unknown of unsupported build types - which in most
 * cases might mean executing no operations.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class UnknownBuildTypeAction extends AbstractBuildTypeAction<AbstractBuild> {

    /**
     * Initialises a new action for an unknown build type.
     *
     * @param build    the <i>Gitflow</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @throws IOException          if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public UnknownBuildTypeAction(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {
        super(build, launcher, listener);
    }

    @Override
    public List<String> updateVersion(final String version) throws IOException, InterruptedException {
        this.consoleLogger.println("[WARNING] Unsupported project type. Cannot change release number in project files.");
        return Collections.emptyList();
    }
}
