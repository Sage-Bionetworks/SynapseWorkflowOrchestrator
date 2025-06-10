package org.sagebionetworks;

import static org.sagebionetworks.Constants.ROOT_TEMPLATE_ANNOTATION_NAME;
import static org.sagebionetworks.Constants.SYNAPSE_PAT_PROPERTY;
import static org.sagebionetworks.Constants.SYNAPSE_USERNAME_PROPERTY;
import static org.sagebionetworks.EvaluationUtils.JOB_LAST_UPDATED_TIME_STAMP;
import static org.sagebionetworks.EvaluationUtils.JOB_STARTED_TIME_STAMP;
import static org.sagebionetworks.EvaluationUtils.STATUS_DESCRIPTION;
import static org.sagebionetworks.Utils.getProperty;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.wiki.WikiPage;

public class WorkflowAdmin {

	// if 'TEAR_DOWN_AFTER' is set to false, then use unique names for projects and the evaluation:
	private static final String CHALLENGE_EVALUATION_NAME = "Workflow Queue for project ";

	private SynapseClient synapseAdmin;
	private Archiver archiver ;
	
	enum TASK {
		SET_UP,
		SUBMIT
	};

	// arguments: 
	// SET_UP template-file-path
	// SUBMIT file-path parentID, evaluation queue ID
	public static void main( String[] args ) throws Throwable {
		SynapseClient synapseAdmin = SynapseClientFactory.createSynapseClient();
		String pat = getProperty(SYNAPSE_PAT_PROPERTY);
		synapseAdmin.setBearerAuthorizationToken(pat);
		Archiver archiver = new Archiver(synapseAdmin, null);
		WorkflowAdmin workflowAdmin = new WorkflowAdmin(synapseAdmin, archiver);
		
		TASK task = TASK.valueOf(args[0]);
		switch(task) {
		case SET_UP:
			// Set Up
			if (args.length!=2) {
				throw new IllegalArgumentException("Expected two param's but found: "+Arrays.asList(args));
			}
			String workflowUrl = getProperty("WORKFLOW_TEMPLATE_URL", false);
			String rootTemplate = getProperty("ROOT_TEMPLATE", false);
			if (StringUtils.isNotEmpty(workflowUrl) && StringUtils.isNotEmpty(rootTemplate)) {
				String projectId = workflowAdmin.createProject();
				String fileEntityId = workflowAdmin.createExternalFileEntity(workflowUrl, projectId, rootTemplate);
				workflowAdmin.setUp(fileEntityId, projectId);
			} else {
				throw new IllegalArgumentException("invalid combination of env var's");
			}
			break;
		case SUBMIT:
			// Create Submission
			if (args.length!=3) throw new IllegalArgumentException("usage: SUBMIT <file path> <evaluation queue ID>");
			workflowAdmin.submit(args[1], args[2]);
			break;
		default:
			throw new IllegalArgumentException("Unexpected task: "+task);
		}

	}
	
	public SynapseClient getSynapseClient() {return synapseAdmin;}

	public WorkflowAdmin(SynapseClient synapseAdmin, Archiver archiver) {
		this.archiver = archiver;
		this.synapseAdmin = synapseAdmin;
	}

	public WorkflowAdmin() throws SynapseException {
		synapseAdmin = SynapseClientFactory.createSynapseClient();
		String userName = getProperty(SYNAPSE_USERNAME_PROPERTY);
		String pat = getProperty(SYNAPSE_PAT_PROPERTY);
		synapseAdmin.setBearerAuthorizationToken(pat);
		archiver = new Archiver(synapseAdmin, null);
	}

	public String createFileEntityForFile(String path, String parentId) throws Throwable {
		return archiver.uploadToSynapse(new File(path), parentId);
	}
	
	public String createExternalFileEntity(String externalURL, String parentId, String rootTemplate) throws Throwable {
		ExternalFileHandle efh = new ExternalFileHandle();
		efh.setExternalURL(externalURL);
		efh = synapseAdmin.createExternalFileHandle(efh);
		FileEntity fileEntity = new FileEntity();
		fileEntity.setDataFileHandleId(efh.getId());
		fileEntity.setParentId(parentId);
		fileEntity = synapseAdmin.createEntity(fileEntity);
		Annotations annotations = synapseAdmin.getAnnotationsV2(fileEntity.getId());
		Map<String, AnnotationsValue> updatedAnnotations = annotations.getAnnotations();
		AnnotationsValue value = new AnnotationsValue();
		value.setValue(Collections.singletonList(rootTemplate));
		value.setType(AnnotationsValueType.STRING);
		updatedAnnotations.put(ROOT_TEMPLATE_ANNOTATION_NAME, value);
		annotations.setAnnotations(updatedAnnotations);
		annotations = synapseAdmin.updateAnnotationsV2(fileEntity.getId(), annotations);
		return fileEntity.getId();
	}
	
