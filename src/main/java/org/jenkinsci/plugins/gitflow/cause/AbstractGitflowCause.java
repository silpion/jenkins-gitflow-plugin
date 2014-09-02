package org.jenkinsci.plugins.gitflow.cause;

import hudson.model.Cause;

/**
 * The {@link Cause} object for the executed Gitflow actions.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public abstract class AbstractGitflowCause extends Cause {

    private boolean dryRun;

    @Override
    public String getShortDescription() {
        return "Triggered by Gitflow Plugin";
    }

    public boolean isDryRun() {
        return this.dryRun;
    }

    public void setDryRun(final boolean dryRun) {
        this.dryRun = dryRun;
    }

    /**
     * Returns the version number to be displayed as tool tip for the build badges.
     *
     * @return the version number to be displayed as tool tip for the build badges.
     */
    public abstract String getVersionForBadge();
}
