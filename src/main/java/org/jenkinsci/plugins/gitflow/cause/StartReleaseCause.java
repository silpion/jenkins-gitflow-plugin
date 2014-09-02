package org.jenkinsci.plugins.gitflow.cause;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gitflow.GitflowBuildWrapper;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;

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
        assert "develop".equals(GitflowBuildWrapper.getGitflowBuildWrapperDescriptor().getBranchType(developBranch.getBranchName()));

        this.releaseVersion = StringUtils.removeEnd(developBranch.getLastBuildVersion(), "-SNAPSHOT");
        this.nextPatchDevelopmentVersion = this.releaseVersion + ".1-SNAPSHOT";

        final String majorVersion = StringUtils.substringBeforeLast(this.releaseVersion, ".");
        final String latestMinorVersion = StringUtils.substringAfterLast(this.releaseVersion, ".");
        final int nextMinorVersion = Integer.valueOf(latestMinorVersion).intValue() + 1;
        this.nextReleaseDevelopmentVersion = majorVersion + "." + nextMinorVersion + "-SNAPSHOT";
    }

    @Override
    public String getVersionForBadge() {
        return this.releaseVersion;
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
