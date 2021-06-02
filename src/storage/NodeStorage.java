package storage;

import utils.Utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

public class NodeStorage implements Serializable {
    private double storageCapacity;
    private double occupiedSpace;
    private final ConcurrentHashMap<String, StorageFile> sentFiles; // <FileName, StorageFile>
    private final ConcurrentHashMap<String, StorageFile> storedFiles; // <FileID, StorageFile>
    private final String storagePath;

    public NodeStorage(int id) {
        this.storageCapacity = 104857600; // 100 MBytes
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

    public static NodeStorage loadState(int id) {
        NodeStorage storage = null;
        try {
            FileInputStream fileIn = new FileInputStream("../PeerStorage/Peer" + id + "/_state");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            storage = (NodeStorage) in.readObject();
            in.close();
            fileIn.close();
        } catch (Exception e) {
            System.out.println("[STORAGE] Unable to load Peer storage state from file");
        }

        if (storage == null) {
            storage = new NodeStorage(id);
            System.out.println("[STORAGE] Created new storage");
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
        if (this.sentFiles.containsKey(storageFile.getFilePath())) {
            StorageFile storedFile = this.sentFiles.get(storageFile.getFilePath());
            for (Integer key : storedFile.getStoringKeys()) {
                storageFile.addStoringKey(key);
            }
        }
        this.sentFiles.put(storageFile.getFilePath(), storageFile);
    }

    public StorageFile getSentFile(String filename) {
        return this.sentFiles.get(filename);
    }

    public ConcurrentHashMap<String, StorageFile> getSentFiles() {
        return this.sentFiles;
    }

    public StorageFile getStoredFile(String fileId) {
        return this.storedFiles.get(fileId);
    }

    public ConcurrentHashMap<String, StorageFile> getStoredFiles() {
        return this.storedFiles;
    }

    public void removeSentFile(String filename) {
        this.sentFiles.remove(filename);
    }

    public void removeStoredFile(String fileId) {
        StorageFile storedFile = this.getStoredFile(fileId);
        if (storedFile != null) {
            this.freeSpace(storedFile.getSize());
            this.storedFiles.remove(fileId);
        }
    }

    public synchronized void occupySpace(double space) {
        this.occupiedSpace += space;
    }

    public synchronized void freeSpace(double space) {
        this.occupiedSpace -= space;
    }

    public boolean hasEnoughSpace(double fileSize) {
        return this.occupiedSpace + fileSize <= this.storageCapacity;
    }

    public void setStorageCapacity(double capacity) {
        this.storageCapacity = capacity;
    }

    public double getStorageCapacity() {
        return storageCapacity;
    }

    public String printStorageCapacity() {
        return Utils.convertSize(this.storageCapacity);
    }

    public double getOccupiedSpace() {
        return occupiedSpace;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public byte[] restoreFileData(String fileId) throws IOException {
        return Files.readAllBytes(Paths.get(this.storagePath + fileId));
    }

    public void deleteStoredFile(String fileId) throws IOException {
        if (Files.deleteIfExists(Paths.get(this.getStoragePath() + fileId))) {
            this.removeStoredFile(fileId);
        }
        else {
            throw new IOException();
        }
    }

    @Override
    public String toString() {
        String sb = "";


        sb+="---Sent Files---\n";
        for (StorageFile storageFile : this.sentFiles.values()) {
            sb+=storageFile + "\n";
        }
        sb+="\n---Stored Files---\n";
        for (StorageFile storageFile : this.storedFiles.values()) {
            sb+=(storageFile.toString()).substring(0, storageFile.toString().length()-16) + "}\n";
        }




        sb+="\n---Storage---\n"
               +"Maximum capacity: "+Utils.convertSize(this.storageCapacity)+"\n"
                +"Occupied space: "+Utils.convertSize(this.occupiedSpace);

        return sb.toString();
    }
}
