package org.jenkinsci.plugins.gitflow.gitclient;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jenkinsci.plugins.gitclient.GitClient;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.git.GitSCM;
import hudson.util.VersionNumber;

/**
 * Proxy implementation for the {@link GitClient} implementation of the Jenkins Git plugin version 2.2.0 (and newer).
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GitClient22Proxy extends GitClientProxy {

    /** The base (minimum) version of the Git plugin version that is covered by this proxy. */
    public static final VersionNumber BASE_VERSION_NUMBER = new VersionNumber("2.2.0");

    private final GitClient gitClient;

    /**
     * Creates a new instance.
     *
     * @param build the build that is in progress.
     * @param listener can be used to send any message.
     * @param dryRun is the build dryRun or not
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public GitClient22Proxy(final AbstractBuild<?, ?> build, final BuildListener listener, final boolean dryRun) throws IOException, InterruptedException {
        super(listener, dryRun);

        final GitSCM gitSCM = (GitSCM) build.getProject().getScm();
        final EnvVars environment = build.getEnvironment(listener);

        try {
            final Method createClientMethod = gitSCM.getClass().getMethod("createClient", BuildListener.class, EnvVars.class, AbstractBuild.class);
            this.gitClient = (GitClient) createClientMethod.invoke(gitSCM, listener, environment, build);
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
    }

    /** {@inheritDoc} */
    @Override
    protected GitClient getGitClient() {
        return this.gitClient;
    }
}
