package org.jenkinsci.plugins.gitflow.cause;

import static org.jenkinsci.plugins.gitflow.GitflowBuildWrapper.getGitflowBuildWrapperDescriptor;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;
import org.semver.Version;

/**
 * The {@link hudson.model.Cause Cause} object for the <i>Start Hotfix</i> action to be executed.
 *
 * @author Hannes Osius, Silpion IT-Solutions GmbH - osius@silpion.de
 */
public class StartHotfixCause extends AbstractGitflowCause {

    private final String hotfixVersion;
    private final String publishedPatchReleaseVersion;

    private String nextPatchDevelopmentVersion;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param masterBranch the <i>develop</i> branch containing base data for the cause.
     */
    public StartHotfixCause(final RemoteBranch masterBranch) {
        assert "master".equals(getGitflowBuildWrapperDescriptor().getBranchType(masterBranch.getBranchName()));

        final Version semverLastReleaseVersion = Version.parse(masterBranch.getLastReleaseVersion());
        this.publishedPatchReleaseVersion = semverLastReleaseVersion.toString();
        this.hotfixVersion = StringUtils.substringBeforeLast(this.publishedPatchReleaseVersion, ".");

        // Unfortunately the Semantic Versioning library (currently) cannot add the SNAPSHOT version suffix itself.
        this.nextPatchDevelopmentVersion = semverLastReleaseVersion.next(Version.Element.PATCH) + MAVEN_SNAPSHOT_VERSION_SUFFIX;
    }

    /** {@inheritDoc} */
    @Override
    public String getVersionForBadge() {
        return this.hotfixVersion;
    }

    public String getHotfixBranch() {
        return getGitflowBuildWrapperDescriptor().getHotfixBranchPrefix() + this.hotfixVersion;
    }

    public String getHotfixVersion() {
        return this.hotfixVersion;
    }

    public String getPublishedPatchReleaseVersion() {
        return this.publishedPatchReleaseVersion;
    }

    public String getNextPatchDevelopmentVersion() {
        return this.nextPatchDevelopmentVersion;
    }

    public void setNextPatchDevelopmentVersion(final String nextPatchDevelopmentVersion) {
        this.nextPatchDevelopmentVersion = nextPatchDevelopmentVersion;
    }
}
