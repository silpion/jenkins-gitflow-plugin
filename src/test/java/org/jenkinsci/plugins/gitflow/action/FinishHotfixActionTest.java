package org.jenkinsci.plugins.gitflow.action;

import static org.mockito.Mockito.verify;

import java.util.Collections;
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
import hudson.plugins.git.GitSCM;

@RunWith(PowerMockRunner.class)
public class FinishHotfixActionTest extends AbstractGitflowActionTest<FinishHotfixAction<AbstractBuild<?, ?>>, FinishHotfixCause> {

    private FinishHotfixAction<AbstractBuild<?, ?>> testAction;

    @Mock
    private GitSCM scm;

    @Mock
    private AbstractBuildTypeAction<?> buildTypeAction;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Instanciate the test subject.
        final FinishHotfixCause cause = new FinishHotfixCause(new RemoteBranch("origin", "hotfix/foobar"));
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

        // No expectations, because the main build is omitted.
        return Collections.emptyMap();
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
