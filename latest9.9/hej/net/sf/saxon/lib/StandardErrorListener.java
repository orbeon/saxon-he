////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.lib;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.instruct.*;
import net.sf.saxon.expr.parser.ExplicitLocation;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.expr.parser.XPathParser;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.regex.BMPString;
import net.sf.saxon.regex.GeneralUnicodeString;
import net.sf.saxon.regex.LatinString;
import net.sf.saxon.regex.UnicodeString;
import net.sf.saxon.serialize.charcode.UTF16CharacterSet;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trace.LocationKind;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.KeyDefinition;
import net.sf.saxon.trans.Mode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.trans.rules.BuiltInRuleSet;
import net.sf.saxon.trans.rules.Rule;
import net.sf.saxon.tree.AttributeLocation;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.tree.util.Navigator;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.ValidationException;
import net.sf.saxon.type.ValidationFailure;
import org.xml.sax.SAXException;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMLocator;
import java.util.List;

/**
 * <B>StandardErrorListener</B> is the standard error handler for XSLT and XQuery processing
 * errors, used if no other ErrorListener is nominated.
 *
 * @author Michael H. Kay
 */

public class StandardErrorListener implements UnfailingErrorListener {

    private int recoveryPolicy = Configuration.RECOVER_WITH_WARNINGS;
    private int requestedRecoveryPolicy = Configuration.RECOVER_WITH_WARNINGS;
    private int warningCount = 0;
    private int maximumNumberOfWarnings = 25;
    private int maxOrdinaryCharacter = 255;
    private int stackTraceDetail = 2;
    protected transient Logger logger = new StandardLogger();

    /**
     * Create a Standard Error Listener
     */

    public StandardErrorListener() {
    }

    /**
     * Make a clean copy of this ErrorListener. This is necessary because the
     * standard error listener is stateful (it remembers how many errors there have been)
     *
     * @param hostLanguage the host language (not used by this implementation)
     * @return a copy of this error listener
     */

    public StandardErrorListener makeAnother(int hostLanguage) {
        StandardErrorListener sel;
        try {
            sel = this.getClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            sel = new StandardErrorListener();
        }
        sel.logger = logger;
        return sel;
    }

    // Note, when the standard error listener is used, a new
    // one is created for each transformation, because it holds
    // the recovery policy and the warning count.

    /**
     * Set output destination for error messages (default is System.err)
     *
     * @param logger The Logger to use for error messages
     */

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Get the error output stream
     *
     * @return the error output stream
     */

    public Logger getLogger() {
        return logger;
    }

    /**
     * Set the recovery policy
     *
     * @param policy the recovery policy for XSLT recoverable errors. One of
     *               {@link Configuration#RECOVER_SILENTLY},
     *               {@link Configuration#RECOVER_WITH_WARNINGS},
     *               {@link Configuration#DO_NOT_RECOVER}.
     */

    public void setRecoveryPolicy(int policy) {
        recoveryPolicy = policy;
        requestedRecoveryPolicy = policy;
    }

    /**
     * Get the recovery policy
     *
     * @return the recovery policy for XSLT recoverable errors. One of
     *         {@link Configuration#RECOVER_SILENTLY},
     *         {@link Configuration#RECOVER_WITH_WARNINGS},
     *         {@link Configuration#DO_NOT_RECOVER}.
     */

    public int getRecoveryPolicy() {
        return recoveryPolicy;
    }

    /**
     * Get the requested recovery policy. This returns the original policy, which may differ
     * from the current policy, in the case where the current policy has switched from "warnings"
     * to "silent" because too many warnings have been issued
     * @return the original requested recovery policy
     */

    public int getRequestedRecoveryPolicy() {
        return requestedRecoveryPolicy;
    }

    /**
     * Set the maximum number of warnings that are reported; further warnings after this limit
     * are silently ignored
     *
     * @param max the maximum number of warnings output
     */

    public void setMaximumNumberOfWarnings(int max) {
        this.maximumNumberOfWarnings = max;
    }

