package de.silpion.jenkins.plugins.gitflow.cause;

import de.silpion.jenkins.plugins.gitflow.data.RemoteBranch;
import org.eclipse.jgit.lib.ObjectId;

/**
 * The {@link hudson.model.Cause Cause} object for the <i>Publish Hotfix</i> action to be executed.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class PublishHotfixCause extends AbstractHotfixBranchCause {

    private final String lastPatchReleaseVersion;
    private final ObjectId lastPatchReleaseCommit;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param releaseBranch the <i>release</i> branch containing base data for the cause.
     */
    public PublishHotfixCause(final RemoteBranch releaseBranch) {
        super(releaseBranch, true);

        this.lastPatchReleaseVersion = releaseBranch.getLastReleaseVersion();
        this.lastPatchReleaseCommit = releaseBranch.getLastReleaseVersionCommit();
    }

    @Override
    public String getVersionForBadge() {
        return this.lastPatchReleaseVersion;
    }

    public String getLastPatchReleaseVersion() {
        return this.lastPatchReleaseVersion;
    }

    public ObjectId getLastPatchReleaseCommit() {
        return this.lastPatchReleaseCommit;
    }
}
