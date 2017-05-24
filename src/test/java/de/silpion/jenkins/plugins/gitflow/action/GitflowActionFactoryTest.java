package de.silpion.jenkins.plugins.gitflow.action;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import de.silpion.jenkins.plugins.gitflow.AbstractGitflowPluginTest;
import de.silpion.jenkins.plugins.gitflow.cause.AbstractGitflowCause;
import de.silpion.jenkins.plugins.gitflow.cause.StartHotfixCause;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.plugins.git.GitSCM;

@RunWith(PowerMockRunner.class)
public class GitflowActionFactoryTest extends AbstractGitflowPluginTest {

    @Mock
    private AbstractBuild build;

    @Mock
    private Launcher launcher;

    @Mock
    private BuildListener listener;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        AbstractProject project = mock(AbstractProject.class);
        when(project.getScm()).thenReturn(mock(GitSCM.class));
        when(build.getProject()).thenReturn(project);

    }

    /** {@inheritDoc} */
    @Override
    protected Descriptor<?> getGitflowBuildWrapperDescriptor() {
        // We currently don't need the descriptor in this test.
        return null;
    }

    @Test
    public void testNewInstanceStartHotfixAction() throws Exception {

        StartHotfixCause startHotfixCause = mock(StartHotfixCause.class);
        when(build.getCause(AbstractGitflowCause.class)).thenReturn(startHotfixCause);

        AbstractGitflowAction<?, ?> abstractGitflowAction = GitflowActionFactory.newInstance(build, launcher, listener);
        assertThat(abstractGitflowAction, is(instanceOf(StartHotfixAction.class)));

    }
}