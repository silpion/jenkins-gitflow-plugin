package org.jenkinsci.plugins.gitflow.action;

import static org.jenkinsci.plugins.gitflow.GitflowBuildWrapper.getGitflowBuildWrapperDescriptor;

import java.io.IOException;

import org.jenkinsci.plugins.gitflow.GitflowBuildWrapper;
import org.jenkinsci.plugins.gitflow.cause.StartReleaseCause;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;
import org.jenkinsci.plugins.gitflow.proxy.gitclient.GitClientProxy;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;

/**
 * This class executes the required steps for the Gitflow action <i>Start Release</i>.
 *
 * @param <B> the build in progress.
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class StartReleaseAction<B extends AbstractBuild<?, ?>> extends AbstractGitflowAction<B, StartReleaseCause> {

    private static final String ACTION_NAME = "Start Release";

    private static final String MSG_PATTERN_CREATED_RELEASE_BRANCH = "Gitflow - %s: Created release branch %s%n";
    private static final String MSG_PATTERN_UPDATED_RELEASE_VERSION = "Gitflow - %s: Updated project files to release version %s%n";
    private static final String MSG_PATTERN_CREATED_RELEASE_TAG = "Gitflow - %s: Created release version tag %s%n";
    private static final String MSG_PATTERN_UPDATED_NEXT_PATCH_DEVELOPMENT_VERSION = "Gitflow - %s: Updated project files to next patch development version %s%n";
    private static final String MSG_PATTERN_UPDATED_NEXT_RELEASE_DEVELOPMENT_VERSION = "Gitflow - %s: Updated project files on %s branch to next release development version %s%n";

    /**
     * Initialises a new <i>Start Release</i> action.
     *
     * @param build the <i>Start Release</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @param git the Git client used to execute commands for the Gitflow actions.
     * @param gitflowCause the cause for the new action.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public <BC extends B> StartReleaseAction(final BC build, final Launcher launcher, final BuildListener listener, final GitClientProxy git, final StartReleaseCause gitflowCause)
            throws IOException, InterruptedException {
        super(build, launcher, listener, git, gitflowCause);
    }

    @Override
    protected String getActionName() {
        return ACTION_NAME;
    }

    @Override
    protected void beforeMainBuildInternal() throws IOException, InterruptedException {

        // Create a new release branch based on the develop branch.
        final GitflowBuildWrapper.DescriptorImpl buildWrapperDescriptor = getGitflowBuildWrapperDescriptor();
        final String releaseVersion = this.gitflowCause.getReleaseVersion();
        final String releaseBranch = this.gitflowCause.getReleaseBranch();
        this.git.checkoutBranch(releaseBranch, "origin/" + buildWrapperDescriptor.getDevelopBranch());
        this.consoleLogger.printf(MSG_PATTERN_CREATED_RELEASE_BRANCH, ACTION_NAME, releaseBranch);

        // Update the version numbers in the project files to the release version.
        this.addFilesToGitStage(this.buildTypeAction.updateVersion(releaseVersion));
        final String msgUpadtedReleaseVersion = formatPattern(MSG_PATTERN_UPDATED_RELEASE_VERSION, ACTION_NAME, releaseVersion);
        this.git.commit(msgUpadtedReleaseVersion);
        this.consoleLogger.print(msgUpadtedReleaseVersion);

        // Tell the main build that it will perform a release build.
        this.buildTypeAction.prepareForReleaseBuild();

        // Add environment and property variables
        this.additionalBuildEnvVars.put("GIT_SIMPLE_BRANCH_NAME", releaseBranch);
        this.additionalBuildEnvVars.put("GIT_REMOTE_BRANCH_NAME", "origin/" + releaseBranch);
        this.additionalBuildEnvVars.put("GIT_BRANCH_TYPE", buildWrapperDescriptor.getBranchType(releaseBranch));
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
        final String releaseVersion = this.gitflowCause.getReleaseVersion();
        final GitflowBuildWrapper.DescriptorImpl buildWrapperDescriptor = getGitflowBuildWrapperDescriptor();
        final String tagName = buildWrapperDescriptor.getVersionTagPrefix() + releaseVersion;
        final String msgCreatedReleaseTag = formatPattern(MSG_PATTERN_CREATED_RELEASE_TAG, ACTION_NAME, tagName);
        this.git.tag(tagName, msgCreatedReleaseTag);
        this.consoleLogger.print(msgCreatedReleaseTag);

        // Update the project files to the development version for the release fixes.
        final String nextPatchDevelopmentVersion = this.gitflowCause.getNextPatchDevelopmentVersion();
        this.addFilesToGitStage(this.buildTypeAction.updateVersion(nextPatchDevelopmentVersion));
        final String msgUpdatedFixesVersion = formatPattern(MSG_PATTERN_UPDATED_NEXT_PATCH_DEVELOPMENT_VERSION, ACTION_NAME, nextPatchDevelopmentVersion);
        this.git.commit(msgUpdatedFixesVersion);
        this.consoleLogger.print(msgUpdatedFixesVersion);

        // Update the project files in the develop branch to the development version for the next release.
        final String developBranch = buildWrapperDescriptor.getDevelopBranch();
        this.git.checkoutBranch(developBranch, "origin/" + developBranch);
        final String nextReleaseDevelopmentVersion = this.gitflowCause.getNextReleaseDevelopmentVersion();
        this.addFilesToGitStage(this.buildTypeAction.updateVersion(nextReleaseDevelopmentVersion));
        final String msgUpdatedNextVersion = formatPattern(MSG_PATTERN_UPDATED_NEXT_RELEASE_DEVELOPMENT_VERSION, ACTION_NAME, developBranch, nextReleaseDevelopmentVersion);
        this.git.commit(msgUpdatedNextVersion);
        this.consoleLogger.print(msgUpdatedNextVersion);

        // Push everything - the new release branch and its commits, the new tag and the commit on the develop branch.
        final String releaseBranch = this.gitflowCause.getReleaseBranch();
        this.git.push("origin", "refs/tags/" + tagName + ":refs/tags/" + tagName);
        this.git.push("origin", "refs/heads/" + releaseBranch + ":refs/heads/" + releaseBranch);
        this.git.push("origin", "refs/heads/" + developBranch + ":refs/heads/" + developBranch);

        // Record the information about the state of the release branch.
        final RemoteBranch remoteBranchRelease = this.gitflowPluginData.getOrAddRemoteBranch(releaseBranch);
        remoteBranchRelease.setLastBuildResult(Result.SUCCESS);
        remoteBranchRelease.setLastBuildVersion(nextPatchDevelopmentVersion);
        remoteBranchRelease.setBaseReleaseVersion(releaseVersion);
        remoteBranchRelease.setLastReleaseVersion(releaseVersion);
        remoteBranchRelease.setLastReleaseVersionCommit(this.git.getHeadRev(releaseBranch));

        // Record the information about the state of the develop branch.
        final RemoteBranch remoteBranchDevelop = this.gitflowPluginData.getOrAddRemoteBranch(developBranch);
        remoteBranchDevelop.setLastBuildResult(Result.SUCCESS);
        remoteBranchDevelop.setLastBuildVersion(nextReleaseDevelopmentVersion);

        // TODO Might configure further branches to merge to.
    }

    private void afterUnsuccessfulMainBuild() {

        // Here we assume that there was an error on the develop branch right before we created the release branch.
        // TODO We should not offer the Start Release action when no record for the develop branch exists - the method 'getOrAddRemoteBranch' can be used then.
        final RemoteBranch remoteBranchDevelop = this.gitflowPluginData.getOrAddRemoteBranch(getGitflowBuildWrapperDescriptor().getDevelopBranch());
        remoteBranchDevelop.setLastBuildResult(this.getBuildResultNonNull());
        remoteBranchDevelop.setLastBuildVersion(remoteBranchDevelop.getLastBuildVersion());
    }
}
