package de.silpion.jenkins.plugins.gitflow.it.action;

import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import de.silpion.jenkins.plugins.gitflow.data.GitflowPluginData;
import hudson.EnvVars;
import hudson.model.Result;
import hudson.plugins.git.Branch;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

/**
 * Integration Test for the Gitflow action <i>Start Hotfix</i>.
 *
 * @author Hannes Osius, Silpion IT-Solutions GmbH
 */
public class StartHotfixIT extends AbstractGitflowActionIT {

    /**
     * Run the <i>Start Hotfix</i> Gitflow action via a webclient.
     *
     * @throws Exception
     */
    @Test
    public void testStartHotfix() throws Exception {

        File gitRepro = folder.newFolder("testrepo.git");
        setUpGitRepo("../testrepo.git_finished-release-1.0.zip", gitRepro);

        //make a build before
        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));
        GitflowPluginData data = mavenProject.getLastBuild().getAction(GitflowPluginData.class);

        addRemoteBranch(data, "master", Result.SUCCESS, "1.0.1", "1.0", "1.0.1");

        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        JenkinsRule.WebClient webClient = this.jenkinsRule.createWebClient();
        HtmlPage page = webClient.goTo(mavenProject.getUrl() + "gitflow");

        assertThat("more than on element found", page.getElementsByName("startHotfix_nextPatchDevelopmentVersion").size(), is(1));
        HtmlTextInput versionElement = (HtmlTextInput) page.getElementsByName("startHotfix_nextPatchDevelopmentVersion").get(0);

        assertThat("more than on element found", page.getElementsByName("dryRun").size(), is(1));
        HtmlCheckBoxInput dryRunElement = (HtmlCheckBoxInput) page.getElementsByName("dryRun").get(0);

        assertThat(versionElement.getText(), is("1.0.2-SNAPSHOT"));
        assertThat(dryRunElement.isChecked(), is(false));

        //set values and submit the form
        checkRadioButton("action", "startHotfix", page);
        versionElement.setAttribute("value","1.0.5-SNAPSHOT");
        this.jenkinsRule.submit(page.getFormByName("performGitflowRelease"));

        this.jenkinsRule.waitUntilNoActivity();
        assertThat("StartHotfixAction failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        //check the Git-Repro
        File repository = folder.newFolder();
        GitClient gitClient = Git.with(this.jenkinsRule.createTaskListener(), new EnvVars()).in(repository).getClient();
        gitClient.clone(gitRepro.getAbsolutePath(), "origin", false, null);
        Map<String, Branch> branches = getAllBranches(gitClient);

        assertThat(branches.keySet(), containsInAnyOrder("origin/master", "origin/develop", "origin/hotfix/1.0"));

        gitClient.checkoutBranch("hotfix/superHotfix", branches.get("origin/hotfix/1.0").getSHA1String());
        this.checkMultiModuleProject(repository, "1.0.5-SNAPSHOT", 4);
    }

    /**
     * Run the <i>Start Hotfix</i> Gitflow action in <i>dryRun</i> via a webclient.
     *
     * @throws Exception
     */
    @Test
    public void testStartHotfixDryRun() throws Exception {

        File gitRepro = folder.newFolder("testrepo.git");
        setUpGitRepo("../testrepo.git_finished-release-1.0.zip", gitRepro);

        //make a build before
        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));
        GitflowPluginData data = mavenProject.getLastBuild().getAction(GitflowPluginData.class);

        addRemoteBranch(data, "master", Result.SUCCESS, "1.0.1", "1.0", "1.0.1");

        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        JenkinsRule.WebClient webClient = this.jenkinsRule.createWebClient();
        HtmlPage page = webClient.goTo(mavenProject.getUrl() + "gitflow");
        HtmlTextInput versionElement = (HtmlTextInput) page.getElementsByName("startHotfix_nextPatchDevelopmentVersion").get(0);
        HtmlCheckBoxInput dryRunElement = (HtmlCheckBoxInput) page.getElementsByName("dryRun").get(0);

        //set values and submit the form
        checkRadioButton("action", "startHotfix", page);
        versionElement.setAttribute("value","1.0.5-SNAPSHOT");
        dryRunElement.setChecked(true);
        this.jenkinsRule.submit(page.getFormByName("performGitflowRelease"));

        this.jenkinsRule.waitUntilNoActivity();
        assertThat("StartHotfixAction failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        //check the Git-Repro
        File repository = folder.newFolder();
        GitClient gitClient = Git.with(this.jenkinsRule.createTaskListener(), new EnvVars()).in(repository).getClient();
        gitClient.clone(gitRepro.getAbsolutePath(), "origin", false, null);
        Map<String, Branch> branches = getAllBranches(gitClient);

        assertThat(branches.keySet(), containsInAnyOrder("origin/master", "origin/develop"));

        gitClient.checkoutBranch("hotfix/master", branches.get("origin/master").getSHA1String());
        checkMultiModuleProject(repository, "1.0.1", 4);
    }

    @Test
    public void testStartHotfixDisabled() throws Exception {

        File gitRepro = this.folder.newFolder("testrepo.git");
        this.setUpGitRepo("../testrepo.git_initial.zip", gitRepro);

        //make a build before
        this.mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", this.mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));
        GitflowPluginData data = this.mavenProject.getLastBuild().getAction(GitflowPluginData.class);

        addRemoteBranch(data, "master", Result.SUCCESS, "1.0-SNAPSHOT", null, null);

        this.mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", this.mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        JenkinsRule.WebClient webClient = this.jenkinsRule.createWebClient();
        HtmlPage page = webClient.goTo(this.mavenProject.getUrl() + "gitflow");
        assertEquals(0, page.getElementsByName("startHotfix_nextPatchDevelopmentVersion").size());
    }
}
