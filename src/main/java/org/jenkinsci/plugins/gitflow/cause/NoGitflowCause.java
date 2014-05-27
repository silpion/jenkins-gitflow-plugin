package org.jenkinsci.plugins.gitflow.cause;

/**
 * The {@link hudson.model.Cause} object for the <i>No Gitflow</i> action to be executed.
 *
 * @author Maria Lüdemann, Silpion IT-Solutions GmbH - luedemann@silpion.de
 */
public class NoGitflowCause extends AbstractGitflowCause {

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     */
    public NoGitflowCause() {
        super(false);
    }

}
