////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import net.sf.saxon.expr.instruct.ComponentBody;
import net.sf.saxon.style.StylesheetPackage;
import net.sf.saxon.trans.Visibility;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a component as defined in the XSLT 3.0 specification: for example a function, a named template,
 * an attribute set, a global variable.
 */

public class Component {

    private ComponentBody procedure;
    private Visibility visibility;
    private List<ComponentBinding> bindings = new ArrayList<ComponentBinding>();
    private StylesheetPackage owningPackage;
    private StylesheetPackage declaringPackage;
    private Component originalComponent;

    /**
     * Create a component
     *
     * @param procedure        the compiled code that implements the component, for example a Template or Function
     * @param visibility       the visibility of the component
     * @param owningPackage    the package to which this component belongs
     * @param declaringPackage the package in which the original declaration of the component appears
     */

    public Component(ComponentBody procedure, Visibility visibility, StylesheetPackage owningPackage, StylesheetPackage declaringPackage) {
        this.procedure = procedure;
        this.visibility = visibility;
        this.owningPackage = owningPackage;
        this.declaringPackage = declaringPackage;
    }

    /**
     * Get the component's binding vector; that is the list of external references to other components
     *
     * @return the binding vector, a list of component bindings. These are identified by a binding
     *         slot number held with the individual instruction (such as a call-template instruction or a global
     *         variable reference) that contains the external component reference.
     */

    public List<ComponentBinding> getComponentBindings() {
        return bindings;
    }

    /**
     * Set the component's binding vector; that is the list of external references to other components
     *
     * @param bindings the binding vector, a list of component bindings. These are identified by a binding
     *                 slot number held with the individual instruction (such as a call-template instruction or a global
     *                 variable reference) that contains the external component reference.
     */

    public void setComponentBindings(List<ComponentBinding> bindings) {
        this.bindings = bindings;
    }

    /**
     * Get the visibility of the component
     *
     * @return the component's visibility. In the declaring package this will be the original
     *         declared or exposed visibility; in a using package, it will be the visibility of the component
     *         within that package.
     */

    public Visibility getVisibility() {
        return visibility;
    }

    /**
     * Get the procedure (for example a compiled template, function, or variable) represented
     * by this component
     *
     * @return the procedure represented by this component
     */

    public ComponentBody getProcedure() {
        return procedure;
    }

    /**
     * Get the declaring package of this component
     *
     * @return the package in which the component was originally declared
     */

    public StylesheetPackage getDeclaringPackage() {
        return declaringPackage;
    }

    /**
     * Get the ownnig package of this component
     *
     * @return the package that owns this (version of the) component
     */

    public StylesheetPackage getOwningPackage() {
        return owningPackage;
    }

    /**
     * Get the component from which this one is derived
     * @return the component from which this one is derived. In the case of an overriding component
     * this will be the component in the used package that is being overridden. May be null.
     */

    public Component getOriginalComponent() {
        return originalComponent;
    }

    /**
     * Set the component from which this one is derived
     * @param original the component from which this one is derived. In the case of an overriding component
     * this will be the component in the used package that is being overridden.
     */

    public void setOriginalComponent(Component original) {
        originalComponent = original;
    }
}

