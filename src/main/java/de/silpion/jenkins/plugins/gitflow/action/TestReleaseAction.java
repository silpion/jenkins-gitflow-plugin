package de.silpion.jenkins.plugins.gitflow.action;

import de.silpion.jenkins.plugins.gitflow.cause.TestReleaseCause;
import de.silpion.jenkins.plugins.gitflow.data.RemoteBranch;
import de.silpion.jenkins.plugins.gitflow.proxy.gitclient.GitClientProxy;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import org.eclipse.jgit.lib.ObjectId;

import java.io.IOException;

import static de.silpion.jenkins.plugins.gitflow.GitflowBuildWrapper.getGitflowBuildWrapperDescriptor;

/**
 * This class executes the required steps for the Gitflow action <i>Test Release</i>.
 *
 * @param <B> the build in progress.
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class TestReleaseAction<B extends AbstractBuild<?, ?>> extends AbstractGitflowAction<B, TestReleaseCause> {

    private static final String ACTION_NAME = "Test Release";

    private static final String MSG_PATTERN_CHECKED_OUT_RELEASE_BRANCH = "Gitflow - %s: Checked out release branch %s%n";
    private static final String MSG_PATTERN_UPDATED_PATCH_RELEASE_VERSION = "Gitflow - %s: Updated project files to patch release version %s%n";
    private static final String MSG_PATTERN_CREATED_RELEASE_TAG = "Gitflow - %s: Created release version tag %s%n";
    private static final String MSG_PATTERN_UPDATED_NEXT_PATCH_DEVELOPMENT_VERSION = "Gitflow - %s: Updated project files to next patch development version %s%n";

    /**
     * Initialises a new <i>Publish Release</i> action.
     *
     * @param build the <i>Publish Release</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @param git the Git client used to execute commands for the Gitflow actions.
     * @param gitflowCause the cause for the new action.
     * @param <BC> the build in progress.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public <BC extends B> TestReleaseAction(final BC build, final Launcher launcher, final BuildListener listener, final GitClientProxy git, final TestReleaseCause gitflowCause)
            throws IOException, InterruptedException {
        super(build, launcher, listener, git, gitflowCause);
    }

    @Override
    public String getActionName() {
        return ACTION_NAME;
    }

    @Override
    protected void beforeMainBuildInternal() throws IOException, InterruptedException {

        // Checkout the release Branch
        final String releaseBranch = this.gitflowCause.getReleaseBranch();
        final ObjectId releaseBranchRev = this.git.getHeadRev(releaseBranch);
        this.git.checkoutBranch(releaseBranch, releaseBranchRev.getName());
        this.consoleLogger.printf(MSG_PATTERN_CHECKED_OUT_RELEASE_BRANCH, ACTION_NAME, releaseBranch);

        // Update the project files to the minor release number
        final String patchReleaseVersion = this.gitflowCause.getPatchReleaseVersion();
        this.addFilesToGitStage(this.buildTypeAction.updateVersion(patchReleaseVersion));
        final String msgUpdatedReleaseVersion = formatPattern(MSG_PATTERN_UPDATED_PATCH_RELEASE_VERSION, ACTION_NAME, patchReleaseVersion);
        this.git.commit(msgUpdatedReleaseVersion);
        this.consoleLogger.print(msgUpdatedReleaseVersion);

        // Tell the main build that it will perform a release build.
        this.buildTypeAction.prepareForReleaseBuild();

        // Add environment and property variables
        this.additionalBuildEnvVars.put("GIT_SIMPLE_BRANCH_NAME", releaseBranch);
        this.additionalBuildEnvVars.put("GIT_REMOTE_BRANCH_NAME", "origin/" + releaseBranch);
        this.additionalBuildEnvVars.put("GIT_BRANCH_TYPE", getGitflowBuildWrapperDescriptor().getBranchType(releaseBranch));
    }

    @Override
    protected void afterMainBuildInternal() throws IOException, InterruptedException {
        if (this.build.getResult() == Result.SUCCESS) {
            this.afterSuccessfulMainBuild();
        } else {
            this.afterUnsuccessfulMainBuild();
        }
    }

    private void afterSuccessfulMainBuild() throws IOException, InterruptedException {

        // Create a tag for the release version.
        final String patchReleaseVersion = this.gitflowCause.getPatchReleaseVersion();
        final String tagName = getGitflowBuildWrapperDescriptor().getVersionTagPrefix() + patchReleaseVersion;
        final String msgCreatedReleaseTag = formatPattern(MSG_PATTERN_CREATED_RELEASE_TAG, ACTION_NAME, tagName);
        this.git.tag(tagName, msgCreatedReleaseTag);
        this.consoleLogger.print(msgCreatedReleaseTag);

        // Update and commit the project files to the minor version for the next release
        final String nextPatchDevelopmentVersion = this.gitflowCause.getNextPatchDevelopmentVersion();
        this.addFilesToGitStage(this.buildTypeAction.updateVersion(nextPatchDevelopmentVersion));
        final String msgUpdatedFixesVersion = formatPattern(MSG_PATTERN_UPDATED_NEXT_PATCH_DEVELOPMENT_VERSION, ACTION_NAME, nextPatchDevelopmentVersion);
        this.git.commit(msgUpdatedFixesVersion);
        this.consoleLogger.print(msgUpdatedFixesVersion);

        // Push everything - the release branch and its commits and the new tag.
        final String releaseBranch = this.gitflowCause.getReleaseBranch();
        this.git.push("origin", "refs/tags/" + tagName + ":refs/tags/" + tagName);
        this.git.push("origin", "refs/heads/" + releaseBranch + ":refs/heads/" + releaseBranch);

        // Record the information about the state of the release branch.
        final RemoteBranch remoteBranchRelease = this.gitflowPluginData.getRemoteBranch(releaseBranch);
        remoteBranchRelease.setLastBuildResult(Result.SUCCESS);
        remoteBranchRelease.setLastBuildVersion(nextPatchDevelopmentVersion);
        remoteBranchRelease.setLastReleaseVersion(patchReleaseVersion);
        remoteBranchRelease.setLastReleaseVersionCommit(this.git.revParse(tagName));
    }

    private void afterUnsuccessfulMainBuild() {

        // Here we assume that there was an error on the release branch right before exetuted this action.
        final RemoteBranch remoteBranchRelease = this.gitflowPluginData.getRemoteBranch(this.gitflowCause.getReleaseBranch());
        remoteBranchRelease.setLastBuildResult(this.getBuildResultNonNull());
        remoteBranchRelease.setLastBuildVersion(remoteBranchRelease.getLastBuildVersion());
    }
}
