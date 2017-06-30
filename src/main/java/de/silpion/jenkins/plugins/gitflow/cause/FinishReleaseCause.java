package de.silpion.jenkins.plugins.gitflow.cause;

import static de.silpion.jenkins.plugins.gitflow.GitflowBuildWrapper.getGitflowBuildWrapperDescriptor;

import org.apache.commons.lang.StringUtils;
import de.silpion.jenkins.plugins.gitflow.data.RemoteBranch;

/**
 * The {@link hudson.model.Cause Cause} object for the <i>Finish Release</i> action to be executed.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class FinishReleaseCause extends AbstractReleaseBranchCause {

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param releaseBranch the <i>release</i> branch containing base data for the cause.
     */
    public FinishReleaseCause(final RemoteBranch releaseBranch) {
        super(releaseBranch, true);

    }

    @Override
    public String getVersionForBadge() {
        return StringUtils.removeStart(this.getReleaseBranch(), getGitflowBuildWrapperDescriptor().getReleaseBranchPrefix());
    }
}
