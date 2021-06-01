package tasks.protocol;

import messages.protocol.ErrorMessage;
import messages.protocol.FileMessage;
import messages.protocol.RestoreMessage;
import peer.Peer;
import peer.storage.StorageFile;
import tasks.Task;

import javax.net.ssl.SSLSocket;
import java.io.IOException;

public class RestoreTask extends Task {
    public RestoreTask(RestoreMessage message, Peer peer, SSLSocket socket) {
        super(message, peer, socket);
    }

    @Override
    public void run() {
        RestoreMessage restoreMessage = (RestoreMessage) message;

        StorageFile storageFile = peer.getPeerStorage().getStoredFile(restoreMessage.getFileId());

        // Don't have the requested file
        if (storageFile == null) {
            System.err.println("[ERROR-RESTORE] Don't have requested file! FileId=" + restoreMessage.getFileId());
            ErrorMessage error = new ErrorMessage(peer.getSelfReference(), "NOFILE");
            try {
                peer.sendMessage(socket, error);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                byte[] fileData = peer.getPeerStorage().restoreFileData(restoreMessage.getFileId());
                FileMessage fileMessage = new FileMessage(peer.getSelfReference(), fileData);
                peer.sendMessage(socket, fileMessage);
                System.out.println("[RESTORE] Read and sent file data successfully for file " + storageFile.getFilePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
