package org.jenkinsci.plugins.gitflow.cause;

import hudson.model.Cause;

/**
 * The {@link Cause} object for the executed Gitflow actions.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class AbstractGitflowCause extends Cause {

    private final boolean dryRun;

    public AbstractGitflowCause(final boolean dryRun) {
        this.dryRun = dryRun;
    }

    @Override
    public String getShortDescription() {
        return "Triggered by Gitflow Plugin";
    }

    public boolean isDryRun() {
        return this.dryRun;
    }
}
