package net.sf.saxon.value;

import net.sf.saxon.Configuration;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 * A qualified name: this is an abstract superclass for QNameValue and NotationValue, representing the
 * XPath primitive types xs:QName and xs:NOTATION respectively
 */

public abstract class QualifiedNameValue extends AtomicValue {

    protected String prefix;  // "" for the default prefix
    protected String uri;     // null for the null namespace
    protected String localPart;

    // Note: an alternative design was considered in which the QName was represented by a NamePool and a
    // nameCode. This caused difficulties because there is not always enough context information available
    // when creating a QName to locate the NamePool.


    /**
     * Factory method to construct either a QName or a NOTATION value, or a subtype of either of these.
     * Note that it is the caller's responsibility to resolve the QName prefix into a URI
     * @param prefix the prefix part of the value. Use "" or null for the empty prefix.
     * @param uri the namespace URI part of the value. Use "" or null for the non-namespace
     * @param local the local part of the value
     * @param targetType the target type, which must be xs:QName or a subtype of xs:NOTATION or xs:QName
     * @param lexicalForm the original lexical form of the value. This is needed in case there are facets
     * such as pattern that check the lexical form
     * @param th the type hierarchy cache
     * @return the converted value
     * @throws XPathException if the value cannot be converted.
     */

    public static AtomicValue makeQName(String prefix, String uri, String local,
                                        AtomicType targetType, CharSequence lexicalForm, TypeHierarchy th)
            throws XPathException {

        if (targetType.getFingerprint() == StandardNames.XS_QNAME) {
            return new QNameValue(prefix, uri, local, BuiltInAtomicType.QNAME, null);
        } else if (th.isSubType(targetType, BuiltInAtomicType.QNAME)) {
            QualifiedNameValue q = new QNameValue(prefix, uri, local, targetType, null);
            AtomicValue av = targetType.setDerivedTypeLabel(q, lexicalForm, true);
            if (av instanceof ValidationErrorValue) {
                throw ((ValidationErrorValue)av).getException();
            }
            return av;
        } else {
            NotationValue n = new NotationValue(prefix, uri, local, (AtomicType)null);
            AtomicValue av =  targetType.setDerivedTypeLabel(n, lexicalForm, true);
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
     * Get the name in Clark notation, that is {uri}local
     */

    public String getClarkName() {
        if (uri == null) {
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
        return uri;
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



    public int hashCode() {
        if (uri == null) {
            return localPart.hashCode();
        } else {
            return localPart.hashCode() ^ uri.hashCode();
        }
    }


    /**
     * Convert to Java object (for passing to external functions)
     */

    public Object convertToJava(Class target, XPathContext context) throws XPathException {
        if (target.isAssignableFrom(QualifiedNameValue.class)) {
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

