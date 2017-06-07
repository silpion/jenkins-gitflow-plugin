package de.silpion.jenkins.plugins.gitflow.it.action;

import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import de.silpion.jenkins.plugins.gitflow.data.GitflowPluginData;
import hudson.EnvVars;
import hudson.model.Result;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

/**
 * @author Hannes Osius, Silpion IT-Solutions GmbH - osius@silpion.de
 */
public class FinishHotfixIT extends AbstractGitflowActionIT {

    @Test
    public void testFinishHotfix() throws Exception {
        final Set<String> remoteBranches = this.testFinishHotfix(false);
        assertThat(remoteBranches, containsInAnyOrder("origin/master", "origin/develop"));
    }

    @Test
    public void testFinishHotfixDryRun() throws Exception {
        final Set<String> remoteBranches = this.testFinishHotfix(true);
        assertThat(remoteBranches, containsInAnyOrder("origin/master", "origin/develop", "origin/hotfix/1.0"));
    }

    private Set<String> testFinishHotfix(final boolean dryRun) throws Exception {

        File gitRepro = folder.newFolder("testrepo.git");
        this.setUpGitRepo("../testrepo.git_started-hotfix-1.0.zip", gitRepro);

        //make a build before
        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        GitflowPluginData data = mavenProject.getLastBuild().getAction(GitflowPluginData.class);
        addRemoteBranch(data, "hotfix/1.0", Result.SUCCESS, "1.0.2-SNAPSHOT", null, null);
        addRemoteBranch(data, "hotfix/foobar2", Result.SUCCESS, "1.1-SNAPSHOT", null, null);
        addRemoteBranch(data, "hotfix/foobar3", Result.SUCCESS, "1.1-SNAPSHOT", null, null);
        addRemoteBranch(data, "hotfix/foobar4", Result.SUCCESS, "1.1-SNAPSHOT", null, null);
        addRemoteBranch(data, "hotfix/foobar5", Result.SUCCESS, "1.1-SNAPSHOT", null, null);

        mavenProject.scheduleBuild2(0).get();
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        JenkinsRule.WebClient webClient = this.jenkinsRule.createWebClient();
        HtmlPage page = webClient.goTo(mavenProject.getUrl() + "gitflow");

        HtmlForm form = page.getFormByName("performGitflowRelease");
        checkRadioButton("action", "finishHotfix_1.0", page);
        ((HtmlCheckBoxInput) page.getElementsByName("dryRun").get(0)).setChecked(dryRun);

        this.jenkinsRule.submit(form);
        this.jenkinsRule.waitUntilNoActivity();
        mavenProject.getLastBuild().getLogText().writeLogTo(0, System.out);
        assertThat("TestBuild failed", mavenProject.getLastBuild().getResult(), is(Result.SUCCESS));

        //check the Git-Repro
        File repository = folder.newFolder();
        GitClient gitClient = Git.with(this.jenkinsRule.createTaskListener(), new EnvVars()).in(repository).getClient();
        gitClient.clone(gitRepro.getAbsolutePath(), "origin", false, null);

        return getRemoteBranches(gitClient).keySet();
    }

    private HtmlTableRow findParentTableRow(final DomNode htmlNode) {
        final DomNode parentNode = htmlNode.getParentNode();
        if (parentNode instanceof HtmlTableRow) {
            return (HtmlTableRow) parentNode;
        } else if (parentNode != null) {
            return this.findParentTableRow(parentNode);
        }
        return null;
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
}
