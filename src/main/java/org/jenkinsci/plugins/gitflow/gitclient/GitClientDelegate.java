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

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.gitclient.ChangelogCommand;
import org.jenkinsci.plugins.gitclient.CloneCommand;
import org.jenkinsci.plugins.gitclient.FetchCommand;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.jenkinsci.plugins.gitclient.MergeCommand;
import org.jenkinsci.plugins.gitclient.RepositoryCallback;

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

    private static final MessageFormat MSG_PATTERN_PUSHED_RELEASE_BRANCH = new MessageFormat("Gitflow - Pushed {0} to {1}");
    private static final MessageFormat MSG_PATTERN_PUSHED_RELEASE_BRANCH_DRY_RUN = new MessageFormat("Gitflow - Dry run: didn''t push {0} to {1}");

    protected final PrintStream consoleLogger;

    private final GitClient gitClient;
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

    /**
     * {@inheritDoc}
     */
    public void clearCredentials() {
        this.gitClient.clearCredentials();
    }

    /**
     * {@inheritDoc}
     */
    public void addCredentials(String url, StandardCredentials credentials) {
        this.gitClient.addCredentials(url, credentials);
    }

    /**
     * {@inheritDoc}
     */
    public void addDefaultCredentials(StandardCredentials credentials) {
        this.gitClient.addDefaultCredentials(credentials);
    }

    /**
     * {@inheritDoc}
     */
    public void setAuthor(String name, String email) throws GitException {
        this.gitClient.setAuthor(name, email);
    }

    /**
     * {@inheritDoc}
     */
    public void setAuthor(PersonIdent p) throws GitException {
        this.gitClient.setAuthor(p);
    }

    /**
     * {@inheritDoc}
     */
    public void setCommitter(String name, String email) throws GitException {
        this.gitClient.setCommitter(name, email);
    }

    /**
     * {@inheritDoc}
     */
    public void setCommitter(PersonIdent p) throws GitException {
        this.gitClient.setCommitter(p);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public Repository getRepository() throws GitException {
        return this.gitClient.getRepository();
    }

    /**
     * {@inheritDoc}
     */
    public <T> T withRepository(RepositoryCallback<T> callable) throws IOException, InterruptedException {
        return this.gitClient.withRepository(callable);
    }

    /**
     * {@inheritDoc}
     */
    public FilePath getWorkTree() {
        return this.gitClient.getWorkTree();
    }

    /**
     * {@inheritDoc}
     */
    public void init() throws GitException, InterruptedException {
        this.gitClient.init();
    }

    /**
     * {@inheritDoc}
     */
    public void add(String filePattern) throws GitException, InterruptedException {
        this.gitClient.add(filePattern);
    }

    /**
     * {@inheritDoc}
     */
    public void commit(String message) throws GitException, InterruptedException {
        this.gitClient.commit(message);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public void commit(String message, PersonIdent author, PersonIdent committer) throws GitException, InterruptedException {
        this.gitClient.commit(message, author, committer);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasGitRepo() throws GitException, InterruptedException {
        return this.gitClient.hasGitRepo();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCommitInRepo(ObjectId commit) throws GitException, InterruptedException {
        return this.gitClient.isCommitInRepo(commit);
    }

    /**
     * {@inheritDoc}
     */
    public String getRemoteUrl(String name) throws GitException, InterruptedException {
        return this.gitClient.getRemoteUrl(name);
    }

    /**
     * {@inheritDoc}
     */
    public void setRemoteUrl(String name, String url) throws GitException, InterruptedException {
        this.gitClient.setRemoteUrl(name, url);
    }

    /**
     * {@inheritDoc}
     */
    public void addRemoteUrl(String name, String url) throws GitException, InterruptedException {
        this.gitClient.addRemoteUrl(name, url);
    }

    /**
     * {@inheritDoc}
     */
    public void checkout(String ref) throws GitException, InterruptedException {
        this.gitClient.checkout(ref);
    }

    /**
     * {@inheritDoc}
     */
    public void checkout(String ref, String branch) throws GitException, InterruptedException {
        this.gitClient.checkout(ref, branch);
    }

    /**
     * {@inheritDoc}
     */
    public void checkoutBranch(String branch, String ref) throws GitException, InterruptedException {
        this.gitClient.checkoutBranch(branch, ref);
    }

    /**
     * {@inheritDoc}
     */
    public void clone(String url, String origin, boolean useShallowClone, String reference) throws GitException, InterruptedException {
        this.gitClient.clone(url, origin, useShallowClone, reference);
    }

    /**
     * {@inheritDoc}
     */
    public CloneCommand clone_() {
        return this.gitClient.clone_();
    }

    /**
     * {@inheritDoc}
     */
    public void fetch(URIish url, List<RefSpec> refspecs) throws GitException, InterruptedException {
        this.gitClient.fetch(url, refspecs);
    }

    /**
     * {@inheritDoc}
     */
    public void fetch(String remoteName, RefSpec... refspec) throws GitException, InterruptedException {
        this.gitClient.fetch(remoteName, refspec);
    }

    /**
     * {@inheritDoc}
     */
    public void fetch(String remoteName, RefSpec refspec) throws GitException, InterruptedException {
        this.gitClient.fetch(remoteName, refspec);
    }

    /**
     * {@inheritDoc}
     */
    public FetchCommand fetch_() {
        return this.gitClient.fetch_();
    }

    /**
     * {@inheritDoc}
     */
    public void push(String remoteName, String refspec) throws GitException, InterruptedException {
        String[] list = new String[] { refspec, remoteName };
        if (!this.dryRun) {
            this.gitClient.push(remoteName, refspec);
            this.consoleLogger.println(formatPattern(MSG_PATTERN_PUSHED_RELEASE_BRANCH, list));
        } else {
            String formatPattern = formatPattern(MSG_PATTERN_PUSHED_RELEASE_BRANCH_DRY_RUN, list);
            this.consoleLogger.println(formatPattern);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void push(URIish url, String refspec) throws GitException, InterruptedException {
        this.gitClient.push(url, refspec);
    }

    /**
     * {@inheritDoc}
     */
    public void merge(ObjectId rev) throws GitException, InterruptedException {
        this.gitClient.merge(rev);
    }

    /**
     * {@inheritDoc}
     */
    public MergeCommand merge() {
        return this.gitClient.merge();
    }

    /**
     * {@inheritDoc}
     */
    public void prune(RemoteConfig repository) throws GitException, InterruptedException {
        this.gitClient.prune(repository);
    }

    /**
     * {@inheritDoc}
     */
    public void clean() throws GitException, InterruptedException {
        this.gitClient.clean();
    }

    /**
     * {@inheritDoc}
     */
    public void branch(String name) throws GitException, InterruptedException {
        this.gitClient.branch(name);
    }

    /**
     * {@inheritDoc}
     */
    public void deleteBranch(String name) throws GitException, InterruptedException {
        this.gitClient.deleteBranch(name);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Branch> getBranches() throws GitException, InterruptedException {
        return this.gitClient.getBranches();
    }

    /**
     * {@inheritDoc}
     */
    public Set<Branch> getRemoteBranches() throws GitException, InterruptedException {
        return this.gitClient.getRemoteBranches();
    }

    /**
     * {@inheritDoc}
     */
    public void tag(String tagName, String comment) throws GitException, InterruptedException {
        this.gitClient.tag(tagName, comment);
    }

    /**
     * {@inheritDoc}
     */
    public boolean tagExists(String tagName) throws GitException, InterruptedException {
        return this.gitClient.tagExists(tagName);
    }

    /**
     * {@inheritDoc}
     */
    public String getTagMessage(String tagName) throws GitException, InterruptedException {
        return this.gitClient.getTagMessage(tagName);
    }

    /**
     * {@inheritDoc}
     */
    public void deleteTag(String tagName) throws GitException, InterruptedException {
        this.gitClient.deleteTag(tagName);
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getTagNames(String tagPattern) throws GitException, InterruptedException {
        return this.gitClient.getTagNames(tagPattern);
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, ObjectId> getHeadRev(String url) throws GitException, InterruptedException {
        return this.gitClient.getHeadRev(url);
    }

    /**
     * {@inheritDoc}
     */
    public ObjectId getHeadRev(String remoteRepoUrl, String branch) throws GitException, InterruptedException {
        return this.gitClient.getHeadRev(remoteRepoUrl, branch);
    }

    /**
     * {@inheritDoc}
     */
    public ObjectId revParse(String revName) throws GitException, InterruptedException {
        return this.gitClient.revParse(revName);
    }

    /**
     * {@inheritDoc}
     */
    public List<ObjectId> revListAll() throws GitException, InterruptedException {
        return this.gitClient.revListAll();
    }

    /**
     * {@inheritDoc}
     */
    public List<ObjectId> revList(String ref) throws GitException, InterruptedException {
        return this.gitClient.revList(ref);
    }

    /**
     * {@inheritDoc}
     */
    public GitClient subGit(String subdir) {
        return this.gitClient.subGit(subdir);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasGitModules() throws GitException, InterruptedException {
        return this.gitClient.hasGitModules();
    }

    /**
     * {@inheritDoc}
     */
    public List<IndexEntry> getSubmodules(String treeIsh) throws GitException, InterruptedException {
        return this.gitClient.getSubmodules(treeIsh);
    }

    /**
     * {@inheritDoc}
     */
    public void addSubmodule(String remoteURL, String subdir) throws GitException, InterruptedException {
        this.gitClient.addSubmodule(remoteURL, subdir);
    }

    /**
     * {@inheritDoc}
     */
    public void submoduleUpdate(boolean recursive) throws GitException, InterruptedException {
        this.gitClient.submoduleUpdate(recursive);
    }

    /**
     * {@inheritDoc}
     */
    public void submoduleUpdate(boolean recursive, String reference) throws GitException, InterruptedException {
        this.gitClient.submoduleUpdate(recursive, reference);
    }

    /**
     * {@inheritDoc}
     */
    public void submoduleClean(boolean recursive) throws GitException, InterruptedException {
        this.gitClient.submoduleClean(recursive);
    }

    /**
     * {@inheritDoc}
     */
    public void submoduleInit() throws GitException, InterruptedException {
        this.gitClient.submoduleInit();
    }

    /**
     * {@inheritDoc}
     */
    public void setupSubmoduleUrls(Revision rev, TaskListener listener) throws GitException, InterruptedException {
        this.gitClient.setupSubmoduleUrls(rev, listener);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public void changelog(String revFrom, String revTo, OutputStream os) throws GitException, InterruptedException {
        this.gitClient.changelog(revFrom, revTo, os);
    }

    /**
     * {@inheritDoc}
     */
    public void changelog(String revFrom, String revTo, Writer os) throws GitException, InterruptedException {
        this.gitClient.changelog(revFrom, revTo, os);
    }

    /**
     * {@inheritDoc}
     */
    public ChangelogCommand changelog() {
        return this.gitClient.changelog();
    }

    /**
     * {@inheritDoc}
     */
    public void appendNote(String note, String namespace) throws GitException, InterruptedException {
        this.gitClient.appendNote(note, namespace);
    }

    /**
     * {@inheritDoc}
     */
    public void addNote(String note, String namespace) throws GitException, InterruptedException {
        this.gitClient.addNote(note, namespace);
    }

    /**
     * {@inheritDoc}
     */
    public List<String> showRevision(ObjectId r) throws GitException, InterruptedException {
        return this.gitClient.showRevision(r);
    }

    /**
     * {@inheritDoc}
     */
    public List<String> showRevision(ObjectId from, ObjectId to) throws GitException, InterruptedException {
        return this.gitClient.showRevision(from, to);
    }

    /**
     * {@inheritDoc}
     */
    public String describe(String commitIsh) throws GitException, InterruptedException {
        return this.gitClient.describe(commitIsh);
    }

    /**
     * {@inheritDoc}
     */
    public void setCredentials(StandardUsernameCredentials cred) {
        this.gitClient.setCredentials(cred);
    }

    /**
     * {@inheritDoc}
     */
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
}
