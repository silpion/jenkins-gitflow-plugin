package org.jenkinsci.plugins.gitflow.action;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jenkinsci.plugins.gitflow.cause.NoGitflowCause;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.model.AbstractBuild;
import hudson.plugins.git.GitTagAction;

/**
 * Unit tests for the {@link NoGitflowAction} class.
 */
@RunWith(PowerMockRunner.class)
public class NoGitflowActionTest extends AbstractGitflowActionTest<NoGitflowAction<AbstractBuild<?, ?>>, NoGitflowCause> {

    @Mock
    private NoGitflowCause cause;

    private NoGitflowAction<AbstractBuild<?, ?>> testAction;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        this.testAction = new NoGitflowAction<AbstractBuild<?, ?>>(this.build, this.launcher, this.listener, this.git);
    }

    /** {@inheritDoc} */
    @Override
    protected NoGitflowAction<AbstractBuild<?, ?>> getTestAction() {
        return this.testAction;
    }

    /** {@inheritDoc} */
    @Override
    protected Map<String, String> setUpTestGetAdditionalBuildEnvVars() throws InterruptedException {
        final Map<String, String> expectedAdditionalBuildEnvVars = new HashMap<String, String>();

        // Mock relevant method calls.
        final GitTagAction gitTagAction = mock(GitTagAction.class);
        when(this.build.getAction(GitTagAction.class)).thenReturn(gitTagAction);
        when(gitTagAction.getTags()).thenReturn(Collections.<String, List<String>>singletonMap("origin/develop", null));
        when(this.gitflowBuildWrapperDescriptor.getBranchType("develop")).thenReturn("develop");

        // Define expectations.
        expectedAdditionalBuildEnvVars.put("GIT_SIMPLE_BRANCH_NAME", "develop");
        expectedAdditionalBuildEnvVars.put("GIT_REMOTE_BRANCH_NAME", "origin/develop");
        expectedAdditionalBuildEnvVars.put("GIT_BRANCH_TYPE", "develop");

        return expectedAdditionalBuildEnvVars;
    }
}
