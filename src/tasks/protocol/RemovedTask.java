package tasks.protocol;

import chord.ChordNodeReference;
import messages.protocol.BackupMessage;
import messages.protocol.RemovedMessage;
import peer.Peer;
import peer.storage.StorageFile;
import tasks.Task;
import utils.Utils;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class RemovedTask extends Task {
    public RemovedTask(RemovedMessage message, Peer peer, SSLSocket socket) {
        super(message, peer, socket);
    }

    @Override
    public void run() {
        RemovedMessage removedMessage = (RemovedMessage) message;
        String fileId = removedMessage.getFileId();
        int fileKey = removedMessage.getRemovedKey();

        try {
            this.peer.closeClient(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }

        StorageFile sentFile = null;
        for (Map.Entry<String, StorageFile> entry : peer.getNodeStorage().getSentFiles().entrySet()) {
            if (entry.getValue().getFileId().equals(fileId)) {
                sentFile = entry.getValue();
                break;
            }
        }

        if (sentFile == null) {
            System.err.println("[ERROR-REMOVED] Don't have the file to remove that key! FileId=" + fileId);
            return;
        }


        sentFile.removeStoringKey(fileKey);
        System.out.println("[RECLAIM] Removed stored key " + fileKey + " for file " + sentFile.getFilePath());

        //checking if replication degree is lower than desired and backup again
        if (sentFile.getRealDesiredReplicationDegree() < sentFile.getDesiredReplicationDegree()) {
            int neededReplicationDegree = sentFile.getDesiredReplicationDegree() - sentFile.getRealDesiredReplicationDegree();
            System.out.println("[SUB-BACKUP] File " + sentFile.getFilePath() + " needs " + neededReplicationDegree + " more backups to achieve desired replication degree.");

            final int[] keys = new Random().ints(0, Utils.CHORD_MAX_PEERS).distinct().limit(neededReplicationDegree * 4L).toArray();

            System.out.println("[SUB-BACKUP] Generated Keys: " + Arrays.toString(keys));

            Map<ChordNodeReference, Integer> nodesToStore = new HashMap<>();
            for (int key : keys) {
                if (!sentFile.getStoringKeys().contains(key)) {
                    ChordNodeReference node = peer.findSuccessor(key);
                    if ((node.getGuid() != peer.getSelfReference().getGuid()) && (node.getGuid() != removedMessage.getSenderGuid()) && !nodesToStore.containsKey(node)) {
                        nodesToStore.put(node, key);
                        if (nodesToStore.size() == neededReplicationDegree) break;
                    }
                }
            }

            if (nodesToStore.size() == 0) {
                System.out.println("[ERROR-BACKUP] No available peers to backup file.");
                return;
            }

            StringBuilder sb = new StringBuilder("[SUB-BACKUP] Storing Keys: ");
            for (Integer key : nodesToStore.values()) {
                sb.append(key).append("; ");
            }
            System.out.println(sb);

            byte[] fileData;
            try {
                fileData = Files.readAllBytes(Paths.get(sentFile.getFilePath()));
            } catch (IOException e) {
                System.err.println("[ERROR-BACKUP] Aborting backup, couldn't read " + sentFile.getFilePath() + " data. " + e.getMessage());
                return;
            }

            List<Future<String>> backups = new ArrayList<>();
            for (Map.Entry<ChordNodeReference, Integer> entry : nodesToStore.entrySet()) {
                BackupMessage bm = new BackupMessage(peer.getSelfReference(), new StorageFile(entry.getValue(), peer.getSelfReference(), sentFile.getFileId(), sentFile.getFilePath(), sentFile.getSize(), sentFile.getDesiredReplicationDegree()), fileData);
                StorageFile finalSentFile = sentFile;
                backups.add(peer.getScheduler().submit(() -> peer.backupFile(bm, entry.getKey(), finalSentFile)));
            }

            try {
                StringBuilder sbackups = new StringBuilder("[SUB-BACKUP] Result for " + sentFile.getFilePath() + "\n");
                // Wait for backups to finish and print result
                for (Future<String> backup : backups) {
                    sbackups.append(backup.get()).append("\n");
                }
                peer.getNodeStorage().addSentFile(sentFile);

                System.out.println(sbackups);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
