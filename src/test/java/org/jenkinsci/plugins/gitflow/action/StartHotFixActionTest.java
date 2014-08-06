package org.jenkinsci.plugins.gitflow.action;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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

import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.gitclient.PushCommand;
import org.jenkinsci.plugins.gitflow.action.buildtype.AbstractBuildTypeAction;
import org.jenkinsci.plugins.gitflow.action.buildtype.BuildTypeActionFactory;
import org.jenkinsci.plugins.gitflow.cause.StartHotFixCause;
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
public class StartHotFixActionTest extends AbstractGitflowActionTest<StartHotFixAction<AbstractBuild<?, ?>>, StartHotFixCause> {

    private StartHotFixAction<AbstractBuild<?, ?>> testAction;

    @Mock
    private GitSCM scm;

    @Mock
    private PushCommand pushCommand;

    @Mock
    @SuppressWarnings("rawtypes")
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

        // Mock the call to the BuildTypeAction.
        final List<String> changeFiles = Arrays.asList("pom.xml", "child1/pom.xml", "child2/pom.xml", "child3/pom.xml");
        mockStatic(BuildTypeActionFactory.class);
        when(BuildTypeActionFactory.newInstance(this.build, this.launcher, this.listener)).thenReturn(this.buildTypeAction);
        when(this.buildTypeAction.updateVersion("1.0.2-Snapshot")).thenReturn(changeFiles);

        // Mock calls to the GitflowPluginData object.
        when(this.gitflowPluginData.getRemoteBranch("origin", "master")).thenReturn(new RemoteBranch("origin", "master"));
        when(this.gitflowPluginData.getOrAddRemoteBranch("origin", "hotfix/VeryHotFix")).thenReturn(this.remoteBranchHotfix);
        when(this.build.getAction(GitflowPluginData.class)).thenReturn(this.gitflowPluginData);

        // Instanciate the test subject.
        final StartHotFixCause cause = new StartHotFixCause("VeryHotFix", "1.0.2-Snapshot", false);
        this.testAction = new StartHotFixAction<AbstractBuild<?, ?>>(this.build, this.launcher, this.listener, this.git, cause);

        // Mock calls to Git client.
        when(this.git.push()).thenReturn(this.pushCommand);
        when(this.pushCommand.ref(anyString())).thenReturn(this.pushCommand);
        when(this.pushCommand.to(any(URIish.class))).thenReturn(this.pushCommand);

        // Mock calls to build wrapper descriptor.
        when(this.gitflowBuildWrapperDescriptor.getMasterBranch()).thenReturn("master");
        when(this.gitflowBuildWrapperDescriptor.getHotfixBranchPrefix()).thenReturn("hotfix/");
    }

    /** {@inheritDoc} */
    @Override
    protected StartHotFixAction<AbstractBuild<?, ?>> getTestAction() {
        return this.testAction;
    }

    /** {@inheritDoc} */
    @Override
    protected Map<String, String> setUpTestGetAdditionalBuildEnvVars() throws InterruptedException {

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
        verify(this.git).checkoutBranch("hotfix/VeryHotFix", "origin/master");
        verify(this.git).add("pom.xml");
        verify(this.git).add("child1/pom.xml");
        verify(this.git).add("child2/pom.xml");
        verify(this.git).add("child3/pom.xml");
        verify(this.git).commit(any(String.class));
        verify(this.git, atLeastOnce()).push();

        verify(this.gitflowPluginData).setDryRun(false);
        verify(this.gitflowPluginData).getRemoteBranch("origin", "master");
        verify(this.gitflowPluginData, atLeastOnce()).getOrAddRemoteBranch("origin", "hotfix/VeryHotFix");

        verify(this.remoteBranchHotfix, atLeastOnce()).setLastBuildResult(Result.SUCCESS);
        verify(this.remoteBranchHotfix, atLeastOnce()).setLastBuildVersion("1.0.2-Snapshot");

        verify(this.pushCommand, atLeastOnce()).to(this.urIishArgumentCaptor.capture());
        verify(this.pushCommand, atLeastOnce()).ref("refs/heads/hotfix/VeryHotFix:refs/heads/hotfix/VeryHotFix");
        verify(this.pushCommand, atLeastOnce()).execute();
        assertThat(this.urIishArgumentCaptor.getValue().getPath(), is("origin"));

        verifyNoMoreInteractions(this.git, this.gitflowPluginData, this.pushCommand);
    }
}
