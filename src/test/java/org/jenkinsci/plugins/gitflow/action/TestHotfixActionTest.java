package org.jenkinsci.plugins.gitflow.action;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.jenkinsci.plugins.gitflow.action.buildtype.AbstractBuildTypeAction;
import org.jenkinsci.plugins.gitflow.cause.TestHotfixCause;
import org.jenkinsci.plugins.gitflow.data.GitflowPluginData;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.verification.Times;
import org.mockito.runners.MockitoJUnitRunner;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.git.GitSCM;

@RunWith(MockitoJUnitRunner.class)
public class TestHotfixActionTest {

    @Mock
    private AbstractBuild build;

    @Mock
    private Launcher launcher;

    @Mock
    private BuildListener listener;

    @Mock
    private GitSCM scm;

    @Mock
    private GitClient gitClient;

    @Mock
    private AbstractBuildTypeAction<?> buildTypeAction;

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
    private TestHotfixAction createAction(TestHotfixCause cause) throws IOException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        TestHotfixAction action = new TestHotfixAction(build, launcher, listener, cause);
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


    @Test
    public void testBeforeMainBuildInternal() throws Exception {
        //Setup
        String hotfixBranch = "hotfix/foobar";
        String hotfixReleaseVersion = "1.2.3";
        TestHotfixCause cause = new TestHotfixCause(hotfixBranch, hotfixReleaseVersion,"1.2.4-SNAPSHOT", false);
        TestHotfixAction action = createAction(cause);

        ObjectId id = ObjectId.zeroId();
        String remoteRepoUrl = "someOriginUrl";
        when(gitClient.getRemoteUrl("origin")).thenReturn(remoteRepoUrl);
        when(gitClient.getHeadRev(remoteRepoUrl, hotfixBranch)).thenReturn(id);

        List<String> changeFiles = Arrays.asList("pom.xml", "child1/pom.xml", "child2/pom.xml", "child3/pom.xml");
        when(buildTypeAction.updateVersion(hotfixReleaseVersion)).thenReturn(changeFiles);

        //Run
        action.beforeMainBuildInternal();

        //Check
        verify(gitClient).getRemoteUrl("origin");
        verify(gitClient).getHeadRev(remoteRepoUrl, hotfixBranch);
        verify(gitClient).checkout(id.getName());

        verify(gitClient).add("pom.xml");
        verify(gitClient).add("child1/pom.xml");
        verify(gitClient).add("child2/pom.xml");
        verify(gitClient).add("child3/pom.xml");
        verify(gitClient).commit(any(String.class));

        verifyNoMoreInteractions(gitClient);

    }

    @Test
    public void testAfterMainBuildInternal() throws Exception {
        //Setup
        String hotfixBranch = "hotfix/foobar";
        String hotfixVersion = "1.2.3";
        String nextHotfixVersion = "1.2.4-SNAPSHOT";

        GitflowPluginData pluginData = mock(GitflowPluginData.class);
        when(build.getAction(GitflowPluginData.class)).thenReturn(pluginData);

        TestHotfixCause cause = new TestHotfixCause(hotfixBranch, hotfixVersion, nextHotfixVersion, false);
        TestHotfixAction action = createAction(cause);

        when(build.getResult()).thenReturn(Result.SUCCESS);

        List<String> changeFiles = Arrays.asList("pom.xml", "child1/pom.xml", "child2/pom.xml", "child3/pom.xml");
        when(buildTypeAction.updateVersion(nextHotfixVersion)).thenReturn(changeFiles);

        //Run
        action.afterMainBuildInternal();

        //Check
        verify(pluginData).setDryRun(false);
        verify(gitClient, times(2)).push("origin", "HEAD:refs/heads/" + hotfixBranch);

        verify(gitClient).add("pom.xml");
        verify(gitClient).add("child1/pom.xml");
        verify(gitClient).add("child2/pom.xml");
        verify(gitClient).add("child3/pom.xml");
        verify(gitClient).commit(any(String.class));

        verify(pluginData).recordRemoteBranch("origin", hotfixBranch,Result.SUCCESS,nextHotfixVersion);

        verifyNoMoreInteractions(gitClient, pluginData);
    }

    @Test
    public void testAfterMainBuildInternalFail() throws Exception {
        //Setup
        String hotfixBranch = "hotfix/foobar";
        String lastVersion = "1.2.3-SNAPSHOT";
        String hotfixVersion = "1.2.3";
        String nextHotfixVersion = "1.2.4-SNAPSHOT";

        GitflowPluginData pluginData = mock(GitflowPluginData.class);
        when(build.getAction(GitflowPluginData.class)).thenReturn(pluginData);
        RemoteBranch remoteBranch = mock(RemoteBranch.class);

        when(remoteBranch.getLastBuildVersion()).thenReturn(lastVersion);
        when(pluginData.getRemoteBranch("origin", hotfixBranch)).thenReturn(remoteBranch);

        TestHotfixCause cause = new TestHotfixCause(hotfixBranch, hotfixVersion, nextHotfixVersion, false);
        TestHotfixAction action = createAction(cause);

        when(build.getResult()).thenReturn(Result.FAILURE);

        //Run
        action.afterMainBuildInternal();

        //Check
        verify(pluginData).setDryRun(false);
        verify(pluginData).getRemoteBranch("origin", hotfixBranch);
        verify(pluginData).recordRemoteBranch("origin", hotfixBranch,Result.FAILURE,lastVersion);
        verify(remoteBranch).getLastBuildVersion();

        verifyNoMoreInteractions(gitClient, pluginData, remoteBranch);

    }
}