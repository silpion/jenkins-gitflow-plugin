package org.jenkinsci.plugins.gitflow.cause;

import java.io.IOException;

import net.sf.json.JSONObject;

/**
 * The {@link hudson.model.Cause} object for the <i>Test Release</i> action to be executed.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class TestReleaseCause extends AbstractGitflowCause {

    private static final String PARAM_RELEASE = "release";
    private static final String PARAM_RELEASE_BRANCH = "releaseBranch";
    private static final String PARAM_FIXES_RELEASE_VERSION = "fixesReleaseVersion";
    private static final String PARAM_NEXT_FIXES_DEVELOPMENT_VERSION = "nextFixesDevelopmentVersion";

    private final String releaseBranch;
    private final String fixesReleaseVersion;
    private final String nextFixesDevelopmentVersion;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param structuredActionConent the structured content for the selected action to be instanciated.
     * @return a new cause instance for the <i>Gitflow</i> build.
     */
    public TestReleaseCause(final JSONObject structuredActionConent) throws IOException {
        super();

        final JSONObject releaseContent = structuredActionConent.getJSONObject(PARAM_RELEASE);
        this.releaseBranch = releaseContent.getString(PARAM_RELEASE_BRANCH);
        this.fixesReleaseVersion = releaseContent.getString(PARAM_FIXES_RELEASE_VERSION);
        this.nextFixesDevelopmentVersion = releaseContent.getString(PARAM_NEXT_FIXES_DEVELOPMENT_VERSION);
    }

    public String getReleaseBranch() {
        return this.releaseBranch;
    }

    public String getFixesReleaseVersion() {
        return this.fixesReleaseVersion;
    }

    public String getNextFixesDevelopmentVersion() {
        return this.nextFixesDevelopmentVersion;
    }
}
