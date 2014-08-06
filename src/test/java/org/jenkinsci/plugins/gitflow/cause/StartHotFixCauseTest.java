package org.jenkinsci.plugins.gitflow.cause;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import org.junit.Test;

import net.sf.json.JSONObject;

public class StartHotFixCauseTest {

    @Test
    public void testJsonConstructor() throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.element(StartHotFixCause.PARAM_HOTFIX_RELEASE_VERSION, "testName");
        jsonObject.element(StartHotFixCause.PARAM_NEXT_HOTFIX_DEVELOPMENT_VERSION, "1.1-SNAPSHOT");

        StartHotFixCause cause = new StartHotFixCause(jsonObject, true);

        assertThat("HotFixName was not set", cause.getHotfixReleaseVersion(), is("testName"));
        assertThat("NextHotfixDevelopmentVersion was not set", cause.getNextHotfixDevelopmentVersion(), is("1.1-SNAPSHOT"));
        assertThat("dryRun was not set", cause.isDryRun(), is(true));
    }
}
