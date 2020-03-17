////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2020 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.pull;

import net.sf.saxon.om.*;
import net.sf.saxon.s9api.Location;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.value.AtomicValue;

import java.util.List;

/**
 * PullProvider is Saxon's pull-based interface for reading XML documents and XDM sequences.
 * A PullProvider can deliver any sequence of nodes or atomic values.
 *
 * <p>An {@code UnfailingPullProvider} is a subtype where the methods throw no checked Exceptions.</p>
 */

public interface UnfailingPullProvider extends PullProvider {

    Event next() throws XPathException;

    Event current();

    AttributeMap getAttributes();

    NamespaceBinding[] getNamespaceDeclarations();

    Event skipToMatchingEnd();

    void close();

    NodeName getNodeName();

    CharSequence getStringValue() throws XPathException;

    SchemaType getSchemaType();

    AtomicValue getAtomicValue();

    Location getSourceLocator();

    List<UnparsedEntity> getUnparsedEntities();

}

