package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;
import java.io.PrintStream;

import org.jenkinsci.plugins.gitclient.GitClient;

import hudson.Launcher;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.git.GitSCM;
import hudson.tasks.Maven;

/**
 * Abstract base class for the different Gitflow actions to be executed - before and after the main build.
 * 
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public abstract class AbstractGitflowAction {

    protected final AbstractBuild build;
    protected final PrintStream consoleLogger;
    protected final GitClient git;

    private final Launcher launcher;
    private final BuildListener listener;

    protected AbstractGitflowAction(final AbstractBuild build, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
        this.build = build;
        this.launcher = launcher;
        this.listener = listener;
        this.consoleLogger = listener.getLogger();

        final GitSCM gitSCM = (GitSCM) build.getProject().getScm();
        this.git = gitSCM.createClient(listener, build.getEnvironment(listener), build);
    }

    /** Runs the Gitflow actions that must be executed before the main build. */
    public abstract void beforeMainBuild() throws IOException, InterruptedException;

    /** Runs the Gitflow actions that must be executed after the main build. */
    public abstract void afterMainBuild() throws IOException, InterruptedException;

    // TODO Build-type-specific actions shoul be implemented in a dedicated class.
    protected void executeMaven(final String arguments) throws IOException, InterruptedException {

        final MavenModuleSet mavenProject = (MavenModuleSet) this.build.getProject();
        final String mavenInstallation = mavenProject.getMaven().getName();
        final String pom = mavenProject.getRootPOM(this.build.getEnvironment(this.listener));

        final boolean success = new Maven(arguments, mavenInstallation, pom, null, null).perform(this.build, this.launcher, this.listener);
        if (!success) {
            throw new IOException("Error while executing mvn " + arguments);
        }
    }
}
