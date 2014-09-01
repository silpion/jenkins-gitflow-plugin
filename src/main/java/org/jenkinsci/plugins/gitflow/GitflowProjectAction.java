package org.jenkinsci.plugins.gitflow;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gitflow.cause.AbstractGitflowCause;
import org.jenkinsci.plugins.gitflow.cause.FinishReleaseCause;
import org.jenkinsci.plugins.gitflow.cause.HotfixBranchCauseGroup;
import org.jenkinsci.plugins.gitflow.cause.PublishReleaseCause;
import org.jenkinsci.plugins.gitflow.cause.ReleaseBranchCauseGroup;
import org.jenkinsci.plugins.gitflow.cause.StartHotfixCause;
import org.jenkinsci.plugins.gitflow.cause.StartReleaseCause;
import org.jenkinsci.plugins.gitflow.cause.TestHotfixCause;
import org.jenkinsci.plugins.gitflow.cause.TestReleaseCause;
import org.jenkinsci.plugins.gitflow.data.GitflowPluginData;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;
import org.jenkinsci.plugins.gitflow.gitclient.GitClientDelegate;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import net.sf.json.JSONObject;

import com.google.common.annotations.VisibleForTesting;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.PermalinkProjectAction;
import hudson.model.StreamBuildListener;
import hudson.util.NullStream;

