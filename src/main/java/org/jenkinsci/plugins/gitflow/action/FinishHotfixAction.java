package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;

import org.jenkinsci.plugins.gitflow.cause.FinishHotfixCause;
import org.jenkinsci.plugins.gitflow.gitclient.GitClientDelegate;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

/**
 * This class executes the required steps for the Gitflow action <i>Finish Hotfix</i>.
 *
 * @param <B> the build in progress.
 * @author Hannes Osius, Silpion IT-Solutions GmbH - osius@silpion.de
 */
public class FinishHotfixAction<B extends AbstractBuild<?, ?>> extends AbstractGitflowAction<B, FinishHotfixCause> {

    private static final String ACTION_NAME = "Finish Hotfix";

    /**
     * Initialises a new <i>Finish Hotfix</i> action.
     *
     * @param build the <i>Finish Hotfix</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @param git the Git client used to execute commands for the Gitflow actions.
     * @param gitflowCause the cause for the new action.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public <BC extends B> FinishHotfixAction(final BC build, final Launcher launcher, final BuildListener listener, final GitClientDelegate git, final FinishHotfixCause gitflowCause)
            throws IOException, InterruptedException {
        super(build, launcher, listener, git, gitflowCause);
    }

    @Override
    protected String getActionName() {
        return ACTION_NAME;
    }

    @Override
    protected void beforeMainBuildInternal() throws IOException, InterruptedException {

        // Finish Release: just delete the release branch.
        this.deleteBranch(this.gitflowCause.getHotfixBranch());

        // Abort the job, because there's no need to execute the main build.
        this.omitMainBuild();
    }

    @Override
    protected void afterMainBuildInternal() throws IOException, InterruptedException {
        // Nothing to do.
    }
}
