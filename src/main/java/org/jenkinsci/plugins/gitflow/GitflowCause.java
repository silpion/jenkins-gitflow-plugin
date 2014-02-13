package org.jenkinsci.plugins.gitflow;

import java.util.Map;

import hudson.model.Cause;

/**
 * The {@link Cause} object for the executed Gitflow actions.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GitflowCause extends Cause {

    private final String action;
    private final Map<String, String> actionParams;

    public GitflowCause(final String action, final Map<String, String> actionParams) {
        this.action = action;
        this.actionParams = actionParams;
    }

    @Override
    public String getShortDescription() {
        return "Triggered by Gitflow Plugin";
    }

    public String getAction() {
        return this.action;
    }

    public Map<String, String> getActionParams() {
        return this.actionParams;
    }
}
