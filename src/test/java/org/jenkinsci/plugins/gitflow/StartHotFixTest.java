package org.jenkinsci.plugins.gitflow;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.last;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

import hudson.FilePath;
import hudson.maven.MavenModuleSet;
import hudson.model.Result;
import hudson.plugins.git.GitSCM;

/**
 * @author Hannes Osius, Silpion IT-Solutions GmbH
 */

public class StartHotFixTest {

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

        assertThat(nameElement.getText(), is("hotfix/"));
        //TODO check the value of the version Element
        //assertThat(versionElement.getText(), is("1.0.1-SNAPSHOT"));

        versionElement.setTextContent("1.0.2-SNAPSHOT");

        //submit the form
        HtmlForm form = page.getFormByName("performGitflowRelease");
        Page page1 = form.submit((HtmlButton) last(form.getHtmlElementsByTagName("button")));

        //TODO find a better way to wait for the build success
        long lines = 0;
        while (mavenProject.getBuildByNumber(2).getResult() == null) {
            Thread.sleep(5000);
            lines = mavenProject.getLastBuild().getLogText().writeLogTo(lines, System.out);
        }
        assertThat("StartHotFixAction failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));
        mavenProject.getLastBuild().getLogText().writeLogTo(lines, System.out);
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


