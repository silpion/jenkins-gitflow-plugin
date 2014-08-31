package org.jenkinsci.plugins.gitflow.cause;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gitflow.GitflowBuildWrapper;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;

/**
 * The {@link hudson.model.Cause Cause} object for the <i>Start Hotfix</i> action to be executed.
 *
 * @author Hannes Osius, Silpion IT-Solutions GmbH - osius@silpion.de
 */
public class StartHotfixCause extends AbstractGitflowCause {

    private final String hotfixReleaseVersion;
    private final String publishedFixesReleaseVersion;

    private String nextHotfixDevelopmentVersion;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param masterBranch the <i>develop</i> branch containing base data for the cause.
     */
    public StartHotfixCause(final RemoteBranch masterBranch) {
        assert "master".equals(GitflowBuildWrapper.getGitflowBuildWrapperDescriptor().getBranchType(masterBranch.getBranchName()));

        this.hotfixReleaseVersion = masterBranch.getBaseReleaseVersion();
        this.publishedFixesReleaseVersion = masterBranch.getLastReleaseVersion();

        final int patchVersion;
        if (StringUtils.equals(this.publishedFixesReleaseVersion, this.hotfixReleaseVersion)) {
            patchVersion = 1;
        } else {
            final String previousPatchVersion = StringUtils.removeStart(this.publishedFixesReleaseVersion, this.hotfixReleaseVersion + ".");
            patchVersion = Integer.valueOf(previousPatchVersion).intValue() + 1;
        }

        this.nextHotfixDevelopmentVersion = this.hotfixReleaseVersion + "." + patchVersion + "-SNAPSHOT";
    }

    /** {@inheritDoc} */
    @Override
    public String getVersionForBadge() {
        return this.hotfixReleaseVersion;
    }

    public String getHotfixReleaseVersion() {
        return this.hotfixReleaseVersion;
    }

    public String getPublishedFixesReleaseVersion() {
        return this.publishedFixesReleaseVersion;
    }

    public String getNextHotfixDevelopmentVersion() {
        return this.nextHotfixDevelopmentVersion;
    }

    public void setNextHotfixDevelopmentVersion(final String nextHotfixDevelopmentVersion) {
        this.nextHotfixDevelopmentVersion = nextHotfixDevelopmentVersion;
    }
}
