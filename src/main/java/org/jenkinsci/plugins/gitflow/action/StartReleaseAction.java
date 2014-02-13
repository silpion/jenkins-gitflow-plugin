package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import hudson.Launcher;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;

/**
 * The action appears as the link in the side bar that users will click on in order to execute a Gitflow action.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class StartReleaseAction extends AbstractGitflowAction {

    private static final String DEVELOP_BRANCH = "develop";

    private final String releaseVersion;
    private final String nextDevelopmentVersion;
    private final String releaseBranch;
    private final String releaseNextDevelopmentVersion;

    public StartReleaseAction(final Map<String, String> gitflowActionParams, final AbstractBuild build, final Launcher launcher,
                              final BuildListener listener) throws IOException, InterruptedException {
        super(build, launcher, listener);

        this.releaseVersion = gitflowActionParams.get("releaseVersion");
        this.nextDevelopmentVersion = gitflowActionParams.get("nextDevelopmentVersion");
        this.releaseNextDevelopmentVersion = gitflowActionParams.get("releaseNextDevelopmentVersion");

        this.releaseBranch = "release/" + this.releaseVersion;
    }

    @Override
    public void beforeMainBuild() throws IOException, InterruptedException {

        // Ensure that there are no modified files in the working directory.
        this.consoleLogger.println("Gitflow: Ensuring clean working/checkout directory");
        this.git.clean();

        // Create a new release branch based on the develop branch.
        this.consoleLogger.println("Gitflow - Start Release: Creating release branch " + this.releaseBranch);
        this.git.checkoutBranch(this.releaseBranch, "origin/" + "develop");

        // Set the release version numbers in the project files and commit them.
        if (this.build instanceof MavenModuleSetBuild) {
            final MavenModuleSetBuild mavenBuild = (MavenModuleSetBuild) this.build;

            this.consoleLogger.println("Gitflow - Start Release: Setting Maven POM(s) to version " + this.releaseVersion);

            // Set the release version numbers in the Maven POM(s).
            executeMaven("org.codehaus.mojo:versions-maven-plugin:2.1:set -DnewVersion=" + this.releaseVersion + " -DgenerateBackupPoms=false");

            // Add the project files with the release version numbers to the Git stage.
            // TODO Would be nicer if the GitSCM offered something like 'git ls-files -m'.
            for (final MavenModule module : mavenBuild.getProject().getModules()) {
                final String moduleRelativePath = module.getRelativePath();
                final String modulePomFile = (StringUtils.isBlank(moduleRelativePath) ? "." : moduleRelativePath) + "/pom.xml";
                this.git.add(modulePomFile);
            }

        } else {
            this.consoleLogger.println("[WARNING] Gitflow - Start Release: Unsupported project type. Cannot change release number in project files.");
        }
        this.git.commit("Gitflow: Start release " + this.releaseVersion);
    }

    @Override
    public void afterMainBuild() throws IOException, InterruptedException {

        // Only run the Gitflow post build actions if the main build was successful.
        if (this.build.getResult() == Result.SUCCESS) {

            // Push the new release branch to the remote repo.
            this.consoleLogger.println("Gitflow - Start Release: Pushing new release branch " + this.releaseBranch);
            this.git.push("origin", "refs/heads/" + this.releaseBranch + ":refs/heads/" + this.releaseBranch);

            // Create and push a tag for the new release version.
            final String tagName = "version/" + releaseVersion;
            this.consoleLogger.println("Gitflow - Start Release: Creating tag " + tagName);
            this.git.tag(tagName, "Gitflow: Start release tag " + tagName);
            this.git.push("origin", "refs/tags/" + tagName + ":refs/tags/" + tagName);

            // Set the devlopment version numbers for the next release fix in the project files and commit them.
            if (this.build instanceof MavenModuleSetBuild) {
                final MavenModuleSetBuild mavenBuild = (MavenModuleSetBuild) this.build;

                this.consoleLogger.println("Gitflow - Start Release: Setting Maven POM(s) to version " + this.releaseNextDevelopmentVersion);

                // Set the version numbers in the Maven POM(s).
                executeMaven("org.codehaus.mojo:versions-maven-plugin:2.1:set -DnewVersion=" + this.releaseNextDevelopmentVersion + " -DgenerateBackupPoms=false");

                // Add the project files with the changed numbers to the Git stage.
                // TODO Would be nicer if the GitSCM offered something like 'git ls-files -m'.
                for (final MavenModule module : mavenBuild.getProject().getModules()) {
                    final String moduleRelativePath = module.getRelativePath();
                    final String modulePomFile = (StringUtils.isBlank(moduleRelativePath) ? "." : moduleRelativePath) + "/pom.xml";
                    this.git.add(modulePomFile);
                }

            } else {
                this.consoleLogger.println("[WARNING] Gitflow - Start Release: Unsupported project type. Cannot change release number in project files.");
            }
            this.git.commit("Gitflow: Start release - next release fix version " + this.releaseNextDevelopmentVersion);
            this.git.push("origin", "refs/heads/" + this.releaseBranch + ":refs/heads/" + this.releaseBranch);

            this.consoleLogger.println("Gitflow - Start Release: Merging release branch to branch " + DEVELOP_BRANCH);
            this.git.checkoutBranch(DEVELOP_BRANCH, "origin/" + DEVELOP_BRANCH);

            if (this.build instanceof MavenModuleSetBuild) {
                final MavenModuleSetBuild mavenBuild = (MavenModuleSetBuild) this.build;

                this.consoleLogger.println("Gitflow - Start Release: Setting Maven POM(s) to version " + this.nextDevelopmentVersion);

                executeMaven("org.codehaus.mojo:versions-maven-plugin:2.1:set -DnewVersion=" + this.nextDevelopmentVersion + " -DgenerateBackupPoms=false");

                // Add the project files with the changed numbers to the Git stage.
                // TODO Would be nicer if the GitSCM offered something like 'git ls-files -m'.
                for (final MavenModule module : mavenBuild.getProject().getModules()) {
                    final String moduleRelativePath = module.getRelativePath();
                    final String modulePomFile = (StringUtils.isBlank(moduleRelativePath) ? "." : moduleRelativePath) + "/pom.xml";
                    this.git.add(modulePomFile);
                }
            } else {
                this.consoleLogger.println("[WARNING] Gitflow - Start Release: Unsupported project type. Cannot change release number in project files.");
            }
            this.git.commit("Gitflow: Start release - next development version " + this.nextDevelopmentVersion);
            this.git.push("origin", "refs/heads/" + DEVELOP_BRANCH + ":refs/heads/" + DEVELOP_BRANCH);

            // TODO Might configure further branches to merge to.

        }
    }

    public String getReleaseVersion() {
        return this.releaseVersion;
    }

    public String getNextDevelopmentVersion() {
        return this.nextDevelopmentVersion;
    }
}
