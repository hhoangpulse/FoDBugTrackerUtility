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
package com.fortify.processrunner.octane.connection;

import org.apache.commons.lang.StringUtils;

import com.fortify.processrunner.context.Context;
import com.fortify.processrunner.context.ContextPropertyDefinition;
import com.fortify.processrunner.context.ContextPropertyDefinitions;
import com.fortify.processrunner.octane.connection.OctaneAuthenticatingRestConnection.OctaneClientCredentials;
import com.fortify.processrunner.octane.connection.OctaneAuthenticatingRestConnection.OctaneUserCredentials;
import com.fortify.processrunner.octane.context.IContextOctane;
import com.fortify.processrunner.util.rest.ContextAwareProxyConfigurationFactory;
import com.fortify.util.rest.ProxyConfiguration;

public final class OctaneConnectionFactory 
{
	public static final void addContextPropertyDefinitions(ContextPropertyDefinitions contextPropertyDefinitions, Context context) {
		contextPropertyDefinitions.add(new ContextPropertyDefinition(IContextOctane.PRP_BASE_URL, "Octane base URL", true).readFromConsole(true));
		contextPropertyDefinitions.add(new ContextPropertyDefinition(IContextOctane.PRP_USER_NAME, "Octane user name (leave blank to use client credentials)", true).readFromConsole(true).ignoreIfPropertySet(IContextOctane.PRP_CLIENT_ID));
		contextPropertyDefinitions.add(new ContextPropertyDefinition(IContextOctane.PRP_PASSWORD, "Octane password", true).readFromConsole(true).isPassword(true).ignoreIfPropertyNotSet(IContextOctane.PRP_USER_NAME));
		contextPropertyDefinitions.add(new ContextPropertyDefinition(IContextOctane.PRP_CLIENT_ID, "Octane client id (leave blank to use user credentials)", true).readFromConsole(true).ignoreIfPropertySet(IContextOctane.PRP_USER_NAME));
		contextPropertyDefinitions.add(new ContextPropertyDefinition(IContextOctane.PRP_CLIENT_SECRET, "Octane client secret", true).readFromConsole(true).isPassword(true).ignoreIfPropertyNotSet(IContextOctane.PRP_CLIENT_ID));
		ContextAwareProxyConfigurationFactory.addContextPropertyDefinitions(contextPropertyDefinitions, context, "Octane");
	}
	
	public static final OctaneAuthenticatingRestConnection getConnection(Context context) {
		IContextOctaneConnection ctx = context.as(IContextOctaneConnection.class);
		OctaneAuthenticatingRestConnection result = ctx.getOctaneConnection();
		if ( result == null ) {
			result = createConnection(context);
			ctx.setOctaneConnection(result);
		}
		return result;
	}

	private static final OctaneAuthenticatingRestConnection createConnection(Context context) {
		IContextOctane ctx = context.as(IContextOctane.class);
		
		String baseUrl = ctx.getOctaneBaseUrl();
		ProxyConfiguration proxy = ContextAwareProxyConfigurationFactory.getProxyConfiguration(context, "Octane");
		if ( StringUtils.isNotBlank(ctx.getOctaneUserName()) ) {
			OctaneUserCredentials userCreds = new OctaneUserCredentials();
			userCreds.setUserName(ctx.getOctaneUserName());
			userCreds.setPassword(ctx.getOctanePassword());
			return new OctaneAuthenticatingRestConnection(baseUrl, userCreds, proxy);
		} else if ( StringUtils.isNotBlank(ctx.getOctaneClientId()) ) {
			OctaneClientCredentials clientCreds = new OctaneClientCredentials();
			clientCreds.setClientId(ctx.getOctaneClientId());
			clientCreds.setClientSecret(ctx.getOctaneClientSecret());
			return new OctaneAuthenticatingRestConnection(baseUrl, clientCreds, proxy);
		} else {
			throw new IllegalStateException("Either Octane user name and password, or client id and secret must be specified");
		}
	}
	
	private interface IContextOctaneConnection {
		public void setOctaneConnection(OctaneAuthenticatingRestConnection connection);
		public OctaneAuthenticatingRestConnection getOctaneConnection();
	}
}
