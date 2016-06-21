package net.sf.saxon.style;


import net.sf.saxon.expr.Component;
import net.sf.saxon.trans.XPathException;

/**
 * An abstraction of the capability of the xsl:accept declaration, provided because the real xsl:accept
 * element is not available in Saxon-HE. Conceptually, the ComponentAcceptor is a rule that modifies
 * the visibility of components accepted from a used package.
 */

public interface ComponentAcceptor {

    /**
     * Accept a component from a used package, modifying its visibility if necessary
     * @param component the component to be accepted; as a side-effect of this method, the
     *                  visibility of the component may change
     * @throws XPathException if the requested visibility is incompatible with the declared
     * visibility
     */

    void acceptComponent(Component component) throws XPathException;
}

