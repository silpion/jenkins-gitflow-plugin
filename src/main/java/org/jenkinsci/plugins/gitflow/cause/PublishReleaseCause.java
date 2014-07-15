package org.jenkinsci.plugins.gitflow.cause;

import net.sf.json.JSONObject;

/**
 * The {@link hudson.model.Cause} object for the <i>Publish Release</i> action to be executed.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class PublishReleaseCause extends AbstractGitflowCause {

    private static final String PARAM_RELEASE = "release";
    private static final String PARAM_RELEASE_BRANCH = "releaseBranch";
    private static final String PARAM_LAST_FIXES_RELEASE_VERSION = "lastFixesReleaseVersion";
    private static final String PARAM_LAST_FIXES_RELEASE_COMMIT = "lastFixesReleaseCommit";

    private final String releaseBranch;
    private final String lastFixesReleaseVersion;
    private final String lastFixesReleaseCommit;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param structuredActionConent the structured content for the selected action to be instanciated.
     * @param dryRun is the build dryRun or not
     */
    public PublishReleaseCause(final JSONObject structuredActionConent, final boolean dryRun) {
        super(dryRun);

        final JSONObject releaseContent = structuredActionConent.getJSONObject(PARAM_RELEASE);
        this.releaseBranch = releaseContent.getString(PARAM_RELEASE_BRANCH);
        this.lastFixesReleaseVersion = releaseContent.getString(PARAM_LAST_FIXES_RELEASE_VERSION);
        this.lastFixesReleaseCommit = releaseContent.getString(PARAM_LAST_FIXES_RELEASE_COMMIT);
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
}
