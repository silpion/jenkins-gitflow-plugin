package de.silpion.jenkins.plugins.gitflow.action;

import java.io.IOException;

import de.silpion.jenkins.plugins.gitflow.proxy.gitclient.GitClientProxy;
import de.silpion.jenkins.plugins.gitflow.cause.AbstractGitflowCause;
import de.silpion.jenkins.plugins.gitflow.cause.FinishHotfixCause;
import de.silpion.jenkins.plugins.gitflow.cause.FinishReleaseCause;
import de.silpion.jenkins.plugins.gitflow.cause.PublishHotfixCause;
import de.silpion.jenkins.plugins.gitflow.cause.PublishReleaseCause;
import de.silpion.jenkins.plugins.gitflow.cause.StartHotfixCause;
import de.silpion.jenkins.plugins.gitflow.cause.StartReleaseCause;
import de.silpion.jenkins.plugins.gitflow.cause.TestHotfixCause;
import de.silpion.jenkins.plugins.gitflow.cause.TestReleaseCause;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

/**
 * Factory class to create the instances for the  different <i>Gitflow</i> actions to be executed.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GitflowActionFactory {

    public static <B extends AbstractBuild<?, ?>> AbstractGitflowAction<?, ?> newInstance(final B build, final Launcher launcher, final BuildListener listener)
            throws IOException, InterruptedException {
        final AbstractGitflowAction<?, ?> gitflowAction;

        final AbstractGitflowCause gitflowCause = build.getCause(AbstractGitflowCause.class);

        final boolean dryRun = gitflowCause != null && gitflowCause.isDryRun();
        final GitClientProxy git = new GitClientProxy(build, listener, dryRun);

        // The action to be created depends on the cause.
        if (gitflowCause == null) {
            gitflowAction = new NoGitflowAction<B>(build, launcher, listener, git);
        } else if (gitflowCause instanceof StartReleaseCause) {
            gitflowAction = new StartReleaseAction<B>(build, launcher, listener, git, (StartReleaseCause) gitflowCause);
        } else if (gitflowCause instanceof TestReleaseCause) {
            gitflowAction = new TestReleaseAction<B>(build, launcher, listener, git, (TestReleaseCause) gitflowCause);
        } else if (gitflowCause instanceof PublishReleaseCause) {
            gitflowAction = new PublishReleaseAction<B>(build, launcher, listener, git, (PublishReleaseCause) gitflowCause);
        } else if (gitflowCause instanceof FinishReleaseCause) {
            gitflowAction = new FinishReleaseAction<B>(build, launcher, listener, git, (FinishReleaseCause) gitflowCause);
        } else if (gitflowCause instanceof StartHotfixCause) {
            gitflowAction = new StartHotfixAction<B>(build, launcher, listener, git, (StartHotfixCause) gitflowCause);
        } else if (gitflowCause instanceof TestHotfixCause) {
            gitflowAction = new TestHotfixAction<B>(build, launcher, listener, git, (TestHotfixCause) gitflowCause);
        } else if (gitflowCause instanceof PublishHotfixCause) {
            gitflowAction = new PublishHotfixAction<B>(build, launcher, listener, git, (PublishHotfixCause) gitflowCause);
        } else if (gitflowCause instanceof FinishHotfixCause) {
            gitflowAction = new FinishHotfixAction<B>(build, launcher, listener, git, (FinishHotfixCause) gitflowCause);
        } else {
            // Only an IOException causes the build to fail properly.
            throw new IOException("Unknown Gitflow cause " + gitflowCause.getClass().getName());
        }

        return gitflowAction;
    }
}
