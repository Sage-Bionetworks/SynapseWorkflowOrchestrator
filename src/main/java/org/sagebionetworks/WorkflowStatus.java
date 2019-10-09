package org.sagebionetworks;

public class WorkflowStatus {
	boolean isRunning;
	ExitStatus exitStatus; // is only meaningful if 'isRunning' is false
	Double progress;

	public boolean isRunning() {
		return isRunning;
	}

	public void setRunning(boolean isRunning) {
		this.isRunning = isRunning;
	}


	public ExitStatus getExitStatus() {
		return exitStatus;
	}

	public void setExitStatus(ExitStatus exitStatus) {
		this.exitStatus = exitStatus;
	}

	public Double getProgress() {
		return progress;
	}

	public void setProgress(Double progress) {
		this.progress = progress;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((exitStatus == null) ? 0 : exitStatus.hashCode());
		result = prime * result + (isRunning ? 1231 : 1237);
		result = prime * result + ((progress == null) ? 0 : progress.hashCode());
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
		WorkflowStatus other = (WorkflowStatus) obj;
		if (exitStatus != other.exitStatus)
			return false;
		if (isRunning != other.isRunning)
			return false;
		if (progress == null) {
			if (other.progress != null)
				return false;
		} else if (!progress.equals(other.progress))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "WorkflowStatus [isRunning=" + isRunning + ", exitStatus=" + exitStatus + ", progress=" + progress + "]";
	}


}
