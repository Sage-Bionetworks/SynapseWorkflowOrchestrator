package org.sagebionetworks;

/*
 * Data object used to hold the file path to a folder
 * and also a file within that folder
 */
public class FolderAndRelativePath {
    private ContainerRelativeFile folder;
    private String relativePath;
    public FolderAndRelativePath(ContainerRelativeFile folder, String relativePath) {
        super();
        this.folder = folder;
        this.relativePath = relativePath;
    }

    public ContainerRelativeFile getFolder() {
        return folder;
    }
    public void setFolder(ContainerRelativeFile folder) {
        this.folder = folder;
    }
    public String getRelativePath() {
        return relativePath;
    }
    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((folder == null) ? 0 : folder.hashCode());
        result = prime * result + ((relativePath == null) ? 0 : relativePath.hashCode());
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
        FolderAndRelativePath other = (FolderAndRelativePath) obj;
        if (folder == null) {
            if (other.folder != null)
                return false;
        } else if (!folder.equals(other.folder))
            return false;
        if (relativePath == null) {
            if (other.relativePath != null)
                return false;
        } else if (!relativePath.equals(other.relativePath))
            return false;
        return true;
    }

}
