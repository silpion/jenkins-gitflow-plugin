package de.silpion.jenkins.plugins.gitflow.cause;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.matches;
import static org.powermock.api.mockito.PowerMockito.when;

import de.silpion.jenkins.plugins.gitflow.AbstractGitflowPluginTest;
import de.silpion.jenkins.plugins.gitflow.data.RemoteBranch;
import de.silpion.jenkins.plugins.gitflow.GitflowBuildWrapper;
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
        assertEquals("1.0.1", new TestHotfixCause(createRemoteBranch("hotfix/1.0", "1.0.1-SNAPSHOT")).getPatchReleaseVersion());
        assertEquals("1.0.2", new TestHotfixCause(createRemoteBranch("hotfix/1.0", "1.0.2-SNAPSHOT")).getPatchReleaseVersion());
        assertEquals("1.0.9", new TestHotfixCause(createRemoteBranch("hotfix/1.0", "1.0.9-SNAPSHOT")).getPatchReleaseVersion());
        assertEquals("1.5.0", new TestHotfixCause(createRemoteBranch("hotfix/1.5", "1.5.0-SNAPSHOT")).getPatchReleaseVersion());
        assertEquals("2.18.39", new TestHotfixCause(createRemoteBranch("hotfix/2.18", "2.18.39-SNAPSHOT")).getPatchReleaseVersion());
        assertEquals("2.9.99", new TestHotfixCause(createRemoteBranch("hotfix/2.9", "2.9.99-SNAPSHOT")).getPatchReleaseVersion());
    }

    @Test
    public void testConstructorForNextHotfixDevelopmentVersion() throws Exception {
        assertEquals("1.0.2-SNAPSHOT", new TestHotfixCause(createRemoteBranch("hotfix/1.0", "1.0.1-SNAPSHOT")).getNextPatchDevelopmentVersion());
        assertEquals("1.0.3-SNAPSHOT", new TestHotfixCause(createRemoteBranch("hotfix/1.0", "1.0.2-SNAPSHOT")).getNextPatchDevelopmentVersion());
        assertEquals("1.0.10-SNAPSHOT", new TestHotfixCause(createRemoteBranch("hotfix/1.0", "1.0.9-SNAPSHOT")).getNextPatchDevelopmentVersion());
        assertEquals("1.5.1-SNAPSHOT", new TestHotfixCause(createRemoteBranch("hotfix/1.5", "1.5.0-SNAPSHOT")).getNextPatchDevelopmentVersion());
        assertEquals("2.18.40-SNAPSHOT", new TestHotfixCause(createRemoteBranch("hotfix/2.18", "2.18.39-SNAPSHOT")).getNextPatchDevelopmentVersion());
        assertEquals("2.9.100-SNAPSHOT", new TestHotfixCause(createRemoteBranch("hotfix/2.9", "2.9.99-SNAPSHOT")).getNextPatchDevelopmentVersion());
    }

    private static RemoteBranch createRemoteBranch(final String branchName, final String lastBuildVersion) {
        final RemoteBranch remoteBranch = new RemoteBranch(branchName);
        remoteBranch.setLastBuildVersion(lastBuildVersion);
        return remoteBranch;
    }

    @Override
    protected Descriptor<?> getGitflowBuildWrapperDescriptor() {
        return this.gitflowBuildWrapperDescriptor;
    }
}
