package de.silpion.jenkins.plugins.gitflow;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Executor;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.tasks.BuildWrapper;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.io.IOException;
import java.io.PrintStream;

import static hudson.model.Result.SUCCESS;
import static java.util.Collections.singletonList;

/**
 * A {@link ParametersAction} implementation for the <i>Gitflow</i> plugin.
 * <p>
 * Provides a way to omit the main build of a build excution.
 *
 * @author Marc Rohlfs, T-Systems Multimedia Solutions GmbH - Marc.Rohlfs@t-systems.com
 */
@Restricted(NoExternalUse.class)
public class OmitMainBuildParametersAction extends ParametersAction {

    private static final String MSG_ABORTING_TO_OMIT_MAIN_BUILD = "Intentionally aborting to omit the main build";
    private static final String MSG_PATTERN_ABORTING_TO_OMIT_MAIN_BUILD = "Gitflow - %s: " + MSG_ABORTING_TO_OMIT_MAIN_BUILD + "%n";

    private static final String OMIT_MAIN_BUILD_PARAMETER_NAME = "omitMainBuild";
    private static final ParameterValue OMIT_MAIN_BUILD_PARAMETER_VALUE = new ParameterValue(OMIT_MAIN_BUILD_PARAMETER_NAME) {

        private static final long serialVersionUID = 7216028868241774173L;

        /** {@inheritDoc} */
        @Override
        public BuildWrapper createBuildWrapper(final AbstractBuild<?, ?> build) {
            return new BuildWrapper() {

                /** {@inheritDoc} */
                @Override
                public Environment setUp(@SuppressWarnings("rawtypes") final AbstractBuild build1, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
                    throw new InterruptedException("Intentionally thrown to omit the main build");
                }
            };
        }
    };

    public OmitMainBuildParametersAction() {
        super(singletonList(OMIT_MAIN_BUILD_PARAMETER_VALUE));
    }

    public void interrupt(final PrintStream consoleLogger, final String gitflowActionName) {

        // The result of the interrupted build must be set to SUCCESS. Otherwise the build would be declared as FAILED.
        getExecutor().interrupt(SUCCESS);

        consoleLogger.printf(MSG_PATTERN_ABORTING_TO_OMIT_MAIN_BUILD, gitflowActionName);
    }

    private static Executor getExecutor() {
        return Executor.currentExecutor();
    }
}
