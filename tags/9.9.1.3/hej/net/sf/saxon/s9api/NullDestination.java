package net.sf.saxon.s9api;

import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.Sink;
import net.sf.saxon.serialize.SerializationProperties;

/**
 * A NullDestination is a Destination that discards all output sent to it.
 * @since 9.9
 */
public class NullDestination extends AbstractDestination {

    @Override
    public Receiver getReceiver(PipelineConfiguration pipe, SerializationProperties params) {
        return new Sink(pipe);
    }

    @Override
    public void close() {}
}
