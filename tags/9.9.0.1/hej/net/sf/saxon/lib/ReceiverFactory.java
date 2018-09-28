////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


package net.sf.saxon.lib;

import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.serialize.SerializationProperties;

public interface ReceiverFactory {

    /**
     * Return a Receiver. Saxon calls this method to obtain a Receiver, to which it then sends
     * a sequence of events representing an XDM value. The method is intended
     * primarily for internal use, and may give poor diagnostics if used incorrectly.
     *
     * @param pipe   The pipeline configuration. This is supplied so that the destination can
     *               use information from the configuration (for example, a reference to the name pool)
     *               to construct or configure the returned Receiver.
     * @param params Serialization parameters known to the caller of the method; typically,
     *               output properties defined in a stylesheet or query. These will mainly
     *               be of interest if the destination is performing serialization, but some
     *               properties (such as {@code item-separator}) are also used in other
     *               situations. These properties are typically subordinate to any properties
     *               defined on the (serializer) destination itself: for example if {@code indent=yes}
     *               was explicitly specified on a {@code Serializer}, this takes precedence
     *               over {@code indent=no} defined in a query or stylesheet.
     * @return the Receiver to which events are to be sent. This should be initialized so that
     * {@code receiver.getPipelineConfiguration()} returns the supplied {@code PipelineConfiguration}.
     * <p>The {@code Receiver} is expected to handle a <b>regular event sequence</b> as defined in
     * {@link net.sf.saxon.event.RegularSequenceChecker}. It is the caller's responsibility to
     * ensure that the sequence of calls to the {@code Receiver} satisfies these rules, and it
     * is the responsibility of the implementation to accept any sequence conforming these rules;
     * the implementation is not expected to check that the sequence is valid, but it can do so
     * if it wishes by inserting a {@link net.sf.saxon.event.RegularSequenceChecker} into the pipeline.</p>
     * <p>The sequence of events passed to the {@code Receiver} represents the <b>raw results</b> of
     * the query or transformation. If the destination is to perform sequence normalization,
     * this is typically done by returning a {@link net.sf.saxon.event.SequenceNormalizer} as the
     * result of this method.</p>
     * <p>The returned {@code Receiver} is responsible for ensuring that when its {@link Receiver#close()}
     * method is called, this results in all registered {@code onClose} actions being invoked.
     * An implementation returning a {@code SequenceNormalizer} can achieve this by registering
     * the actions with the {@link net.sf.saxon.event.SequenceNormalizer#onClose} method.</p>
     * @throws SaxonApiException if the Receiver cannot be created
     */

    Receiver getReceiver(PipelineConfiguration pipe, SerializationProperties params) throws SaxonApiException;

}

