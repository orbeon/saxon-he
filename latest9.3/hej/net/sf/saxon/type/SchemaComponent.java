package net.sf.saxon.type;

import java.io.Serializable;

/**
 * This is a marker interface that represents any "schema component" as defined in the XML Schema
 * specification. This may be a user-defined schema component or a built-in schema component. Since
 * all built-in schema components are types, every SchemaComponent in practice is either a
 * {@link com.saxonica.schema.UserSchemaComponent} or a {@link SchemaType} or both.
 */
public interface SchemaComponent extends Serializable {

    /**
     * Get the validation status of this component.
     * @return one of the values {@link #UNVALIDATED}, {@link #VALIDATING},
     * {@link #VALIDATED}, {@link #INVALID}, {@link #INCOMPLETE}
     */

    public int getValidationStatus();

    /**
     * Validation status: not yet validated
     */
    public static final int UNVALIDATED = 0;

    /**
     * Validation status: fixed up (all references to other components have been resolved)
     */
    public static final int FIXED_UP = 1;

    /**
     * Validation status: currently being validated
     */
    public static final int VALIDATING = 2;

    /**
     * Validation status: successfully validated
     */
    public static final int VALIDATED = 3;

    /**
     * Validation status: validation attempted and failed with fatal errors
     */
    public static final int INVALID = 4;

    /**
     * Validation status: validation attempted, component contains references to
     * other components that are not (yet) available
     */
    public static final int INCOMPLETE = 5;

    /**
     * Get the redefinition level. This is zero for a component that has not been redefined;
     * for a redefinition of a level-0 component, it is 1; for a redefinition of a level-N
     * component, it is N+1. This concept is used to support the notion of "pervasive" redefinition:
     * if a component is redefined at several levels, the top level wins, but it is an error to have
     * two versions of the component at the same redefinition level.
     * @return the redefinition level
     */

    public int getRedefinitionLevel();


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
// The Initial Developer of the Original Code is Saxonica Limited
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//