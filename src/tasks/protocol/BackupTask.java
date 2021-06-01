package tasks.protocol;

import messages.protocol.BackupMessage;
import messages.protocol.ErrorMessage;
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
        BackupMessage backupMessage = (BackupMessage) message;
        try {
            if (peer.getNodeStorage().hasStoredFile(backupMessage.getStorageFile().getFileId())) {
                ErrorMessage error = new ErrorMessage(peer.getSelfReference(), "HAVEFILE");
                peer.sendMessage(socket, error);
            }
            else if (peer.getNodeStorage().hasEnoughSpace(backupMessage.getStorageFile().getSize())) {
                FileOutputStream fos = new FileOutputStream(peer.getNodeStorage().getStoragePath() + backupMessage.getStorageFile().getFileId());
                fos.write(backupMessage.getFileData());

                // Stored message, send Okay
                OkMessage okay = new OkMessage(peer.getSelfReference());
                peer.sendMessage(socket, okay);
                peer.getNodeStorage().addStoredFile(backupMessage.getStorageFile());
            }
            else {
                ErrorMessage error = new ErrorMessage(peer.getSelfReference(), "FULL");
                peer.sendMessage(socket, error);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
