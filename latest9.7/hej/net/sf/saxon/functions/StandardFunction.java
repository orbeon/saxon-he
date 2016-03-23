////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.OperandUsage;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.ma.json.JsonDoc;
import net.sf.saxon.ma.json.JsonToXMLFn;
import net.sf.saxon.ma.json.ParseJsonFn;
import net.sf.saxon.ma.json.XMLToJsonFn;
import net.sf.saxon.ma.map.MapType;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.type.*;
import net.sf.saxon.value.*;
import net.sf.saxon.value.StringValue;

import java.util.HashMap;

/**
 * This class contains static data tables defining the properties of standard functions. "Standard functions"
 * here means the XPath 2.0 functions, the XSLT 2.0 functions, and a few selected extension functions
 * which need special recognition.
 */

public abstract class StandardFunction {

    public static Sequence EMPTY = EmptySequence.getInstance();

    /**
     * Categories of functions, bit significant
     */

    public static final int CORE = 1;
    public static final int XSLT = 2;
    public static final int USE_WHEN = 4;
    public static final int XQUPDATE = 8;
    public static final int XPATH30 = 16;
    public static final int INTERNAL = 32;
    public static final int XSLT30 = 64;
    public static final int XPATH31 = 128;
    public static final int HOF = 256;

    /**
     * Local names used for cardinality values
     */

    public static final int ONE = StaticProperty.ALLOWS_ONE;
    public static final int OPT = StaticProperty.ALLOWS_ZERO_OR_ONE;
    public static final int STAR = StaticProperty.ALLOWS_ZERO_OR_MORE;
    public static final int PLUS = StaticProperty.ALLOWS_ONE_OR_MORE;

    /**
     * Function properties
     */

    public static final int AS_ARG0 = 1;          // Result has same type as first argument
    public static final int AS_PRIM_ARG0 = 2;     // Result has same primitive type as first argument
    public static final int CITEM = 4;            // Depends on context item
    public static final int BASE = 8;             // Depends on base URI
    public static final int NS = 16;              // Depends on namespace context
    public static final int DCOLL = 32;           // Depends on default collation
    public static final int DLANG = 64;           // Depends on default language
    public static final int FILTER = 256;         // Result is a subset of the value of the first arg
    public static final int LATE = 512;           // Disallow compile-time evaluation
    public static final int UO = 1024;            // Ordering in first argument is irrelevant
    public static final int POSN = 2048;          // Depends on position
    public static final int LAST = 4096;          // Depends on last

    public static final int DEPENDS_ON_STATIC_CONTEXT = BASE | NS | DCOLL;
    public static final int FOCUS = CITEM | POSN | LAST;


    /**
     * Classification of function arguments for serialization purposes; note that values must not conflict
     * with bit settings used for cardinalities
     */

    public static final int INS = 1 << 24;   // = usage INSPECTION
    public static final int ABS = 1 << 25;   // = usage ABSORPTION (implicit when type is atomic)
    public static final int TRA = 1 << 26;   // = usage TRANSMISSION (node is included in function result)
    public static final int NAV = 1 << 27;   // = usage NAVIGATION (function navigates from this node)

    /**
     * This class is never instantiated
     */

    private StandardFunction() {
    }

    /**
     * Register a system function in the table of function details.
     *
     * @param name                the function name
     * @param implementationClass the class used to implement the function
     * @param minArguments        the minimum number of arguments required
     * @param maxArguments        the maximum number of arguments allowed
     * @param itemType            the item type of the result of the function
     * @param cardinality         the cardinality of the result of the function
     * @param applicability       the host languages (and versions thereof) in which this function is available
     * @param properties          bitwise properties of the function
     * @return the entry describing the function. The entry is incomplete, it does not yet contain information
     *         about the function arguments.
     */

    /*@NotNull*/
    public static Entry register(String name,
                                 Class implementationClass,
                                 int minArguments,
                                 int maxArguments,
                                 ItemType itemType,
                                 int cardinality,
                                 int applicability,
                                 int properties) {
        Entry e = makeEntry(name, implementationClass, minArguments, maxArguments,
                itemType, cardinality, applicability, properties);
        functionTable.put(name, e);
        return e;
    }

    /**
     * Make a table entry describing the signature of a function, with a reference to the implementation class.
     *
     * @param name                the function name
     * @param implementationClass the class used to implement the function
     * @param minArguments        the minimum number of arguments required
     * @param maxArguments        the maximum number of arguments allowed
     * @param itemType            the item type of the result of the function
     * @param cardinality         the cardinality of the result of the function
     * @param applicability       the host languages (and versions of) in which this function is available
     * @return the entry describing the function. The entry is incomplete, it does not yet contain information
     *         about the function arguments.
     */
    public static Entry makeEntry(String name, Class implementationClass,
                                  int minArguments, int maxArguments,
                                  ItemType itemType, int cardinality, int applicability, int properties) {
        Entry e = new Entry();
        int hash = name.indexOf('#');
        if (hash < 0) {
            e.name = name;
        } else {
            e.name = name.substring(0, hash);
        }
        e.implementationClass = implementationClass;
        e.minArguments = minArguments;
        e.maxArguments = maxArguments;
        e.itemType = itemType;
        e.cardinality = cardinality;
        e.applicability = applicability;
        e.properties = properties;
        if (maxArguments > 100) {
            // special case for concat()
            e.argumentTypes = new SequenceType[1];
            e.resultIfEmpty = new AtomicValue[1];
            e.usage = new OperandUsage[1];
        } else {
            e.argumentTypes = new SequenceType[maxArguments];
            e.resultIfEmpty = new Sequence[maxArguments];
            e.usage = new OperandUsage[maxArguments];
        }
        return e;
    }


    private static HashMap<String, Entry> functionTable = new HashMap<String, Entry>(200);



