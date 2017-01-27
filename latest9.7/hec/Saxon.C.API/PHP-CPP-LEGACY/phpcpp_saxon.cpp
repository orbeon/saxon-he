#include "phpcpp_saxon.h"




void PHP_SaxonProcessor::setResourcesDirectory(Php::Parameters &params)
{
    
    const char * dirStr;
    int len;

    if (!params.empty()) {
        dirStr = params[0];
        if(dirStr != NULL) {
            saxonProcessor->setResourcesDirectory(dirStr);
        }
    }
}



Php::Value  PHP_SaxonProcessor::parseXmlFromString(Php::Parameters &params)
{
   
    const char * source;
    int len1;

  
    if (!params.empty()) {
	source = params[0];
        XdmNode* node = saxonProcessor->parseXmlFromString(source);
	PHP_XdmNode * php_node = new PHP_XdmNode(node);
	return Php::Object("Saxon\\XdmNode", php_node);
        
    }
}

Php::Value  PHP_SaxonProcessor::parseXmlFromFile(Php::Parameters &params)
{
   
    const char * source;
    int len1;

    if (!params.empty()) {
	source = params[0];
        XdmNode* node = saxonProcessor->parseXmlFromFile(source);
	PHP_XdmNode * php_xdmNode = new PHP_XdmNode(node);     
	return Php::Object("Saxon\\XdmNode", php_xdmNode);
    }
    return NULL;
}

void PHP_SaxonProcessor::registerPHPFunction(Php::Parameters &params){

 	if (!params.empty() && params.size() == 1) {
		const char * func = params[0];
	
	}
}

JNIEXPORT jobject JNICALL PHP_SaxonProcessor::Java_com_saxonica_functions_extfn_PhpCall_00024PhpFunctionCall__1phpCall
  (JNIEnv * env, jobject thisObj, jobjectArray arr1, jobjectArray arr2){
	return NULL;
}


Php::Value PHP_SaxonProcessor::createAtomicValue(Php::Parameters &params)
{
   /* XdmAtomicValue * xdmValue = NULL;
    SaxonProcessor * proc;
    char * source;
    int len1;
    zval *zvalue;
    bool bVal;
    char * sVal;
    int len;
    long iVal;
    double dVal;

	switch (Z_TYPE_P(zvalue)) {
            case IS_BOOL:
                bVal = Z_BVAL_P(zvalue);
                xdmValue = proc->makeBooleanValue((bool)bVal);
            break;
            case IS_LONG:
                iVal = Z_LVAL_P(zvalue);
		 xdmValue = proc->makeIntegerValue((int)iVal);
            break;
            case IS_STRING:
                sVal = Z_STRVAL_P(zvalue);
                len = Z_STRLEN_P(zvalue);
                xdmValue = proc->makeStringValue((const char*)sVal);
            break;
            case IS_NULL:
                xdmValue = new XdmAtomicValue();
            break;
            case IS_DOUBLE:
                dVal = (double)Z_DVAL_P(zvalue);
		xdmValue = proc->makeDoubleValue((double)iVal);
                break;
            case IS_ARRAY:
                // TODO: Should not be expected. Do this some other way
                //break;
            case IS_OBJECT:
                // TODO: implement this
                //break;
            default:
                obj = NULL;
                zend_throw_exception(zend_exception_get_default(TSRMLS_C), "unknown type specified in XdmValue", 0 TSRMLS_CC); 
                RETURN_NULL();
        }
        if(xdmValue == NULL) {
            RETURN_NULL();
        }
        if (object_init_ex(return_value, xdmAtomicValue_ce) != SUCCESS) {
            RETURN_NULL();
        } else {
            struct xdmAtomicValue_object* vobj = (struct xdmAtomicValue_object *)zend_object_store_get_object(return_value TSRMLS_CC);
            assert (vobj != NULL);
            vobj->xdmAtomicValue = xdmValue;
        }
    } */
	return NULL;
}


