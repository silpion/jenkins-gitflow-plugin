package org.jenkinsci.plugins.gitflow.action;

import static hudson.model.Result.SUCCESS;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.jenkinsci.plugins.gitflow.GitflowBadgeAction;
import org.jenkinsci.plugins.gitflow.GitflowBuildWrapper;
import org.jenkinsci.plugins.gitflow.action.buildtype.AbstractBuildTypeAction;
import org.jenkinsci.plugins.gitflow.action.buildtype.BuildTypeActionFactory;
import org.jenkinsci.plugins.gitflow.cause.AbstractGitflowCause;
import org.jenkinsci.plugins.gitflow.data.GitflowPluginData;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;
import org.jenkinsci.plugins.gitflow.gitclient.GitClientDelegate;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Executor;
import hudson.model.Result;

import jenkins.model.Jenkins;

/**
 * Abstract base class for the different Gitflow actions to be executed - before and after the main build.
 *
 * @param <B> the build in progress.
 * @param <C> the <i>Gitflow</i> cause for the build in progress.
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public abstract class AbstractGitflowAction<B extends AbstractBuild<?, ?>, C extends AbstractGitflowCause> extends AbstractActionBase<B> {

    private static final String MSG_CLEAN_WORKING_DIRECTORY = "Ensuring clean working/checkout directory";
    private static final String MSG_ABORTING_TO_OMIT_MAIN_BUILD = "Gitflow - Intentionally aborting to omit the main build";

    private static final MessageFormat MSG_PATTERN_RESULT_TO_UNSTABLE = new MessageFormat("Gitflow - Changing result of successful build to"
                                                                                          + " unstable, because there are unstable branches: {0}");

    protected final C gitflowCause;

    protected final AbstractBuildTypeAction<?> buildTypeAction;
    protected final GitClientDelegate git;

    protected GitflowPluginData gitflowPluginData;

    protected Map<String, String> additionalBuildEnvVars = new HashMap<String, String>();

    /**
     * Initialises a new Gitflow action.
     *
     * @param build the build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @param gitflowCause the <i>Gitflow</i> cause for the build in progress.
     * @param actionName the name of the new action.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    protected AbstractGitflowAction(final B build, final Launcher launcher, final BuildListener listener, C gitflowCause, final String actionName)
            throws IOException, InterruptedException {
        super(build, listener);

        this.gitflowCause = gitflowCause;

        final boolean dryRun = gitflowCause.isDryRun();
        this.git = new GitClientDelegate(build, listener, dryRun);

        this.buildTypeAction = BuildTypeActionFactory.newInstance(build, launcher, listener);

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
        this.gitflowPluginData.setDryRun(dryRun);

        // Prepare the action object for the build badges to be displayed.
        final GitflowBadgeAction gitflowBadgeAction = new GitflowBadgeAction();
        gitflowBadgeAction.setGitflowActionName(actionName);
        build.addAction(gitflowBadgeAction);
    }

    /**
     * Returns the build wrapper descriptor.
     *
     * @return the build wrapper descriptor.
     */
    protected static GitflowBuildWrapper.DescriptorImpl getBuildWrapperDescriptor() {
        return (GitflowBuildWrapper.DescriptorImpl) Jenkins.getInstance().getDescriptor(GitflowBuildWrapper.class);
    }

    /**
     * Runs the Gitflow actions that must be executed before the main build.
     *
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public final void beforeMainBuild() throws IOException, InterruptedException {
        this.cleanCheckout();
        this.beforeMainBuildInternal();
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
        if (buildResult.isBetterThan(Result.UNSTABLE) && getBuildWrapperDescriptor().isMarkSuccessfulBuildUnstableOnBrokenBranches()) {
            final Map<Result, Collection<RemoteBranch>> unstableBranchesGroupedByResult = this.gitflowPluginData.getUnstableRemoteBranchesGroupedByResult();
            if (MapUtils.isNotEmpty(unstableBranchesGroupedByResult)) {
                this.consoleLogger.println(formatPattern(MSG_PATTERN_RESULT_TO_UNSTABLE, unstableBranchesGroupedByResult.toString()));
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
        this.consoleLogger.println(this.getConsoleMessagePrefix() + MSG_CLEAN_WORKING_DIRECTORY);
        this.git.clean();
    }

    /**
     * Omit the main build by throwing an {@link InterruptedException}.
     *
     * @throws InterruptedException always thrown to omit the main build.
     */
    protected void omitMainBuild() throws InterruptedException {
        this.consoleLogger.println(MSG_ABORTING_TO_OMIT_MAIN_BUILD);
        Executor.currentExecutor().interrupt(SUCCESS);
        throw new InterruptedException(MSG_ABORTING_TO_OMIT_MAIN_BUILD);
    }

    /**
     * Returns the action-specific prefix for console messages.
     *
     * @return the action-specific prefix for console messages.
     */
    protected abstract String getConsoleMessagePrefix();

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
