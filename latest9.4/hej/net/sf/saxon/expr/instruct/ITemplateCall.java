package net.sf.saxon.expr.instruct;

/**
 * An interface satisfied by all instructions that invoke templates: apply-templates,
 * call-template. apply-imports, next-match
 */

public interface ITemplateCall {

    /**
     * Get the actual parameters passed to the called template
     * @return the non-tunnel parameters
     */

    /*@NotNull*/
    WithParam[] getActualParams();

    /**
     * Get the tunnel parameters passed to the called template
     * @return the tunnel parameters
     */

    /*@NotNull*/
    WithParam[] getTunnelParams();
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