using System;
using System.Collections.Generic;
using System.Text;

using JStaticContext = net.sf.saxon.expr.StaticContext;
using JXPathException = net.sf.saxon.trans.XPathException;
using JXPathContext = net.sf.saxon.expr.XPathContext;
using JExtensionFunctionDefinition = net.sf.saxon.functions.ExtensionFunctionDefinition;
using JExtensionFunctionCall = net.sf.saxon.functions.ExtensionFunctionCall;
using JStructuredQName = net.sf.saxon.om.StructuredQName;
using JSequenceIterator = net.sf.saxon.om.SequenceIterator;
using JSequenceType = net.sf.saxon.value.SequenceType;

namespace Saxon.Api
{

    /// <summary>
    /// The class <c>StaticContext</c> provides information about the static context of an expression
    /// </summary>

    public class StaticContext {

        private JStaticContext env;

        internal StaticContext(JStaticContext jsc) {
            env = jsc;
        }


        /// <summary>
        /// The URI of the module where an expression appears, suitable for use in diagnostics
        /// </summary>
        /// 
        public Uri ModuleUri {
            get {
                return new Uri(env.getSystemId());
            }
        }

        /// <summary>
        /// The line number within a module where an expression appears, suitable for use in diagnostics
        /// </summary>
        /// 
        public int LineNumber {
            get {
                return env.getLineNumber();
            }
        }


        /// <summary>
        /// The static base URI of the expression. Often the same as the URI of the containing module,
        /// but not necessarily so, for example in a stylesheet that uses external XML entities or the
        /// xml:base attribute
        /// </summary>
        /// 
        public Uri BaseUri {
            get {
                return new Uri(env.getBaseURI());
            }
        }

        /// <summary>
        /// Resolve an in-scope namespace prefix to obtain the corresponding namespace URI. If the prefix
        /// is a zero-length string, the default namespace for elements and types is returned.
        /// </summary>
        /// <param name="Prefix">The namespace prefix</param>
        /// <returns>The corresponding namespace URI if there is one, or null otherwise</returns>
        /// 
        public String GetNamespaceForPrefix(string Prefix) {
            if (Prefix == "") {
                return env.getDefaultElementNamespace();
            }
            try {
                return env.getURIForPrefix(Prefix);
            } catch (JXPathException) {
                return null;
            }
        }
    }

    /// <summary>
    /// The class <c>DynamicContext</c> provides information about the dynamic context of an expression
    /// </summary>
    /// 
    public class DynamicContext {

        internal JXPathContext context;

        internal DynamicContext(JXPathContext context) {
            this.context = context;
        }

        /// <summary>
        /// The context item. May be null if no context item is defined
        /// </summary>
        /// 
        public XdmItem ContextItem {
            get {
                return (XdmItem)XdmItem.Wrap(context.getContextItem());
            }
        }

        /// <summary>
        /// The context position (equivalent to the XPath position() function).
        /// </summary>
        /// <remarks>Calling this method throws an exception if the context item is undefined.</remarks>
        /// 
        public int ContextPosition {
            get {
                return context.getContextPosition();
            }
        }

        /// <summary>
        /// The context size (equivalent to the XPath last() function).
        /// </summary>
        /// <remarks>Calling this method throws an exception if the context item is undefined.</remarks>
        /// 
        public int ContextSize {
            get {
                return context.getLast();
            }
        }
        
    }

    /// <summary>
    /// <para>Abstract superclass for user-written extension functions. An extension function may be implemented as a subclass
    /// of this class, with appropriate implementations of the defined methods.</para>
    /// <para>More precisely, a subclass of <c>ExtensionFunctionDefinition</c> identifies a family of extension functions
    /// with the same (namespace-qualified) name but potentially having different arity (number of arguments).</para>
    /// </summary>
    /// 

    public abstract class ExtensionFunctionDefinition
    {
        /// <summary>
        /// Read-only property returning the name of the extension function, as a QName.
        /// </summary>

        public abstract QName FunctionName {get;}

        /// <summary>
        /// Read-only property giving the minimum number of arguments in a call to this extension function.
        /// </summary>

        public abstract int MinimumNumberOfArguments {get;}

        /// <summary>
        /// Read-only property giving the maximum number of arguments in a call to this extension function.
        /// </summary>

        public abstract int MaximumNumberOfArguments {get;}

        /// <summary>
        /// Read-only property giving the required types of the arguments to this extension function. 
        /// If the number of items in the array is less than the maximum number of arguments, 
        /// then the last entry in the returned ArgumentTypes is assumed to apply to all the rest; 
        /// if the returned array is empty, then all arguments are assumed to be of type <c>item()*</c>
        /// </summary>

        public abstract XdmSequenceType[] ArgumentTypes {get;}

        /// <summary>
        /// Method returning the declared type of the return value from the function. The type of the return
        /// value may be known more precisely if the types of the arguments are known (for example, some functions
        /// return a value that is the same type as the first argument. The method is therefore called supplying the
        /// static types of the actual arguments present in the call.
        /// </summary>
        /// <param name="ArgumentTypes">
        /// The static types of the arguments present in the function call
        /// </param>
        /// <returns>
        /// An <c>XdmSequenceType</c> representing the declared return type of the extension function
        /// </returns>

        public abstract XdmSequenceType ResultType(XdmSequenceType[] ArgumentTypes);

        /// <summary>
        /// This property may be set to true in a subclass if it guarantees that the returned result of the function
        /// will always be of the declared return type: setting this to true by-passes the run-time checking of the type
        /// of the value, together with code that would otherwise perform atomization, numeric type promotion, and similar
        /// conversions. If the value is set to true and the value is not of the correct type, the effect is unpredictable
        /// and probably disastrous.
        /// </summary>

