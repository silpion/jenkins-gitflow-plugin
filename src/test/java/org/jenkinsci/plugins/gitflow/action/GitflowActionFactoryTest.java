package org.jenkinsci.plugins.gitflow.action;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jenkinsci.plugins.gitflow.cause.AbstractGitflowCause;
import org.jenkinsci.plugins.gitflow.cause.StartHotFixCause;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.plugins.git.GitSCM;

@RunWith(MockitoJUnitRunner.class)
public class GitflowActionFactoryTest {

    @Mock
    private AbstractBuild build;

    @Mock
    private Launcher launcher;

    @Mock
    private BuildListener listener;

    @Before
    public void setUp() throws Exception {
        AbstractProject project = mock(AbstractProject.class);
        when(project.getScm()).thenReturn(mock(GitSCM.class));
        when(build.getProject()).thenReturn(project);

    }

    @Test
    public void testNewInstanceStartHotFixAction() throws Exception {

        StartHotFixCause startHotFixCause = mock(StartHotFixCause.class);//new StartHotFixCause("name", "next", false);
        when(build.getCause(AbstractGitflowCause.class)).thenReturn(startHotFixCause);

        AbstractGitflowAction<?, ?> abstractGitflowAction = GitflowActionFactory.newInstance(build, launcher, listener);
        assertThat(abstractGitflowAction, is(instanceOf(StartHotFixAction.class)));

    }
}