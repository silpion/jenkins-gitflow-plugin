package de.silpion.jenkins.plugins.gitflow.action;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import de.silpion.jenkins.plugins.gitflow.AbstractGitflowPluginTest;
import de.silpion.jenkins.plugins.gitflow.GitflowBuildWrapper;
import de.silpion.jenkins.plugins.gitflow.cause.AbstractGitflowCause;
import de.silpion.jenkins.plugins.gitflow.proxy.gitclient.GitClientProxy;
import org.junit.Before;
import org.junit.Test;
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
    protected GitClientProxy git;

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

    @Test
    public void testGetAdditionalBuildEnvVars() throws Exception {
        final A testAction = this.getTestAction();

        // Set up the test for the individual action and retrieve its expected variables.
        // NOTE: Must be executed before 'beforeMainBuild' method.
        final Map<String, String> expectedBuildEnvVars = this.setUpTestGetAdditionalBuildEnvVars();

        // The additionalBuildEnvVars must be created before the main build.
        testAction.beforeMainBuild();

        // The implementations of this class must provide the expectations for the required variables - if they don't omit the main build.
        final String testActionClassName = this.getClass().getSimpleName();
        assertThat(testActionClassName + " must provide an expectation for GIT_SIMPLE_BRANCH_NAME", expectedBuildEnvVars, hasKey("GIT_SIMPLE_BRANCH_NAME"));
        assertThat(testActionClassName + " must provide an expectation for GIT_REMOTE_BRANCH_NAME", expectedBuildEnvVars, hasKey("GIT_REMOTE_BRANCH_NAME"));
        assertThat(testActionClassName + " must provide an expectation for GIT_BRANCH_TYPE", expectedBuildEnvVars, hasKey("GIT_BRANCH_TYPE"));

        // Test the expectations.
        final Map<String, String> buildEnvVars = testAction.getAdditionalBuildEnvVars();
        for (final Map.Entry<String, String> expectedBuildEnvVar : expectedBuildEnvVars.entrySet()) {
            assertThat(buildEnvVars, hasEntry(expectedBuildEnvVar.getKey(), expectedBuildEnvVar.getValue()));
        }
    }

    /**
     * Mocks the relevant method calls for the {@link #testGetAdditionalBuildEnvVars()}
     * test and returns a map containing the expected build environment variables.
     *
     * @return a map containing the expected build environment variables.
     */
    protected abstract Map<String, String> setUpTestGetAdditionalBuildEnvVars() throws InterruptedException, IOException;
}
