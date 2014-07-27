/*
 * Copyright (c) 2014 Silpion IT Solutions GmbH
 */
package org.jenkinsci.plugins.gitflow.gitclient;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.gitclient.ChangelogCommand;
import org.jenkinsci.plugins.gitclient.CliGitAPIImpl;
import org.jenkinsci.plugins.gitclient.CheckoutCommand;
import org.jenkinsci.plugins.gitclient.CloneCommand;
import org.jenkinsci.plugins.gitclient.FetchCommand;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.jenkinsci.plugins.gitclient.JGitAPIImpl;
import org.jenkinsci.plugins.gitclient.InitCommand;
import org.jenkinsci.plugins.gitclient.MergeCommand;
import org.jenkinsci.plugins.gitclient.MergeCommand.Strategy;
import org.jenkinsci.plugins.gitclient.PushCommand;
import org.jenkinsci.plugins.gitclient.RepositoryCallback;
import org.jenkinsci.plugins.gitflow.gitclient.merge.CliGitMergeCommand;
import org.jenkinsci.plugins.gitflow.gitclient.merge.GenericMergeCommand;
import org.jenkinsci.plugins.gitflow.gitclient.merge.GenericMergeCommand.StrategyOption;
import org.jenkinsci.plugins.gitflow.gitclient.merge.JGitMergeCommand;
import org.jenkinsci.plugins.gitclient.SubmoduleUpdateCommand;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;

import hudson.FilePath;
import hudson.ProxyConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.plugins.git.Branch;
import hudson.plugins.git.GitException;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.IndexEntry;
import hudson.plugins.git.Revision;

/**
 * Base on GitClient Class, modified version for Jenkins Gitflow Plugin
 *
 * @author Maria Lüdemann Silpion IT-Solutions GmbH - luedemann@silpion.de
 */
public class GitClientDelegate implements GitClient {

    private static final MessageFormat MSG_PATTERN_PUSHED_TO_REMOTE = new MessageFormat("Gitflow - {0}: Pushed to {1} using refspec {2}");
    private static final MessageFormat MSG_PATTERN_PUSH_OMITTED_DUE_TO_DRY_RUN = new MessageFormat("Gitflow - {0} (dry run): Omitted push to {1} using refspec {2}");

    protected final PrintStream consoleLogger;

    private final GitClient gitClient;

    private String gitflowActionName = "unknown action";
    private final boolean dryRun;

    /**
     * Creates a new GitClientDelegate instance.
     *
     * @param build the build that is in progress.
     * @param listener can be used to send any message.
     * @param dryRun is the build dryRun or not
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public GitClientDelegate(final AbstractBuild<?, ?> build, final BuildListener listener, final boolean dryRun) throws IOException, InterruptedException {
        final GitSCM gitSCM = (GitSCM) build.getProject().getScm();
        this.gitClient = gitSCM.createClient(listener, build.getEnvironment(listener), build);
        this.dryRun = dryRun;
        this.consoleLogger = listener.getLogger();
    }

    /**
     * Creates a new GitClientDelegate instance.
     *
     * @param build the build that is in progress.
     * @param listener can be used to send any message.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public GitClientDelegate(final AbstractBuild<?, ?> build, final BuildListener listener) throws IOException, InterruptedException {
        this(build, listener, false);
    }

    /** {@inheritDoc} */
    public void clearCredentials() {
        this.gitClient.clearCredentials();
    }

    /** {@inheritDoc} */
    public void addCredentials(String url, StandardCredentials credentials) {
        this.gitClient.addCredentials(url, credentials);
    }

