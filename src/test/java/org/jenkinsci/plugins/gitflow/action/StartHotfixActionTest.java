package org.jenkinsci.plugins.gitflow.action;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.gitflow.action.buildtype.AbstractBuildTypeAction;
import org.jenkinsci.plugins.gitflow.action.buildtype.BuildTypeActionFactory;
import org.jenkinsci.plugins.gitflow.cause.StartHotfixCause;
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
public class StartHotfixActionTest extends AbstractGitflowActionTest<StartHotfixAction<AbstractBuild<?, ?>>, StartHotfixCause> {

    private StartHotfixAction<AbstractBuild<?, ?>> testAction;

    @Mock
    private GitSCM scm;

    @Mock
    @SuppressWarnings("rawtypes")
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
        when(this.gitflowBuildWrapperDescriptor.getBranchType("master")).thenReturn("master");
        when(this.gitflowBuildWrapperDescriptor.getMasterBranch()).thenReturn("master");
        when(this.gitflowBuildWrapperDescriptor.getHotfixBranchPrefix()).thenReturn("hotfix/");

        // Mock the call to the BuildTypeAction.
        final List<String> changeFiles = Arrays.asList("pom.xml", "child1/pom.xml", "child2/pom.xml", "child3/pom.xml");
        mockStatic(BuildTypeActionFactory.class);
        when(BuildTypeActionFactory.newInstance(this.build, this.launcher, this.listener, "Start Hotfix")).thenReturn(this.buildTypeAction);
        when(this.buildTypeAction.updateVersion("1.0.2-SNAPSHOT")).thenReturn(changeFiles);

        // Mock calls to the GitflowPluginData object.
        when(this.gitflowPluginData.getRemoteBranch("origin", "master")).thenReturn(new RemoteBranch("origin", "master"));
        when(this.gitflowPluginData.getOrAddRemoteBranch("origin", "hotfix/1.0")).thenReturn(this.remoteBranchHotfix);
        when(this.build.getAction(GitflowPluginData.class)).thenReturn(this.gitflowPluginData);

        // Instanciate the test subject.
        final StartHotfixCause cause = new StartHotfixCause(createRemoteBranch("master", "1.0", "1.0.1"));
        this.testAction = new StartHotfixAction<AbstractBuild<?, ?>>(this.build, this.launcher, this.listener, this.git, cause);
    }

    private static RemoteBranch createRemoteBranch(final String branchName, final String baseReleaseVersion, final String lastReleaseVersion) {
        final RemoteBranch masterBranch = new RemoteBranch("origin", branchName);
        masterBranch.setBaseReleaseVersion(baseReleaseVersion);
        masterBranch.setLastReleaseVersion(lastReleaseVersion);
        return masterBranch;
    }

    /** {@inheritDoc} */
    @Override
    protected StartHotfixAction<AbstractBuild<?, ?>> getTestAction() {
        return this.testAction;
    }

    /** {@inheritDoc} */
    @Override
    protected Map<String, String> setUpTestGetAdditionalBuildEnvVars() throws InterruptedException {

        // Mock call to Git client proxy.
        when(this.git.getHeadRev(anyString(),anyString())).thenReturn(ObjectId.zeroId());

        // No expectations, because the main build is omitted.
        return Collections.emptyMap();
    }

    //**********************************************************************************************************************************************************
    //
    // Tests
    //
    //**********************************************************************************************************************************************************

    @Test
    public void testBeforeMainBuildInternal() throws Exception {

        //Run
        this.testAction.beforeMainBuildInternal();

        //Check
        verify(this.git).setGitflowActionName(this.testAction.getActionName());
        verify(this.git).checkoutBranch("hotfix/1.0", "origin/master");
        verify(this.git).add("pom.xml");
        verify(this.git).add("child1/pom.xml");
        verify(this.git).add("child2/pom.xml");
        verify(this.git).add("child3/pom.xml");
        verify(this.git).commit(any(String.class));
        verify(this.git, atLeastOnce()).push(anyString(), anyString());

        verify(this.gitflowPluginData).setDryRun(false);
        verify(this.gitflowPluginData).getRemoteBranch("origin", "master");
        verify(this.gitflowPluginData, atLeastOnce()).getOrAddRemoteBranch("origin", "hotfix/1.0");

        verify(this.remoteBranchHotfix, atLeastOnce()).setLastBuildResult(Result.SUCCESS);
        verify(this.remoteBranchHotfix, atLeastOnce()).setLastBuildVersion("1.0.2-SNAPSHOT");

        verifyNoMoreInteractions(this.git, this.gitflowPluginData);
    }
}
