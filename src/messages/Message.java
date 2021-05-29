package messages;

import chord.ChordNode;
import chord.ChordNodeReference;
import messages.chord.ChordMessage;
import messages.protocol.ProtocolMessage;
import tasks.Task;

import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

//<TYPE> <PROTOCOL/OPERATION> <SENDER GUID> <SENDER ADDRESS IP> <SENDER PORT> <\r\n\r\n> <BODY>
public abstract class Message {
    protected final String type;
    protected final String action;
    protected ChordNodeReference senderReference;
    protected byte[] body;

    public Message(String type, String action, ChordNodeReference senderReference, byte[] body) {
        this.type = type;
        this.action = action;
        this.senderReference = senderReference;
        this.body = body;
    }

    public Message(String type, String action, ChordNodeReference senderReference) {
        this.type = type;
        this.action = action;
        this.senderReference = senderReference;
        this.body = new byte[0];
    }

    public static Message create(byte[] data) throws Exception {
        String message = new String(data);

        // Split Message header and rest of the message from <CRLF><CRLF>
        String[] parts = message.split("\r\n\r\n", 2);

        // Get header bytes before removing spaces so we store the real value
        int headerBytes = parts[0].length();

        // Remove trailing, leading and extra spaces between fields and split header fields
        String[] header = parts[0].trim().replaceAll("( )+", " ").split(" ");

        String type = header[0];
        String action = header[1];
        int guid = Integer.parseInt(header[2]);
        String address = header[3];
        int port = Integer.parseInt(header[4]);

        byte[] body = new byte[0];
        if (parts.length == 2)
            body = Arrays.copyOfRange(data, headerBytes + 4, data.length);

        ChordNodeReference senderReference = new ChordNodeReference(new InetSocketAddress(address, port), guid);

        switch (type) {
            case "CHORD":
                return ChordMessage.create(action, senderReference, body);
            case "PROTOCOL":
                return ProtocolMessage.create(action, senderReference, body);
            default:
                throw new Exception("Unknown Message type");
        }
    }

    public abstract byte[] encode();

    public abstract Task getTask(ChordNode node, SocketChannel channel, SSLEngine engine);

    public ChordNodeReference getSenderNodeReference() {
        return senderReference;
    }

    public InetSocketAddress getSenderSocketAddress() {
        return this.senderReference.getSocketAddress();
    }

    public String getSenderHostAddress() {
        return this.senderReference.getSocketAddress().getAddress().getHostAddress();
    }

    public int getSenderPort() {
        return this.senderReference.getSocketAddress().getPort();
    }

    public int getSenderGuid() {
        return this.senderReference.getGuid();
    }

    public byte[] getBody() {
        return this.body;
    }
}
