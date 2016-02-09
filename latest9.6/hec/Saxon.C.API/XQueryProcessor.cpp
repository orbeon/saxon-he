#include "XQueryProcessor.h"
#include "XdmValue.h"
#include "XdmItem.h"
#include "XdmNode.h"
#include "XdmAtomicValue.h"

    XQueryProcessor::XQueryProcessor() {
	SaxonProcessor *p = new SaxonProcessor(false);
	XQueryProcessor(p, "");
     }

    XQueryProcessor::XQueryProcessor(SaxonProcessor *p, std::string curr) {
    proc = p;

    /*
     * Look for class.
     */
     cppClass = lookForClass(proc->environ->env, "net/sf/saxon/option/cpp/XQueryEngine");


    cppXQ = createSaxonProcessor2 (proc->environ->env, cppClass, "(Lnet/sf/saxon/s9api/Processor;)V", proc->proc);
    
#ifdef DEBUG
	jmethodID debugMID = proc->environ->env->GetStaticMethodID(cppClass, "setDebugMode", "(Z)V");
	proc->environ->env->CallStaticVoidMethod(cppClass, debugMID, (jboolean)true);
#endif

    proc->exception = NULL;
    outputfile1 = "";
	if(!(proc->cwd.empty()) && curr.empty()){
		cwdXQ = proc->cwd;
	} else {
		cwdXQ = curr;
	}
        queryFileExists = false;
}


std::map<std::string,XdmValue*>& XQueryProcessor::getParameters(){
	std::map<std::string,XdmValue*>& ptr = parameters;
	return ptr;
}

