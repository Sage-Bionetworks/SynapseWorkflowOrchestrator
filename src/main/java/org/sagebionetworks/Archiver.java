package org.sagebionetworks;
import static org.sagebionetworks.Utils.getProperty;
import static org.sagebionetworks.Utils.getSynIdProperty;
import static org.sagebionetworks.Utils.getTempDir;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.fuin.utils4j.Utils4J;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseServerException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.util.ModelConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Archiver {
	private SynapseClient synapse;
	private WorkflowManager workflowManager;

	private static Logger log = LoggerFactory.getLogger(Archiver.class);

	public static final int MAX_FILE_NAME_LENGTH = 100;

	public Archiver(SynapseClient synapse, WorkflowManager workflowManager) {
		this.synapse=synapse;
		this.workflowManager=workflowManager;
	}

	/*
	 * return false if already shared, true if newly shared
	 */
	public boolean shareEntity(String entityId, Map<String,Set<ACCESS_TYPE>> principalAndPermissions) throws SynapseException {
		try {
			// have we already shared the folder?
			synapse.getACL(entityId);
			return false;
		} catch (SynapseNotFoundException e) {
			AccessControlList acl = new AccessControlList();
			acl.setId(entityId);
			Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
			acl.setResourceAccess(ras);
			{
				ResourceAccess ra = new ResourceAccess();
				ras.add(ra);
				ra.setPrincipalId(Long.parseLong(synapse.getMyProfile().getOwnerId()));
				ra.setAccessType(ModelConstants.ENITY_ADMIN_ACCESS_PERMISSIONS);
			}
			for (String principalId : principalAndPermissions.keySet()) {
				ResourceAccess ra = new ResourceAccess();
				ras.add(ra);
				ra.setPrincipalId(Long.parseLong(principalId));
				ra.setAccessType(principalAndPermissions.get(principalId));
			}
			synapse.createACL(acl);
			return true;
		}
	}

	/**
	 * Upload a file to Synapse
	 * 
	 * @param file the file to upload
	 * @param parentId the ID of the folder or project to which the file will be uploaded
	 * @return the ID of the created entity
	 * @throws IOException
	 * @throws SynapseException
	 * @throws InterruptedException 
	 */
	public String uploadToSynapse(
			final File file, 
			String parentId) throws Throwable {
		//S3FileHandle uploadResult = synapse.multipartUpload(file, null, true, false);
		S3FileHandle uploadResult = synapse.multipartUpload(file, null, true, false);
		FileEntity fileEntity = new FileEntity();
		String fileName = file.getName();
		fileEntity.setName(fileName);
		fileEntity.setDataFileHandleId(uploadResult.getId());
		fileEntity.setParentId(parentId);
		try {
			fileEntity = synapse.createEntity(fileEntity);
		} catch (SynapseServerException e) {
			// We will arrive here if the file already exists, though the exception may also be due to another problem.
			// We try to query for the file.  If the exception was due to another problem, we will likely throw another
			// exception and terminate.
			String fileId = synapse.lookupChild(parentId, fileName);
			fileEntity = synapse.getEntity(fileId, FileEntity.class);
			fileEntity.setDataFileHandleId(uploadResult.getId());
			fileEntity = synapse.putEntity(fileEntity);
		}
		return fileEntity.getId();
	}

	/*
	 * zip the given path, using the given file name prefix.  
	 * @return the zipped file
	 */
	private File zip(Path pathToArchive, String zipFilePrefix) throws IOException {
		// make sure the file name is legal
		final File directoryOrFileToArchive = pathToArchive.toFile();
		String zipFileName = zipFilePrefix.replaceAll("[^a-zA-Z0-9-]", "_")+".zip";
		// instead of zipping to directoryOrFileToArchive.getParentFile() which may or may not
		// be on the same volume of directoryOrFileToArchive, we just zip to a known temp dir
		File zipFile=new File(getTempDir(), zipFileName);
		if (directoryOrFileToArchive.isDirectory()) {
			Utils4J.zipDir(directoryOrFileToArchive, null, zipFile);
		} else if (directoryOrFileToArchive.isFile()) {
			Utils4J.zipDir(directoryOrFileToArchive.getParentFile(), 
					new FileFilter() {
				public boolean accept(File pathname) {
					return pathname.equals(directoryOrFileToArchive);
				}},
					null, zipFile);
		} else {
			throw new RuntimeException(directoryOrFileToArchive.getAbsolutePath()+" is neither a directory nor a file");
		}
		return zipFile;
	}

	/**
	 * Zip a file or directory and upload to a given Synapse folder using a given 
	 * Synapse file name (prefix). If a file has already been uploaded under the
	 * target folder+file name, then upload as a *revision* of the Synapse file.
	 * If the zip file exceeded the submitter's daily OR round limit then throw an exception.
	 * 
	 * @param pathToArchive directory to archive
	 * @param zipFilePrefix name of zip file to create from directory
	 * @param submissionFolder submission folder in Synapse
	 * @return the ID of the created entity
	 * @throws IOException
	 * @throws SynapseException
	 * @throws InterruptedException 
	 */
	public String archiveLogsToSynapse(
			Path pathToArchive, 
			String zipFilePrefix, 
			Folder submissionFolder) throws Throwable {
		log.info("Will archive "+pathToArchive.toFile().getAbsolutePath());

		zipFilePrefix = Utils.trunc(zipFilePrefix, MAX_FILE_NAME_LENGTH-4);

		final File zipFile = zip(pathToArchive, zipFilePrefix);

		String fileEntityId = uploadToSynapse(zipFile, submissionFolder.getId());
		log.info("Archived "+pathToArchive.toFile().getAbsolutePath()+" to "+fileEntityId);

		zipFile.delete();
		return fileEntityId;
	}


	public Folder getOrCreateFolder(String name, String parentId) throws SynapseException {
		if (StringUtils.isEmpty(name)) throw new IllegalArgumentException("name is required.");
		if (StringUtils.isEmpty(parentId)) throw new IllegalArgumentException("parentId is required.");
		Folder folder = new Folder();
		folder.setParentId(parentId);
		folder.setName(name);
		try {
			folder = synapse.createEntity(folder);
		} catch (SynapseServerException cue) {
			// We will arrive here if the folder already exists, though the exception may also be due to another problem.
			// We try to query for the folder.  If the exception was due to another problem, we will likely throw another
			// exception and terminate.
			folder = getFolder(name, parentId);
			if (folder==null) throw new IllegalStateException("Can neither create nor find folder named "+name+" with parent "+parentId, cue);	
		}	    	
		return folder;
	}

	private Folder getFolder(String name, String parentId) throws SynapseException {
		if (StringUtils.isEmpty(name)) throw new IllegalArgumentException("name is required.");
		if (StringUtils.isEmpty(parentId)) throw new IllegalArgumentException("parentId is required.");
		try {
			String folderId = synapse.lookupChild(parentId, name);
			return synapse.getEntity(folderId, Folder.class);
		} catch (SynapseNotFoundException e) {
			return null;
		}
	}

	private static Set<ACCESS_TYPE> READ_WRITE;
	static {
		READ_WRITE = new HashSet<ACCESS_TYPE>();
		READ_WRITE.add(ACCESS_TYPE.READ);
		READ_WRITE.add(ACCESS_TYPE.CREATE);
		READ_WRITE.add(ACCESS_TYPE.UPDATE);
	}

	private static Set<ACCESS_TYPE> READ_DOWNLOAD;
	static {
		READ_DOWNLOAD = new HashSet<ACCESS_TYPE>();
		READ_DOWNLOAD.add(ACCESS_TYPE.READ);
		READ_DOWNLOAD.add(ACCESS_TYPE.DOWNLOAD);
	}

	public Folder getOrCreateSubmitterFolder(String submittingUserOrTeamId, boolean shareImmediately) throws SynapseException {
		String parentId = getSynIdProperty("WORKFLOW_OUTPUT_ROOT_ENTITY_ID");

		Map<String,Set<ACCESS_TYPE>> principalsAndPermissions = new HashMap<String,Set<ACCESS_TYPE>>();
		String folderName;
		if (shareImmediately) {
			folderName=submittingUserOrTeamId;
			principalsAndPermissions.put(submittingUserOrTeamId, READ_DOWNLOAD);
		} else {
			folderName=submittingUserOrTeamId+"_LOCKED";
			principalsAndPermissions.put(submittingUserOrTeamId, Collections.singleton(ACCESS_TYPE.READ));
			String dataUnlockSynapsePrincipalId=getProperty("DATA_UNLOCK_SYNAPSE_PRINCIPAL_ID", false);
			if (!StringUtils.isEmpty(dataUnlockSynapsePrincipalId)) {
				principalsAndPermissions.put(dataUnlockSynapsePrincipalId, READ_WRITE);
			}
		}

		
		Folder submitterFolder = getOrCreateFolder(folderName, parentId);
		shareEntity(submitterFolder.getId(), principalsAndPermissions);

		return submitterFolder;
	}

	public Folder getOrCreateSubmissionUploadFolder(String submissionId, String submittingUserOrTeamId) throws SynapseException {
		String shareImmediatelyString = getProperty("SHARE_RESULTS_IMMEDIATELY", false);
		boolean shareImmediately = StringUtils.isEmpty(shareImmediatelyString) ? true : new Boolean(shareImmediatelyString);
		return getOrCreateSubmissionUploadFolder( submissionId, submittingUserOrTeamId, shareImmediately);
	}
		
	public Folder getOrCreateSubmissionUploadFolder(String submissionId, String submittingUserOrTeamId, boolean sharedWithSubmitter) throws SynapseException {
		Folder submitterFolder = getOrCreateSubmitterFolder(submittingUserOrTeamId, sharedWithSubmitter);

		String name = submissionId;
		// Entity names may only contain: letters, numbers, spaces, underscores, hypens, periods, plus signs, and parentheses
		// This is slightly more restrictive, replacing all but letters, numbers, hyphens and periods
		String normalizedName = name.replaceAll("[^a-zA-Z0-9.-]", "_");

		Folder submissionFolder = getOrCreateFolder(normalizedName, submitterFolder.getId());
		return submissionFolder;
	}

	/*
	 * returns the tail of the uploaded logs
	 */
	public String uploadLogs(WorkflowJob workflowJob, 
			String submissionId,
			String submittingUserOrTeamId,
			String nameSuffix, // e.g., "_logs"
			Integer maxTailLengthInCharacters,
			Folder submissionFolder) throws Throwable {
		String filePrefix = submissionId+nameSuffix;
		filePrefix  = filePrefix.replaceAll("[^a-zA-Z0-9-]", "_");
		// get the logs from the container
		Path logFile = FileSystems.getDefault().getPath(getTempDir().getAbsolutePath(), filePrefix+".txt");
		String logTail=workflowManager.getWorkflowLog(workflowJob, logFile, maxTailLengthInCharacters);
		assert Files.exists(logFile);

		// if no output, just return
		if (Files.size(logFile)==0) {
			log.info("logFile "+logFile+" has no content.  Nothing to upload.");
			return null;
		}

		log.info("Found "+Files.size(logFile)+" bytes to log.");

		archiveLogsToSynapse(logFile, filePrefix, submissionFolder);

		logFile.toFile().delete();

		return logTail;
	}
}
