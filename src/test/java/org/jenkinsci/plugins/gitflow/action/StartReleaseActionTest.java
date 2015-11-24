package org.jenkinsci.plugins.gitflow.action;

import static org.mockito.Matchers.startsWith;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.HashMap;
import java.util.Map;

import org.jenkinsci.plugins.gitflow.cause.StartReleaseCause;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.model.AbstractBuild;

/**
 * Unit tests for the {@link StartReleaseAction} class.
 */
@RunWith(PowerMockRunner.class)
public class StartReleaseActionTest extends AbstractGitflowActionTest<StartReleaseAction<AbstractBuild<?, ?>>, StartReleaseCause> {

    @Mock
    private StartReleaseCause cause;

    private StartReleaseAction<AbstractBuild<?, ?>> testAction;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        this.testAction = new StartReleaseAction<AbstractBuild<?, ?>>(this.build, this.launcher, this.listener, this.git, this.cause);
    }

    /** {@inheritDoc} */
    @Override
    protected StartReleaseAction<AbstractBuild<?, ?>> getTestAction() {
        return this.testAction;
    }

    /** {@inheritDoc} */
    @Override
    protected Map<String, String> setUpTestGetAdditionalBuildEnvVars() {
        final Map<String, String> expectedAdditionalBuildEnvVars = new HashMap<String, String>();

        // Mock relevant method calls.
        when(this.cause.getReleaseVersion()).thenReturn("1.0");
        when(this.cause.getReleaseBranch()).thenReturn("release/1.0");
        when(this.gitflowBuildWrapperDescriptor.getReleaseBranchPrefix()).thenReturn("release/");
        when(this.gitflowBuildWrapperDescriptor.getBranchType(startsWith("release/"))).thenReturn("release");

        // Define expectations.
        expectedAdditionalBuildEnvVars.put("GIT_SIMPLE_BRANCH_NAME", "release/1.0");
        expectedAdditionalBuildEnvVars.put("GIT_REMOTE_BRANCH_NAME", "origin/release/1.0");
        expectedAdditionalBuildEnvVars.put("GIT_BRANCH_TYPE", "release");

        return expectedAdditionalBuildEnvVars;
    }
}
