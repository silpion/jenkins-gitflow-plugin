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

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.gitclient.PushCommand;
import org.jenkinsci.plugins.gitflow.action.buildtype.AbstractBuildTypeAction;
import org.jenkinsci.plugins.gitflow.cause.FinishHotfixCause;
import org.jenkinsci.plugins.gitflow.data.GitflowPluginData;
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
public class FinishHotfixActionTest {

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
    private AbstractBuildTypeAction<?> buildTypeAction;

    @Captor
    private ArgumentCaptor<URIish> urIishArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        AbstractProject project = mock(AbstractProject.class);
        when(project.getScm()).thenReturn(scm);
        when(build.getProject()).thenReturn(project);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(listener.getLogger()).thenReturn(new PrintStream(outputStream));
    }

    //**********************************************************************************************************************************************************
    //
    // Helper
    //
    //**********************************************************************************************************************************************************

    //TODO This Method only exist for make UnitTesting work, the AbstractGitflowAction needs some refactoring
    private FinishHotfixAction createAction(FinishHotfixCause cause) throws IOException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        FinishHotfixAction action = new FinishHotfixAction(build, launcher, listener, cause);
        setPrivateFinalField(action, "git", gitClient);
        setPrivateFinalField(action, "buildTypeAction", buildTypeAction);
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
        FinishHotfixCause cause = new FinishHotfixCause("hotfix/foobar", false);
        FinishHotfixAction action = createAction(cause);

        ObjectId id = ObjectId.zeroId();
        when(gitClient.getRemoteUrl("origin")).thenReturn("someOriginUrl");
        when(gitClient.getHeadRev("someOriginUrl", "master")).thenReturn(id);

        //Run
        action.beforeMainBuildInternal();

        //Check
        verify(gitClient).getRemoteUrl("origin");
        verify(gitClient).getHeadRev("someOriginUrl", "master");
        verify(gitClient).checkout(id.getName());

        verifyNoMoreInteractions(gitClient);
    }

    @Test
    public void testAfterMainBuildInternalSuccess() throws Exception {
        //Setup
        FinishHotfixCause cause = new FinishHotfixCause("hotfix/foobar", false);
        FinishHotfixAction action = createAction(cause);

        GitflowPluginData pluginData = mock(GitflowPluginData.class);
        when(build.getAction(GitflowPluginData.class)).thenReturn(pluginData);
        when(build.getResult()).thenReturn(Result.SUCCESS);

        PushCommand pushCommand = mock(PushCommand.class);
        when(gitClient.push()).thenReturn(pushCommand);
        when(pushCommand.to(any(URIish.class))).thenReturn(pushCommand);
        when(pushCommand.ref(any(String.class))).thenReturn(pushCommand);

        //Run
        action.afterMainBuildInternal();

        //Check
        verify(gitClient).push();
        verify(pushCommand).to(urIishArgumentCaptor.capture());
        verify(pushCommand).ref(":hotfix/foobar");
        verify(pushCommand).execute();

        assertThat(urIishArgumentCaptor.getValue().getPath(), is("origin"));
        verifyNoMoreInteractions(gitClient, pluginData, pushCommand);
    }

    @Test
    public void testAfterMainBuildInternalFail() throws Exception {
        //Setup
        FinishHotfixCause cause = new FinishHotfixCause("hotfix/foobar", false);
        FinishHotfixAction action = createAction(cause);

        GitflowPluginData pluginData = mock(GitflowPluginData.class);
        when(build.getAction(GitflowPluginData.class)).thenReturn(pluginData);
        when(build.getResult()).thenReturn(Result.FAILURE);

        //Run
        action.afterMainBuildInternal();

        //Check
        verifyNoMoreInteractions(gitClient, pluginData);
    }

}