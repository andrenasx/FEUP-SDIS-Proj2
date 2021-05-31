package tasks.chord;

import messages.protocol.BackupMessage;
import messages.protocol.OkMessage;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;
import java.io.FileOutputStream;


public class BackupTask extends Task {
    public BackupTask(BackupMessage message, Peer peer, SSLSocket socket) {
        super(message, peer, socket);
    }

    @Override
    public void run() {
        // TODO check if peer has space or already has file, send ErrorMessage
        System.out.println("Testing capacity.. (to be implemented)");

        try (FileOutputStream fos = new FileOutputStream(peer.getPeerStorage().getStoragePath() + ((BackupMessage) message).getStorageFile().getFileId())) {
            fos.write(((BackupMessage) message).getFileData());
            System.out.println("[BACKUP] Ready to receive file...");

            OkMessage okay = new OkMessage(peer.getSelfReference());
            peer.sendMessage(socket, okay);
            peer.getPeerStorage().addStoredFile(((BackupMessage) message).getStorageFile());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
