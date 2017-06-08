package de.silpion.jenkins.plugins.gitflow.cause;

/**
 * The {@link hudson.model.Cause} object for the <i>No Gitflow</i> action to be executed.
 *
 * @author Maria Lüdemann, Silpion IT-Solutions GmbH - luedemann@silpion.de
 */
public class NoGitflowCause extends AbstractGitflowCause {

    public NoGitflowCause() {
        super(false);
    }

    @Override
    public String getVersionForBadge() {
        return "";
    }
}
