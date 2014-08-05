package org.jenkinsci.plugins.gitflow;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import org.junit.Test;

public class GitflowProjectActionTest {

    @Test
    public void testComputeNextHotfixVersion() throws Exception {

        assertThat(GitflowProjectAction.computeNextHotfixVersion(null), is(GitflowProjectAction.DEFAULT_STRING));
        assertThat(GitflowProjectAction.computeNextHotfixVersion(""), is(GitflowProjectAction.DEFAULT_STRING));
        assertThat(GitflowProjectAction.computeNextHotfixVersion("  "), is(GitflowProjectAction.DEFAULT_STRING));

        assertThat(GitflowProjectAction.computeNextHotfixVersion("1.0"), is("1.0.1-SNAPSHOT"));
        assertThat(GitflowProjectAction.computeNextHotfixVersion("1.5"), is("1.5.1-SNAPSHOT"));

        assertThat(GitflowProjectAction.computeNextHotfixVersion("2.18.39"), is("2.18.40-SNAPSHOT"));
        assertThat(GitflowProjectAction.computeNextHotfixVersion("2.9.99"), is("2.9.100-SNAPSHOT"));

    }
}