package org.jenkinsci.plugins.gitflow.cause;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * Factory class to create instances for the cause of a <i>Gitflow</i> build.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GitflowCauseFactory {

    /**
     * Creates a cause instance for the <i>Gitflow</i> build.
     *
     * @param formParams the form params containing the settings for the cause instance to be created.
     * @return a new cause instance for the <i>Gitflow</i> build.
     */
    public static AbstractGitflowCause newInstance(final Map<String, String[]> formParams) throws IOException {
        final AbstractGitflowCause gitflowCause;

        // The action denotes the cause to be created.
        final String action = StringUtils.join(formParams.get("action"));

        // Extract the params for the cause to be created.
        final Map<String, String> actionParams = new HashMap<String, String>();
        for (final Map.Entry<String, String[]> formParamEntry : formParams.entrySet()) {
            final String formParamKey = formParamEntry.getKey();
            if (action.equals(StringUtils.substringBefore(formParamKey, "_"))) {
                final String actionParamKey = StringUtils.substringAfter(formParamKey, "_");
                final String actionParamValue = StringUtils.join(formParamEntry.getValue());
                actionParams.put(actionParamKey, actionParamValue);
            }
        }

        // Instanciate the cause object for the
        if ("startRelease".equals(action)) {
            gitflowCause = new StartReleaseCause(actionParams);
        } else {
            // Only an IOException causes the build to fail properly.
            throw new IOException("Unknown Gitflow action " + action);
        }

        return gitflowCause;
    }
}
