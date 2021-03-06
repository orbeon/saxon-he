package net.sf.saxon.expr.sort;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.flwor.ExpressionProcessor;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.StringConverter;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.type.ValidationFailure;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Whitespace;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * A SortKeyDefinition defines one component of a sort key. <BR>
 *
 * Note that most attributes defining the sort key can be attribute value templates,
 * and can therefore vary from one invocation to another. We hold them as expressions. As
 * soon as they are all known (which in general is only at run-time), the SortKeyDefinition
 * is replaced by a FixedSortKeyDefinition in which all these values are fixed.
 */

// TODO: optimise also for the case where the attributes depend only on global variables
// or parameters, in which case the same AtomicComparer can be used for the duration of a
// transformation.

// TODO: at present the SortKeyDefinition is evaluated to obtain a AtomicComparer, which can
// be used to compare two sort keys. It would be more efficient to use a Collator to
// obtain collation keys for all the items to be sorted, as these can be compared more
// efficiently.


public class SortKeyDefinition implements Serializable {

	private static StringLiteral defaultOrder = new StringLiteral("ascending");
	private static StringLiteral defaultCaseOrder = new StringLiteral("#default");
	private static StringLiteral defaultLanguage = new StringLiteral(StringValue.EMPTY_STRING);

	/*@Nullable*/ protected Expression sortKey;
	protected Expression order = defaultOrder;
	protected Expression dataTypeExpression = null;
	// used when the type is not known till run-time
	protected Expression caseOrder = defaultCaseOrder;
	protected Expression language = defaultLanguage;
	protected Expression collationName = null;
	protected Expression stable = null; // not actually used, but present so it can be validated
	protected StringCollator collation;
	protected String baseURI;           // needed in case collation URI is relative
	protected boolean emptyLeast = true;
	protected boolean backwardsCompatible = false;
    protected boolean setContextForSortKey = false;

	private transient AtomicComparer finalComparator = null;
	// Note, the "collation" defines the collating sequence for the sort key. The
	// "finalComparator" is what is actually used to do comparisons, after taking into account
	// ascending/descending, caseOrder, etc.

	// The comparer is transient because a RuleBasedCollator is not serializable. This means that
	// when a stylesheet is compiled, the finalComparator is discarded, which means a new finalComparator will be
	// constructed for each sort at run-time.

	/**
	 * Set the expression used as the sort key
     * @param exp the sort key select expression
     * @param setContext set to true if the sort key is to be evaluated with the
     * item-being-sorted as the context item (as in XSLT); false if the context item
     * is not to be set (as in XQuery)
     */

	public void setSortKey(Expression exp, boolean setContext) {
		sortKey = exp;
        setContextForSortKey = setContext;
	}

	/**
	 * Get the expression used as the sort key
	 * @return the sort key select expression
	 */

	public Expression getSortKey() {
		return sortKey;
	}

    /**
     * Ask whether the sortkey is to be evaluated with the item-being-sorted
     * as the context item
     * @return true if the context needs to be set for evaluating the sort key
     */

    public boolean isSetContextForSortKey() {
        return setContextForSortKey;
    }


	/**
	 * Set the order. This is supplied as an expression which must evaluate to "ascending"
	 * or "descending". If the order is fixed, supply e.g. new StringValue("ascending").
	 * Default is "ascending".
	 * @param exp the expression that determines the order (always a literal in XQuery, but
	 * can be defined by an AVT in XSLT)
	 */

	public void setOrder(Expression exp) {
		order = exp;
	}

	/**
	 * Get the expression that defines the order as ascending or descending
	 * @return the expression that determines the order (always a literal in XQuery, but
	 * can be defined by an AVT in XSLT)
	 */

	public Expression getOrder() {
		return order;
	}

	/**
	 * Set the data type. This is supplied as an expression which must evaluate to "text",
	 * "number", or a QName. If the data type is fixed, the valus should be supplied using
	 * setDataType() and not via this method.
	 * @param exp the expression that defines the data type, as used in XSLT 1.0
	 */

	public void setDataTypeExpression(Expression exp) {
		dataTypeExpression = exp;
	}

	/**
	 * Get the expression that defines the data type of the sort keys
	 * @return the expression that defines the data type, as used in XSLT 1.0
	 */

	public Expression getDataTypeExpression() {
		return dataTypeExpression;
	}

	/**
	 * Set the case order. This is supplied as an expression which must evaluate to "upper-first"
	 * or "lower-first" or "#default". If the order is fixed, supply e.g. new StringValue("lower-first").
	 * Default is "#default".
	 * @param exp the expression that defines the case order
	 */

	public void setCaseOrder(Expression exp) {
		caseOrder = exp;
	}

	/**
	 * Get the expression that defines the case order of the sort keys.
	 * @return the expression that defines the case order, whose run-time value will be "upper-first",
	 * "lower-first", or "#default".
	 */

