package org.jenkinsci.plugins.gitflow.cause;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;

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
        super(hotfixBranch);

        this.patchReleaseVersion = StringUtils.removeEnd(hotfixBranch.getLastBuildVersion(), "-SNAPSHOT");

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
