package org.jenkinsci.plugins.gitflow;

import java.util.Collection;
import java.util.Collections;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.plugins.git.GitSCM;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

/**
 * Wraps a build that works on a Git repository. It enables the creation of Git releases,
 * respecting the <a href="http://nvie.com/posts/a-successful-git-branching-model/">Git Flow</a>.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GitflowBuildWrapper extends BuildWrapper {

    @DataBoundConstructor
    public GitflowBuildWrapper() {
        // TODO Add config params
    }

    @Override
    public Collection<? extends Action> getProjectActions(final AbstractProject job) {
        return Collections.singletonList(new GitflowReleaseAction());
    }

    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public boolean isApplicable(final AbstractProject<?, ?> item) {
            return item.getScm() instanceof GitSCM;
        }

        @Override
        public String getDisplayName() {
            return "Gitflow";
        }
    }
}
