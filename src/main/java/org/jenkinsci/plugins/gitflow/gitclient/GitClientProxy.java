package org.jenkinsci.plugins.gitflow.gitclient;

import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.gitclient.CliGitAPIImpl;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.jenkinsci.plugins.gitclient.JGitAPIImpl;
import org.jenkinsci.plugins.gitclient.MergeCommand.Strategy;
import org.jenkinsci.plugins.gitflow.gitclient.merge.CliGitMergeCommand;
import org.jenkinsci.plugins.gitflow.gitclient.merge.GenericMergeCommand;
import org.jenkinsci.plugins.gitflow.gitclient.merge.GenericMergeCommand.StrategyOption;
import org.jenkinsci.plugins.gitflow.gitclient.merge.JGitMergeCommand;

import hudson.model.BuildListener;
import hudson.plugins.git.Branch;
import hudson.plugins.git.GitException;

/**
 * Abstract proxy implementation for the Jenkins {@link GitClient}. Subclasses should use <i>Reflections</i>
 * to implement version-dependant functions without causing compiler and/or runtime errors.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public abstract class GitClientProxy {

    private static final String MSG_PATTERN_PUSHED_TO_REMOTE = "Gitflow - %s: Pushed to %s using refspec %s%n";
    private static final String MSG_PATTERN_PUSH_OMITTED_DUE_TO_DRY_RUN = "Gitflow - %s (dry run): Omitted push to %s using refspec %s%n";

    protected final PrintStream consoleLogger;

    private String gitflowActionName = "unknown action";
    private final boolean dryRun;

    /**
     * Creates a new instance.
     *
     * @param listener can be used to send any message.
     * @param dryRun is the build dryRun or not
     */
    public GitClientProxy(final BuildListener listener, final boolean dryRun) {
        this.dryRun = dryRun;
        this.consoleLogger = listener.getLogger();
    }

    /**
     * Returns the proxied {@link GitClient}.
     *
     * @return the proxied {@link GitClient}.
     */
    protected abstract GitClient getGitClient();

    /**
     * Stage files for commit.
     *
     * @param filePattern the name/path pattern of the files to be staged.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public void add(final String filePattern) throws InterruptedException {
        this.getGitClient().add(filePattern);
    }

    /**
     * Commit staged files to the local repository.
     *
     * @param message the commit message.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public void commit(final String message) throws InterruptedException {
        this.getGitClient().commit(message);
    }

    /**
     * Regardless of the current state of the workspace (whether there is some dirty files, etc)
     * and the state of the repository (whether the branch of the specified name exists or not),
     * when this method exits the following conditions hold:
     * <ul>
     * <li>The branch of the specified name <em>branch</em> exists and points to the specified <em>ref</em>
     * <li><tt>HEAD</tt> points to <em>branch</em>. IOW, the workspace is on the specified branch.
     * <li>Both index and workspace are the same tree with <em>ref</em>.
     * (no dirty files and no staged changes, although this method will not touch untracked files
     * in the workspace.)
     * </ul>
     * <p/>
     * The method's Javadoc has been copied from {@link GitClient#checkoutBranch(String, String)}.
     *
     * @param branch the name of the branch.
     * @param ref the start point for the branch - either a commit ref or a branch.
     * @throws InterruptedException if the build is interrupted during execution.
     * @see GitClient#checkoutBranch(String, String)
     */
    public void checkoutBranch(final String branch, final String ref) throws InterruptedException {
        this.getGitClient().checkoutBranch(branch, ref);
    }

    /**
     * Push local objects to a remote repository.
     *
     * @param remoteAlias the alias for the remote repository.
     * @param refspec specifies what local source ref to push to what remote target ref.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public void push(final String remoteAlias, final String refspec) throws InterruptedException {
        final Object[] messageArguments = new String[] { this.gitflowActionName, remoteAlias, refspec };
        if (this.dryRun) {
            this.consoleLogger.printf(MSG_PATTERN_PUSH_OMITTED_DUE_TO_DRY_RUN, messageArguments);
        } else {
            this.pushInternal(remoteAlias, refspec);
            this.consoleLogger.printf(MSG_PATTERN_PUSHED_TO_REMOTE, messageArguments);
        }
    }

    private void pushInternal(final String remoteAlias, final String refspec) throws InterruptedException {

        // Create remote URL.
        final URIish remoteUrl;
        try {
            remoteUrl = new URIish(remoteAlias);
        } catch (final URISyntaxException urise) {
            throw new GitException("Cannot create remote URL", urise);
        }

        this.getGitClient().push().to(remoteUrl).ref(refspec).execute();
    }

    /**
     * Merge the given revision to the current branch using the provided merge settings.
     *
     * @param rev the revision to be merged.
     * @param fastForwardMode the fast forward mode for the merge.
     * @param strategy the merge strategy.
     * @param strategyOption the option for the merge strategy.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public void merge(final ObjectId rev, final FastForwardMode fastForwardMode, final Strategy strategy, final StrategyOption strategyOption, final boolean autoCommit) throws InterruptedException {

        // Create  merge command object regarding to the underlying (configured) Git client implementation.
        final GenericMergeCommand<? extends GitClient> mergeCommand;
        if (this.getGitClient() instanceof CliGitAPIImpl) {
            mergeCommand = new CliGitMergeCommand<CliGitAPIImpl>((CliGitAPIImpl) this.getGitClient(), this.consoleLogger);
        } else if (this.getGitClient() instanceof JGitAPIImpl) {
            mergeCommand = new JGitMergeCommand<JGitAPIImpl>((JGitAPIImpl) this.getGitClient(), this.consoleLogger);
        } else {
            mergeCommand = new GenericMergeCommand<GitClient>(this.getGitClient(), this.consoleLogger);
        }

        // Set the provided merge options.
        mergeCommand.setFastForwardMode(fastForwardMode);
        mergeCommand.setStrategy(strategy);
        mergeCommand.setStrategyOption(strategyOption);
        mergeCommand.setAutoCommit(autoCommit);

        // Merge the given revision.
        mergeCommand.setRevisionToMerge(rev).execute();
    }

    /**
     * Fully revert working copy to a clean state, i.e. run both
     * <a href="https://www.kernel.org/pub/software/scm/git/docs/git-reset.html">git-reset(1) --hard</a> then
     * <a href="https://www.kernel.org/pub/software/scm/git/docs/git-clean.html">git-clean(1)</a> for working copy to
     * match a fresh clone.
     * <p/>
     * The method's Javadoc has been copied from {@link GitClient#clean()}.
     *
     * @throws InterruptedException if the build is interrupted during execution.
     * @see GitClient#clean()
     */
    public void clean() throws InterruptedException {
        this.getGitClient().clean();
    }

    /**
     * Delete a local branch.
     *
     * @param name the name of the branch.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public void deleteBranch(final String name) throws InterruptedException {
        this.getGitClient().deleteBranch(name);
    }

    /**
     * Returns the existing local and remote branches.
     *
     * @return the existing local and remote branches.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public Set<Branch> getBranches() throws InterruptedException {
        return this.getGitClient().getBranches();
    }

    /**
     * Create (or update) a tag. If tag already exist it gets updated (equivalent to <tt>git tag --force</tt>)
     * <p/>
     * The method's Javadoc has been copied from {@link GitClient#tag(String, String)}.
     *
     * @param tagName the name of the tag.
     * @param comment the commit message for the tag.
     * @throws InterruptedException if the build is interrupted during execution.
     * @see GitClient#tag(String, String)
     */
    public void tag(final String tagName, final String comment) throws InterruptedException {
        this.getGitClient().tag(tagName, comment);
    }

    /**
     * Returns the ref for the head commit of the specified remote branch.
     * <p/>
     * This method fixes/prevents a bug in the {@link GitClient#getHeadRev(String, String)}
     * method: When the {@code branch} is provided with a simple branch name that contains
     * slashes, the original method might mix up branches. E.g.: When looking for branch
     * {@code release/1.0}, the head rev of {@code hotfix/1.0} may be returned.
     *
     * @param branch the name of the branch.
     * @return the ref for the head commit of the specified branch or {@code null}.
     * @throws InterruptedException
     */
    public ObjectId getHeadRev(final String branch) throws InterruptedException {
        ObjectId headRev = null;

        final String remoteUrl = this.getGitClient().getRemoteUrl("origin");
        if (branch.startsWith("remotes/") || branch.startsWith("refs/heads/")) {
            headRev = this.getGitClient().getHeadRev(remoteUrl, branch);
        } else {
            for (final Map.Entry<String, ObjectId> branchHeadRev : this.getGitClient().getHeadRev(remoteUrl).entrySet()) {
                final String branchName = StringUtils.removeStart(branchHeadRev.getKey(), "refs/heads/");
                if (branchName.equals(branch)) {
                    headRev = branchHeadRev.getValue();
                    break;
                }
            }
        }

        return headRev;
    }

    /**
     * Set the name of the Gitflow action.
     *
     * @param gitflowActionName the name of the Gitflow action.
     */
    public void setGitflowActionName(final String gitflowActionName) {
        this.gitflowActionName = gitflowActionName;
    }
}