Php::Value PHP_SaxonProcessor::newXPathProcessor()
{ 
   	XPathProcessor * node = saxonProcessor->newXPathProcessor();
	PHP_XPathProcessor * php_xpathProc = new PHP_XPathProcessor(node);     
	return Php::Object("Saxon\\XPathProcessor", php_xpathProc);
          
}

Php::Value PHP_SaxonProcessor::newXsltProcessor()
{
   	XsltProcessor * node = saxonProcessor->newXsltProcessor();
	PHP_XsltProcessor * php_xsltProc = new PHP_XsltProcessor(node);     
	return Php::Object("Saxon\\XsltProcessor", php_xsltProc);
     
}

Php::Value PHP_SaxonProcessor::newXQueryProcessor()
{
   	XQueryProcessor * node = saxonProcessor->newXQueryProcessor();
	PHP_XQueryProcessor * php_xqueryProc = new PHP_XQueryProcessor(node);     
	return Php::Object("Saxon\\XQueryProcessor", php_xqueryProc);   
 }

Php::Value PHP_SaxonProcessor::newSchemaValidator()
{
   	SchemaValidator * node = saxonProcessor->newSchemaValidator();
	PHP_SchemaValidator * php_schema = new PHP_SchemaValidator(node);     
	return Php::Object("Saxon\\SchemaValidator", php_schema);   
	
}


Php::Value PHP_SaxonProcessor::version()
{
	
    return saxonProcessor->version();
 
}

void PHP_SaxonProcessor::setcwd(Php::Parameters &params)
{
	 if (!params.empty()) {
		_cwd = params[0];
		saxonProcessor->setcwd((const char *)_cwd);
	}
}

void PHP_SaxonProcessor::setConfigurationProperty(Php::Parameters &params)
{
    const char* name;
    int len1;
    const char* value;
    int len2;
    if (params.size()== 2) {
	name = params[0];
	value = params[1];
        saxonProcessor->setConfigurationProperty(name, value);
    }
    
}

void PHP_XsltProcessor::transformFileToFile(Php::Parameters &params){

    const char* infilename;
    const char* styleFileName;
    const char* outfileName;
    if (params.size()== 3) {
	infilename = params[0];
	styleFileName = params[1];
	outfileName = params[3];
 	xsltProcessor->transformFileToFile(infilename, styleFileName, outfileName);
        if(xsltProcessor->exceptionOccurred()) {
     	  // TODO: throw exception
        }
    }
}

Php::Value  PHP_XsltProcessor::transformFileToString(Php::Parameters &params){

    const char* infilename;
    const char* styleFileName;
    if (params.size()== 2) {
	infilename = params[0];
	styleFileName = params[1];
 	const char * result = xsltProcessor->transformFileToString(infilename, styleFileName);
        if(result != NULL) {
     	  return result;
        }
    }
    return NULL;
}

