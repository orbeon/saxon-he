package net.sf.saxon.event;

import net.sf.saxon.Configuration;
import net.sf.saxon.StandardErrorListener;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.om.TreeModel;
import net.sf.saxon.om.Validation;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.value.Whitespace;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * This class defines options for parsing a source document
 */

public class ParseOptions implements Serializable {

    private int schemaValidation = Validation.DEFAULT;
    private int dtdValidation = Validation.DEFAULT;
    private StructuredQName topLevelElement;
    private SchemaType topLevelType;
    private transient XMLReader parser = null;
    private Boolean wrapDocument = null;
    private TreeModel treeModel = null;
    private int stripSpace = Whitespace.UNSPECIFIED;
    private Boolean lineNumbering = null;
    private Boolean xIncludeAware = null;
    private boolean pleaseClose = false;
    private transient ErrorListener errorListener = new StandardErrorListener();
    private transient EntityResolver entityResolver = null;
    private List filters = null;
    private boolean sourceIsXQJ = false;
    private boolean continueAfterValidationErrors = false;
    private boolean expandAttributeDefaults = true;
    private boolean useXsiSchemaLocation = true;

    /**
     * Create a ParseOptions object with default options set
     */

    public ParseOptions() {}

    /**
     * Create a ParseOptions object as a copy of another ParseOptions
     */

    public ParseOptions(ParseOptions p) {
        schemaValidation = p.schemaValidation;
        dtdValidation = p.dtdValidation;
        topLevelElement = p.topLevelElement;
        topLevelType = p.topLevelType;
        parser = p.parser;
        wrapDocument = p.wrapDocument;
        treeModel = p.treeModel;
        stripSpace = p.stripSpace;
        lineNumbering = p.lineNumbering;
        xIncludeAware = p.xIncludeAware;
        pleaseClose = p.pleaseClose;
        errorListener = p.errorListener;
        entityResolver = p.entityResolver;
        if (p.filters != null) {
            filters = new ArrayList(p.filters);
        }
        sourceIsXQJ = p.sourceIsXQJ;
        expandAttributeDefaults = p.expandAttributeDefaults;
        useXsiSchemaLocation = p.useXsiSchemaLocation;
        continueAfterValidationErrors = p.continueAfterValidationErrors;
    }

    /**
     * Merge another set of parseOptions into these parseOptions
     * @param options the other parseOptions. If both are present,
     * the other parseOptions take precedence
     */

    public void merge(ParseOptions options) {
        if (options.dtdValidation != Validation.DEFAULT) {
            dtdValidation = options.dtdValidation;
        }
        if (options.schemaValidation != Validation.DEFAULT) {
            schemaValidation = options.schemaValidation;
        }
        if (options.topLevelElement != null) {
            topLevelElement = options.topLevelElement;
        }
        if (options.topLevelType != null) {
            topLevelType = options.topLevelType;
        }
        if (options.parser != null) {
            parser = options.parser;
        }
        if (options.wrapDocument != null) {
            wrapDocument = options.wrapDocument;
        }
        if (options.treeModel != null) {
            treeModel = options.treeModel;
        }
        if (options.stripSpace != Whitespace.UNSPECIFIED) {
            stripSpace = options.stripSpace;
        }
        if (options.lineNumbering != null) {
            lineNumbering = options.lineNumbering;
        }
        if (options.xIncludeAware != null) {
            xIncludeAware = options.xIncludeAware;
        }
        if (options.pleaseClose) {
            pleaseClose = true;
        }
        if (options.errorListener != null) {
            errorListener = options.errorListener;
        }
        if (options.entityResolver != null) {
            entityResolver = options.entityResolver;
        }
        if (options.filters != null) {
            if (filters == null) {
                filters = new ArrayList();
            }
            filters.addAll(options.filters);
        }
        if (options.sourceIsXQJ) {
            sourceIsXQJ = true;
        }
        if (!options.expandAttributeDefaults) {
            // expand defaults unless the other options says don't
            expandAttributeDefaults = false;
        }
        if (!options.useXsiSchemaLocation) {
            // expand defaults unless the other options says don't
            useXsiSchemaLocation = false;
        }
    }

    /**
     * Merge settings from the Configuration object into these parseOptions
     * @param config the Configuration. Settings from the Configuration are
     * used only where no setting is present in this ParseOptions object
     */

