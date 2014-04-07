package org.jenkinsci.plugins.gitflow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import hudson.model.AbstractProject;

/**
 * Handles the Gitflow plugin properties for a Jenkins job/project.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GitflowPluginProperties {

    private static final String GITFLOW_PROPERTIES_FILE = "gitflow-plugin.properties";
    private static final int MODIFICATION_CHECK_INTERVALL_MILLISECONDS = 5000;

    private final Properties properties = new Properties();
    private final File propertiesFile;

    private long nextModificationCheck;

    /**
     * Initialises a new {@link GitflowPluginProperties} object for a Jenkins job/project.
     *
     * @param project the job/project
     */
    public GitflowPluginProperties(final AbstractProject<?, ?> project) {
        this.propertiesFile = new File(project.getRootDir(), GITFLOW_PROPERTIES_FILE);
    }

    /**
     * Loads and returns the Git branches.
     *
     * @return the list of Git branches.
     * @throws IOException if the Gitflow plugin properties file cannot be loaded.
     */
    public Collection<String> loadBranches() throws IOException {
        final Collection<String> branches = new HashSet<String>();

        this.loadProperties();

        for (final String propertyName : this.properties.stringPropertyNames()) {
            final String[] propertyNameTokens = StringUtils.split(propertyName, ".", 2);
            if (ArrayUtils.getLength(propertyNameTokens) == 2 && "branchVersion".equals(propertyNameTokens[0])) {
                branches.add(propertyNameTokens[1]);
            }
        }

        return branches;
    }

    /**
     * Loads and returns the current version of the project files on a specified Git branch.
     *
     * @param branch the branch.
     * @return the version of the project files on the specified Git branch.
     * @throws IOException if the file cannot be loaded or saved.
     */
    public String loadVersionForBranch(final String branch) throws IOException {
        this.loadProperties();
        return this.properties.getProperty("branchVersion." + branch);
    }

    /**
     * Sets the current version of the project files on a Git branch and saves it to the Gitflow plugin properties.
     *
     * @param branch the branch.
     * @param version the version.
     * @throws IOException if the file cannot be loaded or saved.
     */
    public void saveVersionForBranch(final String branch, final String version) throws IOException {
        this.loadProperties();
        this.properties.setProperty("branchVersion." + branch, version);
        this.saveProperties();
    }

    /**
     * Sets the current version of the project files on a collection of Git branch and saves it to the Gitflow plugin properties.
     *
     * @param branches the branches.
     * @param version the version.
     * @throws IOException if the file cannot be loaded or saved.
     */
    public void saveVersionForBranches(final Collection<String> branches, final String version) throws IOException {
        this.loadProperties();
        for (final String branch : branches) {
            this.properties.setProperty("branchVersion." + branch, version);
        }
        this.saveProperties();
    }

    private void loadProperties() throws IOException {

        // Only check for changes in the properties file if the modification check interval has expired.
        if (System.currentTimeMillis() >= this.nextModificationCheck) {

            // Only load the properties if the file has been changed.
            final long lastModificationCheck = this.nextModificationCheck - MODIFICATION_CHECK_INTERVALL_MILLISECONDS;
            if (this.propertiesFile.lastModified() > lastModificationCheck) {

                if (this.propertiesFile.isDirectory()) {
                    throw new FileNotFoundException(GITFLOW_PROPERTIES_FILE + " is a directory");
                } else if (this.propertiesFile.isFile()) {

                    final FileInputStream fileInputStream = new FileInputStream(this.propertiesFile);
                    try {
                        this.properties.load(fileInputStream);
                    } finally {
                        IOUtils.closeQuietly(fileInputStream);
                    }
                }
            }

            // Update the timestamp for the next modification check.
            this.nextModificationCheck = System.currentTimeMillis() + MODIFICATION_CHECK_INTERVALL_MILLISECONDS;
        }
    }

    private void saveProperties() throws IOException {

        // Save the properties.
        final FileOutputStream fileOutputStream = new FileOutputStream(this.propertiesFile);
        try {
            this.properties.store(fileOutputStream, "Gitflow plugin properties for the current project");
        } finally {
            IOUtils.closeQuietly(fileOutputStream);
        }

        // Update the timestamp for the next modification check.
        this.nextModificationCheck = System.currentTimeMillis() + MODIFICATION_CHECK_INTERVALL_MILLISECONDS;
    }
}