        public virtual Boolean TrustResultType {
            get{return false;}
        }

        /// <summary>
        /// This property must be set to true in a subclass if the evaluation of the function makes use of the context
        /// item, position, or size from the dynamic context. This inhibits certain Saxon optimizations.
        /// </summary>

        public virtual Boolean DependsOnFocus {
            get{return false;}
        }

        /// <summary>
        /// This property should be set to true in a subclass if the evaluation of the function has side-effects.
        /// Saxon never guarantees the result of calling functions with side-effects, but if this property is set,
        /// then certain aggressive optimizations will be avoided, making it more likely that the function behaves
        /// as expected.
        /// </summary>

        public virtual Boolean HasSideEffects {
            get{return false;}
        }

        /// <summary>
        /// Factory method to create an ExtensionFunctionCall object, representing a specific function call in the XSLT or XQuery
        /// source code. Saxon will call this method once it has identified that a specific call relates to this extension
        /// function.
        /// </summary>
        /// <returns>
        /// An instance of the appropriate implementation of <code>ExtensionFunctionCall</code>
        /// </returns>

        public abstract ExtensionFunctionCall MakeFunctionCall();
    }

    /// <summary>
    /// <para>An instance of this class will be created by the compiler for each function call to this extension function
    /// that is found in the source code. The implementation may therefore retain information about the static context of the
    /// call. Once compiled, however, the instance object must be immutable.</para>
    /// </summary>

    public abstract class ExtensionFunctionCall {

        /// <summary>
        /// Method called by the compiler (at compile time) to provide information about the static context of the
        /// function call. The implementation may retain this information for use at run-time, if the result of the
        /// function depends on information in the static context.
        /// </summary>
        /// <remarks>
        /// For efficiency, the implementation should only retain copies of the information that it actually needs. It
        /// is not a good idea to hold a reference to the static context itself, since that can result in a great deal of
        /// compile-time information being locked into memory during run-time execution.
        /// </remarks>
        /// <param name="context">Information about the static context in which the function is called</param>

        public virtual void SupplyStaticContext(StaticContext context)
        {
            // default: no action
        }

        /// <summary>
        /// A subclass must implement this method if it retains any local data at the instance level. On some occasions
        /// (for example, when XSLT or XQuery code is inlined), Saxon will make a copy of an <c>ExtensionFunction</c> object.
        /// It will then call this method on the old object, supplying the new object as the value of the argument, and the
        /// method must copy all local data items from the old object to the new.
        /// </summary>
        /// <param name="destination">The new extension function object. This will always be an instance of the same
        /// class as the existing object.</param>

        public virtual void CopyLocalData(ExtensionFunctionCall destination) { }

        /// <summary>
        /// Method called at run time to evaluate the function.
        /// </summary>
        /// <param name="arguments">The values of the arguments to the function, supplied as iterators over XPath
        /// sequence values.</param>
        /// <param name="context"></param>
        /// <returns></returns>

        public abstract IXdmEnumerator Call(IXdmEnumerator[] arguments, DynamicContext context);
    }

    internal class WrappedExtensionFunctionDefinition : JExtensionFunctionDefinition
    {
        ExtensionFunctionDefinition definition;

        public WrappedExtensionFunctionDefinition(ExtensionFunctionDefinition definition)
        {
            this.definition = definition;
        }

        public override JStructuredQName getFunctionQName()
        {
            return definition.FunctionName.ToStructuredQName();
        }

        public override int getMinimumNumberOfArguments()
        {
            return definition.MinimumNumberOfArguments;
        }

        public override int getMaximumNumberOfArguments()
        {
            return definition.MaximumNumberOfArguments;
        }

        public override JSequenceType[] getArgumentTypes()
        {
            XdmSequenceType[] dt = definition.ArgumentTypes;
            JSequenceType[] jt = new JSequenceType[dt.Length];
            for (int i = 0; i < dt.Length; i++)
            {
                jt[i] = dt[i].ToSequenceType();
            }
            return jt;
        }

        public override JSequenceType getResultType(JSequenceType[] argumentTypes)
        {
            XdmSequenceType[] dt = new XdmSequenceType[argumentTypes.Length];
            for (int i = 0; i < dt.Length; i++)
            {
                dt[i] = XdmSequenceType.FromSequenceType(argumentTypes[i]);
            }

            XdmSequenceType rt = definition.ResultType(dt);
            return rt.ToSequenceType();
        }

        public override Boolean trustResultType()
        {
            return definition.TrustResultType;
        }

        public override Boolean dependsOnFocus()
        {
            return definition.DependsOnFocus;
        }

        public override Boolean hasSideEffects()
        {
            return definition.HasSideEffects;
        }

        public override JExtensionFunctionCall makeCallExpression()
        {
            return new WrappedExtensionFunctionCall(definition.MakeFunctionCall());
        }

    }

    internal class WrappedExtensionFunctionCall : JExtensionFunctionCall {

        ExtensionFunctionCall functionCall;

        public WrappedExtensionFunctionCall(ExtensionFunctionCall call)
        {
            this.functionCall = call;
        }

        public override void copyLocalData(JExtensionFunctionCall destination)
        {
            functionCall.CopyLocalData(((WrappedExtensionFunctionCall)destination).functionCall);
        }

        public override JSequenceIterator call(JSequenceIterator[] arguments, JXPathContext context)
        {
            SequenceEnumerator[] na = new SequenceEnumerator[arguments.Length];
            for (int i = 0; i < na.Length; i++)
            {
                na[i] = new SequenceEnumerator(arguments[i]);
            }
            DynamicContext dc = new DynamicContext(context);
            IXdmEnumerator result = functionCall.Call(na, dc);
            return new DotNetSequenceIterator(result);
        }
    }
}
