package net.sf.saxon.value;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.functions.Component;
import net.sf.saxon.om.NameChecker;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 * A QName value. This implements the so-called "triples proposal", in which the prefix is retained as
 * part of the value. The prefix is not used in any operation on a QName other than conversion of the
 * QName to a string.
 */

public class QNameValue extends AtomicValue {

    private String prefix;  // "" for the default prefix
    private String uri;     // "" for the null namespace
    private String localPart;

    // Note: an alternative design was considered in which the QName was represented by a NamePool and a
    // nameCode. This caused difficulties because there is not always enough context information available
    // when creating a QName to locate the NamePool.

    /**
     * Constructor
     * @param namePool The name pool containing the specified name code
     * @param nameCode The name code identifying this name in the name pool
     */

    public QNameValue(NamePool namePool, int nameCode) {
        prefix = namePool.getPrefix(nameCode);
        uri = namePool.getURI(nameCode);
        localPart = namePool.getLocalName(nameCode);
    }

    /**
     * Constructor. This constructor validates that the local part is a valid NCName.
     * @param prefix The prefix part of the QName (not used in comparisons). Use null or "" to represent the
     * default prefix. Note that the prefix is not checked for lexical correctness, because in most cases
     * it will already have been matched against in-scope namespaces. Where necessary the caller must
     * check the prefix.
     * @param uri The namespace part of the QName. Use null or "" to represent the null namespace.
     * @param localName The local part of the QName
     * @param checker NameChecker used to check the name against XML 1.0 or XML 1.1 rules. Supply null
     * if the name does not need to be checked (the caller asserts that it is known to be valid)
     */

    public QNameValue(String prefix, String uri, String localName, NameChecker checker) throws XPathException {
        if (checker != null && !checker.isValidNCName(localName)) {
            DynamicError err = new DynamicError("Malformed local name in QName: '" + localName + '\'');
            err.setErrorCode("FORG0001");
            throw err;
        }
        this.prefix = (prefix==null ? "" : prefix);
        this.uri = (uri==null ? "" : uri);
        if (checker != null && this.uri.equals("") && !this.prefix.equals("")) {
            DynamicError err = new DynamicError("QName has null namespace but non-empty prefix");
            err.setErrorCode("FOCA0002");
            throw err;
        }
        this.localPart = localName;
    }

//    /**
//     * Create a QName value (possibly a DerivedAtomicValue derived from QName, or
//     * from Notation) from
//     * a string literal, given a namespace context
//     * @param operand the input string
//     * @param targetType the type required: QName, or a type derived from QName or NOTATION
//     * @param env the static context, including the namespace context
//     * @return the value after conversion
//     * @throws XPathException if the name is lexically invalid or uses an undeclared prefix
//     */

//    public static AtomicValue castToQName(StringValue operand, AtomicType targetType, StaticContext env)
//            throws XPathException {
//        try {
//            CharSequence arg = operand.getStringValueCS();
//            String parts[] = env.getConfiguration().getNameChecker().getQNameParts(arg);
//            String uri;
//            if ("".equals(parts[0])) {
//                uri = "";
//            } else {
//                uri = env.getURIForPrefix(parts[0]);
//                if (uri==null) {
//                    StaticError e = new StaticError("Prefix '" + parts[0] + "' has not been declared");
//                    throw e;
//                }
//            }
//            return makeQName(parts[0], uri, parts[1], targetType, arg, env.getConfiguration().getTypeHierarchy());
//        } catch (QNameException err) {
//            StaticError e = new StaticError(err);
//            throw e;
//        }
//    }

    public static AtomicValue makeQName(String prefix, String uri, String local,
                                        AtomicType targetType, CharSequence lexicalForm, TypeHierarchy th)
            throws XPathException {

        if (targetType.getFingerprint() == StandardNames.XS_QNAME) {
            return new QNameValue(prefix, uri, local, null);
        } else if (th.isSubType(targetType, Type.QNAME_TYPE)) {
            QNameValue q = new QNameValue(prefix, uri, local, null);
            AtomicValue av = targetType.makeDerivedValue(q, lexicalForm, true);
            if (av instanceof ValidationErrorValue) {
                throw ((ValidationErrorValue)av).getException();
            }
            return av;
        } else {
            NotationValue n = new NotationValue(prefix, uri, local, null);
            AtomicValue av =  targetType.makeDerivedValue(n, lexicalForm, true);
            if (av instanceof ValidationErrorValue) {
                throw ((ValidationErrorValue)av).getException();
            }
            return av;
        }

    }


    /**
     * Get the string value as a String. Returns the QName as a lexical QName, retaining the original
     * prefix if available.
     */

    public String getStringValue() {
        if ("".equals(prefix)) {
            return localPart;
        } else {
            return prefix + ':' + localPart;
        }
    }

    /**
     * Get the value as a JAXP QName
     */

//    public QName getQName() {
//        return new QName(uri, localPart, prefix);
//    }

    /**
     * Get the name in Clark notation, that is {uri}local
     */

