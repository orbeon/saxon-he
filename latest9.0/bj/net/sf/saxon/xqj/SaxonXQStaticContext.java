package net.sf.saxon.xqj;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.Validation;
import net.sf.saxon.javax.xml.xquery.XQConstants;
import net.sf.saxon.javax.xml.xquery.XQException;
import net.sf.saxon.javax.xml.xquery.XQItemType;
import net.sf.saxon.javax.xml.xquery.XQStaticContext;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Saxon implementation of the XQJ XQStaticContext interface
 */
public class SaxonXQStaticContext implements XQStaticContext {

    private int bindingMode = XQConstants.BINDING_MODE_IMMEDIATE;
    private int holdability = XQConstants.HOLDTYPE_HOLD_CURSORS_OVER_COMMIT;
    private int scrollability = XQConstants.SCROLLTYPE_FORWARD_ONLY;
    private StaticQueryContext sqc;

    public SaxonXQStaticContext(Configuration config) {
        sqc = new StaticQueryContext(config);
    }

    protected StaticQueryContext getSaxonStaticQueryContext() {
        return sqc;
    }


    public void declareNamespace(String prefix, String uri) throws XQException {
        sqc.declareNamespace(prefix, uri);
    }

    public String getBaseURI() {
        return sqc.getBaseURI();
    }


    public int getBindingMode() {
        return bindingMode;
    }

    public int getBoundarySpacePolicy() {
        return XQConstants.BOUNDARY_SPACE_STRIP;
    }

    public int getConstructionMode() {
        return XQConstants.CONSTRUCTION_MODE_STRIP;
    }


    public XQItemType getContextItemStaticType() {
        ItemType type = sqc.getRequiredContextItemType();
        return new SaxonXQItemType(type, sqc.getConfiguration());
    }

    public int getCopyNamespacesModeInherit()  {
        return XQConstants.COPY_NAMESPACES_MODE_INHERIT;
    }

    public int getCopyNamespacesModePreserve() {
        return XQConstants.COPY_NAMESPACES_MODE_PRESERVE;
    }

    public String getDefaultCollation() {
        return sqc.getDefaultCollationName();
    }

    public String getDefaultElementTypeNamespace()  {
        return sqc.getDefaultElementNamespace();
    }

    public String getDefaultFunctionNamespace() {
        return sqc.getDefaultFunctionNamespace();
    }

    public int getDefaultOrderForEmptySequences() {
       return sqc.isEmptyLeast() ?
           XQConstants.DEFAULT_ORDER_FOR_EMPTY_SEQUENCES_LEAST :
           XQConstants.DEFAULT_ORDER_FOR_EMPTY_SEQUENCES_GREATEST;
    }

    public String[] getNamespacePrefixes() {
        Iterator iter = sqc.iterateDeclaredPrefixes();
        ArrayList list = new ArrayList(20);
        while (iter.hasNext()) {
            list.add(iter.next());
        }
        String[] result = new String[list.size()];
        for (int i=0; i<result.length; i++) {
            result[i] = (String)list.get(i);
        }
        return result;
    }

    public String getNamespaceURI(String prefix) throws XQException {
        if (prefix == null) {
            throw new XQException("prefix is null");
        }
        return sqc.getNamespaceForPrefix(prefix);
    }

    public int getOrderingMode() {
        return XQConstants.ORDERING_MODE_ORDERED;
    }

    public int getHoldability() {
        return holdability;
    }

    public int getQueryLanguageTypeAndVersion() {
        return XQConstants.LANGTYPE_XQUERY;
    }

    public int getQueryTimeout() {
        return 0;
    }

    public int getScrollability() {
        return scrollability;
    }


    public void setBaseURI(String baseUri) {
        sqc.setBaseURI(baseUri);
    }

    public void setBindingMode(int bindingMode) {
        this.bindingMode = bindingMode;
    }

