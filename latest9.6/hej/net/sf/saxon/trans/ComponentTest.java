////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.trans;


import net.sf.saxon.expr.instruct.ComponentBody;
import net.sf.saxon.expr.instruct.UserFunction;
import net.sf.saxon.om.StandardNames;
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

    public boolean matches(ComponentBody component) {
        return component.getComponentKind() == componentKind &&
                nameTest.matches(component.getObjectName()) &&
                !((componentKind == StandardNames.XSL_FUNCTION) && arity != ((UserFunction) component).getNumberOfArguments());
    }
}
