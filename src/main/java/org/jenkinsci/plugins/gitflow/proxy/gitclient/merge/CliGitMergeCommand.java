package org.jenkinsci.plugins.gitflow.proxy.gitclient.merge;

import java.io.PrintStream;

import org.jenkinsci.plugins.gitclient.CliGitAPIImpl;
import org.jenkinsci.plugins.gitclient.MergeCommand;

import hudson.plugins.git.GitException;
import hudson.util.ArgumentListBuilder;

/**
 * The merge command implementation for Git command line clients, that can be provided with merge options.
 *
 * @param <C> the Git command line client type used to exectue the merge command.
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class CliGitMergeCommand<C extends CliGitAPIImpl> extends GenericMergeCommand<C> {

    /**
     * Contructs a new merge command for Git command line clients.
     *
     * @param gitClient the Git command line client used to excute the merge.
     * @param consoleLogger the logger instance, used to print messages to the Jenkins console.
     */
    public CliGitMergeCommand(final C gitClient, final PrintStream consoleLogger) {
        super(gitClient, consoleLogger);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("ThrowsRuntimeException")
    public void execute() throws GitException, InterruptedException {

        final ArgumentListBuilder argumentListBuilder = new ArgumentListBuilder("merge");

        if (this.autoCommit) {
            argumentListBuilder.add("--commit");
        } else {
            argumentListBuilder.add("--no-commit");
        }

        if (this.fastForwardMode != null) {
            argumentListBuilder.add("--" + this.fastForwardMode.name().toLowerCase().replaceAll("_", "-"));
        }

        if (this.strategy != null && !this.strategy.equals(MergeCommand.Strategy.DEFAULT)) {
            argumentListBuilder.add("-s").add(this.strategy);
        }

        if (this.strategyOption != null) {
            argumentListBuilder.add("-X").add(this.strategyOption.name().toLowerCase().replaceAll("_", "-"));
        }

        argumentListBuilder.add(this.revisionToMerge.getName());
        try {
            this.gitClient.launchCommand(argumentListBuilder);
        } catch (final GitException ge) {
            throw new GitException("Could not merge " + this.revisionToMerge, ge);
        }
    }
}
