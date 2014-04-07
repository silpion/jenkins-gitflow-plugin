package org.jenkinsci.plugins.gitflow.cause;

import org.jenkinsci.plugins.gitflow.GitflowBuildWrapper;

import hudson.model.Cause;

import jenkins.model.Jenkins;

/**
 * The {@link Cause} object for the executed Gitflow actions.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class AbstractGitflowCause extends Cause {

    protected static GitflowBuildWrapper.DescriptorImpl getBuildWrapperDescriptor() {
        return (GitflowBuildWrapper.DescriptorImpl) Jenkins.getInstance().getDescriptor(GitflowBuildWrapper.class);
    }

    @Override
    public String getShortDescription() {
        return "Triggered by Gitflow Plugin";
    }
}
