////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.instruct.AnalyzeString;
import net.sf.saxon.expr.instruct.FixedElement;
import net.sf.saxon.expr.instruct.ProcessRegexMatchInstruction;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.lib.Validation;
import net.sf.saxon.om.*;
import net.sf.saxon.regex.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.StringValue;

/**
 * Implements the fn:analyze-string function defined in Functions and Operators 1.1. This is handled
 * by compiling code to generate the defined element structure, based on a call of the
 * xsl:analyze-string instruction
 */
public class AnalyzeStringFn extends CompileTimeFunction {

    // TODO: map the XSLT error codes (which aren't always being generated anyway)

    /*@NotNull*/
    public Expression simplify(ExpressionVisitor visitor) throws XPathException {

        Expression[] args = getArguments();

        Configuration config = visitor.getConfiguration();
        boolean schemaAware = visitor.getStaticContext().isSchemaAware();
        if (schemaAware) {
            config.addSchemaForBuiltInNamespace(NamespaceConstant.FN);
        }
        NamePool namePool = config.getNamePool();
        FixedElement onMatch = createInstr(namePool, Validation.PRESERVE, "fn", NamespaceConstant.FN, "match", new ProcessRegexMatchInstruction(namePool));
        FixedElement onNonMatch = createInstr(namePool, Validation.PRESERVE, "fn", NamespaceConstant.FN, "non-match", new ContextItemExpression());

        AnalyzeString inst = new AnalyzeString(
                args[0],
                args[1],
                args.length > 2 ? args[2] : new StringLiteral(StringValue.EMPTY_STRING, getContainer()),
                onMatch,
                onNonMatch,
                null);
        inst.setUseXsltErrorCodes(false);
        return createInstr(
                namePool,
                schemaAware ? Validation.STRICT : Validation.SKIP,
                "fn", NamespaceConstant.FN, "analyze-string-result", inst);
    }


    FixedElement createInstr(NamePool namePool, int validation, String prefix, String uri, String name, Expression content) {
        NamespaceBinding[] nscodes = NamespaceBinding.EMPTY_ARRAY;
        FingerprintedQName instrQN = new FingerprintedQName(prefix, uri, name);
        instrQN.allocateNameCode(namePool);

        FixedElement instr = new FixedElement(
                instrQN,
                nscodes,
                true,
                true, null,
                validation);
        instr.setContentExpression(content);
        return instr;
    }


    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as Sequences
     * @return the result of the evaluation, in the form of a Sequence
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public NodeInfo call(XPathContext context, Sequence[] arguments) throws XPathException {
        CharSequence input = arguments[0].head().getStringValueCS();
        CharSequence pattern = arguments[1].head().getStringValueCS();
        String flags = "";
        if (arguments.length == 3) {
            flags = arguments[2].head().getStringValue();
        }
        REFlags reFlags;
        try {
            reFlags = new REFlags(flags, "XP30");
        } catch (RESyntaxException err) {
            throw new XPathException(err.getMessage(), "FORX0001");
        }

        UnicodeString rawPattern = UnicodeString.makeUnicodeString(pattern);
        RECompiler comp2 = new RECompiler();
        comp2.setFlags(reFlags);
        REProgram regex = comp2.compile(rawPattern);

        RegexIterator iter = new ARegexIterator(UnicodeString.makeUnicodeString(input), rawPattern, new REMatcher(regex));

        boolean schemaAware = context.getController().getExecutable().isSchemaAware();
        if (schemaAware) {
            context.getConfiguration().addSchemaForBuiltInNamespace(NamespaceConstant.FN);
        }

        XPathContextMajor c2 = context.newContext();
        c2.setOrigin(this);
        c2.setCurrentIterator(new FocusTrackingIterator(iter));
        c2.setCurrentRegexIterator(iter);


        NamePool namePool = context.getConfiguration().getNamePool();
        FingerprintedQName resultQN = new FingerprintedQName("fn", NamespaceConstant.FN, "analyze-string-result");
        resultQN.allocateNameCode(namePool);

        FixedElement onMatch = createInstr(namePool, Validation.PRESERVE, "fn", NamespaceConstant.FN, "match", new ProcessRegexMatchInstruction(namePool));
        FixedElement onNonMatch = createInstr(namePool, Validation.PRESERVE, "fn", NamespaceConstant.FN, "non-match", new ProcessRegexMatchInstruction(namePool));

        AnalyzeMappingFunction fn = new AnalyzeMappingFunction(iter, c2, onNonMatch, onMatch);
        ContextMappingIterator<StringValue> itr = new ContextMappingIterator<StringValue>(fn, c2);
        GroundedValue extent = SequenceExtent.makeSequenceExtent(itr);
        int validation = schemaAware ? Validation.STRICT : Validation.SKIP;
        FixedElement result = createInstr(namePool, validation, "fn", NamespaceConstant.FN, "analyze-string-result", null);
        result.setContentExpression(Literal.makeLiteral(extent, getContainer()));
        return (NodeInfo)result.evaluateItem(context);
    }
}

// Copyright (c) 2011 Saxonica Limited. All rights reserved.



