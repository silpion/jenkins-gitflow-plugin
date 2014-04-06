package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;

import org.jenkinsci.plugins.gitflow.cause.AbstractGitflowCause;
import org.jenkinsci.plugins.gitflow.cause.StartReleaseCause;

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

        // The action to be created depends on the cause.
        final AbstractGitflowCause gitflowCause = build.getCause(AbstractGitflowCause.class);
        if (gitflowCause == null) {
            gitflowAction = new NoGitflowAction(build, launcher, listener);
        } else if (gitflowCause instanceof StartReleaseCause) {
            gitflowAction = new StartReleaseAction(build, launcher, listener, (StartReleaseCause) gitflowCause);
        } else {
            // Only an IOException causes the build to fail properly.
            throw new IOException("Unknown Gitflow cause " + gitflowCause.getClass().getName());
        }

        return gitflowAction;
    }
}
