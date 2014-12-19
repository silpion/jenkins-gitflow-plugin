package org.jenkinsci.plugins.gitflow.proxy.git;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Formatter;

import org.jenkinsci.plugins.gitclient.GitClient;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
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

    private static final String MSG_PATTERN_UNSUPPORTED_PLUGIN_VERSION = "Gitflow plugin requires at least Git plugin version %s. Currently installed version is %s%n";

    private static final VersionNumber VERSION_NUMBER_21 = new VersionNumber("2.1");
    private static final VersionNumber VERSION_NUMBER_23 = new VersionNumber("2.3");

    private final GitSCM gitSCM;
    private VersionNumber gitPluginVersion;

    /**
     * Creates a new instance.
     *
     * @param build the build that is in progress.
     * @throws IOException if the version of the Git plugin is not supported.
     */
    public GitSCMProxy(final AbstractBuild<?, ?> build) throws IOException {
        this.gitSCM = (GitSCM) build.getProject().getScm();

        // Verify that the minimal required version of the Git Client plugin is installed.
        this.gitPluginVersion = Jenkins.getInstance().getPlugin("git").getWrapper().getVersionNumber();
        if (this.gitPluginVersion.isOlderThan(VERSION_NUMBER_21)) {
            final String message = new Formatter().format(MSG_PATTERN_UNSUPPORTED_PLUGIN_VERSION, VERSION_NUMBER_21, this.gitPluginVersion).toString();
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
        final GitClient gitClient;

        final EnvVars environment = build.getEnvironment(listener);

        // Create GitClient object depending on supported plugin versions.
        if (this.gitPluginVersion.isOlderThan(VERSION_NUMBER_23)) {
            gitClient = this.createClient21(build, listener, environment);
        } else {
            final FilePath workspace = build.getWorkspace();
            gitClient = this.createClient23(build, listener, environment, workspace);
        }

        return gitClient;
    }

    private GitClient createClient21(final AbstractBuild<?, ?> build, final BuildListener listener, final EnvVars environment) throws IOException, InterruptedException {
        final GitClient gitClient;

        try {
            final Method createClientMethod = this.gitSCM.getClass().getMethod("createClient", BuildListener.class, EnvVars.class, AbstractBuild.class);
            gitClient = (GitClient) createClientMethod.invoke(this.gitSCM, listener, environment, build);
        } catch (final NoSuchMethodException nsme) {
            throw new IOException("Cannot create Git client", nsme);
        } catch (final IllegalAccessException iae) {
            throw new IOException("Cannot create Git client", iae);
        } catch (final InvocationTargetException ite) {
            final Throwable cause = ite.getCause();
            if (cause instanceof InterruptedException) {
                throw (InterruptedException) cause;
            } else if (cause instanceof IOException) {
                throw (IOException) cause;
            } else {
                throw new IOException("Cannot create Git client", cause);
            }
        }

        return gitClient;
    }

    private GitClient createClient23(final Run<?, ?> build, final TaskListener listener, final EnvVars environment, final FilePath workspace) throws IOException, InterruptedException {
        final GitClient gitClient;

        try {
            final Method createClientMethod = this.gitSCM.getClass().getMethod("createClient", TaskListener.class, EnvVars.class, Run.class, FilePath.class);
            gitClient = (GitClient) createClientMethod.invoke(this.gitSCM, listener, environment, build, workspace);
        } catch (final NoSuchMethodException nsme) {
            throw new IOException("Cannot create Git client", nsme);
        } catch (final IllegalAccessException iae) {
            throw new IOException("Cannot create Git client", iae);
        } catch (final InvocationTargetException ite) {
            final Throwable cause = ite.getCause();
            if (cause instanceof InterruptedException) {
                throw (InterruptedException) cause;
            } else if (cause instanceof IOException) {
                throw (IOException) cause;
            } else {
                throw new IOException("Cannot create Git client", cause);
            }
        }

        return gitClient;
    }
}
