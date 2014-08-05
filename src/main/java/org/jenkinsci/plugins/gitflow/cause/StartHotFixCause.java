package org.jenkinsci.plugins.gitflow.cause;

import net.sf.json.JSONObject;

/**
 * The {@link hudson.model.Cause} object for the <i>Start Hotfix</i> action to be executed.
 *
 * @author Hannes Osius, Silpion IT-Solutions GmbH - osius@silpion.de
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

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param name the name of the Hotfix.
     * @param nextHotfixDevelopmentVersion the suggestion for the next hotfix development version.
     * @param dryRun is the build dryRun or not
     */
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


