package org.jenkinsci.plugins.gitflow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

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
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.jenkinsci.plugins.gitflow.data.GitflowPluginData;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.maven.MavenModuleSet;
import hudson.model.Result;
import hudson.plugins.git.Branch;
import hudson.plugins.git.GitSCM;

/**
 * @author Hannes Osius, Silpion IT-Solutions GmbH  osius@silpion.de
 */
public class TestHotfixIT {

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

    @Test
    public void testStartHotfix() throws Exception {

        File gitRepro = folder.newFolder("testrepo.git");
        setUpGitRepo("/TestHotfixAction/testrepo.git.zip", gitRepro);

        //make a build before
        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        GitflowPluginData data = mavenProject.getLastBuild().getAction(GitflowPluginData.class);
        data.recordRemoteBranch("origin", "hotfix/foobar1", Result.SUCCESS, "1.1.2-SNAPSHOT");
        data.recordRemoteBranch("origin", "hotfix/foobar2", Result.SUCCESS, "1.1.3-SNAPSHOT");
        data.recordRemoteBranch("origin", "hotfix/foobar3", Result.SUCCESS, "1.1-SNAPSHOT");
        data.recordRemoteBranch("origin", "hotfix/foobar4", Result.SUCCESS, "1.1.5-SNAPSHOT");

        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        JenkinsRule.WebClient webClient = j.createWebClient();
        HtmlPage page = webClient.goTo(mavenProject.getUrl() + "gitflow");

        HtmlForm form = page.getFormByName("performGitflowRelease");
        checkRadioButton("action", "testHotfix", page);

        List<HtmlSelect> selects = form.getSelectsByName("");
        HtmlSelect hotfixSelect = getHtmlSelect(selects, "foobar");

        assertThat("HotFixSelect not found", hotfixSelect, is(notNullValue()));
        assertThat("HotFixSelect missing Element", hotfixSelect.getOptions().size(), is(4));
        for (HtmlOption htmlOption : hotfixSelect.getOptions()) {
            if ("foobar3".equals(htmlOption.getValueAttribute())) {
                htmlOption.setSelected(true);
            } else {
                htmlOption.setSelected(false);
            }
        }
        //Check values in (nextHotfixReleaseVersion, hotfixReleaseVersion)
        //foobar3 is select, so the Fields must be visible
        for (HtmlElement htmlElement : page.getElementsByName("hotfixBranch")) {
            if (htmlElement.isDisplayed()){
                if (htmlElement instanceof HtmlTextInput) {
                    HtmlTextInput textInput = (HtmlTextInput) htmlElement;
                    assertThat(textInput.getText(),is("hotfix/foobar3") );
                }
            }
        }
        for (HtmlElement htmlElement : page.getElementsByName("hotfixReleaseVersion")) {
            if (htmlElement.isDisplayed()){
                if (htmlElement instanceof HtmlTextInput) {
                    HtmlTextInput textInput = (HtmlTextInput) htmlElement;
                    assertThat(textInput.getText(),is("1.1") );
                }
            }
        }
        for (HtmlElement htmlElement : page.getElementsByName("nextHotfixReleaseVersion")) {
            if (htmlElement.isDisplayed()){
                if (htmlElement instanceof HtmlTextInput) {
                    HtmlTextInput textInput = (HtmlTextInput) htmlElement;
                    assertThat(textInput.getText(),is("1.2-SNAPSHOT"));
                    textInput.setAttribute("value", "1.2.1-SNAPSHOT");
                }
            }
        }

        j.submit(form);
        j.waitUntilNoActivity();
        mavenProject.getLastBuild().getLogText().writeLogTo(0, System.out);
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        GitflowPluginData data1 = mavenProject.getLastBuild().getAction(GitflowPluginData.class);
        assertThat(data1.getRemoteBranch("origin", "hotfix/foobar3").getLastBuildVersion(), is("1.2.1-SNAPSHOT"));
        assertThat(data1.getRemoteBranch("origin", "hotfix/foobar3").getLastBuildResult(), is(Result.SUCCESS));

        //check the Git-Repro
        File repository = folder.newFolder();
        GitClient gitClient = Git.with(j.createTaskListener(), new EnvVars()).in(repository).getClient();
        gitClient.clone(gitRepro.getAbsolutePath(), "origin", false, null);
        Map<String, Branch> branches = getAllBranches(gitClient);

        assertThat(branches.keySet(), containsInAnyOrder("origin/hotfix/foobar1", "origin/hotfix/foobar2", "origin/hotfix/foobar3", "origin/hotfix/foobar4",
                                                         "origin/hotfix/foobar5", "origin/master", "origin/develop", "master", "origin/release/1.0"));

        gitClient.checkoutBranch("hotfix/foobar3", branches.get("origin/hotfix/foobar3").getSHA1String());
        checkMultiModuleProject(repository, "1.2.1-SNAPSHOT", 4);

    }

