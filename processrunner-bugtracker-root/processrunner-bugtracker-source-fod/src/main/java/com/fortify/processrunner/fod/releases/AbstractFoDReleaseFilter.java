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
package com.fortify.processrunner.fod.releases;

import com.fortify.processrunner.context.Context;
import com.fortify.util.json.JSONMap;

/**
 * This abstract base class allows for either including or excluding FoD releases
 * based on the configured {@link #includeMatched} property. Concrete implementations need to
 * implement the {@link #isApplicationVersionMatching(Context, String, JSONMap)} method
 * to actually match the FoD release against some implementation-dependent criteria. 
 * 
 * @author Ruud Senden
 *
 */
public abstract class AbstractFoDReleaseFilter implements IFoDReleaseFilter {
	private boolean includeMatched = true;
	
	public final boolean isReleaseIncluded(Context context, JSONMap release) {
		return isReleaseMatching(context, release.get("releaseId", String.class), release) == isIncludeMatched();
	}
	
	public abstract boolean isReleaseMatching(Context context, String releaseId, JSONMap release);

	public boolean isIncludeMatched() {
		return includeMatched;
	}

	public void setIncludeMatched(boolean includeMatched) {
		this.includeMatched = includeMatched;
	}

}
