package org.sagebionetworks;

import static org.sagebionetworks.Constants.WES_ENDPOINT_PROPERTY_NAME;
import static org.sagebionetworks.Constants.WES_SHARED_DIR_PROPERTY_NAME;
import static org.sagebionetworks.Utils.checkHttpResponseCode;
import static org.sagebionetworks.Utils.getHttpClient;
import static org.sagebionetworks.Utils.getProperty;
import static org.sagebionetworks.Utils.getResponseBodyAsJson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;


public class WorkflowManagerWES implements WorkflowManager {

    private static String WES_ENDPOINT = getProperty(WES_ENDPOINT_PROPERTY_NAME);

    private static final String RUN_LOG = "run_log";
    private static final String STD_OUT = "stdout";
    private static final String STD_ERR = "stderr";
    private static final String NEXT_PAGE_TOKEN = "next_page_token";

    private static final List<String> PRE_TERMINAL_STATES = Arrays.asList(new String[] {
            "QUEUED",
            "INITIALIZING",
            "RUNNING",
            "PAUSED",
            "CANCELING",
            "UNKNOWN"
    });

    private static final List<String> TERMINAL_STATES = Arrays.asList(new String[] {
            "COMPLETE",
            "EXECUTOR_ERROR",
            "SYSTEM_ERROR",
            "CANCELED"
    });

    private WorkflowURLDownloader workflowURLDownloader;

    public WorkflowManagerWES(WorkflowURLDownloader downloader) {
        this.workflowURLDownloader = downloader;
    }

