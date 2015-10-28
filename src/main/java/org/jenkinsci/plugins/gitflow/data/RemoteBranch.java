package org.jenkinsci.plugins.gitflow.data;

import java.io.Serializable;

import org.eclipse.jgit.lib.ObjectId;

import hudson.model.Result;

/**
 * The object holding the information about a Git remote branch for a Jenkins job/project.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class RemoteBranch implements Serializable, Cloneable, Comparable<RemoteBranch> {

    private static final long serialVersionUID = -405556788597424146L;

    private final String branchName;

    private Result lastBuildResult;
    private String lastBuildVersion;

    private String baseReleaseVersion;

    private String lastReleaseVersion;
    private ObjectId lastReleaseVersionCommit;

    /**
     * Constructor for a new {@link RemoteBranch} object.
     *
     * @param branchName the simple name of the branch (without remote alias).
     */
    public RemoteBranch(final String branchName) {
        this.branchName = branchName;
    }

    /** {@inheritDoc} */
    @Override
    public RemoteBranch clone() throws CloneNotSupportedException {
        return (RemoteBranch) super.clone();
    }

    /** {@inheritDoc} */
    public int compareTo(final RemoteBranch remoteBranch) {
        return String.CASE_INSENSITIVE_ORDER.compare(this.getBranchName(), remoteBranch.getBranchName());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.branchName;
    }

    public String getBranchName() {
        return this.branchName;
    }

    public void setLastBuildResult(final Result lastBuildResult) {
        this.lastBuildResult = lastBuildResult;
    }

    public Result getLastBuildResult() {
        return this.lastBuildResult != null ? this.lastBuildResult : Result.FAILURE;
    }

    public String getBaseReleaseVersion() {
        return this.baseReleaseVersion;
    }

    public void setBaseReleaseVersion(final String baseReleaseVersion) {
        this.baseReleaseVersion = baseReleaseVersion;
    }

    public void setLastBuildVersion(final String lastBuildVersion) {
        this.lastBuildVersion = lastBuildVersion;
    }

    public String getLastBuildVersion() {
        return this.lastBuildVersion;
    }

    public void setLastReleaseVersion(final String lastReleaseVersion) {
        this.lastReleaseVersion = lastReleaseVersion;
    }

    public String getLastReleaseVersion() {
        return this.lastReleaseVersion;
    }

    public void setLastReleaseVersionCommit(final ObjectId lastReleaseVersionCommit) {
        this.lastReleaseVersionCommit = lastReleaseVersionCommit;
    }

    public ObjectId getLastReleaseVersionCommit() {
        return this.lastReleaseVersionCommit;
    }
}
