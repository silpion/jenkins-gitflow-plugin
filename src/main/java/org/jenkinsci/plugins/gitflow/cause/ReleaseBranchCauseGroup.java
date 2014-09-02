package org.jenkinsci.plugins.gitflow.cause;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gitflow.GitflowBuildWrapper;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;

/**
 * Encapsulates a group of release branch {@link hudson.model.Cause Cause} objects:
 * <ul>
 * <li>{@link org.jenkinsci.plugins.gitflow.cause.TestReleaseCause TestReleaseCause}</li>
 * <li>{@link org.jenkinsci.plugins.gitflow.cause.PublishReleaseCause PublishReleaseCause}</li>
 * <li>{@link org.jenkinsci.plugins.gitflow.cause.FinishReleaseCause FinishReleaseCause}</li>
 * </ul>
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class ReleaseBranchCauseGroup {

    private final String branchName;
    private final String releaseVersion;

    private final TestReleaseCause testReleaseCause;
    private final PublishReleaseCause publishReleaseCause;
    private final FinishReleaseCause finishReleaseCause;

    public ReleaseBranchCauseGroup(final RemoteBranch releaseBranch) {
        assert "release".equals(GitflowBuildWrapper.getGitflowBuildWrapperDescriptor().getBranchType(releaseBranch.getBranchName()));

        final String releaseBranchPrefix = GitflowBuildWrapper.getGitflowBuildWrapperDescriptor().getReleaseBranchPrefix();

        this.branchName = releaseBranch.getBranchName();
        this.releaseVersion = StringUtils.removeStart(this.branchName, releaseBranchPrefix);

        this.testReleaseCause = new TestReleaseCause(releaseBranch);
        this.publishReleaseCause = new PublishReleaseCause(releaseBranch);
        this.finishReleaseCause = new FinishReleaseCause(releaseBranch);
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getBranchName() {
        return this.branchName;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getReleaseVersion() {
        return this.releaseVersion;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getReleaseVersionDotfree() {
        return this.releaseVersion.replaceAll("\\.", "_");
    }

    @SuppressWarnings("UnusedDeclaration")
    public TestReleaseCause getTestReleaseCause() {
        return this.testReleaseCause;
    }

    @SuppressWarnings("UnusedDeclaration")
    public PublishReleaseCause getPublishReleaseCause() {
        return this.publishReleaseCause;
    }

    @SuppressWarnings("UnusedDeclaration")
    public FinishReleaseCause getFinishReleaseCause() {
        return this.finishReleaseCause;
    }
}
