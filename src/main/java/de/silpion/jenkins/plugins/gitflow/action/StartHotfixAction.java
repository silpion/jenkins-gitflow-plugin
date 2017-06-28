package de.silpion.jenkins.plugins.gitflow.action;

import de.silpion.jenkins.plugins.gitflow.cause.StartHotfixCause;
import de.silpion.jenkins.plugins.gitflow.data.RemoteBranch;
import de.silpion.jenkins.plugins.gitflow.proxy.gitclient.GitClientProxy;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.IOException;

import static de.silpion.jenkins.plugins.gitflow.GitflowBuildWrapper.getGitflowBuildWrapperDescriptor;

/**
 * This class executes the required steps for the Gitflow action <i>Start Hotfix</i>.
 *
 * @param <B> the build in progress.
 * @author Hannes Osius, Silpion IT-Solutions GmbH - osius@silpion.de
 */
public class StartHotfixAction<B extends AbstractBuild<?, ?>> extends AbstractGitflowAction<B, StartHotfixCause> {

    private static final String ACTION_NAME = "Start Hotfix";

    private static final String MSG_PATTERN_CREATED_BRANCH_BASED_ON_OTHER = "Gitflow - %s: Created a new branch %s based on %s%n";
    private static final String MSG_PATTERN_UPDATED_NEXT_PATCH_DEVELOPMENT_VERSION = "Gitflow - %s: Updated project files to next patch development version %s%n";

    /**
     * Initialises a new <i>Start Hotfix</i> action.
     *
     * @param build the <i>Start Hotfix</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @param git the Git client used to execute commands for the Gitflow actions.
     * @param startHotfixCause the cause for the new action.
     * @param <BC> the build in progress.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public <BC extends B> StartHotfixAction(BC build, Launcher launcher, BuildListener listener, GitClientProxy git, StartHotfixCause startHotfixCause) throws IOException, InterruptedException {
        super(build, launcher, listener, git, startHotfixCause);
    }

    /** {@inheritDoc} */
    @Override
    public String getActionName() {
        return ACTION_NAME;
    }

    /** {@inheritDoc} */
    @Override
    protected void beforeMainBuildInternal() throws IOException, InterruptedException {

        // Create a new hotfix branch based on the master branch.
        final String hotfixBranch = this.gitflowCause.getHotfixBranch();
        final String masterBranch = getGitflowBuildWrapperDescriptor().getMasterBranch();
        this.git.checkoutBranch(hotfixBranch, "origin/" + masterBranch);
        this.consoleLogger.printf(MSG_PATTERN_CREATED_BRANCH_BASED_ON_OTHER, this.getActionName(), hotfixBranch, masterBranch);

        // Update the version numbers in the project files to the hotfix version.
        final String nextPatchDevelopmentVersion = this.gitflowCause.getNextPatchDevelopmentVersion();
        this.addFilesToGitStage(this.buildTypeAction.updateVersion(nextPatchDevelopmentVersion));
        final String msgUpadtedReleaseVersion = formatPattern(MSG_PATTERN_UPDATED_NEXT_PATCH_DEVELOPMENT_VERSION, ACTION_NAME, nextPatchDevelopmentVersion);
        this.git.commit(msgUpadtedReleaseVersion);
        this.consoleLogger.print(msgUpadtedReleaseVersion);

        // Push the new hotfix branch.
        this.git.push("origin", "refs/heads/" + hotfixBranch + ":refs/heads/" + hotfixBranch);

        // Record the information about the state of the new hotfix branch.
        final RemoteBranch remoteBranchRef = this.gitflowPluginData.getRemoteBranch(masterBranch);
        final RemoteBranch remoteBranchNew = this.gitflowPluginData.getOrAddRemoteBranch(hotfixBranch);
        remoteBranchNew.setLastBuildResult(remoteBranchRef.getLastBuildResult());
        remoteBranchNew.setLastBuildVersion(nextPatchDevelopmentVersion);
        remoteBranchNew.setBaseReleaseVersion(remoteBranchRef.getBaseReleaseVersion());
        remoteBranchNew.setLastReleaseVersion(remoteBranchRef.getLastReleaseVersion());
        remoteBranchNew.setLastReleaseVersionCommit(remoteBranchRef.getLastReleaseVersionCommit());

        // Add environment and property variables
        this.additionalBuildEnvVars.put("GIT_SIMPLE_BRANCH_NAME", hotfixBranch);
        this.additionalBuildEnvVars.put("GIT_REMOTE_BRANCH_NAME", "origin/" + hotfixBranch);
        this.additionalBuildEnvVars.put("GIT_BRANCH_TYPE", getGitflowBuildWrapperDescriptor().getBranchType(hotfixBranch));
    }

    /** {@inheritDoc} */
    @Override
    protected void afterMainBuildInternal() throws IOException, InterruptedException {
        // Nothing to do.
    }
}
