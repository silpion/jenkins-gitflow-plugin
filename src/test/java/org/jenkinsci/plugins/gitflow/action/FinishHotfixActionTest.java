package org.jenkinsci.plugins.gitflow.action;

import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.HashMap;
import java.util.Map;

import org.jenkinsci.plugins.gitflow.action.buildtype.AbstractBuildTypeAction;
import org.jenkinsci.plugins.gitflow.cause.FinishHotfixCause;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.model.AbstractBuild;

@RunWith(PowerMockRunner.class)
public class FinishHotfixActionTest extends AbstractGitflowActionTest<FinishHotfixAction<AbstractBuild<?, ?>>, FinishHotfixCause> {

    private FinishHotfixAction<AbstractBuild<?, ?>> testAction;

    @Mock
    private AbstractBuildTypeAction<?> buildTypeAction;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Instanciate the test subject.
        final FinishHotfixCause cause = new FinishHotfixCause(new RemoteBranch("hotfix/foobar"));
        this.testAction = new FinishHotfixAction<AbstractBuild<?, ?>>(this.build, this.launcher, this.listener, this.git, cause);
    }

    /** {@inheritDoc} */
    @Override
    protected FinishHotfixAction<AbstractBuild<?, ?>> getTestAction() {
        return this.testAction;
    }

    /** {@inheritDoc} */
    @Override
    protected Map<String, String> setUpTestGetAdditionalBuildEnvVars() throws InterruptedException {
        final Map<String, String> expectedAdditionalBuildEnvVars = new HashMap<String, String>();

        // Mock relevant method calls.
        when(this.gitflowBuildWrapperDescriptor.getBranchType(startsWith("hotfix/"))).thenReturn("hotfix");

        // Define expectations.
        expectedAdditionalBuildEnvVars.put("GIT_SIMPLE_BRANCH_NAME", "hotfix/foobar");
        expectedAdditionalBuildEnvVars.put("GIT_REMOTE_BRANCH_NAME", "origin/hotfix/foobar");
        expectedAdditionalBuildEnvVars.put("GIT_BRANCH_TYPE", "hotfix");

        return expectedAdditionalBuildEnvVars;
    }

    //**********************************************************************************************************************************************************
    //
    // Tests
    //
    //**********************************************************************************************************************************************************

    @Test
    public void testBeforeMainBuildInternal() throws Exception {

        //Run
        this.testAction.beforeMainBuildInternal();

        //Check
        verify(this.git).push("origin", ":refs/heads/hotfix/foobar");
    }
}
