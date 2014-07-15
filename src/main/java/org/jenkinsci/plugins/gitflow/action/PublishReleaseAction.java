package org.jenkinsci.plugins.gitflow.action;

import static org.eclipse.jgit.api.MergeCommand.FastForwardMode.NO_FF;
import static org.jenkinsci.plugins.gitclient.MergeCommand.Strategy.RECURSIVE;
import static org.jenkinsci.plugins.gitflow.gitclient.merge.GenericMergeCommand.StrategyOption.THEIRS;

import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.gitflow.cause.PublishReleaseCause;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;

/**
 * This class executes the required steps for the Gitflow action <i>Publish Release</i>.
 *
 * @param <B> the build in progress.
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class PublishReleaseAction<B extends AbstractBuild<?, ?>> extends AbstractGitflowAction<B, PublishReleaseCause> {

    private static final String ACTION_NAME = "Publish Release";
    private static final String MSG_PREFIX = "Gitflow - " + ACTION_NAME + ": ";
    private static final MessageFormat MSG_PATTERN_CHECKOUT_MASTER_BRANCH = new MessageFormat(MSG_PREFIX + "Checked out branch {0}");
    private static final MessageFormat MSG_PATTERN_MERGED_TO_MASTER = new MessageFormat(MSG_PREFIX + "Merged last fixes release {0} to branch {1}");

    /**
     * Initialises a new <i>Publish Release</i> action.
     *
     * @param build the <i>Publish Release</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @param gitflowCause the cause for the new action.
     * @throws java.io.IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public <BC extends B> PublishReleaseAction(final BC build, final Launcher launcher, final BuildListener listener, final PublishReleaseCause gitflowCause)
            throws IOException, InterruptedException {
        super(build, launcher, listener, gitflowCause, ACTION_NAME);
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

        // Checkout the master Branch
        final String masterBranch = getBuildWrapperDescriptor().getMasterBranch();
        final ObjectId masterBranchRev = this.git.getHeadRev(this.git.getRemoteUrl("origin"), masterBranch);
        this.git.checkoutBranch(masterBranch, masterBranchRev.getName());
        this.consoleLogger.println(formatPattern(MSG_PATTERN_CHECKOUT_MASTER_BRANCH, masterBranch));

        // Merge the release branch to the master branch.
        final ObjectId lastFixesReleaseCommit = ObjectId.fromString(this.gitflowCause.getLastFixesReleaseCommit());
        this.git.merge(lastFixesReleaseCommit, NO_FF, RECURSIVE, THEIRS, false);
        final String lastFixesReleaseVersion = this.gitflowCause.getLastFixesReleaseVersion();
        final String msgMergedToMaster = formatPattern(MSG_PATTERN_MERGED_TO_MASTER, lastFixesReleaseVersion, masterBranch);
        this.git.commit(msgMergedToMaster);
        this.consoleLogger.println(msgMergedToMaster);

        // Push the master branch with the new merge commit.
        this.git.push("origin", "refs/heads/" + masterBranch + ":refs/heads/" + masterBranch);

        // Record the fixes development version on the release branch.
        final RemoteBranch remoteBranchMaster = this.gitflowPluginData.getOrAddRemoteBranch("origin", masterBranch);
        remoteBranchMaster.setLastBuildResult(Result.SUCCESS);
        remoteBranchMaster.setLastBuildVersion(lastFixesReleaseVersion);
        remoteBranchMaster.setLastReleaseVersion(lastFixesReleaseVersion);
        remoteBranchMaster.setLastReleaseVersionCommit(lastFixesReleaseCommit);

        // Abort the job, because there's no need to execute the main build.
        this.build.getExecutor().interrupt(Result.SUCCESS);
    }

    /** {@inheritDoc} */
    @Override
    protected void afterMainBuildInternal() throws IOException, InterruptedException {
        // Nothing to do.
    }
}
