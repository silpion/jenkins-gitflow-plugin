package org.jenkinsci.plugins.gitflow;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

import org.jenkinsci.plugins.gitflow.cause.StartHotfixCause;
import org.jenkinsci.plugins.gitflow.data.GitflowPluginData;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import net.sf.json.JSONObject;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.ItemGroup;

@RunWith(PowerMockRunner.class)
public class GitflowProjectActionTest extends AbstractGitflowPluginTest {

    @Mock
    private GitflowPluginData gitflowPluginData;

    @Mock
    @SuppressWarnings("rawtypes")
    private AbstractBuild lastBuild;

    @Mock
    @SuppressWarnings("rawtypes")
    private AbstractProject job;

    @Mock
    private GitflowBuildWrapper.DescriptorImpl gitflowBuildWrapperDescriptor;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(this.job.getLastBuild()).thenReturn(this.lastBuild);
        when(this.lastBuild.getAction(GitflowPluginData.class)).thenReturn(this.gitflowPluginData);
        when(this.gitflowBuildWrapperDescriptor.getBranchType("master")).thenReturn("master");
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

    @Test
    public void testConstructorForStartHotfixCause() {

        // When a master branch exists, a StartHotfixCause must be created.
        when(this.gitflowPluginData.getRemoteBranches()).thenReturn(Collections.singletonList(new RemoteBranch("origin", "master")));
        assertNotNull(new GitflowProjectAction(this.job).getStartHotfixCause());

        // No master branch, no StartHotfixCause.
        when(this.gitflowPluginData.getRemoteBranches()).thenReturn(Collections.singletonList(new RemoteBranch("origin", "develop")));
        assertNull(new GitflowProjectAction(this.job).getStartHotfixCause());
    }

    @Test
    public void testDoSubmitStartHotfixCause() throws Exception {

        when(this.gitflowPluginData.getRemoteBranches()).thenReturn(Collections.singletonList(new RemoteBranch("origin", "master")));

        final JSONObject actionObject = new JSONObject();
        actionObject.element(GitflowProjectAction.JSON_PARAM_VALUE, "startHotfix");
        actionObject.element(GitflowProjectAction.JSON_PARAM_NEXT_HOTFIX_DEVELOPMENT_VERSION, "1.1.1-SNAPSHOT");
        final JSONObject formObject = new JSONObject();
        formObject.element(GitflowProjectAction.JSON_PARAM_ACTION, actionObject);
        formObject.element(GitflowProjectAction.JSON_PARAM_DRY_RUN, Boolean.FALSE);

        final StaplerRequest staplerRequest = mock(StaplerRequest.class);
        when(staplerRequest.getSubmittedForm()).thenReturn(formObject);

        final StaplerResponse staplerResponse = mock(StaplerResponse.class);

        final ItemGroup<?> itemGroup = mock(ItemGroup.class);
        when(this.job.getParent()).thenReturn(itemGroup);
        when(itemGroup.getUrl()).thenReturn("url");

        final GitflowProjectAction gitflowProjectAction = new GitflowProjectAction(this.job);
        gitflowProjectAction.doSubmit(staplerRequest, staplerResponse);

        final ArgumentCaptor<StartHotfixCause> startHotfixCauseArgumentCaptor = ArgumentCaptor.forClass(StartHotfixCause.class);
        verify(this.job).scheduleBuild(anyInt(), startHotfixCauseArgumentCaptor.capture());
        assertEquals("1.1.1-SNAPSHOT", startHotfixCauseArgumentCaptor.getValue().getNextHotfixDevelopmentVersion());
    }
}
