package org.jenkinsci.plugins.gitflow.cause;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.matches;
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
public class TestHotfixCauseTest extends AbstractGitflowPluginTest {

    @Mock
    private GitflowBuildWrapper.DescriptorImpl gitflowBuildWrapperDescriptor;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(this.gitflowBuildWrapperDescriptor.getBranchType(matches("hotfix/.*"))).thenReturn("hotfix");
    }

    @Test
    public void testConstructorForHotfixReleaseVersion() throws Exception {
        assertEquals("1.0.1", new TestHotfixCause(createRemoteBranch("hotfix/1.0", "1.0.1-SNAPSHOT")).getHotfixReleaseVersion());
        assertEquals("1.0.2", new TestHotfixCause(createRemoteBranch("hotfix/1.0", "1.0.2-SNAPSHOT")).getHotfixReleaseVersion());
        assertEquals("1.0.9", new TestHotfixCause(createRemoteBranch("hotfix/1.0", "1.0.9-SNAPSHOT")).getHotfixReleaseVersion());
        assertEquals("1.5.0", new TestHotfixCause(createRemoteBranch("hotfix/1.5", "1.5.0-SNAPSHOT")).getHotfixReleaseVersion());
        assertEquals("2.18.39", new TestHotfixCause(createRemoteBranch("hotfix/2.18", "2.18.39-SNAPSHOT")).getHotfixReleaseVersion());
        assertEquals("2.9.99", new TestHotfixCause(createRemoteBranch("hotfix/2.9", "2.9.99-SNAPSHOT")).getHotfixReleaseVersion());
    }

    @Test
    public void testConstructorForNextHotfixDevelopmentVersion() throws Exception {
        assertEquals("1.0.2-SNAPSHOT", new TestHotfixCause(createRemoteBranch("hotfix/1.0", "1.0.1-SNAPSHOT")).getNextHotfixDevelopmentVersion());
        assertEquals("1.0.3-SNAPSHOT", new TestHotfixCause(createRemoteBranch("hotfix/1.0", "1.0.2-SNAPSHOT")).getNextHotfixDevelopmentVersion());
        assertEquals("1.0.10-SNAPSHOT", new TestHotfixCause(createRemoteBranch("hotfix/1.0", "1.0.9-SNAPSHOT")).getNextHotfixDevelopmentVersion());
        assertEquals("1.5.1-SNAPSHOT", new TestHotfixCause(createRemoteBranch("hotfix/1.5", "1.5.0-SNAPSHOT")).getNextHotfixDevelopmentVersion());
        assertEquals("2.18.40-SNAPSHOT", new TestHotfixCause(createRemoteBranch("hotfix/2.18", "2.18.39-SNAPSHOT")).getNextHotfixDevelopmentVersion());
        assertEquals("2.9.100-SNAPSHOT", new TestHotfixCause(createRemoteBranch("hotfix/2.9", "2.9.99-SNAPSHOT")).getNextHotfixDevelopmentVersion());
    }

    private static RemoteBranch createRemoteBranch(final String branchName, final String lastBuildVersion) {
        final RemoteBranch remoteBranch = new RemoteBranch("origin", branchName);
        remoteBranch.setLastBuildVersion(lastBuildVersion);
        return remoteBranch;
    }

    @Override
    protected Descriptor<?> getGitflowBuildWrapperDescriptor() {
        return this.gitflowBuildWrapperDescriptor;
    }
}
