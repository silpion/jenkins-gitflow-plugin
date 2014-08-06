package org.jenkinsci.plugins.gitflow.action;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.gitclient.PushCommand;
import org.jenkinsci.plugins.gitflow.action.buildtype.AbstractBuildTypeAction;
import org.jenkinsci.plugins.gitflow.action.buildtype.BuildTypeActionFactory;
import org.jenkinsci.plugins.gitflow.cause.TestHotfixCause;
import org.jenkinsci.plugins.gitflow.data.GitflowPluginData;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.plugins.git.GitSCM;

@PrepareForTest(BuildTypeActionFactory.class)
@RunWith(PowerMockRunner.class)
public class TestHotfixActionTest extends AbstractGitflowActionTest<TestHotfixAction<AbstractBuild<?, ?>>, TestHotfixCause> {

    private TestHotfixAction<AbstractBuild<?, ?>> testAction;

    @Mock
    private GitSCM scm;

    @Mock
    private PushCommand pushCommand;

    @Mock
    private AbstractBuildTypeAction buildTypeAction;

    @Mock
    private GitflowPluginData gitflowPluginData;

    @Mock
    private RemoteBranch remoteBranchHotfix;

    @Captor
    private ArgumentCaptor<URIish> urIishArgumentCaptor;

    @Before
    @SuppressWarnings("unchecked assignment")
    public void setUp() throws Exception {
        super.setUp();

        // Mock the BuildTypeAction.
        mockStatic(BuildTypeActionFactory.class);
        when(BuildTypeActionFactory.newInstance(build, launcher, listener)).thenReturn(buildTypeAction);

        // Mock calls to the GitflowPluginData object.
        when(this.gitflowPluginData.getRemoteBranch("origin", "hotfix/foobar")).thenReturn(this.remoteBranchHotfix);
        when(this.build.getAction(GitflowPluginData.class)).thenReturn(this.gitflowPluginData);

        // Instanciate the test subject.
        final TestHotfixCause cause = new TestHotfixCause("hotfix/foobar", "1.2.3", "1.2.4-SNAPSHOT", false);
        this.testAction = new TestHotfixAction<AbstractBuild<?, ?>>(this.build, this.launcher, this.listener, this.git, cause);

        // Mock calls to Git client.
        when(this.git.push()).thenReturn(this.pushCommand);
        when(this.git.getRemoteUrl("origin")).thenReturn("someOriginUrl");
        when(this.git.getHeadRev("someOriginUrl", "hotfix/foobar")).thenReturn(ObjectId.zeroId());
        when(this.pushCommand.ref(anyString())).thenReturn(this.pushCommand);
        when(this.pushCommand.to(any(URIish.class))).thenReturn(this.pushCommand);

        // Mock calls to build wrapper descriptor.
        when(this.gitflowBuildWrapperDescriptor.getVersionTagPrefix()).thenReturn("version/");
        when(this.gitflowBuildWrapperDescriptor.getBranchType("hotfix/foobar")).thenReturn("hotfix");
    }

    /** {@inheritDoc} */
    @Override
    protected TestHotfixAction<AbstractBuild<?, ?>> getTestAction() {
        return this.testAction;
    }

    /** {@inheritDoc} */
    @Override
    protected Map<String, String> setUpTestGetAdditionalBuildEnvVars() throws InterruptedException {
        final Map<String, String> expectedAdditionalBuildEnvVars = new HashMap<String, String>();

        // Define expectations.
        expectedAdditionalBuildEnvVars.put("GIT_SIMPLE_BRANCH_NAME", "hotfix/foobar");
        expectedAdditionalBuildEnvVars.put("GIT_REMOTE_BRANCH_NAME", "origin/hotfix/foobar");
        expectedAdditionalBuildEnvVars.put("GIT_BRANCH_TYPE", "hotfix");

        return expectedAdditionalBuildEnvVars;
    }

    //**********************************************************************************************************************************************************
    //
    // Test
    //
    //**********************************************************************************************************************************************************

