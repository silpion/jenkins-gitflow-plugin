package de.silpion.jenkins.plugins.gitflow.action;

import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.silpion.jenkins.plugins.gitflow.cause.NoGitflowCause;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.EnvVars;
import hudson.model.AbstractBuild;

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
    protected Map<String, String> setUpTestGetAdditionalBuildEnvVars() throws InterruptedException, IOException {
        final Map<String, String> expectedAdditionalBuildEnvVars = new HashMap<String, String>();

        // Set build environment with Git commit.
        final EnvVars environment = new EnvVars();
        final ObjectId gitCommitId = ObjectId.zeroId();
        environment.put("GIT_COMMIT", gitCommitId.getName());

        // Mock relevant method calls.
        when(this.build.getEnvironment(this.listener)).thenReturn(environment);
        when(this.git.getRemoteBranchNamesContaining(anyString())).thenReturn(Collections.singletonList("origin/develop"));
        when(this.gitflowBuildWrapperDescriptor.getBranchType("develop")).thenReturn("develop");

        // Define expectations.
        expectedAdditionalBuildEnvVars.put("GIT_SIMPLE_BRANCH_NAME", "develop");
        expectedAdditionalBuildEnvVars.put("GIT_REMOTE_BRANCH_NAME", "origin/develop");
        expectedAdditionalBuildEnvVars.put("GIT_BRANCH_TYPE", "develop");

        return expectedAdditionalBuildEnvVars;
    }
}
