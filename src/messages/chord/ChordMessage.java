package messages.chord;

import chord.ChordNodeReference;
import messages.Message;

public abstract class ChordMessage extends Message {
    public ChordMessage(String action, ChordNodeReference senderReference, byte[] body) {
        super("CHORD", action, senderReference, body);
    }

    public ChordMessage(String action, ChordNodeReference senderReference) {
        super("CHORD", action, senderReference);
    }

    public static ChordMessage create(String action, ChordNodeReference senderReference, byte[] body) throws Exception {
        switch (action) {
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
                throw new Exception("Unknown Chord action");
        }
    }
}
