package net.sf.saxon.dotnet;

import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.s9api.AbstractDestination;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.serialize.SerializationProperties;

/**
 * DotNetDomDestination is a Destination that constructs an XmlDocument, the .NET implementation of a DOM
 *  @since 9.9
 */
public class DotNetDomDestination extends AbstractDestination {

    private DotNetDomBuilder builder;


    public DotNetDomDestination(DotNetDomBuilder builder){
        this.builder = builder;
    }

    @Override
    public Receiver getReceiver(PipelineConfiguration pipe, SerializationProperties params) throws SaxonApiException {
        builder.setPipelineConfiguration(pipe);
        return builder;
    }

    @Override
    public void close() throws SaxonApiException {
        // no action
    }
}