    static {
        register("abs", Abs.class, 1, 1, NumericType.getInstance(), OPT, CORE, AS_PRIM_ARG0)
                .arg(0, NumericType.getInstance(), OPT, EMPTY);

        register("adjust-date-to-timezone#1", Adjust_1.class, 1, 1, BuiltInAtomicType.DATE, OPT, CORE, LATE)
                .arg(0, BuiltInAtomicType.DATE, OPT, EMPTY);

        register("adjust-date-to-timezone#2", Adjust_2.class, 2, 2, BuiltInAtomicType.DATE, OPT, CORE, 0)
            .arg(0, BuiltInAtomicType.DATE, OPT, EMPTY)
            .arg(1, BuiltInAtomicType.DAY_TIME_DURATION, OPT, null);

        register("adjust-dateTime-to-timezone#1", Adjust_1.class, 1, 1, BuiltInAtomicType.DATE_TIME, OPT, CORE, LATE)
                .arg(0, BuiltInAtomicType.DATE_TIME, OPT, EMPTY);

        register("adjust-dateTime-to-timezone#2", Adjust_2.class, 2, 2, BuiltInAtomicType.DATE_TIME, OPT, CORE, 0)
            .arg(0, BuiltInAtomicType.DATE_TIME, OPT, EMPTY)
            .arg(1, BuiltInAtomicType.DAY_TIME_DURATION, OPT, null);

        register("adjust-time-to-timezone#1", Adjust_1.class, 1, 1, BuiltInAtomicType.TIME, OPT, CORE, LATE)
                .arg(0, BuiltInAtomicType.TIME, OPT, EMPTY);

        register("adjust-time-to-timezone#2", Adjust_2.class, 2, 2, BuiltInAtomicType.TIME, OPT, CORE, 0)
            .arg(0, BuiltInAtomicType.TIME, OPT, EMPTY)
            .arg(1, BuiltInAtomicType.DAY_TIME_DURATION, OPT, null);

        register("analyze-string#2", RegexFunctionSansFlags.class, 2, 2, NodeKindTest.ELEMENT,
            ONE, StandardFunction.XPATH30, LATE)
            .arg(0, BuiltInAtomicType.STRING, OPT, null)
            .arg(1, BuiltInAtomicType.STRING, ONE, null);

        register("analyze-string#3", AnalyzeStringFn.class, 3, 3, NodeKindTest.ELEMENT,
                ONE, StandardFunction.XPATH30, LATE)
                .arg(0, BuiltInAtomicType.STRING, OPT, null)
                .arg(1, BuiltInAtomicType.STRING, ONE, null)
                .arg(2, BuiltInAtomicType.STRING, ONE, null);

        register("available-environment-variables", AvailableEnvironmentVariables.class, 0, 0, BuiltInAtomicType.STRING,
                 STAR, XPATH30 | USE_WHEN, LATE);

        register("available-system-properties", AvailableSystemProperties.class, 0, 0, BuiltInAtomicType.QNAME,
                 STAR, XSLT30 | USE_WHEN, LATE);

        register("avg", Average.class, 1, 1, BuiltInAtomicType.ANY_ATOMIC, OPT, CORE, UO)
                // can't say "same as first argument" because the avg of a set of integers is decimal
                .arg(0, BuiltInAtomicType.ANY_ATOMIC, STAR, EMPTY);

        register("base-uri#0", ContextItemAccessorFunction.class, 0, 0, BuiltInAtomicType.ANY_URI, OPT, CORE, CITEM | BASE | LATE);

        register("base-uri#1", BaseUri_1.class, 1, 1, BuiltInAtomicType.ANY_URI, OPT, CORE, BASE)
                .arg(0, Type.NODE_TYPE, OPT | INS, EMPTY);

        register("boolean", BooleanFn.class, 1, 1, BuiltInAtomicType.BOOLEAN, ONE, CORE, 0)
                .arg(0, Type.ITEM_TYPE, STAR | INS, null);

        register("ceiling", Ceiling.class, 1, 1, NumericType.getInstance(), OPT, CORE, AS_PRIM_ARG0)
                .arg(0, NumericType.getInstance(), OPT, EMPTY);

        register("codepoint-equal", CodepointEqual.class, 2, 2, BuiltInAtomicType.BOOLEAN, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.STRING, OPT, EMPTY)
                .arg(1, BuiltInAtomicType.STRING, OPT, EMPTY);

        register("codepoints-to-string", CodepointsToString.class, 1, 1, BuiltInAtomicType.STRING, ONE, CORE, 0)
                .arg(0, BuiltInAtomicType.INTEGER, STAR, null);

        register("collation-key#1", CollationKeyFn.class, 1, 1, BuiltInAtomicType.ANY_ATOMIC,
                 OPT, StandardFunction.XPATH30 | StandardFunction.USE_WHEN, StandardFunction.DCOLL)
                .arg(0, BuiltInAtomicType.STRING, ONE, null);

        register("collation-key#2", CollatingFunctionFree.class, 2, 2, BuiltInAtomicType.ANY_ATOMIC,
                 OPT, StandardFunction.XPATH30 | StandardFunction.USE_WHEN, StandardFunction.DCOLL)
                .arg(0, BuiltInAtomicType.STRING, ONE, null)
                .arg(1, BuiltInAtomicType.STRING, ONE, null);

        register("collection", CollectionFn.class, 0, 1, Type.ITEM_TYPE, STAR, CORE, BASE | LATE)
                .arg(0, BuiltInAtomicType.STRING, OPT, null);

        register("compare#2", Compare.class, 2, 2, BuiltInAtomicType.INTEGER, OPT, CORE, DCOLL)
                .arg(0, BuiltInAtomicType.STRING, OPT, EMPTY)
                .arg(1, BuiltInAtomicType.STRING, OPT, EMPTY);

        register("compare#3", CollatingFunctionFree.class, 3, 3, BuiltInAtomicType.INTEGER, OPT, CORE, BASE)
                .arg(0, BuiltInAtomicType.STRING, OPT, EMPTY)
                .arg(1, BuiltInAtomicType.STRING, OPT, EMPTY)
                .arg(2, BuiltInAtomicType.STRING, ONE, null);

        register("concat", Concat.class, 2, Integer.MAX_VALUE, BuiltInAtomicType.STRING, ONE, CORE, 0)
                .arg(0, BuiltInAtomicType.ANY_ATOMIC, OPT, null);
        // Note, this has a variable number of arguments so it is treated specially

        register("contains#2", Contains.class, 2, 2, BuiltInAtomicType.BOOLEAN, ONE, CORE, DCOLL)
                .arg(0, BuiltInAtomicType.STRING, OPT, null)
                .arg(1, BuiltInAtomicType.STRING, OPT, BooleanValue.TRUE);

        register("contains#3", CollatingFunctionFree.class, 3, 3, BuiltInAtomicType.BOOLEAN, ONE, CORE, BASE)
                .arg(0, BuiltInAtomicType.STRING, OPT, null)
                .arg(1, BuiltInAtomicType.STRING, OPT, BooleanValue.TRUE)
                .arg(2, BuiltInAtomicType.STRING, ONE, null);

        register("contains-token#2", ContainsToken.class, 2, 2, BuiltInAtomicType.BOOLEAN, ONE, XPATH31, DCOLL)
                .arg(0, BuiltInAtomicType.STRING, STAR, null)
                .arg(1, BuiltInAtomicType.STRING, ONE, null);

        register("contains-token#3", CollatingFunctionFree.class, 3, 3, BuiltInAtomicType.BOOLEAN, ONE, XPATH31, BASE)
                .arg(0, BuiltInAtomicType.STRING, STAR, null)
                .arg(1, BuiltInAtomicType.STRING, ONE, null)
                .arg(2, BuiltInAtomicType.STRING, ONE, null);

        register("count", Count.class, 1, 1, BuiltInAtomicType.INTEGER, ONE, CORE, UO)
                .arg(0, Type.ITEM_TYPE, STAR | INS, Int64Value.ZERO);

        register("current", Current.class, 0, 0, Type.ITEM_TYPE, ONE, XSLT, LATE);

        register("current-date", DynamicContextAccessor.CurrentDate.class, 0, 0, BuiltInAtomicType.DATE, ONE, CORE, LATE);

        register("current-dateTime", DynamicContextAccessor.CurrentDateTime.class, 0, 0, BuiltInAtomicType.DATE_TIME, ONE, CORE, LATE);

        register("current-time", DynamicContextAccessor.CurrentTime.class, 0, 0, BuiltInAtomicType.TIME, ONE, CORE, LATE);

        register("current-group", CurrentGroup.class, 0, 0, Type.ITEM_TYPE, STAR, XSLT, LATE);

        register("current-grouping-key", CurrentGroupingKey.class, 0, 0, BuiltInAtomicType.ANY_ATOMIC, STAR, XSLT, LATE);

        register("data#0", ContextItemAccessorFunction.class, 0, 0, BuiltInAtomicType.ANY_ATOMIC, STAR, XPATH30, CITEM | LATE);

        register("data#1", Data_1.class, 1, 1, BuiltInAtomicType.ANY_ATOMIC, STAR, CORE, 0)
                .arg(0, Type.ITEM_TYPE, STAR | ABS, EMPTY);

        register("dateTime", DateTimeConstructor.class, 2, 2, BuiltInAtomicType.DATE_TIME, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.DATE, OPT, EMPTY)
                .arg(1, BuiltInAtomicType.TIME, OPT, EMPTY);

        register("day-from-date", AccessorFn.DayFromDate.class, 1, 1, BuiltInAtomicType.INTEGER, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.DATE, OPT, EMPTY);

        register("day-from-dateTime", AccessorFn.DayFromDateTime.class, 1, 1, BuiltInAtomicType.INTEGER, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.DATE_TIME, OPT, EMPTY);

        register("days-from-duration", AccessorFn.DaysFromDuration.class, 1, 1, BuiltInAtomicType.INTEGER, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.DURATION, OPT, EMPTY);

        register("deep-equal#2", DeepEqual.class, 2, 3, BuiltInAtomicType.BOOLEAN, ONE, CORE, DCOLL)
                .arg(0, Type.ITEM_TYPE, STAR | ABS, null)
                .arg(1, Type.ITEM_TYPE, STAR | ABS, null);

        register("deep-equal#3", CollatingFunctionFree.class, 2, 3, BuiltInAtomicType.BOOLEAN, ONE, CORE, BASE)
                .arg(0, Type.ITEM_TYPE, STAR, null)
                .arg(1, Type.ITEM_TYPE, STAR, null)
                .arg(2, BuiltInAtomicType.STRING, ONE, null);

        register("default-collation", StaticContextAccessor.DefaultCollation.class, 0, 0, BuiltInAtomicType.STRING, ONE, CORE, DCOLL);

        register("default-language", DynamicContextAccessor.DefaultLanguage.class, 0, 0, BuiltInAtomicType.LANGUAGE, ONE, CORE, DLANG);

        register("distinct-values#1", DistinctValues.class, 1, 2, BuiltInAtomicType.ANY_ATOMIC, STAR, CORE, DCOLL|UO)
                .arg(0, BuiltInAtomicType.ANY_ATOMIC, STAR, EMPTY);

        register("distinct-values#2", CollatingFunctionFree.class, 1, 2, BuiltInAtomicType.ANY_ATOMIC, STAR, CORE, BASE|UO)
                .arg(0, BuiltInAtomicType.ANY_ATOMIC, STAR, EMPTY)
                .arg(1, BuiltInAtomicType.STRING, ONE, null);

        register("doc", Doc.class, 1, 1, NodeKindTest.DOCUMENT, OPT, CORE, BASE | LATE)
                .arg(0, BuiltInAtomicType.STRING, OPT, EMPTY);

        register("doc-available", DocAvailable.class, 1, 1, BuiltInAtomicType.BOOLEAN, ONE, CORE, BASE | LATE)
                .arg(0, BuiltInAtomicType.STRING, OPT, BooleanValue.FALSE);

        register("document", DocumentFn.class, 1, 2, Type.NODE_TYPE, STAR, XSLT, BASE|LATE|UO)
                .arg(0, Type.ITEM_TYPE, STAR, null)
                .arg(1, Type.NODE_TYPE, ONE, null);

        register("document-uri#0", ContextItemAccessorFunction.class, 0, 0,
                 BuiltInAtomicType.ANY_URI, OPT, XPATH30, CITEM | LATE);

        register("document-uri#1", DocumentUri_1.class, 1, 1,
            BuiltInAtomicType.ANY_URI, OPT, CORE, LATE)
                .arg(0, Type.NODE_TYPE, OPT | INS, EMPTY);

        register("element-available", ElementAvailable.class, 1, 1, BuiltInAtomicType.BOOLEAN, ONE, XSLT | USE_WHEN, NS)
            .arg(0, BuiltInAtomicType.STRING, ONE, null);

        register("element-with-id#1", SuperId.ElementWithId.class, 1, 1, NodeKindTest.ELEMENT, STAR, CORE, CITEM | LATE | UO)
            .arg(0, BuiltInAtomicType.STRING, STAR, EMPTY);

        register("element-with-id#2", SuperId.ElementWithId.class, 2, 2, NodeKindTest.ELEMENT, STAR, CORE, UO)
            .arg(0, BuiltInAtomicType.STRING, STAR, EMPTY)
            .arg(1, Type.NODE_TYPE, ONE, null);

        register("empty", Empty.class, 1, 1, BuiltInAtomicType.BOOLEAN, ONE, CORE, UO)
            .arg(0, Type.ITEM_TYPE, STAR | INS, BooleanValue.TRUE);

        register("encode-for-uri", EncodeForUri.class, 1, 1, BuiltInAtomicType.STRING, ONE, CORE, 0)
            .arg(0, BuiltInAtomicType.STRING, OPT, StringValue.EMPTY_STRING);

        register("ends-with#2", EndsWith.class, 2, 2, BuiltInAtomicType.BOOLEAN, ONE, CORE, DCOLL)
                .arg(0, BuiltInAtomicType.STRING, OPT, null)
                .arg(1, BuiltInAtomicType.STRING, OPT, BooleanValue.TRUE);

        register("ends-with#3", CollatingFunctionFree.class, 3, 3, BuiltInAtomicType.BOOLEAN, ONE, CORE, BASE)
                .arg(0, BuiltInAtomicType.STRING, OPT, null)
                .arg(1, BuiltInAtomicType.STRING, OPT, BooleanValue.TRUE)
                .arg(2, BuiltInAtomicType.STRING, ONE, null);

        register("environment-variable", EnvironmentVariable.class, 1, 1, BuiltInAtomicType.STRING,
                 OPT, XPATH30 | USE_WHEN, LATE)
                .arg(0, BuiltInAtomicType.STRING, ONE, null);



        register("escape-html-uri", EscapeHtmlUri.class, 1, 1, BuiltInAtomicType.STRING, ONE, CORE, 0)
                .arg(0, BuiltInAtomicType.STRING, OPT, StringValue.EMPTY_STRING);

        register("error", Error.class, 0, 3, Type.ITEM_TYPE, OPT, CORE, LATE)
                // The return type is chosen so that use of the error() function will never give a static type error,
                // on the basis that item()? overlaps every other type, and it's almost impossible to make any
                // unwarranted inferences from it, except perhaps count(error()) lt 2.
                .arg(0, BuiltInAtomicType.QNAME, OPT, null)
                .arg(1, BuiltInAtomicType.STRING, ONE, null)
                .arg(2, Type.ITEM_TYPE, STAR, null);

        register("exactly-one", TreatFn.ExactlyOne.class, 1, 1, Type.ITEM_TYPE, ONE, CORE, AS_ARG0 | FILTER)
                .arg(0, Type.ITEM_TYPE, STAR | TRA, null);


        register("exists", Exists.class, 1, 1, BuiltInAtomicType.BOOLEAN, ONE, CORE, UO)
                .arg(0, Type.ITEM_TYPE, STAR | INS, BooleanValue.FALSE);

        register("false", ConstantFunction.False.class, 0, 0, BuiltInAtomicType.BOOLEAN, ONE, CORE, 0);

        register("floor", Floor.class, 1, 1, NumericType.getInstance(), OPT, CORE, AS_PRIM_ARG0)
                .arg(0, NumericType.getInstance(), OPT, EMPTY);

        register("format-date", FormatDate.class, 2, 5, BuiltInAtomicType.STRING,
                OPT, XSLT | XPATH30, 0)
                .arg(0, BuiltInAtomicType.DATE, OPT, null)
                .arg(1, BuiltInAtomicType.STRING, ONE, null)
                .arg(2, BuiltInAtomicType.STRING, OPT, null)
                .arg(3, BuiltInAtomicType.STRING, OPT, null)
                .arg(4, BuiltInAtomicType.STRING, OPT, null);

        register("format-dateTime", FormatDate.class, 2, 5, BuiltInAtomicType.STRING,
                OPT, XSLT | XPATH30, 0)
                .arg(0, BuiltInAtomicType.DATE_TIME, OPT, null)
                .arg(1, BuiltInAtomicType.STRING, ONE, null)
                .arg(2, BuiltInAtomicType.STRING, OPT, null)
                .arg(3, BuiltInAtomicType.STRING, OPT, null)
                .arg(4, BuiltInAtomicType.STRING, OPT, null);

        register("format-integer", FormatInteger.class, 2, 3, AnyItemType.getInstance(), ONE, XPATH30, 0)
                .arg(0, BuiltInAtomicType.INTEGER, OPT, null)
                .arg(1, BuiltInAtomicType.STRING, ONE, null)
                .arg(2, BuiltInAtomicType.STRING, OPT, null);

        register("format-number#2", FormatNumber.class, 2, 2, BuiltInAtomicType.STRING, ONE, XSLT | XPATH30, LATE)
                .arg(0, NumericType.getInstance(), OPT, null)
                .arg(1, BuiltInAtomicType.STRING, ONE, null);

        register("format-number#3", FormatNumber.class, 3, 3, BuiltInAtomicType.STRING, ONE, XSLT | XPATH30, NS | LATE)
                .arg(0, NumericType.getInstance(), OPT, null)
                .arg(1, BuiltInAtomicType.STRING, ONE, null)
                .arg(2, BuiltInAtomicType.STRING, OPT, null);

        register("format-time", FormatDate.class, 2, 5, BuiltInAtomicType.STRING,
                OPT, XSLT | XPATH30, 0)
                .arg(0, BuiltInAtomicType.TIME, OPT, null)
                .arg(1, BuiltInAtomicType.STRING, ONE, null)
                .arg(2, BuiltInAtomicType.STRING, OPT, null)
                .arg(3, BuiltInAtomicType.STRING, OPT, null)
                .arg(4, BuiltInAtomicType.STRING, OPT, null);

        register("function-available", FunctionAvailable.class, 1, 2, BuiltInAtomicType.BOOLEAN, ONE, XSLT | USE_WHEN, NS | LATE)
                .arg(0, BuiltInAtomicType.STRING, ONE, null)
                .arg(1, BuiltInAtomicType.INTEGER, ONE, null);

        register("generate-id#0", ContextItemAccessorFunction.class, 0, 0, BuiltInAtomicType.STRING, ONE, XSLT | XPATH30, CITEM | LATE);

        register("generate-id#1", GenerateId_1.class, 1, 1, BuiltInAtomicType.STRING, ONE, XSLT | XPATH30, 0)
                .arg(0, Type.NODE_TYPE, OPT | INS, StringValue.EMPTY_STRING);

        register("has-children#0", ContextItemAccessorFunction.class, 0, 0, BuiltInAtomicType.BOOLEAN,
                 ONE, StandardFunction.XPATH30, CITEM | LATE);

        register("has-children#1", HasChildren_1.class, 1, 1, BuiltInAtomicType.BOOLEAN,
                OPT, XPATH30, 0)
                .arg(0, AnyNodeTest.getInstance(), OPT | INS, null);

        register("head", HeadFn.class, 1, 1, AnyItemType.getInstance(),
                OPT, XPATH30, FILTER)
                .arg(0, AnyItemType.getInstance(), STAR | TRA, null);

        register("hours-from-dateTime", AccessorFn.HoursFromDateTime.class, 1, 1,
                BuiltInAtomicType.INTEGER, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.DATE_TIME, OPT, EMPTY);

        register("hours-from-duration", AccessorFn.HoursFromDuration.class, 1, 1,
                BuiltInAtomicType.INTEGER, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.DURATION, OPT, EMPTY);

        register("hours-from-time", AccessorFn.HoursFromTime.class, 1, 1,
                BuiltInAtomicType.INTEGER, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.TIME, OPT, EMPTY);

        register("id#1", SuperId.Id.class, 1, 1, NodeKindTest.ELEMENT, STAR, CORE, CITEM | LATE | UO)
                .arg(0, BuiltInAtomicType.STRING, STAR, EMPTY);

        register("id#2", SuperId.Id.class, 2, 2, NodeKindTest.ELEMENT, STAR, CORE, LATE|UO)
                .arg(0, BuiltInAtomicType.STRING, STAR, EMPTY)
                .arg(1, Type.NODE_TYPE, ONE | NAV, null);

        register("idref#1", Idref.class, 1, 1, Type.NODE_TYPE, STAR, CORE, CITEM | LATE)
                .arg(0, BuiltInAtomicType.STRING, STAR, EMPTY);

        register("idref#2", Idref.class, 2, 2, Type.NODE_TYPE, STAR, CORE, LATE)
                .arg(0, BuiltInAtomicType.STRING, STAR, EMPTY)
                .arg(1, Type.NODE_TYPE, ONE | NAV, null);

        register("implicit-timezone", DynamicContextAccessor.ImplicitTimezone.class, 0, 0, BuiltInAtomicType.DAY_TIME_DURATION, ONE, CORE, LATE);

        register("in-scope-prefixes", InScopePrefixes.class, 1, 1, BuiltInAtomicType.STRING, STAR, CORE, 0)
                .arg(0, NodeKindTest.ELEMENT, ONE | INS, null);

        register("index-of#2", IndexOf.class, 2, 2, BuiltInAtomicType.INTEGER, STAR, CORE, DCOLL)
                .arg(0, BuiltInAtomicType.ANY_ATOMIC, STAR, EMPTY)
                .arg(1, BuiltInAtomicType.ANY_ATOMIC, ONE, null);

        register("index-of#3", CollatingFunctionFree.class, 3, 3, BuiltInAtomicType.INTEGER, STAR, CORE, BASE)
                .arg(0, BuiltInAtomicType.ANY_ATOMIC, STAR, EMPTY)
                .arg(1, BuiltInAtomicType.ANY_ATOMIC, ONE, null)
                .arg(2, BuiltInAtomicType.STRING, ONE, null);

        register("innermost", Innermost.class, 1, 1, AnyNodeTest.getInstance(),
                 STAR, XPATH30, 0)
                        .arg(0, AnyNodeTest.getInstance(), STAR | NAV, null);

        register("insert-before", InsertBefore.class, 3, 3, Type.ITEM_TYPE, STAR, CORE, 0)
                .arg(0, Type.ITEM_TYPE, STAR | TRA, null)
                .arg(1, BuiltInAtomicType.INTEGER, ONE, null)
                .arg(2, Type.ITEM_TYPE, STAR | TRA, null);

        register("iri-to-uri", IriToUri.class, 1, 1, BuiltInAtomicType.STRING, ONE, CORE, 0)
                .arg(0, BuiltInAtomicType.STRING, OPT, StringValue.EMPTY_STRING);

        register("json-doc", JsonDoc.class, 1, 2, AnyItemType.getInstance(),
                OPT, StandardFunction.XPATH30, LATE)
                .arg(0, BuiltInAtomicType.STRING, OPT, null)
                .arg(1, MapType.ANY_MAP_TYPE, ONE, null);

        register("json-to-xml", JsonToXMLFn.class, 1, 2, AnyItemType.getInstance(),
                OPT, StandardFunction.XPATH30, LATE)
                .arg(0, BuiltInAtomicType.STRING, OPT, null)
                .arg(1, MapType.ANY_MAP_TYPE, ONE, null);

        register("key#2", KeyFn.class, 2, 2, Type.NODE_TYPE, STAR, XSLT, CITEM | NS | LATE)
                .arg(0, BuiltInAtomicType.STRING, ONE, null)
                .arg(1, BuiltInAtomicType.ANY_ATOMIC, STAR, EMPTY);

        register("key#3", KeyFn.class, 3, 3, Type.NODE_TYPE, STAR, XSLT, NS|LATE)
                .arg(0, BuiltInAtomicType.STRING, ONE, null)
                .arg(1, BuiltInAtomicType.ANY_ATOMIC, STAR, EMPTY)
                .arg(2, Type.NODE_TYPE, ONE, null);

        register("lang#1", Lang.class, 1, 1, BuiltInAtomicType.BOOLEAN, ONE, CORE, CITEM | LATE)
                .arg(0, BuiltInAtomicType.STRING, OPT, null);

        register("lang#2", Lang.class, 2, 2, BuiltInAtomicType.BOOLEAN, ONE, CORE, 0)
                .arg(0, BuiltInAtomicType.STRING, OPT, null)
                .arg(1, Type.NODE_TYPE, ONE | INS, null);

        register("last", PositionAndLast.Last.class, 0, 0, BuiltInAtomicType.INTEGER, ONE, CORE, LAST | LATE);

        register("local-name#0", ContextItemAccessorFunction.class, 0, 0, BuiltInAtomicType.STRING, ONE, CORE, CITEM | LATE);

        register("local-name#1", LocalName_1.class, 1, 1, BuiltInAtomicType.STRING, ONE, CORE, 0)
                .arg(0, Type.NODE_TYPE, OPT | INS, StringValue.EMPTY_STRING);

        register("local-name-from-QName", AccessorFn.LocalNameFromQName.class, 1, 1,
                BuiltInAtomicType.NCNAME, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.QNAME, OPT, EMPTY);

        register("lower-case", LowerCase.class, 1, 1, BuiltInAtomicType.STRING, ONE, CORE, 0)
                .arg(0, BuiltInAtomicType.STRING, OPT, StringValue.EMPTY_STRING);

        register("matches#2", RegexFunctionSansFlags.class, 2, 2, BuiltInAtomicType.BOOLEAN, ONE, CORE, 0)
                .arg(0, BuiltInAtomicType.STRING, OPT, null)
                .arg(1, BuiltInAtomicType.STRING, ONE, null);

        register("matches#3", Matches.class, 3, 3, BuiltInAtomicType.BOOLEAN, ONE, CORE, 0)
            .arg(0, BuiltInAtomicType.STRING, OPT, null)
            .arg(1, BuiltInAtomicType.STRING, ONE, null)
            .arg(2, BuiltInAtomicType.STRING, ONE, null);

        register("max#1", Minimax.Max.class, 1, 1, BuiltInAtomicType.ANY_ATOMIC, OPT, CORE, DCOLL | UO)
                .arg(0, BuiltInAtomicType.ANY_ATOMIC, STAR, EMPTY);

        register("max#2", CollatingFunctionFree.class, 2, 2, BuiltInAtomicType.ANY_ATOMIC, OPT, CORE, BASE|UO)
                .arg(0, BuiltInAtomicType.ANY_ATOMIC, STAR, EMPTY)
                .arg(1, BuiltInAtomicType.STRING, ONE, null);

        register("min#1", Minimax.Min.class, 1, 1, BuiltInAtomicType.ANY_ATOMIC, OPT, CORE, DCOLL | UO)
                .arg(0, BuiltInAtomicType.ANY_ATOMIC, STAR, EMPTY);

        register("min#2", CollatingFunctionFree.class, 2, 2, BuiltInAtomicType.ANY_ATOMIC, OPT, CORE, BASE|UO)
                .arg(0, BuiltInAtomicType.ANY_ATOMIC, STAR, EMPTY)
                .arg(1, BuiltInAtomicType.STRING, ONE, null);

        register("minutes-from-dateTime", AccessorFn.MinutesFromDateTime.MinutesFromDuration.class, 1, 1,
                 BuiltInAtomicType.INTEGER, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.DATE_TIME, OPT, EMPTY);

        register("minutes-from-duration", AccessorFn.MinutesFromDuration.class, 1, 1,
                BuiltInAtomicType.INTEGER, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.DURATION, OPT, EMPTY);

        register("minutes-from-time", AccessorFn.MinutesFromTime.class, 1, 1,
                BuiltInAtomicType.INTEGER, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.TIME, OPT, EMPTY);

        register("month-from-date", AccessorFn.MonthFromDate.class, 1, 1,
                BuiltInAtomicType.INTEGER, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.DATE, OPT, EMPTY);

        register("month-from-dateTime", AccessorFn.MonthFromDateTime.class, 1, 1,
                BuiltInAtomicType.INTEGER, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.DATE_TIME, OPT, EMPTY);

        register("months-from-duration", AccessorFn.MonthsFromDuration.class, 1, 1,
                BuiltInAtomicType.INTEGER, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.DURATION, OPT, EMPTY);

        register("name#0", ContextItemAccessorFunction.class, 0, 0, BuiltInAtomicType.STRING, ONE, CORE, CITEM | LATE);

        register("name#1", Name_1.class, 1, 1, BuiltInAtomicType.STRING, ONE, CORE, 0)
                .arg(0, Type.NODE_TYPE, OPT | INS, StringValue.EMPTY_STRING);

        register("namespace-uri#0", ContextItemAccessorFunction.class, 0, 0, BuiltInAtomicType.ANY_URI, ONE, CORE, CITEM | LATE);

        register("namespace-uri#1", NamespaceUri_1.class, 1, 1, BuiltInAtomicType.ANY_URI, ONE, CORE, 0)
                .arg(0, Type.NODE_TYPE, OPT | INS, StringValue.EMPTY_STRING);

        register("namespace-uri-for-prefix", NamespaceForPrefix.class, 2, 2, BuiltInAtomicType.ANY_URI, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.STRING, OPT, null)
                .arg(1, NodeKindTest.ELEMENT, ONE | INS, null);

        register("namespace-uri-from-QName", AccessorFn.NamespaceUriFromQName.class, 1, 1, BuiltInAtomicType.ANY_URI, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.QNAME, OPT, EMPTY);

        register("nilled#0", ContextItemAccessorFunction.class, 0, 0, BuiltInAtomicType.BOOLEAN, OPT, XPATH30, CITEM | LATE);

        register("nilled#1", Nilled_1.class, 1, 1, BuiltInAtomicType.BOOLEAN, OPT, CORE, 0)
                .arg(0, Type.NODE_TYPE, OPT | INS, EMPTY);

        register("node-name#0", ContextItemAccessorFunction.class, 0, 0, BuiltInAtomicType.QNAME, OPT, XPATH30, CITEM | LATE);

        register("node-name#1", NodeName_1.class, 1, 1, BuiltInAtomicType.QNAME, OPT, CORE, 0)
                .arg(0, Type.NODE_TYPE, OPT | INS, EMPTY);

        register("not", NotFn.class, 1, 1, BuiltInAtomicType.BOOLEAN, ONE, CORE, 0)
                .arg(0, Type.ITEM_TYPE, STAR | INS, BooleanValue.TRUE);

        register("normalize-space#0", ContextItemAccessorFunction.StringAccessor.class, 0, 0, BuiltInAtomicType.STRING, ONE, CORE, CITEM | LATE);

        register("normalize-space#1", NormalizeSpace_1.class, 1, 1, BuiltInAtomicType.STRING, ONE, CORE, 0)
                .arg(0, BuiltInAtomicType.STRING, OPT, null);

        register("normalize-unicode", NormalizeUnicode.class, 1, 2, BuiltInAtomicType.STRING, ONE, CORE, 0)
                .arg(0, BuiltInAtomicType.STRING, OPT, StringValue.EMPTY_STRING)
                .arg(1, BuiltInAtomicType.STRING, ONE, null);

        register("number#0", ContextItemAccessorFunction.Number_0.class, 0, 0, BuiltInAtomicType.DOUBLE, ONE, CORE, CITEM | LATE);

        register("number#1", Number_1.class, 1, 1, BuiltInAtomicType.DOUBLE, ONE, CORE, 0)
                .arg(0, BuiltInAtomicType.ANY_ATOMIC, OPT, DoubleValue.NaN);

        register("one-or-more", TreatFn.OneOrMore.class, 1, 1, Type.ITEM_TYPE, PLUS, CORE, AS_ARG0|FILTER)
                .arg(0, Type.ITEM_TYPE, STAR | TRA, null);

        register("outermost", Outermost.class, 1, 1, AnyNodeTest.getInstance(), STAR, XPATH30, AS_ARG0 | FILTER)
                .arg(0, AnyNodeTest.getInstance(), STAR | TRA, null);

        register("parse-ietf-date", ParseIetfDate.class, 1, 1, BuiltInAtomicType.DATE_TIME, OPT, XPATH31, 0)
                .arg(0, BuiltInAtomicType.STRING, OPT, EMPTY);

        register("parse-json", ParseJsonFn.class, 1, 2, AnyItemType.getInstance(),
                OPT, StandardFunction.XPATH30, 0)
                .arg(0, BuiltInAtomicType.STRING, ONE, null)
                .arg(1, MapType.ANY_MAP_TYPE, ONE, null);

        register("parse-xml", ParseXml.class, 1, 1, NodeKindTest.DOCUMENT, ONE, XPATH30, LATE)
                .arg(0, BuiltInAtomicType.STRING, ONE, null);

        register("parse-xml-fragment", ParseXmlFragment.class, 1, 1, NodeKindTest.DOCUMENT, ONE, XPATH30, LATE)
                .arg(0, BuiltInAtomicType.STRING, ONE, null);

        register("path#0", ContextItemAccessorFunction.class, 0, 0, BuiltInAtomicType.STRING, OPT, XPATH30, CITEM|LATE);

        register("path#1", Path_1.class, 1, 1, BuiltInAtomicType.STRING, OPT, XPATH30, 0)
                .arg(0, AnyNodeTest.getInstance(), OPT | StandardFunction.INS, null);

        register("position", PositionAndLast.Position.class, 0, 0, BuiltInAtomicType.INTEGER, ONE, CORE, POSN|LATE);

        register("prefix-from-QName", AccessorFn.PrefixFromQName.class, 1, 1, BuiltInAtomicType.NCNAME, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.QNAME, OPT, EMPTY);

        register("put", Put.class, 2, 2, AnyItemType.getInstance(), OPT, XQUPDATE, LATE)
                .arg(0, Type.NODE_TYPE, ONE, null)
                .arg(1, BuiltInAtomicType.STRING, ONE, null);

        register("QName", QNameFn.class, 2, 2, BuiltInAtomicType.QNAME, ONE, CORE, 0)
                .arg(0, BuiltInAtomicType.STRING, OPT, null)
                .arg(1, BuiltInAtomicType.STRING, ONE, null);

        register("regex-group", RegexGroup.class, 1, 1, BuiltInAtomicType.STRING, ONE, XSLT, LATE)
                .arg(0, BuiltInAtomicType.INTEGER, ONE, null);

        register("remove", Remove.class, 2, 2, Type.ITEM_TYPE, STAR, CORE, AS_ARG0|FILTER)
                .arg(0, Type.ITEM_TYPE, STAR | TRA, EMPTY)
                .arg(1, BuiltInAtomicType.INTEGER, ONE, null);

        register("replace#3", RegexFunctionSansFlags.class, 3, 3, BuiltInAtomicType.STRING,
            ONE, CORE, 0)
                .arg(0, BuiltInAtomicType.STRING, OPT, StringValue.EMPTY_STRING)
                .arg(1, BuiltInAtomicType.STRING, ONE, null)
                .arg(2, BuiltInAtomicType.STRING, ONE, null);

        register("replace#4", Replace.class, 4, 4, BuiltInAtomicType.STRING,
                ONE, CORE, 0)
                .arg(0, BuiltInAtomicType.STRING, OPT, StringValue.EMPTY_STRING)
                .arg(1, BuiltInAtomicType.STRING, ONE, null)
                .arg(2, BuiltInAtomicType.STRING, ONE, null)
                .arg(3, BuiltInAtomicType.STRING, ONE, null);

        register("resolve-QName", ResolveQName.class, 2, 2, BuiltInAtomicType.QNAME, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.STRING, OPT, EMPTY)
                .arg(1, NodeKindTest.ELEMENT, ONE | INS, null);

        register("resolve-uri#1", ResolveURI.class, 1, 1, BuiltInAtomicType.ANY_URI, OPT, CORE, BASE)
                .arg(0, BuiltInAtomicType.STRING, OPT, null);

        register("resolve-uri#2", ResolveURI.class, 2, 2, BuiltInAtomicType.ANY_URI, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.STRING, OPT, null)
                .arg(1, BuiltInAtomicType.STRING, ONE, null);

        register("reverse", Reverse.class, 1, 1, Type.ITEM_TYPE, STAR, CORE, AS_ARG0|FILTER)
                .arg(0, Type.ITEM_TYPE, STAR | NAV, EMPTY);

        register("root#0", ContextItemAccessorFunction.class, 0, 0, Type.NODE_TYPE, OPT, CORE, CITEM | LATE);

        register("root#1", Root_1.class, 1, 1, Type.NODE_TYPE, OPT, CORE, 0)
                .arg(0, Type.NODE_TYPE, OPT | NAV, EMPTY);

        register("round#1", Round.class, 1, 1, NumericType.getInstance(), OPT, CORE, AS_PRIM_ARG0)
                .arg(0, NumericType.getInstance(), OPT, EMPTY);

        register("round#2", Round.class, 2, 2, NumericType.getInstance(), OPT, XPATH30, AS_PRIM_ARG0)
                .arg(0, NumericType.getInstance(), OPT, EMPTY)
                .arg(1, BuiltInAtomicType.INTEGER, ONE, null);

        register("round-half-to-even", RoundHalfToEven.class, 1, 2, NumericType.getInstance(), OPT, CORE, AS_PRIM_ARG0)
                .arg(0, NumericType.getInstance(), OPT, EMPTY)
                .arg(1, BuiltInAtomicType.INTEGER, ONE, null);

        register("seconds-from-dateTime", AccessorFn.SecondsFromDateTime.class, 1, 1, BuiltInAtomicType.DECIMAL, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.DATE_TIME, OPT, EMPTY);

        register("seconds-from-duration", AccessorFn.SecondsFromDuration.class, 1, 1, BuiltInAtomicType.DECIMAL, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.DURATION, OPT, EMPTY);

        register("seconds-from-time", AccessorFn.SecondsFromTime.class, 1, 1, BuiltInAtomicType.DECIMAL, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.TIME, OPT, EMPTY);

        register("serialize", Serialize.class, 1, 2, BuiltInAtomicType.STRING, ONE, XPATH30, 0)
                .arg(0, AnyItemType.getInstance(), STAR, null)
                .arg(1, Type.ITEM_TYPE, OPT, null);
                //.arg(1, NodeKindTest.ELEMENT, STAR, null);

        StandardFunction.register("sort#1", SortOne.class, 1, 1, AnyItemType.getInstance(), STAR, XPATH31, 0)
                .arg(0, AnyItemType.getInstance(), STAR, null);

        register("starts-with#2", StartsWith.class, 2, 2, BuiltInAtomicType.BOOLEAN, ONE, CORE, DCOLL)
                .arg(0, BuiltInAtomicType.STRING, OPT, null)
                .arg(1, BuiltInAtomicType.STRING, OPT, BooleanValue.TRUE);

        register("starts-with#3", CollatingFunctionFree.class, 3, 3, BuiltInAtomicType.BOOLEAN, ONE, CORE, BASE)
                .arg(0, BuiltInAtomicType.STRING, OPT, null)
                .arg(1, BuiltInAtomicType.STRING, OPT, BooleanValue.TRUE)
                .arg(2, BuiltInAtomicType.STRING, ONE, null);

        register("static-base-uri", StaticContextAccessor.StaticBaseUri.class, 0, 0, BuiltInAtomicType.ANY_URI, OPT, CORE, BASE);

        register("string#0", ContextItemAccessorFunction.class, 0, 0, BuiltInAtomicType.STRING, ONE, CORE, CITEM | LATE);

        register("string#1", String_1.class, 1, 1, BuiltInAtomicType.STRING, ONE, CORE, 0)
                .arg(0, Type.ITEM_TYPE, OPT | ABS, StringValue.EMPTY_STRING);

        register("string-length#0", ContextItemAccessorFunction.StringAccessor.class, 0, 0, BuiltInAtomicType.INTEGER, ONE, CORE, CITEM | LATE);

        register("string-length#1", StringLength_1.class, 1, 1, BuiltInAtomicType.INTEGER, ONE, CORE, 0)
                .arg(0, BuiltInAtomicType.STRING, OPT, null);

        register("string-join#1", StringJoin.class, 1, 1, BuiltInAtomicType.STRING, ONE, XPATH30, 0)
                .arg(0, BuiltInAtomicType.ANY_ATOMIC, STAR, StringValue.EMPTY_STRING);

        register("string-join#2", StringJoin.class, 2, 2, BuiltInAtomicType.STRING, ONE, CORE, 0)
                .arg(0, BuiltInAtomicType.ANY_ATOMIC, STAR, StringValue.EMPTY_STRING)
                .arg(1, BuiltInAtomicType.STRING, ONE, null);

            // The argument type for string-join() is relaxed from xs:string to xs:anyAtomicType in 3.1, and we don't have
            // machinery to have different signatures for different XPath versions.

        register("string-to-codepoints", StringToCodepoints.class, 1, 1, BuiltInAtomicType.INTEGER, STAR, CORE, 0)
                .arg(0, BuiltInAtomicType.STRING, OPT, EMPTY);

        register("subsequence#2", Subsequence_2.class, 2, 2, Type.ITEM_TYPE, STAR, CORE, AS_ARG0|FILTER)
            .arg(0, Type.ITEM_TYPE, STAR | TRA, EMPTY)
            .arg(1, NumericType.getInstance(), ONE, null);

        register("subsequence#3", Subsequence_3.class, 3, 3, Type.ITEM_TYPE, STAR, CORE, AS_ARG0|FILTER)
                .arg(0, Type.ITEM_TYPE, STAR | TRA, EMPTY)
                .arg(1, NumericType.getInstance(), ONE, null)
                .arg(2, NumericType.getInstance(), ONE, null);

        register("substring", Substring.class, 2, 3, BuiltInAtomicType.STRING, ONE, CORE, 0)
                .arg(0, BuiltInAtomicType.STRING, OPT, StringValue.EMPTY_STRING)
                .arg(1, NumericType.getInstance(), ONE, null)
                .arg(2, NumericType.getInstance(), ONE, null);

        register("substring-after#2", SubstringAfter.class, 2, 2, BuiltInAtomicType.STRING, ONE, CORE, DCOLL)
                .arg(0, BuiltInAtomicType.STRING, OPT, null)
                .arg(1, BuiltInAtomicType.STRING, OPT, null);

        register("substring-after#3", CollatingFunctionFree.class, 3, 3, BuiltInAtomicType.STRING, ONE, CORE, BASE)
                .arg(0, BuiltInAtomicType.STRING, OPT, null)
                .arg(1, BuiltInAtomicType.STRING, OPT, null)
                .arg(2, BuiltInAtomicType.STRING, ONE, null);

        register("substring-before#2", SubstringBefore.class, 2, 2, BuiltInAtomicType.STRING, ONE, CORE, DCOLL)
                .arg(0, BuiltInAtomicType.STRING, OPT, null)
                .arg(1, BuiltInAtomicType.STRING, OPT, StringValue.EMPTY_STRING);

        register("substring-before#3", CollatingFunctionFree.class, 3, 3, BuiltInAtomicType.STRING, ONE, CORE, BASE)
                .arg(0, BuiltInAtomicType.STRING, OPT, null)
                .arg(1, BuiltInAtomicType.STRING, OPT, StringValue.EMPTY_STRING)
                .arg(2, BuiltInAtomicType.STRING, ONE, null);

        register("sum", Sum.class, 1, 2, BuiltInAtomicType.ANY_ATOMIC, OPT, CORE, UO)
                .arg(0, BuiltInAtomicType.ANY_ATOMIC, STAR, null)
                .arg(1, BuiltInAtomicType.ANY_ATOMIC, OPT, null);

        register("system-property", SystemProperty.class, 1, 1, BuiltInAtomicType.STRING, ONE, XSLT | USE_WHEN, NS)
                .arg(0, BuiltInAtomicType.STRING, ONE, null);

        register("tail", TailFn.class, 1, 1, AnyItemType.getInstance(), STAR, XPATH30, AS_ARG0|FILTER)
                .arg(0, AnyItemType.getInstance(), STAR | TRA, null);

        register("timezone-from-date", AccessorFn.TimezoneFromDate.class, 1, 1, BuiltInAtomicType.DAY_TIME_DURATION, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.DATE, OPT, EMPTY);

        register("timezone-from-dateTime", AccessorFn.TimezoneFromDateTime.class, 1, 1,
                BuiltInAtomicType.DAY_TIME_DURATION, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.DATE_TIME, OPT, EMPTY);

        register("timezone-from-time", AccessorFn.TimezoneFromTime.class, 1, 1,
                BuiltInAtomicType.DAY_TIME_DURATION, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.TIME, OPT, EMPTY);

        register("trace#1", Trace.class, 1, 1, Type.ITEM_TYPE, STAR, XPATH31, AS_ARG0 | LATE)
            .arg(0, Type.ITEM_TYPE, STAR | TRA, null);

        register("trace#2", Trace.class, 2, 2, Type.ITEM_TYPE, STAR, CORE, AS_ARG0|LATE)
                .arg(0, Type.ITEM_TYPE, STAR | TRA, null)
                .arg(1, BuiltInAtomicType.STRING, ONE, null);

        register("transform", TransformFn.class, 0, 1, MapType.ANY_MAP_TYPE, ONE, XPATH31, 0)
                .arg(0, MapType.ANY_MAP_TYPE, OPT, EMPTY);

        register("translate", Translate.class, 3, 3, BuiltInAtomicType.STRING, ONE, CORE, 0)
                .arg(0, BuiltInAtomicType.STRING, OPT, StringValue.EMPTY_STRING)
                .arg(1, BuiltInAtomicType.STRING, ONE, null)
                .arg(2, BuiltInAtomicType.STRING, ONE, null);

        register("true", ConstantFunction.True.class, 0, 0, BuiltInAtomicType.BOOLEAN, ONE, CORE, 0);

        register("tokenize#1", Tokenize_1.class, 1, 1, BuiltInAtomicType.STRING, STAR, XPATH31, 0)
            .arg(0, BuiltInAtomicType.STRING, OPT, EMPTY);

        register("tokenize#2", RegexFunctionSansFlags.class, 2, 2, BuiltInAtomicType.STRING, STAR, CORE, 0)
                .arg(0, BuiltInAtomicType.STRING, OPT, EMPTY)
                .arg(1, BuiltInAtomicType.STRING, ONE, null);

        register("tokenize#3", Tokenize_3.class, 3, 3, BuiltInAtomicType.STRING, STAR, CORE, 0)
            .arg(0, BuiltInAtomicType.STRING, OPT, EMPTY)
            .arg(1, BuiltInAtomicType.STRING, ONE, null)
            .arg(2, BuiltInAtomicType.STRING, ONE, null);

        register("type-available", TypeAvailable.class, 1, 1, BuiltInAtomicType.BOOLEAN, ONE, XSLT | USE_WHEN, NS)
                .arg(0, BuiltInAtomicType.STRING, ONE, null);

        register("unordered", Unordered.class, 1, 1, Type.ITEM_TYPE, STAR, CORE, AS_ARG0|FILTER|UO)
                .arg(0, Type.ITEM_TYPE, STAR | TRA, EMPTY);

        register("upper-case", UpperCase.class, 1, 1, BuiltInAtomicType.STRING, ONE, CORE, 0)
            .arg(0, BuiltInAtomicType.STRING, OPT, StringValue.EMPTY_STRING);

        register("unparsed-entity-uri#1", UnparsedEntity.UnparsedEntityUri.class, 1, 2, BuiltInAtomicType.ANY_URI, ONE, XSLT, CITEM | LATE)
                .arg(0, BuiltInAtomicType.STRING, ONE, null);

        register("unparsed-entity-uri#2", UnparsedEntity.UnparsedEntityUri.class, 1, 2, BuiltInAtomicType.ANY_URI, ONE, XSLT, 0)
            .arg(0, BuiltInAtomicType.STRING, ONE, null)
            .arg(1, Type.NODE_TYPE, ONE, null);

        register("unparsed-entity-public-id#1", UnparsedEntity.UnparsedEntityPublicId.class, 1, 2, BuiltInAtomicType.STRING, ONE, XSLT, CITEM | LATE)
                .arg(0, BuiltInAtomicType.STRING, ONE, null);

        register("unparsed-entity-public-id#2", UnparsedEntity.UnparsedEntityPublicId.class, 1, 2, BuiltInAtomicType.STRING, ONE, XSLT, 0)
            .arg(0, BuiltInAtomicType.STRING, ONE, null)
            .arg(1, Type.NODE_TYPE, ONE, null);

        register("unparsed-text", UnparsedText.class, 1, 2,
                BuiltInAtomicType.STRING, OPT, XSLT | XPATH30, BASE | LATE)
                .arg(0, BuiltInAtomicType.STRING, OPT, null)
                .arg(1, BuiltInAtomicType.STRING, ONE, null);

        register("unparsed-text-available", UnparsedTextAvailable.class, 1, 2,
                BuiltInAtomicType.BOOLEAN, ONE, XSLT | XPATH30, BASE | LATE)
                .arg(0, BuiltInAtomicType.STRING, ONE, null)
                .arg(1, BuiltInAtomicType.STRING, ONE, null);

        register("unparsed-text-lines", UnparsedTextLines.class, 1, 2, BuiltInAtomicType.STRING, STAR, XPATH30, BASE | LATE)
                .arg(0, BuiltInAtomicType.STRING, OPT, null)
                .arg(1, BuiltInAtomicType.STRING, ONE, null);

        register("uri-collection", UriCollection.class, 0, 1, BuiltInAtomicType.ANY_URI, STAR, XPATH30, LATE)
                .arg(0, BuiltInAtomicType.STRING, OPT, null);

        register("xml-to-json", XMLToJsonFn.class, 1, 2, AnyItemType.getInstance(),
                OPT, StandardFunction.XPATH30, LATE)
                .arg(0, AnyNodeTest.getInstance(), OPT, null)
                .arg(1, MapType.ANY_MAP_TYPE, ONE, null);

        register("year-from-date", AccessorFn.YearFromDate.class, 1, 1,
                BuiltInAtomicType.INTEGER, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.DATE, OPT, EMPTY);

        register("year-from-dateTime", AccessorFn.YearFromDateTime.class, 1, 1,
                BuiltInAtomicType.INTEGER, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.DATE_TIME, OPT, EMPTY);

        register("years-from-duration", AccessorFn.YearsFromDuration.class, 1, 1,
                BuiltInAtomicType.INTEGER, OPT, CORE, 0)
                .arg(0, BuiltInAtomicType.DURATION, OPT, EMPTY);

        register("zero-or-one", TreatFn.ZeroOrOne.class, 1, 1, Type.ITEM_TYPE, OPT, CORE, AS_ARG0|FILTER)
                .arg(0, Type.ITEM_TYPE, STAR | TRA, null);
    }


