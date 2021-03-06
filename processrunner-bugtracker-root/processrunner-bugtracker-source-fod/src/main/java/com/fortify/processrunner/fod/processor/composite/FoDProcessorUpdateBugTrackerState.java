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
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fortify.processrunner.common.issue.IssueState;
import com.fortify.processrunner.common.processor.AbstractProcessorUpdateIssueStateForVulnerabilities;
import com.fortify.processrunner.context.Context;
import com.fortify.processrunner.context.ContextPropertyDefinitions;
import com.fortify.processrunner.fod.connection.FoDConnectionFactory;
import com.fortify.processrunner.fod.processor.enrich.FoDProcessorEnrichWithBugDataFromComment;
import com.fortify.processrunner.fod.processor.enrich.FoDProcessorEnrichWithExtraFoDData;
import com.fortify.processrunner.fod.processor.enrich.FoDProcessorEnrichWithVulnDeepLink;
import com.fortify.processrunner.fod.processor.enrich.FoDProcessorEnrichWithVulnState;
import com.fortify.processrunner.fod.processor.filter.FoDFilterOnBugSubmittedComment;
import com.fortify.processrunner.fod.processor.filter.FoDFilterOnBugSubmittedField;
import com.fortify.processrunner.fod.processor.filter.FoDFilterOnHasCommentsField;
import com.fortify.processrunner.fod.processor.retrieve.FoDProcessorRetrieveVulnerabilities;
import com.fortify.processrunner.processor.AbstractCompositeProcessor;
import com.fortify.processrunner.processor.CompositeProcessor;
import com.fortify.processrunner.processor.IProcessor;
import com.fortify.util.spring.SpringExpressionUtil;

/**
 * This {@link IProcessor} implementation allows for updating tracker state
 * based on FoD vulnerability state. It will retrieve all FoD vulnerabilities
 * (both open and closed) that have been previously submitted to the bug tracker,
 * group them by external bug link/id, and determine whether all vulnerabilities
 * in each group can be considered 'closed' and thus the corresponding bug
 * can be closed as well. 
 */
@Component
public class FoDProcessorUpdateBugTrackerState extends AbstractCompositeProcessor {
	private final FoDProcessorEnrichWithVulnState enrichWithVulnStateProcessor = new FoDProcessorEnrichWithVulnState(); 
	private Set<String> extraFields = new HashSet<String>();
	private boolean useFoDCommentForSubmittedBugs = false;
	
	private AbstractProcessorUpdateIssueStateForVulnerabilities<?> updateIssueStateProcessor;
	
	@Override
	protected void addCompositeContextPropertyDefinitions(ContextPropertyDefinitions contextPropertyDefinitions, Context context) {
		FoDConnectionFactory.addContextPropertyDefinitions(contextPropertyDefinitions, context);
	}
	
	@Override
	public List<IProcessor> getProcessors() {
		return Arrays.asList(createRootVulnerabilityArrayProcessor());
	}
	
	protected IProcessor createRootVulnerabilityArrayProcessor() {
		FoDProcessorRetrieveVulnerabilities result = new FoDProcessorRetrieveVulnerabilities(
			isUseFoDCommentForSubmittedBugs() ? createCommentBasedProcessors() : createBugLinkBasedProcessors(),
			createAddVulnDeepLinkProcessor(),
			createAddJSONDataProcessor(),
			getVulnState(),
			getUpdateIssueStateProcessor()
		);
		result.setIncludeRemoved(true);
		return result;
	}

	/**
	 * Create the processors for managing bug state based on the FoD bugLink field.
	 */
	protected IProcessor createBugLinkBasedProcessors() {
		return new CompositeProcessor(
				// Add a top-level field filter on the bugSubmitted field to include only vulnerabilities 
				// that have already been submitted to the bug tracker
				new FoDFilterOnBugSubmittedField("true")
				// TODO Add processor to add bugId field to vulnerability?
		);
	}
	
	/**
	 * Create the processor for managing bug state based on FoD comments.
	 * @return
	 */
	protected IProcessor createCommentBasedProcessors() {
		return new CompositeProcessor(
			// Add top-level field filter to include only vulnerabilities with comments
			// (to avoid loading summary data if there are no comments anyway)
			new FoDFilterOnHasCommentsField("true"),
			// Add processor to add FoD summary data as required by FoDFilterOnBugSubmittedComment
			new FoDProcessorEnrichWithExtraFoDData("summary"),
			// Add comment-based filter to include only vulnerabilities that have already been submitted 
			// to the bug tracker
			new FoDFilterOnBugSubmittedComment(false),
			// Add processor to add bugLink (and bugId?) fields from comment to vulnerability
			new FoDProcessorEnrichWithBugDataFromComment()
		);
	}
	
	protected FoDProcessorEnrichWithExtraFoDData createAddJSONDataProcessor() {
		FoDProcessorEnrichWithExtraFoDData result = new FoDProcessorEnrichWithExtraFoDData();
		result.setFields(getExtraFields());
		return result;
	}
	
	protected IProcessor createAddVulnDeepLinkProcessor() {
		return new FoDProcessorEnrichWithVulnDeepLink();
	}

	/**
	 * @return the useFoDCommentForSubmittedBugs
	 */
	public boolean isUseFoDCommentForSubmittedBugs() {
		return useFoDCommentForSubmittedBugs;
	}

	/**
	 * @param useFoDCommentForSubmittedBugs the useFoDCommentForSubmittedBugs to set
	 */
	public void setUseFoDCommentForSubmittedBugs(boolean useFoDCommentForSubmittedBugs) {
		this.useFoDCommentForSubmittedBugs = useFoDCommentForSubmittedBugs;
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
	
	public FoDProcessorEnrichWithVulnState getVulnState() {
		return enrichWithVulnStateProcessor;
	}

	public AbstractProcessorUpdateIssueStateForVulnerabilities<?> getUpdateIssueStateProcessor() {
		return updateIssueStateProcessor;
	}

	@Autowired(required=false) // non-required to avoid Spring autowiring failures if bug tracker implementation doesn't include bug state management
	public void setUpdateIssueStateProcessor(AbstractProcessorUpdateIssueStateForVulnerabilities<?> updateIssueStateProcessor) {
		updateIssueStateProcessor.setGroupTemplateExpression(SpringExpressionUtil.parseTemplateExpression("${bugLink}"));
		updateIssueStateProcessor.setIsVulnStateOpenExpression(SpringExpressionUtil.parseSimpleExpression(FoDProcessorEnrichWithVulnState.NAME_VULN_STATE+"=='"+IssueState.OPEN.name()+"'"));
		updateIssueStateProcessor.setVulnBugIdExpression(SpringExpressionUtil.parseSimpleExpression("bugId"));
		updateIssueStateProcessor.setVulnBugLinkExpression(SpringExpressionUtil.parseSimpleExpression("bugLink"));
		this.updateIssueStateProcessor = updateIssueStateProcessor;
	}
	
	@Autowired(required=false)
	public void setConfiguration(FoDBugTrackerProcessorConfiguration config) {
		setExtraFields(config.getExtraFields());
		setUseFoDCommentForSubmittedBugs(config.isUseFoDCommentForSubmittedBugs());
		getVulnState().setIsVulnerabilityOpenExpression(config.getIsVulnerabilityOpenExpression());
	}
}
