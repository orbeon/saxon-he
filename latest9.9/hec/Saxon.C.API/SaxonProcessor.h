////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2020 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#ifndef SAXON_PROCESSOR_H
#define SAXON_PROCESSOR_H
	
#if defined __linux__ || defined __APPLE__
        #include <stdlib.h>
        #include <string.h>
        #include <dlfcn.h>

        #define HANDLE void*
        #define LoadLibrary(x) dlopen(x, RTLD_LAZY)
        #define GetProcAddress(x,y) dlsym(x,y)
#else
    #include <windows.h>
#endif

//#define DEBUG //remove
#define CVERSION "1.3.0"
#define CVERSION_API_NO 130
#include <string>
#include <iostream>
#include <sstream>  
#include <map>	
#include <vector>
#include <stdexcept>      // std::logic_error

#include "SaxonCGlue.h"
#include "SaxonCXPath.h"
#include "XsltProcessor.h"
#include "Xslt30Processor.h"
#include "XQueryProcessor.h"
#include "XPathProcessor.h"
#include "SchemaValidator.h"
#include "SaxonApiException.h"
//#include "com_saxonica_functions_extfn_PhpCall.h"
//#include "com_saxonica_functions_extfn_PhpCall_PhpFunctionCall.h"

class XsltProcessor;
class Xslt30Processor;
class XQueryProcessor;
class XPathProcessor;
class SchemaValidator;
class XdmValue;
class XdmNode;
class XdmItem;
class XdmAtomicValue;

#if CVERSION_API_NO >= 123
class XdmFunctionItem;
class XdmArray;
class XdmMap;
#endif



// The Saxon XSLT interface class

//std::mutex mtx;
/*! <code>MyException</code>. This struct captures details of the Java exception thrown from Saxon s9api API (Java).
 * <p/>
 */
typedef struct {
		std::string errorCode;
		std::string errorMessage;
		int linenumber;
	    	bool isType;
	    	bool isStatic;
	    	bool isGlobal;
	}MyException;

typedef struct
{
    jobjectArray stringArray;
    jobjectArray objectArray;

}JParameters;



//==========================================



/*! An <code>SaxonProcessor</code> acts as a factory for generating XQuery, XPath, Schema and XSLT compilers
 */
class SaxonProcessor {
friend class XsltProcessor;
friend class Xslt30Processor;
friend class XQueryProcessor;
friend class SchemaValidator;
friend class XPathProcessor;
friend class XdmValue;
friend class XdmAtomicValue;

public:

   //! A default constructor.
    /*!
      * Create Saxon Processor.
    */

    SaxonProcessor();

   //! constructor based upon a Saxon configuration file.
    /*!
      * Create Saxon Processor.
    */

    SaxonProcessor(const char * configFile);


   //! A constructor.
    /*!
      * Create Saxon Processor.
      * @param l - Flag that a license is to be used. Default is false.	
    */
    SaxonProcessor(bool l);

    SaxonProcessor& operator=( const SaxonProcessor& other );


	/**
	 * Xslt30Processor copy constructor.
	 * @param other - Xslt30Processor
	 */
    SaxonProcessor(const SaxonProcessor &other);

   /*!

      * Destructor
    */
    ~SaxonProcessor();


   //! Get the Processor object. Method used in Python
   /* SaxonProcessor * getProcessor(){
	return this;
    }*/
	
   /*!

      * Create an XsltProcessor. An XsltProcessor is used to compile XSLT stylesheets.
      * @return a newly created XsltProcessor	
    */	
    XsltProcessor * newXsltProcessor();

   /*!

      * Create an Xslt30Processor. An Xslt30Processor is used to compile XSLT30 stylesheets.
      * @return a newly created Xslt30Processor	
    */	
    Xslt30Processor * newXslt30Processor();


    /*!
     * Create an XQueryProcessor. An XQueryProcessor is used to compile XQuery queries.
     *
     * @return a newly created XQueryProcessor
     */
    XQueryProcessor * newXQueryProcessor();


    /*!
     * Create an XPathProcessor. An XPathProcessor is used to compile XPath expressions.
     *
     * @return a newly created XPathProcessor
     */
    XPathProcessor * newXPathProcessor();

    /*!
     * Create a SchemaValidator which can be used to validate instance documents against the schema held by this
     * SchemaManager
     *
     * @return a new SchemaValidator
     */
    SchemaValidator * newSchemaValidator();


    /*!
     * Factory method. Unlike the constructor, this avoids creating a new StringValue in the case
     * of a zero-length string (and potentially other strings, in future)
     *
     * @param value the String value. Null is taken as equivalent to "".
     * @return the corresponding StringValue
     */
    XdmAtomicValue * makeStringValue(std::string str);

