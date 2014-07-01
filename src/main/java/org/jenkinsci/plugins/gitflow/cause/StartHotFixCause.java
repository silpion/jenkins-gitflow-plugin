package org.jenkinsci.plugins.gitflow.cause;

import net.sf.json.JSONObject;

/**
 * @author Hannes Osius, Silpion IT-Solutions GmbH
 */

public class StartHotFixCause extends AbstractGitflowCause {

    public static final String PARAM_HOTFIX_NAME = "hotfixName";
    public static final String PARAM_NEXT_HOTFIX_DEVELOPMENT_VERSION = "nextHotfixDevelopmentVersion";

    private final String name;
    private final String nextHotfixDevelopmentVersion;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param structuredActionConent the structured content for the selected action to be instanciated.
     * @param dryRun is the build dryRun or not
     */
    public StartHotFixCause(JSONObject structuredActionConent, boolean dryRun) {
        this(structuredActionConent.getString(PARAM_HOTFIX_NAME), structuredActionConent.getString(PARAM_NEXT_HOTFIX_DEVELOPMENT_VERSION), dryRun);
    }

    public StartHotFixCause(String name, String nextHotfixDevelopmentVersion, boolean dryRun) {
        super(dryRun);

        this.name = name;
        this.nextHotfixDevelopmentVersion = nextHotfixDevelopmentVersion;
    }

    public String getName() {
        return name;
    }

    public String getNextHotfixDevelopmentVersion() {
        return nextHotfixDevelopmentVersion;
    }

    @Override
    public String getVersionForBadge() {
        //TODO
        return null;
    }
}


