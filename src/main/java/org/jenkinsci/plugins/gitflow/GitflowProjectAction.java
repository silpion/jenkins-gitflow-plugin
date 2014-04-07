package org.jenkinsci.plugins.gitflow;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gitflow.cause.AbstractGitflowCause;
import org.jenkinsci.plugins.gitflow.cause.GitflowCauseFactory;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.model.AbstractProject;
import hudson.model.PermalinkProjectAction;

import jenkins.model.Jenkins;

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

    private static GitflowBuildWrapper.DescriptorImpl getBuildWrapperDescriptor() {
        return (GitflowBuildWrapper.DescriptorImpl) Jenkins.getInstance().getDescriptor(GitflowBuildWrapper.class);
    }

    public String computeReleaseVersion() throws IOException {
        final String developVersion = this.gitflowPluginProperties.loadVersionForBranch(getBuildWrapperDescriptor().getDevelopBranch());
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

    public SortedSet<String> computeReleaseBranches() throws IOException {
        final SortedSet<String> releaseBranches = new TreeSet<String>();

        final String releaseBranchPrefix = getBuildWrapperDescriptor().getReleaseBranchPrefix();

        for (final String branch : this.gitflowPluginProperties.loadBranches()) {
            if (StringUtils.startsWith(branch, releaseBranchPrefix)) {
                releaseBranches.add(branch);
            }
        }

        return releaseBranches;
    }

    public String computeReleaseVersion(final String releaseBranch) throws IOException {
        final String releaseBranchPrefix = getBuildWrapperDescriptor().getReleaseBranchPrefix();
        return StringUtils.removeStart(releaseBranch, releaseBranchPrefix);
    }

    public String computeFixesReleaseVersion(final String releaseBranch) throws IOException {
        final String versionForBranch = this.gitflowPluginProperties.loadVersionForBranch(releaseBranch);
        return StringUtils.removeEnd(versionForBranch, "-SNAPSHOT");
    }

    public String computeNextFixesDevelopmentVersion(final String releaseBranch) throws IOException {
        final StringBuilder nextFixesDevelopmentVersionBuilder = new StringBuilder();

        final String fixesReleaseVersion = this.computeFixesReleaseVersion(releaseBranch);

        nextFixesDevelopmentVersionBuilder.append(StringUtils.substringBeforeLast(fixesReleaseVersion, "."));
        nextFixesDevelopmentVersionBuilder.append(".");
        nextFixesDevelopmentVersionBuilder.append(Integer.valueOf(StringUtils.substringAfterLast(fixesReleaseVersion, ".")).intValue() + 1);
        nextFixesDevelopmentVersionBuilder.append("-SNAPSHOT");

        return nextFixesDevelopmentVersionBuilder.toString();
    }

    public void doSubmit(final StaplerRequest request, final StaplerResponse response) throws IOException, ServletException {

        // TODO Validate that the versions for the selected action are not empty and don't equal DEFAULT_STRING.

        // Create the cause object for the selected action.
        final AbstractGitflowCause gitflowCause = GitflowCauseFactory.newInstance(request.getSubmittedForm());

        // Start a build.
        this.job.scheduleBuild(0, gitflowCause);

        // Return to the main page of the job.
        response.sendRedirect(request.getContextPath() + '/' + this.job.getUrl());
    }
}
