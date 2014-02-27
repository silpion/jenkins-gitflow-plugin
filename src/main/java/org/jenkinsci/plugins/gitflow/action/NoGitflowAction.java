package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gitflow.GitflowPluginProperties;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.git.GitTagAction;

/**
 * This class executes some actions that are required when the <i>Gitflow</i> plugin is configured for a project and the standard (non-Gitflow) job is started.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class NoGitflowAction extends AbstractGitflowAction {

    private static final Transformer REMOVE_ORIGIN_PREFIX_TRANSFORMER = new Transformer() {

        public Object transform(final Object input) {
            return StringUtils.removeStart((String) input, "origin/");
        }
    };

    public NoGitflowAction(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
        super(build, launcher, listener);
    }

    @Override
    public void beforeMainBuild() throws IOException, InterruptedException {
        // Nothing to do.
    }

    @Override
    public void afterMainBuild() throws IOException, InterruptedException {
        this.recordGitflowBranchVersions();
    }

    @SuppressWarnings("rawtypes")
    private void recordGitflowBranchVersions() throws IOException {
        final String version = this.buildTypeAction.getCurrentVersion();
        final Set<String> remoteBranchNames = this.build.getAction(GitTagAction.class).getTags().keySet();
        final Collection branchNames = CollectionUtils.collect(remoteBranchNames, REMOVE_ORIGIN_PREFIX_TRANSFORMER);
        new GitflowPluginProperties(this.build.getProject()).saveVersionForBranches(branchNames, version);
    }
}
