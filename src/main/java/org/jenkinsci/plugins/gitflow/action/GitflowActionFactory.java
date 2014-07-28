package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;

import org.jenkinsci.plugins.gitflow.cause.AbstractGitflowCause;
import org.jenkinsci.plugins.gitflow.cause.FinishReleaseCause;
import org.jenkinsci.plugins.gitflow.cause.PublishReleaseCause;
import org.jenkinsci.plugins.gitflow.cause.StartHotFixCause;
import org.jenkinsci.plugins.gitflow.cause.StartReleaseCause;
import org.jenkinsci.plugins.gitflow.cause.TestReleaseCause;
import org.jenkinsci.plugins.gitflow.gitclient.GitClientDelegate;

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
        final GitClientDelegate git = new GitClientDelegate(build, listener, dryRun);

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
        } else if (gitflowCause instanceof StartHotFixCause) {
            gitflowAction = new StartHotFixAction<B>(build, launcher, listener, (StartHotFixCause) gitflowCause);
        } else {
            // Only an IOException causes the build to fail properly.
            throw new IOException("Unknown Gitflow cause " + gitflowCause.getClass().getName());
        }

        return gitflowAction;
    }
}
