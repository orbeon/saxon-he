////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.trans;


import net.sf.saxon.expr.instruct.ComponentCode;
import net.sf.saxon.expr.instruct.UserFunction;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.pattern.LocalNameTest;
import net.sf.saxon.pattern.NamespaceTest;
import net.sf.saxon.pattern.QNameTest;

public class ComponentTest {

    private int componentKind;
    private QNameTest nameTest;
    private int arity;

    public ComponentTest(int componentKind, QNameTest nameTest, int arity) {
        this.componentKind = componentKind;
        this.nameTest = nameTest;
        this.arity = arity;
    }

    public int getComponentKind() {
        return componentKind;
    }

    public QNameTest getQNameTest() {
        return nameTest;
    }

    public int getArity() {
        return arity;
    }

    public boolean isPartialWildcard() {
        return nameTest instanceof LocalNameTest || nameTest instanceof NamespaceTest;
    }

    public boolean matches(ComponentCode component) {
        return (componentKind == -1 || component.getComponentKind() == componentKind) &&
                nameTest.matches(component.getObjectName()) &&
                !((componentKind == StandardNames.XSL_FUNCTION) && arity != -1 && arity != ((UserFunction) component).getArity());
    }

    public boolean equals(Object other) {
        return other instanceof ComponentTest &&
            ((ComponentTest) other).componentKind == componentKind &&
            ((ComponentTest) other).arity == arity &&
            ((ComponentTest) other).nameTest.equals(nameTest);
    }

    public int hashCode() {
        return componentKind ^ arity ^ nameTest.hashCode();
    }
}
