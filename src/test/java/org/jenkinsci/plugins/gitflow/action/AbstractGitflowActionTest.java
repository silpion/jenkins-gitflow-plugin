package org.jenkinsci.plugins.gitflow.action;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jenkinsci.plugins.gitflow.AbstractGitflowPluginTest;
import org.jenkinsci.plugins.gitflow.GitflowBuildWrapper;
import org.jenkinsci.plugins.gitflow.cause.AbstractGitflowCause;
import org.jenkinsci.plugins.gitflow.gitclient.GitClientDelegate;
import org.junit.Before;
import org.mockito.Mock;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.StreamBuildListener;
import hudson.util.NullStream;

/**
 * Abstract base class for to test the different {@link AbstractGitflowAction}
 * implementations. Contains unit tests for the methods provided by that class.
 *
 * @param <A> the action to be tested.
 * @param <C> the <i>Gitflow</i> cause for the action to be tested.
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public abstract class AbstractGitflowActionTest<A extends AbstractGitflowAction<AbstractBuild<?, ?>, C>, C extends AbstractGitflowCause> extends AbstractGitflowPluginTest {

    @Mock
    @SuppressWarnings("rawtypes")
    protected AbstractBuild build;

    @Mock
    protected Launcher launcher;

    protected BuildListener listener = new StreamBuildListener(new NullStream());

    @Mock
    protected GitClientDelegate git;

    @Mock
    protected GitflowBuildWrapper.DescriptorImpl gitflowBuildWrapperDescriptor;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Initialise the project mock and attach it to the build mock.
        final AbstractProject<?, ?> project = mock(AbstractProject.class);
        when(this.build.getProject()).thenReturn(project);
    }

    /** {@inheritDoc} */
    @Override
    public GitflowBuildWrapper.DescriptorImpl getGitflowBuildWrapperDescriptor() {
        return this.gitflowBuildWrapperDescriptor;
    }

    /**
     * Returns the action to be tested.
     *
     * @return the action to be tested.
     */
    protected abstract A getTestAction();
}