    /**
     * Get the table entry for the function with a given name
     *
     * @param name  the name of the function. This may be an unprefixed local-name for functions in the
     *              system namespace, or may use the conventional prefix "saxon:" in the case of Saxon extension functions
     *              that are specially recognized
     * @param arity the number of arguments of the function, or -1 if any arity will do
     * @return if the function name is known, an Entry containing information about the function. Otherwise,
     *         null
     */

    public static Entry getFunction(String name, int arity) {
        if (arity == -1) {
            for (int i = 0; i < 10; i++) {
                Entry e = getFunction(name, i);
                if (e != null) {
                    return e;
                }
            }
            return null;
        }
        // try first for an entry of the form name#arity
        Entry e = functionTable.get(name + '#' + arity);
        if (e != null) {
            return e;
        }
        // try for a generic entry
        e = functionTable.get(name);
        if (e != null && e.minArguments <= arity && e.maxArguments >= arity) {
            return e;
        }
        return null;
    }

    /**
     * An entry in the table describing the properties of a function
     */
    public static class Entry implements java.io.Serializable {
        /**
         * The name of the function: a local name in the case of functions in the standard library, or a
         * name with the conventional prefix "saxon:" in the case of Saxon extension functions
         */
        public String name;
        /**
         * The class containing the implementation of this function (always a subclass of SystemFunction)
         */
        public Class implementationClass;
        /**
         * The minimum number of arguments required
         */
        public int minArguments;
        /**
         * The maximum number of arguments permitted
         */
        public int maxArguments;
        /**
         * The item type of the result of the function
         */
        public ItemType itemType;
        /**
         * The cardinality of the result of the function
         */
        public int cardinality;
        /**
         * Flags indicating which host languages the function is applicable to
         */
        public int applicability;
        /**
         * The syntactic context of each argument for the purposes of streamability analysis
         */
        public OperandUsage[] usage;
        /**
         * An array holding the types of the arguments to the function
         */
        public SequenceType[] argumentTypes;
        /**
         * An array holding, for each declared argument, the value that is to be returned if an empty sequence
         * as the value of this argument allows the result to be determined irrespective of the values of the
         * other arguments; null if there is no such calculation possible
         */
        public Sequence[] resultIfEmpty;
        /**
         * Any additional properties. Various bit settings are defined: for example SAME_AS_FIRST_ARGUMENT indicates that
         * the result type is the same as the type of the first argument
         */
        public int properties;

