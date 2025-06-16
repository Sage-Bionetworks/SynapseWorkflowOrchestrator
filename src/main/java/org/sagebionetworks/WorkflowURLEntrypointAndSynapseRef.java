package org.sagebionetworks;

import java.net.URL;

public class WorkflowURLEntrypointAndSynapseRef {
    private URL workflowUrl;
    private String entryPoint;
    private String synapseId;
    public WorkflowURLEntrypointAndSynapseRef(URL workflowUrl, String entryPoint, String synapseId) {
        super();
        this.workflowUrl = workflowUrl;
        this.entryPoint = entryPoint;
        this.synapseId = synapseId;
    }
    public URL getWorkflowUrl() {
        return workflowUrl;
    }
    public void setWorkflowUrl(URL workflowUrl) {
        this.workflowUrl = workflowUrl;
    }
    public String getEntryPoint() {
        return entryPoint;
    }
    public void setEntryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }
    public String getSynapseId() {
        return synapseId;
    }
    public void setSynapseId(String synapseId) {
        this.synapseId = synapseId;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((entryPoint == null) ? 0 : entryPoint.hashCode());
        result = prime * result + ((synapseId == null) ? 0 : synapseId.hashCode());
        result = prime * result + ((workflowUrl == null) ? 0 : workflowUrl.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WorkflowURLEntrypointAndSynapseRef other = (WorkflowURLEntrypointAndSynapseRef) obj;
        if (entryPoint == null) {
            if (other.entryPoint != null)
                return false;
        } else if (!entryPoint.equals(other.entryPoint))
            return false;
        if (synapseId == null) {
            if (other.synapseId != null)
                return false;
        } else if (!synapseId.equals(other.synapseId))
            return false;
        if (workflowUrl == null) {
            if (other.workflowUrl != null)
                return false;
        } else if (!workflowUrl.equals(other.workflowUrl))
            return false;
        return true;
    }



}