    private void addAllDirToHttpEntity(File dir, File rootDir, MultipartEntityBuilder requestBuilder) throws IOException {
        if (!dir.isDirectory()) throw new IllegalArgumentException(dir.getPath()+" must be a directory.");
        if (!rootDir.isDirectory()) throw new IllegalArgumentException(rootDir.getPath()+" must be a directory.");
        for (File child : dir.listFiles()) {
            if (child.isDirectory()) {
                addAllDirToHttpEntity(child, rootDir, requestBuilder);
            } else {
                if (!(child.getName().equals(".") || child.getName().equals(".."))) {
                    // Do this for each file
                    byte[] content = new byte[(int)child.length()];
                    try (FileInputStream input=new FileInputStream(child)) {
                        IOUtils.readFully(input, content);
                    }
                    if (!child.getPath().startsWith(rootDir.getPath()))
                        throw new IllegalArgumentException(child.getPath()+" must start with "+rootDir.getPath());
                    int startIndex = rootDir.getPath().length();
                    if (child.getPath().charAt(startIndex)==File.separatorChar) {
                        startIndex++;
                    }
                    String relativePath = child.getPath().substring(startIndex);
                    requestBuilder.addBinaryBody("workflow_attachment", content, ContentType.DEFAULT_BINARY, relativePath);
                }
            }
        }
    }
    @Override
    public WorkflowJob createWorkflowJob(String workflowUrlString, String entrypoint, WorkflowParameters workflowParameters,
            byte[] synapseConfigFileContent) throws IOException {
        MultipartEntityBuilder requestBuilder = MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

        File tempDir = Utils.getTempDir();
        File templateRoot = new File(tempDir, UUID.randomUUID().toString());
        if (!templateRoot.mkdir()) {
            throw new RuntimeException("Could not create "+templateRoot.getAbsolutePath());
        }

        workflowURLDownloader.downloadWorkflowFromURL(workflowUrlString, entrypoint, templateRoot);

        // NOTE: WES Service constrains entrypoint to be at the top level
        // TODO throw exception if any .cwl files are at higher level than entrypoint
        File entrypointFile = new File(entrypoint);
        File entrypointRoot = new File (templateRoot, entrypointFile.getParent());
        if (!entrypointRoot.exists()) {
            throw new RuntimeException(entrypointRoot.getAbsolutePath()+" does not exist.");
        }
        addAllDirToHttpEntity(entrypointRoot, entrypointRoot, requestBuilder);

        File synapseConfig = new File(getProperty(WES_SHARED_DIR_PROPERTY_NAME), ".synapseConfig");
        // write the synapse config file into the workflow folder
        // this is NOT secure but there's no good option today
        try (FileOutputStream fos=new FileOutputStream(synapseConfig)) {
            IOUtils.write(synapseConfigFileContent, fos);
        }

        // create JSON param's file and add it
        JSONObject params = new JSONObject();
        params.put("submissionId", Integer.parseInt(workflowParameters.getSubmissionId()));
        params.put("workflowSynapseId", workflowParameters.getSynapseWorkflowReference());
        params.put("submitterUploadSynId", workflowParameters.getSubmitterUploadSynId());
        params.put("adminUploadSynId", workflowParameters.getAdminUploadSynId());
        JSONObject config = new JSONObject();
        config.put("class", "File");
        config.put("path", synapseConfig.getAbsolutePath());
        params.put("synapseConfig", config);
        requestBuilder.addTextBody("workflow_params", params.toString());

        // workflow_url
        // "a relative URL corresponding to one of the files attached using `workflow_attachment`."
        requestBuilder.addTextBody("workflow_url", "file://"+entrypointFile.getName());

        HttpEntity requestBody = requestBuilder.build();
        String url = WES_ENDPOINT+"/runs";
        HttpPost request = new HttpPost(url);
        request.setEntity(requestBody);
        String id;
        try {
            HttpResponse response = getHttpClient().execute(request);
            checkHttpResponseCode(response, HttpStatus.SC_OK);
            JSONObject runId = getResponseBodyAsJson(response);
            EntityUtils.consume(response.getEntity());
            id = runId.getString("run_id");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        WorkflowJobWES result = new WorkflowJobWES();
        result.setWorkflowId(id);
        return result;

    }


    @Override
    public List<WorkflowJob> listWorkflowJobs(Boolean running) {
        try {
            List<WorkflowJob> result = new ArrayList<WorkflowJob>();

            String nextPage = null;
            do {
                String url = WES_ENDPOINT+"/runs";
                if (StringUtils.isNotEmpty(nextPage)) url +="?page_token="+nextPage;
                HttpGet request = new HttpGet(url);
                HttpResponse response = getHttpClient().execute(request);
                checkHttpResponseCode(response, HttpStatus.SC_OK);

                JSONObject json = getResponseBodyAsJson(response);
                EntityUtils.consume(response.getEntity());
                if (json.has(NEXT_PAGE_TOKEN)) {
                    nextPage = json.getString(NEXT_PAGE_TOKEN);
                } else {
                    nextPage = null;
                }
                if (json.has("workflows")) { // NOTE:  according to the spec' this should be "runs" not "workflows"
                    JSONArray runs = json.getJSONArray("workflows");
                    for (int i=0; i<runs.length(); i++) {
                        JSONObject run = runs.getJSONObject(i);
                        WorkflowJobWES job = new WorkflowJobWES();
                        job.setWorkflowId(run.getString("run_id"));
                        boolean jobIsRunning = PRE_TERMINAL_STATES.contains(run.getString("state"));
                        // if running==true only add running jobs; if running==false only add non-running jobs
                        if (running==null || (running == jobIsRunning)) result.add(job);
                    }
                }
            } while (StringUtils.isNotEmpty(nextPage));
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public WorkflowStatus getWorkflowStatus(WorkflowJob job) throws IOException {
        String url = WES_ENDPOINT+"/runs/"+job.getWorkflowId()+"/status";
        HttpGet request = new HttpGet(url);
        HttpResponse response = getHttpClient().execute(request);
        checkHttpResponseCode(response, HttpStatus.SC_OK);

        JSONObject runStatus = getResponseBodyAsJson(response);
        EntityUtils.consume(response.getEntity());
        // fields include run_id, state
        String state = runStatus.getString("state");

        WorkflowStatus result = new WorkflowStatus();
        boolean isRunning = false;
        ExitStatus exitStatus = null;
        if (TERMINAL_STATES.contains(state)) {
            isRunning = false;
            if (state.equals("COMPLETE")) {
                exitStatus=ExitStatus.SUCCESS;
            } else if (state.equals("CANCELED")) {
                exitStatus=ExitStatus.CANCELED;
            } else {
                exitStatus = ExitStatus.FAILURE;
            }
        } else {
            isRunning = true;
            exitStatus = null;
        }
        result.setExitStatus(exitStatus);
        result.setRunning(isRunning);

        return result;
    }

    @Override
    public String getWorkflowLog(WorkflowJob job, Path outPath, Integer maxTailLengthInCharacters) throws IOException {
        String url = WES_ENDPOINT+"/runs/"+job.getWorkflowId();
        HttpGet request = new HttpGet(url);
        HttpResponse response = getHttpClient().execute(request);
        checkHttpResponseCode(response, HttpStatus.SC_OK);

        JSONObject runLog = getResponseBodyAsJson(response);
        EntityUtils.consume(response.getEntity());

        if (!runLog.has(RUN_LOG)) {
            return "";
        }
        JSONObject log = runLog.getJSONObject(RUN_LOG);
        String stdOut = "";
        if (log.has(STD_OUT)) {
            stdOut = log.getString(STD_OUT);
        }
        String stdErr = "";
        if (log.has(STD_ERR)) {
            stdErr = log.getString(STD_ERR);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("STDOUT:\n");
        sb.append(stdOut);
        sb.append("STDERR:\n");
        sb.append(stdErr);
        String content = sb.toString();
        try ( OutputStream os = new FileOutputStream(outPath.toFile()) ) {
            IOUtils.write(content, os);
        }

        if (maxTailLengthInCharacters==null || maxTailLengthInCharacters<=0) {
            return null;
        }

        if (content.length()<=maxTailLengthInCharacters) {
            return content;
        } else {
            return content.substring(content.length()-maxTailLengthInCharacters);
        }
    }

    private void cancelWorkflowJob(WorkflowJob job) {
        String url = WES_ENDPOINT+"/runs/"+job.getWorkflowId()+"/cancel";
        HttpPost request = new HttpPost(url);
        try {
            HttpResponse response = getHttpClient().execute(request);
            checkHttpResponseCode(response, HttpStatus.SC_OK);
            EntityUtils.consume(response.getEntity());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stopWorkflowJob(WorkflowJob job) {
        cancelWorkflowJob(job);
    }

    @Override
    public void deleteWorkFlowJob(WorkflowJob job) {
        cancelWorkflowJob(job);
    }

}
