package org.jenkinsci.plugins.gitflow;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

import org.jenkinsci.plugins.gitflow.data.RemoteBranch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.model.Descriptor;

@RunWith(PowerMockRunner.class)
public class GitflowProjectActionTest extends AbstractGitflowPluginTest {

    @Mock
    protected GitflowBuildWrapper.DescriptorImpl gitflowBuildWrapperDescriptor;

    @Test
    public void testComputeNextHotfixVersion() throws Exception {
        assertThat(new GitflowProjectAction(null, Collections.<String, RemoteBranch>emptyMap()).computeNextHotfixDevelopmentVersion(), is(GitflowProjectAction.DEFAULT_STRING));

        // TODO Reactivate the following tests when feature #10605 is implemented.
        //assertThat(GitflowProjectAction.computeNextHotfixVersion(null), is(GitflowProjectAction.DEFAULT_STRING));
        //assertThat(GitflowProjectAction.computeNextHotfixVersion(""), is(GitflowProjectAction.DEFAULT_STRING));
        //assertThat(GitflowProjectAction.computeNextHotfixVersion("  "), is(GitflowProjectAction.DEFAULT_STRING));
        //assertThat(GitflowProjectAction.computeNextHotfixVersion("1.0"), is("1.0.1-SNAPSHOT"));
        //assertThat(GitflowProjectAction.computeNextHotfixVersion("1.5"), is("1.5.1-SNAPSHOT"));
        //assertThat(GitflowProjectAction.computeNextHotfixVersion("2.18.39"), is("2.18.40-SNAPSHOT"));
        //assertThat(GitflowProjectAction.computeNextHotfixVersion("2.9.99"), is("2.9.100-SNAPSHOT"));
    }

    @Override
    protected Descriptor<?> getGitflowBuildWrapperDescriptor() {
        return this.gitflowBuildWrapperDescriptor;
    }

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
