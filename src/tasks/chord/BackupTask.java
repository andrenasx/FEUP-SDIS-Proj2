package tasks.chord;

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
        // TODO check if peer has space or already has file, send ErrorMessage
        System.out.println("Testing capacity.. (to be implemented)");
        BackupMessage backupMessage = (BackupMessage) message;
        try (FileOutputStream fos = new FileOutputStream(peer.getPeerStorage().getStoragePath() + (backupMessage.getStorageFile().getFileId()))) {
            if(peer.getPeerStorage().hasStoredFile(backupMessage.getStorageFile().getFileId())){
                ErrorMessage error = new ErrorMessage(peer.getSelfReference(), "HAVEFILE");
                peer.sendMessage(socket, error);
            }
            else if(peer.getPeerStorage().hasEnoughSpace(backupMessage.getStorageFile().getSize())){
                fos.write(backupMessage.getFileData());
                System.out.println("[BACKUP] Ready to receive file...");

                OkMessage okay = new OkMessage(peer.getSelfReference());
                peer.sendMessage(socket, okay);
                peer.getPeerStorage().addStoredFile(backupMessage.getStorageFile());
            }
            else{
                ErrorMessage error = new ErrorMessage(peer.getSelfReference(), "FULL");
                peer.sendMessage(socket, error);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
