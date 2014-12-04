package org.jenkinsci.plugins.gitflow.action;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.matches;
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
import org.jenkinsci.plugins.gitflow.action.buildtype.AbstractBuildTypeAction;
import org.jenkinsci.plugins.gitflow.action.buildtype.BuildTypeActionFactory;
import org.jenkinsci.plugins.gitflow.cause.TestHotfixCause;
import org.jenkinsci.plugins.gitflow.data.GitflowPluginData;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
    private AbstractBuildTypeAction buildTypeAction;

    @Mock
    private GitflowPluginData gitflowPluginData;

    @Mock
    private RemoteBranch remoteBranchHotfix;

    @Before
    @SuppressWarnings("unchecked assignment")
    public void setUp() throws Exception {
        super.setUp();

        // Mock calls to build wrapper descriptor.
        when(this.gitflowBuildWrapperDescriptor.getVersionTagPrefix()).thenReturn("version/");
        when(this.gitflowBuildWrapperDescriptor.getBranchType(matches("hotfix/.*"))).thenReturn("hotfix");

        // Mock the BuildTypeAction.
        mockStatic(BuildTypeActionFactory.class);
        when(BuildTypeActionFactory.newInstance(build, launcher, listener)).thenReturn(buildTypeAction);

        // Mock calls to the GitflowPluginData object.
        when(this.gitflowPluginData.getRemoteBranch("origin", "hotfix/1.2")).thenReturn(this.remoteBranchHotfix);
        when(this.build.getAction(GitflowPluginData.class)).thenReturn(this.gitflowPluginData);

        // Instanciate the test subject.
        final TestHotfixCause cause = new TestHotfixCause(createRemoteBranch("hotfix/1.2", "1.2.3-SNAPSHOT"));
        this.testAction = new TestHotfixAction<AbstractBuild<?, ?>>(this.build, this.launcher, this.listener, this.git, cause);
    }

    private static RemoteBranch createRemoteBranch(final String branchName, final String lastBuildVersion) {
        final RemoteBranch masterBranch = new RemoteBranch("origin", branchName);
        masterBranch.setLastBuildVersion(lastBuildVersion);
        return masterBranch;
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
        expectedAdditionalBuildEnvVars.put("GIT_SIMPLE_BRANCH_NAME", "hotfix/1.2");
        expectedAdditionalBuildEnvVars.put("GIT_REMOTE_BRANCH_NAME", "origin/hotfix/1.2");
        expectedAdditionalBuildEnvVars.put("GIT_BRANCH_TYPE", "hotfix");

        // Mock call to Git client proxy.
        when(this.git.getHeadRev(anyString(),anyString())).thenReturn(ObjectId.zeroId());

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
        String hotfixBranch = "hotfix/1.2";

        List<String> changeFiles = Arrays.asList("pom.xml", "child1/pom.xml", "child2/pom.xml", "child3/pom.xml");
        when(buildTypeAction.updateVersion("1.2.3")).thenReturn(changeFiles);

        // Mock call to Git client proxy.
        when(this.git.getHeadRev(anyString(),anyString())).thenReturn(ObjectId.zeroId());

        //Run
        this.testAction.beforeMainBuildInternal();

        //Check
        verify(this.git).setGitflowActionName(this.testAction.getActionName());
        verify(this.git).getHeadRev("origin", hotfixBranch);
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
        String hotfixBranch = "hotfix/1.2";
        String nextHotfixVersion = "1.2.4-SNAPSHOT";

        when(build.getResult()).thenReturn(Result.SUCCESS);

        List<String> changeFiles = Arrays.asList("pom.xml", "child1/pom.xml", "child2/pom.xml", "child3/pom.xml");
        when(buildTypeAction.updateVersion(nextHotfixVersion)).thenReturn(changeFiles);

        //Run
        this.testAction.afterMainBuildInternal();

        //Check
        verify(this.gitflowPluginData).setDryRun(false);
        verify(this.gitflowPluginData).getRemoteBranch("origin", hotfixBranch);
        verify(this.git).setGitflowActionName(this.testAction.getActionName());
        verify(this.git, atLeast(2)).push(anyString(), anyString());

        verify(this.git).add("pom.xml");
        verify(this.git).add("child1/pom.xml");
        verify(this.git).add("child2/pom.xml");
        verify(this.git).add("child3/pom.xml");
        verify(this.git).commit(any(String.class));
        verify(this.git).getHeadRev(any(String.class), any(String.class));
        verify(this.git).tag(any(String.class), any(String.class));

        verify(this.remoteBranchHotfix, atLeastOnce()).setLastBuildResult(Result.SUCCESS);
        verify(this.remoteBranchHotfix).setLastBuildVersion(nextHotfixVersion);

        verifyNoMoreInteractions(this.git, this.gitflowPluginData);
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
        verify(this.gitflowPluginData).getRemoteBranch("origin", "hotfix/1.2");
        verify(this.remoteBranchHotfix).setLastBuildResult(Result.FAILURE);

        verifyNoMoreInteractions(this.git, this.gitflowPluginData, this.remoteBranchHotfix);
    }
}
