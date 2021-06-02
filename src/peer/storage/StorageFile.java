package peer.storage;

import chord.ChordNodeReference;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StorageFile implements Serializable {
    private final int key;
    private ChordNodeReference owner;
    private String fileId;
    private String filePath;
    private double size;
    private int desiredRepDegree;
    private final Set<Integer> storingKeys = ConcurrentHashMap.newKeySet();

    public StorageFile(int key, ChordNodeReference owner, String fileId, String filePath, double size, int desiredRepDegree) {
        this.key = key;
        this.fileId = fileId;
        this.owner = owner;
        this.filePath = filePath;
        this.size = size;
        this.desiredRepDegree = desiredRepDegree;
    }

    public int getKey() {
        return this.key;
    }

    public void addStoringKeys(List<Integer> keys) {
        this.storingKeys.addAll(keys);
    }

    public synchronized void addStoringKey(int key) {
        this.storingKeys.add(key);
    }

    public synchronized void removeStoringKey(int key) {
        this.storingKeys.remove(key);
    }

    public Set<Integer> getStoringKeys() {
        return this.storingKeys;
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public double getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getDesiredReplicationDegree() {
        return desiredRepDegree;
    }

    public void setDesiredReplicationDegree(int replicationDegree) {
        this.desiredRepDegree = replicationDegree;
    }

    public int getRealDesiredReplicationDegree() {
        return this.storingKeys.size();
    }

    @Override
    public String toString() {
        return "{filename=" + filePath +
                ", fileId=" + fileId +
                ", owner=" + owner +
                ", size=" + Utils.covertSize(size) +
                ", replicationDegree=" + desiredRepDegree +
                ", storingKeys=" +
                "}";
    }
}
