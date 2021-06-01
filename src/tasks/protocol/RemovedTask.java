package tasks.protocol;

import messages.protocol.RemovedMessage;
import peer.Peer;
import peer.storage.StorageFile;
import tasks.Task;

import javax.net.ssl.SSLSocket;
import java.io.IOException;

public class RemovedTask extends Task {
    public RemovedTask(RemovedMessage message, Peer peer, SSLSocket socket) {
        super(message, peer, socket);
    }

    @Override
    public void run() {
        RemovedMessage removedMessage = (RemovedMessage) message;
        String fileId = removedMessage.getFileId();

        StorageFile file = this.peer.getNodeStorage().getStoredFile(fileId);
        if (file == null) {
            System.err.println("[ERROR-REMOVED] Don't have the file to be removed! FileId=" + removedMessage.getFileId());
        }
        else {
            file.removeStoringKey(removedMessage.getRemovedKey());

            //checking if replication degree is lower than desired and backup again
            if (file.getStoringKeys().size() < file.getReplicationDegree()) {
                this.peer.backup(file.getFilePath(), file.getReplicationDegree(), false);
            }
        }

        try {
            this.peer.closeClient(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
