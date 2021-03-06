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
package com.fortify.processrunner.fod.processor.composite;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.fortify.processrunner.context.Context;
import com.fortify.processrunner.context.ContextPropertyDefinitions;
import com.fortify.processrunner.filter.FilterRegEx;
import com.fortify.processrunner.fod.connection.FoDConnectionFactory;
import com.fortify.processrunner.fod.context.IContextFoD;
import com.fortify.processrunner.fod.processor.enrich.FoDProcessorEnrichWithExtraFoDData;
import com.fortify.processrunner.fod.processor.enrich.FoDProcessorEnrichWithVulnDeepLink;
import com.fortify.processrunner.fod.processor.filter.FoDFilterOnTopLevelField;
import com.fortify.processrunner.fod.processor.retrieve.FoDProcessorRetrieveVulnerabilities;
import com.fortify.processrunner.processor.AbstractCompositeProcessor;
import com.fortify.processrunner.processor.CompositeProcessor;
import com.fortify.processrunner.processor.IProcessor;

/**
 * <p>This composite {@link IProcessor} implementation combines various
 * {@link IProcessor} implementations for retrieving and filtering
 * FoD vulnerabilities, and processing each vulnerability using the 
 * {@link IProcessor} implementation returned by the 
 * {@link #createVulnerabilityProcessor()} that needs to be implemented
 * by subclasses.</p> 
 * 
 * <p>Various filters can be defined using the {@link #setTopLevelFieldRegExFilters(Map)},
 * {@link #setTopLevelFieldRegExFilters(Map)} and {@link #setAllFieldRegExFilters(Map)} 
 * methods.</p>
 * 
 * <p>The configured vulnerability processor can access the current
 * vulnerability using {@link IContextFoD#getFoDCurrentVulnerability()}.</p>
 */
public abstract class AbstractFoDProcessorRetrieveFilteredVulnerabilities extends AbstractCompositeProcessor {
	private Set<String> extraFields = new HashSet<String>();
	private Map<String,String> topLevelFieldSimpleFilters;
	private Map<String,Pattern> topLevelFieldRegExFilters;
	private Map<String,Pattern> allFieldRegExFilters;
	
	@Override
	protected void addCompositeContextPropertyDefinitions(ContextPropertyDefinitions contextPropertyDefinitions, Context context) {
		FoDConnectionFactory.addContextPropertyDefinitions(contextPropertyDefinitions, context);
	}
	
	@Override
	public List<IProcessor> getProcessors() {
		return Arrays.asList(createRootVulnerabilityArrayProcessor());
	}
	
	protected IProcessor createRootVulnerabilityArrayProcessor() {
		return new FoDProcessorRetrieveVulnerabilities(
			createTopLevelFieldFilters(),
			createAddVulnDeepLinkProcessor(),
			createAddJSONDataProcessor(),
			createSubLevelFieldFilters(),
			getVulnerabilityProcessor()
		);
	}
	
	protected CompositeProcessor createTopLevelFieldFilters() {
		return new CompositeProcessor(
			createTopLevelFieldSimpleFilter(),
			createTopLevelFieldRegExFilter()
		);
	}
	
	protected CompositeProcessor createSubLevelFieldFilters() {
		return new CompositeProcessor(
			createSubLevelFieldRegExFilter()
		);
	}
	
	protected abstract IProcessor getVulnerabilityProcessor();

	protected FoDProcessorEnrichWithExtraFoDData createAddJSONDataProcessor() {
		FoDProcessorEnrichWithExtraFoDData result = new FoDProcessorEnrichWithExtraFoDData();
		result.setFields(getExtraFields());
		return result;
	}
	
	protected IProcessor createAddVulnDeepLinkProcessor() {
		return new FoDProcessorEnrichWithVulnDeepLink();
	}

	protected IProcessor createSubLevelFieldRegExFilter() {
		return new FilterRegEx("CurrentVulnerability", getAllFieldRegExFilters());
	}

	protected IProcessor createTopLevelFieldRegExFilter() {
		return new FilterRegEx("CurrentVulnerability", getTopLevelFieldRegExFilters());
	}

	protected IProcessor createTopLevelFieldSimpleFilter() {
		CompositeProcessor result = new CompositeProcessor();
		for ( Map.Entry<String, String> entry : getTopLevelFieldSimpleFilters().entrySet() ) {
			result.getProcessors().add(new FoDFilterOnTopLevelField(entry.getKey(), entry.getValue()));
		}
		return result;
	}

	public Map<String, String> getTopLevelFieldSimpleFilters() {
		return topLevelFieldSimpleFilters;
	}

	public void setTopLevelFieldSimpleFilters(Map<String, String> topLevelFieldSimpleFilters) {
		this.topLevelFieldSimpleFilters = topLevelFieldSimpleFilters;
	}

	public Map<String, Pattern> getTopLevelFieldRegExFilters() {
		return topLevelFieldRegExFilters;
	}

	public void setTopLevelFieldRegExFilters(Map<String, Pattern> topLevelFieldRegExFilters) {
		this.topLevelFieldRegExFilters = topLevelFieldRegExFilters;
	}

	public Map<String, Pattern> getAllFieldRegExFilters() {
		return allFieldRegExFilters;
	}

	public void setAllFieldRegExFilters(Map<String, Pattern> allFieldRegExFilters) {
		this.allFieldRegExFilters = allFieldRegExFilters;
	}

	/**
	 * @return the extraFields
	 */
	public Set<String> getExtraFields() {
		return extraFields;
	}

	/**
	 * @param extraFields the extraFields to set
	 */
	public void setExtraFields(Set<String> extraFields) {
		this.extraFields = extraFields;
	}
}