    /**
     * Get the maximum number of warnings that are reported; further warnings after this limit
     * are silently ignored
     *
     * @return the maximum number of warnings output
     */

    public int getMaximumNumberOfWarnings() {
        return this.maximumNumberOfWarnings;
    }

    /**
     * Set the level of information to be included in a stack trace when a dynamic
     * error occurs.
     * @param level set to 0 (zero) for no stack trace; 1 (one) for a stack trace
     *              showing the templates and functions being executed; 2 (two) to
     *              add values of local variables and parameters (available in Saxon-EE only)
     *              Default is the maximum level available.
     */

    public void setStackTraceDetail(int level) {
        stackTraceDetail = level;
    }

    /**
     * Get the level of information to be included in a stack trace when a dynamic
     * error occurs.
     *
     * @return 0 (zero) for no stack trace; 1 (one) for a stack trace
     *              showing the templates and functions being executed; 2 (two) to
     *              add values of local variables and parameters (available in Saxon-EE only)
     *              Default is the maximum level available.
     */

    public int getStackTraceDetail() {
        return stackTraceDetail;
    }

    /**
     * Set the maximum codepoint value for a character to be considered non-special.
     * Special characters (codepoints above this value) will be expanded in hex for
     * extra clarity in the error message.
     * @param max the highest codepoint considered non-special (defaults to 255)
     */

    public void setMaxOrdinaryCharacter(int max) {
        maxOrdinaryCharacter = max;
    }

    /**
     * Set the maximum codepoint value for a character to be considered non-special.
     * Special characters (codepoints above this value) will be expanded in hex for
     * extra clarity in the error message.
     *
     * @param max the highest codepoint considered non-special (defaults to 255)
     */

    public int getMaxOrdinaryCharacter(int max) {
        return maxOrdinaryCharacter;
    }


    /**
     * Receive notification of a warning.
     * <p>Transformers can use this method to report conditions that
     * are not errors or fatal errors.  The default behaviour is to
     * take no action.</p>
     * <p>After invoking this method, the Transformer must continue with
     * the transformation. It should still be possible for the
     * application to process the document through to the end.</p>
     *
     * @param exception The warning information encapsulated in a
     *                  transformer exception.
     * @see javax.xml.transform.TransformerException
     */

    public void warning(TransformerException exception) {

        if (recoveryPolicy == Configuration.RECOVER_SILENTLY) {
            // do nothing
            return;
        }

        if (logger == null) {
            // can happen after deserialization
            logger = new StandardLogger();
        }
        XPathException xe = XPathException.makeXPathException(exception);
        String message = constructMessage(exception, xe, "", "Warning ");

        if (exception instanceof ValidationException) {
            logger.error(message);

        } else {
            logger.warning(message);
            warningCount++;
            if (warningCount > getMaximumNumberOfWarnings()) {
                logger.info("No more warnings will be displayed");
                recoveryPolicy = Configuration.RECOVER_SILENTLY;
                warningCount = 0;
            }
        }
    }

    /**
     * Receive notification of a recoverable error.
     * <p>The transformer must continue to provide normal parsing events
     * after invoking this method.  It should still be possible for the
     * application to process the document through to the end.</p>
     * <p>The action of the standard error listener depends on the
     * recovery policy that has been set, which may be one of RECOVER_SILENTLY,
     * RECOVER_WITH_WARNING, or DO_NOT_RECOVER</p>
     *
     * @param exception The error information encapsulated in a
     *                  transformer exception.
     * @see TransformerException
     */

