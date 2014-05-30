package org.jenkinsci.plugins.gitflow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

import hudson.model.AbstractProject;
import hudson.model.Result;

/**
 * Handles the Gitflow plugin properties for a Jenkins job/project.
 *
 * @author Marc Rohlfs, Silpion IT-Solutions GmbH - rohlfs@silpion.de
 */
public class GitflowPluginProperties {

    private static final String GITFLOW_PROPERTIES_FILE = "gitflow-plugin.properties";
    private static final int MODIFICATION_CHECK_INTERVALL_MILLISECONDS = 5000;

    private static final Comparator<Result> RESULT_ORDER_BY_SEVERITY = new Comparator<Result>() {

        /** {@inheritDoc} */
        public int compare(final Result result1, final Result result2) {
            return result1.ordinal - result2.ordinal;
        }
    };

    private static final Predicate<Result> RESULT_UNSTABLE_OR_WORSE = new Predicate<Result>() {

        /** {@inheritDoc} */
        public boolean apply(@Nullable final Result result) {
            return Result.UNSTABLE.isBetterOrEqualTo(result);
        }
    };

    private final Properties properties = new Properties();
    private final File propertiesFile;
    private final boolean dryRun;

    private long nextModificationCheck;

    /**
     * Initialises a new {@link GitflowPluginProperties} object for a Jenkins job/project.
     *
     * @param project the job/project
     */
    public GitflowPluginProperties(final AbstractProject<?, ?> project) {
        this(project, false);
    }

    /**
     * Initialises a new {@link GitflowPluginProperties} object for a Jenkins job/project.
     *
     * @param project the job/project
     * @param dryRun is the build dryRun or not
     */
    public GitflowPluginProperties(final AbstractProject<?, ?> project, final boolean dryRun) {
        this.propertiesFile = new File(project.getRootDir(), GITFLOW_PROPERTIES_FILE);
        this.dryRun = dryRun;
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
     * Returns the simple/local names of all branches with <i>UNSTABLE</i> (or worse) results, grouped by result.
     *
     * @return a map containing the names of all branches with <i>UNSTABLE</i> (or worse) results, where each key is a {@link Result} and the regarding value
     * is a collection with the names of the branches with that result.
     * @throws IOException if the file with the Gitflow properties cannot be loaded or saved.
     */
    public Map<Result, Collection<String>> loadUnstableBranchesGroupedByResult() throws IOException {
        return Maps.filterKeys(this.loadBranchesGroupedByResult(), RESULT_UNSTABLE_OR_WORSE);
    }

    private Map<Result, Collection<String>> loadBranchesGroupedByResult() throws IOException {
        final Map<Result, Collection<String>> branchesGroupedByResult = new TreeMap<Result, Collection<String>>(RESULT_ORDER_BY_SEVERITY);

        this.loadProperties();

        for (final String propertyName : this.properties.stringPropertyNames()) {
            final String[] propertyNameTokens = StringUtils.split(propertyName, ".", 2);
            if (ArrayUtils.getLength(propertyNameTokens) == 2 && "branchResult".equals(propertyNameTokens[0])) {
                final Result branchResult = Result.fromString(this.properties.getProperty(propertyName));
                Collection<String> branchNamesWithResult = branchesGroupedByResult.get(branchResult);
                if (branchNamesWithResult == null) {
                    branchNamesWithResult = new TreeSet<String>();
                    branchesGroupedByResult.put(branchResult, branchNamesWithResult);
                }
                branchNamesWithResult.add(propertyNameTokens[1]);
            }
        }

        return branchesGroupedByResult;
    }

    /**
     * Sets the current result and the version of the project files on a Git branch and saves it to the Gitflow plugin properties.
     *
     * @param branch the branch.
     * @param result the result/state of the branch.
     * @param version the version.
     * @throws IOException if the file cannot be loaded or saved.
     */
    public void saveResultAndVersionForBranch(final String branch, final Result result, final String version) throws IOException {
        if (!this.dryRun) {
            this.loadProperties();
            this.properties.setProperty("branchResult." + branch, result.toString());
            this.properties.setProperty("branchVersion." + branch, version);
            this.saveProperties();
        }
    }

    /**
     * Sets the current result and the version of the project files on a collection of Git branch and saves it to the Gitflow plugin properties.
     *
     * @param branches the branches.
     * @param result the result/state of the branch.
     * @param version the version.
     * @throws IOException if the file cannot be loaded or saved.
     */
    public void saveResultAndVersionForBranches(final Collection<String> branches, final Result result, final String version) throws IOException {
        if (!this.dryRun) {
            this.loadProperties();
            for (final String branch : branches) {
                this.properties.setProperty("branchResult." + branch, result.toString());
                this.properties.setProperty("branchVersion." + branch, version);
            }
            this.saveProperties();
        }
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
