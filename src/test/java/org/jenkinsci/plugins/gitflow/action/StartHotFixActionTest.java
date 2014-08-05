package org.jenkinsci.plugins.gitflow.action;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;
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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.plugins.git.GitSCM;

@PrepareForTest(BuildTypeActionFactory.class)
@RunWith(PowerMockRunner.class)
public class StartHotFixActionTest extends AbstractGitflowActionTest<StartHotFixAction<AbstractBuild<?, ?>>, StartHotFixCause> {

    @Mock
    private StartHotFixCause cause;

    private StartHotFixAction<AbstractBuild<?, ?>> testAction;

    @Mock
    private GitSCM scm;

    @Mock
    private AbstractBuildTypeAction buildTypeAction;

    private ByteArrayOutputStream outputStream;

    @Captor
    private ArgumentCaptor<URIish> urIishArgumentCaptor;

    @Before
    @SuppressWarnings("unchecked assignment")
    public void setUp() throws Exception {
        super.setUp();

        this.testAction = new StartHotFixAction<AbstractBuild<?, ?>>(this.build, this.launcher, this.listener, this.git, this.cause);

        mockStatic(BuildTypeActionFactory.class);
        PowerMockito.when(BuildTypeActionFactory.newInstance(build, launcher, listener)).thenReturn(buildTypeAction);

        AbstractProject project = mock(AbstractProject.class);
        when(project.getScm()).thenReturn(scm);
        when(build.getProject()).thenReturn(project);

        this.outputStream = new ByteArrayOutputStream();
        when(listener.getLogger()).thenReturn(new PrintStream(outputStream));

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
        //Setup
        StartHotFixCause cause = new StartHotFixCause("VeryHotFix", "1.0.2-Snapshot", false);
        StartHotFixAction action = new StartHotFixAction(build, launcher, listener, this.git, cause);

        List<String> changeFiles = Arrays.asList("pom.xml", "child1/pom.xml", "child2/pom.xml", "child3/pom.xml");
        when(buildTypeAction.updateVersion("1.0.2-Snapshot")).thenReturn(changeFiles);

        //Run
        action.beforeMainBuildInternal();

        //Check
        verify(this.git).setGitflowActionName(action.getActionName());
        verify(this.git).checkoutBranch("hotfix/VeryHotFix", "origin/master");
        verify(this.git).add("pom.xml");
        verify(this.git).add("child1/pom.xml");
        verify(this.git).add("child2/pom.xml");
        verify(this.git).add("child3/pom.xml");
        verify(this.git).commit(any(String.class));

        verifyNoMoreInteractions(this.git);
    }

    @Test
    public void testAfterMainBuildInternalSuccess() throws Exception {

        //Setup
        GitflowPluginData pluginData = mock(GitflowPluginData.class);
        RemoteBranch remoteBranch = mock(RemoteBranch.class);
        PushCommand pushCommand = mock(PushCommand.class);

        when(build.getAction(GitflowPluginData.class)).thenReturn(pluginData);
        when(build.getResult()).thenReturn(Result.SUCCESS);

        StartHotFixCause cause = new StartHotFixCause("VeryHotFix", "1.0.2-Snapshot", false);
        StartHotFixAction action = new StartHotFixAction(build, launcher, listener, this.git, cause);

        when(pluginData.getOrAddRemoteBranch("origin", "hotfix/VeryHotFix")).thenReturn(remoteBranch);
        when(this.git.push()).thenReturn(pushCommand);
        when(pushCommand.to(any(URIish.class))).thenReturn(pushCommand);
        when(pushCommand.ref(any(String.class))).thenReturn(pushCommand);

        //Run
        action.afterMainBuildInternal();

        //Check
        verify(this.git).setGitflowActionName(action.getActionName());
        verify(this.git).push();

        verify(pluginData).setDryRun(false);
        verify(pluginData).getOrAddRemoteBranch("origin", "hotfix/VeryHotFix");

        verify(remoteBranch).setLastBuildResult(Result.SUCCESS);
        verify(remoteBranch).setLastBuildVersion("1.0.2-Snapshot");

        verify(pushCommand).to(urIishArgumentCaptor.capture());
        verify(pushCommand).ref("refs/heads/hotfix/VeryHotFix:refs/heads/hotfix/VeryHotFix");
        verify(pushCommand).execute();

        assertThat(urIishArgumentCaptor.getValue().getPath(), is("origin"));

        verifyNoMoreInteractions(this.git, pluginData, remoteBranch, pushCommand);
    }

    @Test
    public void testAfterMainBuildInternalFail() throws Exception {
        //Setup
        GitflowPluginData pluginData = mock(GitflowPluginData.class);
        when(build.getAction(GitflowPluginData.class)).thenReturn(pluginData);
        when(build.getResult()).thenReturn(Result.FAILURE);

        StartHotFixCause cause = new StartHotFixCause("VeryHotFix", "1.0.2-Snapshot", false);
        StartHotFixAction action = new StartHotFixAction(build, launcher, listener, this.git, cause);

        RemoteBranch remoteBranch = mock(RemoteBranch.class);
        when(pluginData.getOrAddRemoteBranch("origin", "hotfix/VeryHotFix")).thenReturn(remoteBranch);

        //Run
        action.afterMainBuildInternal();

        //Check
        verify(this.git).setGitflowActionName(action.getActionName());
        verify(pluginData).setDryRun(false);
        verify(pluginData).getOrAddRemoteBranch("origin", "hotfix/VeryHotFix");
        verify(remoteBranch).setLastBuildResult(Result.FAILURE);
        verify(remoteBranch).setLastBuildVersion("1.0.2-Snapshot");

        verifyNoMoreInteractions(this.git, pluginData);
    }

}