    public void error(TransformerException exception) {
        if (recoveryPolicy == Configuration.RECOVER_SILENTLY && !(exception instanceof ValidationException)) {
            // do nothing
            return;
        }
        if (logger == null) {
            // can happen after deserialization
            logger = new StandardLogger();
        }
        String message;
        if (exception instanceof ValidationException) {
            String explanation = getExpandedMessage(exception);
            ValidationFailure failure = ((ValidationException) exception).getValidationFailure();
            String constraintReference = failure.getConstraintReferenceMessage();
            String validationLocation = failure.getValidationLocationText();
            String contextLocation = failure.getContextLocationText();
            message = "Validation error " +
                    getLocationMessage(exception) +
                    "\n  " +
                    wordWrap(explanation) +
                    wordWrap(validationLocation.isEmpty() ? "" : "\n  " + validationLocation) +
                    wordWrap(contextLocation.isEmpty() ? "" : "\n  " + contextLocation) +
                    wordWrap(constraintReference == null ? "" : "\n  " + constraintReference) +
                    getOffenderListText(failure);
        } else {
            String prefix = recoveryPolicy == Configuration.RECOVER_WITH_WARNINGS ?
                    "Recoverable error " : "Error ";
            message = constructMessage(exception, XPathException.makeXPathException(exception), "", prefix);
        }

        if (exception instanceof ValidationException) {
            logger.error(message);

        } else if (recoveryPolicy == Configuration.RECOVER_WITH_WARNINGS) {
            logger.warning(message);
            warningCount++;
            if (warningCount > getMaximumNumberOfWarnings()) {
                logger.info("No more warnings will be displayed");
                recoveryPolicy = Configuration.RECOVER_SILENTLY;
                warningCount = 0;
            }
        } else {
            logger.error(message);
            logger.info("Processing terminated because error recovery is disabled");
            //throw XPathException.makeXPathException(exception);
        }
    }

    /**
     * Receive notification of a non-recoverable error.
     * <p>The application must assume that the transformation cannot
     * continue after the Transformer has invoked this method,
     * and should continue (if at all) only to collect
     * addition error messages. In fact, Transformers are free
     * to stop reporting events once this method has been invoked.</p>
     *
     * @param exception The error information encapsulated in a
     *                  transformer exception.
     */

    public void fatalError(TransformerException exception) {
        XPathException xe = XPathException.makeXPathException(exception);
        if (xe.hasBeenReported()) {
            // don't report the same error twice
            return;
        }
        if (logger == null) {
            // can happen after deserialization
            logger = new StandardLogger();
        }
        String message;

        String lang = xe.getHostLanguage();
        String langText = "";
        if ("XPath".equals(lang)) {
            langText = "in expression ";
        } else if ("XQuery".equals(lang)) {
            langText = "in query ";
        } else if ("XSLT Pattern".equals(lang)) {
            langText = "in pattern ";
        }
        String kind = "Error ";
        if (xe.isSyntaxError()) {
            kind = "Syntax error ";
        } else if (xe.isStaticError()) {
            kind = "Static error ";
        } else if (xe.isTypeError()) {
            kind = "Type error ";
        }

        message = constructMessage(exception, xe, langText, kind);

        logger.error(message);
        if (exception instanceof XPathException) {
            ((XPathException) exception).setHasBeenReported(true);
            // probably redundant. It's the caller's job to set this flag, because there might be
            // a non-standard error listener in use.
        }

        if (exception instanceof XPathException) {
            XPathContext context = ((XPathException) exception).getXPathContext();
            if (context != null && getRequestedRecoveryPolicy() != Configuration.RECOVER_SILENTLY
                    && !(context instanceof EarlyEvaluationContext)) {
                outputStackTrace(logger, context);
            }
        }
    }

