/*******************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development LP
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the Software"),
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.processrunner.ssc.appversion;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fortify.processrunner.context.Context;
import com.fortify.processrunner.ssc.connection.SSCConnectionFactory;
import com.fortify.util.json.JSONMap;

/**
 * Filter SSC application versions based on the SSC bug tracker plugin name configured
 * for each application version.
 * 
 * @author Ruud Senden
 *
 */
public class SSCApplicationVersionBugTrackerNameFilter extends AbstractSSCApplicationVersionFilter {
	private Set<String> bugTrackerPluginNames = null;
	
	public SSCApplicationVersionBugTrackerNameFilter() {}
	
	public SSCApplicationVersionBugTrackerNameFilter(String... bugTrackerPluginNames) {
		this.bugTrackerPluginNames = bugTrackerPluginNames==null ? null : new HashSet<String>(Arrays.asList(bugTrackerPluginNames));
	}
	
	public int getOrder() {
		return 1;
	}

	@Override
	public boolean isApplicationVersionMatching(Context context, String applicationVersionId, JSONMap applicationVersion) {
		Set<String> pluginIds = getBugTrackerPluginIdsForNames(context, getBugTrackerPluginNames());
		return pluginIds!=null && pluginIds.contains(applicationVersion.get("bugTrackerPluginId", String.class));
	}

	private Set<String> getBugTrackerPluginIdsForNames(Context context, Set<String> bugTrackerPluginNames) {
		return bugTrackerPluginNames==null ? null : SSCConnectionFactory.getConnection(context).getBugTrackerPluginIdsForNames(bugTrackerPluginNames);
	}

	public Set<String> getBugTrackerPluginNames() {
		return bugTrackerPluginNames;
	}

	public void setBugTrackerPluginNames(Set<String> bugTrackerPluginNames) {
		this.bugTrackerPluginNames = bugTrackerPluginNames;
	}
}
