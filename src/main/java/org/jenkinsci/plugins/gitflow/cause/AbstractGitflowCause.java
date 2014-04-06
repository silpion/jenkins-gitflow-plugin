package org.jenkinsci.plugins.gitflow.cause;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gitflow.GitflowBuildWrapper;

import hudson.model.Cause;

import jenkins.model.Jenkins;

/**
 * The {@link Cause} object for the executed Gitflow actions.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class AbstractGitflowCause extends Cause {

    protected static GitflowBuildWrapper.DescriptorImpl getBuildWrapperDescriptor() {
        return (GitflowBuildWrapper.DescriptorImpl) Jenkins.getInstance().getDescriptor(GitflowBuildWrapper.class);
    }

    /**
     * Returns the value for the specified parameter from the provided parameters. If the value is emtpy or contains only whitespaces,
     * an {@link java.io.IOException} is thrown (note that only an @{@link java.io.IOException} prpoperly causes a build to fail).
     *
     * @param parameters the parameter map containing the entry with the requested value.
     * @param parameterName the name of the requested parameter.
     * @return the requested parameter value - if not blank.
     * @throws java.io.IOException if the requested parameter value is blank.
     */
    protected static String getParameterValueAssertNotBlank(final Map<String, String> parameters, final String parameterName) throws IOException {
        final String value = parameters.get(parameterName).trim();
        if (StringUtils.isBlank(value)) {
            throw new IOException(MessageFormat.format("{0} must be set with a non-empty value", parameterName));
        } else {
            return value;
        }
    }

    @Override
    public String getShortDescription() {
        return "Triggered by Gitflow Plugin";
    }
}
