package org.jenkinsci.plugins.gitflow.data;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import hudson.model.Action;
import hudson.model.Result;

/**
 * The root (action) object holding the Gitflow plugin data of a Jenkins job/project.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GitflowPluginData implements Action, Serializable, Cloneable {

    private static final long serialVersionUID = -1766342980910311378L;

    private static final Comparator<RemoteBranch> ORDER_BY_REMOTE_ALIAS_AND_NAME = new Comparator<RemoteBranch>() {

        public int compare(final RemoteBranch branch1, final RemoteBranch branch2) {
            int result = String.CASE_INSENSITIVE_ORDER.compare(branch1.getRemoteAlias(), branch2.getRemoteAlias());
            if (result == 0) {
                result = String.CASE_INSENSITIVE_ORDER.compare(branch1.getBranchName(), branch2.getBranchName());
            }
            return result;
        }
    };

    // No SortedSet because it would cause the comparator to be written to the XML. We sort the list when it needs to be sorted.
    private final List<RemoteBranch> remoteBranches = new LinkedList<RemoteBranch>();

    private transient boolean dryRun;

    /** {@inheritDoc} */
    public String getDisplayName() {
        return null;
    }

    /** {@inheritDoc} */
    public String getIconFileName() {
        return null;
    }

    /** {@inheritDoc} */
    public String getUrlName() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public GitflowPluginData clone() throws CloneNotSupportedException {
        return (GitflowPluginData) super.clone();
    }

    /**
     * Records the data for the provided Git remote branches - unless {@code dryRun} isn't set to {@code true}.
     *
     * @param remoteAlias the alias for the remote repository.
     * @param branchNames the simple names of the remote branches (without remote alias).
     * @param buildResult the result of the last build on the remoteBranches.
     * @param buildVersion the project version of the last build on the remoteBranches.
     */
    public void recordRemoteBranches(final String remoteAlias, final Collection<String> branchNames, final Result buildResult, final String buildVersion) {
        if (!this.dryRun) {
            for (final String branchName : branchNames) {
                this.recordRemoteBranch(remoteAlias, branchName, buildResult, buildVersion);
            }
        }
    }

    /**
     * Records the data for the provided Git remote branch - unless {@code dryRun} isn't set to {@code true}.
     *
     * @param remoteAlias the alias for the remote repository.
     * @param branchName the simple name of the branch (without remote alias).
     * @param buildVersion the project version of the last build on the branch.
     */
    public void recordRemoteBranch(final String remoteAlias, final String branchName, final Result buildResult, final String buildVersion) {
        if (!this.dryRun) {
            final RemoteBranch branch = this.getRemoteBranch(remoteAlias, branchName, true);
            branch.setLastBuildResult(buildResult);
            branch.setLastBuildVersion(buildVersion);
        }
    }

    /**
     * Returns the {@link RemoteBranch} with the given remote alias and name.
     * <p/>
     * If there is no record for the requested branch and the parameter {@code createNewIfMissing} is set to {@code true}, a new {@link RemoteBranch} object
     * is be created. If the parameter is set to {@code false}, {@code null} will be returned. attached to the {@link GitflowPluginData} and returned.
     *
     * @param remoteAlias the alias for the remote repository.
     * @param branchName the simple name of the branch.
     * @return the {@link RemoteBranch} with the given remote alias and name or {@code null}.
     */
    private RemoteBranch getRemoteBranch(final String remoteAlias, final String branchName, final boolean createNewIfMissing) {
        RemoteBranch remoteBranch = this.getRemoteBranch(remoteAlias, branchName);
        if (remoteBranch == null && createNewIfMissing) {
            remoteBranch = new RemoteBranch(remoteAlias, branchName);
            this.remoteBranches.add(remoteBranch);
            Collections.sort(this.remoteBranches, ORDER_BY_REMOTE_ALIAS_AND_NAME);
        }
        return remoteBranch;
    }

    /**
     * Returns the {@link RemoteBranch} with the given remote alias and name.
     *
     * @param remoteAlias the alias for the remote repository.
     * @param branchName the simple name of the branch.
     * @return the {@link RemoteBranch} with the given remote alias and name or {@code null}.
     */
    private RemoteBranch getRemoteBranch(final String remoteAlias, final String branchName) {
        for (final RemoteBranch branch : this.remoteBranches) {
            if (branch.getRemoteAlias().equals(remoteAlias) && branch.getBranchName().equals(branchName)) {
                return branch;
            }
        }
        return null;
    }

    public void setDryRun(final boolean dryRun) {
        this.dryRun = dryRun;
    }
}
