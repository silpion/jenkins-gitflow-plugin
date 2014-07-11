package org.jenkinsci.plugins.gitflow.cause;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import org.junit.Test;

import net.sf.json.JSONObject;

public class TestHotfixCauseTest {

    @Test
    public void testConstructor() throws Exception {
        JSONObject jsonObject = new JSONObject();

        JSONObject value = new JSONObject();

        value.element(TestHotfixCause.PARAM_HOTFIX_BRANCH, "hotfix/foobar");
        value.element(TestHotfixCause.PARAM_HOTFIX_RELEASE_VERSION, "1.1.1-SNAPSHOT");
        value.element(TestHotfixCause.PARAM_NEXT_HOTFIX_RELEASE_VERSION, "1.1.1");

        jsonObject.element(TestHotfixCause.PARAM_HOTFIX, value);

        TestHotfixCause cause = new TestHotfixCause(jsonObject, true);

        assertThat("HotFixName was not set", cause.getHotfixBranch(), is("hotfix/foobar"));
        assertThat("HotFix Release Version was not set", cause.getHotfixReleaseVersion(), is("1.1.1-SNAPSHOT"));
        assertThat("HotFix Next Release Version was not set", cause.getNextHotfixReleaseVersion(), is("1.1.1"));
        assertThat("dryRun was not set", cause.isDryRun(), is(true));
    }
}