    public String getClarkName() {
        if ("".equals(uri)) {
            return localPart;
        } else {
            return '{' + uri + '}' + localPart;
        }
    }

    /**
     * Get the local part
     */

    public String getLocalName() {
        return localPart;
    }

    /**
     * Get the namespace part (null means no namespace)
     */

    public String getNamespaceURI() {
        return ("".equals(uri) ? null : uri);
    }

    /**
     * Get the prefix
     */

    public String getPrefix() {
        return prefix;
    }

    /**
     * Allocate a nameCode for this QName in the NamePool
     * @param pool the NamePool to be used
     * @return the allocated nameCode
     */

    public int allocateNameCode(NamePool pool) {
        return pool.allocate(prefix, uri, localPart);
    }

    /**
     * Get a component. Returns a zero-length string if the namespace-uri component is
     * requested and is not present.
     * @param part either Component.LOCALNAME or Component.NAMESPACE indicating which
     * component of the value is required
     * @return either the local name or the namespace URI, in each case as a StringValue
     */

    public AtomicValue getComponent(int part) {
        if (part == Component.LOCALNAME) {
            return RestrictedStringValue.makeRestrictedString(
                    localPart, StandardNames.XS_NCNAME, null);
        } else if (part == Component.NAMESPACE) {
            return new AnyURIValue(uri);
        } else if (part == Component.PREFIX) {
            if ("".equals(prefix)) {
                return null;
            } else {
                return RestrictedStringValue.makeRestrictedString(
                        prefix, StandardNames.XS_NCNAME, null);
            }
        } else {
            throw new UnsupportedOperationException("Component of QName must be URI, Local Name, or Prefix");
        }
    }

    /**
     * Convert to target data type
     * @param requiredType an integer identifying the required atomic type
     * @param context
     * @return an AtomicValue, a value of the required type; or an ErrorValue
     */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch (requiredType.getPrimitiveType()) {
            case Type.ANY_ATOMIC:
            case Type.ITEM:
            case Type.QNAME:
                return this;
            case Type.STRING:
                return new StringValue(getStringValue());
            case Type.UNTYPED_ATOMIC:
                return new UntypedAtomicValue(getStringValue());
            default:
                ValidationException err = new ValidationException("Cannot convert QName to " +
                        requiredType.getDisplayName());
                err.setErrorCode("XPTY0004");
                err.setIsTypeError(true);
                return new ValidationErrorValue(err);
        }
    }

    /**
     * Return the type of the expression
     * @return Type.QNAME (always)
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return Type.QNAME_TYPE;
    }


    /**
     * Determine if two QName values are equal. This comparison ignores the prefix part
     * of the value.
     * @throws ClassCastException if they are not comparable
     * @throws IllegalStateException if the two QNames are in different name pools
     */

    public boolean equals(Object other) {
        QNameValue val = (QNameValue)((AtomicValue)other).getPrimitiveValue();  // force exception if incomparable type
        if (this instanceof NotationValue) {
            val = (NotationValue)val;       // force exception if incomparable type
        }
        return localPart.equals(val.localPart) && uri.equals(val.uri);
    }

    public int hashCode() {
        return localPart.hashCode() ^ uri.hashCode();
    }


    /**
     * Convert to Java object (for passing to external functions)
     */

    public Object convertToJava(Class target, XPathContext context) throws XPathException {
        if (target.isAssignableFrom(QNameValue.class)) {
            return this;
        } else if (target.getName().equals("javax.xml.namespace.QName")) {
            // TODO: rewrite this under JDK 1.5
            return makeQName(context.getConfiguration());
        } else {
            Object o = super.convertToJava(target, context);
            if (o == null) {
                throw new DynamicError("Conversion of QName to " + target.getName() +
                        " is not supported");
            }
            return o;
        }
    }

    /**
     * The toString() method returns the name in the form QName("uri", "local")
     * @return the name in in the form QName("uri", "local")
     */

    public String toString() {
        return "QName(\"" + uri + "\", \"" + localPart + "\")";
    }

    /**
     * Temporary method to construct a javax.xml.namespace.QName without actually mentioning it
     * by name (because the class is not available in JDK 1.4)
     */

    public Object makeQName(Configuration config) {
        try {
            Class qnameClass = config.getClass("javax.xml.namespace.QName", false, null);
            Class[] argTypes = {String.class, String.class, String.class};
            Constructor  constructor = qnameClass.getConstructor(argTypes);
            String[] argValues = {uri, localPart, prefix};
            Object result = constructor.newInstance((Object[])argValues);
            return result;
        } catch (XPathException e) {
            return null;
        } catch (NoSuchMethodException e) {
            return null;
        } catch (InstantiationException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        }

    }

//    public static void main(String[] args) throws Exception {
//        QName q = (QName)new QNameValue("a", "b", "c").makeQName();
//        QNameValue v = Value.makeQNameValue(q);
//        System.err.println(q);
//        System.err.println(v);
//    }

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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

