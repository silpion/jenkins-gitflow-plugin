package org.jenkinsci.plugins.gitflow.data;

import java.io.Serializable;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import hudson.model.Result;

/**
 * The object holding the information about a Git remote branch for a Jenkins job/project.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class RemoteBranch implements Serializable, Cloneable {

    private static final long serialVersionUID = 432731634282740845L;

    @XStreamAsAttribute
    private final String remoteAlias;

    @XStreamAsAttribute
    private final String branchName;

    private String lastBuildResult;
    private String lastBuildVersion;

    /**
     * Constructor for a new {@link RemoteBranch} object.
     *
     * @param remoteAlias the alias for the remote repository.
     * @param branchName the simple name of the branch (without remote alias).
     */
    public RemoteBranch(final String remoteAlias, final String branchName) {
        this.remoteAlias = remoteAlias;
        this.branchName = branchName;
    }

    public String getRemoteAlias() {
        return this.remoteAlias;
    }

    public String getBranchName() {
        return this.branchName;
    }

    public void setLastBuildResult(final Result lastBuildResult) {
        this.lastBuildResult = lastBuildResult.toString();
    }

    public Result getLastBuildResult() {
        return Result.fromString(this.lastBuildResult);
    }

    public void setLastBuildVersion(final String lastBuildVersion) {
        this.lastBuildVersion = lastBuildVersion;
    }

    public String getLastBuildVersion() {
        return this.lastBuildVersion;
    }
}
