package peer.storage;

import chord.ChordNodeReference;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StorageFile {
    private ChordNodeReference owner;
    private String fileId;
    private String filename;
    private long size;
    private int replicationDegree;
    private Set<ChordNodeReference> storingNodes = ConcurrentHashMap.newKeySet();

    public StorageFile(ChordNodeReference owner, String fileId, String filename, long size, int replicationDegree) {
        this.fileId = fileId;
        this.owner = owner;
        this.filename = filename;
        this.size = size;
        this.replicationDegree = replicationDegree;
    }

    public void addStoringNodes(List<ChordNodeReference> nodes) {
        this.storingNodes.addAll(nodes);
    }

    public synchronized void addStoringNode(ChordNodeReference node) {
        this.storingNodes.add(node);
    }

    public Set<ChordNodeReference> getStoringNodes() {
        return this.storingNodes;
    }

    public ChordNodeReference getOwner() {
        return owner;
    }

    public void setOwner(ChordNodeReference owner) {
        this.owner = owner;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getReplicationDegree() {
        return replicationDegree;
    }

    public void setReplicationDegree(int replicationDegree) {
        this.replicationDegree = replicationDegree;
    }

    @Override
    public String toString() {
        return "StorageFile{" +
                ", fileId='" + fileId +
                ", filename= " + filename +
                ", owner=" + owner +
                ", size=" + size +
                ", replicationDegree=" + replicationDegree +
                '}';
    }
}
