package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.gitflow.cause.FinishHotfixCause;
import org.jenkinsci.plugins.gitflow.gitclient.GitClientDelegate;

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

    private static final String MSG_PATTERN_REMOVE_HOTFIX_BRANCH = "Gitflow - %s: Remove hotfix branch %s%n";

    /**
     * Initialises a new <i>Finish Hotfix</i> action.
     *
     * @param build the <i>Finish Hotfix</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @param git the Git client used to execute commands for the Gitflow actions.
     * @param gitflowCause the cause for the new action.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public <BC extends B> FinishHotfixAction(final BC build, final Launcher launcher, final BuildListener listener, final GitClientDelegate git, final FinishHotfixCause gitflowCause)
            throws IOException, InterruptedException {
        super(build, launcher, listener, git, gitflowCause);
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
            this.consoleLogger.printf(MSG_PATTERN_REMOVE_HOTFIX_BRANCH, ACTION_NAME, this.gitflowCause.getHotfixBranche());

            //TODO remove local branch
            //the command is "git branch -d -r origin/gitflowCause.getHotfixBranche()"
            //but the git client has no command with "-d -r"
            // if (remove local branch == SUCCESS) the remove the remote branch

            //remove remote branch
            this.git.push().to(getRemoteURI("origin")).ref(":" + gitflowCause.getHotfixBranche()).execute();
        }
    }

    private URIish getRemoteURI(String remote) throws IOException{
        // Create remote URL.
        try {
            return new URIish(remote);
        } catch (final URISyntaxException urise) {
            throw new IOException("Cannot create remote URL", urise);
        }
    }
}
