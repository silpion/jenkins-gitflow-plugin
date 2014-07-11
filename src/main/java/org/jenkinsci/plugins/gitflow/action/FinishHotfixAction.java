package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.gitflow.cause.FinishHotfixCause;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;

/**
 * This class executes the required steps for the Gitflow action <i>Finish Hotfix</i>.
 *
 * @param <B> the build in progress.
 * @author Hannes Osius, Silpion IT-Solutions GmbH - osius@silpion.de
 */
public class FinishHotfixAction<B extends AbstractBuild<?, ?>> extends AbstractGitflowAction<B, FinishHotfixCause> {

    private static final String ACTION_NAME = "Finish Hotfix";
    private static final String MSG_PREFIX = "Gitflow - " + ACTION_NAME + ": ";

    private static final MessageFormat MSG_PATTERN_REMOVE_HOTFIX_BRANCH = new MessageFormat(MSG_PREFIX + "Remove hotfix branch {0}");

    /**
     * Initialises a new <i>Finish Hotfix</i> action.
     *
     * @param build the <i>Finish Hotfix</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @param gitflowCause the cause for the new action.
     * @throws java.io.IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public <BC extends B> FinishHotfixAction(final BC build, final Launcher launcher, final BuildListener listener, final FinishHotfixCause gitflowCause)
            throws IOException, InterruptedException {
        super(build, launcher, listener, gitflowCause, ACTION_NAME);
    }

    @Override
    protected String getConsoleMessagePrefix() {
        return MSG_PREFIX;
    }

    @Override
    protected String getActionName() {
        return ACTION_NAME;
    }

    @Override
    protected void beforeMainBuildInternal() throws IOException, InterruptedException {

        ObjectId hotfixBranchRev = git.getHeadRev(git.getRemoteUrl("origin"), "master");
        git.checkout(hotfixBranchRev.getName());

    }

    @Override
    protected void afterMainBuildInternal() throws IOException, InterruptedException {
        if (this.build.getResult() == Result.SUCCESS) {
            String message = formatPattern(MSG_PATTERN_REMOVE_HOTFIX_BRANCH, gitflowCause.getHotfixBranche());
            this.consoleLogger.println(message);

            //TODO remove local branch
            //the command is "git branch -d -r origin/gitflowCause.getHotfixBranche()"
            //but the git client has no command with "-d -r"
            // if (remove local branch == SUCCESS) the remove the remote branch

            //remove remote branch
            this.git.push("origin", ":" + gitflowCause.getHotfixBranche());
        }
    }
}
