package tasks;

import chord.ChordNode;
import chord.ChordNodeReference;
import messages.GuidMessage;
import messages.JoinMessage;
import utils.Utils;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class JoinTask extends ChordTask {
    public JoinTask(JoinMessage message, ChordNode node, SocketChannel channel, SSLEngine engine) {
        super(message, node, channel, engine);
    }

    @Override
    public void run() {
        InetSocketAddress socketAddress = message.getSenderSocketAddress();

        int guidToSend = Utils.generateId(socketAddress);

        //finds successor of new id
        ChordNodeReference successor = this.node.findSuccessor(guidToSend);

        while(successor.getGuid() == guidToSend){
            System.out.println("oops... " + guidToSend + " already exists");
            guidToSend = (guidToSend + 1) % (2^Utils.CHORD_M);
            successor = this.node.findSuccessor(guidToSend);
        }

        //updates boot peer successor
        if(this.node.between(guidToSend,this.node.getSelfReference().getGuid(),this.node.successor().getGuid(), false))
            this.node.setChordNodeReference(1,new ChordNodeReference(socketAddress,guidToSend));

        GuidMessage response = new GuidMessage(node.getSelfReference(), (guidToSend + " " + successor).getBytes(StandardCharsets.UTF_8));

        try {
            node.write(channel, engine, response.encode());
            System.out.println("Server sent: " + response);
        } catch (IOException e) {
            System.err.println("Couldn't send GUID");
            e.printStackTrace();
        }
    }
}
