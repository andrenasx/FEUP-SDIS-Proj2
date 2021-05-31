package messages.protocol;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import peer.storage.StorageFile;
import tasks.protocol.BackupTask;

import javax.net.ssl.SSLSocket;

public class BackupMessage extends Message {
    private final StorageFile storageFile;
    private final byte[] fileData;

    public BackupMessage(ChordNodeReference senderReference, StorageFile storageFile, byte[] fileData) {
        super("BACKUP", senderReference);

        this.storageFile = storageFile;
        this.fileData = fileData;
    }


    @Override
    public BackupTask getTask(Peer peer, SSLSocket socket) {
        return new BackupTask(this, peer, socket);
    }

    public StorageFile getStorageFile() {
        return this.storageFile;
    }

    public byte[] getFileData() {
        return fileData;
    }
}
