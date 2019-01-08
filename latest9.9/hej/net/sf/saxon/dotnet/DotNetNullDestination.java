package net.sf.saxon.dotnet;

import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.Sink;
import net.sf.saxon.s9api.Action;
import net.sf.saxon.s9api.Destination;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.serialize.SerializationProperties;

import java.net.URI;

/**
 * A NullDestination is a Destination that constructs an XmlDocument, the .NET implementation for Null Destination. The getReceiver returns a Sink
 * which disregards all information passed to it.
  *  @since 9.9
 */
public class DotNetNullDestination implements Destination {

    URI baseUri = null;

    @Override
    public void setDestinationBaseURI(URI baseURI) {
         baseUri = baseURI;
    }

    @Override
    public URI getDestinationBaseURI() {
        return baseUri;
    }

    @Override
    public Receiver getReceiver(PipelineConfiguration pipe, SerializationProperties params) throws SaxonApiException {
        return new Sink(pipe);
    }

    @Override
    public void onClose(Action listener) {

    }

    @Override
    public void closeAndNotify() throws SaxonApiException {

    }

    @Override
    public void close() throws SaxonApiException {

    }
}
