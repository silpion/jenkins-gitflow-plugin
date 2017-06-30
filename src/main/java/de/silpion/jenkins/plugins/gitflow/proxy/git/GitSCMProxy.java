package de.silpion.jenkins.plugins.gitflow.proxy.git;

import java.io.IOException;
import java.util.Formatter;

import org.jenkinsci.plugins.gitclient.GitClient;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.git.GitSCM;
import hudson.util.VersionNumber;

import jenkins.model.Jenkins;

/**
 * Proxy implementation for the Jenkins {@link GitSCM}. Uses <i>Reflections</i> to
 * implement version-dependant functions without causing compiler and/or runtime errors.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GitSCMProxy {

    public static final VersionNumber MINIMAL_VERSION_NUMBER = new VersionNumber("2.3");

    private static final String MSG_PATTERN_UNSUPPORTED_PLUGIN_VERSION = "Gitflow plugin requires at least Git plugin version %s. Currently installed version is %s%n";

    private final GitSCM gitSCM;

    /**
     * Creates a new instance.
     *
     * @param build the build that is in progress.
     * @throws IOException if the version of the Git plugin is not supported.
     */
    public GitSCMProxy(final AbstractBuild<?, ?> build) throws IOException {
        this.gitSCM = (GitSCM) build.getProject().getScm();

        // Verify that the minimal required version of the Git Client plugin is installed.
        final VersionNumber gitPluginVersion = Jenkins.getInstance().getPlugin("git").getWrapper().getVersionNumber();
        if (gitPluginVersion.isOlderThan(MINIMAL_VERSION_NUMBER)) {
            final String message = new Formatter().format(MSG_PATTERN_UNSUPPORTED_PLUGIN_VERSION, MINIMAL_VERSION_NUMBER, gitPluginVersion).toString();
            throw new IOException(message);
        }
    }

    /**
     * Creates and returns a new {@link GitClient} instance.
     *
     * @param build the build that is in progress.
     * @param listener can be used to send any message.
     * @return the new {@link GitClient} instance.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public GitClient createClient(final AbstractBuild<?, ?> build, final BuildListener listener) throws IOException, InterruptedException {
        return this.gitSCM.createClient(listener, build.getEnvironment(listener), build, build.getWorkspace());
    }
}
