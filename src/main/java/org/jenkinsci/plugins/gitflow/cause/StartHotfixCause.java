package org.jenkinsci.plugins.gitflow.cause;

import net.sf.json.JSONObject;

/**
 * The {@link hudson.model.Cause} object for the <i>Start Hotfix</i> action to be executed.
 *
 * @author Hannes Osius, Silpion IT-Solutions GmbH - osius@silpion.de
 */
public class StartHotfixCause extends AbstractGitflowCause {

    public static final String PARAM_HOTFIX_RELEASE_VERSION = "hotfixReleaseVersion";
    public static final String PARAM_NEXT_HOTFIX_DEVELOPMENT_VERSION = "nextHotfixDevelopmentVersion";

    private final String hotfixReleaseVersion;
    private final String nextHotfixDevelopmentVersion;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param structuredActionConent the structured content for the selected action to be instanciated.
     * @param dryRun is the build dryRun or not
     */
    public StartHotfixCause(JSONObject structuredActionConent, boolean dryRun) {
        this(structuredActionConent.getString(PARAM_HOTFIX_RELEASE_VERSION), structuredActionConent.getString(PARAM_NEXT_HOTFIX_DEVELOPMENT_VERSION), dryRun);
    }

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param hotfixReleaseVersion the release version of the Hotfix.
     * @param nextHotfixDevelopmentVersion the suggestion for the next hotfix development version.
     * @param dryRun is the build dryRun or not
     */
    public StartHotfixCause(String hotfixReleaseVersion, String nextHotfixDevelopmentVersion, boolean dryRun) {
        this.setDryRun(dryRun);

        this.hotfixReleaseVersion = hotfixReleaseVersion;
        this.nextHotfixDevelopmentVersion = nextHotfixDevelopmentVersion;
    }

    public String getHotfixReleaseVersion() {
        return this.hotfixReleaseVersion;
    }

    public String getNextHotfixDevelopmentVersion() {
        return nextHotfixDevelopmentVersion;
    }

    @Override
    public String getVersionForBadge() {
        return this.hotfixReleaseVersion;
    }
}
