package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.gitflow.cause.TestHotfixCause;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;

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
    private static final MessageFormat MSG_PATTERN_PUSHED_HOTFIX_BRANCH = new MessageFormat(MSG_PREFIX + "Pushed hotfix branch {0}");
    private static final MessageFormat MSG_PATTERN_UPDATED_NEXT_HOTFIX_VERSION = new MessageFormat(MSG_PREFIX
                                                                                             + "Updated project files to next hotfix version {0}");
    private static final MessageFormat MSG_PATTERN_PUSHED_NEXT_HOTFIX_VERSION = new MessageFormat(MSG_PREFIX
                                                                                            + "Pushed project files with next hotfix version {0}");

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
        git.commit(msgUpdatedReleaseVersion);
        consoleLogger.println(msgUpdatedReleaseVersion);
    }

    @Override
    protected void afterMainBuildInternal() throws IOException, InterruptedException {
        if (build.getResult() == Result.SUCCESS) {
            afterSuccessfulMainBuild();
        } else {
            afterUnsuccessfulMainBuild();
        }
    }

    private void afterSuccessfulMainBuild() throws IOException, InterruptedException {

        // Push the new minor release version to the remote repo.
        String hotfixBranch = gitflowCause.getHotfixBranch();
        git.push().to(getRemoteURI("origin")).ref("HEAD:refs/heads/" + hotfixBranch).execute();
        consoleLogger.println(formatPattern(MSG_PATTERN_PUSHED_HOTFIX_BRANCH, hotfixBranch));

        // Update and commit the project files to the next version for the next hotfix
        String nextHotfixVersion = gitflowCause.getNextHotfixReleaseVersion();
        addFilesToGitStage(buildTypeAction.updateVersion(nextHotfixVersion));
        String msgUpdatedFixesVersion = formatPattern(MSG_PATTERN_UPDATED_NEXT_HOTFIX_VERSION, nextHotfixVersion);
        git.commit(msgUpdatedFixesVersion);
        consoleLogger.println(msgUpdatedFixesVersion);

        // Push the project files with the next version for the next hotfix.
        git.push().to(getRemoteURI("origin")).ref("HEAD:refs/heads/" + hotfixBranch).execute();
        consoleLogger.println(formatPattern(MSG_PATTERN_PUSHED_NEXT_HOTFIX_VERSION, nextHotfixVersion));

        // Record the next hot development version on the hotfix branch.
        updatePluginData("origin", hotfixBranch, build.getResult(), nextHotfixVersion);
    }

    private void afterUnsuccessfulMainBuild() throws IOException {

        // Here we assume that there was an error on the hotfix branch right before executed this action.
        String hotfixBranch = gitflowCause.getHotfixBranch();
        String hotfixBranchVersion = gitflowPluginData.getRemoteBranch("origin", hotfixBranch).getLastBuildVersion();
        updatePluginData("origin", hotfixBranch, build.getResult(), hotfixBranchVersion);
    }

    private RemoteBranch updatePluginData(String remote, String branch, Result result, String version) {
        RemoteBranch remoteBranch = gitflowPluginData.getOrAddRemoteBranch(remote, branch);
        remoteBranch.setLastBuildResult(result);
        remoteBranch.setLastBuildVersion(version);
        return remoteBranch;
    }

    private URIish getRemoteURI(String remote) throws IOException{
        // Create remote URL.
        try {
            return new URIish(remote);
        } catch (final URISyntaxException urise) {
            throw new IOException("Cannot create remote URL", urise);
        }
    }

}
