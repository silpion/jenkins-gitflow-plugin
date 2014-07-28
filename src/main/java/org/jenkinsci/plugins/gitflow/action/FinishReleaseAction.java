package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gitflow.GitflowBuildWrapper;
import org.jenkinsci.plugins.gitflow.cause.FinishReleaseCause;
import org.jenkinsci.plugins.gitflow.gitclient.GitClientDelegate;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

/**
 * This class executes the required steps for the Gitflow action <i>Finish Release</i>.
 *
 * @param <B> the build in progress.
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class FinishReleaseAction<B extends AbstractBuild<?, ?>> extends AbstractGitflowAction<B, FinishReleaseCause> {

    private static final String ACTION_NAME = "Finish Release";

    /**
     * Initialises a new <i>Finish Release</i> action.
     *
     * @param build the <i>Finish Release</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @param git the Git client used to execute commands for the Gitflow actions.
     * @param gitflowCause the cause for the new action.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public <BC extends B> FinishReleaseAction(final BC build, final Launcher launcher, final BuildListener listener, final GitClientDelegate git, final FinishReleaseCause gitflowCause)
            throws IOException, InterruptedException {
        super(build, launcher, listener, git, gitflowCause);
    }

    /** {@inheritDoc} */
    @Override
    protected String getActionName() {
        return ACTION_NAME;
    }

    /** {@inheritDoc} */
    @Override
    protected void beforeMainBuildInternal() throws IOException, InterruptedException {

        final String releaseBranch = this.gitflowCause.getReleaseBranch();

        // Include Start Hotfix action.
        if (this.gitflowCause.isIncludeStartHotfixAction()) {
            final GitflowBuildWrapper.DescriptorImpl buildWrapperDescriptor = getBuildWrapperDescriptor();
            final String releaseBranchPrefix = buildWrapperDescriptor.getReleaseBranchPrefix();
            final String hotfixBranchPrefix = buildWrapperDescriptor.getHotfixBranchPrefix();
            final String hotfixBranch = hotfixBranchPrefix + StringUtils.removeStart(releaseBranch, releaseBranchPrefix);
            this.createBranch(hotfixBranch, releaseBranch);
        }

        // Finish Release: just delete the release branch.
        this.deleteBranch(releaseBranch);

        // Abort the job, because there's no need to execute the main build.
        this.omitMainBuild();
    }

    /** {@inheritDoc} */
    @Override
    protected void afterMainBuildInternal() throws IOException, InterruptedException {
        // Nothing to do.
    }
}
