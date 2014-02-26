package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;
import java.util.Set;

import org.jenkinsci.plugins.gitflow.GitflowPluginProperties;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.git.GitTagAction;

/**
 * This class executes some actions that are required when the <i>Gitflow</i> plugin is configured for a project and the standard (non-Gitflow) job is started.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class NoGitflowAction extends AbstractGitflowAction {

    public NoGitflowAction(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
        super(build, launcher, listener);
    }

    @Override
    public void beforeMainBuild() throws IOException, InterruptedException {
        // Nothing to do.
    }

    @Override
    public void afterMainBuild() throws IOException, InterruptedException {
        this.recordGitflowBranchVersions();
    }

    private void recordGitflowBranchVersions() throws IOException {
        final String version = this.buildTypeAction.getCurrentVersion();
        final Set<String> branches = this.build.getAction(GitTagAction.class).getTags().keySet();
        new GitflowPluginProperties(this.build.getProject()).saveVersionForBranches(branches, version);
    }
}
