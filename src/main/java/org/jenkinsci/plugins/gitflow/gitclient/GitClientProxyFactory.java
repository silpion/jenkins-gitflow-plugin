package org.jenkinsci.plugins.gitflow.gitclient;

import java.io.IOException;
import java.util.Formatter;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.util.VersionNumber;

import jenkins.model.Jenkins;

/**
 * Factory implementation to instanciate/provide the {@link GitClientProxy} implementation/subclass
 * regarding to the installed version of the Jenkins Git plugin.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GitClientProxyFactory {

    private static final String MSG_PATTERN_UNSUPPORTED_PLUGIN_VERSION = "Gitflow plugin requires at least Git plugin version %s. Currently installed version is %s%n";

    /**
     * Creates and returns the {@link GitClientProxy} for the installed version of the Jenkins Git plugin.
     *
     * @param build the build that is in progress.
     * @param listener can be used to send any message.
     * @param dryRun is the build dryRun or not
     * @return the {@link GitClientProxy} for the installed version of the Jenkins Git plugin.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public static GitClientProxy newInstance(final AbstractBuild<?, ?> build, final BuildListener listener, final boolean dryRun) throws IOException, InterruptedException {
        final GitClientProxy gitClient;

        final VersionNumber gitPluginVersion = Jenkins.getInstance().getPlugin("git").getWrapper().getVersionNumber();
        if (!gitPluginVersion.isOlderThan(GitClient23Proxy.BASE_VERSION_NUMBER)) {
            gitClient = new GitClient23Proxy(build, listener, dryRun);
        } else if (!gitPluginVersion.isOlderThan(GitClient22Proxy.BASE_VERSION_NUMBER)) {
            gitClient = new GitClient22Proxy(build, listener, dryRun);
        } else {
            final String message = new Formatter().format(MSG_PATTERN_UNSUPPORTED_PLUGIN_VERSION, GitClient22Proxy.BASE_VERSION_NUMBER, gitPluginVersion).toString();
            throw new RuntimeException(message);
        }

        return gitClient;
    }
}
