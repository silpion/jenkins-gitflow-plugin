package de.silpion.jenkins.plugins.gitflow.action;

import static de.silpion.jenkins.plugins.gitflow.GitflowBuildWrapper.getGitflowBuildWrapperDescriptor;

import java.io.IOException;

import de.silpion.jenkins.plugins.gitflow.cause.TestHotfixCause;
import de.silpion.jenkins.plugins.gitflow.data.RemoteBranch;
import org.eclipse.jgit.lib.ObjectId;
import de.silpion.jenkins.plugins.gitflow.proxy.gitclient.GitClientProxy;

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
    private static final String MSG_PATTERN_UPDATED_PATCH_RELEASE_VERSION = "Gitflow - %s: Updated project files to patch release version %s%n";
    private static final String MSG_PATTERN_CREATED_HOTFIX_TAG = "Gitflow - %s: Created hotfix version tag %s%n";
    private static final String MSG_PATTERN_UPDATED_NEXT_PATCH_DEVELOPMENT_VERSION = "Gitflow - %s: Updated project files to next patch development version %s%n";

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
    public <BC extends B> TestHotfixAction(final BC build, final Launcher launcher, final BuildListener listener, final GitClientProxy git, final TestHotfixCause gitflowCause)
            throws IOException, InterruptedException {
        super(build, launcher, listener, git, gitflowCause);
    }

    @Override
    public String getActionName() {
        return ACTION_NAME;
    }

    @Override
    protected void beforeMainBuildInternal() throws IOException, InterruptedException {

        // Checkout the hotfix Branch
        String hotfixBranch = gitflowCause.getHotfixBranch();
        final ObjectId hotfixBranchRev = git.getHeadRev(hotfixBranch);
        this.git.checkoutBranch(hotfixBranch, hotfixBranchRev.getName());
        this.consoleLogger.printf(MSG_PATTERN_CHECKOUT_HOTFIX_BRANCH, ACTION_NAME, hotfixBranch);

        // Update the project files to the minor release number
        final String patchReleaseVersion = this.gitflowCause.getPatchReleaseVersion();
        addFilesToGitStage(buildTypeAction.updateVersion(patchReleaseVersion));
        final String msgUpdatedReleaseVersion = formatPattern(MSG_PATTERN_UPDATED_PATCH_RELEASE_VERSION, ACTION_NAME, patchReleaseVersion);
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

        // Create a tag for the release version.
        final String patchReleaseVersion = this.gitflowCause.getPatchReleaseVersion();
        final String tagName = getGitflowBuildWrapperDescriptor().getVersionTagPrefix() + patchReleaseVersion;
        final String msgCreatedReleaseTag = formatPattern(MSG_PATTERN_CREATED_HOTFIX_TAG, ACTION_NAME, tagName);
        this.git.tag(tagName, msgCreatedReleaseTag);
        this.consoleLogger.print(msgCreatedReleaseTag);

        // Update and commit the project files to the next version for the next hotfix
        final String nextPatchDevelopmentVersion = this.gitflowCause.getNextPatchDevelopmentVersion();
        addFilesToGitStage(buildTypeAction.updateVersion(nextPatchDevelopmentVersion));
        final String msgUpdatedFixesVersion = formatPattern(MSG_PATTERN_UPDATED_NEXT_PATCH_DEVELOPMENT_VERSION, ACTION_NAME, nextPatchDevelopmentVersion);
        git.commit(msgUpdatedFixesVersion);
        this.consoleLogger.print(msgUpdatedFixesVersion);

        // Push everything - the hotfix branch and its commits and the new tag.
        String hotfixBranch = this.gitflowCause.getHotfixBranch();
        this.git.push("origin", "refs/tags/" + tagName + ":refs/tags/" + tagName);
        this.git.push("origin", "refs/heads/" + hotfixBranch + ":refs/heads/" + hotfixBranch);

        // Record the information about the state of the hotfix branch.
        final RemoteBranch remoteBranchHotfix = this.gitflowPluginData.getRemoteBranch(hotfixBranch);
        remoteBranchHotfix.setLastBuildResult(Result.SUCCESS);
        remoteBranchHotfix.setLastBuildVersion(nextPatchDevelopmentVersion);
        remoteBranchHotfix.setLastReleaseVersion(patchReleaseVersion);
        remoteBranchHotfix.setLastReleaseVersionCommit(this.git.revParse(tagName));
    }

    private void afterUnsuccessfulMainBuild() {

        // Here we assume that there was an error on the hotfix branch right before executed this action.
        String hotfixBranch = gitflowCause.getHotfixBranch();
        RemoteBranch remoteBranch = gitflowPluginData.getRemoteBranch(hotfixBranch);
        remoteBranch.setLastBuildResult(this.getBuildResultNonNull());
    }
}