    private String constructMessage(TransformerException exception, XPathException xe, String langText, String kind) {
        String message;
        Expression failingExpression = null;
        if (exception instanceof XPathException) {
            failingExpression = ((XPathException)exception).getFailingExpression();
        }
        if (xe.getLocator() instanceof AttributeLocation) {
            String line1 = kind + /*where +*/ langText + getLocationMessageText(xe.getLocator()) + "\n";
            String line2 = "  " + wordWrap(getExpandedMessage(exception));
            message = line1 + line2;
        } else if (xe.getLocator() instanceof XPathParser.NestedLocation) {

            XPathParser.NestedLocation nestedLoc = (XPathParser.NestedLocation)xe.getLocator();
            Location outerLoc = nestedLoc.getContainingLocation();

            if (outerLoc instanceof AttributeLocation) {
                // Typical XSLT case
                int line = nestedLoc.getLocalLineNumber();
                int column = nestedLoc.getColumnNumber();
                String innerLoc = "";
                String lineInfo = line <= 0 ? "" : "on line " + line + ' ';
                String columnInfo = column <= 0 ? "" : "at " + (line <= 0 ? "char " : "column ") + column + ' ';
                String nearBy = nestedLoc.getNearbyText();

                innerLoc = lineInfo + columnInfo;
                if (failingExpression != null) {
                    innerLoc = lineInfo + "evaluating (" + failingExpression.toShortString() + ") ";
                } else if (nearBy != null && !nearBy.isEmpty()) {
                    innerLoc = lineInfo + (nearBy.startsWith("...") ? "near" : "in") +
                            ' ' + Err.wrap(nearBy) + ' ' + columnInfo;
                }

                String where = innerLoc;
                String line1 = kind + where + langText + getLocationMessageText(outerLoc) + "\n";
                String line2 = "  " + wordWrap(getExpandedMessage(exception));
                message = line1 + line2;

            } else {

                int line = nestedLoc.getLocalLineNumber();
                int column = nestedLoc.getColumnNumber();
                String innerLoc = "";

                // Typical XQuery case; no extra information available from the outer location
                innerLoc += line < 0 ? "" : "on line " + (line+1) + ' ';
                if (column >= 0) {
                    innerLoc += "at " + (line < 0 ? "char " : "column ") + (column+1) + ' ';
                }
                if (outerLoc.getLineNumber() > 1) {
                    innerLoc += "(" + langText + "on line " + outerLoc.getLineNumber() + ") ";
                }
                if (outerLoc.getSystemId() != null) {
                    innerLoc += "of " + outerLoc.getSystemId() + " ";
                }
                String nearBy = nestedLoc.getNearbyText();
                if (failingExpression != null) {
                    innerLoc += "evaluating (" + failingExpression.toShortString() + ")";
                } else if (nearBy != null && !nearBy.isEmpty()) {
                    innerLoc += (nearBy.startsWith("...") ? "near" : "in") +
                            ' ' + Err.wrap(nearBy) + ' ';
                }
                String line1 = kind + innerLoc + "\n";

                String line2 = "  " + wordWrap(getExpandedMessage(exception));
                message = line1 + line2;
            }

        } else if (xe instanceof ValidationException) {
            String explanation = getExpandedMessage(exception);
            ValidationFailure failure = ((ValidationException)xe).getValidationFailure();
            String constraintReference = failure.getConstraintReferenceMessage();
            if (constraintReference != null) {
                explanation += " (" + constraintReference + ')';
            }
            message = "Validation error " +
                    getLocationMessage(exception) +
                    "\n  " +
                    wordWrap(explanation);
            message += getOffenderListText(failure);
        } else {
            message = kind +
                    (failingExpression != null ? "evaluating (" + failingExpression.toShortString() + ") " : "") +
                    getLocationMessage(exception) +
                    "\n  " +
                    wordWrap(getExpandedMessage(exception));

        }
        return expandSpecialCharacters(message).toString();
    }

    public static String getOffenderListText(ValidationFailure failure) {
        StringBuilder message = new StringBuilder();
        List<NodeInfo> offendingNodes = failure.getOffendingNodes();
        if (!offendingNodes.isEmpty()) {
            message.append("\n  Nodes for which the assertion fails:");
            for (NodeInfo offender : offendingNodes) {
                String nodeDesc = Type.displayTypeName(offender);
                if (offender.getNodeKind() == Type.TEXT) {
                    nodeDesc += " " + Err.wrap(offender.getStringValueCS(), Err.VALUE);
                }
                if (offender.getLineNumber() != -1) {
                    nodeDesc += " on line " + offender.getLineNumber();
                    if (offender.getColumnNumber() != -1) {
                        nodeDesc += " column " + offender.getColumnNumber();
                    }
                    if (offender.getSystemId() != null) {
                        nodeDesc += " of " + offender.getSystemId();
                    }
                } else {
                    nodeDesc += " at " + Navigator.getPath(offender);
                }
                message.append("\n  * ").append(nodeDesc);
            }
        }
        return message.toString();
    }

