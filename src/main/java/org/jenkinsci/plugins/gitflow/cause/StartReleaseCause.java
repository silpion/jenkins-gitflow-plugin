package org.jenkinsci.plugins.gitflow.cause;

import java.io.IOException;

import net.sf.json.JSONObject;

/**
 * The {@link hudson.model.Cause} object for the <i>Start Release</i> action to be executed.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class StartReleaseCause extends AbstractGitflowCause {

    private static final String PARAM_RELEASE_VERSION = "releaseVersion";
    private static final String PARAM_NEXT_DEVELOPMENT_VERSION = "nextDevelopmentVersion";
    private static final String PARAM_RELEASE_NEXT_DEVELOPMENT_VERSION = "releaseNextDevelopmentVersion";

    private final String releaseBranch;
    private final String releaseVersion;
    private final String releaseNextDevelopmentVersion;
    private final String nextDevelopmentVersion;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param structuredActionConent the structured content for the selected action to be instanciated.
     * @return a new cause instance for the <i>Gitflow</i> build.
     */
    public StartReleaseCause(final JSONObject structuredActionConent) throws IOException {
        super();

        this.releaseVersion = structuredActionConent.getString(PARAM_RELEASE_VERSION);
        this.releaseNextDevelopmentVersion = structuredActionConent.getString(PARAM_RELEASE_NEXT_DEVELOPMENT_VERSION);
        this.nextDevelopmentVersion = structuredActionConent.getString(PARAM_NEXT_DEVELOPMENT_VERSION);

        this.releaseBranch = getBuildWrapperDescriptor().getReleaseBranchPrefix() + this.releaseVersion;
    }

    public String getReleaseBranch() {
        return this.releaseBranch;
    }

    public String getReleaseVersion() {
        return this.releaseVersion;
    }

    public String getReleaseNextDevelopmentVersion() {
        return this.releaseNextDevelopmentVersion;
    }

    public String getNextDevelopmentVersion() {
        return this.nextDevelopmentVersion;
    }
}
