package org.jenkinsci.plugins.gitflow.cause;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import org.junit.Test;

import net.sf.json.JSONObject;

public class GitflowCauseFactoryTest {

    @Test
    public void testStartHotFix() throws Exception {

        JSONObject action = new JSONObject();
        action.element(StartHotFixCause.PARAM_HOTFIX_NAME, "testName");
        action.element(StartHotFixCause.PARAM_NEXT_HOTFIX_DEVELOPMENT_VERSION, "1.1-SNAPSHOT");

        JSONObject jsonObject = new JSONObject();

        jsonObject.element("dryRun", true);
        jsonObject.element("value", "startHotfix");
        jsonObject.element("action", action);

        AbstractGitflowCause cause = GitflowCauseFactory.newInstance(action);
        assertThat(cause, is(instanceOf(StartHotFixCause.class)));
    }
}