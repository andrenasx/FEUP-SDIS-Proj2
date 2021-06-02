package tasks.protocol;

import messages.protocol.ErrorMessage;
import messages.protocol.FileMessage;
import messages.protocol.GetFileMessage;
import peer.Peer;
import peer.storage.StorageFile;
import tasks.Task;

import javax.net.ssl.SSLSocket;
import java.io.IOException;

public class GetFileTask extends Task {
    public GetFileTask(GetFileMessage message, Peer peer, SSLSocket socket) {
        super(message, peer, socket);
    }

    @Override
    public void run() {
        GetFileMessage restoreMessage = (GetFileMessage) message;

        StorageFile storageFile = peer.getNodeStorage().getStoredFile(restoreMessage.getFileId());

        if (storageFile == null) {
            // Don't have the requested file, send error message
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
                // Peer has file, restore file data and send it to requester
                byte[] fileData = peer.getNodeStorage().restoreFileData(restoreMessage.getFileId());
                FileMessage fileMessage = new FileMessage(peer.getSelfReference(), fileData);
                peer.sendMessage(socket, fileMessage);
                System.out.println("[RESTORE] Sent file data successfully to peer " + message.getSenderGuid() + " for file " + storageFile.getFilePath());
            } catch (IOException e) {
                System.err.println("[ERROR-RESTORE] Couldn't restore file " + restoreMessage.getFileId());
            }
        }
    }
}
