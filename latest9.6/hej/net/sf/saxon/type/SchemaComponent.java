////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.type;

import net.sf.saxon.om.FunctionItem;
import net.sf.saxon.value.SequenceType;

import java.io.Serializable;

/**
 * This is a marker interface that represents any "schema component" as defined in the XML Schema
 * specification. This may be a user-defined schema component or a built-in schema component. Since
 * all built-in schema components are types, every SchemaComponent in practice is either a
 * {@link com.saxonica.ee.schema.UserSchemaComponent} or a {@link SchemaType} or both.
 */
public interface SchemaComponent {

    /**
     * Get the validation status of this component.
     *
     * @return one of the values {@link #UNVALIDATED}, {@link #VALIDATING},
     *         {@link #VALIDATED}, {@link #INVALID}, {@link #INCOMPLETE}
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
     *
     * @return the redefinition level
     */

    public int getRedefinitionLevel();

//#ifdefined SCHEMA

    /**
     * Get the schema component in the form of a function item. This allows schema information
     * to be made visible to XSLT or XQuery code. The function makes available the contents of the
     * schema component as defined in the XSD specification. The function takes a string as argument
     * representing a property name, and returns the corresponding property of the schema component.
     * There is also a property "class" which returns the kind of schema component, for example
     * "Attribute Declaration".
     *
     * @return the schema component represented as a function from property names to property values.
     */

    public FunctionItem getComponentAsFunction();


    /**
     * The function type of the function returned by getComponentAsFunction()
     */

    public final static FunctionItemType COMPONENT_FUNCTION_TYPE =
            new com.saxonica.functions.hof.SpecificFunctionType(new SequenceType[]{SequenceType.SINGLE_STRING}, SequenceType.ANY_SEQUENCE);
//#endif

}

