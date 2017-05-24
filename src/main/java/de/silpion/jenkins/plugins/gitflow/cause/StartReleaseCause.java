package de.silpion.jenkins.plugins.gitflow.cause;

import static de.silpion.jenkins.plugins.gitflow.GitflowBuildWrapper.getGitflowBuildWrapperDescriptor;

import de.silpion.jenkins.plugins.gitflow.data.RemoteBranch;
import org.apache.commons.lang.StringUtils;
import org.semver.Version;

/**
 * The {@link hudson.model.Cause Cause} object for the <i>Start Release</i> action to be executed.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class StartReleaseCause extends AbstractGitflowCause {

    private String releaseVersion;
    private String nextPatchDevelopmentVersion;
    private String nextReleaseDevelopmentVersion;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param developBranch the <i>develop</i> branch containing base data for the cause.
     */
    public StartReleaseCause(final RemoteBranch developBranch) {
        assert "develop".equals(getGitflowBuildWrapperDescriptor().getBranchType(developBranch.getBranchName()));

        final Version semverReleaseVersion = Version.parse(developBranch.getLastBuildVersion()).toReleaseVersion();
        this.releaseVersion = semverReleaseVersion.toString();

        // Unfortunately the Semantic Versioning library (currently) cannot add the SNAPSHOT version suffix itself.
        this.nextPatchDevelopmentVersion = semverReleaseVersion.next(Version.Element.PATCH).toString() + MAVEN_SNAPSHOT_VERSION_SUFFIX;
        this.nextReleaseDevelopmentVersion = semverReleaseVersion.next(Version.Element.MINOR).toString() + MAVEN_SNAPSHOT_VERSION_SUFFIX;
    }

    @Override
    public String getVersionForBadge() {
        return this.releaseVersion;
    }

    public String getReleaseBranch() {
        return getGitflowBuildWrapperDescriptor().getReleaseBranchPrefix() + StringUtils.substringBeforeLast(this.releaseVersion, ".");
    }

    public String getReleaseVersion() {
        return this.releaseVersion;
    }

    public void setReleaseVersion(final String releaseVersion) {
        this.releaseVersion = releaseVersion;
    }

    public String getNextPatchDevelopmentVersion() {
        return this.nextPatchDevelopmentVersion;
    }

    public void setNextPatchDevelopmentVersion(final String nextPatchDevelopmentVersion) {
        this.nextPatchDevelopmentVersion = nextPatchDevelopmentVersion;
    }

    public String getNextReleaseDevelopmentVersion() {
        return this.nextReleaseDevelopmentVersion;
    }

    public void setNextReleaseDevelopmentVersion(final String nextReleaseDevelopmentVersion) {
        this.nextReleaseDevelopmentVersion = nextReleaseDevelopmentVersion;
    }
}
