package org.jenkinsci.plugins.gitflow.cause;

import static org.jenkinsci.plugins.gitflow.GitflowBuildWrapper.getGitflowBuildWrapperDescriptor;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;

/**
 * The {@link hudson.model.Cause} object for the <i>Finish Hotfix</i> action to be executed.
 *
 * @author Hannes Osius, Silpion IT-Solutions GmbH - osius@silpion.de
 */
public class FinishHotfixCause extends AbstractHotfixBranchCause {

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param hotfixBranch the <i>hotfix</i> branch containing base data for the cause.
     */
    public FinishHotfixCause(final RemoteBranch hotfixBranch) {
        super(hotfixBranch);
    }

    @Override
    public String getVersionForBadge() {
        return StringUtils.removeStart(this.getHotfixBranch(), getGitflowBuildWrapperDescriptor().getHotfixBranchPrefix());
    }
}
