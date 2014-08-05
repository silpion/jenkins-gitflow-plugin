package org.jenkinsci.plugins.gitflow.action;

import static hudson.model.Result.SUCCESS;
import static org.jenkinsci.plugins.gitflow.GitflowBuildWrapper.getGitflowBuildWrapperDescriptor;
import static org.jenkinsci.plugins.gitflow.GitflowBuildWrapper.OMIT_MAIN_BUILD_PARAMETER_NAME;
import static org.jenkinsci.plugins.gitflow.GitflowBuildWrapper.OMIT_MAIN_BUILD_PARAMETER_VALUE_TRUE;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.gitflow.GitflowBadgeAction;
import org.jenkinsci.plugins.gitflow.action.buildtype.AbstractBuildTypeAction;
import org.jenkinsci.plugins.gitflow.action.buildtype.BuildTypeActionFactory;
import org.jenkinsci.plugins.gitflow.cause.AbstractGitflowCause;
import org.jenkinsci.plugins.gitflow.data.GitflowPluginData;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;
import org.jenkinsci.plugins.gitflow.gitclient.GitClientDelegate;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Executor;
import hudson.model.Result;
import hudson.plugins.git.Branch;

/**
 * Abstract base class for the different Gitflow actions to be executed - before and after the main build.
 *
 * @param <B> the build in progress.
 * @param <C> the <i>Gitflow</i> cause for the build in progress.
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public abstract class AbstractGitflowAction<B extends AbstractBuild<?, ?>, C extends AbstractGitflowCause> extends AbstractActionBase<B> {

    private static final String MSG_ABORTING_TO_OMIT_MAIN_BUILD = "Intentionally aborting to omit the main build";
    private static final String MSG_PATTERN_ABORTING_TO_OMIT_MAIN_BUILD = "Gitflow - %s: " + MSG_ABORTING_TO_OMIT_MAIN_BUILD + "%n";
    private static final String MSG_PATTERN_CLEANED_UP_WORKING_DIRECTORY = "Gitflow - %s: Cleaned up working/checkout directory%n";
    private static final String MSG_PATTERN_CREATED_BRANCH_BASED_ON_OTHER = "Gitflow - %s: Created a new branch %s based on %s%n";
    private static final String MSG_PATTERN_DELETED_BRANCH = "Gitflow - %s: Deleted branch %s%n";
    private static final String MSG_PATTERN_RESULT_TO_UNSTABLE = "Gitflow - %s: Changing result of successful build to unstable, because there are unstable branches: %s%n";

    private static final Function<Branch, String> BRANCH_TO_NAME_FUNCTION = new Function<Branch, String>() {

        /** {@inheritDoc} */
        public String apply(final Branch input) {
            return input == null ? null : input.getName();
        }
    };

    protected final C gitflowCause;

    protected final AbstractBuildTypeAction<?> buildTypeAction;
    protected final GitClientDelegate git;

    protected final URIish remoteUrl;

    protected GitflowPluginData gitflowPluginData;

    protected Map<String, String> additionalBuildEnvVars = new HashMap<String, String>();

    /**
     * Initialises a new Gitflow action.
     *
     * @param build the build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @param git the Git client used to execute commands for the Gitflow actions.
     * @param gitflowCause the <i>Gitflow</i> cause for the build in progress.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    protected AbstractGitflowAction(final B build, final Launcher launcher, final BuildListener listener, final GitClientDelegate git, C gitflowCause) throws IOException, InterruptedException {
        super(build, listener);

        this.gitflowCause = gitflowCause;
        this.buildTypeAction = BuildTypeActionFactory.newInstance(build, launcher, listener);

        this.git = git;
        this.git.setGitflowActionName(this.getActionName());

        // Create remote URL.
        try {
            this.remoteUrl = new URIish("origin");
        } catch (final URISyntaxException urise) {
            throw new IOException("Cannot create remote URL", urise);
        }

        // Prepare the action object that holds the data for the Gitflow plugin.
        this.gitflowPluginData = build.getAction(GitflowPluginData.class);
        if (this.gitflowPluginData == null) {

            // Try to find the action object in one of the previous builds and clone it to a new one.
            for (AbstractBuild<?, ?> previousBuild = build.getPreviousBuild(); previousBuild != null; previousBuild = previousBuild.getPreviousBuild()) {
                final GitflowPluginData previousGitflowPluginData = previousBuild.getAction(GitflowPluginData.class);
                if (previousGitflowPluginData != null) {

                    // Clone the Gitflow plugin data from the previous build.
                    try {
                        this.gitflowPluginData = previousGitflowPluginData.clone();
                    } catch (final CloneNotSupportedException cnse) {
                        throw new IOException("Cloning of " + previousGitflowPluginData.getClass().getName() + " is not supported but should be.", cnse);
                    }

                    // Collect remote branches that don't exist anymore.
                    final List<RemoteBranch> removeRemoteBranches = new LinkedList<RemoteBranch>();
                    for (final RemoteBranch remoteBranch : this.gitflowPluginData.getRemoteBranches()) {
                        if (this.git.getHeadRev(this.git.getRemoteUrl(remoteBranch.getRemoteAlias()), remoteBranch.getBranchName()) == null) {
                            removeRemoteBranches.add(remoteBranch);
                        }
                    }

                    // Remove the obsolte remote branches from the Gitflow plugin data.
                    if (!removeRemoteBranches.isEmpty()) {
                        this.gitflowPluginData.removeRemoteBranches(removeRemoteBranches, true);
                    }

                    break;
                }
            }

            // Create a new action object if none was found in the previous builds.
            if (this.gitflowPluginData == null) {
                this.gitflowPluginData = new GitflowPluginData();
            }

            // Add the new action object to the build.
            build.addAction(this.gitflowPluginData);
        }
        this.gitflowPluginData.setDryRun(gitflowCause.isDryRun());
    }

    /**
     * Runs the Gitflow actions that must be executed before the main build.
     *
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public final void beforeMainBuild() throws IOException, InterruptedException {

        // Prepare the action object for the build badges to be displayed.
        final GitflowBadgeAction gitflowBadgeAction = new GitflowBadgeAction();
        gitflowBadgeAction.setGitflowActionName(this.getActionName());
        this.build.addAction(gitflowBadgeAction);

        // Clean up the checkout.
        this.cleanCheckout();

        // Execute the action-specific tasks.
        this.beforeMainBuildInternal();

        // Don't publish/deploy archives on Dry Run.
        if (this.gitflowCause.isDryRun()) {
            this.buildTypeAction.preventArchivePublication(this.additionalBuildEnvVars);
        }
    }

    /**
     * Runs the Gitflow actions that must be executed after the main build.
     *
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public final void afterMainBuild() throws IOException, InterruptedException {
        this.afterMainBuildInternal();

        // Mark successful build as unstable if there are unstable branches.
        final Result buildResult = this.build.getResult();
        if (buildResult.isBetterThan(Result.UNSTABLE) && getGitflowBuildWrapperDescriptor().isMarkSuccessfulBuildUnstableOnBrokenBranches()) {
            final Map<Result, Collection<RemoteBranch>> unstableBranchesGroupedByResult = this.gitflowPluginData.getUnstableRemoteBranchesGroupedByResult();
            if (MapUtils.isNotEmpty(unstableBranchesGroupedByResult)) {
                this.consoleLogger.printf(MSG_PATTERN_RESULT_TO_UNSTABLE, this.getActionName(), unstableBranchesGroupedByResult.toString());
                this.build.setResult(Result.UNSTABLE);
            }
        }
    }

    /**
     * Runs the Gitflow actions that must be executed before the main build.
     *
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    protected abstract void beforeMainBuildInternal() throws IOException, InterruptedException;

    /**
     * Runs the Gitflow actions that must be executed after the main build.
     *
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    protected abstract void afterMainBuildInternal() throws IOException, InterruptedException;

    /**
     * Adds the provided files to the Git stages - executing {@code git add [file1] [file2] ...}.
     * <p/>
     * TODO Instead of adding the modified files manually, it would be more reliable to ask the Git client for the files that have been mofified and add those.
     * Unfortunately the {@link hudson.plugins.git.GitSCM GitSCM} class doesn't offer a method to get the modified files. We might file a feature request and/or
     * implement it ourselves and then do a pull request on GitHub. The method to be implemented should execute something like {@code git ls-files -m}).
     *
     * @param files the files to be staged.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    protected void addFilesToGitStage(final List<String> files) throws InterruptedException {
        for (final String file : files) {
            this.git.add(file);
        }
    }

    /**
     * Before entering the {@link #beforeMainBuildInternal()}, the checkout directory is cleaned up so that there a no modified files.
     *
     * @throws InterruptedException if the build is interrupted during execution.
     */
    protected void cleanCheckout() throws InterruptedException {
        this.git.clean();
        this.consoleLogger.printf(MSG_PATTERN_CLEANED_UP_WORKING_DIRECTORY, this.getActionName());
    }

    protected void createBranch(final String newBranchName, final String releaseBranch) throws InterruptedException {

        // Create a new hotfix branch.
        this.git.checkoutBranch(newBranchName, "origin/" + releaseBranch);
        this.consoleLogger.printf(MSG_PATTERN_CREATED_BRANCH_BASED_ON_OTHER, this.getActionName(), newBranchName, releaseBranch);

        // Push the new hotfix branch.
        this.git.push().to(this.remoteUrl).ref("refs/heads/" + newBranchName + ":refs/heads/" + newBranchName).execute();

        // Record the data for the new remote branch.
        final RemoteBranch remoteBranchRelease = this.gitflowPluginData.getRemoteBranch("origin", releaseBranch);
        final RemoteBranch remoteBranchNew = this.gitflowPluginData.getOrAddRemoteBranch("origin", newBranchName);
        remoteBranchNew.setLastBuildResult(remoteBranchRelease.getLastBuildResult());
        remoteBranchNew.setLastBuildVersion(remoteBranchRelease.getLastBuildVersion());
        remoteBranchNew.setBaseReleaseVersion(remoteBranchRelease.getBaseReleaseVersion());
        remoteBranchNew.setLastReleaseVersion(remoteBranchRelease.getLastReleaseVersion());
        remoteBranchNew.setLastReleaseVersionCommit(remoteBranchRelease.getLastReleaseVersionCommit());
    }

    protected void deleteBranch(final String branchName) throws InterruptedException {

        // Delete the remote branch locally and remotely.
        final Collection<String> localBranches = Collections2.transform(this.git.getBranches(), BRANCH_TO_NAME_FUNCTION);
        if (localBranches.contains(branchName)) {
            // The local branch might be missing when the action was executed in 'Dry Run' mode before.
            this.git.deleteBranch(branchName);
        }
        this.consoleLogger.printf(MSG_PATTERN_DELETED_BRANCH, this.getActionName(), branchName);
        this.git.push().to(this.remoteUrl).ref(":refs/heads/" + branchName).execute();

        // Remove the recorded data of the deleted remote branch.
        final RemoteBranch remoteBranch = this.gitflowPluginData.getRemoteBranch("origin", branchName);
        if (remoteBranch != null) {
            this.gitflowPluginData.removeRemoteBranch(remoteBranch, false);
        }
    }

    /**
     * Cause the omission of the main build - the build will be interrupted by a subsequent build wrapper then.
     * Furthermore the method ensures proper settings for some post build actions that might the results of miss
     * the main build.
     *
     * @throws IOException if an error occurs that causes or should cause the build to fail.
     */
    protected void omitMainBuild() throws IOException {

        // Publication must be prevented, otherwise the publisher raises an error when not artifacts are to be deployed.
        this.buildTypeAction.preventArchivePublication(this.additionalBuildEnvVars);

        // The result of the interrupted build must be set to SUCCESS. Otherwise the build would be declared as FAILED.
        Executor.currentExecutor().interrupt(SUCCESS);

        // The build must be interrupted by a subsequent build wrapper, otherwise configurations for the post build actions aren't properly provided.
        this.additionalBuildEnvVars.put(OMIT_MAIN_BUILD_PARAMETER_NAME, OMIT_MAIN_BUILD_PARAMETER_VALUE_TRUE);

        this.consoleLogger.printf(MSG_PATTERN_ABORTING_TO_OMIT_MAIN_BUILD, this.getActionName());
    }

    /**
     * Returns the action-specific name for console messages.
     *
     * @return the action-specific name for console messages.
     */
    protected abstract String getActionName();

    public Map<String, String> getAdditionalBuildEnvVars() {
        return this.additionalBuildEnvVars;
    }
}