Php::Value  PHP_XsltProcessor::transformFileToValue(Php::Parameters &params){
    const char* infilename;
    const char* styleFileName;
    if (params.size()== 2) {
	infilename = params[0];
	styleFileName = params[1];
 	XdmValue * node = xsltProcessor->transformFileToValue(infilename, styleFileName);
        if(node != NULL) {
		PHP_XdmValue * php_xdmValue = new PHP_XdmValue(node);     
		return Php::Object("Saxon\\XdmValue", php_xdmValue);
        }
    }
    return NULL;
}
Php::Value  PHP_XsltProcessor::transformToString(Php::Parameters &params){

	if (params.size()>0) {
		throw Php::Exception("Wrong number of arguments");
	
	}


	const char * result = xsltProcessor->transformToString();
        if(result != NULL) {
            return result;
        } 
	return NULL;

}
Php::Value  PHP_XsltProcessor::transformToValue(Php::Parameters &params){


	if (params.size()>0) {
		throw Php::Exception("Wrong number of arguments");
	
	}

	XdmValue * node = xsltProcessor->transformToValue();
        if(node != NULL) {
		PHP_XdmValue * php_xdmValue = new PHP_XdmValue(node);     
		return Php::Object("Saxon\\XdmValue", php_xdmValue);
        }
	return NULL;
}
void  PHP_XsltProcessor::transformToFile(Php::Parameters &params){

	if (params.size()>0) {
		throw Php::Exception("Wrong number of arguments");
	
	}

	xsltProcessor->transformToFile();
        
}
void PHP_XsltProcessor::compileFromFile(Php::Parameters &params){
    const char* styleFileName;
    if (params.size()== 1) {

	styleFileName = params[0];
 	xsltProcessor->compileFromFile(styleFileName);

    }
}
void PHP_XsltProcessor::compileFromValue(Php::Parameters &params){
   
    if (params.size()== 1) {

	PHP_XdmValue * node = (PHP_XdmValue *)params[0].implementation();
	if(node != NULL) {
 		xsltProcessor->compileFromXdmNode((XdmNode *)node->getInternal());
	}

    }
}
void PHP_XsltProcessor::compileFromString(Php::Parameters &params){
    if (params.size()== 1) {

	const char * stylesheet = params[0];
	if(stylesheet != NULL) {
 		xsltProcessor->compileFromString(stylesheet);
	}

    }

}
void  PHP_XsltProcessor::setOutputFile(Php::Parameters &params){
if (params.size()== 1) {

	const char * outputFilename = params[0];
	if(outputFilename != NULL) {
 		xsltProcessor->setOutputFile(outputFilename);
	}

    }
}
void  PHP_XsltProcessor::setSourceFromFile(Php::Parameters &params){
	if (params.size()== 1) {

		const char * outputFilename = params[0];
		if(outputFilename != NULL) {
 			xsltProcessor->setSourceFromFile(outputFilename);
		}

   	 }
}


void  PHP_XsltProcessor::setSourceFromXdmValue(Php::Parameters &params){
	if (params.size()== 1) {

		PHP_XdmValue * node = (PHP_XdmValue*)params[0].implementation();
		if(node != NULL) {
 			xsltProcessor->setSourceFromXdmValue((XdmItem *)node->getInternal());
		}

    	}

}
void  PHP_XsltProcessor::setParameter(Php::Parameters &params){
	PHP_XdmValue * value;
	const char * name;	
	if (params.size()== 2) {
		name = params[0];
		value = (PHP_XdmValue *)params[1].implementation();
		if(name != NULL && value != NULL) {
			xsltProcessor->setParameter(name, value->getInternal());
		}	
	}
}

void  PHP_XsltProcessor::setProperty(Php::Parameters &params){
	if (params.size()== 2) {

		const char * name = params[0];
		const char * value = params[1];
		if(name != NULL && value != NULL) {
 			xsltProcessor->setProperty(name, value);
		}

   	 }
}




void  PHP_XsltProcessor::clearParameters(){
 	xsltProcessor->clearParameters(true);
}

void PHP_XsltProcessor::clearProperties(){		
 	xsltProcessor->clearProperties();
}
void  PHP_XsltProcessor::exceptionClear(){
	xsltProcessor->exceptionClear();
}
Php::Value  PHP_XsltProcessor::exceptionOccurred(){
	bool result = xsltProcessor->exceptionOccurred();
	return result;
}
Php::Value  PHP_XsltProcessor::getErrorCode(Php::Parameters &params){
	if (params.size()== 1) {
		int index = params[0];
		return xsltProcessor->getErrorCode(index);
	}
	return NULL;
}

Php::Value  PHP_XsltProcessor::getErrorMessage(Php::Parameters &params){
	if (params.size()== 1) {
		int index = params[0];
		return xsltProcessor->getErrorMessage(index);
	}
	return NULL;
}

Php::Value  PHP_XsltProcessor::getExceptionCount(){
	return xsltProcessor->exceptionCount();
}


