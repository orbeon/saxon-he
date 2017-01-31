////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2016 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////#ifndef PHP_SAXON_H

#ifndef PHP_SAXON_H
#define PHP_SAXON_H

#include <phpcpp.h>
#include <unistd.h>
#include <iostream>
#include <jni.h>
#include "SaxonProcessor.h"
#include "XdmValue.h"
#include "XdmItem.h"
#include "XdmNode.h"
#include "XdmAtomicValue.h"







#define PHP_SAXON_EXTNAME  "Saxon/C"
#define PHP_SAXON_EXTVER   "1.1.0"

class PHP_SaxonProcessor : public Php::Base
{

private:

	 SaxonProcessor* saxonProcessor;
	bool _license;
	const char* _cwd;

public:





/*
 * Class:     com_saxonica_functions_extfn_PhpCall_PhpFunctionCall
 * Method:    _phpCall
 * Signature: ([Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_com_saxonica_functions_extfn_PhpCall_00024PhpFunctionCall__1phpCall
  (JNIEnv *, jobject, jobjectArray, jobjectArray);





	
	    /**
     		*  Constructor
     		*
     		*  @param  boolean   optional to accept one parameter - license flag
		*  @param  boolean   optional to accept two parameter - license flag
     		*  @return int     New value
     		*/
	PHP_SaxonProcessor() = default;

	void __construct(Php::Parameters &params)
    {
        // copy first parameter (if available)
        if (params.size()== 1) {
		_license = params[0];
		saxonProcessor = new SaxonProcessor(_license);
#if !(defined (__linux__) || (defined (__APPLE__) && defined(__MACH__)))
	    TCHAR s[256];

            // --
            DWORD a = GetCurrentDirectory(256, s);
	    const size_t newsize = wcslen(s)*2;
	    char* cwd = new char[newsize];
	    wcstombs_s(0, cwd, newsize, s, _TRUNCATE);
	    // -- code above returns the apache installtion directory as the CWD

	    char* cwd2;

	    //php_error(E_WARNING,cwd2);

	    saxonProcessor->setcwd(cwd2);
	    // -- code above tries to use VCWD_GETCWD but there is a linkage error
#else

	    char cwd[256];

	    getcwd(cwd, sizeof(cwd));
	    if(cwd == NULL) {
	     //php_error(E_WARNING,"cwd is null");
	   }else {
             //php_error(E_WARNING,cwd);

	    saxonProcessor->setcwd(cwd);
          }
#endif
	}
	 else if (params.size() == 2) {
		 _license = params[0];
		_cwd = params[1];
		saxonProcessor = new SaxonProcessor(_license);
		saxonProcessor->setcwd((const char *)_cwd);
	}
    }

	virtual ~PHP_SaxonProcessor(){
		delete saxonProcessor;
	}


    	Php::Value createAtomicValue(Php::Parameters &params);
        Php::Value  parseXmlFromString(Php::Parameters &params);
        Php::Value  parseXmlFromFile(Php::Parameters &params);
         void setcwd(Php::Parameters &params);
//    PHP_METHOD(SaxonProcessor,  importDocument);
       void setResourcesDirectory(Php::Parameters &params);
    void registerPHPFunction(Php::Parameters &params);
 //   PHP_METHOD(SaxonProcessor,  getXslMessages);
       void  setConfigurationProperty(Php::Parameters &params);
    Php::Value newXsltProcessor();
    Php::Value newXQueryProcessor();
    Php::Value newXPathProcessor();
    Php::Value newSchemaValidator();
    Php::Value version();

};

class PHP_XsltProcessor : public Php::Base
{
private:
	XsltProcessor *xsltProcessor;

public:
    PHP_XsltProcessor() = default;

    /*void __construct(Php::Parameters &params){

    }*/

    virtual ~PHP_XsltProcessor(){
	delete xsltProcessor;
    }

    PHP_XsltProcessor(XsltProcessor * value) {
	xsltProcessor = value;
    }

    void transformFileToFile(Php::Parameters &params);

    Php::Value  transformFileToString(Php::Parameters &params);

