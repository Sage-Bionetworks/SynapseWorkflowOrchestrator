package org.sagebionetworks;

public class WorkflowJobWES implements WorkflowJob {
    private String workflowId;


    public void setWorkflowId(String id) {
        this.workflowId=id;
    }

    @Override
    public String getWorkflowId() {
        return workflowId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((workflowId == null) ? 0 : workflowId.hashCode());
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
        WorkflowJobWES other = (WorkflowJobWES) obj;
        if (workflowId == null) {
            if (other.workflowId != null)
                return false;
        } else if (!workflowId.equals(other.workflowId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "WorkflowJobWES [workflowId=" + workflowId + "]";
    }
}