    public void applyDefaults(Configuration config) {
        if (dtdValidation == Validation.DEFAULT) {
            dtdValidation = config.isValidation() ? Validation.STRICT : Validation.SKIP;
        }        
        if (schemaValidation == Validation.DEFAULT) {
            schemaValidation = config.getSchemaValidationMode();
        }
        if (treeModel == null) {
            treeModel = TreeModel.getTreeModel(config.getTreeModel());
        }
        if (stripSpace == Whitespace.UNSPECIFIED) {
            stripSpace = config.getStripsWhiteSpace();
        }
        if (lineNumbering == null) {
            lineNumbering = config.isLineNumbering();
        }
        if (xIncludeAware == null) {
            xIncludeAware = config.isXIncludeAware();
        }
        if (errorListener == null) {
            errorListener = config.getErrorListener();
        }

    }


    /**
     * Add a filter to the list of filters to be applied to the raw input
     * @param filter the filter to be added
     */

    public void addFilter(ProxyReceiver filter) {
        if (filters == null) {
            filters = new ArrayList(5);
        }
        filters.add(filter);
    }

    /**
     * Get the list of filters to be applied to the input. Returns null if there are no filters.
     * @return the list of filters, if there are any
     */

    public List getFilters() {
        return filters;
    }

    /**
     * Set the space-stripping action to be applied to the source document
     * @param stripAction one of {@link net.sf.saxon.value.Whitespace#IGNORABLE},
     * {@link net.sf.saxon.value.Whitespace#ALL}, or {@link net.sf.saxon.value.Whitespace#NONE}
     */

    public void setStripSpace(int stripAction) {
        stripSpace = stripAction;
    }

    /**
     * Get the space-stripping action to be applied to the source document
     * @return one of {@link net.sf.saxon.value.Whitespace#IGNORABLE},
     * {@link net.sf.saxon.value.Whitespace#ALL}, or {@link net.sf.saxon.value.Whitespace#NONE}
     */

    public int getStripSpace() {
        return (stripSpace == Whitespace.UNSPECIFIED ? Whitespace.IGNORABLE : stripSpace);
    }

    /**
     * Set the tree model to use. Default is the tiny tree
     * @param model one of {@link net.sf.saxon.event.Builder#TINY_TREE},
     * {@link net.sf.saxon.event.Builder#LINKED_TREE} or {@link net.sf.saxon.event.Builder#TINY_TREE_CONDENSED}
     */

    public void setTreeModel(int model) {
        treeModel = TreeModel.getTreeModel(model);
    }

    /**
     * Get the tree model that will be used.
     * @return one of {@link net.sf.saxon.event.Builder#TINY_TREE}, {@link net.sf.saxon.event.Builder#LINKED_TREE},
     * or {@link net.sf.saxon.event.Builder#TINY_TREE_CONDENSED},
     * or {link Builder#UNSPECIFIED_TREE_MODEL} if no call on setTreeModel() has been made
     */

    public int getTreeModel() {
        if (treeModel == null) {
            return Builder.UNSPECIFIED_TREE_MODEL;
        }
        return treeModel.getSymbolicValue();
    }

    /**
     * Set the tree model to use. Default is the tiny tree
     * @param model typically one of the constants {@link net.sf.saxon.om.TreeModel#TINY_TREE},
     * {@link TreeModel#TINY_TREE_CONDENSED}, or {@link TreeModel#LINKED_TREE}. However, in principle
     * a user-defined tree model can be used.
     * @since 9.2
     */

    public void setModel(TreeModel model) {
        treeModel = model;
    }

    /**
     * Get the tree model that will be used.
     * @return typically one of the constants {@link net.sf.saxon.om.TreeModel#TINY_TREE},
     * {@link TreeModel#TINY_TREE_CONDENSED}, or {@link TreeModel#LINKED_TREE}. However, in principle
     * a user-defined tree model can be used.
     */

    public TreeModel getModel() {
        return treeModel==null ? TreeModel.TINY_TREE : treeModel;
    }



    /**
     * Set whether or not schema validation of this source is required
     * @param option one of {@link net.sf.saxon.om.Validation#STRICT},
     * {@link net.sf.saxon.om.Validation#LAX}, {@link net.sf.saxon.om.Validation#STRIP},
     * {@link net.sf.saxon.om.Validation#PRESERVE}, {@link net.sf.saxon.om.Validation#DEFAULT}
     */

    public void setSchemaValidationMode(int option) {
        schemaValidation = option;
    }

    /**
     * Get whether or not schema validation of this source is required
     * @return the validation mode requested, or {@link net.sf.saxon.om.Validation#DEFAULT}
     * to use the default validation mode from the Configuration.

     */

    public int getSchemaValidationMode() {
        return schemaValidation;
    }

    /**
     * Set whether to expand default attributes defined in a DTD or schema.
     * By default, default attribute values are expanded
     * @param expand true if missing attribute values are to take the default value
     * supplied in a DTD or schema, false if they are to be left as absent
     */

