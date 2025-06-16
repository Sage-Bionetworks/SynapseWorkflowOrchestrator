package org.sagebionetworks;

public class FileIdAndSize {
    private String id;
    private long size;
    public FileIdAndSize(String id, long size) {
        super();
        this.id = id;
        this.size = size;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public long getSize() {
        return size;
    }
    public void setSize(long size) {
        this.size = size;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + (int) (size ^ (size >>> 32));
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
        FileIdAndSize other = (FileIdAndSize) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (size != other.size)
            return false;
        return true;
    }
    @Override
    public String toString() {
        return "FileIdAndSize [id=" + id + ", size=" + size + "]";
    }



}