    @Test
    public void testBeforeMainBuildInternal() throws Exception {

        //Setup
        String hotfixBranch = "hotfix/foobar";

        List<String> changeFiles = Arrays.asList("pom.xml", "child1/pom.xml", "child2/pom.xml", "child3/pom.xml");
        when(buildTypeAction.updateVersion("1.2.3")).thenReturn(changeFiles);

        //Run
        this.testAction.beforeMainBuildInternal();

        //Check
        verify(this.git).setGitflowActionName(this.testAction.getActionName());
        verify(this.git).getRemoteUrl("origin");
        verify(this.git).getHeadRev("someOriginUrl", hotfixBranch);
        verify(this.git).checkoutBranch(hotfixBranch, ObjectId.zeroId().getName());

        verify(this.git).add("pom.xml");
        verify(this.git).add("child1/pom.xml");
        verify(this.git).add("child2/pom.xml");
        verify(this.git).add("child3/pom.xml");
        verify(this.git).commit(any(String.class));

        verifyNoMoreInteractions(this.git);

    }

    @Test
    public void testAfterMainBuildInternal() throws Exception {

        //Setup
        String hotfixBranch = "hotfix/foobar";
        String nextHotfixVersion = "1.2.4-SNAPSHOT";

        when(build.getResult()).thenReturn(Result.SUCCESS);

        List<String> changeFiles = Arrays.asList("pom.xml", "child1/pom.xml", "child2/pom.xml", "child3/pom.xml");
        when(buildTypeAction.updateVersion(nextHotfixVersion)).thenReturn(changeFiles);

        when(this.git.push()).thenReturn(pushCommand);
        when(pushCommand.to(any(URIish.class))).thenReturn(pushCommand);
        when(pushCommand.ref(any(String.class))).thenReturn(pushCommand);
        //Run
        this.testAction.afterMainBuildInternal();

        //Check
        verify(this.gitflowPluginData).setDryRun(false);
        verify(this.gitflowPluginData).getRemoteBranch("origin", hotfixBranch);
        verify(this.git).setGitflowActionName(this.testAction.getActionName());
        verify(this.git, atLeast(2)).push();

        verify(this.git).add("pom.xml");
        verify(this.git).add("child1/pom.xml");
        verify(this.git).add("child2/pom.xml");
        verify(this.git).add("child3/pom.xml");
        verify(this.git).commit(any(String.class));
        verify(this.git).getRemoteUrl("origin");
        verify(this.git).getHeadRev(any(String.class), any(String.class));
        verify(this.git).tag(any(String.class), any(String.class));

        verify(this.remoteBranchHotfix, atLeastOnce()).setLastBuildResult(Result.SUCCESS);
        verify(this.remoteBranchHotfix).setLastBuildVersion(nextHotfixVersion);

        verify(this.pushCommand, atLeast(2)).to(this.urIishArgumentCaptor.capture());
        verify(this.pushCommand, atLeast(2)).ref("refs/heads/" + hotfixBranch + ":refs/heads/" + hotfixBranch);
        verify(this.pushCommand).ref("refs/tags/version/1.2.3:refs/tags/version/1.2.3");
        verify(this.pushCommand, atLeast(2)).execute();

        assertThat(urIishArgumentCaptor.getValue().getPath(), is("origin"));

        verifyNoMoreInteractions(this.git, this.gitflowPluginData, this.pushCommand);
    }

    @Test
    public void testAfterMainBuildInternalFail() throws Exception {

        when(this.remoteBranchHotfix.getLastBuildVersion()).thenReturn("1.2.3-SNAPSHOT");

        when(build.getResult()).thenReturn(Result.FAILURE);

        //Run
        this.testAction.afterMainBuildInternal();

        //Check
        verify(this.git).setGitflowActionName(this.testAction.getActionName());
        verify(this.gitflowPluginData).setDryRun(false);
        verify(this.gitflowPluginData).getRemoteBranch("origin", "hotfix/foobar");
        verify(this.remoteBranchHotfix).setLastBuildResult(Result.FAILURE);

        verifyNoMoreInteractions(this.git, this.gitflowPluginData, this.remoteBranchHotfix);
    }
}
