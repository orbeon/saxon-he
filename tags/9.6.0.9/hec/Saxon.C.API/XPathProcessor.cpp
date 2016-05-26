#include "XPathProcessor.h"
#include "XdmValue.h"
#include "XdmItem.h"
#include "XdmNode.h"
#include "XdmAtomicValue.h"

XPathProcessor::XPathProcessor() {
	SaxonProcessor *p = new SaxonProcessor(false);
	XPathProcessor(p, "");
}

XPathProcessor::XPathProcessor(SaxonProcessor* p, std::string curr) {
	proc = p;

	/*
	 * Look for class.
	 */
	cppClass = lookForClass(proc->environ->env,
			"net/sf/saxon/option/cpp/XPathProcessor");
	if ((proc->proc) == NULL) {
		cerr << "Processor is NULL" << endl;
	}

	cppXP = createSaxonProcessor2(proc->environ->env, cppClass,
			"(Lnet/sf/saxon/s9api/Processor;)V", proc->proc);



#ifdef DEBUG
	jmethodID debugMID = proc->environ->env->GetStaticMethodID(cppClass, "setDebugMode", "(Z)V");
	proc->environ->env->CallStaticVoidMethod(cppClass, debugMID, (jboolean)true);
#endif    

	proc->exception = NULL;
	if(!(proc->cwd.empty()) && curr.empty()){
		cwdXP = proc->cwd;
	} else {
		cwdXP = curr;
	}

}

XdmValue * XPathProcessor::evaluate(const char * xpathStr) {
	if (xpathStr == NULL) {
		cerr << "Error:: XPath string cannot be empty or NULL" << std::endl;
	return NULL;
}
setProperty("resources", proc->getResourcesDirectory());
jmethodID mID =
		(jmethodID) proc->environ->env->GetMethodID(cppClass, "evaluate",
				"(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)[Lnet/sf/saxon/s9api/XdmValue;");
if (!mID) {
	cerr << "Error: MyClassInDll." << "evaluate" << " not found\n"
			<< endl;

} else {
	jobjectArray stringArray = NULL;
	jobjectArray objectArray = NULL;
	jclass objectClass = lookForClass(proc->environ->env, "java/lang/Object");
	jclass stringClass = lookForClass(proc->environ->env, "java/lang/String");

	int size = parameters.size() + properties.size();
	if (size > 0) {
		objectArray = proc->environ->env->NewObjectArray((jint) size,
				objectClass, 0);
		stringArray = proc->environ->env->NewObjectArray((jint) size,
				stringClass, 0);
		int i = 0;
		for (map<std::string, XdmValue*>::iterator iter = parameters.begin();
				iter != parameters.end(); ++iter, i++) {
			proc->environ->env->SetObjectArrayElement(stringArray, i,
					proc->environ->env->NewStringUTF((iter->first).c_str()));
			proc->environ->env->SetObjectArrayElement(objectArray, i,
					(iter->second)->getUnderlyingValue(proc));
		}
		for (map<std::string, std::string>::iterator iter = properties.begin();
				iter != properties.end(); ++iter, i++) {
			proc->environ->env->SetObjectArrayElement(stringArray, i,
					proc->environ->env->NewStringUTF((iter->first).c_str()));
			proc->environ->env->SetObjectArrayElement(objectArray, i,
					proc->environ->env->NewStringUTF((iter->second).c_str()));
		}
	}
	jobjectArray results = (jobjectArray)(
			proc->environ->env->CallObjectMethod(cppXP, mID,
					proc->environ->env->NewStringUTF(cwdXP.c_str()),
					proc->environ->env->NewStringUTF(xpathStr), stringArray, objectArray));
	if(!results) {
		if(exceptionOccurred()) {
			if(proc->exception != NULL) {
				delete proc->exception;
			}
			proc->exception = proc->checkForExceptionCPP(proc->environ->env, cppClass, NULL);
			proc->exceptionClear();
			return NULL;
	   		
     		}
	}
	
	int sizex = proc->environ->env->GetArrayLength(results);
	if (size > 0) {
		proc->environ->env->DeleteLocalRef(stringArray);
		proc->environ->env->DeleteLocalRef(objectArray);
	}
	if (sizex>0) {
		jclass atomicValueClass = lookForClass(proc->environ->env, "net/sf/saxon/s9api/XdmAtomicValue");
		jclass nodeClass = lookForClass(proc->environ->env, "net/sf/saxon/s9api/XdmNode");
		jclass functionItemClass = lookForClass(proc->environ->env, "net/sf/saxon/s9api/XdmFunctionItem");

		XdmValue * value = new XdmValue();
		value->setProcessor(proc);
		XdmItem * xdmItem = NULL;
		for (int p=0; p < sizex; ++p) 
		{
			jobject resulti = proc->environ->env->GetObjectArrayElement(results, p);
			//value->addUnderlyingValue(resulti);

			if(proc->environ->env->IsInstanceOf(resulti, atomicValueClass)           == JNI_TRUE) {
				xdmItem = new XdmAtomicValue(resulti);
				

			} else if(proc->environ->env->IsInstanceOf(resulti, nodeClass)           == JNI_TRUE) {
				xdmItem = new XdmNode(resulti);


			} else if (proc->environ->env->IsInstanceOf(resulti, functionItemClass)           == JNI_TRUE) {
				continue;
			}
			xdmItem->setProcessor(proc);
			value->addXdmItem(xdmItem);
		}
		proc->environ->env->DeleteLocalRef(results);
		return value;
	}
}
return NULL;

}

