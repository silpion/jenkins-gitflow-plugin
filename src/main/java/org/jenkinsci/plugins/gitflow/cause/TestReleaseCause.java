package org.jenkinsci.plugins.gitflow.cause;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;

/**
 * The {@link hudson.model.Cause Cause} object for the <i>Test Release</i> action to be executed.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class TestReleaseCause extends AbstractReleaseBranchCause {

    private String patchReleaseVersion;
    private String nextPatchDevelopmentVersion;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param releaseBranch the <i>release</i> branch containing base data for the cause.
     */
    public TestReleaseCause(final RemoteBranch releaseBranch) {
        super(releaseBranch);

        this.patchReleaseVersion = StringUtils.removeEnd(releaseBranch.getLastBuildVersion(), "-SNAPSHOT");

        final String baseVersion = StringUtils.substringBeforeLast(this.patchReleaseVersion, ".");
        final int newPatchNumber = Integer.valueOf(StringUtils.substringAfterLast(this.patchReleaseVersion, ".")).intValue() + 1;
        this.nextPatchDevelopmentVersion = baseVersion + "." + newPatchNumber + "-SNAPSHOT";
    }

    @Override
    public String getVersionForBadge() {
        return this.patchReleaseVersion;
    }

    public String getPatchReleaseVersion() {
        return this.patchReleaseVersion;
    }

    public void setPatchReleaseVersion(final String patchReleaseVersion) {
        this.patchReleaseVersion = patchReleaseVersion;
    }

    public String getNextPatchDevelopmentVersion() {
        return this.nextPatchDevelopmentVersion;
    }

    public void setNextPatchDevelopmentVersion(final String nextPatchDevelopmentVersion) {
        this.nextPatchDevelopmentVersion = nextPatchDevelopmentVersion;
    }
}
