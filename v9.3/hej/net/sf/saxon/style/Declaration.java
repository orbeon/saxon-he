package net.sf.saxon.style;

/**
 * The object represents a declaration (that is, a top-level element) in a stylesheet.
 * A declaration exists within a stylesheet module and takes its import precedence
 * from that of the module. The declaration corresponds to a source element in a stylesheet
 * document. However, if a stylesheet module is imported twice with different precedences,
 * then two declarations may share the same source element.
 */

public class Declaration {

    private StyleElement sourceElement;
    private StylesheetModule module;

    public Declaration(StylesheetModule module, StyleElement source) {
        this.module = module;
        this.sourceElement = source;
    }

    public StylesheetModule getModule() {
        return module;
    }

    public StyleElement getSourceElement() {
        return sourceElement;
    }

    public int getPrecedence() {
        return module.getPrecedence();
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



