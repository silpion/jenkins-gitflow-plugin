package org.jenkinsci.plugins.gitflow.cause;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;

/**
 * The {@link hudson.model.Cause Cause} object for the <i>Test Release</i> action to be executed.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class TestReleaseCause extends AbstractReleaseBranchCause {

    private String fixesReleaseVersion;
    private String nextFixesDevelopmentVersion;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param releaseBranch the <i>release</i> branch containing base data for the cause.
     */
    public TestReleaseCause(final RemoteBranch releaseBranch) {
        super(releaseBranch);

        this.fixesReleaseVersion = StringUtils.removeEnd(releaseBranch.getLastBuildVersion(), "-SNAPSHOT");

        final String baseVersion = StringUtils.substringBeforeLast(this.fixesReleaseVersion, ".");
        final int newPatchNumber = Integer.valueOf(StringUtils.substringAfterLast(this.fixesReleaseVersion, ".")).intValue() + 1;
        this.nextFixesDevelopmentVersion = baseVersion + "." + newPatchNumber + "-SNAPSHOT";
    }

    @Override
    public String getVersionForBadge() {
        return this.fixesReleaseVersion;
    }

    public String getFixesReleaseVersion() {
        return this.fixesReleaseVersion;
    }

    public void setFixesReleaseVersion(final String fixesReleaseVersion) {
        this.fixesReleaseVersion = fixesReleaseVersion;
    }

    public String getNextFixesDevelopmentVersion() {
        return this.nextFixesDevelopmentVersion;
    }

    public void setNextFixesDevelopmentVersion(final String nextFixesDevelopmentVersion) {
        this.nextFixesDevelopmentVersion = nextFixesDevelopmentVersion;
    }
}
