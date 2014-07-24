package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gitflow.cause.AbstractGitflowCause;
import org.jenkinsci.plugins.gitflow.cause.NoGitflowCause;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.git.GitTagAction;

/**
 * This class executes some actions that are required when the <i>Gitflow</i> plugin is configured for a project and the standard (non-Gitflow) job is started.
 *
 * @param <B> the build in progress.
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class NoGitflowAction<B extends AbstractBuild<?, ?>> extends AbstractGitflowAction<B, AbstractGitflowCause> {

    private static final String ACTION_NAME = "";
    private static final String CONSOLE_MESSAGE_PREFIX = "Gitflow - " + ACTION_NAME + ": ";

    public <BC extends B> NoGitflowAction(final BC build, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
        super(build, launcher, listener, new NoGitflowCause(), ACTION_NAME);
    }

    @Override
    protected String getConsoleMessagePrefix() {
        return CONSOLE_MESSAGE_PREFIX;
    }

    @Override
    protected String getActionName() {
        return ACTION_NAME;
    }

    @Override
    protected void cleanCheckout() throws InterruptedException {
        // Override without actually cleaning up, because standard builds should folllow the cleanup configuration of the Git plugin.
    }

    @Override
    protected void beforeMainBuildInternal() throws IOException, InterruptedException {
        // Nothing to do.
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void afterMainBuildInternal() throws IOException, InterruptedException {

        // Record the data about the Gitflow branches that have been built.
        final Collection<String> remoteBranchNames = this.build.getAction(GitTagAction.class).getTags().keySet();
        for (final String remoteBranchName : remoteBranchNames) {
            final String[] remoteBranchNameTokens = StringUtils.split(remoteBranchName, "/", 2);

            final RemoteBranch remoteBranch = this.gitflowPluginData.getOrAddRemoteBranch(remoteBranchNameTokens[0], remoteBranchNameTokens[1]);
            remoteBranch.setLastBuildResult(this.build.getResult());
            remoteBranch.setLastBuildVersion(this.buildTypeAction.getCurrentVersion());
        }
    }
}
