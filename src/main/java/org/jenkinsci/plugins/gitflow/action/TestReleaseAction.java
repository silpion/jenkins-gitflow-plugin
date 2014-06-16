package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.gitflow.cause.TestReleaseCause;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;

/**
 * This class executes the required steps for the Gitflow action <i>Test Release</i>.
 *
 * @param <B> the build in progress.
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class TestReleaseAction<B extends AbstractBuild<?, ?>> extends AbstractGitflowAction<B, TestReleaseCause> {

    private static final String MSG_PREFIX = "Gitflow - Test Release: ";
    private static final MessageFormat MSG_PATTERN_CHECKOUT_RELEASE_BRANCH = new MessageFormat(MSG_PREFIX + "Checkout release branch {0}");
    private static final MessageFormat MSG_PATTERN_UPDATED_RELEASE_VERSION = new MessageFormat(MSG_PREFIX + "Updated project files to release version {0}");
    private static final MessageFormat MSG_PATTERN_PUSHED_RELEASE_BRANCH = new MessageFormat(MSG_PREFIX + "Pushed release branch {0}");
    private static final MessageFormat MSG_PATTERN_CREATED_RELEASE_TAG = new MessageFormat(MSG_PREFIX + "Created release version tag {0}");
    private static final MessageFormat MSG_PATTERN_PUSHED_RELEASE_TAG = new MessageFormat(MSG_PREFIX + "Pushed release tag {0}");
    private static final MessageFormat MSG_PATTERN_UPDATED_FIXES_VERSION = new MessageFormat(MSG_PREFIX
                                                                                             + "Updated project files to fixes development version {0}");
    private static final MessageFormat MSG_PATTERN_PUSHED_FIXES_VERSION = new MessageFormat(MSG_PREFIX
                                                                                            + "Pushed project files with fixes development version {0}");

    /**
     * Initialises a new <i>Publish Release</i> action.
     *
     * @param build the <i>Publish Release</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @param gitflowCause the cause for the new action.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public <BC extends B> TestReleaseAction(final BC build, final Launcher launcher, final BuildListener listener, final TestReleaseCause gitflowCause)
            throws IOException, InterruptedException {
        super(build, launcher, listener, gitflowCause);
    }

    @Override
    protected String getConsoleMessagePrefix() {
        return MSG_PREFIX;
    }

    @Override
    protected void beforeMainBuildInternal() throws IOException, InterruptedException {

        // Checkout the release Branch
        final String releaseBranch = this.gitflowCause.getReleaseBranch();
        final ObjectId releaseBranchRev = this.git.getHeadRev(this.git.getRemoteUrl("origin"), releaseBranch);
        this.git.checkout(releaseBranchRev.getName());
        this.consoleLogger.println(formatPattern(MSG_PATTERN_CHECKOUT_RELEASE_BRANCH, releaseBranch));

        // Update the project files to the minor release number
        final String fixesReleaseVersion = this.gitflowCause.getFixesReleaseVersion();
        this.addFilesToGitStage(this.buildTypeAction.updateVersion(fixesReleaseVersion));
        final String msgUpdatedReleaseVersion = formatPattern(MSG_PATTERN_UPDATED_RELEASE_VERSION, fixesReleaseVersion);
        this.git.commit(msgUpdatedReleaseVersion);
        this.consoleLogger.println(msgUpdatedReleaseVersion);
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

        // Push the new minor release version to the remote repo.
        final String releaseBranch = this.gitflowCause.getReleaseBranch();
        this.git.push("origin", "HEAD:refs/heads/" + releaseBranch);
        this.consoleLogger.println(formatPattern(MSG_PATTERN_PUSHED_RELEASE_BRANCH, releaseBranch));

        // Create a tag for the release version.
        final String fixesReleaseVersion = this.gitflowCause.getFixesReleaseVersion();
        final String tagName = getBuildWrapperDescriptor().getVersionTagPrefix() + fixesReleaseVersion;
        final String msgCreatedReleaseTag = formatPattern(MSG_PATTERN_CREATED_RELEASE_TAG, tagName);
        this.git.tag(tagName, msgCreatedReleaseTag);
        this.consoleLogger.println(msgCreatedReleaseTag);

        // Push the tag for the release version.
        this.git.push("origin", "refs/tags/" + tagName + ":refs/tags/" + tagName);
        this.consoleLogger.println(formatPattern(MSG_PATTERN_PUSHED_RELEASE_TAG, tagName));

        // Update and commit the project files to the minor version for the next release
        final String nextFixesDevelopmentVersion = this.gitflowCause.getNextFixesDevelopmentVersion();
        this.addFilesToGitStage(this.buildTypeAction.updateVersion(nextFixesDevelopmentVersion));
        final String msgUpdatedFixesVersion = formatPattern(MSG_PATTERN_UPDATED_FIXES_VERSION, nextFixesDevelopmentVersion);
        this.git.commit(msgUpdatedFixesVersion);
        this.consoleLogger.println(msgUpdatedFixesVersion);

        // Push the project files with the minor version for the next release.
        this.git.push("origin", "HEAD:refs/heads/" + releaseBranch);
        this.consoleLogger.println(formatPattern(MSG_PATTERN_PUSHED_FIXES_VERSION, nextFixesDevelopmentVersion));

        // Record the fixes development version on the release branch.
        this.gitflowPluginData.recordRemoteBranch("origin", releaseBranch, Result.SUCCESS, nextFixesDevelopmentVersion);
    }

    private void afterUnsuccessfulMainBuild() throws IOException {

        // Here we assume that there was an error on the release branch right before exetuted this action.
        final String releaseBranch = this.gitflowCause.getReleaseBranch();
        final String releaseBranchVersion = this.gitflowPluginData.getRemoteBranch("origin", releaseBranch).getLastBuildVersion();
        this.gitflowPluginData.recordRemoteBranch("origin", releaseBranch, this.build.getResult(), releaseBranchVersion);
    }
}
