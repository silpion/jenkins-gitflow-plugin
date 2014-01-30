package org.jenkinsci.plugins.gitflow;

import java.util.Collections;
import java.util.List;

import hudson.model.AbstractProject;
import hudson.model.PermalinkProjectAction;

/**
 * The action appears as the link in the side bar that users will click on in order to create a Git release.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GitflowReleaseAction implements PermalinkProjectAction {

    private AbstractProject<?, ?> job;

    public GitflowReleaseAction(final AbstractProject<?, ?> job) {
        this.job = job;
    }

    public List<Permalink> getPermalinks() {
        return Collections.emptyList();
    }

    public String getIconFileName() {
        if (GitflowBuildWrapper.hasReleasePermission(this.job)) {
            return "/plugin/" + this.getUrlName() + "/images/24x24/gitflow.png";
        } else {
            return null;
        }
    }

    public String getDisplayName() {
        return "Gitflow";
    }

    public String getUrlName() {
        return "gitflow";
    }
}
