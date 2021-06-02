package messages.chord;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import storage.StorageFile;
import tasks.chord.AlertSuccessorTask;

import javax.net.ssl.SSLSocket;
import java.util.List;

public class AlertSuccessorMessage extends Message {
    ChordNodeReference predecessor;
    List<StorageFile> delegatedFiles;

    public AlertSuccessorMessage(ChordNodeReference senderReference, List<StorageFile> delegatedFiles, ChordNodeReference predecessor) {
        super(senderReference);

        this.predecessor = predecessor;
        this.delegatedFiles = delegatedFiles;
    }

    @Override
    public AlertSuccessorTask getTask(Peer peer, SSLSocket socket) {
        return new AlertSuccessorTask(this, peer, socket);
    }

    public ChordNodeReference getPredecessor() {
        return predecessor;
    }

    public List<StorageFile> getDelegatedFiles() {
        return delegatedFiles;
    }
}
