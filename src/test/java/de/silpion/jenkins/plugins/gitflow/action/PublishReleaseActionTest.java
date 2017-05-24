package de.silpion.jenkins.plugins.gitflow.action;

import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.HashMap;
import java.util.Map;

import de.silpion.jenkins.plugins.gitflow.cause.PublishReleaseCause;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.model.AbstractBuild;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;

/**
 * Unit tests for the {@link PublishReleaseAction} class.
 */
@RunWith(PowerMockRunner.class)
public class PublishReleaseActionTest extends AbstractGitflowActionTest<PublishReleaseAction<AbstractBuild<?, ?>>, PublishReleaseCause> {

    @Mock
    private PublishReleaseCause cause;

    private PublishReleaseAction<AbstractBuild<?, ?>> testAction;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        this.testAction = new PublishReleaseAction<AbstractBuild<?, ?>>(this.build, this.launcher, this.listener, this.git, this.cause);
    }

    /** {@inheritDoc} */
    @Override
    protected PublishReleaseAction<AbstractBuild<?, ?>> getTestAction() {
        return this.testAction;
    }

    /** {@inheritDoc} */
    @Override
    protected Map<String, String> setUpTestGetAdditionalBuildEnvVars() throws InterruptedException {
        final Map<String, String> expectedAdditionalBuildEnvVars = new HashMap<String, String>();

        // Mock relevant method calls.
        PowerMockito.when(this.gitflowBuildWrapperDescriptor.getMasterBranch()).thenReturn("master");
        when(this.cause.getLastPatchReleaseCommit()).thenReturn(ObjectId.zeroId());
        PowerMockito.when(this.git.getHeadRev(anyString())).thenReturn(ObjectId.zeroId());
        when(this.cause.getReleaseBranch()).thenReturn("release/1.0");
        PowerMockito.when(this.gitflowBuildWrapperDescriptor.getBranchType("master")).thenReturn("master");

        // Mock build data.
        final BuildData buildData = mock(BuildData.class);
        when(this.build.getAction(BuildData.class)).thenReturn(buildData);
        when(buildData.getBuildsByBranchName()).thenReturn(new HashMap<String, Build>());

        // Define expectations.
        expectedAdditionalBuildEnvVars.put("GIT_SIMPLE_BRANCH_NAME", "master");
        expectedAdditionalBuildEnvVars.put("GIT_REMOTE_BRANCH_NAME", "origin/master");
        expectedAdditionalBuildEnvVars.put("GIT_BRANCH_TYPE", "master");

        return expectedAdditionalBuildEnvVars;
    }
}
