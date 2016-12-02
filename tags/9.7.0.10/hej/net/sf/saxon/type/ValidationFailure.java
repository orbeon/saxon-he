////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.type;

import net.sf.saxon.expr.parser.ExplicitLocation;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.lib.Invalidity;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.AbsolutePath;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.value.AtomicValue;

import javax.xml.transform.SourceLocator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This exception indicates a failure when validating an instance against a type
 * defined in a schema.
 * <p/>
 * <p>This class holds the same information as a ValidationException, except that it is not an exception,
 * and does not carry system overheads such as a stack trace. It is used because operations such as "castable",
 * and validation of values in a union, cause validation failures on a success path and it is costly to throw,
 * or even to create, exception objects on a success path.</p>
 */

public class ValidationFailure
        implements Location, ConversionResult, Invalidity {

    private String message;
    private String systemId;
    private String publicId;
    private int lineNumber = -1;
    private int columnNumber = -1;
    private AbsolutePath path;
    private AbsolutePath contextPath;
    private NodeInfo invalidNode;

    private int schemaPart = -1;
    private String constraintName;
    private String clause;
    private SchemaType schemaType;
    /*@Nullable*/ private StructuredQName errorCode;
    private ValidationException exception;
    private boolean hasBeenReported;
    private List<NodeInfo> offendingNodes;


    /**
     * Creates a new ValidationException with the given message.
     *
     * @param message the message for this Exception
     */
    public ValidationFailure(String message) {
        this.message = message;
        setErrorCode("FORG0001");
    }

    /**
     * Creates a new ValidationFailure with the given nested
     * exception.
     *
     * @param exception the nested exception
     */
    public ValidationFailure(/*@NotNull*/ Exception exception) {
        message = exception.getMessage();
        if (exception instanceof XPathException) {
            errorCode = ((XPathException) exception).getErrorCodeQName();
        } else {
            setErrorCode("FORG0001");
        }
        if (exception instanceof ValidationException) {
            ValidationException ve = (ValidationException) exception;
            this.exception = ve;
            this.message = ve.getMessage();
            this.errorCode = ve.getErrorCodeQName();
            this.systemId = ve.getSystemId();
            this.publicId = ve.getPublicId();
            this.lineNumber = ve.getLineNumber();
            this.columnNumber = ve.getColumnNumber();
            this.constraintName = ve.getConstraintName();
            this.clause = ve.getConstraintClauseNumber();
            this.schemaType = ve.getSchemaType();
            this.hasBeenReported = ve.hasBeenReported();
        }
    }

    /**
     * Set a reference to the constraint in XML Schema that is not satisfied
     *
     * @param schemaPart     - 1 or 2, depending whether the constraint is in XMLSchema part 1 or part 2
     * @param constraintName - the short name of the constraint in XMLSchema, as a fragment identifier in the
     *                       HTML of the XML Schema Part 1 specification
     * @param clause         - the clause number within the description of that constraint
     */

    public void setConstraintReference(int schemaPart, String constraintName, String clause) {
        this.schemaPart = schemaPart;
        this.constraintName = constraintName;
        this.clause = clause;
    }

    /**
     * Copy the constraint reference from another exception object
     *
     * @param e the other exception object from which to copy the information
     */

    public void setConstraintReference(/*@NotNull*/ ValidationFailure e) {
        schemaPart = e.schemaPart;
        constraintName = e.constraintName;
        clause = e.clause;
    }

    /**
     * Get the "schema part" component of the constraint reference
     *
     * @return 1 or 2 depending on whether the violated constraint is in XML Schema Part 1 or Part 2;
     *         or -1 if there is no constraint reference
     */

    public int getSchemaPart() {
        return schemaPart;
    }

    /**
     * Get the constraint name
     *
     * @return the name of the violated constraint, in the form of a fragment identifier within
     *         the published XML Schema specification; or null if the information is not available.
     */

    public String getConstraintName() {
        return constraintName;
    }

    /**
     * Get the constraint clause number
     *
     * @return the section number of the clause containing the constraint that has been violated.
     *         Generally a decimal number in the form n.n.n; possibly a sequence of such numbers separated
     *         by semicolons. Or null if the information is not available.
     */

    public String getConstraintClauseNumber() {
        return clause;
    }

    /**
     * Get the constraint name and clause in the format defined in XML Schema Part C (Outcome Tabulations).
     * This mandates the format validation-rule-name.clause-number
     *
     * @return the constraint reference, for example "cos-ct-extends.1.2"; or null if the reference
     *         is not known.
     */

    /*@NotNull*/
    public String getConstraintReference() {
        return constraintName + '.' + clause;
    }

    /**
     * Add information about an "offending node". This is used for assertions, where the assertion on a
     * particular node A places conditions on descendant nodes D: for example <code>every $n in .//* satisfies self::x</code>.
     * With that kind of assertion, the nodes in <code>.//*</code> that do not satisfy the condition are reported
     * as "offending nodes", although it is the root node containing the assertion that is technically invalid.
     *
     * @param node a node that fails to satisfy the conditions specified in an assertion
     */

    public void addOffendingNode(NodeInfo node) {
        if (offendingNodes == null) {
            offendingNodes = new ArrayList<NodeInfo>();
        }
        offendingNodes.add(node);
    }

    /**
     * Get the list of "offending nodes". This is used for assertions, where the assertion on a
     * particular node A places conditions on descendant nodes D: for example <code>every $n in .//* satisfies self::x</code>.
     * With that kind of assertion, the nodes in <code>.//*</code> that do not satisfy the condition are reported
     * as "offending nodes", although it is the root node containing the assertion that is technically invalid.
     *
     * @return the list of offending nodes
     */

    public List<NodeInfo> getOffendingNodes() {
        if (offendingNodes == null) {
            return Collections.emptyList();
        } else {
            return offendingNodes;
        }
    }


    public AbsolutePath getPath() {
        return path;
    }

    public void setPath(AbsolutePath path) {
        this.path = path;
    }

    public AbsolutePath getContextPath() {
        return contextPath;
    }

    public void setContextPath(AbsolutePath contextPath) {
        this.contextPath = contextPath;
    }

    public NodeInfo getInvalidNode() {
        return invalidNode;
    }

    public void setInvalidNode(NodeInfo invalidNode) {
        this.invalidNode = invalidNode;
    }



    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns the String representation of this Exception
     *
     * @return the String representation of this Exception
     */
    public String toString() {
        FastStringBuffer sb = new FastStringBuffer("ValidationException: ");
        String message = getMessage();
        if (message != null) {
            sb.append(message);
        }
        return sb.toString();
    }

    public String getPublicId() {
        SourceLocator loc = getLocator();
        if (publicId == null && loc != null && loc != this) {
            return loc.getPublicId();
        } else {
            return publicId;
        }
    }

    public String getSystemId() {
        SourceLocator loc = getLocator();
        if (systemId == null && loc != null && loc != this) {
            return loc.getSystemId();
        } else {
            return systemId;
        }
    }

    public int getLineNumber() {
        SourceLocator loc = getLocator();
        if (lineNumber == -1 && loc != null && loc != this) {
            return loc.getLineNumber();
        } else {
            return lineNumber;
        }
    }

    public int getColumnNumber() {
        SourceLocator loc = getLocator();
        if (columnNumber == -1 && loc != null && loc != this) {
            return loc.getColumnNumber();
        } else {
            return columnNumber;
        }
    }

    /**
     * Get an immutable copy of this Location object. By default Location objects may be mutable, so they
     * should not be saved for later use. The result of this operation holds the same location information,
     * but in an immutable form.
     */
    public Location saveLocation() {
        return new ExplicitLocation(this);
    }

    public void setPublicId(String id) {
        publicId = id;
    }

    public void setSystemId(String id) {
        systemId = id;
    }

    public void setLineNumber(int line) {
        lineNumber = line;
    }

    public void setColumnNumber(int column) {
        columnNumber = column;
    }

    public void setLocator(/*@Nullable*/ SourceLocator locator) {
        if (locator != null) {
            setPublicId(locator.getPublicId());
            setSystemId(locator.getSystemId());
            setLineNumber(locator.getLineNumber());
            setColumnNumber(locator.getColumnNumber());
        }
    }

    public void setSourceLocator(/*@Nullable*/ SourceLocator locator) {
        if (locator != null) {
            setPublicId(locator.getPublicId());
            setSystemId(locator.getSystemId());
            setLineNumber(locator.getLineNumber());
            setColumnNumber(locator.getColumnNumber());
        }
    }

    /*@NotNull*/
    public Location getLocator() {
        return this;
    }

    public void setErrorCode(String errorCode) {
        if (errorCode == null) {
            this.errorCode = null;
        } else {
            this.errorCode = new StructuredQName("err", NamespaceConstant.ERR, errorCode);
        }
    }

    public void setErrorCodeQName(StructuredQName errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Get the error code associated with the validity error. This is relevant only when validation
     * is run from within XSLT or XQuery, which define different error codes for validition errors
     *
     * @return the error code associated with the error, if any. The error is returned as a simple
     * string if it is in the standard error namespace, or as an EQName (that is Q{uri}local) otherwise.
     */
    public String getErrorCode() {
        if (errorCode == null) {
            return null;
        } else if (errorCode.hasURI(NamespaceConstant.ERR)) {
            return errorCode.getLocalPart();
        } else {
            return errorCode.getEQName();
        }
    }

    /*@Nullable*/
    public StructuredQName getErrorCodeQName() {
        return errorCode;
    }

    public void setSchemaType(SchemaType type) {
        schemaType = type;
    }

    public SchemaType getSchemaType()  {
        return schemaType;
    }

    /*@NotNull*/
    public ValidationException makeException() {
        if (exception != null) {
            exception.maybeSetLocation(this);
            return exception;
        }
        ValidationException ve = new ValidationException(message, getLocator());
        ve.setConstraintReference(schemaPart, constraintName, clause);
        if (errorCode == null) {
            ve.setErrorCode("FORG0001");
        } else {
            ve.setErrorCodeQName(errorCode);
        }
        ve.setHasBeenReported(hasBeenReported);
        ve.offendingNodes = offendingNodes;
        exception = ve;
        return ve;
    }

    /*@Nullable*/
    public ValidationException makeException(/*@Nullable*/ String contextMessage) {
        ValidationException ve = new ValidationException((contextMessage == null ? message : contextMessage + message));
        ve.setConstraintReference(schemaPart, constraintName, clause);
        if (errorCode == null) {
            ve.setErrorCode("FORG0001");
        } else {
            ve.setErrorCodeQName(errorCode);
        }
        return ve;
    }


    /**
     * Calling this method on a ConversionResult returns the AtomicValue that results
     * from the conversion if the conversion was successful, and throws a ValidationException
     * explaining the conversion error otherwise.
     * <p/>
     * <p>Use this method if you are calling a conversion method that returns a ConversionResult,
     * and if you want to throw an exception if the conversion fails.</p>
     *
     * @return the atomic value that results from the conversion if the conversion was successful
     * @throws net.sf.saxon.type.ValidationException
     *          if the conversion was not successful
     */

    /*@NotNull*/
    public AtomicValue asAtomic() throws ValidationException {
        throw makeException();
    }

    public boolean hasBeenReported() {
        return hasBeenReported;
    }

    public void setHasBeenReported(boolean reported) {
        hasBeenReported = reported;
        if (exception != null) {
            exception.setHasBeenReported(reported);
        }
    }

    /**
     * Get additional location text, if any. This gives extra information about the position of the error
     * in textual form. Where XPath is embedded within a host language such as XSLT, the
     * formal location information identifies the location of the error in the XSLT module,
     * while this string locates the error within a specific XPath expression. The information
     * is typically used only for static errors.
     *
     * @return additional information about the location of the error, designed to be output
     * as a prefix to the error message if desired. (It is not concatenated with the message, because
     * it may be superfluous in an IDE environment.)
     */

    public String getValidationLocationText() {
        FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.C256);
        AbsolutePath valPath = getAbsolutePath();
        if (valPath != null) {
            fsb.append("Validating ");
            fsb.append(valPath.getPathUsingPrefixes());
            if (valPath.getSystemId() != null) {
                fsb.append(" in ");
                fsb.append(valPath.getSystemId());
            }
        }
        return fsb.toString();
    }


    /**
     * Get additional location text, if any. This gives extra information about the position of the error
     * in textual form. Where XPath is embedded within a host language such as XSLT, the
     * formal location information identifies the location of the error in the XSLT module,
     * while this string locates the error within a specific XPath expression. The information
     * is typically used only for static errors.
     *
     * @return additional information about the location of the error, designed to be output
     * as a prefix to the error message if desired. (It is not concatenated with the message, because
     * it may be superfluous in an IDE environment.)
     */

    public String getContextLocationText() {
        FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.C256);
        AbsolutePath contextPath = getContextPath();
        if (contextPath != null) {
            fsb.append("Currently processing ");
            fsb.append(contextPath.getPathUsingPrefixes());
            if (contextPath.getSystemId() != null) {
                fsb.append(" in ");
                fsb.append(contextPath.getSystemId());
            }
        }
        return fsb.toString();
    }

    /**
     * Get the location of the error as a structured path object
     *
     * @return the location, as a structured path object indicating the position of the error within the containing document
     */

    public AbsolutePath getAbsolutePath() {
        if (path != null) {
            return path;
//        } else if (node != null) {
//            return Navigator.getAbsolutePath(node);
        } else {
            return null;
        }
    }


}
