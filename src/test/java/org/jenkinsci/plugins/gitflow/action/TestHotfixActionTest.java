package org.jenkinsci.plugins.gitflow.action;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.gitclient.PushCommand;
import org.jenkinsci.plugins.gitflow.action.buildtype.AbstractBuildTypeAction;
import org.jenkinsci.plugins.gitflow.action.buildtype.BuildTypeActionFactory;
import org.jenkinsci.plugins.gitflow.cause.TestHotfixCause;
import org.jenkinsci.plugins.gitflow.data.GitflowPluginData;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;
import org.jenkinsci.plugins.gitflow.gitclient.GitClientDelegate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.git.GitSCM;

import jenkins.model.Jenkins;

@PrepareForTest({ Jenkins.class, BuildTypeActionFactory.class })
@RunWith(PowerMockRunner.class)
public class TestHotfixActionTest {

    @Mock
    private Jenkins jenkins;

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
    private AbstractBuildTypeAction buildTypeAction;

    @Captor
    private ArgumentCaptor<URIish> urIishArgumentCaptor;

    @Before
    @SuppressWarnings("unchecked assignment")
    public void setUp() throws Exception {
        mockStatic(BuildTypeActionFactory.class);
        when(BuildTypeActionFactory.newInstance(build, launcher, listener)).thenReturn(buildTypeAction);

        AbstractProject project = mock(AbstractProject.class);
        when(project.getScm()).thenReturn(scm);
        when(build.getProject()).thenReturn(project);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(listener.getLogger()).thenReturn(new PrintStream(outputStream));
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
        String hotfixReleaseVersion = "1.2.3";
        TestHotfixCause cause = new TestHotfixCause(hotfixBranch, hotfixReleaseVersion, "1.2.4-SNAPSHOT", false);
        TestHotfixAction action = new TestHotfixAction(build, launcher, listener, gitClient, cause);

        ObjectId id = ObjectId.zeroId();
        String remoteRepoUrl = "someOriginUrl";
        when(gitClient.getRemoteUrl("origin")).thenReturn(remoteRepoUrl);
        when(gitClient.getHeadRev(remoteRepoUrl, hotfixBranch)).thenReturn(id);

        List<String> changeFiles = Arrays.asList("pom.xml", "child1/pom.xml", "child2/pom.xml", "child3/pom.xml");
        when(buildTypeAction.updateVersion(hotfixReleaseVersion)).thenReturn(changeFiles);

        //Run
        action.beforeMainBuildInternal();

        //Check
        verify(gitClient).setGitflowActionName(action.getActionName());
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
        RemoteBranch remoteBranch = mock(RemoteBranch.class);
        PushCommand pushCommand = mock(PushCommand.class);

        when(build.getAction(GitflowPluginData.class)).thenReturn(pluginData);

        TestHotfixCause cause = new TestHotfixCause(hotfixBranch, hotfixVersion, nextHotfixVersion, false);
        TestHotfixAction action = new TestHotfixAction(build, launcher, listener, gitClient, cause);

        when(build.getResult()).thenReturn(Result.SUCCESS);

        List<String> changeFiles = Arrays.asList("pom.xml", "child1/pom.xml", "child2/pom.xml", "child3/pom.xml");
        when(buildTypeAction.updateVersion(nextHotfixVersion)).thenReturn(changeFiles);

        when(pluginData.getOrAddRemoteBranch("origin", "hotfix/foobar")).thenReturn(remoteBranch);
        when(gitClient.push()).thenReturn(pushCommand);
        when(pushCommand.to(any(URIish.class))).thenReturn(pushCommand);
        when(pushCommand.ref(any(String.class))).thenReturn(pushCommand);
        //Run
        action.afterMainBuildInternal();

        //Check
        verify(pluginData).setDryRun(false);
        verify(pluginData).getOrAddRemoteBranch("origin", hotfixBranch);
        verify(gitClient).setGitflowActionName(action.getActionName());
        verify(gitClient, times(2)).push();

        verify(gitClient).add("pom.xml");
        verify(gitClient).add("child1/pom.xml");
        verify(gitClient).add("child2/pom.xml");
        verify(gitClient).add("child3/pom.xml");
        verify(gitClient).commit(any(String.class));

        verify(remoteBranch).setLastBuildResult(Result.SUCCESS);
        verify(remoteBranch).setLastBuildVersion(nextHotfixVersion);

        verify(pushCommand, times(2)).to(urIishArgumentCaptor.capture());
        verify(pushCommand, times(2)).ref("HEAD:refs/heads/" + hotfixBranch);
        verify(pushCommand, times(2)).execute();

        assertThat(urIishArgumentCaptor.getValue().getPath(), is("origin"));

        verifyNoMoreInteractions(gitClient, pluginData, remoteBranch, pushCommand);
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
        when(pluginData.getOrAddRemoteBranch("origin", hotfixBranch)).thenReturn(remoteBranch);

        TestHotfixCause cause = new TestHotfixCause(hotfixBranch, hotfixVersion, nextHotfixVersion, false);
        TestHotfixAction action = new TestHotfixAction(build, launcher, listener, gitClient, cause);

        when(build.getResult()).thenReturn(Result.FAILURE);

        //Run
        action.afterMainBuildInternal();

        //Check
        verify(gitClient).setGitflowActionName(action.getActionName());
        verify(pluginData).setDryRun(false);
        verify(pluginData).getRemoteBranch("origin", hotfixBranch);
        verify(pluginData).getOrAddRemoteBranch("origin", hotfixBranch);

        verify(remoteBranch).setLastBuildResult(Result.FAILURE);
        verify(remoteBranch).setLastBuildVersion(lastVersion);

        verify(remoteBranch).getLastBuildVersion();

        verifyNoMoreInteractions(gitClient, pluginData, remoteBranch);

    }
}