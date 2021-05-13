package messages.chord;

import chord.ChordNodeReference;
import chord.Peer;

import javax.net.ssl.SSLSocket;
import java.net.InetSocketAddress;

public abstract class ChordMessage {
    protected String messageType;
    protected ChordNodeReference node;

    ChordMessage(String messageType, ChordNodeReference node) {
        this.messageType = messageType;
        this.node = node;
    }

    public static ChordMessage decode(byte[] bytes) throws Exception {
        String message = new String(bytes);
        //message = message.substring(0, Math.min(bytes.getLength(), message.length()));

        // Split Header and Body from <CRLF><CRLF>
        String[] parts = message.split("\r\n\r\n", 2);

        // Get header bytes before removing spaces so we store the real value
        int headerBytes = parts[0].length();

        // Remove trailing, leading and extra spaces between fields and split header fields
        String[] header = parts[0].trim().replaceAll("( )+", " ").split(" ");

        String messageType = header[0];
        int requestedId = Integer.parseInt(header[1]);
        String address = header[2];
        int port = Integer.parseInt(header[3]);

        /*byte[] body = new byte[0];
        if (parts.length == 2)
            body = Arrays.copyOfRange(packet.getData(), headerBytes + 4, packet.getLength());*/

        switch (messageType) {
            case "JOIN":
                return new JoinMessage(new ChordNodeReference(new InetSocketAddress(address,port), requestedId));
            default:
                throw new Exception("Unknown message type");
        }
    }

    public abstract byte[] encode();

    public abstract void submitTask(Peer peer);

    public ChordNodeReference getNodeReference() {
        return node;
    }

    public InetSocketAddress getSocketAddress() { return this.node.getSocketAddress(); }

    public int getPort() { return this.node.getSocketAddress().getPort(); }

    public int getId() { return this.node.getId(); }

}
