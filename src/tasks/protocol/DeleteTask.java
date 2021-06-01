package tasks.protocol;

import messages.protocol.BackupMessage;
import messages.protocol.DeleteMessage;
import messages.protocol.ErrorMessage;
import messages.protocol.OkMessage;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;
import java.io.FileOutputStream;

public class DeleteTask extends Task{

        public DeleteTask(DeleteMessage message, Peer peer, SSLSocket socket) {
            super(message, peer, socket);
        }

        @Override
        public void run() {
            DeleteMessage deleteMessage = (DeleteMessage) message;

            try {
                if (peer.getPeerStorage().hasStoredFile(deleteMessage.getFileId())) {
                    OkMessage okay = new OkMessage(peer.getSelfReference());
                    try{
                        peer.getPeerStorage().deleteFile(peer, deleteMessage.getFileId());
                        peer.sendMessage(socket, okay);
                    }catch(Exception e){
                        ErrorMessage error = new ErrorMessage(peer.getSelfReference(), "NOTDELETED");
                        peer.sendMessage(socket, error);
                    }
                }
                else {
                    ErrorMessage error = new ErrorMessage(peer.getSelfReference(), "NOTFOUND");
                    peer.sendMessage(socket, error);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

