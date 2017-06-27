package de.silpion.jenkins.plugins.gitflow.it.action;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import de.silpion.jenkins.plugins.gitflow.GitflowBuildWrapper;
import de.silpion.jenkins.plugins.gitflow.data.GitflowPluginData;
import de.silpion.jenkins.plugins.gitflow.data.RemoteBranch;
import hudson.FilePath;
import hudson.maven.MavenModuleSet;
import hudson.model.Result;
import hudson.plugins.git.Branch;
import hudson.plugins.git.GitSCM;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Abstract base class for integration tests for the <i>Jenkins Gitflow Plugin</i> actions.
 *
 * @author Marc Rohlfs, T-Systems Multimedia Solutions GmbH - Marc.Rohlfs@t-systems.com
 */
public abstract class AbstractGitflowActionIT {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    protected MavenModuleSet mavenProject;

    @Before
    public void setUp() throws Exception {
        ToolInstallations.configureDefaultMaven();
        this.mavenProject = this.jenkinsRule.getInstance().createProject(MavenModuleSet.class, this.getClass().getName());
        this.mavenProject.getBuildWrappersList().add(new GitflowBuildWrapper());
        this.mavenProject.setMavenOpts("-Djava.awt.headless=true");
    }

    /**
     * Add a {@link RemoteBranch} for the branchName with the given name to the {@link GitflowPluginData} and set the provided members.
     *
     * @param gitflowPluginData  the {@link GitflowPluginData} object
     * @param branchName         the name of the branchName
     * @param result             the {@link Result} to be set as <i>lastBuildResult</i>
     * @param buildVersion       the version number to be set as <i>lastBuildVersion</i>
     * @param baseReleaseVersion the version number to be set as <i>baseReleaseVersion</i>
     * @param lastReleaseVersion the version number to be set as <i>lastReleaseVersion</i>
     */
    protected static void addRemoteBranch(final GitflowPluginData gitflowPluginData, final String branchName, final Result result, final String buildVersion, final String baseReleaseVersion, final String lastReleaseVersion) {
        final RemoteBranch remoteBranch = gitflowPluginData.getOrAddRemoteBranch(branchName);
        remoteBranch.setLastBuildResult(result);
        remoteBranch.setLastBuildVersion(buildVersion);
        remoteBranch.setBaseReleaseVersion(baseReleaseVersion);
        remoteBranch.setLastReleaseVersion(lastReleaseVersion);
    }

    /**
     * Check the Version of a multi module Maven Project.
     *
     * @param rootDir  the root of the Maven Project.
     * @param version  the version to check.
     * @param pomCount the number of poms to check.
     * @throws IOException            if one of the Maven project files cannot be read.
     * @throws XmlPullParserException if one of the Maven project files cannot be parsed.
     */
    protected static void checkMultiModuleProject(final File rootDir, final String version, final int pomCount) throws IOException, XmlPullParserException {
        final MavenXpp3Reader mavenReader = new MavenXpp3Reader();

        int count = 0;
        final Iterator iterator = FileUtils.iterateFiles(rootDir, new String[]{"xml"}, true);
        while (iterator.hasNext()) {
            File next = (File) iterator.next();
            if ("pom.xml".compareTo(next.getName()) == 0) {
                count++;
                assertThat("Development Version was not set", mavenReader.read(new FileReader(next)).getVersion(), is(version));
            }
        }
        assertThat("not all Modules was checked", count, is(pomCount));
    }

    /**
     * Find a RadionButton and set it to <i>checked</i>.
     *
     * @param radioButtonGroup the name of the RadioButton group.
     * @param buttonName       the name of the button to Check
     * @param page             the Page with the Button on it
     */
    protected static void checkRadioButton(final String radioButtonGroup, final String buttonName, final HtmlPage page) {
        final List<DomElement> elementList = page.getElementsByName(radioButtonGroup);
        for (final DomElement element : elementList) {
            if (element instanceof HtmlRadioButtonInput) {
                final HtmlRadioButtonInput radioButtonInput = (HtmlRadioButtonInput) element;
                final String value = radioButtonInput.getAttributesMap().get("value").getValue();
                if (buttonName.equals(value)) {
                    radioButtonInput.setChecked(true);
                    break;
                }
            }
        }
    }

    /**
     * Get all branches of a Git repo as map.
     * <p>
     * The key is the branch name and the value is the branch object.
     *
     * @param gitClient the gitClient
     * @return the Map of branches.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    protected static Map<String, Branch> getAllBranches(final GitClient gitClient) throws InterruptedException {
        final Map<String, Branch> map = new HashMap<String, Branch>();
        for (final Branch branch : gitClient.getBranches()) {
            map.put(branch.getName(), branch);
        }
        return map;
    }

    /**
     * Get the remote branches of a Git repo as map.
     * <p>
     * The key is the branch name and the value is the branch object.
     *
     * @param gitClient the gitClient
     * @return the Map of remote branches.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    protected static Map<String, Branch> getRemoteBranches(final GitClient gitClient) throws InterruptedException {
        final Map<String, Branch> map = new HashMap<String, Branch>();
        for (final Branch branch : gitClient.getRemoteBranches()) {
            map.put(branch.getName(), branch);
        }
        return map;
    }

    /**
     * Unzip the given ZIP file containing a Git repository to the given folder and configures the SCM in the Maven project.
     *
     * @param pathToGitZip the path to the ZIP file
     * @param pathToUnpack the path to the extraction folder
     * @throws IOException          if the ZIP file cannot be unpacked or the SCM cannot be configured in the Maven project.
     * @throws URISyntaxException   if the ZIP file cannot properly be located.
     * @throws InterruptedException if the build is interrupted during execution.
     */
    protected void setUpGitRepo(final String pathToGitZip, final File pathToUnpack) throws IOException, URISyntaxException, InterruptedException {
        final URL resource = this.getClass().getResource(pathToGitZip);
        final FilePath filePath = new FilePath(new File(resource.toURI()));
        filePath.unzip(new FilePath(pathToUnpack));

        final GitSCM gitSCM = new GitSCM(pathToUnpack.getAbsolutePath());
        this.mavenProject.setScm(gitSCM);
    }
}