    public void setBoundarySpacePolicy(int policy) throws XQException {
        switch (policy) {
            case XQConstants.BOUNDARY_SPACE_PRESERVE:
                sqc.setPreserveBoundarySpace(true);
            case XQConstants.BOUNDARY_SPACE_STRIP:
                sqc.setPreserveBoundarySpace(false);
            default:
                throw new XQException("Invalid value for boundary space policy - " + policy);
        }
    }

    public void setConstructionMode(int mode) throws XQException {
        switch (mode) {
            case XQConstants.CONSTRUCTION_MODE_PRESERVE:
                sqc.setConstructionMode(Validation.PRESERVE);
            case XQConstants.CONSTRUCTION_MODE_STRIP:
                sqc.setConstructionMode(Validation.STRIP);
            default:
                throw new XQException("Invalid value for construction mode - " + mode);
        }
    }

    public void setContextItemStaticType(XQItemType contextItemType) {
        sqc.setRequiredContextItemType(((SaxonXQItemType)contextItemType).getSaxonItemType());
    }

    public void setCopyNamespacesModeInherit(int mode) throws XQException {
        switch (mode) {
            case XQConstants.COPY_NAMESPACES_MODE_INHERIT:
                sqc.setInheritNamespaces(true);
            case XQConstants.COPY_NAMESPACES_MODE_NO_INHERIT:
                sqc.setInheritNamespaces(false);
            default:
                throw new XQException("Invalid value for namespaces inherit mode - " + mode);
        }
    }

    public void setCopyNamespacesModePreserve(int mode) throws XQException {
        switch (mode) {
            case XQConstants.COPY_NAMESPACES_MODE_PRESERVE:
                sqc.setPreserveNamespaces(true);
            case XQConstants.COPY_NAMESPACES_MODE_NO_PRESERVE:
                sqc.setPreserveNamespaces(false);
            default:
                throw new XQException("Invalid value for namespaces preserve mode - " + mode);
        }
    }

    public void setDefaultCollation(String uri) {
        sqc.declareDefaultCollation(uri);
    }

    public void setDefaultElementTypeNamespace(String uri) throws XQException {
        try {
            sqc.setDefaultElementNamespace(uri);
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public void setDefaultFunctionNamespace(String uri) {
        sqc.setDefaultFunctionNamespace(uri);
    }

    public void setDefaultOrderForEmptySequences(int order) throws XQException {
        switch (order) {
            case XQConstants.DEFAULT_ORDER_FOR_EMPTY_SEQUENCES_GREATEST:
                sqc.setEmptyLeast(false);
            case XQConstants.DEFAULT_ORDER_FOR_EMPTY_SEQUENCES_LEAST:
                sqc.setEmptyLeast(true);
            default:
                throw new XQException("Invalid value for default order for empty sequences - " + order);
        }
    }

    public void setOrderingMode(int mode) {
        // no-op
    }

    public void setQueryTimeout(int seconds) {
        // no-op
    }

    public void setHoldability(int holdability) throws XQException {
        switch (holdability) {
            case XQConstants.HOLDTYPE_HOLD_CURSORS_OVER_COMMIT:
            case XQConstants.HOLDTYPE_CLOSE_CURSORS_AT_COMMIT:
                this.holdability = holdability;
            default:
                throw new XQException("Invalid holdability value - " + holdability);
        }
    }

    public void setQueryLanguageTypeAndVersion(int langtype) throws XQException {
        if (langtype != XQConstants.LANGTYPE_XQUERY) {
            throw new XQException("XQueryX is not supported");
        }
    }

    public void setScrollability(int scrollability) throws XQException {
        switch (scrollability) {
            case XQConstants.SCROLLTYPE_FORWARD_ONLY:
            case XQConstants.SCROLLTYPE_SCROLLABLE:
                this.scrollability = scrollability;
            default:
                throw new XQException("Invalid scrollability value - " + scrollability);
        }
    }

    private XQException newXQException(Exception err) {
        XQException xqe = new XQException(err.getMessage());
        xqe.initCause(err);
        return xqe;
    }

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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