    /** {@inheritDoc} */
    public void addDefaultCredentials(StandardCredentials credentials) {
        this.gitClient.addDefaultCredentials(credentials);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void setAuthor(String name, String email) throws GitException {
        this.gitClient.setAuthor(name, email);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void setAuthor(PersonIdent p) throws GitException {
        this.gitClient.setAuthor(p);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void setCommitter(String name, String email) throws GitException {
        this.gitClient.setCommitter(name, email);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void setCommitter(PersonIdent p) throws GitException {
        this.gitClient.setCommitter(p);
    }

    /** {@inheritDoc} */
    @Deprecated
    @SuppressWarnings("ThrowsRuntimeException")
    public Repository getRepository() throws GitException {
        return this.gitClient.getRepository();
    }

    /** {@inheritDoc} */
    public <T> T withRepository(RepositoryCallback<T> callable) throws IOException, InterruptedException {
        return this.gitClient.withRepository(callable);
    }

    /** {@inheritDoc} */
    public FilePath getWorkTree() {
        return this.gitClient.getWorkTree();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void init() throws GitException, InterruptedException {
        this.gitClient.init();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void add(String filePattern) throws GitException, InterruptedException {
        this.gitClient.add(filePattern);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void commit(String message) throws GitException, InterruptedException {
        this.gitClient.commit(message);
    }

    /** {@inheritDoc} */
    @Deprecated
    @SuppressWarnings("ThrowsRuntimeException")
    public void commit(String message, PersonIdent author, PersonIdent committer) throws GitException, InterruptedException {
        this.gitClient.commit(message, author, committer);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public boolean hasGitRepo() throws GitException, InterruptedException {
        return this.gitClient.hasGitRepo();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public boolean isCommitInRepo(ObjectId commit) throws GitException, InterruptedException {
        return this.gitClient.isCommitInRepo(commit);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public String getRemoteUrl(String name) throws GitException, InterruptedException {
        return this.gitClient.getRemoteUrl(name);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void setRemoteUrl(String name, String url) throws GitException, InterruptedException {
        this.gitClient.setRemoteUrl(name, url);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void addRemoteUrl(String name, String url) throws GitException, InterruptedException {
        this.gitClient.addRemoteUrl(name, url);
    }

    /** {@inheritDoc} */
    @Deprecated
    @SuppressWarnings("ThrowsRuntimeException")
    public void checkout(String ref) throws GitException, InterruptedException {
        this.gitClient.checkout(ref);
    }

    /** {@inheritDoc} */
    @Deprecated
    @SuppressWarnings("ThrowsRuntimeException")
    public void checkout(String ref, String branch) throws GitException, InterruptedException {
        this.gitClient.checkout(ref, branch);
    }

    /** {@inheritDoc} */
    public CheckoutCommand checkout() {
        return this.gitClient.checkout();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void checkoutBranch(String branch, String ref) throws GitException, InterruptedException {
        this.gitClient.checkoutBranch(branch, ref);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void clone(String url, String origin, boolean useShallowClone, String reference) throws GitException, InterruptedException {
        this.gitClient.clone(url, origin, useShallowClone, reference);
    }

    /** {@inheritDoc} */
    public CloneCommand clone_() {
        return this.gitClient.clone_();
    }

    /** {@inheritDoc} */
    @Deprecated
    @SuppressWarnings("ThrowsRuntimeException")
    public void fetch(URIish url, List<RefSpec> refspecs) throws GitException, InterruptedException {
        this.gitClient.fetch(url, refspecs);
    }

    /** {@inheritDoc} */
    @Deprecated
    @SuppressWarnings("ThrowsRuntimeException")
    public void fetch(String remoteName, RefSpec... refspec) throws GitException, InterruptedException {
        this.gitClient.fetch(remoteName, refspec);
    }

    /** {@inheritDoc} */
    @Deprecated
    @SuppressWarnings("ThrowsRuntimeException")
    public void fetch(String remoteName, RefSpec refspec) throws GitException, InterruptedException {
        this.gitClient.fetch(remoteName, refspec);
    }

    /** {@inheritDoc} */
    public FetchCommand fetch_() {
        return this.gitClient.fetch_();
    }

    /** {@inheritDoc} */
    @Deprecated
    @SuppressWarnings("ThrowsRuntimeException")
    public void push(String remoteName, String refspec) throws GitException, InterruptedException {
        // This method is deprecated, but let's still support 'Dry Run' - just for the case ...
        final String[] messageArguments = new String[] { this.gitflowActionName, remoteName, refspec };
        if (GitClientDelegate.this.dryRun) {
            GitClientDelegate.this.consoleLogger.println(formatPattern(MSG_PATTERN_PUSH_OMITTED_DUE_TO_DRY_RUN, messageArguments));
        } else {
            this.gitClient.push(remoteName, refspec);
            GitClientDelegate.this.consoleLogger.println(formatPattern(MSG_PATTERN_PUSHED_TO_REMOTE, messageArguments));
        }
    }

    /** {@inheritDoc} */
    @Deprecated
    @SuppressWarnings("ThrowsRuntimeException")
    public void push(URIish url, String refspec) throws GitException, InterruptedException {
        // This method is deprecated, but let's still support 'Dry Run' - just for the case ...
        final String[] messageArguments = new String[] { this.gitflowActionName, url.getHumanishName(), refspec };
        if (GitClientDelegate.this.dryRun) {
            GitClientDelegate.this.consoleLogger.println(formatPattern(MSG_PATTERN_PUSH_OMITTED_DUE_TO_DRY_RUN, messageArguments));
        } else {
            this.gitClient.push(url, refspec);
            GitClientDelegate.this.consoleLogger.println(formatPattern(MSG_PATTERN_PUSHED_TO_REMOTE, messageArguments));
        }
    }

    /** {@inheritDoc} */
    public PushCommand push() {
        final PushCommand pushCommand = this.gitClient.push();
        return new PushCommand() {

            private URIish remote;
            private String refspec;

            /** {@inheritDoc} */
            public PushCommand to(final URIish remote) {
                this.remote = remote;
                pushCommand.to(remote);
                return this;
            }

            /** {@inheritDoc} */
            public PushCommand ref(final String refspec) {
                this.refspec = refspec;
                pushCommand.ref(refspec);
                return this;
            }

            /** {@inheritDoc} */
            public PushCommand force() {
                pushCommand.force();
                return this;
            }

            /** {@inheritDoc} */
            public PushCommand timeout(final Integer timeout) {
                pushCommand.timeout(timeout);
                return this;
            }

            /** {@inheritDoc} */
            @SuppressWarnings("ThrowsRuntimeException")
            public void execute() throws GitException, InterruptedException {
                final String[] messageArguments = new String[] { GitClientDelegate.this.gitflowActionName, this.remote.getHumanishName(), this.refspec };
                if (GitClientDelegate.this.dryRun) {
                    GitClientDelegate.this.consoleLogger.println(formatPattern(MSG_PATTERN_PUSH_OMITTED_DUE_TO_DRY_RUN, messageArguments));
                } else {
                    pushCommand.execute();
                    GitClientDelegate.this.consoleLogger.println(formatPattern(MSG_PATTERN_PUSHED_TO_REMOTE, messageArguments));
                }
            }
        };
    }

    /** {@inheritDoc} */
    @Deprecated
    @SuppressWarnings("ThrowsRuntimeException")
    public void merge(ObjectId rev) throws GitException, InterruptedException {
        this.gitClient.merge(rev);
    }

    /** {@inheritDoc} */
    public MergeCommand merge() {
        return this.gitClient.merge();
    }

    /**
     * Merge the given revision to the current branch using the provided merge settings.
     *
     * @param rev the revision to be merged.
     * @param fastForwardMode the fast forward mode for the merge.
     * @param strategy the merge strategy.
     * @param strategyOption the option for the merge strategy.
     * @throws GitException if an error occurs while merging.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public void merge(final ObjectId rev, final FastForwardMode fastForwardMode, final Strategy strategy, final StrategyOption strategyOption,
                      final boolean autoCommit) throws InterruptedException {

        // Create  merge command object regarding to the underlying (configured) Git client implementation.
        final GenericMergeCommand<? extends GitClient> mergeCommand;
        if (this.gitClient instanceof CliGitAPIImpl) {
            mergeCommand = new CliGitMergeCommand<CliGitAPIImpl>((CliGitAPIImpl) this.gitClient, this.consoleLogger);
        } else if (this.gitClient instanceof JGitAPIImpl) {
            mergeCommand = new JGitMergeCommand<JGitAPIImpl>((JGitAPIImpl) this.gitClient, this.consoleLogger);
        } else {
            mergeCommand = new GenericMergeCommand<GitClient>(this.gitClient, this.consoleLogger);
        }

        // Set the provided merge options.
        mergeCommand.setFastForwardMode(fastForwardMode);
        mergeCommand.setStrategy(strategy);
        mergeCommand.setStrategyOption(strategyOption);
        mergeCommand.setAutoCommit(autoCommit);

        // Merge the given revision.
        mergeCommand.setRevisionToMerge(rev).execute();
    }

    /** {@inheritDoc} */
    public InitCommand init_() {
        return this.gitClient.init_();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void prune(RemoteConfig repository) throws GitException, InterruptedException {
        this.gitClient.prune(repository);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void clean() throws GitException, InterruptedException {
        this.gitClient.clean();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void branch(String name) throws GitException, InterruptedException {
        this.gitClient.branch(name);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void deleteBranch(String name) throws GitException, InterruptedException {
        this.gitClient.deleteBranch(name);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public Set<Branch> getBranches() throws GitException, InterruptedException {
        return this.gitClient.getBranches();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public Set<Branch> getRemoteBranches() throws GitException, InterruptedException {
        return this.gitClient.getRemoteBranches();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void tag(String tagName, String comment) throws GitException, InterruptedException {
        this.gitClient.tag(tagName, comment);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public boolean tagExists(String tagName) throws GitException, InterruptedException {
        return this.gitClient.tagExists(tagName);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public String getTagMessage(String tagName) throws GitException, InterruptedException {
        return this.gitClient.getTagMessage(tagName);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void deleteTag(String tagName) throws GitException, InterruptedException {
        this.gitClient.deleteTag(tagName);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public Set<String> getTagNames(String tagPattern) throws GitException, InterruptedException {
        return this.gitClient.getTagNames(tagPattern);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public Map<String, ObjectId> getHeadRev(String url) throws GitException, InterruptedException {
        return this.gitClient.getHeadRev(url);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public ObjectId getHeadRev(String remoteRepoUrl, String branch) throws GitException, InterruptedException {
        return this.gitClient.getHeadRev(remoteRepoUrl, branch);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public ObjectId revParse(String revName) throws GitException, InterruptedException {
        return this.gitClient.revParse(revName);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public List<ObjectId> revListAll() throws GitException, InterruptedException {
        return this.gitClient.revListAll();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public List<ObjectId> revList(String ref) throws GitException, InterruptedException {
        return this.gitClient.revList(ref);
    }

    /** {@inheritDoc} */
    public GitClient subGit(String subdir) {
        return this.gitClient.subGit(subdir);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public boolean hasGitModules() throws GitException, InterruptedException {
        return this.gitClient.hasGitModules();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public List<IndexEntry> getSubmodules(String treeIsh) throws GitException, InterruptedException {
        return this.gitClient.getSubmodules(treeIsh);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void addSubmodule(String remoteURL, String subdir) throws GitException, InterruptedException {
        this.gitClient.addSubmodule(remoteURL, subdir);
    }

    /** {@inheritDoc} */
    @Deprecated
    @SuppressWarnings("ThrowsRuntimeException")
    public void submoduleUpdate(boolean recursive) throws GitException, InterruptedException {
        this.gitClient.submoduleUpdate(recursive);
    }

    /** {@inheritDoc} */
    @Deprecated
    @SuppressWarnings("ThrowsRuntimeException")
    public void submoduleUpdate(boolean recursive, String reference) throws GitException, InterruptedException {
        this.gitClient.submoduleUpdate(recursive, reference);
    }

    /** {@inheritDoc} */
    @Deprecated
    @SuppressWarnings("ThrowsRuntimeException")
    public void submoduleUpdate(final boolean recursive, final boolean remoteTracking) throws GitException, InterruptedException {
        this.gitClient.submoduleUpdate(recursive, remoteTracking);
    }

    /** {@inheritDoc} */
    @Deprecated
    @SuppressWarnings("ThrowsRuntimeException")
    public void submoduleUpdate(final boolean recursive, final boolean remoteTracking, final String reference) throws GitException, InterruptedException {
        this.gitClient.submoduleUpdate(recursive, remoteTracking, reference);
    }

    /** {@inheritDoc} */
    public SubmoduleUpdateCommand submoduleUpdate() {
        return this.gitClient.submoduleUpdate();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void submoduleClean(boolean recursive) throws GitException, InterruptedException {
        this.gitClient.submoduleClean(recursive);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void submoduleInit() throws GitException, InterruptedException {
        this.gitClient.submoduleInit();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void setupSubmoduleUrls(Revision rev, TaskListener listener) throws GitException, InterruptedException {
        this.gitClient.setupSubmoduleUrls(rev, listener);
    }

    /** {@inheritDoc} */
    @Deprecated
    @SuppressWarnings("ThrowsRuntimeException")
    public void changelog(String revFrom, String revTo, OutputStream os) throws GitException, InterruptedException {
        this.gitClient.changelog(revFrom, revTo, os);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void changelog(String revFrom, String revTo, Writer os) throws GitException, InterruptedException {
        this.gitClient.changelog(revFrom, revTo, os);
    }

    /** {@inheritDoc} */
    public ChangelogCommand changelog() {
        return this.gitClient.changelog();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void appendNote(String note, String namespace) throws GitException, InterruptedException {
        this.gitClient.appendNote(note, namespace);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public void addNote(String note, String namespace) throws GitException, InterruptedException {
        this.gitClient.addNote(note, namespace);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public List<String> showRevision(ObjectId r) throws GitException, InterruptedException {
        return this.gitClient.showRevision(r);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public List<String> showRevision(ObjectId from, ObjectId to) throws GitException, InterruptedException {
        return this.gitClient.showRevision(from, to);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowsRuntimeException")
    public String describe(String commitIsh) throws GitException, InterruptedException {
        return this.gitClient.describe(commitIsh);
    }

    /** {@inheritDoc} */
    public void setCredentials(StandardUsernameCredentials cred) {
        this.gitClient.setCredentials(cred);
    }

    /** {@inheritDoc} */
    public void setProxy(ProxyConfiguration proxy) {
        this.gitClient.setProxy(proxy);
    }

    /**
     * Formats a message pattern, substituting its placeholders with the provided arguments (see {@link java.text.MessageFormat}).
     * It may not only format messages, but any string pattern (like e.g. a command pattern).
     * <p/>
     * This is a convenience method for the {@code format} methods of the {@link java.text.MessageFormat} class.
     *
     * @param messageFormat the string pattern to be formatted.
     * @param messageArguments the format arguments.
     * @return the formatted string.
     */
    private static String formatPattern(final MessageFormat messageFormat, final String... messageArguments) {
        return messageFormat.format(messageArguments);
    }

    public void setGitflowActionName(final String gitflowActionName) {
        this.gitflowActionName = gitflowActionName;
    }
}
