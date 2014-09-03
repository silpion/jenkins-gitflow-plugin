package org.jenkinsci.plugins.gitflow.cause;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gitflow.GitflowBuildWrapper;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;

/**
 * Encapsulates a group of hotfix branch {@link hudson.model.Cause Cause} objects:
 * <ul>
 * <li>{@link TestHotfixCause TestHotfixCause}</li>
 * <li>{@link PublishHotfixCause PublishHotfixCause}</li>
 * <li>{@link FinishHotfixCause FinishHotfixCause}</li>
 * </ul>
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class HotfixBranchCauseGroup {

    private final String branchName;
    private final String hotfixVersion;

    private final TestHotfixCause testHotfixCause;
    private final PublishHotfixCause publishHotfixCause;
    private final FinishHotfixCause finishHotfixCause;

    public HotfixBranchCauseGroup(final RemoteBranch hotfixBranch) {
        assert "hotfix".equals(GitflowBuildWrapper.getGitflowBuildWrapperDescriptor().getBranchType(hotfixBranch.getBranchName()));

        final String hotfixBranchPrefix = GitflowBuildWrapper.getGitflowBuildWrapperDescriptor().getHotfixBranchPrefix();

        this.branchName = hotfixBranch.getBranchName();
        this.hotfixVersion = StringUtils.removeStart(this.branchName, hotfixBranchPrefix);

        this.testHotfixCause = new TestHotfixCause(hotfixBranch);
        this.publishHotfixCause = new PublishHotfixCause(hotfixBranch);
        this.finishHotfixCause = new FinishHotfixCause(hotfixBranch);
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getBranchName() {
        return this.branchName;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getHotfixVersion() {
        return this.hotfixVersion;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getHotfixVersionDotfree() {
        return this.hotfixVersion.replaceAll("\\.", "_");
    }

    @SuppressWarnings("UnusedDeclaration")
    public TestHotfixCause getTestHotfixCause() {
        return this.testHotfixCause;
    }

    @SuppressWarnings("UnusedDeclaration")
    public PublishHotfixCause getPublishHotfixCause() {
        return this.publishHotfixCause;
    }

    @SuppressWarnings("UnusedDeclaration")
    public FinishHotfixCause getFinishHotfixCause() {
        return this.finishHotfixCause;
    }
}