/*     ============== XQuery10/30/31: PHP Interface of   XQueryProcessor =============== */
    void  PHP_XQueryProcessor::setQueryContent(Php::Parameters &params){
	if (params.size()== 1) {
		const char * queryStr = params[0];
		if(queryStr != NULL)
			xqueryProcessor->setProperty("qs",queryStr);
	}
	
	}
    void  PHP_XQueryProcessor::setContextItem(Php::Parameters &params){
		//TODO allow XdmNode, XdmItem and XdmValue - might not need to do this
	if (params.size()== 1) {
		Php:Value value = params[0];
		xqueryProcessor->setContextItem((XdmItem *)value.implementation());
		//value.instanceOf("Xdm");
		
	}

     }
    void  PHP_XQueryProcessor::setContextItemFromFile(Php::Parameters &params){
	const char * filename;
	if (params.size()== 1) {
		filename = params[0];
		if(filename != NULL)
			xqueryProcessor->setContextItemFromFile(filename);	
	}
	
    }

    void  PHP_XQueryProcessor::setParameter(Php::Parameters &params){
	PHP_XdmValue * value;
	const char * name;	
	if (params.size()== 2) {
		name = params[0];
		value = (PHP_XdmValue *)params[1].implementation();
		if(name != NULL && value != NULL) {
			xqueryProcessor->setParameter(name, value->getInternal());
		}	
	}
    }

    void  PHP_XQueryProcessor::setProperty(Php::Parameters &params){
	if (params.size()== 2) {

		const char * name = params[0];
		const char * value = params[1];
		if(name != NULL && value != NULL) {
 			xqueryProcessor->setProperty(name, value);
		}

   	 }
    }

    void  PHP_XQueryProcessor::setOutputFile(Php::Parameters &params){
	if (params.size()== 1) {

		const char * outfilename = params[0];
		if(outfilename != NULL) {
 			xqueryProcessor->setOutputFile(outfilename);
		}

   	 }
   }
    void  PHP_XQueryProcessor::setQueryFile(Php::Parameters &params){
	if (params.size()== 1) {

		const char * outfilename = params[0];
		if(outfilename != NULL) {
 			xqueryProcessor->setQueryFile(outfilename);
		}

   	 }
}
    void  PHP_XQueryProcessor::setQueryBaseURI(Php::Parameters &params){
	if (params.size()== 1) {

		const char * base = params[0];
		if(base != NULL) {
 			xqueryProcessor->setQueryBaseURI(base);
		}

   	 }
}

    void  PHP_XQueryProcessor::declareNamespace(Php::Parameters &params){
	if (params.size()== 2) {

		const char * prefix = params[0];
		const char * ns = params[1];
 		xqueryProcessor->declareNamespace(prefix, ns);
		
   	 }
}

    void  PHP_XQueryProcessor::clearParameters(){
	 	xqueryProcessor->clearParameters(true);
    }


    void  PHP_XQueryProcessor::clearProperties(){
	xqueryProcessor->clearProperties();
	}

    Php::Value  PHP_XQueryProcessor::runQueryToValue(){
	XdmValue * node = xqueryProcessor->runQueryToValue();
	if(node != NULL) {
		PHP_XdmValue * php_xdmValue = new PHP_XdmValue(node);     
		return Php::Object("Saxon\\XdmValue", php_xdmValue);
        }
	return NULL;
    }
    Php::Value  PHP_XQueryProcessor::runQueryToString(){
	 const char * result = xqueryProcessor->runQueryToString();
        if(result != NULL) {
		return result;
	}
    }

    Php::Value  PHP_XQueryProcessor::runQueryToFile(Php::Parameters &params){
	const char * ofilename;	
	if (params.size()== 1) {

		ofilename = params[0];

		if(ofilename != NULL) {
			xqueryProcessor->setOutputFile(ofilename);	
		}
		
	}
	xqueryProcessor->runQueryToFile();
    }


    void  PHP_XQueryProcessor::exceptionClear(){
	xqueryProcessor->exceptionClear();
}
    Php::Value  PHP_XQueryProcessor::exceptionOccurred(){
	 bool result = xqueryProcessor->exceptionOccurred();
	return result;
}
    Php::Value  PHP_XQueryProcessor::getErrorCode(Php::Parameters &params){
	if (params.size()== 1) {
		int index = params[0];
		return xqueryProcessor->getErrorCode(index);
	}
	return NULL;
}

    Php::Value  PHP_XQueryProcessor::getErrorMessage(Php::Parameters &params){
	if (params.size()== 1) {
		int index = params[0];
		return xqueryProcessor->getErrorMessage(index);
	}
	return NULL;
}

    Php::Value  PHP_XQueryProcessor::getExceptionCount(){
	int count = xqueryProcessor->exceptionCount();
	return count;
	}


	
