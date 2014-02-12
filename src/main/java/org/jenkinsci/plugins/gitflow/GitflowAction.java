package org.jenkinsci.plugins.gitflow;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hudson.model.AbstractProject;
import hudson.model.Cause;
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

        // Log the form field values.
        final Map parameterMap = request.getParameterMap();
        for (final Object entry : parameterMap.entrySet()) {
            final Map.Entry<String, String[]> paramEntry = (Map.Entry<String, String[]>) entry;
            this.log.info("Submitted action param '" + paramEntry.getKey() + "': " + Arrays.asList(paramEntry.getValue()));
        }

        // TODO Validate that the releaseVersion is not empty.

        // Record the settings for the action to be executed.
        final GitflowArgumentsAction gitflowArgumentsAction = new GitflowArgumentsAction();
        gitflowArgumentsAction.setReleaseVersion(request.getParameter("startRelease_releaseVersion"));
        gitflowArgumentsAction.setNextDevelopmentVersion(request.getParameter("startRelease_nextDevelopmentVersion"));

        // Start a build.
        this.job.scheduleBuild(0, new Cause.UserIdCause(), gitflowArgumentsAction);

        // Return to the main page of the job.
        response.sendRedirect(request.getContextPath() + '/' + this.job.getUrl());
    }
}
