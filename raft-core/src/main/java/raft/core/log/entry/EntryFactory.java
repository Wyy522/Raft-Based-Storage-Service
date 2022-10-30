package raft.core.log.entry;


public class EntryFactory {

    public Entry create(int kind, int index, int term, byte[] commandBytes) {

        switch (kind) {
            case Entry.Kind_NO_OP:
                return new NoOpEntry(index, term);
            case Entry.Kind_GENERAL:
                return new GeneralEntry(index, term, commandBytes);
            default:
                throw new IllegalArgumentException("unexpected entry kind " + kind);
        }
    }
}
