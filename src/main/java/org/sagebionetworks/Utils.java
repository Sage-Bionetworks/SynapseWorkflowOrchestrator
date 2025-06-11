package org.sagebionetworks;

import static org.sagebionetworks.Constants.COMPOSE_PROJECT_NAME_ENV_VAR;
import static org.sagebionetworks.Constants.SUBMITTER_NOTIFICATION_MASK_DEFAULT;
import static org.sagebionetworks.Constants.SUBMITTER_NOTIFICATION_MASK_PARAM_NAME;
import static org.sagebionetworks.Constants.SYNAPSE_PAT_PROPERTY;
import static org.sagebionetworks.Constants.SYNAPSE_USERNAME_PROPERTY;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

import com.github.dockerjava.api.model.Container;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.fuin.utils4j.Utils4J;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
	private static Logger log = LoggerFactory.getLogger(Utils.class);

	public static final String SYNAPSE_DOCKER_HOST = "docker.synapse.org";

	public static final long ONE_DAY_AS_MILLISEC = 24*3600*1000L;

	public static final String SEP = "."; // a string that's not contained in any token
	private static final String WORKFLOW_CONTAINER_PREFIX = "workflow_job";
	private static final String ARCHIVE_PREFIX = "archive";

	public static final String DATE_FORMAT = "yyyy-MM-dd.HH:mm:ss";
	public static final String DECIMAL_PATTERN = "##.####";

	private static final String PRIMARY_DESCRIPTOR_TYPE = "PRIMARY_DESCRIPTOR";
	private static final String SECONDARY_DESCRIPTOR_TYPE = "SECONDARY_DESCRIPTOR";
	
	private static final String ZIP_SUFFIX = ".zip";
	private static final String GA4GH_TRS_FILE_FRAGMENT = "/api/ga4gh/v2/tools";
	
	private static Properties properties = null;

	public static void initProperties() {
		if (properties!=null) return;
		properties = new Properties();
		InputStream is = null;
		try {
			is = Utils.class.getClassLoader().getResourceAsStream("global.properties");
			properties.load(is);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (is!=null) try {
				is.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public static void validateSynapseId(String id) {
		if (!Pattern.matches(SubmissionUtils.SYNID_REGEX, id.toLowerCase().trim()))
			throw new RuntimeException(id+" is not a Synapse ID.");
		
	}
	
	/*
	 * Docker Compose creates container and volume names by prepending the project name 
	 * followed by an underscore.  This recreates the construct
	 */
	public static String dockerComposeName(String name) {
		return getProperty(COMPOSE_PROJECT_NAME_ENV_VAR)+"_"+name;
	}

	public static String getSynIdProperty(String key) {
		String result = getProperty(key, true);
		validateSynapseId(result);
		return result;
	}

	public static String getProperty(String key) {
		return getProperty(key, true);
	}
	
	public static File createTempFile(String suffix, File parentFolder) throws IOException {
		return File.createTempFile("TMP", suffix, parentFolder);
	}
	
	public static File getTempDir() {
		return new File(System.getProperty("java.io.tmpdir"));
	}
	
	private static boolean missing(String s) {
		return StringUtils.isEmpty(s) || "null".equals(s);
	}

	public static String getProperty(String key, boolean required) {
		initProperties();
		{
			String commandlineOption = System.getProperty(key);
			if (!missing(commandlineOption)) return commandlineOption;
		}
		{
			String environmentVariable = System.getenv(key);
			if (!missing(environmentVariable)) return environmentVariable;
		}
		{
			String embeddedProperty = properties.getProperty(key);
			if (!missing(embeddedProperty)) return embeddedProperty;
		}
		if (required) throw new RuntimeException("Cannot find value for "+key);
		return null;
	}
	
	public static void deleteFolderContent(File folder) {
		File[] files = folder.listFiles();
		if (files==null) return;
		for (File file : files) {
			if (file.isDirectory()) deleteFolderContent(file);
			file.delete();
		}
	}

	public static boolean isDirectoryEmpty(File dir) {
		File[] files = dir.listFiles();
		return files==null || files.length==0;
	}

	public static String getDockerRepositoryNameFromEntityBundle(String s) {
		try {
			EntityBundle bundle = EntityFactory.createEntityFromJSONString(
					s, EntityBundle.class);
			return ((DockerRepository)bundle.getEntity()).getRepositoryName();
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

	public static String createContainerName() {
		StringBuilder sb = new StringBuilder(WORKFLOW_CONTAINER_PREFIX);
		sb.append(SEP);
		sb.append(UUID.randomUUID()); 
		return sb.toString();
	}

	public static Filter WORKFLOW_FILTER = new Filter() {

		@Override
		public boolean match(String s) {
			return s.startsWith(WORKFLOW_CONTAINER_PREFIX);
		}

	};

	/*
	 * This is the name to give containers on the server once they stop running.
	 */
	public static String archiveContainerName(String name) {
		return ARCHIVE_PREFIX+SEP+name;
	}

	public static List<WorkflowJob> findRunningWorkflowJobs(Map<String, Container> agentContainers) {
		List<WorkflowJob> result = new ArrayList<WorkflowJob>();
		for (String containerName: agentContainers.keySet()) {
			WorkflowJobDocker job = new WorkflowJobDocker();
			job.setContainerName(containerName);
			job.setContainer(agentContainers.get(containerName));
			result.add(job);
		}
		return result;
	}

	public static Properties readPropertiesFile(InputStream is) {
		Properties properties = new Properties();
		try {
			try {
				properties.load(is);
			} finally {
				is.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return properties;
	}

	public static long getTodayStart(long now) {
		return (now/ONE_DAY_AS_MILLISEC)*ONE_DAY_AS_MILLISEC;
	}

	public static String trunc(String s, int max) {
		return s.length()>max ? s.substring(0, max) : s;
	}

	/*
	 * return the numeric version of the first n characters of s,
	 * or null if s is empty or not numeric
	 */
	public static Double getProgressPercentFromString(String s, int n) {
		if (s==null) return null;
		s=s.replaceAll("STDOUT:", "");
		s=s.replaceAll("STDERR:", "");
		s=s.trim();
		if (s.length()>n) s = s.substring(0, n);
		try {
			double d = Double.parseDouble(s);
			if (d<0) return 0d;
			if (d>100) return 100d;
			return d;
		} catch (NumberFormatException e) {
			return null;			
		}
	}
	
	public static void writeSynapseConfigFile(OutputStream os) throws IOException {
		String username=getProperty(SYNAPSE_USERNAME_PROPERTY);
		String pat=getProperty(SYNAPSE_PAT_PROPERTY);;
		IOUtils.write("[authentication]\nusername="+username+"\nauthtoken="+pat+"\n", os, StandardCharsets.UTF_8);
	}
	
	public static boolean notificationEnabled(int mask) {
		String notificationEnabledString = getProperty(SUBMITTER_NOTIFICATION_MASK_PARAM_NAME, false);
		int notificationEnabled;
		if (StringUtils.isEmpty(notificationEnabledString)) {
			notificationEnabled = SUBMITTER_NOTIFICATION_MASK_DEFAULT;
		} else {
			notificationEnabled = Integer.parseInt(notificationEnabledString);
		}
		int result = notificationEnabled & mask;
		log.info("mask: "+mask+" notificationEnabled: "+notificationEnabled+" result: "+result);
		return result !=0;
	}
	
	public static JSONObject getResponseBodyAsJson(HttpResponse response) throws UnsupportedOperationException, IOException, JSONException {
		InputStream inputStream = response.getEntity().getContent();
		StringBuffer result = new StringBuffer();
		try {
			BufferedReader rd = new BufferedReader(
					new InputStreamReader(inputStream));

			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
		} finally {
			inputStream.close();
		}
		return new JSONObject(result.toString());
	}

	public static HttpClient getHttpClient() {
		return HttpClientBuilder.create().build();
	}

	public static void checkHttpResponseCode(HttpResponse response, int expected) {
		if (expected!=response.getStatusLine().getStatusCode()) 
			throw new RuntimeException("Expected "+expected+" but received "+
					response.getStatusLine().getStatusCode());		
	}

	public static void downloadZip(final URLInterface url, File tempDir, File target) throws IOException {
		File tempZipFile = createTempFile(".zip", tempDir);
		try {
			(new ExponentialBackoffRunner()).execute(new NoRefreshExecutableAdapter<Void,Void>() {
				@Override
				public Void execute(Void args) throws Throwable {
					try (InputStream is = url.openStream(); OutputStream os = new FileOutputStream(tempZipFile)) {
						IOUtils.copy(is, os);
					}
					return null;
				}}, null);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
		Utils4J.unzip(tempZipFile, target);
		tempZipFile.delete();
	}

	public static String downloadWebDocument(URLInterface url) throws IOException {
		String result;
		try (InputStream is = url.openStream(); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			IOUtils.copy(is, os);
			result = os.toString();
		}
		return result;
	}


}
