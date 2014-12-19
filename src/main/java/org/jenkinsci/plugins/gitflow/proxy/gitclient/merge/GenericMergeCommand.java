package org.jenkinsci.plugins.gitflow.proxy.gitclient.merge;

import java.io.PrintStream;

import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.jenkinsci.plugins.gitclient.MergeCommand;

import hudson.plugins.git.GitException;

/**
 * The generic merge command for unknown {@link GitClient} implementations. It enables its subclasses to provide merge options.
 *
 * @param <C> the {@link GitClient} type used to exectue the merge command.
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GenericMergeCommand<C extends GitClient> implements MergeCommand {

    protected final C gitClient;
    protected final PrintStream consoleLogger;

    protected ObjectId revisionToMerge;
    protected Strategy strategy;
    protected StrategyOption strategyOption;
    protected FastForwardMode fastForwardMode;
    protected boolean autoCommit;

    /**
     * Contructs a new generic merge command.
     *
     * @param gitClient the Git client used to excute the merge.
     * @param consoleLogger the logger instance, used to print messages to the Jenkins console.
     */
    public GenericMergeCommand(final C gitClient, final PrintStream consoleLogger) {
        this.gitClient = gitClient;
        this.consoleLogger = consoleLogger;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void execute() throws GitException, InterruptedException {
        this.gitClient.merge().setRevisionToMerge(this.revisionToMerge).setStrategy(this.strategy).execute();
    }

    /** {@inheritDoc} */
    public MergeCommand setRevisionToMerge(final ObjectId rev) {
        this.revisionToMerge = rev;
        return this;
    }

    /** {@inheritDoc} */
    public MergeCommand setStrategy(final Strategy strategy) {
        this.strategy = strategy;
        return this;
    }

    public GenericMergeCommand<C> setStrategyOption(final StrategyOption strategyOption) {
        this.strategyOption = strategyOption;
        return this;
    }

    public GenericMergeCommand<C> setFastForwardMode(final FastForwardMode fastForwardMode) {
        this.fastForwardMode = fastForwardMode;
        return this;
    }

    public GenericMergeCommand<C> setAutoCommit(final boolean autoCommit) {
        this.autoCommit = autoCommit;
        return this;
    }

    /** The possible option for the merge strategy (see git merge option {@code -X}). */
    public static enum StrategyOption {

        /** The merge strategy option {@code ours}. */
        OURS,

        /** The merge strategy option {@code theirs}. */
        THEIRS
    }
}
