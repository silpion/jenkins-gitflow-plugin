package org.jenkinsci.plugins.gitflow;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.plugins.git.GitSCM;
import hudson.security.Permission;
import hudson.security.PermissionScope;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import jenkins.util.NonLocalizable;

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
    public Environment setUp(final AbstractBuild build, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
        return new Environment() {
            // There's nothing to be overwritten so far.
        };
    }

    public static boolean hasReleasePermission(@SuppressWarnings("rawtypes") AbstractProject job) {
        return job.hasPermission(DescriptorImpl.EXECUTE_GITFLOW);
    }

    @Override
    public Collection<? extends Action> getProjectActions(@SuppressWarnings("rawtypes") final AbstractProject job) {
        return Collections.singletonList(new GitflowReleaseAction(job));
    }

    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {

        public static final Permission EXECUTE_GITFLOW =
                new Permission(Item.PERMISSIONS, "Gitflow", new NonLocalizable("Gitflow"), Hudson.ADMINISTER, PermissionScope.ITEM);

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