        /**
         * Add information to a function entry about the argument types of the function
         *
         * @param a             the position of the argument, counting from zero
         * @param type          the item type of the argument
         * @param options       the cardinality and usage of the argument
         * @param resultIfEmpty the value returned by the function if an empty sequence appears as the value
         *                      of this argument, in the case when this result is unaffected by any other arguments. Supply null
         *                      if this does not apply.
         * @return this entry (to allow chaining)
         */

        public Entry arg(int a, ItemType type, int options, /*@Nullable*/ Sequence resultIfEmpty) {
            int cardinality = options & StaticProperty.CARDINALITY_MASK;
            OperandUsage usage = OperandUsage.NAVIGATION;
            if ((options & ABS) != 0) {
                usage = OperandUsage.ABSORPTION;
            } else if ((options & TRA) != 0) {
                usage = OperandUsage.TRANSMISSION;
            } else if ((options & INS) != 0) {
                usage = OperandUsage.INSPECTION;
            } else if (type instanceof PlainType) {
                usage = OperandUsage.ABSORPTION;
            }
            try {
                this.argumentTypes[a] = SequenceType.makeSequenceType(type, cardinality);
                this.resultIfEmpty[a] = resultIfEmpty;
                this.usage[a] = usage;
            } catch (ArrayIndexOutOfBoundsException err) {
                System.err.println("Internal Saxon error: Can't set argument " + a + " of " + name);
            }
            return this;
        }

    }


}

