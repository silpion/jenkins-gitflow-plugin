package org.jenkinsci.plugins.gitflow.cause;

import java.io.IOException;

import net.sf.json.JSONObject;

/**
 * Factory class to create instances for the cause of a <i>Gitflow</i> build.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GitflowCauseFactory {

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param submittedForm the structured content of the submitted action.
     * @return a new cause instance for the <i>Gitflow</i> build.
     */
    public static AbstractGitflowCause newInstance(final JSONObject submittedForm) throws IOException {
        final AbstractGitflowCause gitflowCause;

        // The action denotes the cause to be created.
        final JSONObject submittedActionConent = submittedForm.getJSONObject("action");
        final String action = submittedActionConent.getString("value");

        // Instanciate the cause object for the submitted action.
        if ("startRelease".equals(action)) {
            gitflowCause = new StartReleaseCause(submittedActionConent);
        } else if ("testRelease".equals(action)) {
            gitflowCause = new TestReleaseCause(submittedActionConent);
        } else {
            // Only an IOException causes the build to fail properly.
            throw new IOException("Unknown Gitflow action " + action);
        }

        return gitflowCause;
    }
}
