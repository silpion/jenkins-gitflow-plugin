package org.jenkinsci.plugins.gitflow.action.buildtype;

import hudson.Launcher;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

/**
 * Factory class to create instances for the build-type-specific actions regarding to the type of a build.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class BuildTypeActionFactory {

    /**
     * Creates an instance for the build-type-specific actions regarding to the type of the build in progress.
     *
     * @param build    the <i>Gitflow</i> build that is in progress.
     * @param launcher can be used to launch processes for this build - even if the build runs remotely.
     * @param listener can be used to send any message.
     * @return a new instance of the build-type-specific action class.
     */
    public static AbstractBuildTypeAction newInstance(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {
        final AbstractBuildTypeAction abstractBuildTypeAction;

        if (build instanceof MavenModuleSetBuild) {
            final MavenModuleSetBuild mavenModuleSetBuild = (MavenModuleSetBuild) build;
            abstractBuildTypeAction = new MavenBuildTypeAction(mavenModuleSetBuild, launcher, listener);
        } else {
            final MavenModuleSetBuild mavenModuleSetBuild = (MavenModuleSetBuild) build;
            abstractBuildTypeAction = new UnknownBuildTypeAction(mavenModuleSetBuild, launcher, listener);
        }

        return abstractBuildTypeAction;
    }
}
