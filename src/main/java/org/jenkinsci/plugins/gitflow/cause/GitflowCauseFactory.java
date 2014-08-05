package org.jenkinsci.plugins.gitflow.cause;

import java.io.IOException;

import net.sf.json.JSONObject;

/**
 * Factory class to create instances for the cause of a <i>Gitflow</i> build.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GitflowCauseFactory {

    private static final String PARAM_DRY_RUN = "dryRun";
    private static final String JSON_CONTENT_ACTION = "action";
    private static final String JSON_CONTENT_VALUE = "value";

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param submittedForm the structured content of the submitted action.
     * @return a new cause instance for the <i>Gitflow</i> build.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     */
    public static AbstractGitflowCause newInstance(final JSONObject submittedForm) throws IOException {
        final AbstractGitflowCause gitflowCause;

        // The action denotes the cause to be created.
        final JSONObject submittedActionConent = submittedForm.getJSONObject(JSON_CONTENT_ACTION);
        final String action = submittedActionConent.getString(JSON_CONTENT_VALUE);
        final boolean dryRun = submittedForm.getBoolean(PARAM_DRY_RUN);

        // Instanciate the cause object for the submitted action.
        if ("startRelease".equals(action)) {
            gitflowCause = new StartReleaseCause(submittedActionConent, dryRun);
        } else if ("testRelease".equals(action)) {
            gitflowCause = new TestReleaseCause(submittedActionConent, dryRun);
        } else if ("publishRelease".equals(action)) {
            gitflowCause = new PublishReleaseCause(submittedActionConent, dryRun);
        } else if ("finishRelease".equals(action)) {
            gitflowCause = new FinishReleaseCause(submittedActionConent, dryRun);
        } else if ("finishHotfix".equals(action)) {
            gitflowCause = new FinishHotfixCause(submittedActionConent, dryRun);
        } else {
            // Only an IOException causes the build to fail properly.
            throw new IOException("Unknown Gitflow action " + action);
        }

        return gitflowCause;
    }
}
