package org.jenkinsci.plugins.gitflow.action;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.gitflow.cause.TestReleaseCause;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.model.AbstractBuild;

/**
 * Unit tests for the {@link TestReleaseAction} class.
 */
@RunWith(PowerMockRunner.class)
public class TestReleaseActionTest extends AbstractGitflowActionTest<TestReleaseAction<AbstractBuild<?, ?>>, TestReleaseCause> {

    @Mock
    private TestReleaseCause cause;

    private TestReleaseAction<AbstractBuild<?, ?>> testAction;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        this.testAction = new TestReleaseAction<AbstractBuild<?, ?>>(this.build, this.launcher, this.listener, this.git, this.cause);
    }

    /** {@inheritDoc} */
    @Override
    protected TestReleaseAction<AbstractBuild<?, ?>> getTestAction() {
        return this.testAction;
    }

    /** {@inheritDoc} */
    @Override
    protected Map<String, String> setUpTestGetAdditionalBuildEnvVars() throws InterruptedException {
        final Map<String, String> expectedAdditionalBuildEnvVars = new HashMap<String, String>();

        // Mock relevant method calls.
        when(this.cause.getReleaseBranch()).thenReturn("release/1.0");
        when(this.git.getHeadRev(anyString())).thenReturn(ObjectId.zeroId());
        when(this.gitflowBuildWrapperDescriptor.getBranchType(startsWith("release/"))).thenReturn("release");

        // Define expectations.
        expectedAdditionalBuildEnvVars.put("GIT_SIMPLE_BRANCH_NAME", "release/1.0");
        expectedAdditionalBuildEnvVars.put("GIT_REMOTE_BRANCH_NAME", "origin/release/1.0");
        expectedAdditionalBuildEnvVars.put("GIT_BRANCH_TYPE", "release");

        return expectedAdditionalBuildEnvVars;
    }
}
