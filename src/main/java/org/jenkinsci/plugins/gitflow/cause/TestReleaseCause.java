package org.jenkinsci.plugins.gitflow.cause;

import org.jenkinsci.plugins.gitflow.data.RemoteBranch;
import org.semver.Version;

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

        final Version semverPatchReleaseVersion = Version.parse(releaseBranch.getLastBuildVersion()).toReleaseVersion();
        this.patchReleaseVersion = semverPatchReleaseVersion.toString();

        // Unfortunately the Semantic Versioning library (currently) cannot add the SNAPSHOT version suffix itself.
        this.nextPatchDevelopmentVersion = semverPatchReleaseVersion.next(Version.Element.PATCH) + MAVEN_SNAPSHOT_VERSION_SUFFIX;
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
