package messages.chord;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.chord.NotifyTask;

import javax.net.ssl.SSLSocket;


//CHORD NOTIFY <SENDER GUID> <SENDER ADDRESS IP> <SENDER PORT> \r\n\r\n
public class NotifyMessage extends Message {
    public NotifyMessage(ChordNodeReference senderReference) {
        super(senderReference);
    }

    @Override
    public NotifyTask getTask(Peer peer, SSLSocket socket) {
        return new NotifyTask(this, peer, socket);
    }

    @Override
    public String toString() {
        return "NotifyMessage {" +
                "sender=" + this.getSenderNodeReference() +
                '}';
    }
}
