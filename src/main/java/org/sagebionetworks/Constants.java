package org.sagebionetworks;

import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseConflictingUpdateException;
import org.sagebionetworks.client.exceptions.SynapseDeprecatedServiceException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseResultNotReadyException;
import org.sagebionetworks.client.exceptions.SynapseServerException;
import org.sagebionetworks.client.exceptions.SynapseTermsOfUseException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;

public class Constants {
	
	public static final String SYNAPSE_USERNAME_PROPERTY = "SYNAPSE_USERNAME";
	
	public static final String SYNAPSE_PASSWORD_PROPERTY = "SYNAPSE_PASSWORD";
	
	// This the Synapse ID of the user or team to whom administrative notifications are sent
	public static final String NOTIFICATION_PRINCIPAL_ID = "NOTIFICATION_PRINCIPAL_ID";

	// property names appear as "-e" options in the "docker run" command that launches this agent
	// these folders are the paths on the host
	
	public static final String TOIL_CLI_OPTIONS_PROPERTY_NAME = "TOIL_CLI_OPTIONS";
	
	public static final String WORKFLOW_ENGINE_DOCKER_IMAGES_PROPERTY_NAME = "WORKFLOW_ENGINE_DOCKER_IMAGE";

	// The Docker name of the mounted volume for sharing data files between containers
	public static final String SHARED_VOLUME_NAME = "shared";
	
	
	// this is the environment variable docker compose uses for the project name
	public static final String COMPOSE_PROJECT_NAME_ENV_VAR = "COMPOSE_PROJECT_NAME";
	
	// the mount point of the shared volume withing the container
	public static final String AGENT_SHARED_DIR_PROPERTY_NAME = "AGENT_SHARED_DIR_PROPERTY";
	public static final String AGENT_SHARED_DIR_DEFAULT = "/shared";
	
	static {
		System.setProperty(AGENT_SHARED_DIR_PROPERTY_NAME, AGENT_SHARED_DIR_DEFAULT);
	}
	
	// the name of the system property containing the path in which to write a file
	// that WES will be able to access.  Note, this means that WES runs on the same file
	// system as the Orchestrator.
	public static final String WES_SHARED_DIR_PROPERTY_NAME = "WES_SHARED_DIR_PROPERTY";
	
	public static final String ROOT_TEMPLATE_ANNOTATION_NAME="ROOT_TEMPLATE";
	
	public static final String MAX_CONCURRENT_WORKFLOWS_PROPERTY_NAME = "MAX_CONCURRENT_WORKFLOWS";
	public static final int DEFAULT_MAX_CONCURRENT_WORKFLOWS = 10;
	
	public static final String RUN_WORKFLOW_CONTAINER_IN_PRIVILEGED_MODE_PROPERTY_NAME = "RUN_WORKFLOW_CONTAINER_IN_PRIVILEGED_MODE";

	/*
	 * If true then rather than removing containers when they finish running, just
	 * archive them for later reference.
	 */
	public static final boolean ARCHIVE_CONTAINER = false;

	public static final int MAX_LOG_ANNOTATION_CHARS = 256; // maximum log tail length to insert in annotation

	public static final long MEGABYTE_IN_BYTES = 1<<20;
	public static final long GIGABYTE_IN_BYTES = 1<<30;

	public static final String[] DUMP_PROGRESS_SHELL_COMMAND = {"cat","/progress.txt"};
	public static final int NUMBER_OF_PROGRESS_CHARACTERS = 5;
	
	public static final String EXECUTION_STAGE_PROPERTY_NAME = "EXECUTION_STAGE";
	
	public static final List<Class<? extends SynapseServerException>> NO_RETRY_EXCEPTIONS = Arrays.asList(
			SynapseResultNotReadyException.class,
			SynapseNotFoundException.class,
			SynapseBadRequestException.class,
			SynapseConflictingUpdateException.class,
			SynapseDeprecatedServiceException.class,
			SynapseForbiddenException.class, 
			SynapseTermsOfUseException.class,
			SynapseUnauthorizedException.class
			); 
	
	public static final Integer[] NO_RETRY_STATUSES = new Integer[] {409};
	
	public static int DEFAULT_NUM_RETRY_ATTEMPTS = 8; // 63 sec
	
	public static final String DOCKER_ENGINE_URL_PROPERTY_NAME = "DOCKER_ENGINE_URL";
	
	public static final String DOCKER_CERT_PATH_PROPERTY_NAME = "DOCKER_CERT_PATH";
	
	public static final String DOCKER_CERT_PATH_HOST_PROPERTY_NAME = "DOCKER_CERT_PATH_HOST";
	
	public static final String UNIX_SOCKET_PREFIX = "unix://";

	public static final String WES_ENDPOINT_PROPERTY_NAME = "WES_ENDPOINT";
	
	public static final int SUBMISSION_STARTED = 1;
	public static final int SUBMISSION_COMPLETED = 2;
	public static final int SUBMISSION_FAILED = 4;
	public static final int SUBMISSION_STOPPED_BY_USER = 8;
	public static final int SUBMISSION_TIMED_OUT = 16;
	
	public static final String SUBMITTER_NOTIFICATION_MASK_PARAM_NAME = "SUBMITTER_NOTIFICATION_MASK";
	
	public static final int SUBMITTER_NOTIFICATION_MASK_DEFAULT = 31;

	public static final String ACCEPT_NEW_SUBMISSIONS_PROPERTY_NAME = "ACCEPT_NEW_SUBMISSIONS";
	

}
