package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;

import org.jenkinsci.plugins.gitflow.GitflowBadgeAction;
import org.jenkinsci.plugins.gitflow.cause.AbstractGitflowCause;
import org.jenkinsci.plugins.gitflow.cause.StartReleaseCause;
import org.jenkinsci.plugins.gitflow.cause.TestReleaseCause;

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
        GitflowBadgeAction gitflowBadgeAction = new GitflowBadgeAction();
        build.addAction(gitflowBadgeAction);
        if (gitflowCause == null) {
            gitflowAction = new NoGitflowAction<B>(build, launcher, listener);

            gitflowBadgeAction.setDryRun(false);
            gitflowBadgeAction.setVersionNumber("");
            gitflowBadgeAction.setGitflowAction(gitflowAction.getActionName());
        } else if (gitflowCause instanceof StartReleaseCause) {
            gitflowAction = new StartReleaseAction<B>(build, launcher, listener, (StartReleaseCause) gitflowCause);

            gitflowBadgeAction.setDryRun(gitflowCause.isDryRun());
            gitflowBadgeAction.setVersionNumber(((StartReleaseCause) gitflowCause).getReleaseVersion());
            gitflowBadgeAction.setGitflowAction(gitflowAction.getActionName());
        } else if (gitflowCause instanceof TestReleaseCause) {
            gitflowAction = new TestReleaseAction<B>(build, launcher, listener, (TestReleaseCause) gitflowCause);

            gitflowBadgeAction.setDryRun(gitflowCause.isDryRun());
            gitflowBadgeAction.setVersionNumber(((TestReleaseCause) gitflowCause).getFixesReleaseVersion());
            gitflowBadgeAction.setGitflowAction(gitflowAction.getActionName());
        } else {
            // Only an IOException causes the build to fail properly.
            throw new IOException("Unknown Gitflow cause " + gitflowCause.getClass().getName());
        }

        return gitflowAction;
    }
}
