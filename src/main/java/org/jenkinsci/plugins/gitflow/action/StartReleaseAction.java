package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

/**
 * This class executes the required steps for the Gitflow action <i>Start Release</i>.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class StartReleaseAction extends AbstractGitflowAction {

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

    private static final String PARAM_RELEASE_VERSION = "releaseVersion";
    private static final String PARAM_NEXT_DEVELOPMENT_VERSION = "nextDevelopmentVersion";
    private static final String PARAM_RELEASE_NEXT_DEVELOPMENT_VERSION = "releaseNextDevelopmentVersion";

    private final String releaseBranch;
    private final String releaseVersion;
    private final String releaseNextDevelopmentVersion;
    private final String nextDevelopmentVersion;

    /**
     * Initialises a new <i>Start Release</i> action.
     *
     * @param params the required parameters for the <i>Start Release</i> action.
     * @param build the <i>Start Release</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public StartReleaseAction(final Map<String, String> params, final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener)
            throws IOException, InterruptedException {
        super(build, launcher, listener);

        this.releaseVersion = getParameterValueAssertNotBlank(params, PARAM_RELEASE_VERSION);
        this.releaseNextDevelopmentVersion = getParameterValueAssertNotBlank(params, PARAM_RELEASE_NEXT_DEVELOPMENT_VERSION);
        this.nextDevelopmentVersion = getParameterValueAssertNotBlank(params, PARAM_NEXT_DEVELOPMENT_VERSION);

        this.releaseBranch = getBuildWrapperDescriptor().getReleaseBranchPrefix() + this.releaseVersion;
    }

    @Override
    protected String getConsoleMessagePrefix() {
        return MSG_PREFIX;
    }

    @Override
    protected void beforeMainBuildInternal() throws IOException, InterruptedException {

        // Create a new release branch based on the develop branch.
        this.git.checkoutBranch(this.releaseBranch, "origin/" + getBuildWrapperDescriptor().getDevelopBranch());
        this.consoleLogger.println(formatPattern(MSG_PATTERN_CREATED_RELEASE_BRANCH, this.releaseBranch));

        // Update the version numbers in the project files to the release version.
        this.addFilesToGitStage(this.buildTypeAction.updateVersion(this.releaseVersion));
        final String msgUpadtedReleaseVersion = formatPattern(MSG_PATTERN_UPDATED_RELEASE_VERSION, this.releaseVersion);
        this.git.commit(msgUpadtedReleaseVersion);
        this.consoleLogger.println(msgUpadtedReleaseVersion);
    }

    @Override
    protected void afterMainBuildInternal() throws IOException, InterruptedException {

        // Push the new release branch to the remote repo.
        this.git.push("origin", "refs/heads/" + this.releaseBranch + ":refs/heads/" + this.releaseBranch);
        this.consoleLogger.println(formatPattern(MSG_PATTERN_PUSHED_RELEASE_BRANCH, this.releaseBranch));

        // Create a tag for the release version.
        final String tagName = getBuildWrapperDescriptor().getVersionTagPrefix() + this.releaseVersion;
        final String msgCreatedReleaseTag = formatPattern(MSG_PATTERN_CREATED_RELEASE_TAG, tagName);
        this.git.tag(tagName, msgCreatedReleaseTag);
        this.consoleLogger.println(msgCreatedReleaseTag);

        // Push the tag for the release version.
        this.git.push("origin", "refs/tags/" + tagName + ":refs/tags/" + tagName);
        this.consoleLogger.println(formatPattern(MSG_PATTERN_PUSHED_RELEASE_TAG, tagName));

        // Update the project files to the development version for the release fixes.
        this.addFilesToGitStage(this.buildTypeAction.updateVersion(this.releaseNextDevelopmentVersion));
        final String msgUpdatedFixesVersion = formatPattern(MSG_PATTERN_UPDATED_FIXES_VERSION, this.releaseNextDevelopmentVersion);
        this.git.commit(msgUpdatedFixesVersion);
        this.consoleLogger.println(msgUpdatedFixesVersion);

        // Push the project files with the development version for the release fixes.
        this.git.push("origin", "refs/heads/" + this.releaseBranch + ":refs/heads/" + this.releaseBranch);
        this.consoleLogger.println(formatPattern(MSG_PATTERN_PUSHED_FIXES_VERSION, this.releaseNextDevelopmentVersion));

        // Record the fixes development version on the release branch.
        this.gitflowPluginProperties.saveVersionForBranch(this.releaseBranch, this.releaseNextDevelopmentVersion);

        // Update the project files in the develop branch to the development version for the next release.
        final String developBranch = getBuildWrapperDescriptor().getDevelopBranch();
        this.git.checkoutBranch(developBranch, "origin/" + developBranch);
        this.addFilesToGitStage(this.buildTypeAction.updateVersion(this.nextDevelopmentVersion));
        final String msgUpdatedNextVersion = formatPattern(MSG_PATTERN_UPDATED_NEXT_VERSION, developBranch, this.nextDevelopmentVersion);
        this.git.commit(msgUpdatedNextVersion);
        this.consoleLogger.println(msgUpdatedNextVersion);

        // Push the project files in the develop branch with the development version for the next release.
        this.git.push("origin", "refs/heads/" + developBranch + ":refs/heads/" + developBranch);
        this.consoleLogger.println(formatPattern(MSG_PATTERN_PUSHED_NEXT_VERSION, developBranch, this.nextDevelopmentVersion));

        // Record the next development version on the develop branch.
        this.gitflowPluginProperties.saveVersionForBranch(developBranch, this.nextDevelopmentVersion);

        // TODO Might configure further branches to merge to.
    }
}
