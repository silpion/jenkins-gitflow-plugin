package org.jenkinsci.plugins.gitflow.cause;

import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;

/**
 * The {@link hudson.model.Cause Cause} object for the <i>Publish Release</i> action to be executed.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class PublishReleaseCause extends AbstractReleaseBranchCause {

    private final String lastPatchReleaseVersion;
    private final ObjectId lastPatchReleaseCommit;

    private boolean mergeToDevelop = false;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param releaseBranch the <i>release</i> branch containing base data for the cause.
     */
    public PublishReleaseCause(final RemoteBranch releaseBranch) {
        super(releaseBranch);

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

    public boolean isMergeToDevelop() {
        return this.mergeToDevelop;
    }

    public void setMergeToDevelop(final boolean mergeToDevelop) {
        this.mergeToDevelop = mergeToDevelop;
    }
}
