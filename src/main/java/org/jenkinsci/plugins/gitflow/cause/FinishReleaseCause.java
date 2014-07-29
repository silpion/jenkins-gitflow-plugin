package org.jenkinsci.plugins.gitflow.cause;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gitflow.GitflowBuildWrapper;

import net.sf.json.JSONObject;

import jenkins.model.Jenkins;

/**
 * The {@link hudson.model.Cause} object for the <i>Finish Release</i> action to be executed.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class FinishReleaseCause extends AbstractGitflowCause {

    /** The actions that can be included/executed after the main <i>Finish Release</i> action. */
    public static enum IncludedAction {
        NONE,
        FINISH_RELEASE,
        START_HOTFIX
    }

    private static final String PARAM_RELEASE = "release";
    private static final String PARAM_RELEASE_BRANCH = "releaseBranch";
    private static final String PARAM_INCLUDE_START_HOTFIX_ACTION = "includeStartHotfixAction";

    private final String releaseBranch;
    private final boolean includeStartHotfixAction;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param structuredActionConent the structured content for the selected action to be instanciated.
     * @param dryRun is the build dryRun or not
     */
    public FinishReleaseCause(final JSONObject structuredActionConent, final boolean dryRun) {
        super(dryRun);

        final JSONObject releaseContent = structuredActionConent.getJSONObject(PARAM_RELEASE);
        this.releaseBranch = releaseContent.getString(PARAM_RELEASE_BRANCH);
        this.includeStartHotfixAction = releaseContent.getBoolean(PARAM_INCLUDE_START_HOTFIX_ACTION);
    }

    @Override
    public String getVersionForBadge() {
        final Jenkins jenkins = Jenkins.getInstance();
        final GitflowBuildWrapper.DescriptorImpl buildWrapperDescriptor = (GitflowBuildWrapper.DescriptorImpl) jenkins.getDescriptor(GitflowBuildWrapper.class);
        return StringUtils.removeStart(this.releaseBranch, buildWrapperDescriptor.getReleaseBranchPrefix());
    }

    public String getReleaseBranch() {
        return this.releaseBranch;
    }

    public boolean isIncludeStartHotfixAction() {
        return this.includeStartHotfixAction;
    }
}
