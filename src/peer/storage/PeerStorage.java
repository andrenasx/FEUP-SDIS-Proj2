package peer.storage;

import peer.Peer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

public class PeerStorage implements Serializable {
    private double storageCapacity;
    private double occupiedSpace;
    private final ConcurrentHashMap<String, StorageFile> sentFiles; // <FileName, StorageFile>
    private final ConcurrentHashMap<String, StorageFile> storedFiles; // <FileID, StorageFile>
    private final String storagePath;

    public PeerStorage(int id) {
        this.storageCapacity = 100000000; // 100 MBytes
        this.occupiedSpace = 0;
        this.sentFiles = new ConcurrentHashMap<>();
        this.storedFiles = new ConcurrentHashMap<>();
        this.storagePath = "../PeerStorage/Peer" + id + "/";

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
            FileInputStream fileIn = new FileInputStream("../PeerStorage/Peer" + peer.getSelfReference().getGuid() + "/_state");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            storage = (PeerStorage) in.readObject();
            in.close();
            fileIn.close();
        } catch (Exception e) {
            System.out.println("[STORAGE] Unable to load Peer storage state from file");
        }

        if (storage == null) {
            storage = new PeerStorage(peer.getSelfReference().getGuid());
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

    public boolean hasStoredFile(String fileId) {
        return this.storedFiles.containsKey(fileId);
    }

    public void addStoredFile(StorageFile storageFile) {
        storedFiles.put(storageFile.getFileId(), storageFile);
        this.occupySpace(storageFile.getSize());
    }

    public void addSentFile(StorageFile storageFile) {
        if (this.sentFiles.containsKey(storageFile.getFileId())) {
            for (Integer key : this.sentFiles.get(storageFile.getFileId()).getStoringKeys()) {
                storageFile.addStoringKey(key);
            }
        }
        this.sentFiles.put(storageFile.getFilePath(), storageFile);
    }

    public StorageFile getSentFile(String filename) {
        return this.sentFiles.get(filename);
    }

    public StorageFile getStoredFile(String fileId) {
        return this.storedFiles.get(fileId);
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

    public double getStorageCapacity() {
        return storageCapacity;
    }

    public double getOccupiedSpace() {
        return occupiedSpace;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public byte[] restoreFileData(String fileId) throws IOException {
        File file = new File(this.storagePath + fileId);
        return Files.readAllBytes(file.toPath());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("---Stored Files---\n");
        for (StorageFile storageFile : this.storedFiles.values()) {
            sb.append(storageFile.liteString()).append("\n");
        }

        sb.append("---Sent Files---\n");
        for (StorageFile storageFile : this.sentFiles.values()) {
            sb.append(storageFile.liteString()).append("\n");
        }


        sb.append("\n---Storage---\n")
                .append("Maximum capacity: ").append(this.storageCapacity / 1000.0).append(" KBytes\n")
                .append("Occupied space: ").append(this.occupiedSpace / 1000.0).append(" KBytes\n");

        return sb.toString();
    }
}
