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

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fortify.fod.connection.FoDAuthenticatingRestConnection;
import com.fortify.processrunner.common.issue.IIssueSubmittedListener;
import com.fortify.processrunner.common.issue.SubmittedIssue;
import com.fortify.processrunner.common.issue.SubmittedIssueCommentHelper;
import com.fortify.processrunner.common.processor.IProcessorSubmitIssueForVulnerabilities;
import com.fortify.processrunner.context.Context;
import com.fortify.processrunner.fod.connection.FoDConnectionFactory;
import com.fortify.processrunner.fod.context.IContextFoD;
import com.fortify.processrunner.fod.processor.enrich.FoDProcessorEnrichWithExtraFoDData;
import com.fortify.processrunner.fod.processor.enrich.FoDProcessorEnrichWithVulnState;
import com.fortify.processrunner.fod.processor.filter.FoDFilterOnBugSubmittedComment;
import com.fortify.processrunner.fod.processor.filter.FoDFilterOnBugSubmittedField;
import com.fortify.processrunner.processor.CompositeProcessor;
import com.fortify.processrunner.processor.IProcessor;
import com.fortify.util.spring.SpringExpressionUtil;

/**
 * This class extends {@link AbstractFoDProcessorRetrieveFilteredVulnerabilities} with the following
 * functionality:
 * <ul>
 *  <li>Add filters to exclude vulnerabilities already submitted to the bug tracker</li>
 *  <li>After submitting a group of vulnerabilities to the bug tracker, add a corresponding
 *      comment or bugLink field to the FoD vulnerabilities</li>
 * </ul>
 * Apart from the configuration as documented for {@link AbstractFoDProcessorRetrieveFilteredVulnerabilities},
 * this class can be configured with an additional {@link #useFoDCommentForSubmittedBugs} flag.
 * If set to false (default), a link to the submitted bug will be stored in the FoD bugLink field.
 * If set to true, information about the submitted bug will be stored as an FoD comment.
 */
@Component
public class FoDProcessorSubmitFilteredVulnerabilitiesToBugTracker extends AbstractFoDProcessorRetrieveFilteredVulnerabilities {
	private final FoDProcessorEnrichWithVulnState enrichWithVulnStateProcessor = new FoDProcessorEnrichWithVulnState(); 
	private boolean useFoDCommentForSubmittedBugs = false;
	private IProcessorSubmitIssueForVulnerabilities submitIssueProcessor;
	
	@Override
	protected CompositeProcessor createTopLevelFieldFilters() {
		CompositeProcessor result = super.createTopLevelFieldFilters();
		if ( !isUseFoDCommentForSubmittedBugs() && submitIssueProcessor.isIgnorePreviouslySubmittedIssues() ) {
			// If FoD comments are not used for submitted bugs, we add a top-level field filter on bugSubmitted=false
			result.getProcessors().add(new FoDFilterOnBugSubmittedField("false"));
		}
		return result;
	}
	
	@Override
	protected CompositeProcessor createSubLevelFieldFilters() {
		CompositeProcessor result = super.createSubLevelFieldFilters();
		if ( isUseFoDCommentForSubmittedBugs() && submitIssueProcessor.isIgnorePreviouslySubmittedIssues() ) {
			// If FoD comments are used for submitted bugs, we add a comment-based filter to exclude vulnerabilities
			// that have already been submitted to the bug tracker
			result.getProcessors().add(new FoDFilterOnBugSubmittedComment(true));
		}
		return result;
	}
	
	@Override
	protected FoDProcessorEnrichWithExtraFoDData createAddJSONDataProcessor() {
		FoDProcessorEnrichWithExtraFoDData result = super.createAddJSONDataProcessor();
		if ( isUseFoDCommentForSubmittedBugs() && submitIssueProcessor.isIgnorePreviouslySubmittedIssues() ) {
			// Add summary field if we used FoD comments for submitted vulnerabilities, 
			// since the FoDFilterOnBugSubmittedComment filter requires access to this field.
			result.getFields().add("summary");
		}
		return result;
	}
	
	@Override
	protected IProcessor getVulnerabilityProcessor() {
		return new CompositeProcessor(getVulnState(), getSubmitIssueProcessor());
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

	public IProcessorSubmitIssueForVulnerabilities getSubmitIssueProcessor() {
		return submitIssueProcessor;
	}

	@Autowired
	public void setSubmitIssueProcessor(IProcessorSubmitIssueForVulnerabilities submitIssueProcessor) {
		submitIssueProcessor.setIssueSubmittedListener(new FoDIssueSubmittedListener());
		this.submitIssueProcessor = submitIssueProcessor;
	}
	
	public FoDProcessorEnrichWithVulnState getVulnState() {
		return enrichWithVulnStateProcessor;
	}
	
	@Autowired(required=false)
	public void setConfiguration(FoDBugTrackerProcessorConfiguration config) {
		setAllFieldRegExFilters(config.getAllFieldRegExFilters());
		setExtraFields(config.getExtraFields());
		setTopLevelFieldRegExFilters(config.getTopLevelFieldRegExFilters());
		setTopLevelFieldSimpleFilters(config.getTopLevelFieldSimpleFilters());
		setUseFoDCommentForSubmittedBugs(config.isUseFoDCommentForSubmittedBugs());
		getVulnState().setIsVulnerabilityOpenExpression(config.getIsVulnerabilityOpenExpression());
	}

	private class FoDIssueSubmittedListener implements IIssueSubmittedListener {
		@SuppressWarnings("unchecked")
		public void issueSubmitted(Context context, String bugTrackerName, SubmittedIssue submittedIssue, Collection<Object> vulnerabilities) {
			IContextFoD ctx = context.as(IContextFoD.class);
			FoDAuthenticatingRestConnection conn = FoDConnectionFactory.getConnection(context);
			String releaseId = ctx.getFoDReleaseId();
			Collection<String> vulnIds = SpringExpressionUtil.evaluateExpression(vulnerabilities, "#root.![vulnId]", Collection.class);
			if ( isUseFoDCommentForSubmittedBugs() ) {
				String comment = SubmittedIssueCommentHelper.getCommentForSubmittedIssue(bugTrackerName, submittedIssue);
				conn.addCommentToVulnerabilities(releaseId, comment, vulnIds);
			} else {
				conn.addBugLinkToVulnerabilities(releaseId, submittedIssue.getDeepLink(), vulnIds);
			}
		}
	}
}
