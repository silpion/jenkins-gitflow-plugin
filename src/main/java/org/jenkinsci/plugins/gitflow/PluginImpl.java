package org.jenkinsci.plugins.gitflow;

import hudson.Plugin;

/**
 * Base class of the <i>Jenkins Gitflow Plugin</i>.
 * <p/>
 * Copied from {@code org.jvnet.hudson.plugins.m2release.PluginImpl}.
 *
 * @author Kohsuke Kawaguchi
 */
public class PluginImpl extends Plugin {

    @Override
    public void start() throws Exception {
        // this permission designates a wrong parent group, which introduces a classloading problem
        // like HUDSON-4172.
        //
        // As a work around, force loading of this permission so that by the time we start loading ACLs,
        // we have this instance already registered, thereby avoiding a lookup.
        GitflowBuildWrapper.DescriptorImpl.EXECUTE_GITFLOW.toString();
    }
}
