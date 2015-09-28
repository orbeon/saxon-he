////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.serialize;

import net.sf.saxon.event.ProxyReceiver;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.lib.SerializerFactory;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.Result;
import java.util.Properties;

/**
 * This class by default acts as a pass-through Receiver, acting as the gateway to a serialization
 * pipeline. But it is capable of dynamic reconfiguration to construct a different serialization
 * pipeline if required. This capability is invoked when xsl:result-document sets serialization
 * parameters dynamically.
 */

public class ReconfigurableSerializer extends ProxyReceiver {

    private Result destination;
    private Properties defaultOutputProperties;

    public ReconfigurableSerializer(Receiver next, Properties defaultOutputProperties, Result destination) {
        super(next);
        this.destination = destination;
        this.defaultOutputProperties = defaultOutputProperties;
    }

    public void reconfigure(Properties outputProperties, CharacterMapIndex charMapIndex) throws XPathException {
        SerializerFactory sf = getConfiguration().getSerializerFactory();
        Properties p = new Properties(defaultOutputProperties);
        for (String s : outputProperties.stringPropertyNames()) {
            p.setProperty(s, outputProperties.getProperty(s));
        }
        Receiver r = sf.getReceiver(destination, getPipelineConfiguration(), p, charMapIndex);
        setUnderlyingReceiver(r);
    }
}

