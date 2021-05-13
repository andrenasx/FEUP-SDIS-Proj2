package storage;

import peer.Peer;
import utils.Utils;
import workers.BackupChunkWorker;
import workers.DeleteFileWorker;
import workers.RestoreChunkWorker;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;


public class StorageFile implements Serializable {
    private final String filePath;
    private final String fileId;
    private final int replicationDegree;
    private int num_chunks = 0;

    public StorageFile(String filePath, int replicationDegree) throws Exception {
        this.filePath = filePath;
        this.replicationDegree = replicationDegree;

        this.fileId = Utils.createFileId(filePath);
    }

    public void backup(Peer peer) throws IOException {
        System.out.printf("\n[BACKUP] Initiated backup for file: %s\n", fileId);
        peer.getStorage().getDeletedFilesMap().remove(fileId);

        // Read file data, split chunks and send them
        File file = new File(this.filePath);
        int fileSize = (int) file.length();
        FileInputStream fileReader = new FileInputStream(file);

        int i = 0;
        for (int bytesRead = 0; bytesRead < fileSize; i++) {
            byte[] data;
            if (fileSize - bytesRead >= Utils.CHUNK_SIZE) {
                data = new byte[Utils.CHUNK_SIZE];
                bytesRead += fileReader.read(data, 0, Utils.CHUNK_SIZE);
            }
            else {
                data = new byte[fileSize - bytesRead];
                bytesRead += fileReader.read(data, 0, fileSize - bytesRead);
            }

            // Create new Chunk and add to peer storage sentChunk map
            Chunk chunk = new Chunk(this.fileId, i, this.replicationDegree, data);
            peer.getStorage().addSentChunk(chunk);
            this.num_chunks++;

            // Submit backup worker
            BackupChunkWorker worker = new BackupChunkWorker(peer, chunk);
            peer.submitBackupThread(worker);

            System.out.printf("[BACKUP] Submitted backup for chunk: %s_%d\n", fileId, i);
        }

        // If the file size is a multiple of the chunk size, the last chunk has size 0
        if (fileSize % Utils.CHUNK_SIZE == 0) {
            Chunk chunk = new Chunk(this.fileId, ++i, this.replicationDegree, new byte[0]);
            peer.getStorage().addSentChunk(chunk);
            this.num_chunks++;

            BackupChunkWorker worker = new BackupChunkWorker(peer, chunk);
            peer.submitBackupThread(worker);

            System.out.printf("[BACKUP] Submitted backup for chunk: %s_%d\n", fileId, i);
        }

        fileReader.close();

    }

    public void delete(Peer peer) {
        // Submit delete worker for this file
        DeleteFileWorker worker = new DeleteFileWorker(peer, this.fileId);
        peer.submitControlThread(worker);

        System.out.printf("[DELETION] Submitted delete for file: %s\n", this.fileId);
    }

    public void restore(Peer peer) throws Exception {
        System.out.printf("[RESTORE] Initiated restore for file: %s\n", fileId);

        List<Future<Chunk>> receivedChunks = new ArrayList<>();

        // Create a restore worker for each chunk of the file
        ConcurrentHashMap<String, Chunk> sentChunks = peer.getStorage().getSentChunks();
        for (Chunk chunk : sentChunks.values()) {
            if (chunk.getFileId().equals(this.fileId)) {
                RestoreChunkWorker worker = new RestoreChunkWorker(peer, chunk);
                receivedChunks.add(peer.submitControlThread(worker));
                System.out.printf("[RESTORE] Submitted restore for chunk: %s\n", chunk.getUniqueId());
            }
        }

        // Create restored file path
        File file = new File(this.filePath);
        String restoredFilePath = file.getParent() + "/restored_" + file.getName();
        Files.createDirectories(Paths.get(file.getParent()));

        // Open channel to write information
        RandomAccessFile raf = new RandomAccessFile(restoredFilePath, "rw");

        for (Future<Chunk> chunkFuture : receivedChunks) {
            Chunk chunk = chunkFuture.get();

            // Abort if chunk or its body is null
            if (chunk == null || chunk.getBody() == null) {
                System.err.println("Error retrieving chunk, aborting restore");
                raf.close();
                return;
            }
            // Abort if not the last chunk but body has less than 64KB
            else if ((chunk.getChunkNo() != this.num_chunks - 1) && chunk.getBody().length != Utils.CHUNK_SIZE) {
                System.err.println("Not last chunk with less than 64KB, aborting restore");
                chunk.clearBody();
                raf.close();
                return;
            }

            // Write body to respective position offset in file
            raf.seek((long) Utils.CHUNK_SIZE * chunk.getChunkNo());
            raf.write(chunk.getBody());

            // Clear Chunk body so we don't waste memory
            chunk.clearBody();
        }

        raf.close();

        System.out.printf("[RESTORE] Finished restore for file %s\n", this.fileId);
    }


    public String getFilePath() {
        return filePath;
    }

    public String getFileId() {
        return fileId;
    }

    public int getReplicationDegree() {
        return replicationDegree;
    }
}
