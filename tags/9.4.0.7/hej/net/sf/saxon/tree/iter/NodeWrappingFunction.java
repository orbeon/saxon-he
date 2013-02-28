package net.sf.saxon.tree.iter;

import net.sf.saxon.om.NodeInfo;

/**
 * Interface to a function that wraps nodes from an external object model in a Saxon NodeInfo
 * representation
 * @param <B> the type of the node in the external object model
 * @param <T> the type of the Saxon node
 *
 */

public interface NodeWrappingFunction<B, T extends NodeInfo> {

    public T wrap(B node);
}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//