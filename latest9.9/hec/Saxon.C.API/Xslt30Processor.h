////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2020 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#ifndef SAXON_XSLT30_H
#define SAXON_XSLT30_H


#include "SaxonProcessor.h"
//#include "XdmValue.h"
#include <string>

class SaxonProcessor;
class SaxonApiException;
class XsltExecutable;
class XdmValue;
class XdmItem;
class XdmNode;

/*! An <code>Xslt30Processor</code> represents factory to compile, load and execute a stylesheet.
 * It is possible to cache the context and the stylesheet in the <code>Xslt30Processor</code>.
 */
class Xslt30Processor {

public:

    //! Default constructor.
    /*!
      Creates a Saxon-HE product
    */
    Xslt30Processor();

    //! Constructor with the SaxonProcessor supplied.
    /*!
      @param proc - Supplied pointer to the SaxonProcessor object
      cwd - The current working directory
    */
    Xslt30Processor(SaxonProcessor* proc, std::string cwd="");

	/**
	 * Xslt30Processor copy constructor.
	 * @param other - Xslt30Processor
	 */
    Xslt30Processor(const Xslt30Processor &other);

     ~Xslt30Processor();

	//! Get the SaxonProcessor object
	/**
	* @return SaxonProcessor - Pointer to the object
	*/
    SaxonProcessor * getSaxonProcessor(){return proc;}

    //!set the current working directory (cwd). This method also applies to the
    // static base URI for XSLT stylesheets when supplied as lexical string.
    /**
      * The cwd is used to set the base URI is part of the static context, and is used to resolve
      * any relative URIs appearing within XSLT.
      * @param cwd - Current working directory
     */
   void setcwd(const char* cwd);



    /**
    * Say whether just-in-time compilation of template rules should be used.
    * @param jit true if just-in-time compilation is to be enabled. With this option enabled,
    *            static analysis of a template rule is deferred until the first time that the
    *            template is matched. This can improve performance when many template
    *            rules are rarely used during the course of a particular transformation; however,
    *            it means that static errors in the stylesheet will not necessarily cause the
    *            {@link #compile(Source)} method to throw an exception (errors in code that is
    *            actually executed will still be notified to the registered <code>ErrorListener</code>
    *            or <code>ErrorList</code>, but this may happen after the {@link #compile(Source)}
    *            method returns). This option is enabled by default in Saxon-EE, and is not available
    *            in Saxon-HE or Saxon-PE.
    *            <p><b>Recommendation:</b> disable this option unless you are confident that the
    *            stylesheet you are compiling is error-free.</p>
    */
    void setJustInTimeCompilation(bool jit);


    /**
     * Set the value of a stylesheet parameter. Static (compile-time) parameters must be provided using
     * this method on the XsltCompiler object, prior to stylesheet compilation. Non-static parameters
     * may also be provided using this method if their values will not vary from one transformation
     * to another.
     *
     * @param name  the name of the stylesheet parameter, as a string. For namespaced parameter use the JAXP solution i.e. "{uri}name"
     * @param value the value of the stylesheet parameter, or null to clear a previously set value
     * @param _static For static (compile-time) parameters we set this flag to true, which means the parameter is
     * must be set on the XsltCompiler object, prior to stylesheet compilation. The default is false. Non-static parameters
     * may also be provided.
     */
    void setParameter(const char* name, XdmValue*value);



    /**
     * Get a parameter value by name
     * @param name - Specified paramater name to get
     * @return XdmValue
    */
     XdmValue* getParameter(const char* name);


    /**
     * Remove a parameter (name, value) pair from a stylesheet
     *
     * @param name  the name of the stylesheet parameter
     * @return bool - outcome of the romoval
     */
    bool removeParameter(const char* name);

	//! Get all parameters as a std::map
     /**
      * 
      * Please note that the key name has been prefixed with 'param:', for example 'param:name'
      * @return std:map with key as string name mapped to XdmValue. 
      * 
     */
     std::map<std::string,XdmValue*>& getParameters();


    //!Clear parameter values set
    /**
     * Default behaviour (false) is to leave XdmValues in memory
     *  true then XdmValues are deleted
     *  @param deleteValues.  Individual pointers to XdmValue objects have to be deleted in the calling program
     */
    void clearParameters(bool deleteValues=false);


    /**
    * Utility method for working with Saxon/C on Python
    */
    XdmValue ** createXdmValueArray(int len){
	return (new XdmValue*[len]);
    }

    /**
    * Utility method for working with Saxon/C on Python
    */
    char** createCharArray(int len){
	return (new char*[len]);
    }

    void deleteXdmValueArray(XdmValue ** arr, int len){
	for(int i =0; i< len; i++) {
		//delete arr[i];	
	}
	delete [] arr;
    }

    /**
     * This method gives users the option to switch on or off the <code>xsl:message</code> feature. It is also possible
     * to send the <code>xsl:message</code> outputs to file given by file name.
     * @param show - boolean to indicate if xsl:message should be outputted. Default is on.
     * @param  filename - If the filename argument is present then the xsl:message output is appended to the given
     *                    filename with location cwd+filename
     */
    void setupXslMessage(bool show, const char* filename=NULL);