    public void setExpandAttributeDefaults(boolean expand) {
        this.expandAttributeDefaults = expand;
    }

    /**
     * Ask whether to expand default attributes defined in a DTD or schema.
     * By default, default attribute values are expanded
     * @return true if missing attribute values are to take the default value
     * supplied in a DTD or schema, false if they are to be left as absent
     */

    public boolean isExpandAttributeDefaults() {
        return expandAttributeDefaults;
    }

    /**
     * Set the name of the top-level element for validation.
     * If a top-level element is set then the document
     * being validated must have this as its outermost element
     * @param elementName the QName of the required top-level element, or null to unset the value
     */

    public void setTopLevelElement(StructuredQName elementName) {
        topLevelElement = elementName;
    }

    /**
     * Get the name of the top-level element for validation.
     * If a top-level element is set then the document
     * being validated must have this as its outermost element
     * @return the QName of the required top-level element, or null if no value is set
     * @since 9.0
     */

    public StructuredQName getTopLevelElement() {
        return topLevelElement;
    }

    /**
     * Set the type of the top-level element for validation.
     * If this is set then the document element is validated against this type
     * @param type the schema type required for the document element, or null to unset the value
     */

    public void setTopLevelType(SchemaType type) {
        topLevelType = type;
    }

    /**
     * Get the type of the document element for validation.
     * If this is set then the document element of the document
     * being validated must have this type
     * @return the type of the required top-level element, or null if no value is set
     */

    public SchemaType getTopLevelType() {
        return topLevelType;
    }

    /**
     * Set whether or not to use the xsi:schemaLocation and xsi:noNamespaceSchemaLocation attributes
     * in an instance document to locate a schema for validation. Note, these attribute are only used
     * if validation is requested.
     * @param use true if these attributes are to be used, false if they are to be ignored
     */

    public void setUseXsiSchemaLocation(boolean use) {
        useXsiSchemaLocation = use;
    }

    /**
     * Ask whether or not to use the xsi:schemaLocation and xsi:noNamespaceSchemaLocation attributes
     * in an instance document to locate a schema for validation. Note, these attribute are only used
     * if validation is requested.
     * return true (the default) if these attributes are to be used, false if they are to be ignored
     */

    public boolean isUseXsiSchemaLocation() {
        return useXsiSchemaLocation;
    }

    /**
      * Set whether or not DTD validation of this source is required
      * @param option one of {@link net.sf.saxon.om.Validation#STRICT},
      * {@link net.sf.saxon.om.Validation#STRIP}, {@link net.sf.saxon.om.Validation#DEFAULT}
      */

     public void setDTDValidationMode(int option) {
         dtdValidation = option;
     }

     /**
      * Get whether or not DTD validation of this source is required
      * @return the validation mode requested, or {@link net.sf.saxon.om.Validation#DEFAULT}
      * to use the default validation mode from the Configuration.
      */

     public int getDTDValidationMode() {
         return dtdValidation;
     }


    /**
     * Set whether line numbers are to be maintained in the constructed document
     * @param lineNumbering true if line numbers are to be maintained
     */

    public void setLineNumbering(boolean lineNumbering) {
        this.lineNumbering = Boolean.valueOf(lineNumbering);
    }

    /**
     * Get whether line numbers are to be maintained in the constructed document
     * @return true if line numbers are maintained
     */

    public boolean isLineNumbering() {
        return lineNumbering != null && lineNumbering.booleanValue();
    }

    /**
     * Determine whether setLineNumbering() has been called
     * @return true if setLineNumbering() has been called
     */

    public boolean isLineNumberingSet()  {
        return lineNumbering != null;
    }

    /**
     * Set the SAX parser (XMLReader) to be used
     * @param parser the SAX parser
     */

    public void setXMLReader(XMLReader parser) {
        this.parser = parser;
    }

    /**
     * Get the SAX parser (XMLReader) to be used
     * @return the parser
     */

    public XMLReader getXMLReader() {
        return parser;
    }

    /**
     * Set an EntityResolver to be used when parsing. Note that this will not be used if an XMLReader
     * has been supplied (in that case, the XMLReader should be initialized with the EntityResolver
     * already set.)
     * @param resolver the EntityResolver to be used
     */

    public void setEntityResolver(EntityResolver resolver) {
        entityResolver = resolver;
    }

    /**
     * Get the EntityResolver that will be used when parsing
     * @return the EntityResolver, if one has been set using {@link #setEntityResolver},
     * otherwise null.
     */

    public EntityResolver getEntityResolver() {
        return entityResolver;
    }


