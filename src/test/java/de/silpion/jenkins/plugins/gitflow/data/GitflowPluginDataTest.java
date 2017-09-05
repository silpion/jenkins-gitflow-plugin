package de.silpion.jenkins.plugins.gitflow.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import hudson.model.Result;

/**
 * Unit tests for the {@link GitflowPluginData} class.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GitflowPluginDataTest {

    @Test
    public void testGetUnstableRemoteBranchesGroupedByResult() throws Exception {

        // Prepare test data.
        final GitflowPluginData gitflowPluginData = new GitflowPluginData();
        final List<RemoteBranch> remoteBranches = gitflowPluginData.getRemoteBranches();
        remoteBranches.add(this.createRemoteBranch("success", Result.SUCCESS));
        remoteBranches.add(this.createRemoteBranch("unstable", Result.UNSTABLE));
        remoteBranches.add(this.createRemoteBranch("failure", Result.FAILURE));
        remoteBranches.add(this.createRemoteBranch("aborted", Result.ABORTED));
        remoteBranches.add(this.createRemoteBranch("not-built", Result.NOT_BUILT));
        remoteBranches.add(this.createRemoteBranch("null", null));

        // Execute the method and verify the results.
        final Iterator<Map.Entry<Result, Collection<RemoteBranch>>> unstableRemoteBranchesGroupedByResultIterator = gitflowPluginData.getUnstableRemoteBranchesGroupedByResult().entrySet().iterator();
        assertTrue(unstableRemoteBranchesGroupedByResultIterator.hasNext());
        this.assertRemoteBranchesGroupedByResultEntry(unstableRemoteBranchesGroupedByResultIterator.next(), Result.UNSTABLE, 1);
        assertTrue(unstableRemoteBranchesGroupedByResultIterator.hasNext());
        this.assertRemoteBranchesGroupedByResultEntry(unstableRemoteBranchesGroupedByResultIterator.next(), Result.FAILURE, 2); // 'null' Result denotes a failure.
        assertTrue(unstableRemoteBranchesGroupedByResultIterator.hasNext());
        this.assertRemoteBranchesGroupedByResultEntry(unstableRemoteBranchesGroupedByResultIterator.next(), Result.NOT_BUILT, 1);
        assertTrue(unstableRemoteBranchesGroupedByResultIterator.hasNext());
        this.assertRemoteBranchesGroupedByResultEntry(unstableRemoteBranchesGroupedByResultIterator.next(), Result.ABORTED, 1);
        assertFalse(unstableRemoteBranchesGroupedByResultIterator.hasNext());
    }

    private RemoteBranch createRemoteBranch(final String branchName, final Result lastBuildResult) {
        final RemoteBranch remoteBranch = new RemoteBranch(branchName);
        remoteBranch.setLastBuildResult(lastBuildResult);
        return remoteBranch;
    }

    private void assertRemoteBranchesGroupedByResultEntry(final Map.Entry<Result, Collection<RemoteBranch>> assertEntry, final Result expectedResult, final int expectedNumberOfRemoteBranches) {
        assertEquals(expectedResult, assertEntry.getKey());
        assertEquals(expectedNumberOfRemoteBranches, assertEntry.getValue().size());
    }
}
