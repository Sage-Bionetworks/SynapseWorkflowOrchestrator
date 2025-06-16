package org.sagebionetworks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class WorkflowURLDownloader {

    private final String PRIMARY_DESCRIPTOR_TYPE = "PRIMARY_DESCRIPTOR";
    private final String SECONDARY_DESCRIPTOR_TYPE = "SECONDARY_DESCRIPTOR";
    private final String ZIP_SUFFIX = ".zip";
    private final String GA4GH_TRS_FILE_FRAGMENT = "/api/ga4gh/v2/tools";

    private URLFactory urlFactory;

    public WorkflowURLDownloader(URLFactory urlFactory) {
        this.urlFactory=urlFactory;
    }

    public WorkflowURLDownloader() {
        this.urlFactory=new URLFactoryImpl();
    }

    public void downloadWorkflowFromURL(String workflowUrlString, String entrypoint, File targetDir) throws IOException {
        URLInterface workflowUrl = urlFactory.createURL(workflowUrlString);
        String path = workflowUrl.getPath();
        if (path.toLowerCase().endsWith(ZIP_SUFFIX)) {
            Utils.downloadZip(workflowUrl, Utils.getTempDir(), targetDir);
            // root file should be relative to unzip location
            if (!(new File(targetDir,entrypoint)).exists()) {
                throw new IllegalStateException(entrypoint+" is not in the unzipped archive downloaded from "+workflowUrl);
            }
        } else if (path.contains(GA4GH_TRS_FILE_FRAGMENT)) {
            URLInterface filesUrl = urlFactory.createURL(workflowUrl.toString()+"/files");
            String filesContent = Utils.downloadWebDocument(filesUrl);
            JSONArray files = new JSONArray(filesContent);
            for (int i=0; i<files.length(); i++) {
                JSONObject file = files.getJSONObject(i);
                String fileType = file.getString("file_type");
                String filePath = file.getString("path");
                if (PRIMARY_DESCRIPTOR_TYPE.equals(fileType)) {
                    if (!filePath.equals(entrypoint)) throw new RuntimeException("Expected entryPoint "+entrypoint+" but found "+filePath);
                } else if (SECONDARY_DESCRIPTOR_TYPE.equals(fileType)) {
                    // OK
                } else {
                    throw new RuntimeException("Unexpected file_type "+fileType);
                }

                URLInterface descriptorUrl = urlFactory.createURL(workflowUrl.toString()+ "/descriptor/" +filePath);
                String descriptorContent = Utils.downloadWebDocument(descriptorUrl);
                JSONObject descriptor = new JSONObject(descriptorContent);
                File targetFile = new File(targetDir, filePath);
                targetFile.getParentFile().mkdirs();
                try (OutputStream os = new FileOutputStream(targetFile)) {
                    IOUtils.write(descriptor.getString("content"), os, StandardCharsets.UTF_8);
                }
            }

        } else {
            throw new RuntimeException("Expected template to be a zip archive or TRS files URL, but found "+path);
        }
    }

}
