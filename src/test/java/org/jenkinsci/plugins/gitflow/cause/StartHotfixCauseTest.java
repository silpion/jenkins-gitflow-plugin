package org.jenkinsci.plugins.gitflow.cause;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import org.junit.Test;

import net.sf.json.JSONObject;

public class StartHotfixCauseTest {

    @Test
    public void testJsonConstructor() throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.element(StartHotfixCause.PARAM_HOTFIX_RELEASE_VERSION, "testName");
        jsonObject.element(StartHotfixCause.PARAM_NEXT_HOTFIX_DEVELOPMENT_VERSION, "1.1-SNAPSHOT");

        StartHotfixCause cause = new StartHotfixCause(jsonObject, true);

        assertThat("HotfixName was not set", cause.getHotfixReleaseVersion(), is("testName"));
        assertThat("NextHotfixDevelopmentVersion was not set", cause.getNextHotfixDevelopmentVersion(), is("1.1-SNAPSHOT"));
        assertThat("dryRun was not set", cause.isDryRun(), is(true));
    }
}
