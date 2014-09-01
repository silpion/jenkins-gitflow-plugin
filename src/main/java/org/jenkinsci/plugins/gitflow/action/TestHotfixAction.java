package org.jenkinsci.plugins.gitflow.action;

import static org.jenkinsci.plugins.gitflow.GitflowBuildWrapper.getGitflowBuildWrapperDescriptor;

import java.io.IOException;

import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.gitflow.cause.TestHotfixCause;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;
import org.jenkinsci.plugins.gitflow.gitclient.GitClientDelegate;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;

/**
 * This class executes the required steps for the Gitflow action <i>Test Hotfix</i>.
 *
 * @param <B> the build in progress.
 * @author Hannes Osius, Silpion IT-Solutions GmbH - osius@silpion.de
 */
public class TestHotfixAction<B extends AbstractBuild<?, ?>> extends AbstractGitflowAction<B, TestHotfixCause> {

    private static final String ACTION_NAME = "Test Hotfix";

    private static final String MSG_PATTERN_CHECKOUT_HOTFIX_BRANCH = "Gitflow - %s: Checkout hotfix branch %s%n";
    private static final String MSG_PATTERN_UPDATED_HOTFIX_VERSION = "Gitflow - %s: Updated project files to Hotfix version %s%n";
    private static final String MSG_PATTERN_CREATED_HOTFIX_TAG = "Gitflow - %s: Created hotfix version tag %s%n";
    private static final String MSG_PATTERN_UPDATED_NEXT_HOTFIX_VERSION = "Gitflow - %s: Updated project files to next hotfix version %s%n";

    /**
     * Initialises a new <i>Test Hotfix</i> action.
     *
     * @param build the <i>Test Hotfix</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @param git the Git client used to execute commands for the Gitflow actions.
     * @param gitflowCause the cause for the new action.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public <BC extends B> TestHotfixAction(final BC build, final Launcher launcher, final BuildListener listener, final GitClientDelegate git, final TestHotfixCause gitflowCause)
            throws IOException, InterruptedException {
        super(build, launcher, listener, git, gitflowCause);
    }

    @Override
    protected String getActionName() {
        return ACTION_NAME;
    }

    @Override
    protected void beforeMainBuildInternal() throws IOException, InterruptedException {

        // Checkout the hotfix Branch
        String hotfixBranch = gitflowCause.getHotfixBranch();
        ObjectId hotfixBranchRev = git.getHeadRev(git.getRemoteUrl("origin"), hotfixBranch);
        this.git.checkoutBranch(hotfixBranch, hotfixBranchRev.getName());
        this.consoleLogger.printf(MSG_PATTERN_CHECKOUT_HOTFIX_BRANCH, ACTION_NAME, hotfixBranch);

        // Update the project files to the minor release number
        String fixesReleaseVersion = gitflowCause.getHotfixReleaseVersion();
        addFilesToGitStage(buildTypeAction.updateVersion(fixesReleaseVersion));
        final String msgUpdatedReleaseVersion = formatPattern(MSG_PATTERN_UPDATED_HOTFIX_VERSION, ACTION_NAME, fixesReleaseVersion);
        git.commit(msgUpdatedReleaseVersion);
        this.consoleLogger.print(msgUpdatedReleaseVersion);

        // Tell the main build that it will perform a release build.
        this.buildTypeAction.prepareForReleaseBuild();

        // Add environment and property variables
        this.additionalBuildEnvVars.put("GIT_SIMPLE_BRANCH_NAME", hotfixBranch);
        this.additionalBuildEnvVars.put("GIT_REMOTE_BRANCH_NAME", "origin/" + hotfixBranch);
        this.additionalBuildEnvVars.put("GIT_BRANCH_TYPE", getGitflowBuildWrapperDescriptor().getBranchType(hotfixBranch));
    }

    @Override
    protected void afterMainBuildInternal() throws IOException, InterruptedException {
        if (build.getResult() == Result.SUCCESS) {
            afterSuccessfulMainBuild();
        } else {
            afterUnsuccessfulMainBuild();
        }
    }

    private void afterSuccessfulMainBuild() throws IOException, InterruptedException {

        // Push the new minor release version to the remote repo.
        String hotfixBranch = gitflowCause.getHotfixBranch();
        this.git.push().to(this.remoteUrl).ref("refs/heads/" + hotfixBranch + ":refs/heads/" + hotfixBranch).execute();

        // Record the information on the currently stable version on the release branch.
        final String hotfixReleaseVersion = this.gitflowCause.getHotfixReleaseVersion();
        final RemoteBranch remoteBranchHotfix = this.gitflowPluginData.getRemoteBranch("origin", hotfixBranch);
        remoteBranchHotfix.setLastBuildResult(Result.SUCCESS);
        remoteBranchHotfix.setLastBuildVersion(hotfixReleaseVersion);
        remoteBranchHotfix.setLastReleaseVersion(hotfixReleaseVersion);
        remoteBranchHotfix.setLastReleaseVersionCommit(this.git.getHeadRev(this.git.getRemoteUrl("origin"), hotfixBranch));

        // Create a tag for the release version.
        final String tagName = getGitflowBuildWrapperDescriptor().getVersionTagPrefix() + hotfixReleaseVersion;
        final String msgCreatedReleaseTag = formatPattern(MSG_PATTERN_CREATED_HOTFIX_TAG, ACTION_NAME, tagName);
        this.git.tag(tagName, msgCreatedReleaseTag);
        this.consoleLogger.print(msgCreatedReleaseTag);

        // Push the tag for the release version.
        this.git.push().to(this.remoteUrl).ref("refs/tags/" + tagName + ":refs/tags/" + tagName).execute();

        // Update and commit the project files to the next version for the next hotfix
        String nextHotfixVersion = this.gitflowCause.getNextHotfixDevelopmentVersion();
        addFilesToGitStage(buildTypeAction.updateVersion(nextHotfixVersion));
        final String msgUpdatedFixesVersion = formatPattern(MSG_PATTERN_UPDATED_NEXT_HOTFIX_VERSION, ACTION_NAME, nextHotfixVersion);
        git.commit(msgUpdatedFixesVersion);
        this.consoleLogger.print(msgUpdatedFixesVersion);

        // Push the project files with the next version for the next hotfix.
        this.git.push().to(this.remoteUrl).ref("refs/heads/" + hotfixBranch + ":refs/heads/" + hotfixBranch).execute();

        // Record the fixes development version on the release branch.
        remoteBranchHotfix.setLastBuildResult(Result.SUCCESS);
        remoteBranchHotfix.setLastBuildVersion(nextHotfixVersion);
    }

    private void afterUnsuccessfulMainBuild() {

        // Here we assume that there was an error on the hotfix branch right before executed this action.
        String hotfixBranch = gitflowCause.getHotfixBranch();
        RemoteBranch remoteBranch = gitflowPluginData.getRemoteBranch("origin", hotfixBranch);
        remoteBranch.setLastBuildResult(build.getResult());
    }
}
