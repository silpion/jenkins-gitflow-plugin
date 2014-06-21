/*
 * The MIT License
 *
 * Copyright (c) 2010, Domi
 * Copyright (c) 2010, James Nord
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.gitflow;

import hudson.model.BuildBadgeAction;
import hudson.model.Result;
import hudson.model.Run;

import jenkins.model.RunAction2;

public class GitflowBadgeAction implements BuildBadgeAction, RunAction2 {

    private transient Run<?, ?> run;

    @Deprecated
    private transient String tooltipText; // kept for backwards compatibility (very old versions of plugin)

    private transient Boolean isDryRun; // kept for backwards compatibility

    /**
     * Version number that was released.
     */
    private transient String versionNumber; // kept for backwards compatibility

    private transient String gitflowAction;

    /**
     * Construct a new BadgeIcon to a Maven release build.
     */
    public GitflowBadgeAction() {
    }

    public Object readResolve() {
        // try to recover versionNumber from tooltipText (for builds by very old versions of the plugin)
        if (versionNumber == null && tooltipText != null && tooltipText.startsWith("Release - ")) {
            versionNumber = tooltipText.substring("Release - ".length());
        }
        return this;
    }

    /**
     * Gets the string to be displayed.
     *
     * @return <code>null</code> as we don't display any text to the user.
     */
    public String getDisplayName() {
        return null;
    }

    /**
     * Gets the file name of the icon.
     *
     * @return <code>null</code> as badges icons are rendered by the jelly.
     */
    public String getIconFileName() {
        return null;
    }

    /**
     * Gets the URL path name.
     *
     * @return <code>null</code> as this action object doesn't need to be bound to web.
     */
    public String getUrlName() {
        return null;
    }

    /**
     * Gets the tooltip text that should be displayed to the user.
     */
    public String getTooltipText() {
        StringBuilder str = new StringBuilder();
        str.append(this.getGitflowAction() + " ");
        if (isFailedBuild()) {
            str.append("Failed release ");
        }
        if (isDryRun()) {
            str.append(" (dryRun) ");
        }
        str.append(this.getVersionNumber());

        return str.toString();
    }

    /**
     * Gets the version number that was released.
     */
    public String getVersionNumber() {
        return this.versionNumber;
    }

    /**
     * Returns if the release was a dryRun or not.
     *
     * @return isDryRun returns true if the build is dry run
     */
    public boolean isDryRun() {
        return this.isDryRun;
    }

    /**
     * Returns <code>true</code> if the release build job failed.
     */
    public boolean isFailedBuild() {
        return !isSuccessfulBuild(this.run);
    }

    private boolean isSuccessfulBuild(Run<?, ?> run) {
        Result result = run.getResult();
        if (result != null) {
            return result.isBetterOrEqualTo(Result.SUCCESS);
        } else { // build true is not yet initiated
            return true;
        }
    }

    public void setVersionNumber(String versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getGitflowAction() {
        return this.gitflowAction;
    }

    public void onAttached(Run<?, ?> run) {
        this.run = run;
    }

    public void onLoad(Run<?, ?> run) {
        this.run = run;
    }

    public void setDryRun(boolean dryRun) {
        this.isDryRun = dryRun;
    }

    public void setGitflowAction(String gitflowAction) {
        this.gitflowAction = gitflowAction;
    }
}