    /*!
     * Factory method. Unlike the constructor, this avoids creating a new StringValue in the case
     * of a zero-length string (and potentially other strings, in future)
     *
     * @param value the char pointer array. Null is taken as equivalent to "".
     * @return the corresponding StringValue
     */
    XdmAtomicValue * makeStringValue(const char * str);

    /*!
     * Factory method: makes either an Int64Value or a BigIntegerValue depending on the value supplied
     *
     * @param i the supplied primitive integer value
     * @return the value as a XdmAtomicValue which is a BigIntegerValue or Int64Value as appropriate
     */
    XdmAtomicValue * makeIntegerValue(int i);


    /*!
     * Factory method (for convenience in compiled bytecode)
     *
     * @param d the value of the double
     * @return a new XdmAtomicValue
     */
    XdmAtomicValue * makeDoubleValue(double d);

    /*!
     * Factory method (for convenience in compiled bytecode)
     *
     * @param f the value of the foat
     * @return a new XdmAtomicValue
     */
    XdmAtomicValue * makeFloatValue(float);

    /*!
     * Factory method: makes either an Int64Value or a BigIntegerValue depending on the value supplied
     *
     * @param l the supplied primitive long value
     * @return the value as a XdmAtomicValue which is a BigIntegerValue or Int64Value as appropriate
     */
    XdmAtomicValue * makeLongValue(long l);

    /*!
     * Factory method: makes a XdmAtomicValue representing a boolean Value
     *
     * @param b true or false, to determine which boolean value is
     *              required
     * @return the XdmAtomicValue requested
     */
    XdmAtomicValue * makeBooleanValue(bool b);

    /**
     * Create an QName Xdm value from string representation in clark notation
     * @param str - The value given in a string form in clark notation. {uri}local
     * @return XdmAtomicValue - value
    */
    XdmAtomicValue * makeQNameValue(const char * str);

    /*!
     * Create an Xdm Atomic value from string representation
     * @param type    - Local name of a type in the XML Schema namespace.
     * @param value - The value given in a string form.
     * In the case of a QName the value supplied must be in clark notation. {uri}local
     * @return XdmValue - value
    */
    XdmAtomicValue * makeAtomicValue(const char * type, const char * value);

#if CVERSION_API_NO >= 123
    /**
        * Make an XdmArray whose members are from string representation
        * @param input the input array of booleans
        * @return an XdmArray whose members are xs:boolean values corresponding one-to-one with the input
   */
    XdmArray * makeArray(const char ** input, int length);


    /**
        * Make an XdmArray whose members are xs:short values
        * @param input the input array of booleans
        * @return an XdmArray whose members are xs:boolean values corresponding one-to-one with the input
   */
    XdmArray * makeArray(short * input, int length);



    /**
        * Make an XdmArray whose members are xs:int values
        * @param input the input array of booleans
        * @return an XdmArray whose members are xs:boolean values corresponding one-to-one with the input
   */
    XdmArray * makeArray(int * input, int length);

    /**
        * Make an XdmArray whose members are xs:long values
        * @param input the input array of booleans
        * @return an XdmArray whose members are xs:boolean values corresponding one-to-one with the input
   */
    XdmArray * makeArray(long * input, int length);

    /**
        * Make an XdmArray whose members are xs:boolean values
        * @param input the input array of booleans
        * @return an XdmArray whose members are xs:boolean values corresponding one-to-one with the input
   */
    XdmArray * makeArray(bool * input, int length);


    XdmMap * makeMap(std::map<XdmAtomicValue *, XdmValue*> dataMap);
    

#endif

     /**
     * Get the string representation of the XdmValue.
     * @return char array
     */
    const char * getStringValue(XdmItem * item);

    /**
     * Parse a lexical representation of the source document and return it as an XdmNode
    */
    XdmNode * parseXmlFromString(const char* source);

    /**
     * Parse a source document file and return it as an XdmNode.
    */
    XdmNode * parseXmlFromFile(const char* source);

    /**
     * Parse a source document available by URI and return it as an XdmNode.
    */
    XdmNode * parseXmlFromUri(const char* source);

    int getNodeKind(jobject);

    bool isSchemaAwareProcessor();

 

    /**
     * Checks for thrown exceptions
     * @return bool - true when there is a pending exception; otherwise return false
    */
    bool exceptionOccurred();

    /**

     * Clears any exception that is currently being thrown. If no exception is currently being thrown, this routine has no effect.
    */
    void exceptionClear();

    /**
     * Checks for pending exceptions and creates a SaxonApiException object, which handles one or more local exceptions objects
     * @param env
     * @param callingClass
     * @param callingObject
     * @return SaxonApiException
    */
    SaxonApiException * checkForExceptionCPP(JNIEnv* env, jclass callingClass,  jobject callingObject);


