package org.jenkinsci.plugins.gitflow.action;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.gitclient.PushCommand;
import org.jenkinsci.plugins.gitflow.cause.PublishHotfixCause;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.model.AbstractBuild;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;

/**
 * Unit tests for the {@link PublishHotfixAction} class.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
@RunWith(PowerMockRunner.class)
public class PublishHotfixActionTest extends AbstractGitflowActionTest<PublishHotfixAction<AbstractBuild<?, ?>>, PublishHotfixCause> {

    @Mock
    private PublishHotfixCause cause;

    private PublishHotfixAction<AbstractBuild<?, ?>> testAction;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        this.testAction = new PublishHotfixAction<AbstractBuild<?, ?>>(this.build, this.launcher, this.listener, this.git, this.cause);
    }

    /** {@inheritDoc} */
    @Override
    protected PublishHotfixAction<AbstractBuild<?, ?>> getTestAction() {
        return this.testAction;
    }

    /** {@inheritDoc} */
    @Override
    protected Map<String, String> setUpTestGetAdditionalBuildEnvVars() throws InterruptedException {

        // Mock relevant method calls.
        when(this.gitflowBuildWrapperDescriptor.getMasterBranch()).thenReturn("master");
        when(this.cause.getLastPatchReleaseCommit()).thenReturn(ObjectId.zeroId().getName());
        when(this.git.getHeadRev(anyString(), anyString())).thenReturn(ObjectId.zeroId());
        when(this.cause.getHotfixBranch()).thenReturn("hotfix/1.0");

        // Mock push command.
        final PushCommand pushCommand = mock(PushCommand.class);
        when(this.git.push()).thenReturn(pushCommand);
        when(pushCommand.ref(anyString())).thenReturn(pushCommand);
        when(pushCommand.to(any(URIish.class))).thenReturn(pushCommand);

        // Mock build data.
        final BuildData buildData = mock(BuildData.class);
        when(this.build.getAction(BuildData.class)).thenReturn(buildData);
        when(buildData.getBuildsByBranchName()).thenReturn(new HashMap<String, Build>());

        // No expectations, because the main build is omitted.
        return Collections.emptyMap();
    }
}
