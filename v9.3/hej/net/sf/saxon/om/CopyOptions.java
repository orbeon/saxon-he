package net.sf.saxon.om;

import net.sf.saxon.event.ReceiverOptions;

/**
 * Non-instantiable class to define options for the {@link NodeInfo#copy} method
 */
public abstract class CopyOptions {

    public static final int LOCAL_NAMESPACES = 1;

    public static final int ALL_NAMESPACES = 2;

    public static final int SOME_NAMESPACES = LOCAL_NAMESPACES | ALL_NAMESPACES;

    public static final int TYPE_ANNOTATIONS = 4;

    public static final int FOR_UPDATE = 8;

    public static boolean includes(int options, int option) {
        return (options & option) == option;
    }

    public static int getStartDocumentProperties(int copyOptions) {
        int properties = 0;
        if (CopyOptions.includes(copyOptions, CopyOptions.FOR_UPDATE)) {
            properties |= ReceiverOptions.MUTABLE_TREE;
        }
        return properties;
    }
}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay,
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//



