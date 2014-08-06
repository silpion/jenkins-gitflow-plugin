package org.jenkinsci.plugins.gitflow.action;

import static hudson.model.Result.SUCCESS;
import static org.jenkinsci.plugins.gitflow.GitflowBuildWrapper.getGitflowBuildWrapperDescriptor;

import java.io.IOException;

import org.jenkinsci.plugins.gitflow.cause.StartHotFixCause;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;
import org.jenkinsci.plugins.gitflow.gitclient.GitClientDelegate;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

/**
 * This class executes the required steps for the Gitflow action <i>Start Hotfix</i>.
 *
 * @param <B> the build in progress.
 * @author Hannes Osius, Silpion IT-Solutions GmbH - osius@silpion.de
 */
public class StartHotFixAction<B extends AbstractBuild<?, ?>> extends AbstractGitflowAction<B, StartHotFixCause> {

    private static final String ACTION_NAME = "Start HotFix";

    private static final String MSG_PATTERN_UPDATED_HOTFIX_VERSION = "Gitflow - %s: Updated project files to hotfix version %s%n";

    /**
     * Initialises a new <i>Start Hotfix</i> action.
     *
     * @param build the <i>Start Hotfix</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @param git the Git client used to execute commands for the Gitflow actions.
     * @param startHotFixCause the cause for the new action.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public <BC extends B> StartHotFixAction(BC build, Launcher launcher, BuildListener listener, GitClientDelegate git, StartHotFixCause startHotFixCause) throws IOException, InterruptedException {
        super(build, launcher, listener, git, startHotFixCause);
    }

    /** {@inheritDoc} */
    @Override
    protected String getActionName() {
        return ACTION_NAME;
    }

    /** {@inheritDoc} */
    @Override
    protected void beforeMainBuildInternal() throws IOException, InterruptedException {

        // Create a new hotfix branch based on the master branch.
        final String hotfixBranch = getGitflowBuildWrapperDescriptor().getHotfixBranchPrefix() + this.gitflowCause.getHotfixReleaseVersion();
        final String masterBranch = getGitflowBuildWrapperDescriptor().getMasterBranch();
        this.createBranch(hotfixBranch, masterBranch);

        // Update the version numbers in the project files to the hotfix version.
        final String nextHotfixDevelopmentVersion = this.gitflowCause.getNextHotfixDevelopmentVersion();
        this.addFilesToGitStage(this.buildTypeAction.updateVersion(nextHotfixDevelopmentVersion));
        final String msgUpadtedReleaseVersion = formatPattern(MSG_PATTERN_UPDATED_HOTFIX_VERSION, ACTION_NAME, nextHotfixDevelopmentVersion);
        this.git.commit(msgUpadtedReleaseVersion);
        this.consoleLogger.print(msgUpadtedReleaseVersion);

        // Push the new hotfix branch to the remote repo.
        this.git.push().to(this.remoteUrl).ref("refs/heads/" + hotfixBranch + ":refs/heads/" + hotfixBranch).execute();

        // Record the remote branch data.
        final RemoteBranch remoteBranch = this.gitflowPluginData.getOrAddRemoteBranch("origin", hotfixBranch);
        remoteBranch.setLastBuildResult(SUCCESS);
        remoteBranch.setLastBuildVersion(nextHotfixDevelopmentVersion);

        // Abort the job, because there's no need to execute the main build.
        this.omitMainBuild();
    }

    /** {@inheritDoc} */
    @Override
    protected void afterMainBuildInternal() throws IOException, InterruptedException {
        // Nothing to do.
    }
}
