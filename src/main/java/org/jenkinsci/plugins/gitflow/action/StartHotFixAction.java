package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import org.jenkinsci.plugins.gitflow.GitflowBuildWrapper;
import org.jenkinsci.plugins.gitflow.cause.StartHotFixCause;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;

/**
 *
 * This class executes the required steps for the Gitflow action <i>Start Hotfix</i>.
 *
 * @param <B> the build in progress.
 * @author Hannes Osius, Silpion IT-Solutions GmbH - osius@silpion.de
 */
public class StartHotFixAction<B extends AbstractBuild<?, ?>> extends AbstractGitflowAction<B, StartHotFixCause> {

    private static final String ACTION_NAME = "Start HotFix";
    private static final String MSG_PREFIX = "Gitflow - " + ACTION_NAME + ": ";

    private static final MessageFormat MSG_PATTERN_CREATED_HOTFIX_BRANCH = new MessageFormat(MSG_PREFIX + "Created hotfix branch {0}");
    private static final MessageFormat MSG_PATTERN_UPDATED_HOTFIX_VERSION = new MessageFormat(MSG_PREFIX + "Updated project files to hotfix version {0}");

    private GitflowBuildWrapper.DescriptorImpl descriptor;

    /**
     * Initialises a new <i>Start Hotfix</i> action.
     *
     * @param build the <i>Start Hotfix</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @param startHotFixCause the cause for the new action.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public <BC extends B> StartHotFixAction(BC build, Launcher launcher, BuildListener listener, StartHotFixCause startHotFixCause)
            throws IOException, InterruptedException {
        super(build, launcher, listener, startHotFixCause, ACTION_NAME);
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
        // Create a new hotfix branch based on the master branch.
        String ref = "origin/" + getDescriptor().getMasterBranch();
        git.checkoutBranch(getHotfixBranchName(), ref);
        consoleLogger.println(formatPattern(MSG_PATTERN_CREATED_HOTFIX_BRANCH, getHotfixBranchName()));

        // Update the version numbers in the project files to the hotfix version.
        String hotfixVersion = gitflowCause.getNextHotfixDevelopmentVersion();
        List<String> changesFiles = buildTypeAction.updateVersion(hotfixVersion);
        addFilesToGitStage(changesFiles);
        String msgUpadtedReleaseVersion = formatPattern(MSG_PATTERN_UPDATED_HOTFIX_VERSION, hotfixVersion);
        git.commit(msgUpadtedReleaseVersion);
        consoleLogger.println(msgUpadtedReleaseVersion);
    }

    @Override
    protected void afterMainBuildInternal() throws IOException, InterruptedException {
        if (build.getResult()== Result.SUCCESS) {
            // Push the new hotfix branch to the remote repo.
            this.git.push("origin", "refs/heads/" + getHotfixBranchName() + ":refs/heads/" + getHotfixBranchName());
        }
        //Record the build Data
        gitflowPluginData.recordRemoteBranch("origin", getHotfixBranchName(), build.getResult(), gitflowCause.getNextHotfixDevelopmentVersion());
    }

    private String getHotfixBranchName() {
        return getDescriptor().getHotfixBranchPrefix() + gitflowCause.getName();
    }

    //TODO This Method only exist for make UnitTesting work, the AbstractGitflowAction needs some refactoring
    public GitflowBuildWrapper.DescriptorImpl getDescriptor() {
        if (descriptor == null){
            return AbstractGitflowAction.getBuildWrapperDescriptor();
        }
        return descriptor;
    }

    //TODO This Method only exist for make UnitTesting work, the AbstractGitflowAction needs some refactoring
    public void setDescriptor(final GitflowBuildWrapper.DescriptorImpl descriptor) {
        this.descriptor = descriptor;
    }

}


