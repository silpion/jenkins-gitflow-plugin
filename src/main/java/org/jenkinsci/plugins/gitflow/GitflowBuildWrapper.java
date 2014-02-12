package org.jenkinsci.plugins.gitflow;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import net.sf.json.JSONObject;

import hudson.Extension;
import hudson.Launcher;
import hudson.maven.MavenModule;
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

            final GitClient git = createGitClient(build, listener);

            // Read the settings for the action to be executed.
            final String releaseVersion = gitflowArgumentsAction.getReleaseVersion();
            final String releaseBranch = "release/" + releaseVersion;
            final String nextDevelopmentVersion = gitflowArgumentsAction.getNextDevelopmentVersion();

            // Ensure that there are no modified files in the working directory.
            listener.getLogger().println("Gitflow: Ensuring clean working/checkout directory");
            git.clean();

            // Create a new release branch based on the develop branch.
            listener.getLogger().println("Gitflow - Start Release: Creating release branch " + releaseBranch);
            git.checkoutBranch(releaseBranch, "origin/" + DEVELOP_BRANCH);

            // Set the release version numbers in the project files and commit them.
            final boolean isVersionUpdateSuccess;
            if (build instanceof MavenModuleSetBuild) {
                final MavenModuleSetBuild mavenBuild = (MavenModuleSetBuild) build;

                listener.getLogger().println("Gitflow - Start Release: Setting Maven POM(s) to version " + releaseVersion);

                // Set the release version numbers in the Maven POM(s).
                final String mavenArgs = "org.codehaus.mojo:versions-maven-plugin:2.1:set -DnewVersion=" + releaseVersion + " -DgenerateBackupPoms=false";
                isVersionUpdateSuccess = executeMaven(mavenArgs, launcher, listener, mavenBuild);
                if (isVersionUpdateSuccess) {

                    // Add the project files with the release version numbers to the Git stage.
                    // TODO Would be nicer if the GitSCM offered something like 'git ls-files -m'.
                    for (final MavenModule module : mavenBuild.getProject().getModules()) {
                        final String moduleRelativePath = module.getRelativePath();
                        final String modulePomFile = (StringUtils.isBlank(moduleRelativePath) ? "." : moduleRelativePath) + "/pom.xml";
                        git.add(modulePomFile);
                    }
                }

            } else {
                listener.getLogger().println("[WARNING] Gitflow - Start Release: Unsupported project type. Cannot change release number in project files.");
                isVersionUpdateSuccess = true;
            }
            git.commit("Gitflow: Start release " + releaseVersion);

            if (isVersionUpdateSuccess) {
                buildEnvironment = new Environment() {

                    @Override
                    public boolean tearDown(final AbstractBuild build, final BuildListener listener) throws IOException, InterruptedException {
                        boolean isVersionUpdateSuccess;

                        // Only run the Gitflow post build actions if the main build was successful.
                        if (build.getResult() == Result.SUCCESS) {

                            final GitClient git = createGitClient(build, listener);

                            // Push the new release branch to the remote repo.
                            listener.getLogger().println("Gitflow - Start Release: Pushing new release branch " + releaseBranch);
                            git.push("origin", "refs/heads/" + releaseBranch + ":refs/heads/" + releaseBranch);

                            // Create and push a tag for the new release version.
                            final String tagName = "version/" + releaseVersion;
                            listener.getLogger().println("Gitflow - Start Release: Creating tag " + tagName);
                            git.tag(tagName, "Gitflow: Start release tag " + tagName);
                            git.push("origin", "refs/tags/" + tagName + ":refs/tags/" + tagName);

                            // Set the devlopment version numbers for the next release fix in the project files and commit them.
                            final String releaseNextDevelopmentVersion = releaseVersion + ".1-SNAPSHOT";
                            if (build instanceof MavenModuleSetBuild) {
                                final MavenModuleSetBuild mavenBuild = (MavenModuleSetBuild) build;

                                listener.getLogger().println("Gitflow - Start Release: Setting Maven POM(s) to version " + releaseNextDevelopmentVersion);

                                // Set the version numbers in the Maven POM(s).
                                final String mavenArgs = "org.codehaus.mojo:versions-maven-plugin:2.1:set -DnewVersion=" + releaseNextDevelopmentVersion + " " +
                                        "-DgenerateBackupPoms=false";
                                isVersionUpdateSuccess = executeMaven(mavenArgs, launcher, listener, mavenBuild);
                                if (isVersionUpdateSuccess) {

                                    // Add the project files with the changed numbers to the Git stage.
                                    // TODO Would be nicer if the GitSCM offered something like 'git ls-files -m'.
                                    for (final MavenModule module : mavenBuild.getProject().getModules()) {
                                        final String moduleRelativePath = module.getRelativePath();
                                        final String modulePomFile = (StringUtils.isBlank(moduleRelativePath) ? "." : moduleRelativePath) + "/pom.xml";
                                        git.add(modulePomFile);
                                    }
                                }

                            } else {
                                listener.getLogger().println("[WARNING] Gitflow - Start Release: Unsupported project type. Cannot change release number in project files.");
                                isVersionUpdateSuccess = true;
                            }
                            git.commit("Gitflow: Start release - next release fix version " + releaseNextDevelopmentVersion);
                            git.push("origin", "refs/heads/" + releaseBranch + ":refs/heads/" + releaseBranch);

                            if (isVersionUpdateSuccess) {

                                listener.getLogger().println("Gitflow - Start Release: Merging release branch to branch " + DEVELOP_BRANCH);
                                git.checkoutBranch(DEVELOP_BRANCH, "origin/" + DEVELOP_BRANCH);

                                if (build instanceof MavenModuleSetBuild) {
                                    final MavenModuleSetBuild mavenBuild = (MavenModuleSetBuild) build;

                                    listener.getLogger().println("Gitflow - Start Release: Setting Maven POM(s) to version " + nextDevelopmentVersion);

                                    final String mavenArgs = "org.codehaus.mojo:versions-maven-plugin:2.1:set -DnewVersion=" + nextDevelopmentVersion + " " +
                                            "-DgenerateBackupPoms=false";
                                    isVersionUpdateSuccess = executeMaven(mavenArgs, launcher, listener, mavenBuild);
                                    if (isVersionUpdateSuccess) {

                                        // Add the project files with the changed numbers to the Git stage.
                                        // TODO Would be nicer if the GitSCM offered something like 'git ls-files -m'.
                                        for (final MavenModule module : mavenBuild.getProject().getModules()) {
                                            final String moduleRelativePath = module.getRelativePath();
                                            final String modulePomFile = (StringUtils.isBlank(moduleRelativePath) ? "." : moduleRelativePath) + "/pom.xml";
                                            git.add(modulePomFile);
                                        }
                                    }
                                } else {
                                    listener.getLogger().println("[WARNING] Gitflow - Start Release: Unsupported project type. Cannot change release number "
                                            + "in project files.");
                                    isVersionUpdateSuccess = true;
                                }
                                git.commit("Gitflow: Start release - next development version " + nextDevelopmentVersion);
                                git.push("origin", "refs/heads/" + DEVELOP_BRANCH + ":refs/heads/" + DEVELOP_BRANCH);

                                // TODO Might configure further branches to merge to.

                            } else {
                                isVersionUpdateSuccess = false;
                            }

                        } else {
                            isVersionUpdateSuccess = false;
                        }

                        return isVersionUpdateSuccess;
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
        return Collections.singletonList(new GitflowAction(job));
    }

    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {

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
