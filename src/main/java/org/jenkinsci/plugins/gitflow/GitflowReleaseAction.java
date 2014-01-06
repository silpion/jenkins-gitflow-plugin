package org.jenkinsci.plugins.gitflow;

import java.util.List;

import hudson.model.PermalinkProjectAction;

/**
 * The action appears as the link in the side bar that users will click on in order to create a Git release.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GitflowReleaseAction implements PermalinkProjectAction {
    public List<Permalink> getPermalinks() {
        return null;
    }

    public String getIconFileName() {
        return "installer.gif";
    }

    public String getDisplayName() {
        return "Gitflow";
    }

    public String getUrlName() {
        return "gitflow";
    }
}
