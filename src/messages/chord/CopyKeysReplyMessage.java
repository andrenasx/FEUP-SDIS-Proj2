package messages.protocol;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import storage.StorageFile;
import tasks.Task;

import javax.net.ssl.SSLSocket;
import java.util.List;

public class CopyKeysReplyMessage extends Message {
    private List<StorageFile> delegatedFiles;

    public CopyKeysReplyMessage(ChordNodeReference senderReference, List<StorageFile> delegatedFiles) {
        super(senderReference);
        this.delegatedFiles = delegatedFiles;
    }

    @Override
    public Task getTask(Peer peer, SSLSocket socket) {
        return null;
    }

    public List<StorageFile> getDelegatedFiles() {
        return delegatedFiles;
    }
}