	public Expression getCaseOrder() {
		return caseOrder;
	}

	/**
	 * Set the language. This is supplied as an expression which evaluates to the language name.
	 * If the order is fixed, supply e.g. new StringValue("de").
	 * @param exp the expression that determines the language
	 */

	public void setLanguage(Expression exp) {
		language = exp;
	}

	/**
	 * Get the expression that defines the language of the sort keys
	 * @return exp the expression that determines the language
	 */

	public Expression getLanguage() {
		return language;
	}

	/**
	 * Set the collation name (specifically, an expression which when evaluated returns the collation URI).
	 * @param collationName the expression that determines the collation name
	 */

	public void setCollationNameExpression(Expression collationName) {
		this.collationName = collationName;
	}

	/**
	 * Get the selected collation name
	 * (specifically, an expression which when evaluated returns the collation URI).
	 * @return the expression that determines the collation name
	 */

	public Expression getCollationNameExpression() {
		return collationName;
	}

	/**
	 * Set the collation to be used
	 * @param collation A StringCollator, which encapsulates both the collation URI and the collating function
	 */

	public void setCollation(StringCollator collation) {
		this.collation = collation;
	}

	/**
	 * Get the collation to be used
	 * @return A StringCollator, which encapsulates both the collation URI and the collating function
	 */

	public StringCollator getCollation() {
		return collation;
	}

	/**
	 * Set the base URI of the expression. This is needed to handle the case where a collation URI
	 * evaluated at run-time turns out to be a relative URI.
	 * @param baseURI the static base URI of the expression
	 */

	public void setBaseURI(String baseURI) {
		this.baseURI = baseURI;
	}

	/**
	 * Get the static base URI of the expression. This is needed to handle the case where a collation URI
	 * evaluated at run-time turns out to be a relative URI.
	 * @return the static base URI of the expression
	 */

	public String getBaseURI() {
		return baseURI;
	}

	/**
	 * Set whether this sort key definition is stable
	 * @param stable the expression that determines whether the sort key definition is stable
	 * (it evaluates to the string "yes" or "no".
	 */

	public void setStable(Expression stable) {
		this.stable = stable;
	}

	/**
	 * Ask whether this sort key definition is stable
	 * @return the expression that determines whether the sort key definition is stable
	 * (it evaluates to the string "yes" or "no".
	 */

	public Expression getStable() {
		return stable;
	}

	/**
	 * Set whether this sort key is evaluated in XSLT 1.0 backwards compatibility mode
	 * @param compatible true if backwards compatibility mode is selected
	 */

	public void setBackwardsCompatible(boolean compatible) {
		backwardsCompatible = compatible;
	}

	/**
	 * Ask whether this sort key is evaluated in XSLT 1.0 backwards compatibility mode
	 * @return true if backwards compatibility mode was selected
	 */

	public boolean isBackwardsCompatible() {
		return backwardsCompatible;
	}

	/**
	 * Set whether empty sequence comes before other values or after them
	 * @param emptyLeast true if () is considered lower than any other value
	 */

	public void setEmptyLeast(boolean emptyLeast) {
		this.emptyLeast = emptyLeast;
	}

	/**
	 * Ask whether empty sequence comes before other values or after them
	 * @return true if () is considered lower than any other value
	 */

	public boolean getEmptyLeast() {
		return emptyLeast;
	}

	/**
	 * Ask whether the sort key definition is fixed, that is, whether all the information needed
	 * to create a Comparator is known statically
	 * @return true if all information needed to create a Comparator is known statically
	 */

	public boolean isFixed() {
		return (order instanceof Literal &&
				(dataTypeExpression == null ||
						dataTypeExpression instanceof Literal) &&
						caseOrder instanceof Literal &&
						language instanceof Literal &&
						(stable == null || stable instanceof Literal) &&
						(collationName == null || collationName instanceof Literal));
	}



	/**
	 * Simplify this sort key definition
	 * @param visitor the expression visitor
	 * @return the simplified sort key definition
	 * @throws XPathException if any failure occurs
	 */

	public SortKeyDefinition simplify(ExpressionVisitor visitor) throws XPathException {
		sortKey = visitor.simplify(sortKey);
		order = visitor.simplify(order);
		dataTypeExpression = visitor.simplify(dataTypeExpression);
		caseOrder = visitor.simplify(caseOrder);
		language = visitor.simplify(language);
		stable = visitor.simplify(stable);
		collationName = visitor.simplify(collationName);
		return this;
	}

	/**
	 * Copy this SortKeyDefinition
	 */

