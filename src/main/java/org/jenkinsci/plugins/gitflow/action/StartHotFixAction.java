package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import org.jenkinsci.plugins.gitclient.GitClient;
import org.jenkinsci.plugins.gitflow.GitflowBuildWrapper;
import org.jenkinsci.plugins.gitflow.cause.StartHotFixCause;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

/**
 * @author Hannes Osius, Silpion IT-Solutions GmbH
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
        String hotfixBranch = getDescriptor().getHotfixBranchPrefix() + gitflowCause.getName();
        String ref = "origin/" + getDescriptor().getMasterBranch();
        getGitClient().checkoutBranch(hotfixBranch, ref);
        consoleLogger.println(formatPattern(MSG_PATTERN_CREATED_HOTFIX_BRANCH, hotfixBranch));

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

    }

    public GitflowBuildWrapper.DescriptorImpl getDescriptor() {
        if (descriptor == null){
            return AbstractGitflowAction.getBuildWrapperDescriptor();
        }
        return descriptor;
    }

    public void setDescriptor(final GitflowBuildWrapper.DescriptorImpl descriptor) {
        this.descriptor = descriptor;
    }

    public GitClient getGitClient() {
        return git;
    }
}


