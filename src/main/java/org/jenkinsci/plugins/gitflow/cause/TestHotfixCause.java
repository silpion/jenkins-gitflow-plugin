package org.jenkinsci.plugins.gitflow.cause;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;

/**
 * The {@link hudson.model.Cause Cause} object for the <i>Test Hotfix</i> action to be executed.
 *
 * @author Hannes Osius, Silpion IT-Solutions GmbH - osius@silpion.de
 */
public class TestHotfixCause extends AbstractHotfixBranchCause {

    private String hotfixReleaseVersion;
    private String nextHotfixDevelopmentVersion;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param hotfixBranch the <i>hotfix</i> branch containing base data for the cause.
     */
    public TestHotfixCause(final RemoteBranch hotfixBranch) {
        super(hotfixBranch);

        this.hotfixReleaseVersion = StringUtils.removeEnd(hotfixBranch.getLastBuildVersion(), "-SNAPSHOT");

        final String baseVersion = StringUtils.substringBeforeLast(this.hotfixReleaseVersion, ".");
        final int newPatchNumber = Integer.valueOf(StringUtils.substringAfterLast(this.hotfixReleaseVersion, ".")).intValue() + 1;
        this.nextHotfixDevelopmentVersion = baseVersion + "." + newPatchNumber + "-SNAPSHOT";
    }

    @Override
    public String getVersionForBadge() {
        return this.hotfixReleaseVersion;
    }

    public String getHotfixReleaseVersion() {
        return this.hotfixReleaseVersion;
    }

    public void setHotfixReleaseVersion(final String hotfixReleaseVersion) {
        this.hotfixReleaseVersion = hotfixReleaseVersion;
    }

    public String getNextHotfixDevelopmentVersion() {
        return this.nextHotfixDevelopmentVersion;
    }

    public void setNextHotfixDevelopmentVersion(final String nextHotfixDevelopmentVersion) {
        this.nextHotfixDevelopmentVersion = nextHotfixDevelopmentVersion;
    }
}
