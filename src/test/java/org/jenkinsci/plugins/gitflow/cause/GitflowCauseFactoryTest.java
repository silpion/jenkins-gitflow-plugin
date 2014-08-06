package org.jenkinsci.plugins.gitflow.cause;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import org.junit.Test;

import net.sf.json.JSONObject;

public class GitflowCauseFactoryTest {

    @Test
    public void testStartHotfix() throws Exception {

        JSONObject action = new JSONObject();
        action.element(StartHotfixCause.PARAM_HOTFIX_RELEASE_VERSION, "testName");
        action.element(StartHotfixCause.PARAM_NEXT_HOTFIX_DEVELOPMENT_VERSION, "1.1-SNAPSHOT");
        action.element("value", "startHotfix");

        JSONObject jsonObject = new JSONObject();

        jsonObject.element("dryRun", true);
        jsonObject.element("action", action);

        AbstractGitflowCause cause = GitflowCauseFactory.newInstance(jsonObject);
        assertThat(cause, is(instanceOf(StartHotfixCause.class)));
    }
}
