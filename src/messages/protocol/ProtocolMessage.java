package messages.protocol;

import chord.ChordNodeReference;
import messages.Message;
import messages.chord.*;

public abstract class ProtocolMessage extends Message {
    public ProtocolMessage(String action, ChordNodeReference senderReference, byte[] body) {
        super("PROTOCOL", action, senderReference, body);
    }

    public ProtocolMessage(String action, ChordNodeReference senderReference) {
        super("PROTOCOL", action, senderReference);
    }

    public static ProtocolMessage create(String action, ChordNodeReference senderReference, byte[] body) throws Exception {
        switch (action) {
            case "BACKUP":
                return new BackupMessage(senderReference, body);
            case "OK":
                return new OkMessage(senderReference);
            case "ERROR":
                return new ErrorMessage(senderReference, body);
            default:
                throw new Exception("Unknown Protocol action");
        }
    }
}
