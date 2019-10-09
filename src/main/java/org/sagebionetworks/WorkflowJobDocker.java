package org.sagebionetworks;

import com.github.dockerjava.api.model.Container;

public class WorkflowJobDocker implements WorkflowJob {
	private String containerName;
	private Container container;

	public WorkflowJobDocker() {}
	
	public String getWorkflowId() {
		return containerName;
	}
	
	public String getContainerName() {
		return containerName;
	}
	public void setContainerName(String containerName) {
		this.containerName = containerName;
	}
	public Container getContainer() {
		return container;
	}
	public void setContainer(Container container) {
		this.container = container;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((container == null) ? 0 : container.hashCode());
		result = prime * result + ((containerName == null) ? 0 : containerName.hashCode());
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
		WorkflowJobDocker other = (WorkflowJobDocker) obj;
		if (container == null) {
			if (other.container != null)
				return false;
		} else if (!container.equals(other.container))
			return false;
		if (containerName == null) {
			if (other.containerName != null)
				return false;
		} else if (!containerName.equals(other.containerName))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "WorkflowJobImpl [containerName=" + containerName + ", container=" + container + "]";
	}


}
