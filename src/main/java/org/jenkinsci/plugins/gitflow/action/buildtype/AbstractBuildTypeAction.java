package org.jenkinsci.plugins.gitflow.action.buildtype;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.jenkinsci.plugins.gitflow.action.AbstractActionBase;

import hudson.Launcher;
import hudson.console.ConsoleNote;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Result;

/**
 * Abstract base class for the different build-type-specific actions.
 *
 * @param <T> the build in progress.
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public abstract class AbstractBuildTypeAction<T extends AbstractBuild<?, ?>> extends AbstractActionBase<T> {

    protected final Launcher launcher;

    /**
     * Initialises a new build-type-specific action.
     *
     * @param build the <i>Gitflow</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     */
    protected AbstractBuildTypeAction(final T build, final Launcher launcher, final BuildListener listener) {
        super(build, listener);
        this.launcher = launcher;
    }

    /**
     * Returns the current version number of the project files.
     *
     * @return the current version number of the project files.
     */
    public abstract String getCurrentVersion();

    /**
     * Update the version numbers in the project files to the provided version.
     *
     * @param version the version to be set in the project files.
     * @return the files that were modified during the update.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    public abstract List<String> updateVersion(final String version) throws IOException, InterruptedException;

    /**
     * Add configurations and settings to the main build, that changes the behaviour of the main build so that it performs a release build.
     *
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     */
    public abstract void prepareForReleaseBuild() throws IOException;

    /**
     * Add environmental variables to the given map that change the behaviour of the build so that it doesn't publish the built archives.
     * <p/>
     * <b>Please note</b> that the default behaviour of the main build might depend on the actual build configuration in the source code that is checked out
     * and built by the Jenkins jobs. E.g. if it overwrites the {@code skip} parameter of the Maven Deploy Plugin, it might also overwrite the configuration
     * change that is intended by this method.
     *
     * @param buildEnvVars the map to add to environmental variables to.
     * @throws IOException if an error occurs that causes/should cause the build to fail.
     */
    public abstract void preventArchivePublication(final Map<String, String> buildEnvVars) throws IOException;

    /**
     * {@link BuildListener} delegate omitting the Jenkins console output and redirecting it to a file.
     *
     * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
     */
    protected class BuildListenerDelegate implements BuildListener {

        private static final long serialVersionUID = 1497107043585114757L;

        private final BuildListener delegate;
        private final PrintStream logger;

        @SuppressWarnings("ResultOfMethodCallIgnored")
        public BuildListenerDelegate(final BuildListener delegate, final File outputLogFile) throws FileNotFoundException {
            outputLogFile.getParentFile().mkdirs();
            this.logger = new PrintStream(new FileOutputStream(outputLogFile));
            this.delegate = delegate;
        }

        /** {@inheritDoc} */
        public void started(final List<Cause> causes) {
            this.delegate.started(causes);
        }

        /** {@inheritDoc} */
        public void finished(final Result result) {
            this.delegate.finished(result);
        }

        /** {@inheritDoc} */
        public PrintStream getLogger() {
            return this.logger;
        }

        /** {@inheritDoc} */
        public void annotate(@SuppressWarnings("rawtypes") final ConsoleNote ann) throws IOException {
            this.delegate.annotate(ann);
        }

        /** {@inheritDoc} */
        public void hyperlink(final String url, final String text) throws IOException {
            this.delegate.hyperlink(url, text);
        }

        /** {@inheritDoc} */
        public PrintWriter error(final String msg) {
            return this.delegate.error(msg);
        }

        /** {@inheritDoc} */
        public PrintWriter error(final String format, final Object... args) {
            return this.delegate.error(format, args);
        }

        /** {@inheritDoc} */
        public PrintWriter fatalError(final String msg) {
            return this.delegate.fatalError(msg);
        }

        /** {@inheritDoc} */
        public PrintWriter fatalError(final String format, final Object... args) {
            return this.delegate.fatalError(format, args);
        }
    }
}
