package org.jenkinsci.plugins.gitflow.cause;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import net.sf.json.JSONObject;

public class FinishHotfixCauseTest {

    @Test
    public void testJsonConstructor() throws Exception {
        JSONObject jsonObject = new JSONObject();

        JSONObject value = new JSONObject();
        value.element(FinishHotfixCause.PARAM_HOTFIX_BRANCH, "hotfix/foobar");

        jsonObject.element(FinishHotfixCause.PARAM_HOTFIX, value);

        FinishHotfixCause cause = new FinishHotfixCause(jsonObject, true);

        assertThat("HotFixName was not set", cause.getHotfixBranche(), is("hotfix/foobar"));
        assertThat("dryRun was not set", cause.isDryRun(), is(true));
    }
}