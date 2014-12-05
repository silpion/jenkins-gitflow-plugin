package org.jenkinsci.plugins.gitflow.action.buildtype;

import static org.jenkinsci.plugins.gitflow.GitflowBuildWrapper.getGitflowBuildWrapperDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gitflow.data.GitflowPluginData;

import hudson.Launcher;
import hudson.maven.MavenArgumentInterceptorAction;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.RedeployPublisher;
import hudson.model.BuildListener;
import hudson.tasks.Maven;
import hudson.tasks.Publisher;
import hudson.util.ArgumentListBuilder;

/**
 * This class implements the different actions, that are required to apply the <i>Gitflow</i> to Maven projects.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class MavenBuildTypeAction extends AbstractBuildTypeAction<MavenModuleSetBuild> {

    private static final String SHORT_MSG_ABORTING_BECAUSE_OF_ARTIFACT_PUBLICATION_PREVENTION =
            "Aborting build because the release artifacts won't (probably) be published";
    private static final String LONG_MSG_PATTERN_ABORTING_BECAUSE_OF_ARTIFACT_PUBLICATION_PREVENTION =
            SHORT_MSG_ABORTING_BECAUSE_OF_ARTIFACT_PUBLICATION_PREVENTION + ":%n"
            + " - There are unstable branches in this job.%n"
            + " - The builds are declared unstable when there are unstable branches (see global configuration).%n"
            + " - The option 'Deploy even if the build is unstable' of the post build action 'Deploy artifacts to Maven repository' is not activated (see job configuration).%n";
    private static final String MSG_PATTERN_MINIMAL_BUILD_INSTEAD_OF_SKIP = "No need to run an entire build. Running minimal Maven build, because Jenkins doesn't support skipping.%n";

    private static final String CMD_PATTERN_SET_POM_VERSION = "org.codehaus.mojo:versions-maven-plugin:2.1:set -DnewVersion=%s -DgenerateBackupPoms=false";

    private static final String SLASH_POM_XML = "/pom.xml";

    private static final String MAVEN_PROPERTY_SKIP_DEPLOYMENT = "maven.deploy.skip";
    private static final String PROPERTY_VALUE_TRUE = Boolean.TRUE.toString();

    private static final MavenArgumentInterceptorAction RELEASE_BUILD_ARGUMENT_INTERCEPTOR_ACTION = new MavenArgumentInterceptorAction() {

        /** {@inheritDoc} */
        public String getGoalsAndOptions(final MavenModuleSetBuild build) {
            return build.getProject().getGoals() + " -Prelease-profile";
        }

        /** {@inheritDoc} */
        public ArgumentListBuilder intercept(final ArgumentListBuilder mavenargs, final MavenModuleSetBuild build) {
            return null;
        }

        /** {@inheritDoc} */
        public String getIconFileName() {
            return null;
        }

        /** {@inheritDoc} */
        public String getDisplayName() {
            return null;
        }

        /** {@inheritDoc} */
        public String getUrlName() {
            return null;
        }
    };

    private static final MavenArgumentInterceptorAction MINIMAL_QUIET_BUILD_ARGUMENT_INTERCEPTOR_ACTION = new MavenArgumentInterceptorAction() {

        /** {@inheritDoc} */
        public String getGoalsAndOptions(final MavenModuleSetBuild build) {
            // Adding the option --non-recursive would finish the build even faster, but it would cause the dependency graph to be re-calculated.
            return "validate --quiet";
        }

        /** {@inheritDoc} */
        public ArgumentListBuilder intercept(final ArgumentListBuilder mavenargs, final MavenModuleSetBuild build) {
            return null;
        }

        /** {@inheritDoc} */
        public String getIconFileName() {
            return null;
        }

        /** {@inheritDoc} */
        public String getDisplayName() {
            return null;
        }

        /** {@inheritDoc} */
        public String getUrlName() {
            return null;
        }
    };

    /**
     * Initialises a new Maven build type action.
     *
     * @param build the <i>Gitflow</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @param gitflowActionName the name of the <i>Gitflow</i> action for the build in progress.
     */
    public MavenBuildTypeAction(final MavenModuleSetBuild build, final Launcher launcher, final BuildListener listener, final String gitflowActionName) {
        super(build, launcher, listener, gitflowActionName);
    }

    /** {@inheritDoc} */
    @Override
    public String getCurrentVersion() {
        return this.build.getProject().getRootModule().getVersion();
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public void prepareForReleaseBuild() throws IOException {

        // Raise an error when the release artifacts probably won't be deployed.
        final boolean markSuccessfulBuildUnstableOnBrokenBranches = getGitflowBuildWrapperDescriptor().isMarkSuccessfulBuildUnstableOnBrokenBranches();
        if (markSuccessfulBuildUnstableOnBrokenBranches && this.isPreventPublicationIfUnstable() && this.hasUnstableBranches()) {
            this.consoleLogger.printf(LONG_MSG_PATTERN_ABORTING_BECAUSE_OF_ARTIFACT_PUBLICATION_PREVENTION);
            throw new IOException(SHORT_MSG_ABORTING_BECAUSE_OF_ARTIFACT_PUBLICATION_PREVENTION);
        }

        // MavenArgumentInterceptorAction that adds ' -Prelease-profile' to the Maven goals.
        this.build.addAction(RELEASE_BUILD_ARGUMENT_INTERCEPTOR_ACTION);
    }

    private boolean isPreventPublicationIfUnstable() {
        final RedeployPublisher redeployPublisher = this.getConfiguredRedeployPublisher();
        return redeployPublisher != null && !redeployPublisher.evenIfUnstable;
    }

    private boolean hasUnstableBranches() {
        return MapUtils.isNotEmpty(this.build.getAction(GitflowPluginData.class).getUnstableRemoteBranchesGroupedByResult());
    }

    /** {@inheritDoc} */
    @Override
    public void skipMainBuild(final Map<String, String> buildEnvVars) throws IOException {
        this.consoleLogger.printf(MSG_PATTERN_MINIMAL_BUILD_INSTEAD_OF_SKIP);

        // MavenArgumentInterceptorAction that sets a minimal build as main build - because there's no way to skip the build.
        this.build.replaceAction(MINIMAL_QUIET_BUILD_ARGUMENT_INTERCEPTOR_ACTION);

        // Publication must be prevented, otherwise the publisher deploys unwanted archives.
        this.preventArchivePublication(buildEnvVars);
    }

    /** {@inheritDoc} */
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
