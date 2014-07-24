package org.jenkinsci.plugins.gitflow.action;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.gitclient.PushCommand;
import org.jenkinsci.plugins.gitflow.GitflowBuildWrapper;
import org.jenkinsci.plugins.gitflow.action.buildtype.AbstractBuildTypeAction;
import org.jenkinsci.plugins.gitflow.cause.StartHotFixCause;
import org.jenkinsci.plugins.gitflow.data.GitflowPluginData;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;
import org.jenkinsci.plugins.gitflow.gitclient.GitClientDelegate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.git.GitSCM;

@RunWith(MockitoJUnitRunner.class)
public class StartHotFixActionTest {

    @Mock
    private AbstractBuild build;

    @Mock
    private Launcher launcher;

    @Mock
    private BuildListener listener;

    @Mock
    private GitSCM scm;

    @Mock
    private GitClientDelegate gitClient;

    @Mock
    private GitflowBuildWrapper.DescriptorImpl descriptor;

    @Mock
    private AbstractBuildTypeAction<?> buildTypeAction;

    private ByteArrayOutputStream outputStream;

    @Captor
    private ArgumentCaptor<URIish> urIishArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        AbstractProject project = mock(AbstractProject.class);
        when(project.getScm()).thenReturn(scm);
        when(build.getProject()).thenReturn(project);

        this.outputStream = new ByteArrayOutputStream();
        when(listener.getLogger()).thenReturn(new PrintStream(outputStream));

        when(descriptor.getMasterBranch()).thenReturn("master");
        when(descriptor.getHotfixBranchPrefix()).thenReturn("hotfix/");
    }

    //**********************************************************************************************************************************************************
    //
    // Helper
    //
    //**********************************************************************************************************************************************************

    //TODO This Method only exist for make UnitTesting work, the AbstractGitflowAction needs some refactoring
    private StartHotFixAction createAction(StartHotFixCause cause) throws IOException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        StartHotFixAction action = new StartHotFixAction(build, launcher, listener, cause);
        setPrivateFinalField(action, "git", gitClient);
        setPrivateFinalField(action, "buildTypeAction", buildTypeAction);
        action.setDescriptor(descriptor);
        return action;
    }

    //TODO This Method only exist for make UnitTesting work, the AbstractGitflowAction needs some refactoring
    private void setPrivateFinalField(Object obj, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field f = obj.getClass().getSuperclass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(obj, value);
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
        StartHotFixAction action = createAction(cause);

        List<String> changeFiles = Arrays.asList("pom.xml", "child1/pom.xml", "child2/pom.xml", "child3/pom.xml");
        when(buildTypeAction.updateVersion("1.0.2-Snapshot")).thenReturn(changeFiles);

        //Run
        action.beforeMainBuildInternal();

        //Check
        verify(gitClient).checkoutBranch("hotfix/VeryHotFix","origin/master");
        verify(gitClient).add("pom.xml");
        verify(gitClient).add("child1/pom.xml");
        verify(gitClient).add("child2/pom.xml");
        verify(gitClient).add("child3/pom.xml");
        verify(gitClient).commit(any(String.class));

        verifyNoMoreInteractions(gitClient);
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
        StartHotFixAction action = createAction(cause);

        when(pluginData.getOrAddRemoteBranch("origin", "hotfix/VeryHotFix")).thenReturn(remoteBranch);
        when(gitClient.push()).thenReturn(pushCommand);
        when(pushCommand.to(any(URIish.class))).thenReturn(pushCommand);
        when(pushCommand.ref(any(String.class))).thenReturn(pushCommand);

        //Run
        action.afterMainBuildInternal();

        //Check
        verify(gitClient).push();

        verify(pluginData).setDryRun(false);
        verify(pluginData).getOrAddRemoteBranch("origin", "hotfix/VeryHotFix");

        verify(remoteBranch).setLastBuildResult(Result.SUCCESS);
        verify(remoteBranch).setLastBuildVersion("1.0.2-Snapshot");

        verify(pushCommand).to(urIishArgumentCaptor.capture());
        verify(pushCommand).ref("refs/heads/hotfix/VeryHotFix:refs/heads/hotfix/VeryHotFix");
        verify(pushCommand).execute();

        assertThat(urIishArgumentCaptor.getValue().getPath(), is("origin"));

        verifyNoMoreInteractions(gitClient, pluginData, remoteBranch, pushCommand);
    }

    @Test
    public void testAfterMainBuildInternalFail() throws Exception {
        //Setup
        GitflowPluginData pluginData = mock(GitflowPluginData.class);
        when(build.getAction(GitflowPluginData.class)).thenReturn(pluginData);
        when(build.getResult()).thenReturn(Result.FAILURE);

        StartHotFixCause cause = new StartHotFixCause("VeryHotFix", "1.0.2-Snapshot", false);
        StartHotFixAction action = createAction(cause);

        RemoteBranch remoteBranch = mock(RemoteBranch.class);
        when(pluginData.getOrAddRemoteBranch("origin", "hotfix/VeryHotFix")).thenReturn(remoteBranch);

        //Run
        action.afterMainBuildInternal();

        //Check
        verify(pluginData).setDryRun(false);
        verify(pluginData).getOrAddRemoteBranch("origin", "hotfix/VeryHotFix");
        verify(remoteBranch).setLastBuildResult(Result.FAILURE);
        verify(remoteBranch).setLastBuildVersion("1.0.2-Snapshot");

        verifyNoMoreInteractions(gitClient, pluginData);
    }

}