package org.jenkinsci.plugins.gitflow.cause;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

import org.jenkinsci.plugins.gitflow.AbstractGitflowPluginTest;
import org.jenkinsci.plugins.gitflow.GitflowBuildWrapper;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.model.Descriptor;

@RunWith(PowerMockRunner.class)
public class StartHotfixCauseTest extends AbstractGitflowPluginTest {

    @Mock
    private GitflowBuildWrapper.DescriptorImpl gitflowBuildWrapperDescriptor;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(this.gitflowBuildWrapperDescriptor.getBranchType("master")).thenReturn("master");
    }

    @Test
    public void testConstructorForHotfixReleaseVersion() throws Exception {
        assertEquals("1.0", new StartHotfixCause(createRemoteBranch("1.0", "1.0.1")).getHotfixVersion());
        assertEquals("1.0", new StartHotfixCause(createRemoteBranch("1.0", "1.0")).getHotfixVersion());
        assertEquals("1.5", new StartHotfixCause(createRemoteBranch("1.5", "1.5.99")).getHotfixVersion());
    }

    @Test
    public void testConstructorForPublishedFixesReleaseVersion() throws Exception {
        assertEquals("1.0.1", new StartHotfixCause(createRemoteBranch("1.0", "1.0.1")).getPublishedPatchReleaseVersion());
        assertEquals("1.0", new StartHotfixCause(createRemoteBranch("1.0", "1.0")).getPublishedPatchReleaseVersion());
        assertEquals("1.5.99", new StartHotfixCause(createRemoteBranch("1.5", "1.5.99")).getPublishedPatchReleaseVersion());
    }

    @Test
    public void testConstructorForNextHotfixDevelopmentVersion() throws Exception {
        assertEquals("1.0.1-SNAPSHOT", new StartHotfixCause(createRemoteBranch("1.0", "1.0")).getNextPatchDevelopmentVersion());
        assertEquals("1.0.3-SNAPSHOT", new StartHotfixCause(createRemoteBranch("1.0", "1.0.2")).getNextPatchDevelopmentVersion());
        assertEquals("1.0.10-SNAPSHOT", new StartHotfixCause(createRemoteBranch("1.0", "1.0.9")).getNextPatchDevelopmentVersion());
        assertEquals("1.5.1-SNAPSHOT", new StartHotfixCause(createRemoteBranch("1.5", "1.5")).getNextPatchDevelopmentVersion());
        assertEquals("2.18.40-SNAPSHOT", new StartHotfixCause(createRemoteBranch("2.18", "2.18.39")).getNextPatchDevelopmentVersion());
        assertEquals("2.9.100-SNAPSHOT", new StartHotfixCause(createRemoteBranch("2.9", "2.9.99")).getNextPatchDevelopmentVersion());
    }

    private static RemoteBranch createRemoteBranch(final String baseReleaseVersion, final String lastReleaseVersion) {
        final RemoteBranch remoteBranch = new RemoteBranch("master");
        remoteBranch.setBaseReleaseVersion(baseReleaseVersion);
        remoteBranch.setLastReleaseVersion(lastReleaseVersion);
        return remoteBranch;
    }

    @Override
    protected Descriptor<?> getGitflowBuildWrapperDescriptor() {
        return this.gitflowBuildWrapperDescriptor;
    }
}
