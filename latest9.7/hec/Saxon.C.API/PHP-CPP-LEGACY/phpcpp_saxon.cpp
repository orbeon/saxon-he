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
	return new PHP_XdmNode(node);
        
    }
}

Php::Value  PHP_SaxonProcessor::parseXmlFromFile(Php::Parameters &params)
{
   
    const char * source;
    int len1;

    if (!params.empty()) {
	source = params[0];
        XdmValue* node = (XdmValue*)saxonProcessor->parseXmlFromFile(source);//TODO this needs to be XdmNode object
	PHP_XdmValue * php_xdmValue = new PHP_XdmValue(node);     
	return Php::Object("Saxon\\XdmValue", php_xdmValue);
    }
    return NULL;
}

void PHP_SaxonProcessor::registerPHPFunction(Php::Parameters &params){

 	if (!params.empty() && params.size() == 1) {
		const char * func = params[0];
	
	}
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
   
    
	return saxonProcessor->newXPathProcessor();
          
}

Php::Value PHP_SaxonProcessor::newXsltProcessor()
{
   return saxonProcessor->newXsltProcessor();
     
}

Php::Value PHP_SaxonProcessor::newXQueryProcessor()
{
   
   return saxonProcessor->newXQueryProcessor();
 }

Php::Value PHP_SaxonProcessor::newSchemaValidator()
{
   
    return saxonProcessor->newSchemaValidator();
	
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

        // add the class to the extension
        extension.add(std::move(saxonProcessor));
        extension.add(std::move(xdmValue));
        
        // return the extension
        return extension;
    }
}
        

