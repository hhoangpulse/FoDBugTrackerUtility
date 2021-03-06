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
package com.fortify.processrunner.fod.processrunner;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import com.fortify.processrunner.ProcessRunner;
import com.fortify.processrunner.common.processor.AbstractProcessorUpdateIssueStateForVulnerabilities;
import com.fortify.processrunner.common.processor.IProcessorSubmitIssueForVulnerabilities;
import com.fortify.processrunner.fod.processor.composite.FoDProcessorSubmitFilteredVulnerabilitiesToBugTracker;
import com.fortify.processrunner.fod.processor.composite.FoDProcessorUpdateBugTrackerState;
import com.fortify.processrunner.processor.IProcessor;

public class FoDBugTrackerProcessRunner extends ProcessRunner {
	private final ProcessRunnerType type;
	
	public enum ProcessRunnerType {
		SUBMIT, UPDATE, SUBMIT_AND_UPDATE
	}
	
	public FoDBugTrackerProcessRunner(ProcessRunnerType type) {
		this.type = type;
	}
	
	private FoDProcessorSubmitFilteredVulnerabilitiesToBugTracker submitVulnerabilitiesProcessor;
	private FoDProcessorUpdateBugTrackerState updateBugTrackerStateProcessor;
	
	@PostConstruct
	public void postConstruct() {
		switch (type) {
		case SUBMIT:
			super.setProcessors(getSubmitVulnerabilitiesProcessor());
			super.setEnabled(isSubmitVulnerabilitiesProcessorEnabled());
			super.setDescription("Submit FoD vulnerabilities to "+getBugTrackerName());
			super.setDefault(isSubmitVulnerabilitiesProcessorEnabled() && !isUpdateBugTrackerStateProcessorEnabled());
			break;
		case SUBMIT_AND_UPDATE:
			super.setProcessors(getUpdateBugTrackerStateProcessor(), getSubmitVulnerabilitiesProcessor());
			super.setEnabled(isSubmitVulnerabilitiesProcessorEnabled() && isUpdateBugTrackerStateProcessorEnabled());
			super.setDescription("Submit FoD vulnerabilities to "+getBugTrackerName()+"and update issue state");
			super.setDefault(isSubmitVulnerabilitiesProcessorEnabled() && isUpdateBugTrackerStateProcessorEnabled());
			break;
		case UPDATE:
			super.setProcessors(getUpdateBugTrackerStateProcessor());
			super.setEnabled(isUpdateBugTrackerStateProcessorEnabled());
			super.setDescription("Update "+getBugTrackerName()+" issue state based on FoD vulnerability state");
			super.setDefault(!isSubmitVulnerabilitiesProcessorEnabled() && isUpdateBugTrackerStateProcessorEnabled());
			break;
		}
	}
	

	@Autowired
	public final void setProcessors(FoDProcessorSubmitFilteredVulnerabilitiesToBugTracker submitVulnerabilitiesProcessor, FoDProcessorUpdateBugTrackerState updateBugTrackerStateProcessor) {
		this.submitVulnerabilitiesProcessor = submitVulnerabilitiesProcessor;
		this.updateBugTrackerStateProcessor = updateBugTrackerStateProcessor;
		
		List<IProcessor> processors = new ArrayList<IProcessor>();
		String description = "";
		if ( submitVulnerabilitiesProcessor.getSubmitIssueProcessor()!=null ) {
			processors.add(submitVulnerabilitiesProcessor);
			description += "Submit vulnerabilities from FoD to "+submitVulnerabilitiesProcessor.getSubmitIssueProcessor().getBugTrackerName();
		}
		if ( updateBugTrackerStateProcessor.getUpdateIssueStateProcessor()!=null ) {
			processors.add(updateBugTrackerStateProcessor);
			description += "\nand update "+submitVulnerabilitiesProcessor.getSubmitIssueProcessor().getBugTrackerName()+" issue state";
		}
		super.setProcessors(processors.toArray(new IProcessor[]{}));
		super.setDescription(description);
	}
	
	protected final FoDProcessorSubmitFilteredVulnerabilitiesToBugTracker getSubmitVulnerabilitiesProcessor() {
		return submitVulnerabilitiesProcessor;
	}
	
	protected final FoDProcessorUpdateBugTrackerState getUpdateBugTrackerStateProcessor() {
		return updateBugTrackerStateProcessor;
	}
	
	protected final boolean isSubmitVulnerabilitiesProcessorEnabled() {
		return getSubmitIssueProcessor()!=null;
	}

	protected IProcessorSubmitIssueForVulnerabilities getSubmitIssueProcessor() {
		return getSubmitVulnerabilitiesProcessor().getSubmitIssueProcessor();
	}
	
	protected final boolean isUpdateBugTrackerStateProcessorEnabled() {
		return getUpdateIssueStateProcessor()!=null;
	}

	protected AbstractProcessorUpdateIssueStateForVulnerabilities<?> getUpdateIssueStateProcessor() {
		return getUpdateBugTrackerStateProcessor().getUpdateIssueStateProcessor();
	}
	
	protected final String getBugTrackerName() {
		if ( getSubmitIssueProcessor()!=null ) {
			return getSubmitIssueProcessor().getBugTrackerName();
		}
		if ( getUpdateIssueStateProcessor()!=null ) {
			return getUpdateIssueStateProcessor().getBugTrackerName();
		}
		throw new IllegalStateException("Cannot determine bug tracker name");
	}
}
