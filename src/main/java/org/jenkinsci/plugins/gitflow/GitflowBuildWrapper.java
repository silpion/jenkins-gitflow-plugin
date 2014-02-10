package org.jenkinsci.plugins.gitflow;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.jenkinsci.plugins.gitclient.GitClient;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.json.JSONObject;

import hudson.Extension;
import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
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
import hudson.tasks.Maven;

import jenkins.util.NonLocalizable;

/**
 * Wraps a build that works on a Git repository. It enables the creation of Git releases,
 * respecting the <a href="http://nvie.com/posts/a-successful-git-branching-model/">Git Flow</a>.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GitflowBuildWrapper extends BuildWrapper {

    private static final String DEVELOP_BRANCH = "develop";

    @DataBoundConstructor
    public GitflowBuildWrapper() {
        // TODO Add config params
    }

    @Override
    public Environment setUp(final AbstractBuild build, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
        final Environment buildEnvironment;

        // Non-Gitflow builds don't contain the Gitflow settings.
        final GitflowArgumentsAction gitflowArgumentsAction = build.getAction(GitflowArgumentsAction.class);
        if (gitflowArgumentsAction == null) {
            buildEnvironment = new Environment() {
                // There's nothing to be overwritten for a non-Gitflow build.
            };
        } else {

            // Read the settings for the action to be executed.
            final String releaseVersion = gitflowArgumentsAction.getReleaseVersion();
            final String releaseBranch = "release/" + releaseVersion;

            // Create a new release branch based on the develop branch.
            listener.getLogger().println("Gitflow - Start Release: Creating branch " + releaseBranch);
            createGitClient(build, listener).checkoutBranch(releaseBranch, "origin/" + DEVELOP_BRANCH);

            // Update the version numbers in the project files.
            final boolean isVersionUpdateSuccess;
            if (build instanceof MavenModuleSetBuild) {
                final MavenModuleSetBuild mavenModuleSetBuild = (MavenModuleSetBuild) build;
                final String mavenArgs = "versions:set -DnewVersion=" + releaseVersion + " -DgenerateBackupPoms=false";
                isVersionUpdateSuccess = executeMaven(mavenArgs, launcher, listener, mavenModuleSetBuild);
            } else {
                // TODO Warn that version numbers in project files are not updated.
                isVersionUpdateSuccess = true;
            }

            if (isVersionUpdateSuccess) {
                buildEnvironment = new Environment() {

                    @Override
                    public boolean tearDown(final AbstractBuild build, final BuildListener listener) throws IOException, InterruptedException {

                        // Only run the Gitflow post build actions if the main build was successful.
                        if (build.getResult() == Result.SUCCESS) {

                            // Push release branch to remote repo.
                            listener.getLogger().println("Gitflow - Start Release: Pushing branch: " + releaseBranch);
                            createGitClient(build, listener).push("origin", "refs/heads/" + releaseBranch + ":refs/heads/" + releaseBranch);
                        }

                        return true;
                    }
                };
            } else {
                // Returning build environment 'null' dnotes a failure.
                buildEnvironment = null;
            }
        }

        return buildEnvironment;
    }

    private static boolean executeMaven(final String arguments, final Launcher launcher, final BuildListener listener,
                                        final MavenModuleSetBuild build) throws IOException, InterruptedException {

        final MavenModuleSet project = build.getProject();
        final String mavenInstallation = project.getMaven().getName();
        final String pom = project.getRootPOM(build.getEnvironment(listener));

        return new Maven(arguments, mavenInstallation, pom, null, null).perform(build, launcher, listener);
    }

    private static GitClient createGitClient(final AbstractBuild build, final BuildListener listener) throws IOException, InterruptedException {
        final GitSCM gitSCM = (GitSCM) build.getProject().getScm();
        return gitSCM.createClient(listener, build.getEnvironment(listener), build);
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

        private transient Logger log = LoggerFactory.getLogger(GitflowBuildWrapper.class);

        public static final Permission EXECUTE_GITFLOW =
                new Permission(Item.PERMISSIONS, "Gitflow", new NonLocalizable("Gitflow"), Hudson.ADMINISTER, PermissionScope.ITEM);

        private String masterBranch = "master";
        private String developBranch = "develop";
        private String releaseBranchPrefix = "release/";
        private String hotfixBranchPrefix = "hotfix/";
        private String featureBranchPrefix = "feature/";
        private String versionTagPrefix = "version/";

        public DescriptorImpl() {
            super(GitflowBuildWrapper.class);
            load();
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

            save();
            return true; // everything is allright so far
        }

        @Override
        public String getDisplayName() {
            return "Gitflow";
        }

        public String getMasterBranch() {
            return masterBranch;
        }

        public void setMasterBranch(String masterBranch) {
            this.masterBranch = masterBranch;
        }

        public String getDevelopBranch() {
            return developBranch;
        }

        public void setDevelopBranch(String developBranch) {
            this.developBranch = developBranch;
        }

        public String getFeatureBranchPrefix() {
            return featureBranchPrefix;
        }

        public void setFeatureBranchPrefix(String featureBranchPrefix) {
            this.featureBranchPrefix = featureBranchPrefix;
        }

        public String getReleaseBranchPrefix() {
            return releaseBranchPrefix;
        }

        public void setReleaseBranchPrefix(String releaseBranchPrefix) {
            this.releaseBranchPrefix = releaseBranchPrefix;
        }

        public String getHotfixBranchPrefix() {
            return hotfixBranchPrefix;
        }

        public void setHotfixBranchPrefix(String hotfixBranchPrefix) {
            this.hotfixBranchPrefix = hotfixBranchPrefix;
        }

        public String getVersionTagPrefix() {
            return versionTagPrefix;
        }

        public void setVersionTagPrefix(String versionTagPrefix) {
            this.versionTagPrefix = versionTagPrefix;
        }
    }
}
