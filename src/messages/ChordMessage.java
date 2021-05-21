package messages;

import chord.ChordNode;
import chord.ChordNodeReference;
import tasks.ChordTask;

import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

public abstract class ChordMessage {
    protected String messageType;
    protected ChordNodeReference senderReference;
    protected byte[] body;

    ChordMessage(String messageType, ChordNodeReference senderReference, byte[] body) {
        this.messageType = messageType;
        this.senderReference = senderReference;
        this.body = body;
    }

    public static ChordMessage create(byte[] data) throws Exception {
        String message = new String(data);
        //message = message.substring(0, Math.min(bytes.getLength(), message.length()));

        // Split Header and Body from <CRLF><CRLF>
        String[] parts = message.split("\r\n\r\n", 2);

        // Get header bytes before removing spaces so we store the real value
        int headerBytes = parts[0].length();

        // Remove trailing, leading and extra spaces between fields and split header fields
        String[] header = parts[0].trim().replaceAll("( )+", " ").split(" ");

        String messageType = header[0];
        int guid = Integer.parseInt(header[1]);
        String address = header[2];
        int port = Integer.parseInt(header[3]);

        byte[] body = new byte[0];
        if (parts.length == 2)
            body = Arrays.copyOfRange(data, headerBytes + 4, data.length);

        ChordNodeReference senderReference = new ChordNodeReference(new InetSocketAddress(address,port), guid);

        switch (messageType) {
            case "JOIN":
                return new JoinMessage(senderReference);
            case "GUID":
                return new GuidMessage(senderReference, body);
            case "LOOKUP":
                return new LookupMessage(senderReference, body);
            case "LOOKUPREPLY":
                return new LookupReplyMessage(senderReference, body);
            case "PREDECESSOR":
                return new PredecessorMessage(senderReference);
            case "PREDECESSORREPLY":
                return new PredecessorReplyMessage(senderReference, body);
            case "NOTIFY":
                return new NotifyMessage(senderReference);
            default:
                throw new Exception("Unknown message type");
        }
    }

    public abstract byte[] encode();

    public abstract ChordTask getTask(ChordNode node, SocketChannel channel, SSLEngine engine);

    public ChordNodeReference getSenderNodeReference() {
        return senderReference;
    }

    public InetSocketAddress getSenderSocketAddress() { return this.senderReference.getSocketAddress(); }

    public String getSenderHostAddress() { return this.senderReference.getSocketAddress().getAddress().getHostAddress(); }

    public int getSenderPort() { return this.senderReference.getSocketAddress().getPort(); }

    public int getSenderGuid() { return this.senderReference.getGuid(); }

    public byte[] getBody() { return this.body; }
}
