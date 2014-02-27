package org.jenkinsci.plugins.gitflow;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.model.AbstractProject;
import hudson.model.PermalinkProjectAction;

/**
 * The action that appears as link in the side bar of a project. Users will click on it in order to execute a Gitflow action.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GitflowProjectAction implements PermalinkProjectAction {

    private static final String DEFAULT_STRING = "Please enter a valid version number...";

    private final AbstractProject<?, ?> job;
    private final GitflowPluginProperties gitflowPluginProperties;

    /**
     * Initialises a new {@link GitflowProjectAction}.
     *
     * @param job the job/project that the Gitflow actions can be applied to.
     */
    public GitflowProjectAction(final AbstractProject<?, ?> job) {
        this.job = job;
        this.gitflowPluginProperties = new GitflowPluginProperties(job);
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

    public String computeReleaseVersion() throws IOException {
        final String developVersion = this.gitflowPluginProperties.loadVersionForBranch("develop");
        if (StringUtils.isBlank(developVersion)) {
            return DEFAULT_STRING;
        } else {
            return StringUtils.removeEnd(developVersion, "-SNAPSHOT");
        }
    }

    public String computeReleaseNextDevelopmentVersion() throws IOException {
        final String releaseVersion = this.computeReleaseVersion();
        if (StringUtils.equals(releaseVersion, DEFAULT_STRING)) {
            return DEFAULT_STRING;
        } else {
            return releaseVersion + ".1-SNAPSHOT";
        }
    }

    public String computeNextDevelopmentVersion() throws IOException {
        final String releaseVersion = this.computeReleaseVersion();
        if (StringUtils.equals(releaseVersion, DEFAULT_STRING)) {
            return DEFAULT_STRING;
        } else {
            final String latestMinorVersion = StringUtils.substringAfterLast(releaseVersion, ".");
            final int nextMinorVersion = Integer.valueOf(latestMinorVersion).intValue() + 1;

            final String baseVersion = StringUtils.substringBeforeLast(releaseVersion, ".");
            return baseVersion + "." + nextMinorVersion + "-SNAPSHOT";
        }
    }

    public void doSubmit(final StaplerRequest request, final StaplerResponse response) throws IOException {

        final String action = request.getParameter("action");

        // TODO Validate that the versions for the selected action are not empty and don't equal DEFAULT_STRING.

        // Record the settings for the action to be executed.
        final Map<String, String> actionParams = new HashMap<String, String>();
        for (final Object actionParamEntryObject : request.getParameterMap().entrySet()) {
            @SuppressWarnings("unchecked")
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
