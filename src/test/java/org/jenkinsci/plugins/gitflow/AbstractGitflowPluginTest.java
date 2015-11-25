package org.jenkinsci.plugins.gitflow;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import org.jenkinsci.plugins.gitflow.proxy.gitclient.GitClientProxy;
import org.junit.Before;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;

import hudson.Plugin;
import hudson.PluginWrapper;
import hudson.model.Descriptor;
import hudson.model.Executor;
import hudson.util.VersionNumber;

import jenkins.model.Jenkins;

/**
 * Abstract base class for the <i>Jenkins Gitflow Plugin</i> tests.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
@PrepareForTest({ Executor.class, Jenkins.class })
public abstract class AbstractGitflowPluginTest {

    @Mock
    private Jenkins jenkins;

    @Mock
    private Executor executor;

    @Before
    public void setUp() throws Exception {

        // Build wrapper descriptors are always provided by a statically retrieved Jenkins instance.
        mockStatic(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(this.jenkins);
        when(this.jenkins.getDescriptorOrDie(GitflowBuildWrapper.class)).thenReturn(this.getGitflowBuildWrapperDescriptor());

        // The result status for interrupted builds can be set using a statically retrieved Executor instance.
        mockStatic(Executor.class);
        when(Executor.currentExecutor()).thenReturn(this.executor);

        // For the tests we assume using the oldest supported version of the Git plugin.
        final Plugin gitPlugin = mock(Plugin.class);
        final PluginWrapper gitPluginWrapper = mock(PluginWrapper.class);
        when(this.jenkins.getPlugin("git")).thenReturn(gitPlugin);
        when(gitPlugin.getWrapper()).thenReturn(gitPluginWrapper);
        when(gitPluginWrapper.getVersionNumber()).thenReturn(new VersionNumber("2.1"));

        // For the tests we assume using the oldest supported version of the Git Client plugin.
        final Plugin gitClientPlugin = mock(Plugin.class);
        final PluginWrapper gitClientPluginWrapper = mock(PluginWrapper.class);
        when(this.jenkins.getPlugin("git-client")).thenReturn(gitClientPlugin);
        when(gitClientPlugin.getWrapper()).thenReturn(gitClientPluginWrapper);
        when(gitClientPluginWrapper.getVersionNumber()).thenReturn(GitClientProxy.MINIMAL_VERSION_NUMBER);
    }

    /**
     * Returns a test instance of the {@link GitflowBuildWrapper.DescriptorImpl}.
     * With this method, implementing tests may use a mock or a manually created instance.
     *
     * @return a test instance of the {@link GitflowBuildWrapper.DescriptorImpl}.
     */
    protected abstract Descriptor<?> getGitflowBuildWrapperDescriptor();
}
