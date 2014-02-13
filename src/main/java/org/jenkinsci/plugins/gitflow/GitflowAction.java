package org.jenkinsci.plugins.gitflow;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hudson.model.AbstractProject;
import hudson.model.PermalinkProjectAction;

/**
 * The action appears as the link in the side bar that users will click on in order to execute a Gitflow action.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GitflowAction implements PermalinkProjectAction {

    private transient Logger log = LoggerFactory.getLogger(GitflowAction.class);

    private AbstractProject<?, ?> job;

    public GitflowAction(final AbstractProject<?, ?> job) {
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

    @SuppressWarnings("UnusedDeclaration")
    public void doSubmit(final StaplerRequest request, final StaplerResponse response) throws IOException, ServletException {

        // TODO Validate that the releaseVersion is not empty.

        final String action = request.getParameter("action");

        // Record the settings for the action to be executed.
        final Map<String, String> actionParams = new HashMap<String, String>();
        for (final Object actionParamEntryObject : request.getParameterMap().entrySet()) {
            final Map.Entry<String, String[]> actionParamEntry = (Map.Entry<String, String[]>) actionParamEntryObject;
            final String actionParamCombinedKey = actionParamEntry.getKey();
            if (action.equals(StringUtils.substringBefore(actionParamCombinedKey, "_"))) {
                final String actionParamKey = StringUtils.substringAfter(actionParamCombinedKey, "_");
                final String[] actionParamValues = actionParamEntry.getValue();
                final String actionParamValue = ArrayUtils.isEmpty(actionParamValues) ? "" : actionParamValues[0];
                actionParams.put(actionParamKey, actionParamValue);
            }
        }

        // Start a build.
        this.job.scheduleBuild(0, new GitflowCause(action, actionParams));

        // Return to the main page of the job.
        response.sendRedirect(request.getContextPath() + '/' + this.job.getUrl());
    }
}
