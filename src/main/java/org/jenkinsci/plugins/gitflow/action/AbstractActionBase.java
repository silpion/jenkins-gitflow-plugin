package org.jenkinsci.plugins.gitflow.action;

import java.io.PrintStream;
import java.text.MessageFormat;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

/**
 * Abstract base class for the any kind of action.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public abstract class AbstractActionBase<T extends AbstractBuild<?, ?>> {

    protected final T build;
    protected final BuildListener listener;

    protected final PrintStream consoleLogger;

    /**
     * Initialises a new action.
     *
     * @param build the <i>Start Release</i> build that is in progress.
     * @param listener can be used to send any message.
     */
    public AbstractActionBase(final T build, final BuildListener listener) {
        this.build = build;
        this.listener = listener;

        this.consoleLogger = listener.getLogger();
    }

    /**
     * Formats a message pattern, substituting its placeholders with the provided arguments (see {@link java.text.MessageFormat}).
     * It may not only format messages, but any string pattern (like e.g. a command pattern).
     * <p/>
     * This is a conventience method for the {@code format} methods of the {@link java.text.MessageFormat} class.
     *
     * @param messageFormat the string pattern to be formatted.
     * @param messageArguments the format arguments.
     * @return the formatted string.
     */
    protected static String formatPattern(final MessageFormat messageFormat, final String... messageArguments) {
        return messageFormat.format(messageArguments);
    }
}
