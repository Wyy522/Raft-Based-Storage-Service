package raft.core.log;

import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.core.log.sequence.EntrySequence;
import raft.core.log.sequence.memory.MemoryEntrySequence;

public class MemoryLog extends AbstractLog{


    private static final Logger logger = LoggerFactory.getLogger(MemoryLog.class);

    public MemoryLog() {
        this(new EventBus());
    }

    public MemoryLog(EventBus eventBus) {
        this(new MemoryEntrySequence(), eventBus);
    }

    public MemoryLog( EntrySequence entrySequence, EventBus eventBus) {
        super(eventBus);
        this.entrySequence = entrySequence;

    }

}
