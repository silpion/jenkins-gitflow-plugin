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
    private String releaseNextDevelopmentVersion;
    private String nextDevelopmentVersion;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param developBranch the <i>develop</i> branch containing base data for the cause.
     */
    public StartReleaseCause(final RemoteBranch developBranch) {
        assert "develop".equals(GitflowBuildWrapper.getGitflowBuildWrapperDescriptor().getBranchType(developBranch.getBranchName()));

        this.releaseVersion = StringUtils.removeEnd(developBranch.getLastBuildVersion(), "-SNAPSHOT");
        this.releaseNextDevelopmentVersion = this.releaseVersion + ".1-SNAPSHOT";

        final String majorVersion = StringUtils.substringBeforeLast(this.releaseVersion, ".");
        final String latestMinorVersion = StringUtils.substringAfterLast(this.releaseVersion, ".");
        final int nextMinorVersion = Integer.valueOf(latestMinorVersion).intValue() + 1;
        this.nextDevelopmentVersion = majorVersion + "." + nextMinorVersion + "-SNAPSHOT";
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

    public String getReleaseNextDevelopmentVersion() {
        return this.releaseNextDevelopmentVersion;
    }

    public void setReleaseNextDevelopmentVersion(final String releaseNextDevelopmentVersion) {
        this.releaseNextDevelopmentVersion = releaseNextDevelopmentVersion;
    }

    public String getNextDevelopmentVersion() {
        return this.nextDevelopmentVersion;
    }

    public void setNextDevelopmentVersion(final String nextDevelopmentVersion) {
        this.nextDevelopmentVersion = nextDevelopmentVersion;
    }
}