    /**
     * Generate a stack trace. This method is protected so it can be overridden in a subclass.
     *
     * @param out     the destination for the stack trace
     * @param context the context (which holds the information to be output)
     */

    protected void outputStackTrace(Logger out, XPathContext context) {
        printStackTrace(context, out, stackTraceDetail);
    }

    /**
     * Get a string identifying the location of an error.
     *
     * @param err the exception containing the location information
     * @return a message string describing the location
     */

    public String getLocationMessage(TransformerException err) {
        SourceLocator loc = err.getLocator();
        while (loc == null) {
            if (err.getException() instanceof TransformerException) {
                err = (TransformerException) err.getException();
                loc = err.getLocator();
            } else if (err.getCause() instanceof TransformerException) {
                err = (TransformerException) err.getCause();
                loc = err.getLocator();
            } else {
                return "";
            }
        }
        return getLocationMessageText(loc);
    }

    public static String getLocationMessageText(SourceLocator loc) {
        String locMessage = "";
        String systemId = null;
        NodeInfo node = null;
        String path;
        String nodeMessage = null;
        int lineNumber = -1;
        if (loc == null) {
            loc = ExplicitLocation.UNKNOWN_LOCATION;
        }
        if (loc instanceof XPathParser.NestedLocation) {
            loc = ((XPathParser.NestedLocation)loc).getContainingLocation();
        }
        if (loc instanceof AttributeLocation) {
            AttributeLocation saLoc = (AttributeLocation)loc;
            nodeMessage = "in " + saLoc.getElementName().getDisplayName();
            if (saLoc.getAttributeName() != null) {
                nodeMessage += "/@" + saLoc.getAttributeName();
            }
            nodeMessage += ' ';
        } else if (loc instanceof DOMLocator) {
            nodeMessage = "at " + ((DOMLocator) loc).getOriginatingNode().getNodeName() + ' ';
        } else if (loc instanceof NodeInfo) {
            node = (NodeInfo) loc;
            nodeMessage = "at " + node.getDisplayName() + ' ';
        } else if (loc instanceof ValidationException && (node = ((ValidationException) loc).getNode()) != null) {
            nodeMessage = "at " + node.getDisplayName() + ' ';
        } else if (loc instanceof ValidationException && loc.getLineNumber() == -1 && (path = ((ValidationException) loc).getPath()) != null) {
            nodeMessage = "at " + path + ' ';
        } else if (loc instanceof Instruction) {
            String instructionName = getInstructionName((Instruction) loc);
            if (!"".equals(instructionName)) {
                nodeMessage = "at " + instructionName + ' ';
            }
            systemId = loc.getSystemId();
            lineNumber = loc.getLineNumber();
        } else if (loc instanceof Actor) {
            String kind = "procedure";
            if (loc instanceof UserFunction) {
                kind = "function";
            } else if (loc instanceof NamedTemplate) {
                kind = "template";
            } else if (loc instanceof AttributeSet) {
                kind = "attribute-set";
            } else if (loc instanceof KeyDefinition) {
                kind = "key";
            }
            systemId = loc.getSystemId();
            lineNumber = loc.getLineNumber();
            nodeMessage = "at " + kind + " ";
            StructuredQName name = ((InstructionInfo) loc).getObjectName();
            if (name != null) {
                nodeMessage += name.toString();
                nodeMessage += " ";
            }
        }
        if (lineNumber == -1) {
            lineNumber = loc.getLineNumber();
        }
        boolean containsLineNumber = lineNumber != -1;
        if (node != null && !containsLineNumber) {
            nodeMessage = "at " + Navigator.getPath(node) + ' ';
        }
        if (nodeMessage != null) {
            locMessage += nodeMessage;
        }
        if (containsLineNumber) {
            locMessage += "on line " + lineNumber + ' ';
            if (loc.getColumnNumber() != -1) {
                locMessage += "column " + loc.getColumnNumber() + ' ';
            }
        }

        if (systemId != null && systemId.isEmpty()) {
            systemId = null;
        }
        if (systemId == null) {
            try {
                systemId = loc.getSystemId();
            } catch (Exception err) {
                err.printStackTrace();
                // no action (can fail with NPE if the expression tree is corrupt)
            }
        }
        if (systemId != null && !systemId.isEmpty()) {
            locMessage += (containsLineNumber ? "of " : "in ") + abbreviatePath(systemId) + ':';
        }
        return locMessage;
    }