/**
 *  tell the compiler that the get_module is a pure C function
 */
extern "C" {
    
    /**
     *  Function that is called by PHP right after the PHP process
     *  has started, and that returns an address of an internal PHP
     *  strucure with all the details and features of your extension
     *
     *  @return void*   a pointer to an address that is understood by PHP
     */
    PHPCPP_EXPORT void *get_module() 
    {
        // static(!) Php::Extension object that should stay in memory
        // for the entire duration of the process (that's why it's static)
        static Php::Extension extension(PHP_SAXON_EXTNAME, PHP_SAXON_EXTVER);
	 // description of the class so that PHP knows which methods are accessible
        Php::Class<PHP_SaxonProcessor> saxonProcessor("Saxon\\SaxonProcessor");
	saxonProcessor.method<&PHP_SaxonProcessor::__construct> ("__construct");
        saxonProcessor.method<&PHP_SaxonProcessor::createAtomicValue> ("createAtomicValue");
        saxonProcessor.method<&PHP_SaxonProcessor::parseXmlFromString> ("parseXmlFromString");
        saxonProcessor.method<&PHP_SaxonProcessor::parseXmlFromFile>     ("parseXmlFromFile");
        saxonProcessor.method<&PHP_SaxonProcessor::setcwd>     ("setcwd");
        saxonProcessor.method<&PHP_SaxonProcessor::setResourcesDirectory>     ("setResourcesDirectory");
        saxonProcessor.method<&PHP_SaxonProcessor::setConfigurationProperty>     ("setConfigurationProperty");
        saxonProcessor.method<&PHP_SaxonProcessor::newXsltProcessor>     ("newXsltProcessor");
        saxonProcessor.method<&PHP_SaxonProcessor::newXQueryProcessor>     ("newXQueryProcessor");
	saxonProcessor.method<&PHP_SaxonProcessor::newXPathProcessor>     ("newXPathProcessor");
	saxonProcessor.method<&PHP_SaxonProcessor::newSchemaValidator>     ("newSchemaValidator");
	saxonProcessor.method<&PHP_SaxonProcessor::version>     ("version");

	Php::Class<PHP_XdmValue> xdmValue("Saxon\\XdmValue");
	//saxonProcessor.method<&PHP_SaxonProcessor::__construct> ("__construct");

	Php::Class<PHP_XdmItem> xdmItem("Saxon\\XdmItem");

	Php::Class<PHP_XdmNode> xdmNode("Saxon\\XdmNode");

	Php::Class<PHP_XdmAtomicValue> xdmAtomicValue("Saxon\\XdmAtomicValue");

	xdmItem.extends(xdmValue);
	xdmNode.extends(xdmValue);
	xdmAtomicValue.extends(xdmValue);

	Php::Class<PHP_XsltProcessor> xsltProcessor("Saxon\\XsltProcessor");
	xsltProcessor.method<&PHP_XsltProcessor::transformFileToFile> ("transformFileToFile");
	xsltProcessor.method<&PHP_XsltProcessor::transformFileToString> ("transformFileToString");
	xsltProcessor.method<&PHP_XsltProcessor::transformToString> ("transformToString");
	xsltProcessor.method<&PHP_XsltProcessor::transformToValue> ("transformToValue");
	xsltProcessor.method<&PHP_XsltProcessor::transformToFile> ("transformToFile");
	xsltProcessor.method<&PHP_XsltProcessor::compileFromFile> ("compileFromFile");
	xsltProcessor.method<&PHP_XsltProcessor::compileFromValue> ("compileFromValue");
	xsltProcessor.method<&PHP_XsltProcessor::compileFromString> ("compileFromString");
	xsltProcessor.method<&PHP_XsltProcessor::setOutputFile> ("setOutputFile");
	xsltProcessor.method<&PHP_XsltProcessor::setSourceFromFile> ("setSourceFromFile");
	xsltProcessor.method<&PHP_XsltProcessor::setSourceFromXdmValue> ("setSourceFromXdmValue");
	xsltProcessor.method<&PHP_XsltProcessor::setParameter> ("setParameter");
	xsltProcessor.method<&PHP_XsltProcessor::setProperty> ("setProperty");
	xsltProcessor.method<&PHP_XsltProcessor::clearParameters> ("clearParameters");
	xsltProcessor.method<&PHP_XsltProcessor::exceptionClear> ("exceptionClear");
	xsltProcessor.method<&PHP_XsltProcessor::exceptionOccurred> ("exceptionOccurred");
	xsltProcessor.method<&PHP_XsltProcessor::clearProperties> ("clearProperties");
	xsltProcessor.method<&PHP_XsltProcessor::getErrorMessage> ("getErrorMessage");
	xsltProcessor.method<&PHP_XsltProcessor::getExceptionCount> ("getExceptionCount");

	Php::Class<PHP_XQueryProcessor> xqueryProcessor("Saxon\\XQueryProcessor");

    xqueryProcessor.method<&PHP_XQueryProcessor::setQueryContent>("setQueryContent");
    xqueryProcessor.method<&PHP_XQueryProcessor::setContextItem>("setContextItem");
    xqueryProcessor.method<&PHP_XQueryProcessor::setContextItemFromFile>("setContextItemFromFile");
    xqueryProcessor.method<&PHP_XQueryProcessor::setParameter>("setParameter");
    xqueryProcessor.method<&PHP_XQueryProcessor::setProperty>("setProperty");
    xqueryProcessor.method<&PHP_XQueryProcessor::setOutputFile>("setOutputFile");
    xqueryProcessor.method<&PHP_XQueryProcessor::setQueryFile>("setQueryFile");
    xqueryProcessor.method<&PHP_XQueryProcessor::setQueryBaseURI>("setQueryBaseURI");
    xqueryProcessor.method<&PHP_XQueryProcessor::declareNamespace>("declareNamespace");
    xqueryProcessor.method<&PHP_XQueryProcessor::clearParameters>("clearParameters");
    xqueryProcessor.method<&PHP_XQueryProcessor::clearProperties>("clearProperties");
    xqueryProcessor.method<&PHP_XQueryProcessor::runQueryToValue>("runQueryToValue");
    xqueryProcessor.method<&PHP_XQueryProcessor::runQueryToString>("runQueryToString");
    xqueryProcessor.method<&PHP_XQueryProcessor::runQueryToFile>("runQueryToFile");
    xqueryProcessor.method<&PHP_XQueryProcessor::exceptionClear>("exceptionClear");
    xqueryProcessor.method<&PHP_XQueryProcessor::exceptionOccurred>("exceptionOccurred");
    xqueryProcessor.method<&PHP_XQueryProcessor::getErrorCode>("getErrorCode");
    xqueryProcessor.method<&PHP_XQueryProcessor::getErrorMessage>("getErrorMessage");
    xqueryProcessor.method<&PHP_XQueryProcessor::getExceptionCount>("getExceptionCount");

	Php::Class<PHP_XPathProcessor> xpathProcessor("Saxon\\XPathProcessor");

	Php::Class<PHP_SchemaValidator> schemaValidator("Saxon\\SchemaValidator");

        // add the class to the extension
        extension.add(std::move(saxonProcessor));
        extension.add(std::move(xdmValue));
        extension.add(std::move(xdmItem));
        extension.add(std::move(xdmNode));
        extension.add(std::move(xdmAtomicValue));
        extension.add(std::move(xsltProcessor));
	extension.add(std::move(xqueryProcessor));
	extension.add(std::move(xpathProcessor));
	extension.add(std::move(schemaValidator));
        
        // return the extension
        return extension;
    }
}
        

