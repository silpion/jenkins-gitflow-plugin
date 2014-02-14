package org.jenkinsci.plugins.gitflow;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.jenkinsci.plugins.gitflow.action.AbstractGitflowAction;
import org.jenkinsci.plugins.gitflow.action.StartReleaseAction;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import net.sf.json.JSONObject;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Result;
import hudson.plugins.git.GitSCM;
import hudson.security.Permission;
import hudson.security.PermissionScope;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import jenkins.util.NonLocalizable;

/**
 * Wraps a build that works on a Git repository. It enables the creation of Git releases, respecting the <a
 * href="http://nvie.com/posts/a-successful-git-branching-model/">Git Flow</a>.
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
        final Environment buildEnvironment;

        // Non-Gitflow builds don't contain the Gitflow cause.
        @SuppressWarnings("unchecked")
        final GitflowCause gitflowCause = (GitflowCause) build.getCause(GitflowCause.class);
        if (gitflowCause == null) {
            buildEnvironment = new Environment() {
                // There's nothing to be overwritten for a non-Gitflow build.
            };
        } else {

            // TODO Implement factory class.
            final String action = gitflowCause.getAction();
            final AbstractGitflowAction gitflowAction;
            if ("startRelease".equals(action)) {
                gitflowAction = new StartReleaseAction(gitflowCause.getActionParams(), build, launcher, listener);
            } else {
                // Only an IOException causes the build to fail properly.
                throw new IOException("Unknown Gitflow action " + action);
            }

            gitflowAction.beforeMainBuild();

            buildEnvironment = new Environment() {

                @Override
                public boolean tearDown(final AbstractBuild build, final BuildListener listener) throws IOException, InterruptedException {

                    // Only run the Gitflow post build actions if the main build was successful.
                    if (build.getResult() == Result.SUCCESS) {
                        gitflowAction.afterMainBuild();
                    }

                    return true;
                }
            };
        }

        return buildEnvironment;
    }

    public static boolean hasReleasePermission(@SuppressWarnings("rawtypes") AbstractProject job) {
        return job.hasPermission(DescriptorImpl.EXECUTE_GITFLOW);
    }

    @Override
    public Collection<? extends Action> getProjectActions(@SuppressWarnings("rawtypes") final AbstractProject job) {
        return Collections.singletonList(new GitflowProjectAction(job));
    }

    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {

        public static final Permission EXECUTE_GITFLOW = new Permission(Item.PERMISSIONS, "Gitflow", new NonLocalizable("Gitflow"), Hudson.ADMINISTER,
                PermissionScope.ITEM);

        private String masterBranch = "master";
        private String developBranch = "develop";
        private String releaseBranchPrefix = "release/";
        private String hotfixBranchPrefix = "hotfix/";
        private String featureBranchPrefix = "feature/";
        private String versionTagPrefix = "version/";

        public DescriptorImpl() {
            super(GitflowBuildWrapper.class);
            this.load();
        }

        @Override
        public boolean isApplicable(final AbstractProject<?, ?> item) {
            return item.getScm() instanceof GitSCM;
        }

        @Override
        public boolean configure(StaplerRequest staplerRequest, JSONObject json) throws FormException {
            this.masterBranch = json.getString("masterBranch");
            this.developBranch = json.getString("developBranch");
            this.releaseBranchPrefix = json.getString("releaseBranchPrefix");
            this.hotfixBranchPrefix = json.getString("hotfixBranchPrefix");
            this.versionTagPrefix = json.getString("versionTagPrefix");
            this.featureBranchPrefix = json.getString("featureBranchPrefix");

            this.save();
            return true; // everything is allright so far
        }

        @Override
        public String getDisplayName() {
            return "Gitflow";
        }

        public String getMasterBranch() {
            return this.masterBranch;
        }

        public void setMasterBranch(String masterBranch) {
            this.masterBranch = masterBranch;
        }

        public String getDevelopBranch() {
            return this.developBranch;
        }

        public void setDevelopBranch(String developBranch) {
            this.developBranch = developBranch;
        }

        public String getFeatureBranchPrefix() {
            return this.featureBranchPrefix;
        }

        public void setFeatureBranchPrefix(String featureBranchPrefix) {
            this.featureBranchPrefix = featureBranchPrefix;
        }

        public String getReleaseBranchPrefix() {
            return this.releaseBranchPrefix;
        }

        public void setReleaseBranchPrefix(String releaseBranchPrefix) {
            this.releaseBranchPrefix = releaseBranchPrefix;
        }

        public String getHotfixBranchPrefix() {
            return this.hotfixBranchPrefix;
        }

        public void setHotfixBranchPrefix(String hotfixBranchPrefix) {
            this.hotfixBranchPrefix = hotfixBranchPrefix;
        }

        public String getVersionTagPrefix() {
            return this.versionTagPrefix;
        }

        public void setVersionTagPrefix(String versionTagPrefix) {
            this.versionTagPrefix = versionTagPrefix;
        }
    }
}
