package tasks.protocol;

import messages.protocol.CopyKeysMessage;
import messages.protocol.CopyKeysReplyMessage;
import peer.Peer;
import peer.storage.StorageFile;
import tasks.Task;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CopyKeysTask extends Task {
    public CopyKeysTask(CopyKeysMessage message, Peer peer, SSLSocket socket) {
        super(message, peer, socket);
    }

    @Override
    public void run() {
        List<StorageFile> delegatedFiles = new ArrayList<>();
        for (StorageFile storageFile : peer.getNodeStorage().getStoredFiles().values()) {
            if (storageFile.getKey() <= message.getSenderGuid()) {
                delegatedFiles.add(storageFile);
            }
        }
        try {
            peer.sendMessage(socket, new CopyKeysReplyMessage(peer.getSelfReference(), delegatedFiles));
            System.out.println("[COPY] Delegated " + delegatedFiles.size() + " files to " + message.getSenderGuid());
        } catch (IOException e) {
            System.err.println("[ERROR-COPY] Couldn't send delegated files to " + message.getSenderGuid());
        }
    }
}
