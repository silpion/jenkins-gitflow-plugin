package org.jenkinsci.plugins.gitflow.gitclient.merge;

import java.io.PrintStream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.merge.MergeStrategy;
import org.jenkinsci.plugins.gitclient.JGitAPIImpl;

import hudson.plugins.git.GitException;

/**
 * The merge command implementation for <i>JGit</i> clients, that can be provided with merge options.
 *
 * @param <C> the <i>JGit</i> type used to exectue the merge command.
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class JGitMergeCommand<C extends JGitAPIImpl> extends GenericMergeCommand<C> {

    /**
     * Contructs a new merge command for <i>JGit</i> clients.
     *
     * @param gitClient the <i>JGit</i> client used to excute the merge.
     * @param consoleLogger the logger instance, used to print messages to the Jenkins console.
     */
    public JGitMergeCommand(final C gitClient, final PrintStream consoleLogger) {
        super(gitClient, consoleLogger);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("ThrowsRuntimeException")
    public void execute() throws GitException, InterruptedException {

        final MergeCommand jgitMergeCommand = Git.wrap(this.gitClient.getRepository()).merge();

        jgitMergeCommand.setCommit(this.autoCommit);

        if (this.fastForwardMode != null) {
            jgitMergeCommand.setFastForward(this.fastForwardMode);
        }

        final MergeStrategy mergeStrategy = resolveMergeStrategy(this.strategy);
        if (mergeStrategy != null) {
            jgitMergeCommand.setStrategy(mergeStrategy);
        }

        if (this.strategyOption != null) {
            this.consoleLogger.println("[WARNING] JGit implementation doesn't support merge strategy options. Default implementation will be used.");
            jgitMergeCommand.setFastForward(this.fastForwardMode);
        }

        jgitMergeCommand.include(this.revisionToMerge);

        final MergeResult mergeResult;
        try {
            mergeResult = jgitMergeCommand.call();
        } catch (final GitAPIException gae) {
            throw new GitException("Could not merge " + this.revisionToMerge, gae);
        }
        if (!mergeResult.getMergeStatus().isSuccessful()) {
            throw new GitException("Could not merge " + this.revisionToMerge);
        }
    }

    private static MergeStrategy resolveMergeStrategy(final Strategy targetStrategy) {
        final MergeStrategy[] mergeStrategies = MergeStrategy.get();
        for (final MergeStrategy mergeStrategy : mergeStrategies) {
            if (targetStrategy == Strategy.OCTOPUS) {
                return MergeStrategy.SIMPLE_TWO_WAY_IN_CORE;
            } else if (mergeStrategy.getName().equals(targetStrategy.toString())) {
                return mergeStrategy;
            }
        }
        return null;
    }
}
