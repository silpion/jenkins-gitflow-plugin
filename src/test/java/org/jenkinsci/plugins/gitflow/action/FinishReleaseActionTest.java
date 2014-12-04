package org.jenkinsci.plugins.gitflow.action;

import java.util.Collections;
import java.util.Map;

import org.jenkinsci.plugins.gitflow.cause.FinishReleaseCause;
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
        // No expectations, because the main build is omitted.
        return Collections.emptyMap();
    }
}
