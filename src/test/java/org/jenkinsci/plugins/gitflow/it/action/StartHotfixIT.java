package org.jenkinsci.plugins.gitflow.it.action;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.jenkinsci.plugins.gitflow.GitflowBuildWrapper;
import org.jenkinsci.plugins.gitflow.data.GitflowPluginData;
import org.jenkinsci.plugins.gitflow.data.RemoteBranch;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.maven.MavenModuleSet;
import hudson.model.Result;
import hudson.plugins.git.Branch;
import hudson.plugins.git.GitSCM;

/**
 * Integration Test for the Gitflow action <i>Start Hotfix</i>.
 *
 * @author Hannes Osius, Silpion IT-Solutions GmbH
 */
public class StartHotfixIT {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private MavenModuleSet mavenProject;

    @Before
    public void setUp() throws Exception {
        j.configureDefaultMaven();
        mavenProject = j.createMavenProject();
        mavenProject.getBuildWrappersList().add(new GitflowBuildWrapper());
    }

    /**
     * Run the <i>Start Hotfix</i> Gitflow action via a webclient.
     *
     * @throws Exception
     */
    @Test
    public void testStartHotfix() throws Exception {

        File gitRepro = folder.newFolder("testrepo.git");
        setUpGitRepo("/StartHotfixAction/testrepo.git.zip", gitRepro);

        //make a build before
        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));
        GitflowPluginData data = mavenProject.getLastBuild().getAction(GitflowPluginData.class);

        addRemoteBranch(data,"origin", "master", Result.SUCCESS, "1.2", "1.2", "1.2");

        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        JenkinsRule.WebClient webClient = j.createWebClient();
        HtmlPage page = webClient.goTo(mavenProject.getUrl() + "gitflow");

        assertThat("more than on element found", page.getElementsByName("hotfixReleaseVersion").size(), is(1));
        HtmlTextInput nameElement = (HtmlTextInput) page.getElementsByName("hotfixReleaseVersion").get(0);

        assertThat("more than on element found", page.getElementsByName("nextHotfixDevelopmentVersion").size(), is(1));
        HtmlTextInput versionElement = (HtmlTextInput) page.getElementsByName("nextHotfixDevelopmentVersion").get(0);

        assertThat("more than on element found", page.getElementsByName("dryRun").size(), is(1));
        HtmlCheckBoxInput dryRunElement = (HtmlCheckBoxInput) page.getElementsByName("dryRun").get(0);

        assertThat(nameElement.getText(), is("1.2"));
        assertThat(versionElement.getText(), is("1.2.1-SNAPSHOT"));
        assertThat(dryRunElement.isChecked(), is(false));

        //set values and submit the form
        checkRadioButton("action", "startHotfix", page);
        nameElement.setAttribute("value", "superHotfix");
        versionElement.setAttribute("value","1.2.2-SNAPSHOT");
        j.submit(page.getFormByName("performGitflowRelease"));

        j.waitUntilNoActivity();
        assertThat("StartHotfixAction failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        //check the Git-Repro
        File repository = folder.newFolder();
        GitClient gitClient = Git.with(j.createTaskListener(), new EnvVars()).in(repository).getClient();
        gitClient.clone(gitRepro.getAbsolutePath(), "origin", false, null);
        Map<String, Branch> branches = getAllBranches(gitClient);

        assertThat(branches.keySet(), containsInAnyOrder("origin/hotfix/superHotfix", "origin/master", "origin/develop", "master", "origin/hotfix/1.3"));

        gitClient.checkoutBranch("hotfix/superHotfix", branches.get("origin/hotfix/superHotfix").getSHA1String());
        checkMultiModuleProject(repository, "1.2.2-SNAPSHOT", 4);
    }

    private void addRemoteBranch(final GitflowPluginData data, final String origin, final String branch, final Result result, final String buildVersion, final String baseReleaseVersion,
                                 final String lastReleaseVersion) {
        RemoteBranch masterBranch = data.getOrAddRemoteBranch(origin, branch);
        masterBranch.setLastBuildResult(result);
        masterBranch.setLastBuildVersion(buildVersion);
        masterBranch.setBaseReleaseVersion(baseReleaseVersion);
        masterBranch.setLastReleaseVersion(lastReleaseVersion);
    }

    /**
     * Run the <i>Start Hotfix</i> Gitflow action in <i>dryRun</i> via a webclient.
     *
     * @throws Exception
     */
    @Test
    public void testStartHotfixDryRun() throws Exception {

        File gitRepro = folder.newFolder("testrepo.git");
        setUpGitRepo("/StartHotfixAction/testrepo.git.zip", gitRepro);

        //make a build before
        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));
        GitflowPluginData data = mavenProject.getLastBuild().getAction(GitflowPluginData.class);
        addRemoteBranch(data, "origin", "master", Result.SUCCESS, "1.2", "1.2", "1.2");

        JenkinsRule.WebClient webClient = j.createWebClient();

        HtmlPage page = webClient.goTo(mavenProject.getUrl() + "gitflow");
        HtmlTextInput nameElement = (HtmlTextInput) page.getElementsByName("hotfixReleaseVersion").get(0);
        HtmlTextInput versionElement = (HtmlTextInput) page.getElementsByName("nextHotfixDevelopmentVersion").get(0);
        HtmlCheckBoxInput dryRunElement = (HtmlCheckBoxInput) page.getElementsByName("dryRun").get(0);

        //set values and submit the form
        checkRadioButton("action", "startHotfix", page);
        nameElement.setAttribute("value", "superHotfix");
        versionElement.setAttribute("value","1.2.2-SNAPSHOT");
        dryRunElement.setChecked(true);
        j.submit(page.getFormByName("performGitflowRelease"));

        j.waitUntilNoActivity();
        assertThat("StartHotfixAction failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        //check the Git-Repro
        File repository = folder.newFolder();
        GitClient gitClient = Git.with(j.createTaskListener(), new EnvVars()).in(repository).getClient();
        gitClient.clone(gitRepro.getAbsolutePath(), "origin", false, null);
        Map<String, Branch> branches = getAllBranches(gitClient);

        assertThat(branches.keySet(), containsInAnyOrder("origin/master", "origin/develop", "master", "origin/hotfix/1.3"));

        gitClient.checkoutBranch("hotfix/master", branches.get("origin/master").getSHA1String());
        checkMultiModuleProject(repository, "1.0-SNAPSHOT", 4);
    }

    /**
     * Run the <i>Start Hotfix</i> Gitflow action in via a webclient that fials.
     *
     * @throws Exception
     */
    @Test
    public void testStartHotfixFail() throws Exception {

        File gitRepro = folder.newFolder("testrepo.git");
        setUpGitRepo("/StartHotfixAction/testrepo.git.zip", gitRepro);

        //make a build before
        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));
        GitflowPluginData data = mavenProject.getLastBuild().getAction(GitflowPluginData.class);
        addRemoteBranch(data, "origin", "master", Result.SUCCESS, "1.2", "1.2", "1.2");

        JenkinsRule.WebClient webClient = j.createWebClient();

        HtmlPage page = webClient.goTo(mavenProject.getUrl() + "gitflow");
        HtmlTextInput nameElement = (HtmlTextInput) page.getElementsByName("hotfixReleaseVersion").get(0);
        HtmlTextInput versionElement = (HtmlTextInput) page.getElementsByName("nextHotfixDevelopmentVersion").get(0);

        // Set values and submit the form.
        // Note: Setting 'nameElement' to 1.3 breaks the build, because there already is a branch 'hotfix/1.3' in the test repository.
        checkRadioButton("action", "startHotfix", page);
        nameElement.setAttribute("value", "1.3");
        versionElement.setAttribute("value","1.2.2-SNAPSHOT");
        j.submit(page.getFormByName("performGitflowRelease"));

        j.waitUntilNoActivity();
        assertThat("StartHotfixAction failed", mavenProject.getLastBuild().getResult(), is(Result.FAILURE));

        //check the Git-Repro
        File repository = folder.newFolder();
        GitClient gitClient = Git.with(j.createTaskListener(), new EnvVars()).in(repository).getClient();
        gitClient.clone(gitRepro.getAbsolutePath(), "origin", false, null);
        Map<String, Branch> branches = getAllBranches(gitClient);

        assertThat(branches.keySet(), containsInAnyOrder("origin/master", "origin/develop", "master", "origin/hotfix/1.3"));

        gitClient.checkoutBranch("hotfix/master", branches.get("origin/master").getSHA1String());
        checkMultiModuleProject(repository, "1.0-SNAPSHOT", 4);

    }

    //**********************************************************************************************************************************************************
    //
    // Helper
    //
    //**********************************************************************************************************************************************************

    /**
     * Find a RadionButton an set it to checked.
     *
     * @param radioButtonGroup the name of the RadioButton Groupe.
     * @param buttonName the name of the button to Check
     * @param page the Page with the Button on it
     */
    private void checkRadioButton(String radioButtonGroup, String buttonName, HtmlPage page) {
        List<HtmlElement> elementList = page.getElementsByName(radioButtonGroup);
        for (HtmlElement htmlElement : elementList) {
            if (htmlElement instanceof HtmlRadioButtonInput) {
                HtmlRadioButtonInput radioButtonInput = (HtmlRadioButtonInput) htmlElement;
                String value = radioButtonInput.getAttributesMap().get("value").getValue();
                if (buttonName.equals(value)) {
                    radioButtonInput.setChecked(true);
                    break;
                }
            }
        }

    }

    /**
     * get all Branches for the Repo as Map.
     *
     * the key is the branchname and the value is the branch.
     *
     * @param gitClient the gitClient
     * @return the Map of Branches.
     * @throws InterruptedException
     */
    private Map<String, Branch> getAllBranches(GitClient gitClient) throws InterruptedException {
        Map<String, Branch> map = new HashMap<String, Branch>();
        for (Branch branch : gitClient.getBranches()) {
            map.put(branch.getName(), branch);
        }
        return map;
    }

    /**
     * Check the Version of a multi module Maven Project.
     *
     * @param rootDir the root of the Maven Project.
     * @param version the version to check.
     * @param pomCount the number of poms to check.
     * @throws IOException
     * @throws InterruptedException
     * @throws XmlPullParserException
     */
    private void checkMultiModuleProject(File rootDir, final String version, int pomCount) throws IOException, InterruptedException,
                                                                                                                        XmlPullParserException {
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        Iterator iterator = FileUtils.iterateFiles(rootDir, new String[] { "xml" }, true);
        int count = 0;
        while (iterator.hasNext()) {
            File next =  (File) iterator.next();
            if ("pom.xml".compareTo(next.getName())==0){
                count++;
                assertThat("Development Version was not set", mavenreader.read(new FileReader(next)).getVersion(), is(version));
            }
        }
        assertThat("not all Modules was checked",count, is(pomCount));
    }

    /**
     * unzip the give zipFile to the given temp-Folder
     *
     * @param pathToGitZip
     * @param pathToUnpack
     * @throws IOException
     * @throws URISyntaxException
     * @throws InterruptedException
     */
    private void setUpGitRepo(final String pathToGitZip, File pathToUnpack) throws IOException, URISyntaxException, InterruptedException {
        URL resource = getClass().getResource(pathToGitZip);
        FilePath filePath = new FilePath(new File(resource.toURI()));
        filePath.unzip(new FilePath(pathToUnpack));

        GitSCM gitSCM = new GitSCM(pathToUnpack.getAbsolutePath());
        mavenProject.setScm(gitSCM);
    }
}


