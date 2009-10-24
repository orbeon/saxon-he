package net.sf.saxon.expr;

import net.sf.saxon.instruct.Executable;
import net.sf.saxon.event.LocationProvider;

import javax.xml.transform.SourceLocator;
import java.io.Serializable;

/**
 * A Container is something other than an expression that can act as the container of an expression.
 * It is typically an object such as a function, a global variable, or in XSLT a template, or an attribute set.
 * When free-standing XPath expressions are compiled, the static context for the expression acts as its
 * container.
 */

public interface Container extends SourceLocator, Serializable {

    /**
     * Get the Executable (representing a complete stylesheet or query) of which this Container forms part
     * @return the executable
     */

    public Executable getExecutable();

    /**
     * Get the LocationProvider allowing location identifiers to be resolved.
     * @return the location provider
     */

    public LocationProvider getLocationProvider();

    /**
     * Get the host language (XSLT, XQuery, XPath) used to implement the code in this container
     * @return typically {@link net.sf.saxon.Configuration#XSLT} or {@link net.sf.saxon.Configuration#XQUERY}
     */

    public int getHostLanguage();

    /**
     * Get the granularity of the container. During successive phases of compilation, growing
     * expression trees are rooted in containers of increasing granularity. The granularity
     * of the container is used to avoid "repotting" a tree more frequently than is required,
     * as this requires a complete traversal of the tree which can take a measurable time.
     * @return 0 for a temporary container created during parsing; 1 for a container
     * that operates at the level of an XPath expression; 2 for a container at the level
     * of a global function or template
     */

    public int getContainerGranularity();
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s): Michael Kay
//
