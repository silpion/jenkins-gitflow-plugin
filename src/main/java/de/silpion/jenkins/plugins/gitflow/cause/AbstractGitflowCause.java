package de.silpion.jenkins.plugins.gitflow.cause;

import hudson.model.Cause;

/**
 * The {@link Cause} object for the executed Gitflow actions.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public abstract class AbstractGitflowCause extends Cause {

    protected static final String MAVEN_SNAPSHOT_VERSION_SUFFIX = "-SNAPSHOT";

    private boolean dryRun;
    private boolean omitMainBuild;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param omitMainBuild defines if the regarding {@link de.silpion.jenkins.plugins.gitflow.action.AbstractGitflowAction}
     *                      should omit the main (Maven) build.
     */
    protected AbstractGitflowCause(final boolean omitMainBuild) {
        this.omitMainBuild = omitMainBuild;
    }

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
     * Denotes if the regarding {@link de.silpion.jenkins.plugins.gitflow.action.AbstractGitflowAction} should omit the
     * main (Maven) build.
     *
     * @return {@code true} if the main build should be omitted, otherwise returns {@code true}.
     */
    public boolean isOmitMainBuild() {
        return this.omitMainBuild;
    }

    /**
     * Returns the version number to be displayed as tool tip for the build badges.
     *
     * @return the version number to be displayed as tool tip for the build badges.
     */
    public abstract String getVersionForBadge();
}
