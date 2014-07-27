package org.jenkinsci.plugins.gitflow.action.buildtype;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import hudson.Launcher;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.RedeployPublisher;
import hudson.model.BuildListener;
import hudson.tasks.Maven;
import hudson.tasks.Publisher;

/**
 * This class implements the different actions, that are required to apply the <i>Gitflow</i> to Maven projects.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class MavenBuildTypeAction extends AbstractBuildTypeAction<MavenModuleSetBuild> {

    private static final MessageFormat CMD_PATTERN_SET_POM_VERSION = new MessageFormat("org.codehaus.mojo:versions-maven-plugin:2.1:set"
                                                                                       + " -DnewVersion={0} -DgenerateBackupPoms=false");

    private static final String SLASH_POM_XML = "/pom.xml";

    private static final String MAVEN_PROPERTY_SKIP_DEPLOYMENT = "maven.deploy.skip";
    private static final String PROPERTY_VALUE_TRUE = Boolean.TRUE.toString();

    /**
     * Initialises a new Maven build type action.
     *
     * @param build the <i>Gitflow</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     */
    public MavenBuildTypeAction(final MavenModuleSetBuild build, final Launcher launcher, final BuildListener listener) {
        super(build, launcher, listener);
    }

    @Override
    public String getCurrentVersion() {
        return this.build.getProject().getRootModule().getVersion();
    }

    @Override
    public List<String> updateVersion(final String version) throws IOException, InterruptedException {
        final List<String> modifiedFiles;

        // Run a Maven build that updates the project versions in the POMs.
        this.executeMaven("set-version_" + version + ".log", formatPattern(CMD_PATTERN_SET_POM_VERSION, version));

        // Each modules' POM should have been modified.
        final Collection<MavenModule> modules = this.build.getProject().getModules();
        modifiedFiles = new ArrayList<String>(modules.size());
        for (final MavenModule module : modules) {
            final String moduleRelativePath = module.getRelativePath();
            final String modulePomFile = (StringUtils.isBlank(moduleRelativePath) ? "." : moduleRelativePath) + SLASH_POM_XML;
            modifiedFiles.add(modulePomFile);
        }

        return modifiedFiles;
    }

    private void executeMaven(final String logFileName, final String... arguments) throws IOException, InterruptedException {

        final MavenModuleSet mavenProject = this.build.getProject();
        final String mavenInstallation = mavenProject.getMaven().getName();
        final String pom = mavenProject.getRootPOM(this.build.getEnvironment(this.listener));

        // Use a BuildListener delegate to redirect the Maven output to a file (instead of being displayed in the Jenkins console).
        final File outputLogFile = new File(this.build.getRootDir(), "gitflow-log/" + logFileName);
        final BuildListener buildListener = new BuildListenerDelegate(this.listener, outputLogFile);

        // Execute Maven and throw an Exception when it returns with an error.
        final String argumentsString = StringUtils.join(arguments, " ");
        final boolean success = new Maven(argumentsString, mavenInstallation, pom, null, null).perform(this.build, this.launcher, buildListener);
        if (!success) {
            throw new IOException("Error while executing mvn " + argumentsString);
        }
    }

    @Override
    public void preventArchivePublication(final Map<String, String> buildEnvVars) throws IOException {

        // Prevent artifact deployment if 'mvn deploy' is configured in the Jenkins job configuration.
        buildEnvVars.put(MAVEN_PROPERTY_SKIP_DEPLOYMENT, PROPERTY_VALUE_TRUE);

        // Prevent artifact deployment if the RedeployPublisher is configured in the Jenkins job configuration.
        final RedeployPublisher redeployPublisher = this.getConfiguredRedeployPublisher();
        if (redeployPublisher != null) {

            // The publisher only skips the deployment, if any 'releaseEnvVar' is configured.
            if (redeployPublisher.releaseEnvVar == null) {
                throw new IOException("Cannot skip deploy step for post build action 'Deploy artifacts to Maven repository'."
                                      + " Please define any 'Release environment variable' for that post build action in the job configuration.");
            } else {
                buildEnvVars.put(redeployPublisher.releaseEnvVar, PROPERTY_VALUE_TRUE);
            }
        }
    }

    private RedeployPublisher getConfiguredRedeployPublisher() {
        for (final Publisher publisher : this.build.getProject().getPublishers()) {
            if (publisher instanceof RedeployPublisher) {
                return (RedeployPublisher) publisher;
            }
        }
        return null;
    }
}