    /**
     * Abbreviate a URI (if requested)
     *
     * @param uri the URI to be abbreviated
     * @return the abbreviated URI, unless full path names were requested, in which case
     *         the URI as supplied
     */

    /*@Nullable*/
    public static String abbreviatePath(String uri) {
        if (uri == null) {
            return "*unknown*";
        }
        int slash = uri.lastIndexOf('/');
        if (slash >= 0 && slash < uri.length() - 1) {
            return uri.substring(slash + 1);
        } else {
            return uri;
        }
    }

    /**
     * Get a string containing the message for this exception and all contained exceptions
     *
     * @param err the exception containing the required information
     * @return a message that concatenates the message of this exception with its contained exceptions,
     *         also including information about the error code and location.
     */

    public String getExpandedMessage(TransformerException err) {

        StructuredQName qCode = null;
        if (err instanceof XPathException) {
            qCode = ((XPathException) err).getErrorCodeQName();
        }
        if (qCode == null && err.getException() instanceof XPathException) {
            qCode = ((XPathException) err.getException()).getErrorCodeQName();
        }
        String message = "";
        if (qCode != null) {
            if (qCode.hasURI(NamespaceConstant.ERR)) {
                message = qCode.getLocalPart();
            } else {
                message = qCode.getDisplayName();
            }
        }

        if (err instanceof XPathException) {
            Sequence<?> errorObject = ((XPathException) err).getErrorObject();
            if (errorObject != null) {
                String errorObjectDesc = getErrorObjectString(errorObject);
                if (errorObjectDesc != null) {
                    message += " " + errorObjectDesc;
                }
            }
        }

        Throwable e = err;
        while (true) {
            if (e == null) {
                break;
            }
            String next = e.getMessage();
            if (next == null) {
                next = "";
            }
            if (next.startsWith("net.sf.saxon.trans.XPathException: ")) {
                next = next.substring(next.indexOf(": ") + 2);
            }
            if (!("TRaX Transform Exception".equals(next) || message.endsWith(next))) {
                if (!"".equals(message) && !message.trim().endsWith(":")) {
                    message += ": ";
                }
                message += next;
            }
            if (e instanceof TransformerException) {
                e = ((TransformerException) e).getException();
            } else if (e instanceof SAXException) {
                e = ((SAXException) e).getException();
            } else {
                // e.printStackTrace();
                break;
            }
        }

        return message;
    }

    /**
     * Get a string representation of the error object associated with the exception (this represents
     * the final argument to fn:error, in the case of error triggered by calls on the fn:error function).
     * The standard implementation returns null, meaning that the error object is not displayed; but
     * the method can be overridden in a subclass to create a custom display of the error object, which
     * is then appended to the message text.
     *
     * @param errorObject the error object passed as the last argument to fn:error. Note: this method is
     *                    not called if the error object is absent/null
     * @return a string representation of the error object to be appended to the message, or null if no
     *         output of the error object is required
     */

    @SuppressWarnings({"UnusedParameters"})
    public String getErrorObjectString(Sequence<?> errorObject) {
        return null;
    }

    /**
     * Extract a name identifying the instruction at which an error occurred
     *
     * @param inst the provider of information
     * @return the name of the containing instruction or expression, in user-meaningful terms
     */

