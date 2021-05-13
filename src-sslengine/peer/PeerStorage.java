package peer;

import storage.Chunk;
import storage.StorageFile;
import utils.Utils;
import workers.DeleteChunkWorker;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PeerStorage implements Serializable {
    private double storageCapacity;
    private double occupiedSpace;
    private final ConcurrentHashMap<String, Chunk> storedChunks;
    private final ConcurrentHashMap<String, Chunk> sentChunks;
    private final ConcurrentHashMap<String, StorageFile> storageFileMap;
    private final ConcurrentHashMap<String, Set<Integer>> deletedFilesMap;
    private final String storagePath;

    public PeerStorage(int id) {
        this.storageCapacity = 100000000; // 100 MBytes
        this.occupiedSpace = 0;
        this.storedChunks = new ConcurrentHashMap<>();
        this.sentChunks = new ConcurrentHashMap<>();
        this.storageFileMap = new ConcurrentHashMap<>();
        this.deletedFilesMap = new ConcurrentHashMap<>();
        this.storagePath = "../../PeerStorage/Peer" + id + "/";

        // Create peer storage folder
        try {
            Files.createDirectories(Paths.get(this.storagePath));
        } catch (IOException e) {
            System.err.println("Failed to create peer storage directory!");
        }
    }

    public static PeerStorage loadState(Peer peer) {
        PeerStorage storage = null;
        try {
            FileInputStream fileIn = new FileInputStream("../../PeerStorage/Peer" + peer.getId() + "/_state");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            storage = (PeerStorage) in.readObject();
            in.close();
            fileIn.close();
        } catch (Exception e) {
            System.out.println("[STORAGE] Unable to load Peer storage state from file");
        }

        if (storage == null) {
            storage = new PeerStorage(peer.getId());
        }
        else {
            System.out.println("[STORAGE] Loaded Peer storage state from file successfully");
        }

        return storage;
    }

    public void saveState() {
        try {
            FileOutputStream fileOut = new FileOutputStream(this.storagePath + "_state");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this);
            out.flush();
            out.close();
            fileOut.close();
            System.out.println("\n[STORAGE] Saved Peer storage state successfully\n");
        } catch (IOException i) {
            System.err.println("[STORAGE] Unable to load Peer storage state from file");
        }
    }


    public void storeChunk(Chunk chunk, byte[] body) throws IOException {
        // Write body to file
        FileOutputStream out = new FileOutputStream(this.storagePath + chunk.getUniqueId());
        out.write(body);
        out.close();

        this.occupySpace(chunk.getSize());
        System.out.println("[BACKUP] Stored chunk " + chunk.getUniqueId());
    }

    public byte[] restoreChunkBody(String chunkId) throws IOException {
        // Read all information from chunk file
        File file = new File(this.storagePath + chunkId);
        return Files.readAllBytes(file.toPath());
    }

    public void deleteStoredChunk(Chunk chunk, String protocol) {
        // Delete stored chunk file and remove it from map
        File file = new File(this.storagePath + chunk.getUniqueId());
        if (file.delete()) {
            this.storedChunks.remove(chunk.getUniqueId());
            this.freeSpace(chunk.getSize());
            System.out.printf("[%s] Deleted chunk %s\n", protocol, chunk.getUniqueId());
        }
        else {
            System.err.printf("Error deleting chunk %s\n", chunk.getUniqueId());
        }
    }

    public void deleteSentChunks(String fileId) {
        if (this.deletedFilesMap.containsKey(fileId)) return;

        Set<Integer> deletedFileAcks = ConcurrentHashMap.newKeySet();

        // Delete all sent chunks with given fileId
        for (Chunk chunk : this.sentChunks.values()) {
            if (chunk.getFileId().equals(fileId)) {
                deletedFileAcks.addAll(chunk.getPeersStoring());
                this.sentChunks.remove(chunk.getUniqueId());
            }
        }

        this.deletedFilesMap.put(fileId, deletedFileAcks);
    }

    public void reclaim(Peer peer, double maxKBytes) {
        // Set new capacity
        System.out.println("\n[RECLAIMING] New storage capacity: " + maxKBytes + " KBytes");
        this.storageCapacity = maxKBytes * 1000;

        // Clean all stored chunks if 0
        if (maxKBytes == 0) {
            for (Chunk chunk : storedChunks.values()) {
                DeleteChunkWorker worker = new DeleteChunkWorker(peer, chunk);
                peer.submitControlThread(worker);
            }
        }
        else {
            // End reclaim if new set capacity is higher than occupied space
            if (this.occupiedSpace <= this.storageCapacity) return;

            // Delete all chunks that are over replicated (perceived replication degree is higher then desired)
            for (Map.Entry<String, Chunk> entry : this.storedChunks.entrySet()) {
                Chunk chunk = entry.getValue();
                if (chunk.isOverReplicated()) {
                    DeleteChunkWorker worker = new DeleteChunkWorker(peer, chunk);
                    peer.submitControlThread(worker);
                }
            }

            // Sleep so all threads finish delete
            Utils.sleep(100);

            // End reclaim if storage capacity is higher than occupied space after last deletion
            if (this.occupiedSpace <= this.storageCapacity) return;

            // Delete chunks until storage capacity is higher than occupied space after deletion
            for (Map.Entry<String, Chunk> entry : this.storedChunks.entrySet()) {
                Chunk chunk = entry.getValue();
                DeleteChunkWorker worker = new DeleteChunkWorker(peer, chunk);
                worker.run();

                if (this.occupiedSpace <= this.storageCapacity) return;
            }
        }
    }


    public synchronized void occupySpace(double space) {
        this.occupiedSpace += space;
    }

    public synchronized void freeSpace(double space) {
        this.occupiedSpace -= space;
    }

    public boolean hasEnoughSpace(double chunkSize) {
        return this.occupiedSpace + chunkSize <= this.storageCapacity;
    }

    public void addStoredChunk(String chunkId, Chunk chunk) {
        this.storedChunks.put(chunkId, chunk);
    }

    public void removeStoredChunk(String chunkUniqueId) {
        this.storedChunks.remove(chunkUniqueId);
    }

    public Chunk getStoredChunk(String chunkId) {
        return this.storedChunks.get(chunkId);
    }

    public Chunk getStoredChunk(String fileId, int chunkNo) {
        return this.storedChunks.get(fileId + "_" + chunkNo);
    }

    public void addSentChunk(Chunk chunk) {
        this.sentChunks.put(chunk.getUniqueId(), chunk);
    }

    public Chunk getSentChunk(String chunkId) {
        return this.sentChunks.get(chunkId);
    }

    public Chunk getSentChunk(String fileId, int chunkId) {
        return this.sentChunks.get(fileId + "_" + chunkId);
    }

    public boolean hasStoredChunk(String fileId, int chunkId) {
        return this.storedChunks.containsKey(fileId + "_" + chunkId);
    }

    public boolean hasStoredChunk(String uniqueId) {
        return this.storedChunks.containsKey(uniqueId);
    }

    public boolean hasSentChunk(String fileId, int chunkId) {
        return this.sentChunks.containsKey(fileId + "_" + chunkId);
    }

    public boolean hasSentChunk(String uniqueId) {
        return this.sentChunks.containsKey(uniqueId);
    }

    public ConcurrentHashMap<String, Chunk> getStoredChunks() {
        return this.storedChunks;
    }

    public ConcurrentHashMap<String, Chunk> getSentChunks() {
        return this.sentChunks;
    }

    public ConcurrentHashMap<String, StorageFile> getStorageFileMap() {
        return storageFileMap;
    }

    public ConcurrentHashMap<String, Set<Integer>> getDeletedFilesMap() {
        return deletedFilesMap;
    }

    public double getStorageCapacity() {
        return storageCapacity;
    }

    public double getOccupiedSpace() {
        return occupiedSpace;
    }

    public String getStoragePath() {
        return storagePath;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("---Backed up Files---\n");
        for (StorageFile storageFile : this.storageFileMap.values()) {
            sb.append("FILE -> pathname: ")
                    .append(storageFile.getFilePath())
                    .append(" ; id: ")
                    .append(storageFile.getFileId())
                    .append(" ; desired replication degree: ")
                    .append(storageFile.getReplicationDegree())
                    .append("\n");

            for (Chunk chunk : this.sentChunks.values()) {
                if (chunk.getFileId().equals(storageFile.getFileId())) {
                    sb.append("\t").append(chunk.toStringSent()).append("\n");
                }
            }
            sb.append("\n");
        }

        sb.append("\n---Stored Chunks---\n");
        for (Chunk chunk : this.storedChunks.values()) {
            sb.append(chunk.toStringStored()).append("\n");
        }

        sb.append("\n---Storage---\n")
                .append("Maximum capacity: ").append(this.storageCapacity / 1000.0).append(" KBytes\n")
                .append("Occupied space: ").append(this.occupiedSpace / 1000.0).append(" KBytes\n");

        return sb.toString();
    }
}