XdmItem * XPathProcessor::evaluateSingle(const char * xpathStr) {
	if (xpathStr == NULL) {
		cerr << "Error:: XPath string cannot be empty or NULL" << std::endl;
	     return NULL;
        }
setProperty("resources", proc->getResourcesDirectory());
jmethodID mID =
		(jmethodID) proc->environ->env->GetMethodID(cppClass, "evaluateSingle",
				"(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)Lnet/sf/saxon/s9api/XdmItem;");
if (!mID) {
	cerr << "Error: MyClassInDll." << "evaluateSingle" << " not found\n"
			<< endl;

} else {
	jobjectArray stringArray = NULL;
	jobjectArray objectArray = NULL;
	jclass objectClass = lookForClass(proc->environ->env, "java/lang/Object");
	jclass stringClass = lookForClass(proc->environ->env, "java/lang/String");

	int size = parameters.size() + properties.size();
#ifdef DEBUG
		cerr<<"Properties size: "<<properties.size()<<endl;
		cerr<<"Parameter size: "<<parameters.size()<<endl;
		cerr<<"size:"<<size<<endl;
#endif
	if (size > 0) {
		objectArray = proc->environ->env->NewObjectArray((jint) size,
				objectClass, 0);
		stringArray = proc->environ->env->NewObjectArray((jint) size,
				stringClass, 0);
		int i = 0;
		for (map<std::string, XdmValue*>::iterator iter = parameters.begin();
				iter != parameters.end(); ++iter, i++) {
			proc->environ->env->SetObjectArrayElement(stringArray, i,
					proc->environ->env->NewStringUTF((iter->first).c_str()));
			proc->environ->env->SetObjectArrayElement(objectArray, i,
					(iter->second)->getUnderlyingValue(proc));
#ifdef DEBUG
				string s1 = typeid(iter->second).name();
				cerr<<"Type of itr:"<<s1<<endl;
				jobject xx = (iter->second)->getUnderlyingValue(proc);
				if(xx == NULL) {
					cerr<<"value failed"<<endl;
				} else {

					cerr<<"Type of value:"<<(typeid(xx).name())<<endl;
				}
				if((iter->second)->getUnderlyingValue(proc) == NULL) {
					cerr<<"(iter->second)->getUnderlyingValue() is NULL"<<endl;
				}
#endif
		}
		for (map<std::string, std::string>::iterator iter = properties.begin();
				iter != properties.end(); ++iter, i++) {
			proc->environ->env->SetObjectArrayElement(stringArray, i,
					proc->environ->env->NewStringUTF((iter->first).c_str()));
			proc->environ->env->SetObjectArrayElement(objectArray, i,
					proc->environ->env->NewStringUTF((iter->second).c_str()));
		}
	}
	jobject result = (jobject)(
			proc->environ->env->CallObjectMethod(cppXP, mID,
					proc->environ->env->NewStringUTF(cwdXP.c_str()),
					proc->environ->env->NewStringUTF(xpathStr), stringArray, objectArray));
	if (size > 0) {
		proc->environ->env->DeleteLocalRef(stringArray);
		proc->environ->env->DeleteLocalRef(objectArray);
	}
	if (result) {
		jclass atomicValueClass = lookForClass(proc->environ->env, "net/sf/saxon/s9api/XdmAtomicValue");
		jclass nodeClass = lookForClass(proc->environ->env, "net/sf/saxon/s9api/XdmNode");
		jclass functionItemClass = lookForClass(proc->environ->env, "net/sf/saxon/s9api/XdmFunctionItem");
		XdmItem * xdmItem = NULL;
		if(proc->environ->env->IsInstanceOf(result, atomicValueClass)           == JNI_TRUE) {
			xdmItem = new XdmAtomicValue(result);

		} else if(proc->environ->env->IsInstanceOf(result, nodeClass)           == JNI_TRUE) {
			
			xdmItem = new XdmNode(result);

		} else if (proc->environ->env->IsInstanceOf(result, functionItemClass)           == JNI_TRUE) {
			return NULL;
		}
		xdmItem->setProcessor(proc);
		return xdmItem;
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

void XPathProcessor::setContextItem(XdmItem * item) {
	contextItem = item;
    	if(item != NULL){
     	 parameters["node"] = (XdmValue *)item;
    	}
}

void XPathProcessor::setContextFile(const char * filename) {
	if (filename != NULL) {
		setProperty("s", filename);
	}
}


//TODO test the declareNameSpace method
void XPathProcessor::declareNamespace(const char *prefix, const char * uri){
        if (prefix == NULL || uri == NULL) {
		return;
        }
	jmethodID mID =
		(jmethodID) proc->environ->env->GetMethodID(cppClass, "declareNamespace",
				"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
	if (!mID) {
	cerr << "Error: MyClassInDll." << "declareNameSpace" << " not found\n"
			<< endl;

	} else {
			proc->environ->env->CallObjectMethod(cppXP, mID,
					proc->environ->env->NewStringUTF(prefix),
					proc->environ->env->NewStringUTF(uri));
	}

}

void XPathProcessor::setBaseURI(const char * uriStr) {
	if (uriStr == NULL) {
		cerr << "Error:: XPath string cannot be empty or NULL" << std::endl;
	     return;
        }
setProperty("resources", proc->getResourcesDirectory());
jmethodID mID =
		(jmethodID) proc->environ->env->GetMethodID(cppClass, "setBaseURI",
				"(Ljava/lang/String;)Z");
if (!mID) {
	cerr << "Error: MyClassInDll." << "setBaseURI" << " not found\n"
			<< endl;

} else {

	proc->environ->env->CallObjectMethod(cppXP, mID,
					proc->environ->env->NewStringUTF(uriStr));
}

}

bool XPathProcessor::effectiveBooleanValue(const char * xpathStr) {
	if (xpathStr == NULL) {
		cerr << "Error:: XPath string cannot be empty or NULL" << std::endl;
	     return NULL;
        }
setProperty("resources", proc->getResourcesDirectory());
jmethodID mID =
		(jmethodID) proc->environ->env->GetMethodID(cppClass, "effectiveBooleanValue",
				"(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)Z");
if (!mID) {
	cerr << "Error: MyClassInDll." << "effectiveBooleanValue" << " not found\n"
			<< endl;

} else {
	jobjectArray stringArray = NULL;
	jobjectArray objectArray = NULL;
	jclass objectClass = lookForClass(proc->environ->env, "java/lang/Object");
	jclass stringClass = lookForClass(proc->environ->env, "java/lang/String");

	int size = parameters.size() + properties.size();
	if (size > 0) {
		objectArray = proc->environ->env->NewObjectArray((jint) size,
				objectClass, 0);
		stringArray = proc->environ->env->NewObjectArray((jint) size,
				stringClass, 0);
		int i = 0;
		for (map<std::string, XdmValue*>::iterator iter = parameters.begin();
				iter != parameters.end(); ++iter, i++) {
			proc->environ->env->SetObjectArrayElement(stringArray, i,
					proc->environ->env->NewStringUTF((iter->first).c_str()));
			proc->environ->env->SetObjectArrayElement(objectArray, i,
					(iter->second)->getUnderlyingValue(proc));
		}
		for (map<std::string, std::string>::iterator iter = properties.begin();
				iter != properties.end(); ++iter, i++) {
			proc->environ->env->SetObjectArrayElement(stringArray, i,
					proc->environ->env->NewStringUTF((iter->first).c_str()));
			proc->environ->env->SetObjectArrayElement(objectArray, i,
					proc->environ->env->NewStringUTF((iter->second).c_str()));
		}
	}
	jboolean result = (jboolean)(
			proc->environ->env->CallBooleanMethod(cppXP, mID,
					proc->environ->env->NewStringUTF(cwdXP.c_str()),
					proc->environ->env->NewStringUTF(xpathStr), stringArray, objectArray));
	if (size > 0) {
		proc->environ->env->DeleteLocalRef(stringArray);
		proc->environ->env->DeleteLocalRef(objectArray);
	}
	if(exceptionOccurred()) {
			if(proc->exception != NULL) {
				delete proc->exception;
			}
			proc->exception = proc->checkForExceptionCPP(proc->environ->env, cppClass, NULL);
			proc->exceptionClear();
	   		
     	}
	return result;
}
return NULL;
}

void XPathProcessor::setParameter(const char * name, XdmValue* value) {
	if(value != NULL){
		value->incrementRefCount();
		parameters["param:"+std::string(name)] = value;
	}
}

bool XPathProcessor::removeParameter(const char * name) {
	return (bool)(parameters.erase("param:"+std::string(name)));
}

void XPathProcessor::setProperty(const char * name, const char * value) {
#ifdef DEBUG	
	if(value == NULL) {
		std::cerr<<"XPathProc setProperty is NULL"<<std::endl;
	}
#endif
	properties.insert(std::pair<std::string, std::string>(std::string(name), std::string((value == NULL ? "" : value))));
}

void XPathProcessor::clearParameters(bool delVal) {
	if(delVal){
       		for(std::map<std::string, XdmValue*>::iterator itr = parameters.begin(); itr != parameters.end(); itr++){
			XdmValue * value = itr->second;
			value->decrementRefCount();
#ifdef DEBUG
			cout<<"XPathProc.clearParameter() - XdmValue refCount="<<value->getRefCount()<<endl;
#endif
			if(value != NULL && value->getRefCount() < 1){		
	        		delete value;
			}
        	}
		parameters.clear();
	}
}

void XPathProcessor::clearProperties() {
	properties.clear();
}


   void XPathProcessor::setcwd(const char* dir){
    cwdXP = std::string(dir);
   }

std::map<std::string,XdmValue*>& XPathProcessor::getParameters(){
	std::map<std::string,XdmValue*>& ptr = parameters;
	return ptr;
}

std::map<std::string,std::string>& XPathProcessor::getProperties(){
	std::map<std::string,std::string> &ptr = properties;
	return ptr;
}

void XPathProcessor::exceptionClear(){
	if(proc->exception != NULL) {
		delete proc->exception;
		proc->exception = NULL;	
	}

   proc->environ->env->ExceptionClear();
 
}

int XPathProcessor::exceptionCount(){
 if(proc->exception != NULL){
 return proc->exception->count();
 }
 return 0;
 }

const char * XPathProcessor::getErrorCode(int i) {
	if(proc->exception == NULL) {return NULL;}
	return proc->exception->getErrorCode(i);
}

const char * XPathProcessor::getErrorMessage(int i ){
	if(proc->exception == NULL) {return NULL;}
	return proc->exception->getErrorMessage(i);
}

    bool XPathProcessor::exceptionOccurred(){
	return proc->exceptionOccurred();
    }



    const char* XPathProcessor::checkException(){
	return checkForException(*(proc->environ), cppClass, cppXP);
    }

