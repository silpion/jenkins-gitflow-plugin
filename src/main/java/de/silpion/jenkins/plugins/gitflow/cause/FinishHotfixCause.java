package de.silpion.jenkins.plugins.gitflow.cause;

import static de.silpion.jenkins.plugins.gitflow.GitflowBuildWrapper.getGitflowBuildWrapperDescriptor;

import de.silpion.jenkins.plugins.gitflow.data.RemoteBranch;
import org.apache.commons.lang.StringUtils;

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
        super(hotfixBranch, true);
    }

    @Override
    public String getVersionForBadge() {
        return StringUtils.removeStart(this.getHotfixBranch(), getGitflowBuildWrapperDescriptor().getHotfixBranchPrefix());
    }
}
