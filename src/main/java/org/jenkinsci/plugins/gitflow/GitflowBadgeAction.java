package org.jenkinsci.plugins.gitflow;

import org.jenkinsci.plugins.gitflow.cause.AbstractGitflowCause;

import hudson.model.BuildBadgeAction;
import hudson.model.Run;

import jenkins.model.RunAction2;

public class GitflowBadgeAction implements BuildBadgeAction, RunAction2 {

    private transient Run<?, ?> run;

    private String gitflowActionName;

    /** {@inheritDoc} */
    public String getDisplayName() {
        return null;
    }

    /** {@inheritDoc} */
    public String getIconFileName() {
        return null;
    }

    /** {@inheritDoc} */
    public String getUrlName() {
        return null;
    }

    /**
     * Returns the tooltip text that should be displayed to the user.
     *
     * @return the tooltip text that should be displayed to the user.
     */
    public String getTooltipText() {
        final StringBuilder str = new StringBuilder();
        str.append(this.gitflowActionName);
        str.append(" ");
        str.append(this.getVersionForBadge());
        if (this.isDryRun()) {
            str.append(" (Dry Run)");
        }
        return str.toString();
    }

    public boolean isGitflowCause() {
        return this.run.getCause(AbstractGitflowCause.class) != null;
    }

    private String getVersionForBadge() {
        final AbstractGitflowCause gitflowCause = this.run.getCause(AbstractGitflowCause.class);
        if (gitflowCause == null) {
            return null;
        } else {
            return gitflowCause.getVersionForBadge();
        }
    }

    public boolean isDryRun() {
        final AbstractGitflowCause gitflowCause = this.run.getCause(AbstractGitflowCause.class);
        if (gitflowCause == null) {
            return false;
        } else {
            return gitflowCause.isDryRun();
        }
    }

    /** {@inheritDoc} */
    public void onAttached(final Run<?, ?> run) {
        this.run = run;
    }

    /** {@inheritDoc} */
    public void onLoad(final Run<?, ?> run) {
        this.run = run;
    }

    public String getGitflowActionName() {
        return this.gitflowActionName;
    }

    public void setGitflowActionName(final String gitflowActionName) {
        this.gitflowActionName = gitflowActionName;
    }
}
