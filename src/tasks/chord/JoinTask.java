package tasks.chord;

import chord.ChordNodeReference;
import messages.chord.GuidMessage;
import messages.chord.JoinMessage;
import peer.Peer;
import tasks.Task;
import utils.Utils;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetSocketAddress;

public class JoinTask extends Task {
    public JoinTask(JoinMessage message, Peer peer, SSLSocket socket) {
        super(message, peer, socket);
    }

    @Override
    public void run() {
        InetSocketAddress socketAddress = message.getSenderSocketAddress();

        int guidToSend = Utils.generateId(socketAddress);

        //finds successor of new id
        ChordNodeReference successor = this.peer.findSuccessor(guidToSend);

        while (successor.getGuid() == guidToSend) {
            System.out.println("oops... " + guidToSend + " already exists");
            guidToSend = (guidToSend + 1) % (Utils.CHORD_MAX_PEERS);
            successor = this.peer.findSuccessor(guidToSend);
        }

        //updates boot peer successor
        if (this.peer.between(guidToSend, this.peer.getSelfReference().getGuid(), this.peer.getSuccessor().getGuid(), false))
            this.peer.setSuccessor(new ChordNodeReference(socketAddress, guidToSend));

        GuidMessage response = new GuidMessage(peer.getSelfReference(), guidToSend, successor);

        try {
            peer.sendMessage(socket, response);
            //System.out.println("Server sent: " + response);
        } catch (IOException e) {
            System.err.println("[ERROR-CHORD] Couldn't send GUID");
            e.printStackTrace();
        }
    }
}