	public String createProject() throws Exception {
		Project project;
		project = new Project();
		project = synapseAdmin.createEntity(project);
		System.out.println("Created "+project.getId());
		return project.getId();
	}
	
	private static String urlEncode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static final String DASHBOARD_TEMPLATE =
			"${leaderboard?queryTableResults=true&path=%2Fevaluation%2Fsubmission%2Fquery%3Fquery%3Dselect%2B%2A%2Bfrom%2Bevaluation%5F##EVALUATION_ID##&"+
	"paging=false&pageSize=100&showRowNumber=false&columnConfig0=none%2CSubmission ID%2CobjectId%3B%2CNONE&columnConfig1=cancelcontrol%2C%2CcancelControl%3B%2CNONE&columnConfig2=none%2CStatus%2Cstatus%3B%2CNONE&"+
	"columnConfig3=none%2CStatus Details%2C"+urlEncode(STATUS_DESCRIPTION)+"%3B%2CNONE&"+
	"columnConfig4=userid%2CUser%2CuserId%3B%2CNONE&"+
	"columnConfig5=userid%2CUser or Team%2CsubmitterId%3B%2CNONE&"+
	"columnConfig6=synapseid%2C%2CentityId%3B%2CNONE&"+
	"columnConfig7=epochdate%2CStarted%2C"+urlEncode(JOB_STARTED_TIME_STAMP)+"%3B%2CNONE&"+
	"columnConfig8=epochdate%2CUpdated On%2C"+urlEncode(JOB_LAST_UPDATED_TIME_STAMP)+"%3B%2CNONE&"+
	"columnConfig9=synapseid%2CWorkflow Output%2CworkflowOutputFile%3B%2CNONE}";
	
	private static final String EVALUATION_ID_PLACEHOLDER = "##EVALUATION_ID##";
	
	/**
	 * Create the Evaluation queue.
	 * Provide access to participants.
	 * Create a submission dashboard
	 * @throws UnsupportedEncodingException  
	 */
	public void setUp(String fileId, String projectId) throws Throwable {
		Evaluation evaluation;
		// first make sure the objects to be created don't already exist

	
		evaluation = new Evaluation();
		evaluation.setContentSource(projectId);
		evaluation.setName(CHALLENGE_EVALUATION_NAME+projectId);
		evaluation.setSubmissionReceiptMessage("Your workflow submission has been received.   Further notifications will be sent by email.");
		evaluation = synapseAdmin.createEvaluation(evaluation);
		JSONObject json = new JSONObject();
		json.put(evaluation.getId(), fileId);
		
		// create wiki with submission dashboard
		WikiPage dashboard = new WikiPage();
		String markdown = DASHBOARD_TEMPLATE.replace(EVALUATION_ID_PLACEHOLDER, evaluation.getId());
		dashboard.setMarkdown(markdown);
		synapseAdmin.createWikiPage(projectId, ObjectType.ENTITY, dashboard);
		
		System.out.println("EVALUATION_TEMPLATES: "+json);
	}

	/**
	 * Submit the file to the Evaluation
	 * @throws SynapseException
	 */
	public void submit(String filePath, String evaluationId) throws Throwable {
		Evaluation evaluation = synapseAdmin.getEvaluation(evaluationId);
		String parentId = evaluation.getContentSource();
		String fileId = archiver.uploadToSynapse(new File(filePath), parentId);
		FileEntity fileEntity = synapseAdmin.getEntity(fileId, FileEntity.class);
		Submission submission = new Submission();
		submission.setEntityId(fileId);
		submission.setEvaluationId(evaluationId);
		submission.setVersionNumber(fileEntity.getVersionNumber());
		synapseAdmin.createIndividualSubmission(submission, fileEntity.getEtag(), null, null);
		System.out.println("Submitted "+fileId+" to "+evaluationId);
	}
}
