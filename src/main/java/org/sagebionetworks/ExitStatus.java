package org.sagebionetworks;

/*
 * This enum applies to workflows which have terminated, 
 * and describes the termination status
 */
public enum ExitStatus {
	SUCCESS, // finished successfully
	FAILURE, // encountered an error during execution
	CANCELED // was canceled while still running
}
