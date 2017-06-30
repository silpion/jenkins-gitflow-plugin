package de.silpion.jenkins.plugins.gitflow.action;

import de.silpion.jenkins.plugins.gitflow.cause.NoGitflowCause;
import de.silpion.jenkins.plugins.gitflow.data.RemoteBranch;
import de.silpion.jenkins.plugins.gitflow.proxy.gitclient.GitClientProxy;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.List;

import static de.silpion.jenkins.plugins.gitflow.GitflowBuildWrapper.getGitflowBuildWrapperDescriptor;

/**
 * This class executes some actions that are required when the <i>Gitflow</i> plugin is configured for a project and the standard (non-Gitflow) job is started.
 *
 * @param <B> the build in progress.
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class NoGitflowAction<B extends AbstractBuild<?, ?>> extends AbstractGitflowAction<B, NoGitflowCause> {

    private static final String ACTION_NAME = "default build";

    /**
     * Initialises a new action for a non-Gitflow build.
     *
     * @param build the <i>Publish Release</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @param git the Git client used to execute commands for the Gitflow actions.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public <BC extends B> NoGitflowAction(final BC build, final Launcher launcher, final BuildListener listener, final GitClientProxy git) throws IOException, InterruptedException {
        super(build, launcher, listener, git, new NoGitflowCause());
    }

    @Override
    public String getActionName() {
        return ACTION_NAME;
    }

    @Override
    protected void cleanCheckout() throws InterruptedException {
        // Override without actually cleaning up, because standard builds should follow the cleanup configuration of the Git plugin.
    }

    @Override
    protected void beforeMainBuildInternal() throws IOException, InterruptedException {

        // Add environment and property variables
        final String remoteBranchName = this.getBranchesForCurrentlyBuiltCommit().iterator().next();
        final String simpleBranchName = StringUtils.split(remoteBranchName, "/", 2)[1];
        this.additionalBuildEnvVars.put("GIT_SIMPLE_BRANCH_NAME", simpleBranchName);
        this.additionalBuildEnvVars.put("GIT_REMOTE_BRANCH_NAME", remoteBranchName);
        this.additionalBuildEnvVars.put("GIT_BRANCH_TYPE", getGitflowBuildWrapperDescriptor().getBranchType(simpleBranchName));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void afterMainBuildInternal() throws IOException, InterruptedException {

        // Record the data about the Gitflow branches that have been built.
        for (final String builtBranch : this.getBranchesForCurrentlyBuiltCommit()) {
            final String builtBranchName = StringUtils.split(builtBranch, "/", 2)[1];

            final RemoteBranch remoteBranch = this.gitflowPluginData.getOrAddRemoteBranch(builtBranchName);
            remoteBranch.setLastBuildResult(this.getBuildResultNonNull());
            remoteBranch.setLastBuildVersion(this.buildTypeAction.getCurrentVersion());
        }
    }

    private List<String> getBranchesForCurrentlyBuiltCommit() throws IOException, InterruptedException {
        final String gitCommit = this.build.getEnvironment(this.listener).get("GIT_COMMIT");
        return this.git.getRemoteBranchNamesContaining(gitCommit);
    }
}
