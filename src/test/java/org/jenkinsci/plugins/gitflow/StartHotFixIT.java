package org.jenkinsci.plugins.gitflow;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.last;
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
import org.jenkinsci.plugins.gitflow.data.GitflowPluginData;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
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
 * @author Hannes Osius, Silpion IT-Solutions GmbH
 */

public class StartHotFixIT {

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

        File gitRepro = setUpGitRepo("/StartHotfixAction/testrepo.git.zip", folder.newFolder("testrepo.git"));

        //make a build before
        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));
        GitflowPluginData data = mavenProject.getLastBuild().getAction(GitflowPluginData.class);
        data.recordRemoteBranch("origin", "master", Result.SUCCESS, "1.2");

        JenkinsRule.WebClient webClient = j.createWebClient();

        HtmlPage page = webClient.goTo(mavenProject.getUrl() + "gitflow");

        List<HtmlElement> elementList = page.getElementsByName("action");
        for (HtmlElement htmlElement : elementList) {
            if (htmlElement instanceof HtmlRadioButtonInput) {
                HtmlRadioButtonInput element = (HtmlRadioButtonInput) htmlElement;
                String value = element.getAttributesMap().get("value").getValue();
                if ("startHotfix".equals(value)) {
                    element.setChecked(true);
                    break;
                }
            }
        }

        assertThat("more than on element found", page.getElementsByName("hotfixName").size(), is(1));
        HtmlTextInput nameElement = (HtmlTextInput) page.getElementsByName("hotfixName").get(0);

        assertThat("more than on element found", page.getElementsByName("nextHotfixDevelopmentVersion").size(), is(1));
        HtmlTextInput versionElement = (HtmlTextInput) page.getElementsByName("nextHotfixDevelopmentVersion").get(0);

        assertThat(nameElement.getText(), is(""));
        assertThat(versionElement.getText(), is("1.2.1-SNAPSHOT"));

        //set values and submit the form
        nameElement.setAttribute("value", "superHotfix");
        versionElement.setAttribute("value","1.2.2-SNAPSHOT");
        HtmlForm form = page.getFormByName("performGitflowRelease");
        form.submit((HtmlButton) last(form.getHtmlElementsByTagName("button")));

        //TODO find a better way to wait for the build success
        long lines = 0;
        while (mavenProject.getBuildByNumber(2).getResult() == null) {
            Thread.sleep(5000);
            lines = mavenProject.getLastBuild().getLogText().writeLogTo(lines, System.out);
        }
        mavenProject.getLastBuild().getLogText().writeLogTo(lines, System.out);
        assertThat("StartHotFixAction failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        //check the Git-Repro
        File repository = folder.newFolder();
        GitClient gitClient = Git.with(j.createTaskListener(), new EnvVars()).in(repository).getClient();
        gitClient.clone(gitRepro.getAbsolutePath(), "origin", false, null);
        Map<String, Branch> branches = getAllBranches(gitClient);

        assertThat(branches.keySet(), containsInAnyOrder("origin/hotfix/superHotfix", "origin/master", "origin/develop", "master"));

        gitClient.checkoutBranch("hotfix/superHotfix", branches.get("origin/hotfix/superHotfix").getSHA1String());
        checkMultiModulProject(repository, "1.2.2-SNAPSHOT", 4);
   }

    private Map<String, Branch> getAllBranches(GitClient gitClient) throws InterruptedException {
        Map<String, Branch> map = new HashMap<String, Branch>();
        for (Branch branch : gitClient.getBranches()) {
            map.put(branch.getName(), branch);
        }
        return map;
    }

    private void checkMultiModulProject(File baseDir, final String nextDevelopmentVersion, int pomCount) throws IOException, InterruptedException,
                                                                                                                        XmlPullParserException {
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        Iterator iterator = FileUtils.iterateFiles(baseDir, new String[] { "xml" }, true);
        int count = 0;
        while (iterator.hasNext()) {
            File next =  (File) iterator.next();
            if ("pom.xml".compareTo(next.getName())==0){
                count++;
                assertThat("Development Version was not set", mavenreader.read(new FileReader(next)).getVersion(), is(nextDevelopmentVersion));
            }
        }
        assertThat("not all Modules was checked",count, is(pomCount));
    }

    private File setUpGitRepo(final String path, File repro) throws IOException, URISyntaxException, InterruptedException {
        URL resource = getClass().getResource(path);
        FilePath filePath = new FilePath(new File(resource.toURI()));
        filePath.unzip(new FilePath(repro));

        GitSCM gitSCM = new GitSCM(repro.getAbsolutePath());
        mavenProject.setScm(gitSCM);

        return repro;
    }
}


