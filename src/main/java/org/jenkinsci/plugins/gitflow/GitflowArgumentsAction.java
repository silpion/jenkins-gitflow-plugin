package org.jenkinsci.plugins.gitflow;

import hudson.model.Action;

/**
 * Action attached to the build that will record the settings for the action to be executed.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GitflowArgumentsAction implements Action {

    private String releaseVersion;

    public String getIconFileName() {
        // no icon.
        return null;
    }

    public String getDisplayName() {
        // don't display
        return null;
    }

    public String getUrlName() {
        // no url
        return null;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public void setReleaseVersion(String releaseVersion) {
        this.releaseVersion = releaseVersion;
    }
}
