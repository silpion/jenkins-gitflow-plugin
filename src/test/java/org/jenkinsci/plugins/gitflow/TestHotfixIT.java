package org.jenkinsci.plugins.gitflow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.jenkinsci.plugins.gitflow.data.GitflowPluginData;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;

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
        data.recordRemoteBranch("origin", "hotfix/foobar3", Result.SUCCESS, "1.1.4-SNAPSHOT");
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
        //TODO Check values in (nextHotfixReleaseVersion, hotfixReleaseVersion)
        j.submit(form);
        j.waitUntilNoActivity();
        mavenProject.getLastBuild().getLogText().writeLogTo(0, System.out);
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        //todo check the gitRepo in the MavenProject. The hotfix/foobar3 branch should be removed.

        //check the Git-Repro
        File repository = folder.newFolder();
        GitClient gitClient = Git.with(j.createTaskListener(), new EnvVars()).in(repository).getClient();
        gitClient.clone(gitRepro.getAbsolutePath(), "origin", false, null);
        Map<String, Branch> branches = getAllBranches(gitClient);

        assertThat(branches.keySet(), containsInAnyOrder("origin/hotfix/foobar1", "origin/hotfix/foobar2", "origin/hotfix/foobar4", "origin/hotfix/foobar5",
                                                         "origin/master", "origin/develop", "master", "origin/release/1.0"));
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