    @Test
    public void testStartHotfixDryrun() throws Exception {

        File gitRepro = folder.newFolder("testrepo.git");
        setUpGitRepo("/TestHotfixAction/testrepo.git.zip", gitRepro);

        //make a build before
        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        GitflowPluginData data = mavenProject.getLastBuild().getAction(GitflowPluginData.class);
        data.recordRemoteBranch("origin", "hotfix/foobar3", Result.SUCCESS, "1.1-SNAPSHOT");

        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        JenkinsRule.WebClient webClient = j.createWebClient();
        HtmlPage page = webClient.goTo(mavenProject.getUrl() + "gitflow");

        HtmlForm form = page.getFormByName("performGitflowRelease");
        checkRadioButton("action", "testHotfix", page);

        List<HtmlSelect> selects = form.getSelectsByName("");
        HtmlSelect hotfixSelect = getHtmlSelect(selects, "foobar");

        assertThat("HotFixSelect not found", hotfixSelect, is(notNullValue()));
        assertThat("HotFixSelect missing Element", hotfixSelect.getOptions().size(), is(1));
        hotfixSelect.getOptions().get(0).setSelected(true);
        HtmlCheckBoxInput dryRunElement = (HtmlCheckBoxInput) page.getElementsByName("dryRun").get(0);
        dryRunElement.setChecked(true);

        j.submit(form);
        j.waitUntilNoActivity();
        mavenProject.getLastBuild().getLogText().writeLogTo(0, System.out);
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        GitflowPluginData data1 = mavenProject.getLastBuild().getAction(GitflowPluginData.class);
        assertThat(data1.getRemoteBranch("origin", "hotfix/foobar3").getLastBuildVersion(), is("1.1-SNAPSHOT"));
        assertThat(data1.getRemoteBranch("origin", "hotfix/foobar3").getLastBuildResult(), is(Result.SUCCESS));

        //check the Git-Repro
        File repository = folder.newFolder();
        GitClient gitClient = Git.with(j.createTaskListener(), new EnvVars()).in(repository).getClient();
        gitClient.clone(gitRepro.getAbsolutePath(), "origin", false, null);
        Map<String, Branch> branches = getAllBranches(gitClient);

        assertThat(branches.keySet(), containsInAnyOrder("origin/hotfix/foobar1", "origin/hotfix/foobar2", "origin/hotfix/foobar3", "origin/hotfix/foobar4",
                                                         "origin/hotfix/foobar5", "origin/master", "origin/develop", "master", "origin/release/1.0"));

        gitClient.checkoutBranch("hotfix/foobar3", branches.get("origin/hotfix/foobar3").getSHA1String());
        checkMultiModuleProject(repository, "1.1-SNAPSHOT", 4);
    }

    @Test
    public void testStartHotfixFail() throws Exception {

        File gitRepro = folder.newFolder("testrepo.git");
        setUpGitRepo("/TestHotfixAction/testrepo.git.zip", gitRepro);

        //make a build before
        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        GitflowPluginData data = mavenProject.getLastBuild().getAction(GitflowPluginData.class);
        data.recordRemoteBranch("origin", "hotfix/foobar3", Result.SUCCESS, "1.1-SNAPSHOT");

        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        JenkinsRule.WebClient webClient = j.createWebClient();
        HtmlPage page = webClient.goTo(mavenProject.getUrl() + "gitflow");

        HtmlForm form = page.getFormByName("performGitflowRelease");
        checkRadioButton("action", "testHotfix", page);

        List<HtmlSelect> selects = form.getSelectsByName("");
        HtmlSelect hotfixSelect = getHtmlSelect(selects, "foobar");

        assertThat("HotFixSelect not found", hotfixSelect, is(notNullValue()));
        assertThat("HotFixSelect missing Element", hotfixSelect.getOptions().size(), is(1));
        hotfixSelect.getOptions().get(0).setSelected(true);

        mavenProject.setGoals("fail");
        j.submit(form);
        j.waitUntilNoActivity();
        mavenProject.getLastBuild().getLogText().writeLogTo(0, System.out);
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.FAILURE));

        GitflowPluginData data1 = mavenProject.getLastBuild().getAction(GitflowPluginData.class);
        assertThat(data1.getRemoteBranch("origin", "hotfix/foobar3").getLastBuildVersion(), is("1.1-SNAPSHOT"));
        assertThat(data1.getRemoteBranch("origin", "hotfix/foobar3").getLastBuildResult(), is(Result.FAILURE));

        //check the Git-Repro
        File repository = folder.newFolder();
        GitClient gitClient = Git.with(j.createTaskListener(), new EnvVars()).in(repository).getClient();
        gitClient.clone(gitRepro.getAbsolutePath(), "origin", false, null);
        Map<String, Branch> branches = getAllBranches(gitClient);

        assertThat(branches.keySet(), containsInAnyOrder("origin/hotfix/foobar1", "origin/hotfix/foobar2", "origin/hotfix/foobar3", "origin/hotfix/foobar4",
                                                         "origin/hotfix/foobar5", "origin/master", "origin/develop", "master", "origin/release/1.0"));

        gitClient.checkoutBranch("hotfix/foobar3", branches.get("origin/hotfix/foobar3").getSHA1String());
        checkMultiModuleProject(repository, "1.1-SNAPSHOT", 4);
    }

    /**
     * finding the HtmlSelect by the Value-Attribute.
     *
     * @param selects the list of Selects
     * @param name the Name to Find
     * @return null or the a Matching Select.
     */
    private HtmlSelect getHtmlSelect(final List<HtmlSelect> selects, String name) {
        for (HtmlSelect select : selects) {
            for (HtmlOption htmlOption : select.getOptions()) {
                if (StringUtils.startsWith(htmlOption.getValueAttribute(), name)) {
                    return select;
                }
            }
        }
        return null;
    }

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
     * <p/>
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
    private void checkMultiModuleProject(File rootDir, final String version, int pomCount) throws IOException, InterruptedException, XmlPullParserException {
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


