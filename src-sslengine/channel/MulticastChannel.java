package channel;

import messages.Message;
import peer.Peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastChannel extends MulticastSocket implements Runnable {
    private final InetAddress address;
    private final int port;
    private final Peer peer;

    public MulticastChannel(String addressString, int port, Peer peer) throws IOException {
        // Create Socket
        super(port);

        this.address = InetAddress.getByName(addressString);
        this.port = port;
        this.peer = peer;
        this.setTimeToLive(1);
        this.joinGroup(this.address);
    }

    @Override
    public void run() {
        while (true) {
            byte[] buf = new byte[65000];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            try {
                this.receive(packet);

                // Create respective message from received packet
                Message message = Message.create(packet);

                // Process message task if it was not sent by this peer
                if (!message.messageOwner(this.peer.getId())) {
                    message.submitTask(this.peer);
                }

            } catch (Exception e) {
                System.err.println("Error receiving DatagramPacket: " + e.getMessage());
            }
        }
    }

    public void sendMessage(byte[] message) {
        DatagramPacket packet = new DatagramPacket(message, message.length, this.address, this.port);

        try {
            this.send(packet);
        } catch (IOException e) {
            System.err.println("Error sending DatagramPacket");
        }
    }
}
