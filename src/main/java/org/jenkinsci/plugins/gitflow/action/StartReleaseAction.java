package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;
import java.text.MessageFormat;

import org.jenkinsci.plugins.gitflow.cause.StartReleaseCause;

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

    private static final String MSG_PREFIX = "Gitflow - Start Release: ";

    private static final MessageFormat MSG_PATTERN_CREATED_RELEASE_BRANCH = new MessageFormat(MSG_PREFIX + "Created release branch {0}");
    private static final MessageFormat MSG_PATTERN_UPDATED_RELEASE_VERSION = new MessageFormat(MSG_PREFIX + "Updated project files to release version {0}");
    private static final MessageFormat MSG_PATTERN_PUSHED_RELEASE_BRANCH = new MessageFormat(MSG_PREFIX + "Pushed release branch {0}");
    private static final MessageFormat MSG_PATTERN_CREATED_RELEASE_TAG = new MessageFormat(MSG_PREFIX + "Created release version tag {0}");
    private static final MessageFormat MSG_PATTERN_PUSHED_RELEASE_TAG = new MessageFormat(MSG_PREFIX + "Pushed release tag {0}");
    private static final MessageFormat MSG_PATTERN_UPDATED_FIXES_VERSION = new MessageFormat(MSG_PREFIX
                                                                                             + "Updated project files to fixes development version {0}");
    private static final MessageFormat MSG_PATTERN_PUSHED_FIXES_VERSION = new MessageFormat(MSG_PREFIX
                                                                                            + "Pushed project files with fixes development version {0}");
    private static final MessageFormat MSG_PATTERN_UPDATED_NEXT_VERSION = new MessageFormat(MSG_PREFIX + "Updated project files on {0} branch"
                                                                                            + " to next development version {1}");
    private static final MessageFormat MSG_PATTERN_PUSHED_NEXT_VERSION = new MessageFormat(MSG_PREFIX + "Pushed project files on {0} branch"
                                                                                           + " with next development version {1}");

    /**
     * Initialises a new <i>Start Release</i> action.
     *
     * @param build the <i>Start Release</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @param gitflowCause the cause for the new action.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public <BC extends B> StartReleaseAction(final BC build, final Launcher launcher, final BuildListener listener, final StartReleaseCause gitflowCause)
            throws IOException, InterruptedException {
        super(build, launcher, listener, gitflowCause);
    }

    @Override
    protected String getConsoleMessagePrefix() {
        return MSG_PREFIX;
    }

    @Override
    protected void beforeMainBuildInternal() throws IOException, InterruptedException {

        // Create a new release branch based on the develop branch.
        final String releaseBranch = this.gitflowCause.getReleaseBranch();
        this.git.checkoutBranch(releaseBranch, "origin/" + getBuildWrapperDescriptor().getDevelopBranch());
        this.consoleLogger.println(formatPattern(MSG_PATTERN_CREATED_RELEASE_BRANCH, releaseBranch));

        // Update the version numbers in the project files to the release version.
        final String releaseVersion = this.gitflowCause.getReleaseVersion();
        this.addFilesToGitStage(this.buildTypeAction.updateVersion(releaseVersion));
        final String msgUpadtedReleaseVersion = formatPattern(MSG_PATTERN_UPDATED_RELEASE_VERSION, releaseVersion);
        this.git.commit(msgUpadtedReleaseVersion);
        this.consoleLogger.println(msgUpadtedReleaseVersion);
    }

    @Override
    protected void afterMainBuildInternal() throws IOException, InterruptedException {
        if (build.getResult() == Result.SUCCESS) {
            this.afterSuccessfulMainBuild();
        } else {
            this.afterUnsuccessfulMainBuild();
        }
    }

    private void afterSuccessfulMainBuild() throws IOException, InterruptedException {

        // Push the new release branch to the remote repo.
        final String releaseBranch = this.gitflowCause.getReleaseBranch();
        this.git.push("origin", "refs/heads/" + releaseBranch + ":refs/heads/" + releaseBranch);
        this.consoleLogger.println(formatPattern(MSG_PATTERN_PUSHED_RELEASE_BRANCH, releaseBranch));

        // Create a tag for the release version.
        final String tagName = getBuildWrapperDescriptor().getVersionTagPrefix() + this.gitflowCause.getReleaseVersion();
        final String msgCreatedReleaseTag = formatPattern(MSG_PATTERN_CREATED_RELEASE_TAG, tagName);
        this.git.tag(tagName, msgCreatedReleaseTag);
        this.consoleLogger.println(msgCreatedReleaseTag);

        // Push the tag for the release version.
        this.git.push("origin", "refs/tags/" + tagName + ":refs/tags/" + tagName);
        this.consoleLogger.println(formatPattern(MSG_PATTERN_PUSHED_RELEASE_TAG, tagName));

        // Update the project files to the development version for the release fixes.
        final String releaseNextDevelopmentVersion = this.gitflowCause.getReleaseNextDevelopmentVersion();
        this.addFilesToGitStage(this.buildTypeAction.updateVersion(releaseNextDevelopmentVersion));
        final String msgUpdatedFixesVersion = formatPattern(MSG_PATTERN_UPDATED_FIXES_VERSION, releaseNextDevelopmentVersion);
        this.git.commit(msgUpdatedFixesVersion);
        this.consoleLogger.println(msgUpdatedFixesVersion);

        // Push the project files with the development version for the release fixes.
        this.git.push("origin", "refs/heads/" + releaseBranch + ":refs/heads/" + releaseBranch);
        this.consoleLogger.println(formatPattern(MSG_PATTERN_PUSHED_FIXES_VERSION, releaseNextDevelopmentVersion));

        // Record the fixes development version on the release branch.
        this.gitflowPluginProperties.saveResultAndVersionForBranch(releaseBranch, Result.SUCCESS, releaseNextDevelopmentVersion);

        // Update the project files in the develop branch to the development version for the next release.
        final String developBranch = getBuildWrapperDescriptor().getDevelopBranch();
        this.git.checkoutBranch(developBranch, "origin/" + developBranch);
        final String nextDevelopmentVersion = this.gitflowCause.getNextDevelopmentVersion();
        this.addFilesToGitStage(this.buildTypeAction.updateVersion(nextDevelopmentVersion));
        final String msgUpdatedNextVersion = formatPattern(MSG_PATTERN_UPDATED_NEXT_VERSION, developBranch, nextDevelopmentVersion);
        this.git.commit(msgUpdatedNextVersion);
        this.consoleLogger.println(msgUpdatedNextVersion);

        // Push the project files in the develop branch with the development version for the next release.
        this.git.push("origin", "refs/heads/" + developBranch + ":refs/heads/" + developBranch);
        this.consoleLogger.println(formatPattern(MSG_PATTERN_PUSHED_NEXT_VERSION, developBranch, nextDevelopmentVersion));

        // Record the next development version on the develop branch.
        this.gitflowPluginProperties.saveResultAndVersionForBranch(developBranch, Result.SUCCESS, nextDevelopmentVersion);

        // TODO Might configure further branches to merge to.
    }

    private void afterUnsuccessfulMainBuild() throws IOException {

        // Here we assume that there was an error on the develop branch right before we created the release branch.
        final String developBranch = getBuildWrapperDescriptor().getDevelopBranch();
        final String developBranchVersion = this.gitflowPluginProperties.loadVersionForBranch(developBranch);
        this.gitflowPluginProperties.saveResultAndVersionForBranch(developBranch, this.build.getResult(), developBranchVersion);
    }
}
