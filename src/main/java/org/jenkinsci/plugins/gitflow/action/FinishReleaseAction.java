package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.gitflow.GitflowBuildWrapper;
import org.jenkinsci.plugins.gitflow.cause.FinishReleaseCause;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;
import org.jenkinsci.plugins.gitflow.gitclient.GitClientDelegate;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.git.Branch;

/**
 * This class executes the required steps for the Gitflow action <i>Finish Release</i>.
 *
 * @param <B> the build in progress.
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class FinishReleaseAction<B extends AbstractBuild<?, ?>> extends AbstractGitflowAction<B, FinishReleaseCause> {

    private static final String ACTION_NAME = "Finish Release";
    private static final String MSG_PREFIX = "Gitflow - " + ACTION_NAME + ": ";
    private static final MessageFormat MSG_PATTERN_NEW_HOTFIX_BRANCH_BASED_ON_RELEASE = new MessageFormat(MSG_PREFIX + "Created a new branch {0} based on {1}");
    private static final MessageFormat MSG_PATTERN_DELETED_BRANCH = new MessageFormat(MSG_PREFIX + "Deleted branch {0}");

    private static final Function<Branch, String> BRANCH_TO_NAME_FUNCTION = new Function<Branch, String>() {

        /** {@inheritDoc} */
        public String apply(final Branch input) {
            return input == null ? null : input.getName();
        }
    };

    private final URIish remoteUrl;

    /**
     * Initialises a new <i>Finish Release</i> action.
     *
     * @param build the <i>Finish Release</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @param git the Git client used to execute commands for the Gitflow actions.
     * @param gitflowCause the cause for the new action.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public <BC extends B> FinishReleaseAction(final BC build, final Launcher launcher, final BuildListener listener, final GitClientDelegate git, final FinishReleaseCause gitflowCause)
            throws IOException, InterruptedException {
        super(build, launcher, listener, git, gitflowCause);

        // Create remote URL.
        try {
            this.remoteUrl = new URIish("origin");
        } catch (final URISyntaxException urise) {
            throw new IOException("Cannot create remote URL", urise);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected String getConsoleMessagePrefix() {
        return MSG_PREFIX;
    }

    /** {@inheritDoc} */
    @Override
    protected String getActionName() {
        return ACTION_NAME;
    }

    /** {@inheritDoc} */
    @Override
    protected void beforeMainBuildInternal() throws IOException, InterruptedException {

        final String releaseBranch = this.gitflowCause.getReleaseBranch();

        // Include Start Hotfix action.
        if (this.gitflowCause.isIncludeStartHotfixAction()) {
            final GitflowBuildWrapper.DescriptorImpl buildWrapperDescriptor = getBuildWrapperDescriptor();
            final String releaseBranchPrefix = buildWrapperDescriptor.getReleaseBranchPrefix();
            final String hotfixBranchPrefix = buildWrapperDescriptor.getHotfixBranchPrefix();
            final String hotfixBranch = hotfixBranchPrefix + StringUtils.removeStart(releaseBranch, releaseBranchPrefix);
            this.createBranch(hotfixBranch, releaseBranch);
        }

        // Finish Release: just delete the release branch.
        this.deleteBranch(releaseBranch);

        // Abort the job, because there's no need to execute the main build.
        this.omitMainBuild();
    }

    private void createBranch(final String newBranchName, final String releaseBranch) throws InterruptedException {

        // Create a new hotfix branch.
        this.git.checkoutBranch(newBranchName, "origin/" + releaseBranch);
        this.consoleLogger.println(formatPattern(MSG_PATTERN_NEW_HOTFIX_BRANCH_BASED_ON_RELEASE, newBranchName, releaseBranch));

        // Push the new hotfix branch.
        this.git.push().to(this.remoteUrl).ref("refs/heads/" + newBranchName + ":refs/heads/" + newBranchName).execute();

        // Record the data for the new remote branch.
        final RemoteBranch remoteBranchRelease = this.gitflowPluginData.getRemoteBranch("origin", releaseBranch);
        final RemoteBranch remoteBranchNew = this.gitflowPluginData.getOrAddRemoteBranch("origin", newBranchName);
        remoteBranchNew.setLastBuildResult(remoteBranchRelease.getLastBuildResult());
        remoteBranchNew.setLastBuildVersion(remoteBranchRelease.getLastBuildVersion());
        remoteBranchNew.setLastReleaseVersion(remoteBranchRelease.getLastReleaseVersion());
        remoteBranchNew.setLastReleaseVersionCommit(remoteBranchRelease.getLastReleaseVersionCommit());
    }

    private void deleteBranch(final String branchName) throws InterruptedException {

        // Delete the remote branch locally and remotely.
        final Collection<String> localBranches = Collections2.transform(this.git.getBranches(), BRANCH_TO_NAME_FUNCTION);
        if (localBranches.contains(branchName)) {
            // The local branch might be missing when the action was executed in 'Dry Run' mode before.
            this.git.deleteBranch(branchName);
        }
        this.consoleLogger.println(formatPattern(MSG_PATTERN_DELETED_BRANCH, branchName));
        this.git.push().to(this.remoteUrl).ref(":refs/heads/" + branchName).execute();

        // Remove the recorded data of the deleted remote branch.
        final RemoteBranch remoteBranch = this.gitflowPluginData.getRemoteBranch("origin", branchName);
        if (remoteBranch != null) {
            this.gitflowPluginData.removeRemoteBranch(remoteBranch, false);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void afterMainBuildInternal() throws IOException, InterruptedException {
        // Nothing to do.
    }
}
