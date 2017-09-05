package de.silpion.jenkins.plugins.gitflow.cause;

import de.silpion.jenkins.plugins.gitflow.data.RemoteBranch;
import org.semver.Version;

/**
 * The {@link hudson.model.Cause Cause} object for the <i>Test Hotfix</i> action to be executed.
 *
 * @author Hannes Osius, Silpion IT-Solutions GmbH - osius@silpion.de
 */
public class TestHotfixCause extends AbstractHotfixBranchCause {

    private String patchReleaseVersion;
    private String nextPatchDevelopmentVersion;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param hotfixBranch the <i>hotfix</i> branch containing base data for the cause.
     */
    public TestHotfixCause(final RemoteBranch hotfixBranch) {
        super(hotfixBranch, false);

        final Version semverPatchReleaseVersion = Version.parse(hotfixBranch.getLastBuildVersion()).toReleaseVersion();
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
