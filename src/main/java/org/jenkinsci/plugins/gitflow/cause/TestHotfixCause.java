package org.jenkinsci.plugins.gitflow.cause;

import net.sf.json.JSONObject;

/**
 * The {@link hudson.model.Cause} object for the <i>Test Hotfix</i> action to be executed.
 *
 * @author Hannes Osius, Silpion IT-Solutions GmbH - osius@silpion.de
 */
public class TestHotfixCause extends AbstractGitflowCause {

    public static final String PARAM_HOTFIX = "testHotfix";
    public static final String PARAM_HOTFIX_BRANCH = "hotfixBranch";
    public static final String PARAM_HOTFIX_RELEASE_VERSION = "fixesHotfixReleaseVersion";
    public static final String PARAM_NEXT_HOTFIX_RELEASE_VERSION = "nextHotfixReleaseVersion";

    private String hotfixBranch;
    private String hotfixReleaseVersion;
    private String nextHotfixReleaseVersion;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param structuredActionConent the structured content for the selected action to be instanciated.
     * @param dryRun is the build dryRun or not
     */
    public TestHotfixCause(final JSONObject structuredActionConent, final boolean dryRun) {
        this(structuredActionConent.getJSONObject(PARAM_HOTFIX).getString(PARAM_HOTFIX_BRANCH),
             structuredActionConent.getJSONObject(PARAM_HOTFIX).getString(PARAM_HOTFIX_RELEASE_VERSION),
             structuredActionConent.getJSONObject(PARAM_HOTFIX).getString(PARAM_NEXT_HOTFIX_RELEASE_VERSION),
             dryRun);
    }

    public TestHotfixCause(String hotfixBranch, String hotfixReleaseVersion, String nextHotfixReleaseVersion, boolean dryRun) {
        this.setDryRun(dryRun);
        this.hotfixBranch = hotfixBranch;
        this.hotfixReleaseVersion = hotfixReleaseVersion;
        this.nextHotfixReleaseVersion = nextHotfixReleaseVersion;
    }

    @Override
    public String getVersionForBadge() {
        return getHotfixReleaseVersion();
    }

    public String getHotfixBranch() {
        return hotfixBranch;
    }

    public String getHotfixReleaseVersion() {
        return hotfixReleaseVersion;
    }

    public String getNextHotfixReleaseVersion() {
        return nextHotfixReleaseVersion;
    }
}
