package org.jenkinsci.plugins.gitflow.action;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;

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
    public void beforeMainBuildInternal() throws IOException, InterruptedException {
        // Nothing to do.
    }

    @Override
    @SuppressWarnings("unchecked")
    public void afterMainBuildInternal() throws IOException, InterruptedException {

        // Record the version for the Gitflow branches that have been built.
        final Collection<String> remoteBranchNames = this.build.getAction(GitTagAction.class).getTags().keySet();
        final Collection<String> simpleBranchNames = CollectionUtils.collect(remoteBranchNames, REMOVE_ORIGIN_PREFIX_TRANSFORMER);
        this.gitflowPluginProperties.saveVersionForBranches(simpleBranchNames, this.buildTypeAction.getCurrentVersion());
    }

    @Override
    protected void cleanCheckout() throws InterruptedException {
        // Override without actually cleaning up, because standard builds should folllow the cleanup configuration of the Git plugin.
    }
}
