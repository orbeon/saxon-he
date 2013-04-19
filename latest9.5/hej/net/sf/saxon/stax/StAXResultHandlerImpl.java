////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.stax;

import net.sf.saxon.event.Receiver;
import net.sf.saxon.lib.StAXResultHandler;

import javax.xml.transform.Result;
import javax.xml.transform.stax.StAXResult;
import java.util.Properties;

/**
 * StAxResultHandler is a helper class
 */
public class StAXResultHandlerImpl implements StAXResultHandler {

    public Receiver getReceiver(Result result, Properties properties) {
        if(((StAXResult)result).getXMLStreamWriter() != null) {
            return  new ReceiverToXMLStreamWriter(((StAXResult) result).getXMLStreamWriter());

        }  else if(((StAXResult)result).getXMLEventWriter() != null) {} {
            throw new UnsupportedOperationException("XMLEventWriter currently not supported in saxon");
        }
    }
}

