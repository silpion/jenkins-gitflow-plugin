package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;

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

    public <BC extends B> StartHotFixAction(BC build, Launcher launcher, BuildListener listener, StartHotFixCause startHotFixCause)
            throws IOException, InterruptedException {
        super(build, launcher, listener, startHotFixCause, ACTION_NAME);
    }

    @Override
    protected void beforeMainBuildInternal() throws IOException, InterruptedException {
        //String releaseBranch = getBuildWrapperDescriptor().getReleaseBranchPrefix() + gitflowCause.getNextHotfixDevelopmentVersion();
        //git.checkoutBranch(releaseBranch, "origin/" + getBuildWrapperDescriptor().getDevelopBranch());
        consoleLogger.println("name: "+ gitflowCause.getName());
        consoleLogger.println("NextHotfixDevelopmentVersion: "+ gitflowCause.getNextHotfixDevelopmentVersion());

    }

    @Override
    protected void afterMainBuildInternal() throws IOException, InterruptedException {

    }

    @Override
    protected String getConsoleMessagePrefix() {
        return MSG_PREFIX;
    }

    @Override
    protected String getActionName() {
        return ACTION_NAME;
    }
}


