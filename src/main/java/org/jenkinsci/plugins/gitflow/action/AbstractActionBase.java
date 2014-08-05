package org.jenkinsci.plugins.gitflow.action;

import java.io.PrintStream;
import java.util.Formatter;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

/**
 * Abstract base class for the any kind of action.
 *
 * @param <B> the build in progress.
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public abstract class AbstractActionBase<B extends AbstractBuild<?, ?>> {

    protected final B build;
    protected final BuildListener listener;

    protected final PrintStream consoleLogger;

    /**
     * Initialises a new action.
     *
     * @param build the build that is in progress.
     * @param listener can be used to send any message.
     */
    public AbstractActionBase(final B build, final BuildListener listener) {
        this.build = build;
        this.listener = listener;

        this.consoleLogger = listener.getLogger();
    }

    /**
     * Creates and returns a formatted string using the specified format string and arguments.
     * It may not only format messages, but any string pattern (like e.g. a command pattern).
     *
     * @param messageFormat the string pattern to be formatted.
     * @param messageArguments the format arguments.
     * @return the formatted string.
     * @see Formatter#format(String, Object...)
     */
    protected static String formatPattern(final String messageFormat, final Object... messageArguments) {
        return new Formatter().format(messageFormat, messageArguments).toString();
    }
}
