////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.instruct;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.LocationProvider;
import net.sf.saxon.expr.*;
import net.sf.saxon.style.StylesheetPackage;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trans.SymbolicName;
import net.sf.saxon.trans.Visibility;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This object represents the compiled form of a user-written function, template, attribute-set, etc
 * (the source can be either an XSLT stylesheet function or an XQuery function).
 * <p/>
 * <p>It is assumed that type-checking, of both the arguments and the results,
 * has been handled at compile time. That is, the expression supplied as the body
 * of the function must be wrapped in code to check or convert the result to the
 * required type, and calls on the function must be wrapped at compile time to check or
 * convert the supplied arguments.
 */

public abstract class ComponentBody implements Container, InstructionInfo {

    protected Expression body;
    private String systemId;
    private int lineNumber;
    private SlotManager stackFrameMap;
    private int hostLanguage;
    private PackageData packageData;
    private Component declaringComponent;

    public ComponentBody() {
    }

    /**
     * Set basic data about the unit of compilation (XQuery module, XSLT package) to which this
     * procedure belongs
     *
     * @param packageData information about the containing package
     */

    public void setPackageData(PackageData packageData) {
        this.packageData = packageData;
    }

    /**
     * Get basic data about the unit of compilation (XQuery module, XSLT package) to which this
     * container belongs
     */
    public PackageData getPackageData() {
        return packageData;
    }

    /**
     * Get the granularity of the container.
     *
     * @return 0 for a temporary container created during parsing; 1 for a container
     *         that operates at the level of an XPath expression; 2 for a container at the level
     *         of a global function or template
     */

    public int getContainerGranularity() {
        return 2;
    }

    public void makeDeclaringComponent(Visibility visibility, StylesheetPackage declaringPackage) {
        if (declaringComponent == null) {
            declaringComponent = new Component(this, visibility, declaringPackage, declaringPackage);
        }
    }

    public Component getDeclaringComponent() {
        return declaringComponent;
    }

    /**
     * Allocate slot numbers to all the external component references in this procedure
     * @param pack the containing package
     */

    public void allocateAllBindingSlots(StylesheetPackage pack) {
        if (getBody() != null) {
            allocateBindingSlotsRecursive(pack, this, getBody());
        }
    }

    protected static void allocateBindingSlotsRecursive(StylesheetPackage pack, ComponentBody p, Expression exp) {
        if (exp instanceof ComponentInvocation) {
            ComponentInvocation invocation = (ComponentInvocation)exp;
            SymbolicName name = invocation.getSymbolicName();
//            Component target = invocation.getTarget();
//            if (target == null && pack != null) {
//                target = pack.getComponent(name);
//            }
            Component target = pack.getComponent(name);
            if (target == null) {
                throw new AssertionError("Target of component reference " + name + " is undefined");
            }
            int slot = p.allocateBindingSlot(name, target);
            invocation.setBindingSlot(slot);
        }
        for (Operand o : exp.operands()) {
            allocateBindingSlotsRecursive(pack, p, o.getExpression());
        }
    }

    private int allocateBindingSlot(SymbolicName name, Component target) {
        List<ComponentBinding> bindings = declaringComponent.getComponentBindings();
        int slot = bindings.size();
        ComponentBinding cb = new ComponentBinding(name);
        cb.setTarget(target, target.getVisibility() == Visibility.PRIVATE || target.getVisibility() == Visibility.FINAL);
        bindings.add(cb);
        return slot;
    }

    /**
     * Get the Configuration to which this Container belongs
     *
     * @return the Configuration
     */
    public Configuration getConfiguration() {
        return packageData.getConfiguration();
    }

    public void setBody(Expression body) {
        this.body = body;
        if (body != null) {
            body.setContainer(this);
        }
    }

    public void setHostLanguage(int language) {
        hostLanguage = language;
    }

    public int getHostLanguage() {
        return hostLanguage;
    }

    public final Expression getBody() {
        return body;
    }

    public void setStackFrameMap(SlotManager map) {
        stackFrameMap = map;
    }

    public SlotManager getStackFrameMap() {
        return stackFrameMap;
    }

    /**
     * Get the LocationProvider allowing location identifiers to be resolved.
     */

    public LocationProvider getLocationProvider() {
        //return this;
        return getPackageData().getLocationMap();
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getSystemId() {
        return systemId;
    }

    public int getColumnNumber() {
        return -1;
    }

    /*@Nullable*/
    public String getPublicId() {
        return null;
    }

    public Object getProperty(String name) {
        return null;
    }


    /**
     * Get an iterator over all the properties available. The values returned by the iterator
     * will be of type String, and each string can be supplied as input to the getProperty()
     * method to retrieve the value of the property. The iterator may return properties whose
     * value is null.
     */

    public Iterator<String> getProperties() {
        final List<String> list = Collections.emptyList();
        return list.iterator();
    }

    /**
     * Get the kind of component that this represents, using integer constants such as
     * {@link net.sf.saxon.om.StandardNames#XSL_FUNCTION}
     */

    public abstract int getComponentKind();
}


