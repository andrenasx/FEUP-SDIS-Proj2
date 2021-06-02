package tasks.protocol;

import messages.protocol.BackupMessage;
import messages.protocol.ErrorMessage;
import messages.protocol.OkMessage;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;
import java.nio.file.Files;
import java.nio.file.Paths;


public class BackupTask extends Task {
    public BackupTask(BackupMessage message, Peer peer, SSLSocket socket) {
        super(message, peer, socket);
    }

    @Override
    public void run() {
        BackupMessage backupMessage = (BackupMessage) message;
        try {
            // Check if this peer already stored this file, send error
            if (peer.getNodeStorage().hasStoredFile(backupMessage.getStorageFile().getFileId())) {
                ErrorMessage error = new ErrorMessage(peer.getSelfReference(), "HAVEFILE");
                peer.sendMessage(socket, error);
                System.out.println("[ERROR-BACKUP] Already have file " + backupMessage.getStorageFile().getFilePath() + " backed up");
            }
            // Check if this peer has enough space to store
            else if (peer.getNodeStorage().hasEnoughSpace(backupMessage.getStorageFile().getSize())) {
                // Write file data
                Files.write(Paths.get(peer.getNodeStorage().getStoragePath() + backupMessage.getStorageFile().getFileId()), backupMessage.getFileData());

                // Add StorageFile to stored files
                peer.getNodeStorage().addStoredFile(backupMessage.getStorageFile());

                // Stored message, send Okay
                OkMessage okay = new OkMessage(peer.getSelfReference());
                peer.sendMessage(socket, okay);
                System.out.println("[BACKUP] Successfully backed up file " + backupMessage.getStorageFile().getFilePath());
            }
            else {
                // No space
                ErrorMessage error = new ErrorMessage(peer.getSelfReference(), "FULL");
                peer.sendMessage(socket, error);
                System.err.println("[ERROR-BACKUP] No space to store file " + backupMessage.getStorageFile().getFilePath());
            }
        } catch (Exception e) {
            System.err.println("[ERROR-BACKUP] Couldn't backup file " + backupMessage.getStorageFile().getFilePath());
        }
    }
}
