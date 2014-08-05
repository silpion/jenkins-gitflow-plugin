package org.jenkinsci.plugins.gitflow;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.jenkinsci.plugins.gitflow.data.RemoteBranch;
import org.junit.Test;

public class GitflowProjectActionTest {

    @Test
    public void testFilterBranches() throws Exception {

        List<RemoteBranch> branches = new ArrayList<RemoteBranch>();
        //empty
        assertThat(GitflowProjectAction.filterBranches("hotfix", branches).isEmpty(), is(true));

        branches.add(new RemoteBranch("/orgin/foo/Bar", "foo/Bar"));
        branches.add(new RemoteBranch("/orgin/hotfi/fix3", "hotfi/fix3"));
        //no matches
        assertThat(GitflowProjectAction.filterBranches("hotfix", branches).isEmpty(), is(true));

        branches.add(new RemoteBranch("/orgin/hotfix/fix1", "hotfix/fix1"));

        SortedSet<String> oneMatch = GitflowProjectAction.filterBranches("hotfix", branches);
        assertThat(oneMatch, containsInAnyOrder("hotfix/fix1"));

        branches.add(new RemoteBranch("/orgin/hotfix/fix2", "hotfix/fix2"));
        branches.add(new RemoteBranch("/orgin/hotfix/fix3", "hotfix/fix3"));
        SortedSet<String> treeMatches = GitflowProjectAction.filterBranches("hotfix", branches);
        assertThat(treeMatches, containsInAnyOrder("hotfix/fix1", "hotfix/fix2", "hotfix/fix3"));
    }
}