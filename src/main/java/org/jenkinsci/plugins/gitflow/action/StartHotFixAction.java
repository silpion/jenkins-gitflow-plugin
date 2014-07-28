package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import org.jenkinsci.plugins.gitflow.cause.StartHotFixCause;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;
import org.jenkinsci.plugins.gitflow.gitclient.GitClientDelegate;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;

/**
 * This class executes the required steps for the Gitflow action <i>Start Hotfix</i>.
 *
 * @param <B> the build in progress.
 * @author Hannes Osius, Silpion IT-Solutions GmbH - osius@silpion.de
 */
public class StartHotFixAction<B extends AbstractBuild<?, ?>> extends AbstractGitflowAction<B, StartHotFixCause> {

    private static final String ACTION_NAME = "Start HotFix";

    private static final MessageFormat MSG_PATTERN_CREATED_HOTFIX_BRANCH = new MessageFormat("Gitflow - {0}: Created hotfix branch {0}");
    private static final MessageFormat MSG_PATTERN_UPDATED_HOTFIX_VERSION = new MessageFormat("Gitflow - {0}: Updated project files to hotfix version {0}");

    /**
     * Initialises a new <i>Start Hotfix</i> action.
     *
     * @param build the <i>Start Hotfix</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @param git the Git client used to execute commands for the Gitflow actions.
     * @param startHotFixCause the cause for the new action.  @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public <BC extends B> StartHotFixAction(BC build, Launcher launcher, BuildListener listener, GitClientDelegate git, StartHotFixCause startHotFixCause)
            throws IOException, InterruptedException {
        super(build, launcher, listener, git, startHotFixCause);
    }

    @Override
    protected String getActionName() {
        return ACTION_NAME;
    }

    @Override
    protected void beforeMainBuildInternal() throws IOException, InterruptedException {
        // Create a new hotfix branch based on the master branch.
        String ref = "origin/" + getBuildWrapperDescriptor().getMasterBranch();
        git.checkoutBranch(getHotfixBranchName(), ref);
        consoleLogger.println(formatPattern(MSG_PATTERN_CREATED_HOTFIX_BRANCH, getActionName(), getHotfixBranchName()));

        // Update the version numbers in the project files to the hotfix version.
        String hotfixVersion = gitflowCause.getNextHotfixDevelopmentVersion();
        List<String> changesFiles = buildTypeAction.updateVersion(hotfixVersion);
        addFilesToGitStage(changesFiles);
        String msgUpadtedReleaseVersion = formatPattern(MSG_PATTERN_UPDATED_HOTFIX_VERSION, getActionName(), hotfixVersion);
        git.commit(msgUpadtedReleaseVersion);
        consoleLogger.println(msgUpadtedReleaseVersion);
    }

    @Override
    protected void afterMainBuildInternal() throws IOException, InterruptedException {
        if (build.getResult() == Result.SUCCESS) {
            // Push the new hotfix branch to the remote repo.
            this.git.push().to(remoteUrl).ref("refs/heads/" + getHotfixBranchName() + ":refs/heads/" + getHotfixBranchName()).execute();
        }
        //Record the build Data
        RemoteBranch remoteBranch = gitflowPluginData.getOrAddRemoteBranch("origin", getHotfixBranchName());
        remoteBranch.setLastBuildResult(build.getResult());
        remoteBranch.setLastBuildVersion(gitflowCause.getNextHotfixDevelopmentVersion());
    }

    private String getHotfixBranchName() {
        return getBuildWrapperDescriptor().getHotfixBranchPrefix() + gitflowCause.getName();
    }
}