    public static String getInstructionName(Instruction inst) {
        try {
            //InstructionInfo info = inst.getInstructionInfo();
            int construct = inst.getInstructionNameCode();
            if (construct < 0) {
                return "";
            }
            if (construct < 1024 &&
                    construct != StandardNames.XSL_FUNCTION &&
                    construct != StandardNames.XSL_TEMPLATE) {
                // it's a standard name
                if (inst.getPackageData().getHostLanguage() == Configuration.XSLT) {
                    return StandardNames.getDisplayName(construct);
                } else {
                    String s = StandardNames.getDisplayName(construct);
                    int colon = s.indexOf(':');
                    if (colon > 0) {
                        String local = s.substring(colon + 1);
                        if (local.equals("document")) {
                            return "document node constructor";
                        } else if (local.equals("text") || s.equals("value-of")) {
                            return "text node constructor";
                        } else if (local.equals("element")) {
                            return "computed element constructor";
                        } else if (local.equals("attribute")) {
                            return "computed attribute constructor";
                        } else if (local.equals("variable")) {
                            return "variable declaration";
                        } else if (local.equals("param")) {
                            return "external variable declaration";
                        } else if (local.equals("comment")) {
                            return "comment constructor";
                        } else if (local.equals("processing-instruction")) {
                            return "processing-instruction constructor";
                        } else if (local.equals("namespace")) {
                            return "namespace node constructor";
                        }
                    }
                    return s;
                }
            }
            switch (construct) {
                case LocationKind.LITERAL_RESULT_ELEMENT: {
                    StructuredQName qName = inst.getObjectName();
                    return "element constructor <" + qName.getDisplayName() + '>';
                }
                case LocationKind.LITERAL_RESULT_ATTRIBUTE: {
                    StructuredQName qName = inst.getObjectName();
                    return "attribute constructor " + qName.getDisplayName() + "=\"{...}\"";
                }

                default:
                    return "";
            }

        } catch (Exception err) {
            return "";
        }
    }

    /**
     * Wordwrap an error message into lines of 72 characters or less (if possible)
     *
     * @param message the message to be word-wrapped
     * @return the message after applying word-wrapping
     */

    public static String wordWrap(String message) {
        if (message.length() > 1000) {
            message = message.substring(0, 1000);
        }
        int nl = message.indexOf('\n');
        if (nl < 0) {
            nl = message.length();
        }
        if (nl > 100) {
            int i = 90;
            while (message.charAt(i) != ' ' && i > 0) {
                i--;
            }
            if (i > 10) {
                return message.substring(0, i) + "\n  " + wordWrap(message.substring(i + 1));
            } else {
                return message;
            }
        } else if (nl < message.length()) {
            return message.substring(0, nl) + '\n' + wordWrap(message.substring(nl + 1));
        } else {
            return message;
        }
    }

    /**
     * Print a stack trace to a specified output destination
     *
     * @param context the XPath dynamic execution context (which holds the head of a linked
     *                list of context objects, representing the execution stack)
     * @param out     the print stream to which the stack trace will be output
     * @param level   the level of detail: 0=none, 1=name and location of function/template,
     *                2=values of variables
     */

    public static void printStackTrace(XPathContext context, Logger out, int level) {
        if (level > 0) {
            int depth = 20;
            while (depth-- > 0) {
                if (level > 0) {
                    Component component = context.getCurrentComponent();
                    if (component != null) {
                        if (component.getActor() instanceof Mode) {
                            Rule rule = context.getCurrentTemplateRule();
                            if (rule != null) {
                                StringBuilder sb = new StringBuilder();
                                Location loc = rule.getPattern().getLocation();
                                sb.append("  In template rule with match=\"")
                                        .append(rule.getPattern().toShortString())
                                        .append("\" ");
                                if (loc != null && loc.getLineNumber() != -1) {
                                    sb.append("on line ").append(loc.getLineNumber()).append(" ");
                                }
                                if (loc != null && loc.getSystemId() != null) {
                                    sb.append("of ").append(abbreviatePath(loc.getSystemId()));
                                }
                                out.error(sb.toString());
                            }
                        } else {
                            out.error(getLocationMessageText(component.getActor()).replace("$at ","In "));
                        }
                    }
                    context.getStackFrame().getStackFrameMap().showStackFrame(context, out);
                }
                while (!(context instanceof XPathContextMajor)) {
                    context = context.getCaller();
                }

                ContextOriginator originator = ((XPathContextMajor) context).getOrigin();
                if (originator instanceof Controller) {
                    return;
                } else {
                    out.error("     invoked by " + showOriginator(originator));
                }
                context = context.getCaller();
                if (context == null) {
                    return;
                }
            }
        }
    }

