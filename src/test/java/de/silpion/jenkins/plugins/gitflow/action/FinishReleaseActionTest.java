package de.silpion.jenkins.plugins.gitflow.action;

import static org.mockito.Matchers.startsWith;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.HashMap;
import java.util.Map;

import de.silpion.jenkins.plugins.gitflow.cause.FinishReleaseCause;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.model.AbstractBuild;

/**
 * Unit tests for the {@link FinishReleaseAction} class.
 */
@RunWith(PowerMockRunner.class)
public class FinishReleaseActionTest extends AbstractGitflowActionTest<FinishReleaseAction<AbstractBuild<?, ?>>, FinishReleaseCause> {

    @Mock
    private FinishReleaseCause cause;

    private FinishReleaseAction<AbstractBuild<?, ?>> testAction;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        this.testAction = new FinishReleaseAction<AbstractBuild<?, ?>>(this.build, this.launcher, this.listener, this.git, this.cause);
    }

    /** {@inheritDoc} */
    @Override
    protected FinishReleaseAction<AbstractBuild<?, ?>> getTestAction() {
        return this.testAction;
    }

    /** {@inheritDoc} */
    @Override
    protected Map<String, String> setUpTestGetAdditionalBuildEnvVars() throws InterruptedException {
        final Map<String, String> expectedAdditionalBuildEnvVars = new HashMap<String, String>();

        // Mock relevant method calls.
        when(this.cause.getReleaseBranch()).thenReturn("release/foobar");
        when(this.gitflowBuildWrapperDescriptor.getBranchType(startsWith("release/"))).thenReturn("release");

        // Define expectations.
        expectedAdditionalBuildEnvVars.put("GIT_SIMPLE_BRANCH_NAME", "release/foobar");
        expectedAdditionalBuildEnvVars.put("GIT_REMOTE_BRANCH_NAME", "origin/release/foobar");
        expectedAdditionalBuildEnvVars.put("GIT_BRANCH_TYPE", "release");

        return expectedAdditionalBuildEnvVars;
    }
}
