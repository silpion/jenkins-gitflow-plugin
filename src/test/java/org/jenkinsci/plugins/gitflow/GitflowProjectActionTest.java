package org.jenkinsci.plugins.gitflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Collection;
import java.util.Collections;

import org.jenkinsci.plugins.gitflow.cause.FinishHotfixCause;
import org.jenkinsci.plugins.gitflow.cause.HotfixBranchCauseGroup;
import org.jenkinsci.plugins.gitflow.cause.StartHotfixCause;
import org.jenkinsci.plugins.gitflow.cause.TestHotfixCause;
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

import com.google.common.collect.Lists;

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
        when(this.gitflowBuildWrapperDescriptor.getBranchType(matches("hotfix/.*"))).thenReturn("hotfix");
        when(this.gitflowBuildWrapperDescriptor.getHotfixBranchPrefix()).thenReturn("hotfix/");
    }

    @Override
    protected Descriptor<?> getGitflowBuildWrapperDescriptor() {
        return this.gitflowBuildWrapperDescriptor;
    }

    @Test
    public void testConstructorForStartHotfixCause() {

        // When a master branch exists, a StartHotfixCause must be created.
        when(this.gitflowPluginData.getRemoteBranches()).thenReturn(Collections.singletonList(createRemoteBranch("master", null, null, null)));
        assertNotNull(new GitflowProjectAction(this.job).getStartHotfixCause());

        // No master branch, no StartHotfixCause.
        when(this.gitflowPluginData.getRemoteBranches()).thenReturn(Collections.singletonList(createRemoteBranch("develop", null, null, null)));
        assertNull(new GitflowProjectAction(this.job).getStartHotfixCause());

        // When the master branch has a snapshot version, no StartHotfixCause should be created.
        when(this.gitflowPluginData.getRemoteBranches()).thenReturn(Collections.singletonList(createRemoteBranch("master", "1.0-SNAPSHOT", null, null)));
        assertNull(new GitflowProjectAction(this.job).getStartHotfixCause());

        // When a hotfix branch for the master release already exists, no StartHotfixCause should be created.
        final RemoteBranch masterBranch = createRemoteBranch("master", "1.0.2", "1.0", "1.0.2");
        final RemoteBranch hotfixBranch = createRemoteBranch("hotfix/1.0", "1.0.4-SNAPSHOT", "1.0", "1.0.3");
        when(this.gitflowPluginData.getRemoteBranches()).thenReturn(Lists.newArrayList(masterBranch, hotfixBranch));
        assertNull(new GitflowProjectAction(this.job).getStartHotfixCause());
    }

    @Test
    public void testConstructorForHotfixBranchCauseGroups() {

        // When a hotfix branch exists, a TestHotfixCause must be created.
        when(this.gitflowPluginData.getRemoteBranches()).thenReturn(Collections.singletonList(createRemoteBranch("hotfix/1.0", "1.0.4-SNAPSHOT", null, null)));
        final Collection<HotfixBranchCauseGroup> hotfixBranchCauseGroups1 = new GitflowProjectAction(this.job).getHotfixBranchCauseGroups();
        assertEquals(1, hotfixBranchCauseGroups1.size());
        final HotfixBranchCauseGroup hotfixBranchCauseGroup = hotfixBranchCauseGroups1.iterator().next();
        assertNotNull(hotfixBranchCauseGroup.getTestHotfixCause());
        assertNotNull(hotfixBranchCauseGroup.getFinishHotfixCause());

        // No hotfix branch, no TestHotfixCause.
        when(this.gitflowPluginData.getRemoteBranches()).thenReturn(Collections.singletonList(createRemoteBranch("release/1.0", "1.0.4-SNAPSHOT", null, null)));
        final Collection<HotfixBranchCauseGroup> hotfixBranchCauseGroups2 = new GitflowProjectAction(this.job).getHotfixBranchCauseGroups();
        assertEquals(0, hotfixBranchCauseGroups2.size());
    }

    @Test
    public void testDoSubmitStartHotfixCause() throws Exception {

        when(this.gitflowPluginData.getRemoteBranches()).thenReturn(Collections.singletonList(createRemoteBranch("master", null, null, null)));

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

    @Test
    public void testDoSubmitTestHotfixCause() throws Exception {

        when(this.gitflowPluginData.getRemoteBranches()).thenReturn(Collections.singletonList(createRemoteBranch("hotfix/1.1", "1.1.4-SNAPSHOT", null, null)));

        final JSONObject actionObject = new JSONObject();
        actionObject.element(GitflowProjectAction.JSON_PARAM_VALUE, "testHotfix");
        actionObject.element(GitflowProjectAction.JSON_PARAM_HOTFIX_VERSION, "1.1");
        actionObject.element(GitflowProjectAction.JSON_PARAM_HOTFIX_RELEASE_VERSION, "1.1.1");
        actionObject.element(GitflowProjectAction.JSON_PARAM_NEXT_HOTFIX_DEVELOPMENT_VERSION, "1.1.2-SNAPSHOT");
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

        final ArgumentCaptor<TestHotfixCause> testHotfixCauseArgumentCaptor = ArgumentCaptor.forClass(TestHotfixCause.class);
        verify(this.job).scheduleBuild(anyInt(), testHotfixCauseArgumentCaptor.capture());
        assertEquals("1.1.1", testHotfixCauseArgumentCaptor.getValue().getHotfixReleaseVersion());
        assertEquals("1.1.2-SNAPSHOT", testHotfixCauseArgumentCaptor.getValue().getNextHotfixDevelopmentVersion());
    }

    @Test
    public void testDoSubmitFinishHotfixCause() throws Exception {

        when(this.gitflowPluginData.getRemoteBranches()).thenReturn(Collections.singletonList(createRemoteBranch("hotfix/1.1", "1.1.4-SNAPSHOT", null, null)));

        final JSONObject actionObject = new JSONObject();
        actionObject.element(GitflowProjectAction.JSON_PARAM_VALUE, "finishHotfix");
        actionObject.element(GitflowProjectAction.JSON_PARAM_HOTFIX_VERSION, "1.1");
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

        final ArgumentCaptor<FinishHotfixCause> finishHotfixCauseArgumentCaptor = ArgumentCaptor.forClass(FinishHotfixCause.class);
        verify(this.job).scheduleBuild(anyInt(), finishHotfixCauseArgumentCaptor.capture());
    }

    private static RemoteBranch createRemoteBranch(final String branchName, final String lastBuildVersion, final String baseReleaseVersion, final String lastReleaseVersion) {
        final RemoteBranch remoteBranch = new RemoteBranch("origin", branchName);
        remoteBranch.setLastBuildVersion(lastBuildVersion);
        remoteBranch.setBaseReleaseVersion(baseReleaseVersion);
        remoteBranch.setLastReleaseVersion(lastReleaseVersion);
        return remoteBranch;
    }
}
