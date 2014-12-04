package org.jenkinsci.plugins.gitflow;

import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import org.jenkinsci.plugins.gitflow.gitclient.GitClient22Proxy;
import org.junit.Before;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;

import hudson.Plugin;
import hudson.PluginWrapper;
import hudson.model.Descriptor;
import hudson.model.Executor;

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

    @Mock
    private Plugin gitPlugin;

    @Mock
    private PluginWrapper gitPluginWrapper;

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
        when(this.jenkins.getPlugin("git")).thenReturn(this.gitPlugin);
        when(this.gitPlugin.getWrapper()).thenReturn(this.gitPluginWrapper);
        when(this.gitPluginWrapper.getVersionNumber()).thenReturn(GitClient22Proxy.BASE_VERSION_NUMBER);
    }

    /**
     * Returns a test instance of the {@link GitflowBuildWrapper.DescriptorImpl}.
     * With this method, implementing tests may use a mock or a manually created instance.
     *
     * @return a test instance of the {@link GitflowBuildWrapper.DescriptorImpl}.
     */
    protected abstract Descriptor<?> getGitflowBuildWrapperDescriptor();
}