std::map<std::string,std::string>& XQueryProcessor::getProperties(){
	std::map<std::string,std::string> &ptr = properties;
	return ptr;
}


    /**
     * Set the source document for the query
    */
    void XQueryProcessor::setContextItem(XdmItem * value){
    	if(value != NULL){
	 value->incrementRefCount();
     	 parameters["node"] = (XdmValue *)value;
    	}
    }


     void XQueryProcessor::declareNamespace(const char *prefix, const char * uri){
        if (prefix == NULL || uri == NULL) {
		return;
        }
	jmethodID mID =
		(jmethodID) proc->environ->env->GetMethodID(cppClass, "declareNamespace",
				"(Ljava/lang/String;Ljava/lang/String;)V");
	if (!mID) {
	cerr << "Error: MyClassInDll." << "declareNameSpace" << " not found\n"
			<< endl;

	} else {
	
			proc->environ->env->CallObjectMethod(cppXQ, mID,
					proc->environ->env->NewStringUTF(prefix),
					proc->environ->env->NewStringUTF(uri));
	}

}


    /**
     * Set the source document for the query
    */
    void XQueryProcessor::setContextItemFromFile(const char * ifile){
	setProperty("s", ifile);
    }

    /**
     * Set the output file where the result is sent
    */
    void XQueryProcessor::setOutputFile(const char* ofile){
       outputfile1 = std::string(ofile); 
       setProperty("o", ofile);
    }

    /**
     * Set a parameter value used in the query
     *
     * @param name  of the parameter, as a string
     * @param value of the query parameter, or null to clear a previously set value
     */
    void XQueryProcessor::setParameter(const char * name, XdmValue*value){
	if(value != NULL){
		value->incrementRefCount();
		parameters["param:"+string(name)] = value;
	} 
    }


    /**
     * Remove a parameter (name, value) pair
     *
     * @param namespacei currently not used
     * @param name  of the parameter
     * @return bool - outcome of the romoval
     */
    bool XQueryProcessor::removeParameter(const char * name){
	return (bool)(parameters.erase("param:"+string(name)));
    }
    /**
     * Set a property.
     *
     * @param name of the property
     * @param value of the property
     */
    void XQueryProcessor::setProperty(const char * name, const char * value){
	properties.insert(std::pair<std::string, std::string>(std::string(name), std::string(value)));
    }

    void XQueryProcessor::clearParameters(bool delVal){
	if(delVal){
       		for(std::map<std::string, XdmValue*>::iterator itr = parameters.begin(); itr != parameters.end(); itr++){
			XdmValue * value = itr->second;
			value->decrementRefCount();
#ifdef DEBUG
			cout<<"XQueryProc.clearParameter() - XdmValue refCount="<<value->getRefCount()<<endl;
#endif
			if(value != NULL && value->getRefCount() < 1){		
	        		delete value;
			}
        	}
		parameters.clear();
	}
    }

   void XQueryProcessor::clearProperties(){
	properties.clear();
        outputfile1.clear();
   }


   void XQueryProcessor::setcwd(const char* dir){
    cwdXQ = std::string(dir);
   }

    void XQueryProcessor::setQueryBaseURI(const char * baseURI){
	setProperty("base", baseURI);
    }

    void XQueryProcessor::executeQueryToFile(const char * infilename, const char * ofilename, const char * query){
	setProperty("resources", proc->getResourcesDirectory());  

	jmethodID mID = (jmethodID)proc->environ->env->GetMethodID (cppClass,"executeQueryToFile", "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)V");
 	if (!mID) {
        cout<<"Error: MyClassInDll."<<"executeQueryToFile"<<" not found\n"<<endl;
    } else {
	jobjectArray stringArray = NULL;
	jobjectArray objectArray = NULL;

	int size = parameters.size() + properties.size();
	if(query!= NULL) size++;
	if(infilename!= NULL) size++;	
	if(size >0) {

	   int i=0;
           jclass objectClass = lookForClass(proc->environ->env, "java/lang/Object");
	   jclass stringClass = lookForClass(proc->environ->env, "java/lang/String");
	   objectArray = proc->environ->env->NewObjectArray( (jint)size, objectClass, 0 );
	   stringArray = proc->environ->env->NewObjectArray( (jint)size, stringClass, 0 );
	   if(query!= NULL) {
	     proc->environ->env->SetObjectArrayElement( stringArray, i, proc->environ->env->NewStringUTF("qs") );
     	     proc->environ->env->SetObjectArrayElement( objectArray, i, proc->environ->env->NewStringUTF(query));
	     i++;	
	   }
	   if(infilename!= NULL) {
	     proc->environ->env->SetObjectArrayElement( stringArray, i, proc->environ->env->NewStringUTF("s") );
     	     proc->environ->env->SetObjectArrayElement( objectArray, i, proc->environ->env->NewStringUTF(infilename));
	     i++;	
	   }
	   for(map<std::string, XdmValue* >::iterator iter=parameters.begin(); iter!=parameters.end(); ++iter, i++) {
	     proc->environ->env->SetObjectArrayElement( stringArray, i, proc->environ->env->NewStringUTF( (iter->first).c_str() ) );
		bool checkCast = proc->environ->env->IsInstanceOf((iter->second)->getUnderlyingValue(proc), lookForClass(proc->environ->env, "net/sf/saxon/option/cpp/XdmValueForCpp") );
		if(( (bool)checkCast)==false ){
			failure = "FAILURE in  array of XdmValueForCpp";
		} 
	     proc->environ->env->SetObjectArrayElement( objectArray, i, (jobject)((iter->second)->getUnderlyingValue(proc)) );
	   }
  	   for(map<std::string, std::string >::iterator iter=properties.begin(); iter!=properties.end(); ++iter, i++) {
	     proc->environ->env->SetObjectArrayElement( stringArray, i, proc->environ->env->NewStringUTF( (iter->first).c_str()  ));
	     proc->environ->env->SetObjectArrayElement( objectArray, i, (jobject)(proc->environ->env->NewStringUTF((iter->second).c_str())) );
	   }
	}

	 proc->environ->env->CallObjectMethod(cppXQ, mID, proc->environ->env->NewStringUTF(cwdXQ.c_str()), proc->environ->env->NewStringUTF(ofilename), stringArray, objectArray );
	  proc->environ->env->DeleteLocalRef(objectArray);
	  proc->environ->env->DeleteLocalRef(stringArray);

	if(exceptionOccurred()) {
			if(proc->exception != NULL) {
				delete proc->exception;
			}		
		proc->exception = proc->checkForExceptionCPP(proc->environ->env, cppClass, NULL);	
		proc->exceptionClear();
	   		
     	}
	 
  }


   }


    XdmValue * XQueryProcessor::executeQueryToValue(const char * infilename, const char * query){
	setProperty("resources", proc->getResourcesDirectory()); 
 jmethodID mID = (jmethodID)proc->environ->env->GetMethodID (cppClass,"executeQueryToValue", "(Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)Lnet/sf/saxon/s9api/XdmValue;");
 if (!mID) {
        cout<<"Error: MyClassInDll."<<"executeQueryToValue"<<" not found\n"<<endl;
    } else {
	jobjectArray stringArray = NULL;
	jobjectArray objectArray = NULL;

	int size = parameters.size() + properties.size();
	if(query!= NULL) size++;
	if(infilename!= NULL) size++;
	if(size >0) {
	   int i=0;
           jclass objectClass = lookForClass(proc->environ->env, "java/lang/Object");
	   jclass stringClass = lookForClass(proc->environ->env, "java/lang/String");
	   objectArray = proc->environ->env->NewObjectArray( (jint)size, objectClass, 0 );
	   stringArray = proc->environ->env->NewObjectArray( (jint)size, stringClass, 0 );

	   if(query!= NULL) {
	     proc->environ->env->SetObjectArrayElement( stringArray, i, proc->environ->env->NewStringUTF("qs") );
     	     proc->environ->env->SetObjectArrayElement( objectArray, i, proc->environ->env->NewStringUTF(query));
	     i++;	
	   }
	   if(infilename!= NULL) {
	     proc->environ->env->SetObjectArrayElement( stringArray, i, proc->environ->env->NewStringUTF("s") );
     	     proc->environ->env->SetObjectArrayElement( objectArray, i, proc->environ->env->NewStringUTF(infilename));
	     i++;	
	   }
	   for(map<std::string, XdmValue* >::iterator iter=parameters.begin(); iter!=parameters.end(); ++iter, i++) {
	     proc->environ->env->SetObjectArrayElement( stringArray, i, proc->environ->env->NewStringUTF( (iter->first).c_str() ) );
		bool checkCast = proc->environ->env->IsInstanceOf((iter->second)->getUnderlyingValue(proc), lookForClass(proc->environ->env, "net/sf/saxon/option/cpp/XdmValueForCpp") );
		if(( (bool)checkCast)==false ){
			failure = "FAILURE in  array of XdmValueForCpp";
		} 
	     proc->environ->env->SetObjectArrayElement( objectArray, i, (jobject)((iter->second)->getUnderlyingValue(proc)) );
	   }
  	   for(map<std::string, std::string >::iterator iter=properties.begin(); iter!=properties.end(); ++iter, i++) {
	     proc->environ->env->SetObjectArrayElement( stringArray, i, proc->environ->env->NewStringUTF( (iter->first).c_str()  ));
	     proc->environ->env->SetObjectArrayElement( objectArray, i, (jobject)(proc->environ->env->NewStringUTF((iter->second).c_str())) );
	   }
	}

	  jobject result = (jobject)(proc->environ->env->CallObjectMethod(cppXQ, mID, proc->environ->env->NewStringUTF(cwdXQ.c_str()), stringArray, objectArray ));
	  proc->environ->env->DeleteLocalRef(objectArray);
	  proc->environ->env->DeleteLocalRef(stringArray);
    if(result) {
		jclass atomicValueClass = lookForClass(proc->environ->env, "net/sf/saxon/s9api/XdmAtomicValue");
		jclass nodeClass = lookForClass(proc->environ->env, "net/sf/saxon/s9api/XdmNode");
		jclass functionItemClass = lookForClass(proc->environ->env, "net/sf/saxon/s9api/XdmFunctionItem");
		XdmValue * xdmValue = NULL;
		if(proc->environ->env->IsInstanceOf(result, atomicValueClass)           == JNI_TRUE) {
				xdmValue = new XdmAtomicValue(result);
				

			} else if(proc->environ->env->IsInstanceOf(result, nodeClass)           == JNI_TRUE) {
				xdmValue = new XdmNode(result);

			} else if (proc->environ->env->IsInstanceOf(result, functionItemClass)           == JNI_TRUE) {
				return NULL;
			} else {
				xdmValue = new XdmValue(result);
			}
	
	xdmValue->setProcessor(proc);
	return xdmValue;
     } else if(exceptionOccurred()) {
			if(proc->exception != NULL) {
				delete proc->exception;
			}
			proc->exception = proc->checkForExceptionCPP(proc->environ->env, cppClass, NULL);
			proc->exceptionClear();
	   		
     		} 
  }
  return NULL;

}

    const char * XQueryProcessor::executeQueryToString(const char * infilename, const char * query){
	setProperty("resources", proc->getResourcesDirectory()); 
 jmethodID mID = (jmethodID)proc->environ->env->GetMethodID (cppClass,"executeQueryToString", "(Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;");
 if (!mID) {
        cout<<"Error: MyClassInDll."<<"executeQueryToString"<<" not found\n"<<endl;
    } else {
	jobjectArray stringArray = NULL;
	jobjectArray objectArray = NULL;

	int size = parameters.size() + properties.size();
	if(query!= NULL) size++;
	if(infilename!= NULL) size++;
	if(size >0) {
	   int i=0;
           jclass objectClass = lookForClass(proc->environ->env, "java/lang/Object");
	   jclass stringClass = lookForClass(proc->environ->env, "java/lang/String");
	   objectArray = proc->environ->env->NewObjectArray( (jint)size, objectClass, 0 );
	   stringArray = proc->environ->env->NewObjectArray( (jint)size, stringClass, 0 );

	   if(query!= NULL) {
	     proc->environ->env->SetObjectArrayElement( stringArray, i, proc->environ->env->NewStringUTF("qs") );
     	     proc->environ->env->SetObjectArrayElement( objectArray, i, proc->environ->env->NewStringUTF(query));
	     i++;	
	   }
	   if(infilename!= NULL) {
	     proc->environ->env->SetObjectArrayElement( stringArray, i, proc->environ->env->NewStringUTF("s") );
     	     proc->environ->env->SetObjectArrayElement( objectArray, i, proc->environ->env->NewStringUTF(infilename));
	     i++;	
	   }
	   for(map<std::string, XdmValue* >::iterator iter=parameters.begin(); iter!=parameters.end(); ++iter, i++) {
	     proc->environ->env->SetObjectArrayElement( stringArray, i, proc->environ->env->NewStringUTF( (iter->first).c_str() ) );
		bool checkCast = proc->environ->env->IsInstanceOf((iter->second)->getUnderlyingValue(proc), lookForClass(proc->environ->env, "net/sf/saxon/option/cpp/XdmValueForCpp") );
		if(( (bool)checkCast)==false ){
			failure = "FAILURE in  array of XdmValueForCpp";
		} 
	     proc->environ->env->SetObjectArrayElement( objectArray, i, (jobject)((iter->second)->getUnderlyingValue(proc)) );
	   }
  	   for(map<std::string, std::string >::iterator iter=properties.begin(); iter!=properties.end(); ++iter, i++) {
	     proc->environ->env->SetObjectArrayElement( stringArray, i, proc->environ->env->NewStringUTF( (iter->first).c_str()  ));
	     proc->environ->env->SetObjectArrayElement( objectArray, i, (jobject)(proc->environ->env->NewStringUTF((iter->second).c_str())) );
	   }
	}

	  jstring result = (jstring)(proc->environ->env->CallObjectMethod(cppXQ, mID, proc->environ->env->NewStringUTF(cwdXQ.c_str()), stringArray, objectArray ));
	  proc->environ->env->DeleteLocalRef(objectArray);
	  proc->environ->env->DeleteLocalRef(stringArray);

	  if(result) {
             const char * str = proc->environ->env->GetStringUTFChars(result, NULL);
            //return "result should be ok";            
	    return str;
	   } else if(exceptionOccurred()) {
			if(proc->exception != NULL) {
				delete proc->exception;
			}
			proc->exception = proc->checkForExceptionCPP(proc->environ->env, cppClass, NULL);
			proc->exceptionClear();
	   		
     		}
  }
  return NULL;


    }


    const char * XQueryProcessor::runQueryToString(){
	return executeQueryToString(NULL, NULL);	

    }


    XdmValue * XQueryProcessor::runQueryToValue(){
	return executeQueryToValue(NULL, NULL);
   }

    void XQueryProcessor::runQueryToFile(){
	executeQueryToFile(NULL, NULL, NULL);
   }

    void XQueryProcessor::setQueryFile(const char * ofile){
	   outputfile1 = std::string(ofile); 
	   setProperty("q", ofile);
	    queryFileExists = true;
    }

   void XQueryProcessor::setQueryContent(const char* content){
	   outputfile1 = std::string(content); 
	   setProperty("qs", content);
	    queryFileExists = false;
  }



void XQueryProcessor::exceptionClear(){
	if(proc->exception != NULL) {
		delete proc->exception;
		proc->exception = NULL;	
		proc->environ->env->ExceptionClear();
	}

   
 
}

bool XQueryProcessor::exceptionOccurred(){
	return proc->exceptionOccurred();

}


const char * XQueryProcessor::getErrorCode(int i) {
	if(proc->exception == NULL) {return NULL;}
	return proc->exception->getErrorCode(i);
}

const char * XQueryProcessor::getErrorMessage(int i ){
	if(proc->exception == NULL) {return NULL;}
	return proc->exception->getErrorMessage(i);
}

const char* XQueryProcessor::checkException(){
	/*if(proc->exception == NULL) {
		proc->exception = proc->checkForException(proc->environ->env, cppClass, cppXQ);
	}
        return proc->exception;*/
	return checkForException(*(proc->environ), cppClass, cppXQ);
}



int XQueryProcessor::exceptionCount(){
	if(proc->exception != NULL){
		return proc->exception->count();
	}
	return 0;
}
