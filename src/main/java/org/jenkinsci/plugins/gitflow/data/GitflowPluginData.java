package org.jenkinsci.plugins.gitflow.data;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

import hudson.model.Action;
import hudson.model.Result;

/**
 * The root (action) object holding the Gitflow plugin data of a Jenkins job/project.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GitflowPluginData implements Action, Serializable, Cloneable {

    private static final long serialVersionUID = 7613596093574533990L;

    private static final Comparator<Result> RESULT_SEVERITY_COMPARATOR = new Comparator<Result>() {

        /** {@inheritDoc} */
        public int compare(final Result result1, final Result result2) {
            return result1.ordinal - result2.ordinal;
        }
    };

    private static final Predicate<Result> RESULT_UNSTABLE_OR_WORSE_PREDICATE = new Predicate<Result>() {

        /** {@inheritDoc} */
        public boolean apply(final Result result) {
            return Result.UNSTABLE.isBetterOrEqualTo(result);
        }
    };

    // No SortedSet because it would cause the comparator to be written to the XML. We sort the list when it needs to be sorted.
    private List<RemoteBranch> remoteBranches = new LinkedList<RemoteBranch>();

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
        final GitflowPluginData clone = (GitflowPluginData) super.clone();

        // Only clone valid remote branches.
        clone.remoteBranches = new LinkedList<RemoteBranch>();
        for (final RemoteBranch remoteBranch : this.remoteBranches) {
            if (remoteBranch.getBranchName() != null) {
                clone.remoteBranches.add(remoteBranch.clone());
            }
        }

        return clone;
    }

    /**
     * Removes the given remote branch from the Gitflow plugin data.
     *
     * @param removeRemoteBranch the remote branch to be removed.
     * @param evenOnDryRun remove the branch even when <i>Dry Run</i> mode is switched on.
     */
    public void removeRemoteBranch(final RemoteBranch removeRemoteBranch, final boolean evenOnDryRun) {
        this.removeRemoteBranches(Collections.singletonList(removeRemoteBranch), evenOnDryRun);
    }

    /**
     * Removes the given remote branches from the Gitflow plugin data.
     *
     * @param removeRemoteBranches the collection of remote branches to be removed.
     * @param evenOnDryRun remove the branches even when <i>Dry Run</i> mode is switched on.
     */
    public void removeRemoteBranches(final Collection<RemoteBranch> removeRemoteBranches, final boolean evenOnDryRun) {
        if (evenOnDryRun || !this.dryRun) {
            for (final Iterator<RemoteBranch> branchIterator = this.remoteBranches.iterator(); branchIterator.hasNext(); ) {
                final RemoteBranch branch = branchIterator.next();
                for (final RemoteBranch removeBranch : removeRemoteBranches) {
                    if (branch.getBranchName().equals(removeBranch.getBranchName())) {
                        branchIterator.remove();
                    }
                }
            }
        }
    }

    /**
     * Returns the remote branches with <i>UNSTABLE</i> (or worse) results, grouped by result.
     *
     * @return a map containing all branches with <i>UNSTABLE</i> (or worse) results, where each key is a {@link Result} and the regarding value
     * is a collection with the branches with that result.
     */
    public Map<Result, Collection<RemoteBranch>> getUnstableRemoteBranchesGroupedByResult() {
        return Maps.filterKeys(this.getRemoteBranchesGroupedByResult(), RESULT_UNSTABLE_OR_WORSE_PREDICATE);
    }

    private Map<Result, Collection<RemoteBranch>> getRemoteBranchesGroupedByResult() {
        final Map<Result, Collection<RemoteBranch>> remoteBranchesGroupedByResult = new TreeMap<Result, Collection<RemoteBranch>>(RESULT_SEVERITY_COMPARATOR);

        for (final RemoteBranch remoteBranch : this.remoteBranches) {
            final Result branchResult = remoteBranch.getLastBuildResult();
            Collection<RemoteBranch> remoteBranchesWithResult = remoteBranchesGroupedByResult.get(branchResult);
            if (remoteBranchesWithResult == null) {
                remoteBranchesWithResult = new TreeSet<RemoteBranch>();
                remoteBranchesGroupedByResult.put(branchResult, remoteBranchesWithResult);
            }
            remoteBranchesWithResult.add(remoteBranch);
        }

        return remoteBranchesGroupedByResult;
    }

    /**
     * Returns the {@link RemoteBranch} with the given remote alias and name. If there is no record for the requested
     * branch, a new {@link RemoteBranch} object is created, attached to the {@link GitflowPluginData} and returned.
     * <p />
     * In <i>Dry Run</i> mode, a copy of the {@link RemoteBranch} is returned. It's a dummy object that is not attached to the persited data.
     *
     * @param branchName the simple name of the branch.
     * @return the {@link RemoteBranch} with the given remote alias and name or a new {@link RemoteBranch} object.
     */
    public RemoteBranch getOrAddRemoteBranch(final String branchName) {
        RemoteBranch remoteBranch = this.getRemoteBranch(branchName);
        if (remoteBranch == null) {
            remoteBranch = new RemoteBranch(branchName);
            if (!this.dryRun) {
                this.remoteBranches.add(remoteBranch);
                Collections.sort(this.remoteBranches);
            }
        }
        return remoteBranch;
    }

    /**
     * Returns the {@link RemoteBranch} with the given remote alias and name.
     * <p />
     * In <i>Dry Run</i> mode, a copy of the {@link RemoteBranch} is returned. It's a dummy object that is not attached to the persited data.
     *
     * @param branchName the simple name of the branch.
     * @return the {@link RemoteBranch} with the given remote alias and name or {@code null}.
     */
    public RemoteBranch getRemoteBranch(final String branchName) {
        RemoteBranch requestedRemoteBranch = null;

        for (final RemoteBranch remoteBranch : this.remoteBranches) {
            if (remoteBranch.getBranchName().equals(branchName)) {
                if (this.dryRun) {
                    try {
                        requestedRemoteBranch = remoteBranch.clone();
                    } catch (final CloneNotSupportedException ignore) {
                        // Should not happen. But even if it happens it's not important, because on dry run the object won't be dropped anyway.
                        requestedRemoteBranch = new RemoteBranch(branchName);
                    }
                } else {
                    requestedRemoteBranch = remoteBranch;
                }
            }
        }

        return requestedRemoteBranch;
    }

    public List<RemoteBranch> getRemoteBranches() {
        return this.remoteBranches;
    }

    public void setDryRun(final boolean dryRun) {
        this.dryRun = dryRun;
    }
}