	public SortKeyDefinition copy() {
		SortKeyDefinition sk2 = new SortKeyDefinition();
		sk2.setSortKey(copy(sortKey), true);
		sk2.setOrder(copy(order));
		sk2.setDataTypeExpression(copy(dataTypeExpression));
		sk2.setCaseOrder(copy(caseOrder));
		sk2.setLanguage(copy(language));
		sk2.setStable(copy(stable));
		sk2.setCollationNameExpression(copy(collationName));
		sk2.collation = collation;
		sk2.emptyLeast = emptyLeast;
		sk2.baseURI = baseURI;
		sk2.backwardsCompatible = backwardsCompatible;
		sk2.finalComparator = finalComparator;
        sk2.setContextForSortKey = setContextForSortKey;
		return sk2;
	}

	private Expression copy(Expression in) {
		return (in == null ? null : in.copy());
	}

	/**
	 * Type-check this sort key definition (all properties other than the sort key
	 * select expression, when it has a different dynamic context)
	 * @param visitor the expression visitor
	 * @param contextItemType the type of the context item
	 * @throws XPathException if any failure occurs
	 */

	public void typeCheck(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
		order = visitor.typeCheck(order, contextItemType);
		dataTypeExpression = visitor.typeCheck(dataTypeExpression, contextItemType);
		caseOrder = visitor.typeCheck(caseOrder, contextItemType);
		language = visitor.typeCheck(language, contextItemType);
		stable = visitor.typeCheck(stable, contextItemType);
		collationName = visitor.typeCheck(collationName, contextItemType);
        if (!setContextForSortKey) {
            sortKey = visitor.typeCheck(sortKey, contextItemType);
        }

		if (language instanceof StringLiteral && ((StringLiteral)language).getStringValue().length() != 0) {
            ValidationFailure vf = StringConverter.STRING_TO_LANGUAGE.validate(((StringLiteral) language).getStringValue());
            if (vf != null) {
                throw new XPathException("The lang attribute of xsl:sort must be a valid language code", "XTDE0030");
            }
		}
	}

     /**
     * Process the subexpressions of this clause
     *
     * @param processor the expression processor used to process the subexpressions
     */

    public void processSubExpressions(ExpressionProcessor processor) throws XPathException {
		order = processor.processExpression(order);
        if (dataTypeExpression != null) {
            dataTypeExpression = processor.processExpression(dataTypeExpression);
        }
        if (caseOrder != null) {
            caseOrder = processor.processExpression(caseOrder);
        }
        if (language != null) {
            language = processor.processExpression(language);
        }
        if (stable != null) {
            stable = processor.processExpression(stable);
        }
        if (collationName != null) {
            collationName = processor.processExpression(collationName);
        }
        sortKey = processor.processExpression(sortKey);
    }

	/**
	 * Allocate an AtomicComparer to perform the comparisons described by this sort key component. This method
	 * is called at run-time. The AtomicComparer takes into account not only the collation, but also parameters
	 * such as order=descending and handling of empty sequence and NaN (the result of the compare()
	 * method of the comparator is +1 if the second item is to sort after the first item)
	 * @param context the dynamic evaluation context
	 * @return an AtomicComparer suitable for making the sort comparisons
	 */

