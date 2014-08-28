package org.jenkinsci.plugins.gitflow.cause;

import net.sf.json.JSONObject;

/**
 * The {@link hudson.model.Cause} object for the <i>Publish Release</i> action to be executed.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class PublishReleaseCause extends AbstractGitflowCause {

    /** The actions that can be included/executed after the main <i>Publish Release</i> action. */
    public static enum IncludedAction {
        NONE,
        FINISH_RELEASE,
        START_HOTFIX
    }

    private static final String PARAM_RELEASE = "release";
    private static final String PARAM_RELEASE_BRANCH = "releaseBranch";
    private static final String PARAM_LAST_FIXES_RELEASE_VERSION = "lastFixesReleaseVersion";
    private static final String PARAM_LAST_FIXES_RELEASE_COMMIT = "lastFixesReleaseCommit";
    private static final String PARAM_MERGE_TO_DEVELOP = "mergeToDevelop";
    private static final String PARAM_INCLUDED_ACTION = "includedAction";

    private final String releaseBranch;
    private final String lastFixesReleaseVersion;
    private final String lastFixesReleaseCommit;
    private final boolean mergeToDevelop;
    private final IncludedAction includedAction;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param structuredActionConent the structured content for the selected action to be instanciated.
     * @param dryRun is the build dryRun or not
     */
    public PublishReleaseCause(final JSONObject structuredActionConent, final boolean dryRun) {
        this.setDryRun(dryRun);

        final JSONObject releaseContent = structuredActionConent.getJSONObject(PARAM_RELEASE);
        this.releaseBranch = releaseContent.getString(PARAM_RELEASE_BRANCH);
        this.lastFixesReleaseVersion = releaseContent.getString(PARAM_LAST_FIXES_RELEASE_VERSION);
        this.lastFixesReleaseCommit = releaseContent.getString(PARAM_LAST_FIXES_RELEASE_COMMIT);
        this.mergeToDevelop = releaseContent.getBoolean(PARAM_MERGE_TO_DEVELOP);
        this.includedAction = IncludedAction.valueOf(releaseContent.getString(PARAM_INCLUDED_ACTION));
    }

    @Override
    public String getVersionForBadge() {
        return this.lastFixesReleaseVersion;
    }

    public String getReleaseBranch() {
        return this.releaseBranch;
    }

    public String getLastFixesReleaseVersion() {
        return this.lastFixesReleaseVersion;
    }

    public String getLastFixesReleaseCommit() {
        return this.lastFixesReleaseCommit;
    }

    public boolean isMergeToDevelop() {
        return this.mergeToDevelop;
    }

    public IncludedAction getIncludedAction() {
        return this.includedAction;
    }
}
