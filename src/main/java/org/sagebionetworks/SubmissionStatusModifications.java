package org.sagebionetworks;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.annotation.AnnotationBase;

public class SubmissionStatusModifications {
    private SubmissionStatusEnum status; // null if unchanged
    private List<AnnotationBase> annotationsToAdd;
    private List<String> annotationNamesToRemove;
    private Boolean canCancel; // null if unchanged
    private Boolean cancelRequested; // null if unchanged

    public SubmissionStatusModifications() {
        annotationsToAdd = new ArrayList<AnnotationBase>();
        annotationNamesToRemove = new ArrayList<String>();
    }

    public SubmissionStatusEnum getStatus() {
        return status;
    }

    public void setStatus(SubmissionStatusEnum status) {
        this.status = status;
    }

    public List<AnnotationBase> getAnnotationsToAdd() {
        return annotationsToAdd;
    }

    public void setAnnotationsToAdd(List<AnnotationBase> annotationsToAdd) {
        this.annotationsToAdd = annotationsToAdd;
    }

    public List<String> getAnnotationNamesToRemove() {
        return annotationNamesToRemove;
    }

    public void setAnnotationNamesToRemove(List<String> annotationNamesToRemove) {
        this.annotationNamesToRemove = annotationNamesToRemove;
    }

    public Boolean getCanCancel() {
        return canCancel;
    }

    public void setCanCancel(Boolean canCancel) {
        this.canCancel = canCancel;
    }

    public Boolean getCancelRequested() {
        return cancelRequested;
    }

    public void setCancelRequested(Boolean cancelRequested) {
        this.cancelRequested = cancelRequested;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((annotationNamesToRemove == null) ? 0 : annotationNamesToRemove.hashCode());
        result = prime * result + ((annotationsToAdd == null) ? 0 : annotationsToAdd.hashCode());
        result = prime * result + ((canCancel == null) ? 0 : canCancel.hashCode());
        result = prime * result + ((cancelRequested == null) ? 0 : cancelRequested.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
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
        SubmissionStatusModifications other = (SubmissionStatusModifications) obj;
        if (annotationNamesToRemove == null) {
            if (other.annotationNamesToRemove != null)
                return false;
        } else if (!annotationNamesToRemove.equals(other.annotationNamesToRemove))
            return false;
        if (annotationsToAdd == null) {
            if (other.annotationsToAdd != null)
                return false;
        } else if (!annotationsToAdd.equals(other.annotationsToAdd))
            return false;
        if (canCancel == null) {
            if (other.canCancel != null)
                return false;
        } else if (!canCancel.equals(other.canCancel))
            return false;
        if (cancelRequested == null) {
            if (other.cancelRequested != null)
                return false;
        } else if (!cancelRequested.equals(other.cancelRequested))
            return false;
        if (status != other.status)
            return false;
        return true;
    }


}