    /*
      * Clean up and destroy Java VM to release memory used. 
     */
    static void release();


    /**
     * set the current working directory
    */
   void setcwd(const char* cwd);

    /**
     * get the current working directory
    */
   const char* getcwd();


    /**
     * set saxon resources directory
    */
   void setResourcesDirectory(const char* dir);
	
    /**
     * set catalog to be used in Saxon
    */
   void setCatalog(const char* catalogFile, bool isTracing);

    /**
     * get saxon resources directory
    */
   const char * getResourcesDirectory();

    /**
     * Set a configuration property specific to the processor in use. 
     * Properties specified here are common across all the processors.
     * Example 'l':enable line number has the value 'on' or 'off'
     * @param name of the property
     * @param value of the property
     */
    void setConfigurationProperty(const char * name, const char * value);

    /**
     * Clear configuration properties specific to the processor in use. 
     */
     void clearConfigurationProperties();


    /**
     * Get the Saxon version
     * @return char array
     */
    const char * version();

/*
     * Add a native method.
     * @param name of the native method
     * @param signature of the native method
     * @param fnPtr Pointer to the native method
 */
void addNativeMethod(char *name, char* signature, void * fnPtr){

	JNINativeMethod method;
	method.name = name;
	method.signature = signature;
	method.fnPtr = fnPtr;

	nativeMethodVect.push_back(method);

	

}

/*
     * Register several native methods for one class.
     * @param libName name of the library which contains the function(s). Loads the library
     * @param gMethods Register native methods. Default is NULL, also NULL allowed in which cause assumption is made the user has added native methods using the method addNativeMethod .
 * @return bool success of registered native method
 */
bool registerCPPFunction(char * libName, JNINativeMethod * gMethods=NULL){
	if(libName != NULL) {
		setConfigurationProperty("extc", libName);
			
	}

	if(gMethods == NULL && nativeMethodVect.size()==0) {
	return false;
	} else {
		if(gMethods == NULL) {
			//copy vector to gMethods
			gMethods = new JNINativeMethod[nativeMethodVect.size()];
		} 
		return registerNativeMethods(sxn_environ->env, "com/saxonica/functions/extfn/CppCall$PhpFunctionCall",
    gMethods, sizeof(gMethods) / sizeof(gMethods[0]));
	

	}
	return false;
}

/*
 * Register several native methods for one class.
 * @return bool success of registered native method
 */
static bool registerNativeMethods(JNIEnv* env, const char* className,
    JNINativeMethod* gMethods, int numMethods)
{
    jclass clazz;
    clazz = env->FindClass(className);
    if (clazz == NULL) {
        std::cerr<<"Native registration unable to find class "<< className<<std::endl;
        return false;
    }
	
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
       // std::cerr<<"RegisterNatives failed for "<< className<<std::endl;
        return false;
    }
    return true;
}

	SaxonApiException * checkAndCreateException(jclass cppClass);



//	XPathEngine
//	XQueryEngine
//	SchemaManager

   // static JNIEnv *env;
    static int jvmCreatedCPP;
    static sxnc_environment * sxn_environ;
    static int refCount;
    std::string cwd; /*!< current working directory */
    jobject proc; /*!< Java Processor object */
    
    /*static JavaVM *jvm;*/
    
protected:



	jclass xdmAtomicClass;
	jclass  versionClass;
	jclass  procClass;
	jclass  saxonCAPIClass;
	std::string cwdV; /*!< current working directory */
	//std::string resources_dir; /*!< current Saxon resources directory */
	char * versionStr;
	std::map<std::string,XdmValue*> parameters; /*!< map of parameters used for the transformation as (string, value) pairs */
	std::map<std::string,std::string> configProperties; /*!< map of properties used for the transformation as (string, string) pairs */	 
	bool licensei; /*!< indicates whether the Processor requires a Saxon that needs a license file (i.e. Saxon-EE) other a Saxon-HE Processor is created  */
	bool closed;


	JNINativeMethod * nativeMethods;
	std::vector<JNINativeMethod> nativeMethodVect; /*!< Vector of native methods defined by user */
    SaxonApiException * exception;


private:

    

	void applyConfigurationProperties();
	// Saxon/C method for internal use
    static JParameters createParameterJArray(std::map<std::string,XdmValue*> parameters, std::map<std::string,std::string> properties);
    static JParameters createParameterJArray2(std::map<std::string,XdmValue*> parameters);
    static jobjectArray createJArray(XdmValue ** values, int length);
    static 	const char* checkException(jobject cpp);
};

//===============================================================================================

#endif /* SAXON_PROCESSOR_H */
