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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;

import hudson.model.Action;
import hudson.model.Result;
import hudson.plugins.git.Branch;

/**
 * The root (action) object holding the Gitflow plugin data of a Jenkins job/project.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GitflowPluginData implements Action, Serializable, Cloneable {

    private static final long serialVersionUID = 109223276757967160L;

    private static final Function<Branch, String> BRANCH_TO_NAME_FUNCTION = new Function<Branch, String>() {

        /** {@inheritDoc} */
        public String apply(final Branch input) {
            return input != null ? input.getName() : null;
        }
    };

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
            final RemoteBranch remoteBranch = this.getRemoteBranch(remoteAlias, branchName, true);
            remoteBranch.setLastBuildResult(buildResult);
            remoteBranch.setLastBuildVersion(buildVersion);
        }
    }

    /**
     * Removes the remote branches from the Gitflow plugin data that are contained not in the provided collection of existing branches.
     *
     * @param existingRemoteBranches the collection of existing branches.
     */
    public void removeObsoleteRemoteBranches(final Collection<Branch> existingRemoteBranches) {

        final Collection<String> existingBranchNames = Collections2.transform(existingRemoteBranches, BRANCH_TO_NAME_FUNCTION);

        for (final Iterator<RemoteBranch> remoteBranchIterator = this.remoteBranches.iterator(); remoteBranchIterator.hasNext(); ) {
            final RemoteBranch remoteBranch = remoteBranchIterator.next();
            if (!existingBranchNames.contains(remoteBranch.getRemoteAlias() + "/" + remoteBranch.getBranchName())) {
                remoteBranchIterator.remove();
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
            Collections.sort(this.remoteBranches);
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
    public RemoteBranch getRemoteBranch(final String remoteAlias, final String branchName) {
        for (final RemoteBranch remoteBranch : this.remoteBranches) {
            if (remoteBranch.getRemoteAlias().equals(remoteAlias) && remoteBranch.getBranchName().equals(branchName)) {
                return remoteBranch;
            }
        }
        return null;
    }

    public List<RemoteBranch> getRemoteBranches() {
        return this.remoteBranches;
    }

    public void setDryRun(final boolean dryRun) {
        this.dryRun = dryRun;
    }
}
