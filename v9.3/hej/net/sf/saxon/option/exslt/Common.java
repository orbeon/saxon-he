package net.sf.saxon.option.exslt;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.Value;
import net.sf.saxon.om.ValueRepresentation;

/**
* This class implements extension functions in the
* http://exslt.org/common namespace. <p>
*/



public abstract class Common  {

    /**
     * Class is not instantiated
     */
    private Common() {
    }

    /**
    * Convert a result tree fragment to a node-set. This is a hangover from XSLT 1.0;
    * it is implemented as a no-op.
    */

    public static ValueRepresentation nodeSet(ValueRepresentation frag) {
        return frag;
    }

    /**
    * Return the type of the supplied value: "sequence", "string", "number", "boolean",
    * "external". (EXSLT spec not yet modified to cater for XPath 2.0 data model)
    */

    public static String objectType(XPathContext context, ValueRepresentation value) {
        final TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
        ItemType type = Value.asValue(value).getItemType(th);
        if (th.isSubType(type, AnyNodeTest.getInstance())) {
            return "node-set";
        } else if (th.isSubType(type, BuiltInAtomicType.STRING)) {
            return "string";
        } else if (th.isSubType(type, BuiltInAtomicType.NUMERIC)) {
            return "number";
        } else if (th.isSubType(type, BuiltInAtomicType.BOOLEAN)) {
            return "boolean";
        } else {
            return type.toString(context.getNamePool());
        }
    }


}

// Copyright (c) Saxonica Limited 2009. All rights reserved.