	public AtomicComparer makeComparator(XPathContext context) throws XPathException {

		String orderX = order.evaluateAsString(context).toString();

		final Configuration config = context.getConfiguration();
		final TypeHierarchy th = config.getTypeHierarchy();

		AtomicComparer atomicComparer;
		StringCollator stringCollator;
		if (collation != null) {
			stringCollator = collation;
		} else if (collationName != null) {
			String cname = collationName.evaluateAsString(context).toString();
			URI collationURI;
			try {
				collationURI = new URI(cname);
				if (!collationURI.isAbsolute()) {
					if (baseURI == null) {
						throw new XPathException("Collation URI is relative, and base URI is unknown");
					} else {
						URI base = new URI(baseURI);
						collationURI = base.resolve(collationURI);
					}
				}
			} catch (URISyntaxException err) {
				throw new XPathException("Collation name " + cname + " is not a valid URI: " + err);
			}
			try {
				stringCollator = context.getCollation(collationURI.toString());
			} catch (XPathException e) {
				if ("FOCH0002".equals(e.getErrorCodeLocalPart())) {
					e.setErrorCode("XTDE1035");
				}
				throw e;
			}
		} else {
			String caseOrderX = caseOrder.evaluateAsString(context).toString();
			String languageX = language.evaluateAsString(context).toString();
			Properties props = new Properties();
			if (languageX.length() != 0) {
                ValidationFailure vf = StringConverter.STRING_TO_LANGUAGE.validate(languageX);
                if (vf != null) {
                    throw new XPathException("The lang attribute of xsl:sort must be a valid language code", "XTDE0030");
                }
				props.setProperty("lang", languageX);
			}
			if (!caseOrderX.equals("#default")) {
				props.setProperty("case-order", caseOrderX);
			}
			stringCollator = Configuration.getPlatform().makeCollation(config, props, "");
			// TODO: build a URI allowing the collation to be reconstructed
		}



		if (dataTypeExpression==null) {
			atomicComparer = AtomicSortComparer.makeSortComparer(stringCollator,
					sortKey.getItemType(th).getAtomizedItemType().getPrimitiveType(), context);
			if (!emptyLeast) {
				atomicComparer = new EmptyGreatestComparer(atomicComparer);
			}
		} else {
			String dataType = dataTypeExpression.evaluateAsString(context).toString();
			if (dataType.equals("text")) {
				atomicComparer = AtomicSortComparer.makeSortComparer(stringCollator,
						StandardNames.XS_STRING, context);
				atomicComparer = new TextComparer(atomicComparer);
			} else if (dataType.equals("number")) {
				if (context.getConfiguration().getXsdVersion() == Configuration.XSD10) {
					atomicComparer = NumericComparer.getInstance();
				} else {
					atomicComparer = NumericComparer11.getInstance();
				}
			} else {
				XPathException err = new XPathException("data-type on xsl:sort must be 'text' or 'number'");
				err.setErrorCode("XTDE0030");
				throw err;
			}
		}

		if (stable != null) {
			StringValue stableVal = (StringValue)stable.evaluateItem(context);
			String s = Whitespace.trim(stableVal.getStringValue());
			if (s.equals("yes") || s.equals("no")) {
				// no action
			} else {
				XPathException err = new XPathException("Value of 'stable' on xsl:sort must be 'yes' or 'no'");
				err.setErrorCode("XTDE0030");
				throw err;
			}
		}

		if (orderX.equals("ascending")) {
			return atomicComparer;
		} else if (orderX.equals("descending")) {
			return new DescendingComparer(atomicComparer);
		} else {
			XPathException err1 = new XPathException("order must be 'ascending' or 'descending'");
			err1.setErrorCode("XTDE0030");
			throw err1;
		}
	}

	/**
	 * Set the comparator which is used to compare two values according to this sort key. The comparator makes the final
	 * decision whether one value sorts before or after another: this takes into account the data type, the collation,
	 * whether empty comes first or last, whether the sort order is ascending or descending.
	 *
	 * <p>This method is called at compile time if all these factors are known at compile time.
	 * It must not be called at run-time, except to reconstitute a finalComparator that has been
	 * lost by virtue of serialization .</p>
	 * @param comp the Atomic Comparer to be used
	 */

	public void setFinalComparator(AtomicComparer comp) {
		finalComparator = comp;
	}

	/**
	 * Get the comparator which is used to compare two values according to this sort key. This method
	 * may be called either at compile time or at run-time. If no comparator has been allocated,
	 * it returns null. It is then necessary to allocate a comparator using the {@link #makeComparator}
	 * method.
	 * @return the Atomic Comparer to be used
	 */

	public AtomicComparer getFinalComparator() {
		return finalComparator;
	}
	
	public SortKeyDefinition fix(XPathContext context) throws XPathException{
		SortKeyDefinition newSKD = this.copy();
		
		newSKD.setLanguage(new StringLiteral(this.getLanguage().evaluateAsString(context)));
		newSKD.setOrder(new StringLiteral(this.getOrder().evaluateAsString(context)));
		
		if(collationName!=null){
			newSKD.setCollationNameExpression(new StringLiteral(this.getCollationNameExpression().evaluateAsString(context)));
		}
		
		newSKD.setCaseOrder(new StringLiteral(this.getCaseOrder().evaluateAsString(context)));
		
		if(dataTypeExpression!=null){
			newSKD.setDataTypeExpression(new StringLiteral(this.getDataTypeExpression().evaluateAsString(context)));
		}
		newSKD.setSortKey(new ContextItemExpression(), true);
		
		return newSKD;
	}
	
	 /**
     * Compare two SortKeyDefinition values for equality. This compares the sortKeys and attribute values.
     * @param other SortKeyDefinition
     * @return boolean
     */

    public boolean equals(Object other) {
        if (other instanceof SortKeyDefinition) {
        	SortKeyDefinition skd2 = (SortKeyDefinition)other;
            return sortKey.hashCode() == skd2.getSortKey().hashCode() && hashCode() == skd2.hashCode();
               
        } else {
            return false;
        }
    }

	/**
	 * Get a hashcode to reflect the equals() method
	 * @return a hashcode based sortkey attribute values.
	 */

	public int hashCode() {
		int h = 0;
		h ^= order.hashCode();
		h ^= caseOrder.hashCode();
		h ^= language.hashCode();
		
		if(dataTypeExpression!=null){
			h ^= dataTypeExpression.hashCode();
		}
		if(stable!=null){
			h ^= stable.hashCode();
		}
		if(collationName!=null){
			h ^= collationName.hashCode();

		}
		return h;
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
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//