    private static String showOriginator(ContextOriginator originator) {
        StringBuilder sb = new StringBuilder();
        if (originator == null) {
            sb.append("unknown caller (null)");
        } else if (originator instanceof ApplyTemplates) {
            sb.append("xsl:apply-templates");
        } else if (originator instanceof NextMatch) {
            sb.append("xsl:next-match");
        } else if (originator instanceof ApplyImports) {
            sb.append("xsl:apply-imports");
        } else if (originator instanceof UserFunctionCall) {
            sb.append("function call");
        } else if (originator instanceof CallTemplate) {
            sb.append("xsl:call-template");
        } else if (originator instanceof Controller) {
            sb.append("external application");
        } else if (originator instanceof BuiltInRuleSet) {
            sb.append("built-in template rule (" + ((BuiltInRuleSet) originator).getName() + ")");
        } else if (originator instanceof IterateInstr) {
            sb.append("xsl:iterate");
        } else if (originator instanceof ForEachGroup) {
            sb.append("xsl:for-each-group");
        } else if (originator instanceof KeyDefinition) {
            sb.append("xsl:key definition");
        } else if (originator instanceof GlobalParam) {
            sb.append("global xsl:param");
        } else if (originator instanceof GlobalVariable) {
            sb.append("global xsl:variable");
        } else {
            sb.append("unknown caller (" + originator.getClass() + ")");
        }
        if (originator instanceof Locatable) {
            Location loc = ((Locatable)originator).getLocation();
            if (loc.getLineNumber() != -1) {
                sb.append(" at ").append(loc.getSystemId() == null ? "line " : (loc.getSystemId() + "#"));
                sb.append(loc.getLineNumber());
            }
        }
        return sb.toString();
    }

    /**
     * Expand any special characters appearing in a message. Special characters
     * will be output as themselves, followed by a hex codepoint in the form [xHHHHH].
     * Characters are considered special if they have a codepoint above the value set
     * using {@link #setMaxOrdinaryCharacter(int)}
     *
     * @param in        the message to be expanded
     */

    public CharSequence expandSpecialCharacters(CharSequence in) {
        if (logger.isUnicodeAware()) {
            return in;
        } else {
            return expandSpecialCharacters(in, maxOrdinaryCharacter);
        }
    }

    /**
     * Expand any special characters appearing in a message. Special characters
     * will be output as themselves, followed by a hex codepoint in the form [xHHHHH].
     * @param in the message to be expanded
     * @param threshold the codepoint above which characters are considered special.
     */

    public static CharSequence expandSpecialCharacters(CharSequence in, int threshold) {
        if (threshold >= UTF16CharacterSet.NONBMP_MAX) {
            return in;
        }
        int max = 0;
        boolean isAstral = false;
        for (int i=0; i<in.length(); i++) {
            char c = in.charAt(i);
            if (c > max) {
                max = c;
            }
            if (UTF16CharacterSet.isSurrogate(c)) {
                isAstral = true;
            }
        }
        if (max <= threshold && !isAstral) {
            return in;
        }
        UnicodeString str;
        if (max <= 255) {
            str = new LatinString(in);
        } else if (!isAstral) {
            str = new BMPString(in);
        } else {
            str = new GeneralUnicodeString(in);
        }
        FastStringBuffer fsb = new FastStringBuffer(str.uLength()*2);
        for (int i=0; i<str.uLength(); i++) {
            int ch = str.uCharAt(i);
            fsb.appendWideChar(ch);
            if (ch > threshold) {
                fsb.append("[x");
                fsb.append(Integer.toHexString(ch));
                fsb.append("]");
            }
        }
        return fsb;
    }

}

