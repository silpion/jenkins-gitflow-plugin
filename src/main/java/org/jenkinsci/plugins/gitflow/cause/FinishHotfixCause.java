package org.jenkinsci.plugins.gitflow.cause;

import net.sf.json.JSONObject;

/**
 * The {@link hudson.model.Cause} object for the <i>Finish Hotfix</i> action to be executed.
 *
 * @author Hannes Osius, Silpion IT-Solutions GmbH - osius@silpion.de
 */
public class FinishHotfixCause extends AbstractGitflowCause {

    public static final String PARAM_HOTFIX = "finishHotfix";
    public static final String PARAM_HOTFIX_BRANCH = "hotfixBranch";

    private final String hotfixBranche;

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param structuredActionConent the structured content for the selected action to be instanciated.
     * @param dryRun is the build dryRun or not
     */
    public FinishHotfixCause(JSONObject structuredActionConent, boolean dryRun) {
        this(structuredActionConent.getJSONObject(PARAM_HOTFIX).getString(PARAM_HOTFIX_BRANCH), dryRun);
    }

    public FinishHotfixCause(String hotfixBranche, boolean dryRun) {
        super(dryRun);
        this.hotfixBranche = hotfixBranche;
    }

    @Override
    public String getVersionForBadge() {
        //TODO
        return null;
    }

    public String getHotfixBranche() {
        return hotfixBranche;
    }
}
