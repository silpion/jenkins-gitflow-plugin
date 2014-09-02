package org.jenkinsci.plugins.gitflow.cause;

import org.jenkinsci.plugins.gitflow.GitflowBuildWrapper;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;

/**
 * The {@link hudson.model.Cause Cause} object for Gitflow actions, that are based on a hotfix branch.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public abstract class AbstractHotfixBranchCause extends AbstractGitflowCause {

    private final String hotfixBranch;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param hotfixBranch the <i>hotfix</i> branch containing base data for the cause.
     */
    public AbstractHotfixBranchCause(final RemoteBranch hotfixBranch) {
        assert "hotfix".equals(GitflowBuildWrapper.getGitflowBuildWrapperDescriptor().getBranchType(hotfixBranch.getBranchName()));
        this.hotfixBranch = hotfixBranch.getBranchName();
    }

    public String getHotfixBranch() {
        return this.hotfixBranch;
    }
}
