package net.sf.saxon.expr;

/**
 * MappingFunction is an interface that must be satisfied by an object passed to a
 * MappingIterator. StatefulMappingFunction is a sub-interface representing a mapping
 * function that maintains state information, and which must therefore be cloned
 * when the mapping iterator is cloned.
*/

public interface StatefulMappingFunction extends MappingFunction {

    /**
     * Return a clone of this MappingFunction, with the state reset to its state at the beginning
     * of the underlying iteration
     * @return a clone of this MappingFunction
     */

    public StatefulMappingFunction getAnother();

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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

