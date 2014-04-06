package org.jenkinsci.plugins.gitflow.cause;

import java.io.IOException;
import java.util.Map;

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

    public StartReleaseCause(final Map<String, String> params) throws IOException {
        super();

        this.releaseVersion = getParameterValueAssertNotBlank(params, PARAM_RELEASE_VERSION);
        this.releaseNextDevelopmentVersion = getParameterValueAssertNotBlank(params, PARAM_RELEASE_NEXT_DEVELOPMENT_VERSION);
        this.nextDevelopmentVersion = getParameterValueAssertNotBlank(params, PARAM_NEXT_DEVELOPMENT_VERSION);

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