/**
 * The action that appears as link in the side bar of a project. Users will click on it in order to execute a Gitflow action.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GitflowProjectAction implements PermalinkProjectAction {

    @VisibleForTesting static final String JSON_PARAM_ACTION = "action";
    @VisibleForTesting static final String JSON_PARAM_VALUE = "value";
    @VisibleForTesting static final String JSON_PARAM_DRY_RUN = "dryRun";

    @VisibleForTesting static final String JSON_PARAM_RELEASE_VERSION = "releaseVersion";
    @VisibleForTesting static final String JSON_PARAM_NEXT_DEVELOPMENT_VERSION = "nextDevelopmentVersion";
    @VisibleForTesting static final String JSON_PARAM_RELEASE_NEXT_DEVELOPMENT_VERSION = "releaseNextDevelopmentVersion";
    @VisibleForTesting static final String JSON_PARAM_FIXES_RELEASE_VERSION = "fixesReleaseVersion";
    @VisibleForTesting static final String JSON_PARAM_NEXT_FIXES_DEVELOPMENT_VERSION = "nextFixesDevelopmentVersion";
    @VisibleForTesting static final String JSON_PARAM_MERGE_TO_DEVELOP = "mergeToDevelop";
    @VisibleForTesting static final String JSON_PARAM_INCLUDED_ACTION = "includedAction";
    @VisibleForTesting static final String JSON_PARAM_INCLUDE_START_HOTFIX_ACTION = "includeStartHotfixAction";
    @VisibleForTesting static final String JSON_PARAM_NEXT_HOTFIX_DEVELOPMENT_VERSION = "nextHotfixDevelopmentVersion";
    @VisibleForTesting static final String JSON_PARAM_HOTFIX_VERSION = "hotfixVersion";
    @VisibleForTesting static final String JSON_PARAM_HOTFIX_RELEASE_VERSION = "hotfixReleaseVersion";

    private static final Comparator<String> VERSION_NUMBER_COMPARATOR = new Comparator<String>() {

        public int compare(final String versionNumber1, final String versionNumber2) {
            int result = 0;

            final String[] versionNumberTokens1 = StringUtils.split(versionNumber1, ".");
            final String[] versionNumberTokens2 = StringUtils.split(versionNumber2, ".");

            for (int i = 0; i < Math.min(versionNumberTokens1.length, versionNumberTokens2.length); i++) {
                result = Integer.compare(Integer.valueOf(versionNumberTokens1[i]).intValue(), Integer.valueOf(versionNumberTokens2[i]).intValue());
                if (result != 0) {
                    break;
                }
            }

            if (result == 0) {
                result = Integer.compare(versionNumberTokens1.length, versionNumberTokens2.length);
            }

            return result;
        }
    };

    private final AbstractProject<?, ?> job;

    private StartReleaseCause startReleaseCause;
    private Map<String, ReleaseBranchCauseGroup> releaseBranchCauseGroupsByVersion = new TreeMap<String, ReleaseBranchCauseGroup>(VERSION_NUMBER_COMPARATOR);

    private StartHotfixCause startHotfixCause;
    private Map<String, HotfixBranchCauseGroup> hotfixBranchCauseGroupsByVersion = new TreeMap<String, HotfixBranchCauseGroup>(VERSION_NUMBER_COMPARATOR);

    /**
     * Initialises a new {@link GitflowProjectAction}.
     *
     * @param job the job/project that the Gitflow actions can be applied to.
     */
    public GitflowProjectAction(final AbstractProject<?, ?> job) {
        this.job = job;

        // Try to get the action object that holds the data for the Gitflow plugin and extract the recorded remote branch information.
        for (AbstractBuild<?, ?> lastBuild = job.getLastBuild(); lastBuild != null; lastBuild = lastBuild.getPreviousBuild()) {
            final GitflowPluginData gitflowPluginData = lastBuild.getAction(GitflowPluginData.class);
            if (gitflowPluginData != null) {

                // The action form should only offer actions on the recorded remote branches that still exist.
                // NOTE that proper error handling for Git client problems is not possible here. That's why the methods
                // 'createGitClient' and 'isExistingBlessedRemoteBranch' swallow exceptions instead of handling them in any way.
                final GitClientDelegate git = createGitClient(job);
                for (final RemoteBranch remoteBranch : gitflowPluginData.getRemoteBranches()) {
                    final String remoteAlias = remoteBranch.getRemoteAlias();
                    final String branchName = remoteBranch.getBranchName();
                    if (git == null || isExistingBlessedRemoteBranch(git, remoteAlias, branchName)) {

                        final String branchType = GitflowBuildWrapper.getGitflowBuildWrapperDescriptor().getBranchType(branchName);
                        if ("develop".equals(branchType)) {
                            this.startReleaseCause = new StartReleaseCause(remoteBranch);
                        } else if ("release".equals(branchType)) {
                            final ReleaseBranchCauseGroup releaseBranchCauseGroup = new ReleaseBranchCauseGroup(remoteBranch);
                            this.releaseBranchCauseGroupsByVersion.put(releaseBranchCauseGroup.getReleaseVersion(), releaseBranchCauseGroup);
                        } else if ("master".equals(branchType)) {
                            // When the master branch has a snapshot version, we assume an initial commit and not a published release.
                            if (!StringUtils.endsWith(remoteBranch.getLastBuildVersion(), "-SNAPSHOT")) {
                                this.startHotfixCause = new StartHotfixCause(remoteBranch);
                            }
                        } else if ("hotfix".equals(branchType)) {
                            final HotfixBranchCauseGroup hotfixBranchCauseGroup = new HotfixBranchCauseGroup(remoteBranch);
                            this.hotfixBranchCauseGroupsByVersion.put(hotfixBranchCauseGroup.getHotfixVersion(), hotfixBranchCauseGroup);
                        }
                    }
                }

                break;
            }
        }

        // Set startHotfixCause to null when the hotfix branch for the published release already exists.
        if (this.startHotfixCause != null && this.hotfixBranchCauseGroupsByVersion.containsKey(this.startHotfixCause.getHotfixReleaseVersion())) {
            this.startHotfixCause = null;
        }
    }

    private static GitClientDelegate createGitClient(final AbstractProject<?, ?> job) {
        try {
            return new GitClientDelegate(job.getLastBuild(), new StreamBuildListener(new NullStream()));
        } catch (final Exception ignored) {
            // NOTE that proper error handling for Git client problems is not possible here.
            // That's why exceptions are swallowed instead of being handled in any way.
            return null;
        }
    }

    private static boolean isExistingBlessedRemoteBranch(final GitClientDelegate git, final String remoteAlias, final String branchName) {
        try {
            return "origin".equals(remoteAlias) && git.getHeadRev(git.getRemoteUrl(remoteAlias), branchName) != null;
        } catch (final Exception ignored) {
            // NOTE that proper error handling for Git client problems is not possible here.
            // That's why exceptions are swallowed instead of being handled in any way.
            return true;
        }
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

        // TODO Validate that the versions for the selected action are not empty and don't equal DEFAULT_STRING.

        // Identify the cause object for the selected action and overwrite the fields that can be changed by the user.
        final JSONObject submittedForm = request.getSubmittedForm();
        final JSONObject submittedAction = submittedForm.getJSONObject(JSON_PARAM_ACTION);
        final String action = submittedAction.getString(JSON_PARAM_VALUE);
        final AbstractGitflowCause gitflowCause;
        if ("startRelease".equals(action)) {
            this.startReleaseCause.setReleaseVersion(submittedAction.getString(JSON_PARAM_RELEASE_VERSION));
            this.startReleaseCause.setReleaseNextDevelopmentVersion(submittedAction.getString(JSON_PARAM_RELEASE_NEXT_DEVELOPMENT_VERSION));
            this.startReleaseCause.setNextDevelopmentVersion(submittedAction.getString(JSON_PARAM_NEXT_DEVELOPMENT_VERSION));
            gitflowCause = this.startReleaseCause;
        } else if ("testRelease".equals(action)) {
            final TestReleaseCause testReleaseCause = this.releaseBranchCauseGroupsByVersion.get(submittedAction.getString(JSON_PARAM_RELEASE_VERSION)).getTestReleaseCause();
            testReleaseCause.setFixesReleaseVersion(submittedAction.getString(JSON_PARAM_FIXES_RELEASE_VERSION));
            testReleaseCause.setNextFixesDevelopmentVersion(submittedAction.getString(JSON_PARAM_NEXT_FIXES_DEVELOPMENT_VERSION));
            gitflowCause = testReleaseCause;
        } else if ("publishRelease".equals(action)) {
            final PublishReleaseCause publishReleaseCause = this.releaseBranchCauseGroupsByVersion.get(submittedAction.getString(JSON_PARAM_RELEASE_VERSION)).getPublishReleaseCause();
            publishReleaseCause.setMergeToDevelop(submittedAction.getBoolean(JSON_PARAM_MERGE_TO_DEVELOP));
            publishReleaseCause.setIncludedAction(submittedAction.getString(JSON_PARAM_INCLUDED_ACTION));
            gitflowCause = publishReleaseCause;
        } else if ("finishRelease".equals(action)) {
            final FinishReleaseCause finishReleaseCause = this.releaseBranchCauseGroupsByVersion.get(submittedAction.getString(JSON_PARAM_RELEASE_VERSION)).getFinishReleaseCause();
            finishReleaseCause.setIncludeStartHotfixAction(submittedAction.getBoolean(JSON_PARAM_INCLUDE_START_HOTFIX_ACTION));
            gitflowCause = finishReleaseCause;
        } else if ("startHotfix".equals(action)) {
            this.startHotfixCause.setNextHotfixDevelopmentVersion(submittedAction.getString(JSON_PARAM_NEXT_HOTFIX_DEVELOPMENT_VERSION));
            gitflowCause = this.startHotfixCause;
        } else if ("testHotfix".equals(action)) {
            final TestHotfixCause testHotfixCause = this.hotfixBranchCauseGroupsByVersion.get(submittedAction.getString(JSON_PARAM_HOTFIX_VERSION)).getTestHotfixCause();
            testHotfixCause.setHotfixReleaseVersion(submittedAction.getString(JSON_PARAM_HOTFIX_RELEASE_VERSION));
            testHotfixCause.setNextHotfixDevelopmentVersion(submittedAction.getString(JSON_PARAM_NEXT_HOTFIX_DEVELOPMENT_VERSION));
            gitflowCause = testHotfixCause;
        } else if ("finishHotfix".equals(action)) {
            gitflowCause = this.hotfixBranchCauseGroupsByVersion.get(submittedAction.getString(JSON_PARAM_HOTFIX_VERSION)).getFinishHotfixCause();
        } else {
            // Only an IOException causes the build to fail properly.
            throw new IOException("Unknown Gitflow action " + action);
        }
        gitflowCause.setDryRun(submittedForm.getBoolean(JSON_PARAM_DRY_RUN));

        // Start a build.
        this.job.scheduleBuild(0, gitflowCause);

        // Return to the main page of the job.
        response.sendRedirect(request.getContextPath() + '/' + this.job.getUrl());
    }

    @SuppressWarnings("UnusedDeclaration")
    public StartReleaseCause getStartReleaseCause() {
        return this.startReleaseCause;
    }

    @SuppressWarnings("UnusedDeclaration")
    public Collection<ReleaseBranchCauseGroup> getReleaseBranchCauseGroups() {
        return this.releaseBranchCauseGroupsByVersion.values();
    }

    @SuppressWarnings("UnusedDeclaration")
    public StartHotfixCause getStartHotfixCause() {
        return this.startHotfixCause;
    }

    @SuppressWarnings("UnusedDeclaration")
    public Collection<HotfixBranchCauseGroup> getHotfixBranchCauseGroups() {
        return this.hotfixBranchCauseGroupsByVersion.values();
    }
}
