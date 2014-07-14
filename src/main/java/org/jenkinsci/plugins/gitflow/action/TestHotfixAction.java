package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.gitflow.cause.TestHotfixCause;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

/**
 * This class executes the required steps for the Gitflow action <i>Test Hotfix</i>.
 *
 * @param <B> the build in progress.
 * @author Hannes Osius, Silpion IT-Solutions GmbH - osius@silpion.de
 */
public class TestHotfixAction<B extends AbstractBuild<?, ?>> extends AbstractGitflowAction<B, TestHotfixCause> {

    private static final String ACTION_NAME = "Test Hotfix";
    private static final String MSG_PREFIX = "Gitflow - " + ACTION_NAME + ": ";
    private static final MessageFormat MSG_PATTERN_CHECKOUT_HOTFIX_BRANCH = new MessageFormat(MSG_PREFIX + "Checkout hotfix branch {0}");
    private static final MessageFormat MSG_PATTERN_UPDATED_HOTFIX_VERSION = new MessageFormat(MSG_PREFIX + "Updated project files to Hotfix version {0}");
    private static final MessageFormat MSG_PATTERN_PUSHED_RELEASE_BRANCH = new MessageFormat(MSG_PREFIX + "Pushed release branch {0}");
    private static final MessageFormat MSG_PATTERN_CREATED_RELEASE_TAG = new MessageFormat(MSG_PREFIX + "Created release version tag {0}");
    private static final MessageFormat MSG_PATTERN_PUSHED_RELEASE_TAG = new MessageFormat(MSG_PREFIX + "Pushed release tag {0}");
    private static final MessageFormat MSG_PATTERN_UPDATED_FIXES_VERSION = new MessageFormat(MSG_PREFIX
                                                                                             + "Updated project files to fixes development version {0}");
    private static final MessageFormat MSG_PATTERN_PUSHED_FIXES_VERSION = new MessageFormat(MSG_PREFIX
                                                                                            + "Pushed project files with fixes development version {0}");

    /**
     * Initialises a new <i>Test Hotfix</i> action.
     *
     * @param build the <i>Test Hotfix</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @param gitflowCause the cause for the new action.
     * @throws java.io.IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public <BC extends B> TestHotfixAction(final BC build, final Launcher launcher, final BuildListener listener, final TestHotfixCause gitflowCause)
            throws IOException, InterruptedException {
        super(build, launcher, listener, gitflowCause, ACTION_NAME);
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

        // Checkout the hotfix Branch
        String hotfixBranch = gitflowCause.getHotfixBranch();
        ObjectId hotfixBranchRev = git.getHeadRev(git.getRemoteUrl("origin"), hotfixBranch);
        git.checkout(hotfixBranchRev.getName());
        consoleLogger.println(formatPattern(MSG_PATTERN_CHECKOUT_HOTFIX_BRANCH, hotfixBranch));

        // Update the project files to the minor release number
        String fixesReleaseVersion = gitflowCause.getHotfixReleaseVersion();

        addFilesToGitStage(buildTypeAction.updateVersion(fixesReleaseVersion));
        String msgUpdatedReleaseVersion = formatPattern(MSG_PATTERN_UPDATED_HOTFIX_VERSION, fixesReleaseVersion);
        this.git.commit(msgUpdatedReleaseVersion);
        this.consoleLogger.println(msgUpdatedReleaseVersion);
    }

    @Override
    protected void afterMainBuildInternal() throws IOException, InterruptedException {
//        if (this.build.getResult() == Result.SUCCESS) {
//            this.afterSuccessfulMainBuild();
//        } else {
//            this.afterUnsuccessfulMainBuild();
//        }
    }

    private void afterSuccessfulMainBuild() throws IOException, InterruptedException {

        // Push the new minor release version to the remote repo.
//        final String releaseBranch = this.gitflowCause.getReleaseBranch();
//        this.git.push("origin", "HEAD:refs/heads/" + releaseBranch);
//        this.consoleLogger.println(formatPattern(MSG_PATTERN_PUSHED_RELEASE_BRANCH, releaseBranch));

        // Create a tag for the release version.
//        final String fixesReleaseVersion = this.gitflowCause.getFixesReleaseVersion();
//        final String tagName = getBuildWrapperDescriptor().getVersionTagPrefix() + fixesReleaseVersion;
//        final String msgCreatedReleaseTag = formatPattern(MSG_PATTERN_CREATED_RELEASE_TAG, tagName);
//        this.git.tag(tagName, msgCreatedReleaseTag);
//        this.consoleLogger.println(msgCreatedReleaseTag);

        // Push the tag for the release version.
//        this.git.push("origin", "refs/tags/" + tagName + ":refs/tags/" + tagName);
//        this.consoleLogger.println(formatPattern(MSG_PATTERN_PUSHED_RELEASE_TAG, tagName));

        // Update and commit the project files to the minor version for the next release
//        final String nextFixesDevelopmentVersion = this.gitflowCause.getNextFixesDevelopmentVersion();
//        this.addFilesToGitStage(this.buildTypeAction.updateVersion(nextFixesDevelopmentVersion));
//        final String msgUpdatedFixesVersion = formatPattern(MSG_PATTERN_UPDATED_FIXES_VERSION, nextFixesDevelopmentVersion);
//        this.git.commit(msgUpdatedFixesVersion);
//        this.consoleLogger.println(msgUpdatedFixesVersion);

        // Push the project files with the minor version for the next release.
//        this.git.push("origin", "HEAD:refs/heads/" + releaseBranch);
//        this.consoleLogger.println(formatPattern(MSG_PATTERN_PUSHED_FIXES_VERSION, nextFixesDevelopmentVersion));

        // Record the fixes development version on the release branch.
//        this.gitflowPluginData.recordRemoteBranch("origin", releaseBranch, Result.SUCCESS, nextFixesDevelopmentVersion);
    }

    private void afterUnsuccessfulMainBuild() throws IOException {

        // Here we assume that there was an error on the release branch right before exetuted this action.
//        final String releaseBranch = this.gitflowCause.getReleaseBranch();
//        final String releaseBranchVersion = this.gitflowPluginData.getRemoteBranch("origin", releaseBranch).getLastBuildVersion();
//        this.gitflowPluginData.recordRemoteBranch("origin", releaseBranch, this.build.getResult(), releaseBranchVersion);
    }
}
