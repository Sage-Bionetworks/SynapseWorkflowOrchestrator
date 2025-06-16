package org.sagebionetworks;

public class WorkflowParameters {
    private String submissionId;
    private String adminUploadSynId;
    private String submitterUploadSynId;
    private String synapseWorkflowReference;
    public WorkflowParameters(String submissionId, String synapseWorkflowReference, String adminUploadSynId, String submitterUploadSynId) {
        this.synapseWorkflowReference=synapseWorkflowReference;
        this.submissionId = submissionId;
        this.adminUploadSynId = adminUploadSynId;
        this.submitterUploadSynId = submitterUploadSynId;
    }
    public String getSubmissionId() {
        return submissionId;
    }
    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }
    public String getAdminUploadSynId() {
        return adminUploadSynId;
    }
    public void setAdminUploadSynId(String adminUploadSynId) {
        this.adminUploadSynId = adminUploadSynId;
    }
    public String getSubmitterUploadSynId() {
        return submitterUploadSynId;
    }
    public void setSubmitterUploadSynId(String submitterUploadSynId) {
        this.submitterUploadSynId = submitterUploadSynId;
    }

    public String getSynapseWorkflowReference() {
        return synapseWorkflowReference;
    }
    public void setSynapseWorkflowReference(String synapseWorkflowReference) {
        this.synapseWorkflowReference = synapseWorkflowReference;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((adminUploadSynId == null) ? 0 : adminUploadSynId.hashCode());
        result = prime * result + ((submissionId == null) ? 0 : submissionId.hashCode());
        result = prime * result + ((submitterUploadSynId == null) ? 0 : submitterUploadSynId.hashCode());
        result = prime * result + ((synapseWorkflowReference == null) ? 0 : synapseWorkflowReference.hashCode());
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
        WorkflowParameters other = (WorkflowParameters) obj;
        if (adminUploadSynId == null) {
            if (other.adminUploadSynId != null)
                return false;
        } else if (!adminUploadSynId.equals(other.adminUploadSynId))
            return false;
        if (submissionId == null) {
            if (other.submissionId != null)
                return false;
        } else if (!submissionId.equals(other.submissionId))
            return false;
        if (submitterUploadSynId == null) {
            if (other.submitterUploadSynId != null)
                return false;
        } else if (!submitterUploadSynId.equals(other.submitterUploadSynId))
            return false;
        if (synapseWorkflowReference == null) {
            if (other.synapseWorkflowReference != null)
                return false;
        } else if (!synapseWorkflowReference.equals(other.synapseWorkflowReference))
            return false;
        return true;
    }


}
