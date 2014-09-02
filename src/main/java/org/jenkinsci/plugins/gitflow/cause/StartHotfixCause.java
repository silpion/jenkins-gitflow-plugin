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

    private final String hotfixVersion;
    private final String publishedPatchReleaseVersion;

    private String nextPatchDevelopmentVersion;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param masterBranch the <i>develop</i> branch containing base data for the cause.
     */
    public StartHotfixCause(final RemoteBranch masterBranch) {
        assert "master".equals(GitflowBuildWrapper.getGitflowBuildWrapperDescriptor().getBranchType(masterBranch.getBranchName()));

        this.hotfixVersion = masterBranch.getBaseReleaseVersion();
        this.publishedPatchReleaseVersion = masterBranch.getLastReleaseVersion();

        final int patchVersion;
        if (StringUtils.equals(this.publishedPatchReleaseVersion, this.hotfixVersion)) {
            patchVersion = 1;
        } else {
            final String previousPatchVersion = StringUtils.removeStart(this.publishedPatchReleaseVersion, this.hotfixVersion + ".");
            patchVersion = Integer.valueOf(previousPatchVersion).intValue() + 1;
        }

        this.nextPatchDevelopmentVersion = this.hotfixVersion + "." + patchVersion + "-SNAPSHOT";
    }

    /** {@inheritDoc} */
    @Override
    public String getVersionForBadge() {
        return this.hotfixVersion;
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
