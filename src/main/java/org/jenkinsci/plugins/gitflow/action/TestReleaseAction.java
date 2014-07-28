package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.gitflow.cause.TestReleaseCause;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;
import org.jenkinsci.plugins.gitflow.gitclient.GitClientDelegate;

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

    private static final String ACTION_NAME = "Test Release";
    private static final String MSG_PREFIX = "Gitflow - " + ACTION_NAME + ": ";
    private static final MessageFormat MSG_PATTERN_CHECKOUT_RELEASE_BRANCH = new MessageFormat(MSG_PREFIX + "Checkout release branch {0}");
    private static final MessageFormat MSG_PATTERN_UPDATED_RELEASE_VERSION = new MessageFormat(MSG_PREFIX + "Updated project files to release version {0}");
    private static final MessageFormat MSG_PATTERN_CREATED_RELEASE_TAG = new MessageFormat(MSG_PREFIX + "Created release version tag {0}");
    private static final MessageFormat MSG_PATTERN_UPDATED_FIXES_VERSION = new MessageFormat(MSG_PREFIX
                                                                                             + "Updated project files to fixes development version {0}");

    /**
     * Initialises a new <i>Publish Release</i> action.
     *
     * @param build the <i>Publish Release</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @param git the Git client used to execute commands for the Gitflow actions.
     * @param gitflowCause the cause for the new action.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public <BC extends B> TestReleaseAction(final BC build, final Launcher launcher, final BuildListener listener, final GitClientDelegate git, final TestReleaseCause gitflowCause)
            throws IOException, InterruptedException {
        super(build, launcher, listener, git, gitflowCause);
    }

    @Override
    protected String getConsoleMessagePrefix() {
        return MSG_PREFIX;
    }

    @Override
    protected String getActionName() {
        return ACTION_NAME;
    }

    @Override
    protected void beforeMainBuildInternal() throws IOException, InterruptedException {

        // Checkout the release Branch
        final String releaseBranch = this.gitflowCause.getReleaseBranch();
        final ObjectId releaseBranchRev = this.git.getHeadRev(this.git.getRemoteUrl("origin"), releaseBranch);
        this.git.checkoutBranch(releaseBranch, releaseBranchRev.getName());
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

        // Create remote URL.
        final URIish remoteUrl;
        try {
            remoteUrl = new URIish("origin");
        } catch (final URISyntaxException urise) {
            throw new IOException("Cannot create remote URL", urise);
        }

        // Push the new minor release version to the remote repo.
        final String releaseBranch = this.gitflowCause.getReleaseBranch();
        this.git.push().to(remoteUrl).ref("HEAD:refs/heads/" + releaseBranch).execute();

        // Record the information on the currently stable version on the release branch.
        final String fixesReleaseVersion = this.gitflowCause.getFixesReleaseVersion();
        final RemoteBranch remoteBranchRelease = this.gitflowPluginData.getRemoteBranch("origin", releaseBranch);
        remoteBranchRelease.setLastBuildResult(Result.SUCCESS);
        remoteBranchRelease.setLastBuildVersion(fixesReleaseVersion);
        remoteBranchRelease.setLastReleaseVersion(fixesReleaseVersion);
        remoteBranchRelease.setLastReleaseVersionCommit(this.git.getHeadRev(this.git.getRemoteUrl("origin"), releaseBranch));

        // Create a tag for the release version.
        final String tagName = getBuildWrapperDescriptor().getVersionTagPrefix() + fixesReleaseVersion;
        final String msgCreatedReleaseTag = formatPattern(MSG_PATTERN_CREATED_RELEASE_TAG, tagName);
        this.git.tag(tagName, msgCreatedReleaseTag);
        this.consoleLogger.println(msgCreatedReleaseTag);

        // Push the tag for the release version.
        this.git.push().to(remoteUrl).ref("refs/tags/" + tagName + ":refs/tags/" + tagName).execute();

        // Update and commit the project files to the minor version for the next release
        final String nextFixesDevelopmentVersion = this.gitflowCause.getNextFixesDevelopmentVersion();
        this.addFilesToGitStage(this.buildTypeAction.updateVersion(nextFixesDevelopmentVersion));
        final String msgUpdatedFixesVersion = formatPattern(MSG_PATTERN_UPDATED_FIXES_VERSION, nextFixesDevelopmentVersion);
        this.git.commit(msgUpdatedFixesVersion);
        this.consoleLogger.println(msgUpdatedFixesVersion);

        // Push the project files with the minor version for the next release.
        this.git.push().to(remoteUrl).ref("HEAD:refs/heads/" + releaseBranch).execute();

        // Record the fixes development version on the release branch.
        remoteBranchRelease.setLastBuildResult(Result.SUCCESS);
        remoteBranchRelease.setLastBuildVersion(nextFixesDevelopmentVersion);
    }

    private void afterUnsuccessfulMainBuild() {

        // Here we assume that there was an error on the release branch right before exetuted this action.
        final RemoteBranch remoteBranchRelease = this.gitflowPluginData.getRemoteBranch("origin", this.gitflowCause.getReleaseBranch());
        remoteBranchRelease.setLastBuildResult(this.build.getResult());
        remoteBranchRelease.setLastBuildVersion(remoteBranchRelease.getLastBuildVersion());
    }
}
