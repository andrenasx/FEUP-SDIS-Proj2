package tasks.chord;

import chord.ChordNodeReference;
import chord.Peer;
import messages.chord.ChordMessage;
import messages.chord.JoinMessage;
import utils.Utils;

import java.net.InetSocketAddress;

public class JoinTask extends Task{
    public JoinTask(Peer peer, JoinMessage message) {
        super(peer, message);
    }

    @Override
    public void run() {
        if (message.getId() == -1) { //handle id request
            InetSocketAddress socketAddress = message.getSocketAddress();

            int idToSend = Utils.generateId(socketAddress);

            ChordMessage message = new JoinMessage(new ChordNodeReference(socketAddress, idToSend));

            //Send join with new id
            peer.send_message(message, peer.getClientSocket());
        }
        else { //handle reply
            peer.setNodeId(message.getId());
            //sends lookup request to fin successor
        }
    }
}