    Php::Value  transformFileToValue(Php::Parameters &params);
    Php::Value  transformToString(Php::Parameters &params);
    Php::Value  transformToValue(Php::Parameters &params);
    void  transformToFile(Php::Parameters &params);
    void compileFromFile(Php::Parameters &params);
    void compileFromValue(Php::Parameters &params);
    void compileFromString(Php::Parameters &params);
    void  setOutputFile(Php::Parameters &params);
    void  setSourceFromFile(Php::Parameters &params);
    void  setSourceFromXdmValue(Php::Parameters &params);
    void  setParameter(Php::Parameters &params);
    void  setProperty(Php::Parameters &params);
    void  clearParameters();
    void  clearProperties();
    void  exceptionClear();
    Php::Value  exceptionOccurred();
    Php::Value  getErrorCode(Php::Parameters &params);
    Php::Value  getErrorMessage(Php::Parameters &params);
    Php::Value  getExceptionCount();


};


class PHP_XQueryProcessor : public Php::Base
{

private:
	XQueryProcessor *xqueryProcessor;

public:
    PHP_XQueryProcessor() = default;

    

    virtual ~PHP_XQueryProcessor(){
	delete xqueryProcessor;
    }

    PHP_XQueryProcessor(XQueryProcessor * value) {
	xqueryProcessor = value;
    }

    void  setQueryContent(Php::Parameters &params);
    void  setContextItem(Php::Parameters &params);
    void  setContextItemFromFile(Php::Parameters &params);
    void  setParameter(Php::Parameters &params);
    void  setProperty(Php::Parameters &params);
    void  setOutputFile(Php::Parameters &params);
    void  setQueryFile(Php::Parameters &params);
    void  setQueryBaseURI(Php::Parameters &params);
    void  declareNamespace(Php::Parameters &params);
    void  clearParameters();
    void  clearProperties();

    Php::Value  runQueryToValue();
    Php::Value  runQueryToString();
    Php::Value  runQueryToFile(Php::Parameters &params);
    void  exceptionClear();
    Php::Value  exceptionOccurred();
    Php::Value  getErrorCode(Php::Parameters &params);
    Php::Value  getErrorMessage(Php::Parameters &params);
    Php::Value  getExceptionCount();

};
    

   class PHP_XPathProcessor : public Php::Base
{


private:
	XPathProcessor *xpathProcessor;

public:
    PHP_XPathProcessor() = default;

    

    virtual ~PHP_XPathProcessor(){
	delete xpathProcessor;
    }

    PHP_XPathProcessor(XPathProcessor * value) {
	xpathProcessor = value;
    }
/*
// PHP_METHOD(XPathProcessor,  __construct);
    PHP_METHOD(XPathProcessor,  __destruct);
    PHP_METHOD(XPathProcessor,  setContextItem);
    PHP_METHOD(XPathProcessor,  setContextFile);
    PHP_METHOD(XQueryProcessor, setBaseURI);
    PHP_METHOD(XPathProcessor,  effectiveBooleanValue);
    PHP_METHOD(XPathProcessor,  evaluate);
    PHP_METHOD(XPathProcessor,  evaluateSingle);
    PHP_METHOD(XPathProcessor, declareNamespace);
    PHP_METHOD(XPathProcessor,  setParameter);
    PHP_METHOD(XPathProcessor,  setProperty);
    PHP_METHOD(XPathProcessor,  clearParameters);
    PHP_METHOD(XPathProcessor,  clearProperties);
    PHP_METHOD(XPathProcessor,  exceptionClear);
    PHP_METHOD(XPathProcessor,  exceptionOccurred);
    PHP_METHOD(XPathProcessor,  getErrorCode);
    PHP_METHOD(XPathProcessor,  getErrorMessage);
    PHP_METHOD(XPathProcessor,  getExceptionCount);
*/

};

   

   class PHP_SchemaValidator : public Php::Base
{


private:
	SchemaValidator *schemaValidator;

public:
    PHP_SchemaValidator() = default;

    

    virtual ~PHP_SchemaValidator(){
	delete schemaValidator;
    }

    PHP_SchemaValidator(SchemaValidator * value) {
	schemaValidator = value;
    }

/*
 // PHP_METHOD(SchemaValidator,  __construct);
    PHP_METHOD(SchemaValidator,  __destruct);
    PHP_METHOD(SchemaValidator,  setSourceNode);
    PHP_METHOD(SchemaValidator,  setOutputFile);
    PHP_METHOD(SchemaValidator, registerSchemaFromFile);
    PHP_METHOD(SchemaValidator, registerSchemaFromString);
    PHP_METHOD(SchemaValidator, validate); 
    PHP_METHOD(SchemaValidator, validateToNode);
    PHP_METHOD(SchemaValidator, getValidationReport);
    PHP_METHOD(SchemaValidator,  setParameter);
    PHP_METHOD(SchemaValidator,  setProperty);
    PHP_METHOD(SchemaValidator,  clearParameters);
    PHP_METHOD(SchemaValidator,  clearProperties);
    PHP_METHOD(SchemaValidator,  exceptionClear);
    PHP_METHOD(SchemaValidator,  exceptionOccurred);
    PHP_METHOD(SchemaValidator,  getErrorCode);
    PHP_METHOD(SchemaValidator,  getErrorMessage);
    PHP_METHOD(SchemaValidator,  getExceptionCount);
*/
};


   
	