      //!Perform a one shot transformation.
    /**
     * The result is stored in the supplied outputfile.
     *
     * @param sourcefile - The file name of the source document
     * @param stylesheetfile - The file name of the stylesheet document. If NULL the most recently compiled stylesheet is used
     * @param outputfile - The file name where results will be stored
     */
    void transformFileToFile(const char* sourcefile, const char* stylesheetfile, const char* outputfile); 

	//!Perform a one shot transformation.
    /**
     * The result is returned as a string
     *
     * @param sourcefile - The file name of the source document
     * @param stylesheetfile - The file name of the stylesheet document. If NULL the most recently compiled stylesheet is used
     * @return char array - result of the transformation
     */
    const char * transformFileToString(const char* sourcefile, const char* stylesheetfile);

    /**
     * Perform a one shot transformation. The result is returned as an XdmValue
     *
     * @param sourcefile - The file name of the source document
     * @param stylesheetfile - The file name of the stylesheet document. If NULL the most recently compiled stylesheet is used
     * @return XdmValue - result of the transformation
     */
    XdmValue * transformFileToValue(const char* sourcefile, const char* stylesheetfile);


     //! compile a stylesheet file.
    /**
     * The compiled stylesheet is cached and available for execution later.
     * @param stylesheet  - The file name of the stylesheet document.
     * @return an XsltExecutable, which represents the compiled stylesheet. The XsltExecutable
     * is immutable and thread-safe; it may be used to run multiple transformations, in series or concurrently.
     */
    XsltExecutable * compileFromFile(const char* stylesheet);

     //!compile a stylesheet received as a string.
    /**
     * 
     * The compiled stylesheet is cached and available for execution later.
     * @param stylesheet as a lexical string representation
     * @return an XsltExecutable, which represents the compiled stylesheet. The XsltExecutable
     * is immutable and thread-safe; it may be used to run multiple transformations, in series or concurrently.
     */
    XsltExecutable * compileFromString(const char* stylesheet);



     //! Get the stylesheet associated
     /* via the xml-stylesheet processing instruction (see
     * http://www.w3.org/TR/xml-stylesheet/) with the document
     * document specified in the source parameter, and that match
     * the given criteria.  If there are several suitable xml-stylesheet
     * processing instructions, then the returned Source will identify
     * a synthesized stylesheet module that imports all the referenced
     * stylesheet module.*/
    /**
     * The compiled stylesheet is cached and available for execution later.
     * @param sourceFile  - The file name of the XML document.
     * @return an XsltExecutable, which represents the compiled stylesheet. The XsltExecutable
     * is immutable and thread-safe; it may be used to run multiple transformations, in series or concurrently.
     */
    XsltExecutable * compileFromAssociatedFile(const char* sourceFile);


     //!compile a stylesheet received as a string and save to an exported file (SEF).
    /**
     * 
     * The compiled stylesheet is saved as SEF to file store
     * @param stylesheet as a lexical string representation
     * @param filename - the file to which the compiled package should be saved
     */
    void compileFromStringAndSave(const char* stylesheet, const char* filename);


     //!compile a stylesheet received as a file and save to an exported file (SEF).
    /**
     * 
     * The compiled stylesheet is saved as SEF to file store
     * @param xslFilename - file name of the stylesheet
     * @param filename - the file to which the compiled package should be saved
     */
    void compileFromFileAndSave(const char* xslFilename, const char* filename);


     //!compile a stylesheet received as an XdmNode.
    /**
     * The compiled stylesheet is cached and available for execution later.
     * @param stylesheet as a lexical string representation
     * @param filename - the file to which the compiled package should be saved
     */
    void compileFromXdmNodeAndSave(XdmNode * node, const char* filename);

     //!compile a stylesheet received as an XdmNode.
    /**
     * The compiled stylesheet is cached and available for execution later.
     * @param stylesheet as a lexical string representation
     * @return an XsltExecutable, which represents the compiled stylesheet. The XsltExecutable
     * is immutable and thread-safe; it may be used to run multiple transformations, in series or concurrently.
     */
    XsltExecutable * compileFromXdmNode(XdmNode * node);


    /**
     * Checks for pending exceptions without creating a local reference to the exception object
     * @return bool - true when there is a pending exception; otherwise return false
    */
    bool exceptionOccurred();


     //! Check for exception thrown.
	/**
	* @return cha*. Returns the exception message if thrown otherwise return NULL
	*/
    const char* checkException();


     //! Clear any exception thrown
    void exceptionClear();


     //! Get the ith error message if there are any error
    /**
     * A transformation may have a number of errors reported against it.
     * @return char* - The message of the exception
    */
    const char * getErrorMessage();

     //! Get the ith error code if there are any error
    /**
     * A transformation may have a number of errors reported against it.
     * @return char* - The error code of the exception. The error code are related to the specific specification
    */
    const char * getErrorCode();



private:
	SaxonProcessor* proc;/*! */
	jclass  cppClass;
	jobject cppXT;
    std::string cwdXT; /*!< current working directory */
	bool tunnel, jitCompilation;
	std::map<std::string,XdmValue*> parameters; /*!< map of parameters used for the transformation as (string, value) pairs */
    SaxonApiException * exception;
};


#endif /* SAXON_XSLT30_H */