    /**
     * Assuming that the contained Source is a node in a tree, indicate whether a tree should be created
     * as a view of this supplied tree, or as a copy.
     * @param wrap if true, the node in the supplied Source is wrapped, to create a view. If false, the node
     * and its contained subtree is copied. If null, the system default is chosen.
     */

    public void setWrapDocument(Boolean wrap) {
        wrapDocument = wrap;
    }

    /**
       Assuming that the contained Source is a node in a tree, determine whether a tree will be created
     * as a view of this supplied tree, or as a copy.
     * @return if true, the node in the supplied Source is wrapped, to create a view. If false, the node
     * and its contained subtree is copied. If null, the system default is chosen.
     * @since 8.8
     */

    public Boolean getWrapDocument() {
        return wrapDocument;
    }

    /**
     * <p>Set state of XInclude processing.</p>
     * <p/>
     * <p>If XInclude markup is found in the document instance, should it be
     * processed as specified in <a href="http://www.w3.org/TR/xinclude/">
     * XML Inclusions (XInclude) Version 1.0</a>.</p>
     * <p/>
     * <p>XInclude processing defaults to <code>false</code>.</p>
     *
     * @param state Set XInclude processing to <code>true</code> or
     *              <code>false</code>
     * @since 8.9
     */
    public void setXIncludeAware(boolean state) {
        xIncludeAware = Boolean.valueOf(state);
    }

    /**
     * <p>Determine whether setXIncludeAware() has been called.</p>
     *
     * @return true if setXIncludeAware() has been called
     */

    public boolean isXIncludeAwareSet() {
        return (xIncludeAware != null);
    }

    /**
     * <p>Get state of XInclude processing.</p>
     *
     * @return current state of XInclude processing. Default value is false.
     */

    public boolean isXIncludeAware() {
        return xIncludeAware != null && xIncludeAware.booleanValue();
    }

    /**
     * Set an ErrorListener to be used when parsing
     * @param listener the ErrorListener to be used
     */

    public void setErrorListener(ErrorListener listener) {
        errorListener = listener;
    }

    /**
     * Get the ErrorListener that will be used when parsing
     * @return the ErrorListener, if one has been set using {@link #setErrorListener},
     * otherwise null.
     */

    public ErrorListener getErrorListener() {
        return errorListener;
    }

    /**
     * Say that processing should continue after a validation error (true when the output is a final
     * output)
     * @param keepGoing true if processing should continue
     */

    public void setContinueAfterValidationErrors(boolean keepGoing) {
        continueAfterValidationErrors = keepGoing;
    }

    /**
     * Ask whether processing should continue after a validation error (true when the output is a final
     * output)
     * @return true if processing should continue
     */

    public boolean isContinueAfterValidationErrors() {
        return continueAfterValidationErrors;
    }

    /**
     * Set whether or not the user of this Source is encouraged to close it as soon as reading is finished.
     * Normally the expectation is that any Stream in a StreamSource will be closed by the component that
     * created the Stream. However, in the case of a Source returned by a URIResolver, there is no suitable
     * interface (the URIResolver has no opportunity to close the stream). Also, in some cases such as reading
     * of stylesheet modules, it is possible to close the stream long before control is returned to the caller
     * who supplied it. This tends to make a difference on .NET, where a file often can't be opened if there
     * is a stream attached to it.
     * @param close true if the source should be closed as soon as it has been consumed
     */

    public void setPleaseCloseAfterUse(boolean close) {
        pleaseClose = close;
    }

    /**
     * Determine whether or not the user of this Source is encouraged to close it as soon as reading is
     * finished.
     * @return true if the source should be closed as soon as it has been consumed
     */

    public boolean isPleaseCloseAfterUse() {
        return pleaseClose;
    }

    /**
     * Close any resources held by a given Source. This only works if the underlying Source is one that is
     * recognized as holding closable resources.
     * @param source the source to be closed
     * @since 9.2
     */

    public static void close(Source source) {
        try {
            if (source instanceof StreamSource) {
                StreamSource ss = (StreamSource)source;
                if (ss.getInputStream() != null) {
                    ss.getInputStream().close();
                }
                if (ss.getReader() != null) {
                    ss.getReader().close();
                }
            } else if (source instanceof SAXSource) {
                InputSource is = ((SAXSource)source).getInputSource();
                if (is != null) {
                    if (is.getByteStream() != null) {
                        is.getByteStream().close();
                    }
                    if (is.getCharacterStream() != null) {
                        is.getCharacterStream().close();
                    }
                }
            }
        } catch (IOException err) {
            // no action
        }
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//