/*     ============== PHP Interface of   XdmValue =============== */

   class PHP_XdmValue : public Php::Base
{
protected:
	XdmValue * _value;

public:
	PHP_XdmValue() = default;

	virtual ~PHP_XdmValue(){
		delete _value;
	}

	/*void __construct(Php::Parameters &params)
   	 {

		//if (!params.empty()) _value = params[0];

	}*/
	PHP_XdmValue(XdmValue * value) {
		_value = value;
	}

	XdmValue * getInternal(){
		return _value;
	}

//TODO implement a __toString() method

/*
PHP_METHOD(XdmValue,  __construct);
    PHP_METHOD(XdmValue,  __destruct);
    PHP_METHOD(XdmValue,  getHead);
    PHP_METHOD(XdmValue,  itemAt);
    PHP_METHOD(XdmValue,  size);
    PHP_METHOD(XdmValue, addXdmItem);
*/

};

    


/*     ============== PHP Interface of   XdmItem =============== */

   class PHP_XdmItem : public PHP_XdmValue
{

private:


public:

	PHP_XdmItem() {}
	
	PHP_XdmItem(XdmItem * nodei) {
		_value = (XdmValue*)nodei;
	}
//TODO implement a __toString() method
/*
PHP_METHOD(XdmItem,  __construct);
    PHP_METHOD(XdmItem,  __destruct);
    PHP_METHOD(XdmItem,  getStringValue);
    PHP_METHOD(XdmItem,  isAtomic);
    PHP_METHOD(XdmItem,  isNode);
    PHP_METHOD(XdmItem,  getAtomicValue);
    PHP_METHOD(XdmItem,  getNodeValue);
*/

};

    	

/*     ============== PHP Interface of   XdmNode =============== */

   class PHP_XdmNode : public PHP_XdmItem
{

private:
	

public:
	
	PHP_XdmNode(XdmNode * nodei) {
		_value = (XdmValue*)nodei;
	}

//TODO implement a __toString() method

/*
PHP_METHOD(XdmNode,  __construct);
    PHP_METHOD(XdmNode,  __destruct);
    PHP_METHOD(XdmNode,  getStringValue);
    PHP_METHOD(XdmNode, getNodeKind);
    PHP_METHOD(XdmNode, getNodeName);
    PHP_METHOD(XdmNode,  isAtomic);
    PHP_METHOD(XdmNode,  getChildCount);   
    PHP_METHOD(XdmNode,  getAttributeCount); 
    PHP_METHOD(XdmNode,  getChildNode);
    PHP_METHOD(XdmNode,  getParent);
    PHP_METHOD(XdmNode,  getAttributeNode);
    PHP_METHOD(XdmNode,  getAttributeValue);
*/

};

    
    

/*     ============== PHP Interface of   XdmAtomicValue =============== */

   class PHP_XdmAtomicValue : public PHP_XdmItem
{

private:
	

public:
	
	PHP_XdmAtomicValue(XdmAtomicValue * value) {
		_value = (XdmValue *)value;
	}

	Php::Value getStringValue();

	Php::Value getBooleanValue();

	Php::Value getLongValue();

	Php::Value getDoubleValue();

	Php::Value isAtomic();

	Php::Value __toString()
   	 {
        return getStringValue();
    	}

/*
PHP_METHOD(XdmAtomicValue,  __construct);
    PHP_METHOD(XdmAtomicValue,  __destruct);
    PHP_METHOD(XdmAtomicValue,  getStringValue);
    PHP_METHOD(XdmAtomicValue,  getBooleanValue);
    PHP_METHOD(XdmAtomicValue,  getDoubleValue);
    PHP_METHOD(XdmAtomicValue,  getLongValue);
    PHP_METHOD(XdmAtomicValue,  isAtomic);
*/

};

    


#endif /* PHP_SAXON_H */

















