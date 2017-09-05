package de.silpion.jenkins.plugins.gitflow.it.action;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
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

/**
 * @author Hannes Osius, Silpion IT-Solutions GmbH  osius@silpion.de
 */
public class TestHotfixIT extends AbstractGitflowActionIT {

    @Test
    public void testTestHotfix() throws Exception {

        File gitRepro = folder.newFolder("testrepo.git");
        this.setUpGitRepo("../testrepo.git_started-hotfix-1.0.zip", gitRepro);

        //make a build before
        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        GitflowPluginData data = mavenProject.getLastBuild().getAction(GitflowPluginData.class);
        addRemoteBranch(data, "hotfix/1.0", Result.SUCCESS, "1.0.2-SNAPSHOT", null, null);

        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        JenkinsRule.WebClient webClient = this.jenkinsRule.createWebClient();
        HtmlPage page = webClient.goTo(mavenProject.getUrl() + "gitflow");

        HtmlForm form = page.getFormByName("performGitflowRelease");
        checkRadioButton("action", "testHotfix_1.0", page);

        this.jenkinsRule.submit(form);
        this.jenkinsRule.waitUntilNoActivity();
        mavenProject.getLastBuild().getLogText().writeLogTo(0, System.out);
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        GitflowPluginData data1 = mavenProject.getLastBuild().getAction(GitflowPluginData.class);
        assertThat(data1.getRemoteBranch("hotfix/1.0").getLastBuildVersion(), is("1.0.3-SNAPSHOT"));

        //check the Git-Repro
        File repository = folder.newFolder();
        GitClient gitClient = Git.with(this.jenkinsRule.createTaskListener(), new EnvVars()).in(repository).getClient();
        gitClient.clone(gitRepro.getAbsolutePath(), "origin", false, null);
        Map<String, Branch> branches = getAllBranches(gitClient);

        assertThat(branches.keySet(), containsInAnyOrder("origin/hotfix/1.0", "origin/master", "origin/develop"));

        gitClient.checkoutBranch("hotfix/foobar3", branches.get("origin/hotfix/1.0").getSHA1String());
        this.checkMultiModuleProject(repository, "1.0.3-SNAPSHOT", 4);
    }

    @Test
    public void testTestHotfixDryRun() throws Exception {

        File gitRepro = folder.newFolder("testrepo.git");
        this.setUpGitRepo("../testrepo.git_started-hotfix-1.0.zip", gitRepro);

        //make a build before
        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        GitflowPluginData data = mavenProject.getLastBuild().getAction(GitflowPluginData.class);
        addRemoteBranch(data, "hotfix/1.0", Result.SUCCESS, "1.0.2-SNAPSHOT", null, null);

        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        JenkinsRule.WebClient webClient = this.jenkinsRule.createWebClient();
        HtmlPage page = webClient.goTo(mavenProject.getUrl() + "gitflow");

        HtmlForm form = page.getFormByName("performGitflowRelease");
        checkRadioButton("action", "testHotfix_1.0", page);

        this.jenkinsRule.submit(form);
        this.jenkinsRule.waitUntilNoActivity();
        mavenProject.getLastBuild().getLogText().writeLogTo(0, System.out);
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        GitflowPluginData data1 = mavenProject.getLastBuild().getAction(GitflowPluginData.class);
        assertThat(data1.getRemoteBranch("hotfix/1.0").getLastBuildVersion(), is("1.0.3-SNAPSHOT"));
        assertThat(data1.getRemoteBranch("hotfix/1.0").getLastBuildResult(), is(Result.SUCCESS));

        //check the Git-Repro
        File repository = folder.newFolder();
        GitClient gitClient = Git.with(this.jenkinsRule.createTaskListener(), new EnvVars()).in(repository).getClient();
        gitClient.clone(gitRepro.getAbsolutePath(), "origin", false, null);
        Map<String, Branch> branches = getAllBranches(gitClient);

        assertThat(branches.keySet(), containsInAnyOrder("origin/hotfix/1.0", "origin/master", "origin/develop"));

        gitClient.checkoutBranch("hotfix/foobar3", branches.get("origin/hotfix/1.0").getSHA1String());
        checkMultiModuleProject(repository, "1.0.3-SNAPSHOT", 4);
    }

    @Test
    public void testTestHotfixFail() throws Exception {

        File gitRepro = folder.newFolder("testrepo.git");
        this.setUpGitRepo("../testrepo.git_started-hotfix-1.0.zip", gitRepro);

        //make a build before
        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        GitflowPluginData data = mavenProject.getLastBuild().getAction(GitflowPluginData.class);
        addRemoteBranch(data, "hotfix/foobar3", Result.SUCCESS, "1.1-SNAPSHOT", null, null);

        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        JenkinsRule.WebClient webClient = this.jenkinsRule.createWebClient();
        HtmlPage page = webClient.goTo(mavenProject.getUrl() + "gitflow");

        HtmlForm form = page.getFormByName("performGitflowRelease");
        checkRadioButton("action", "testHotfix_1.0", page);

        mavenProject.setGoals("fail");
        this.jenkinsRule.submit(form);
        this.jenkinsRule.waitUntilNoActivity();
        mavenProject.getLastBuild().getLogText().writeLogTo(0, System.out);
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.FAILURE));

        GitflowPluginData data1 = mavenProject.getLastBuild().getAction(GitflowPluginData.class);
        assertThat(data1.getRemoteBranch("hotfix/1.0").getLastBuildVersion(), is("1.0.2-SNAPSHOT"));
        assertThat(data1.getRemoteBranch("hotfix/1.0").getLastBuildResult(), is(Result.FAILURE));

        //check the Git-Repro
        File repository = folder.newFolder();
        GitClient gitClient = Git.with(this.jenkinsRule.createTaskListener(), new EnvVars()).in(repository).getClient();
        gitClient.clone(gitRepro.getAbsolutePath(), "origin", false, null);
        Map<String, Branch> branches = getAllBranches(gitClient);

        assertThat(branches.keySet(), containsInAnyOrder("origin/hotfix/1.0", "origin/master", "origin/develop"));

        gitClient.checkoutBranch("hotfix/foobar3", branches.get("origin/hotfix/1.0").getSHA1String());
        checkMultiModuleProject(repository, "1.0.2-SNAPSHOT", 4);
    }
}
