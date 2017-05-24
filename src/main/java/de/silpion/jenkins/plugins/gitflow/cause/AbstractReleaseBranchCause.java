package de.silpion.jenkins.plugins.gitflow.cause;

import de.silpion.jenkins.plugins.gitflow.data.RemoteBranch;
import de.silpion.jenkins.plugins.gitflow.GitflowBuildWrapper;

/**
 * The {@link hudson.model.Cause Cause} object for Gitflow actions, that are based on a release branch.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public abstract class AbstractReleaseBranchCause extends AbstractGitflowCause {

    private final String releaseBranch;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param releaseBranch the <i>release</i> branch containing base data for the cause.
     */
    public AbstractReleaseBranchCause(final RemoteBranch releaseBranch) {
        assert "release".equals(GitflowBuildWrapper.getGitflowBuildWrapperDescriptor().getBranchType(releaseBranch.getBranchName()));
        this.releaseBranch = releaseBranch.getBranchName();
    }

    public String getReleaseBranch() {
        return this.releaseBranch;
    }
}
