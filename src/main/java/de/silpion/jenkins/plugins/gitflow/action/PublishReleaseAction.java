package de.silpion.jenkins.plugins.gitflow.action;

import de.silpion.jenkins.plugins.gitflow.GitflowBuildWrapper;
import de.silpion.jenkins.plugins.gitflow.cause.PublishReleaseCause;
import de.silpion.jenkins.plugins.gitflow.data.RemoteBranch;
import de.silpion.jenkins.plugins.gitflow.proxy.gitclient.GitClientProxy;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.git.Branch;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import org.eclipse.jgit.lib.ObjectId;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static de.silpion.jenkins.plugins.gitflow.GitflowBuildWrapper.getGitflowBuildWrapperDescriptor;
import static de.silpion.jenkins.plugins.gitflow.proxy.gitclient.merge.GenericMergeCommand.StrategyOption.THEIRS;
import static hudson.model.Result.SUCCESS;
import static org.eclipse.jgit.api.MergeCommand.FastForwardMode.NO_FF;
import static org.jenkinsci.plugins.gitclient.MergeCommand.Strategy.RECURSIVE;

/**
 * This class executes the required steps for the Gitflow action <i>Publish Release</i>.
 *
 * @param <B> the build in progress.
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class PublishReleaseAction<B extends AbstractBuild<?, ?>> extends AbstractGitflowAction<B, PublishReleaseCause> {

    private static final String ACTION_NAME = "Publish Release";

    private static final String MSG_PATTERN_CHECKOUT_BRANCH = "Gitflow - %s: Checked out branch %s%n";
    private static final String MSG_PATTERN_MERGED_LAST_PATCH_RELEASE = "Gitflow - %s: Merged last patch release %s to branch %s%n";

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
    public <BC extends B> PublishReleaseAction(final BC build, final Launcher launcher, final BuildListener listener, final GitClientProxy git, final PublishReleaseCause gitflowCause)
            throws IOException, InterruptedException {
        super(build, launcher, listener, git, gitflowCause);
    }

    /** {@inheritDoc} */
    @Override
    public String getActionName() {
        return ACTION_NAME;
    }

    /** {@inheritDoc} */
    @Override
    protected void beforeMainBuildInternal() throws IOException, InterruptedException {

        // Checkout the master branch.
        final GitflowBuildWrapper.DescriptorImpl buildWrapperDescriptor = getGitflowBuildWrapperDescriptor();
        final String masterBranch = buildWrapperDescriptor.getMasterBranch();
        final ObjectId targetBranchRev = this.git.getHeadRev(masterBranch);
        this.git.checkoutBranch(masterBranch, targetBranchRev.getName());
        this.consoleLogger.printf(MSG_PATTERN_CHECKOUT_BRANCH, ACTION_NAME, masterBranch);

        // Merge the last fixes release to the master branch.
        final ObjectId lastFixesReleaseCommit = this.gitflowCause.getLastPatchReleaseCommit();
        this.git.merge(lastFixesReleaseCommit, NO_FF, RECURSIVE, THEIRS, false);
        final String lastFixesReleaseVersion = this.gitflowCause.getLastPatchReleaseVersion();
        final String msgMergedLastFixesRelease = formatPattern(MSG_PATTERN_MERGED_LAST_PATCH_RELEASE, ACTION_NAME, lastFixesReleaseVersion, masterBranch);
        this.git.commit(msgMergedLastFixesRelease);
        this.consoleLogger.print(msgMergedLastFixesRelease);

        // Push the master branch with the new merge commit.
        this.git.push("origin", "refs/heads/" + masterBranch + ":refs/heads/" + masterBranch);

        // Record the version that have been merged to the master branch.
        final String releaseBranch = this.gitflowCause.getReleaseBranch();
        final RemoteBranch remoteBranchRelease = this.gitflowPluginData.getOrAddRemoteBranch(releaseBranch);
        final RemoteBranch remoteBranchMaster = this.gitflowPluginData.getOrAddRemoteBranch(masterBranch);
        remoteBranchMaster.setLastBuildResult(SUCCESS);
        remoteBranchMaster.setLastBuildVersion(lastFixesReleaseVersion);
        remoteBranchMaster.setBaseReleaseVersion(remoteBranchRelease.getBaseReleaseVersion());
        remoteBranchMaster.setLastReleaseVersion(lastFixesReleaseVersion);
        remoteBranchMaster.setLastReleaseVersionCommit(this.gitflowCause.getLastPatchReleaseCommit());

        // Set the build data with the merge commit on the master branch, so that it won't be scheduled for a new build.
        // Otherwise Jenkins might try to rebuild an already existing release and deploy it to the (Maven) repository manager.
        final ObjectId masterMergeCommit = this.git.getHeadRev(masterBranch);
        final String remoteBranchNameMaster = remoteBranchMaster.toString();
        final List<Branch> branches = Collections.singletonList(new Branch(remoteBranchNameMaster, masterMergeCommit));
        final Build masterBuild = new Build(new Revision(masterMergeCommit, branches), this.build.getNumber(), SUCCESS);
        this.build.getAction(BuildData.class).getBuildsByBranchName().put(remoteBranchNameMaster, masterBuild);

        // Add environment and property variables
        this.additionalBuildEnvVars.put("GIT_SIMPLE_BRANCH_NAME", masterBranch);
        this.additionalBuildEnvVars.put("GIT_REMOTE_BRANCH_NAME", "origin/" + masterBranch);
        this.additionalBuildEnvVars.put("GIT_BRANCH_TYPE", getGitflowBuildWrapperDescriptor().getBranchType(masterBranch));
    }

    /** {@inheritDoc} */
    @Override
    protected void afterMainBuildInternal() throws IOException, InterruptedException {
        // Nothing to do.
    }
}
