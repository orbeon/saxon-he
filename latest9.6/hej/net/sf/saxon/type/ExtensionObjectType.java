////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.type;

import net.sf.saxon.om.StructuredQName;

/**
 * This class represents the type of an external object returned by
 * an extension function, or supplied as an external variable/parameter.
 */
public abstract class ExtensionObjectType {


    public abstract String getName();

    public abstract String getTargetNamespace();

    public boolean isExternalType(){
        return true;
    }

    public abstract StructuredQName getTypeName();

    public abstract boolean isPlainType();

    public  abstract int getPrimitiveType();




}
