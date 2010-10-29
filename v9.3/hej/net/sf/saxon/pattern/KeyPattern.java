package net.sf.saxon.pattern;

import net.sf.saxon.expr.*;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.KeyDefinitionSet;
import net.sf.saxon.trans.KeyManager;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.Configuration;

/**
 * A KeyPattern is a pattern of the form key(keyname, keyvalue)
 */

public final class KeyPattern extends NodeSetPattern {

    private StructuredQName keyName;     // the key name
    private KeyDefinitionSet keySet;     // the set of keys corresponding to the key name

    /**
     * Constructor
     *
     * @param keyName the name of the key
     * @param key     the value of the key: either a StringValue or a VariableReference
     */

    public KeyPattern(StructuredQName keyName, Expression key, Configuration config) {
        super(key, config);
        this.keyName = keyName;
    }

    /**
     * Type-check the pattern. This is needed for patterns that contain
     * variable references or function calls.
     *
     * @return the optimised Pattern
     */

    public Pattern analyze(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        expression = visitor.typeCheck(expression, contextItemType);
        RoleLocator role = new RoleLocator(RoleLocator.FUNCTION, "key", 2);
        expression = TypeChecker.staticTypeCheck(expression, SequenceType.ATOMIC_SEQUENCE, false, role, visitor);
        keySet = visitor.getExecutable().getKeyManager().getKeyDefinitionSet(keyName);
        if (keySet == null) {
            XPathException err = new XPathException("Unknown key name " + keyName.getClarkName() + " in pattern");
            err.setErrorCode("XTDE1260");
            err.setLocator(this);
            err.setIsStaticError(true);
            throw err;
        }
        return this;
    }

    /**
     * Determine whether this Pattern matches the given Node.
     *
     * @param e The NodeInfo representing the Element or other node to be tested against the Pattern
     * @return true if the node matches the Pattern, false otherwise
     */

    public boolean matches(NodeInfo e, XPathContext context) throws XPathException {
        KeyDefinitionSet kds = keySet;
        if (kds == null) {
            // shouldn't happen
            kds = context.getController().getExecutable().getKeyManager().getKeyDefinitionSet(keyName);
        }
        DocumentInfo doc = e.getDocumentRoot();
        if (doc == null) {
            return false;
        }
        KeyManager km = context.getController().getKeyManager();
        SequenceIterator iter = expression.iterate(context);
        while (true) {
            Item it = iter.next();
            if (it == null) {
                return false;
            }
            SequenceIterator nodes = km.selectByKey(kds, doc, (AtomicValue)it, context);
            while (true) {
                NodeInfo n = (NodeInfo)nodes.next();
                if (n == null) {
                    break;
                }
                if (n.isSameNodeInfo(e)) {
                    return true;
                }
            }
        }
    }

    /**
     * Get a NodeTest that all the nodes matching this pattern must satisfy
     */

    public NodeTest getNodeTest() {
        return AnyNodeTest.getInstance();
    }

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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
