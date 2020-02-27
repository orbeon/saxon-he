// Xslt30Processor.cpp : Defines the exported functions for the DLL application.
//

#include "Xslt30Processor.h"
#include "XdmValue.h"
#include "XdmItem.h"
#include "XdmNode.h"
#include "XdmAtomicValue.h"
#include "XdmFunctionItem.h"
#ifdef DEBUG
#include <typeinfo> //used for testing only
#endif

Xslt30Processor::Xslt30Processor() {

	SaxonProcessor *p = new SaxonProcessor(false);
	Xslt30Processor(p, "");

}

Xslt30Processor::Xslt30Processor(SaxonProcessor * p, std::string curr) {

	proc = p;

	/*
	 * Look for class.
	 */
	cppClass = lookForClass(SaxonProcessor::sxn_environ->env,
			"net/sf/saxon/option/cpp/Xslt30Processor");

	cppXT = createSaxonProcessor2(SaxonProcessor::sxn_environ->env, cppClass,
			"(Lnet/sf/saxon/s9api/Processor;)V", proc->proc);

#ifdef DEBUG
	jmethodID debugMID = SaxonProcessor::sxn_environ->env->GetStaticMethodID(cppClass, "setDebugMode", "(Z)V");
	SaxonProcessor::sxn_environ->env->CallStaticVoidMethod(cppClass, debugMID, (jboolean)true);
    
#endif
	tunnel = false;
	jitCompilation = false;
	exception = NULL;

	if(!(proc->cwd.empty()) && curr.empty()){
		cwdXT = proc->cwd;
	} else if(!curr.empty()){
		cwdXT = curr;
	}
}

    Xslt30Processor::~Xslt30Processor(){
	    clearParameters();
	    SaxonProcessor::sxn_environ->env->DeleteLocalRef(cppXT);
	    cwdXT.erase();
    }


Xslt30Processor::Xslt30Processor(const Xslt30Processor &other) {
	proc = other.proc;
	cppClass = other.cppClass;
	cppXT = other.cppXT;
    cwdXT = other.cwdXT;
	tunnel = other.tunnel;

	std::map<std::string, XdmValue*>::const_iterator paramIter = other.parameters.begin();
    while(paramIter != other.parameters.end())
    {

       XdmValue * valuei = paramIter->second;
       if(valuei == NULL) {
    	 	//std::cerr<<"Error in Xslt30Processor copy constructor"<<std::endl;
       } else {
            parameters[paramIter->first] = new XdmValue(*(valuei));
       }
       paramIter++;
    }

	std::map<std::string, std::string>::const_iterator propIter = other.properties.begin();
	while(propIter != other.properties.end())
    {
        properties[propIter->first] = propIter->second;
        propIter++;
    }
	jitCompilation = other.jitCompilation;

}


Xslt30Processor * Xslt30Processor::clone() {
     Xslt30Processor * proc = new Xslt30Processor(*this);
     return proc;

}

bool Xslt30Processor::exceptionOccurred() {
	return proc->exceptionOccurred();
}

const char * Xslt30Processor::getErrorCode() {
 if(exception == NULL) {return NULL;}
 return exception->getErrorCode();
 }


void Xslt30Processor::setParameter(const char* name, XdmValue * value) {
	if(value != NULL && name != NULL){
		value->incrementRefCount();
		int s = parameters.size();
		std::string skey = ("sparam:"+std::string(name));
		parameters[skey] = value;
		if(s == parameters.size()) {
            std::map<std::string, XdmValue*>::iterator it;
            it = parameters.find(skey);
            if (it != parameters.end()) {
                XdmValue * valuei = it->second;
                valuei->decrementRefCount();
                if(valuei != NULL && valuei->getRefCount() < 1){
                    delete value;
                }
                parameters.erase(skey);
                parameters[skey] = value;
            }
		}
	 }
}

XdmValue* Xslt30Processor::getParameter(const char* name) {
        std::map<std::string, XdmValue*>::iterator it;
        it = parameters.find("sparam:"+std::string(name));
        if (it != parameters.end())
          return it->second;
	    return NULL;
}

bool Xslt30Processor::removeParameter(const char* name) {
	return (bool)(parameters.erase("param:"+std::string(name)));
}

void Xslt30Processor::setJustInTimeCompilation(bool jit){
    jitCompilation = jit;
}


void Xslt30Processor::clearParameters(bool delValues) {
	if(delValues){
       		for(std::map<std::string, XdmValue*>::iterator itr = parameters.begin(); itr != parameters.end(); itr++){
			
			XdmValue * value = itr->second;
			value->decrementRefCount();
#ifdef DEBUG
			std::cout<<"clearParameter() - XdmValue refCount="<<value->getRefCount()<<std::endl;
#endif
			if(value != NULL && value->getRefCount() < 1){		
	        		delete value;
			}
        	}

	} else {
for(std::map<std::string, XdmValue*>::iterator itr = parameters.begin(); itr != parameters.end(); itr++){
		
			XdmValue * value = itr->second;
			value->decrementRefCount();
		
        	}

	}
	parameters.clear();

	
}



std::map<std::string,XdmValue*>& Xslt30Processor::getParameters(){
	std::map<std::string,XdmValue*>& ptr = parameters;
	return ptr;
}


void Xslt30Processor::exceptionClear(){
 if(exception != NULL) {
 	delete exception;
 	exception = NULL;
	SaxonProcessor::sxn_environ->env->ExceptionClear();
 }
  
 }

   void Xslt30Processor::setcwd(const char* dir){
    if (dir!= NULL) {
        cwdXT = std::string(dir);
    }
   }

const char* Xslt30Processor::checkException() {
	/*if(proc->exception == NULL) {
	 proc->exception = proc->checkForException(environi, cpp);
	 }
	 return proc->exception;*/
	return proc->checkException(cppXT);
}



void Xslt30Processor::compileFromXdmNodeAndSave(XdmNode * node, const char* filename) {
	static jmethodID cAndSNodemID = NULL;

	if(cAndSNodemID == NULL) {
			cAndSNodemID = (jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"compileFromXdmNodeAndSave",
					"(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;)V");
	}
	if (!cAndSNodemID) {
		std::cerr << "Error: "<<getDllname() << ".compileFromStringAndSave"
				<< " not found\n" << std::endl;

	} else {

		
		SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, cAndSNodemID,
						SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXT.c_str()),
						node->getUnderlyingValue(), SaxonProcessor::sxn_environ->env->NewStringUTF(filename));
		
		exception = proc->checkAndCreateException(cppClass);

    }



}

    void Xslt30Processor::compileFromStringAndSave(const char* stylesheetStr, const char* filename){
	static jmethodID cAndSStringmID = NULL;
	if(cAndSStringmID == NULL) {
	   cAndSStringmID = (jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"compileFromStringAndSave",
					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
	}
	if (!cAndSStringmID) {
		std::cerr << "Error: "<<getDllname() << ".compileFromStringAndSave"
				<< " not found\n" << std::endl;

	} else {

		
		SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, cAndSStringmID,
						SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXT.c_str()),
						SaxonProcessor::sxn_environ->env->NewStringUTF(stylesheetStr),
						SaxonProcessor::sxn_environ->env->NewStringUTF(filename));
		
		exception = proc->checkAndCreateException(cppClass);

    }
}



    void Xslt30Processor::compileFromFileAndSave(const char* xslFilename, const char* filename){
	static jmethodID cAndFStringmID =  NULL;

	if (cAndFStringmID == NULL) {
	    cAndFStringmID = (jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"compileFromFileAndSave",
					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
	}
	if (!cAndFStringmID) {
		std::cerr << "Error: "<<getDllname() << ".compileFromFileAndSave"
				<< " not found\n" << std::endl;

	} else {

		
		SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, cAndFStringmID,
						SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXT.c_str()),
						SaxonProcessor::sxn_environ->env->NewStringUTF(xslFilename),SaxonProcessor::sxn_environ->env->NewStringUTF(filename));

		exception = proc->checkAndCreateException(cppClass);


     }
}

XsltExecutable * Xslt30Processor::compileFromString(const char* stylesheetStr) {
	static jmethodID cStringmID = NULL;
	if (cStringmID == NULL) {
			cStringmID = (jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"compileFromString",
					"(Ljava/lang/String;Ljava/lang/String;Z;[Ljava/lang/String;[Ljava/lang/Object;)Lnet/sf/saxon/s9api/XsltExecutable;");
	}
					
	if (!cStringmID) {
		std::cerr << "Error: "<<getDllname() << ".compileFromString"
				<< " not found\n" << std::endl;
		return NULL;

	} else {

		JParameters comboArrays;
		comboArrays = SaxonProcessor::createParameterJArray2(parameters);
		jobject executableObject = (jobject)(
				SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, cStringmID,
						SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXT.c_str()),
						SaxonProcessor::sxn_environ->env->NewStringUTF(stylesheetStr), jitCompilation, comboArrays.stringArray, comboArrays.objectArray));
		if (!executableObject) {
			exception = proc->checkAndCreateException(cppClass);
			return NULL;
		}
		if (comboArrays.stringArray != NULL) {
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.stringArray);
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.objectArray);
		}

        XsltExecutable * executable = new XsltExecutable(SaxonProcessor::sxn_environ->env->NewGlobalRef(executableObject));
        SaxonProcessor::sxn_environ->env->DeleteLocalRef(executableObject);
		return executable;
	}

}

XsltExecutable * Xslt30Processor::compileFromXdmNode(XdmNode * node) {
	static jmethodID cNodemID = NULL;
    if(cNodemID == NULL) {			
			cNodemID = (jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,"compileFromXdmNode",
			"(Ljava/lang/String;Ljava/lang/Object;Z;[Ljava/lang/String;[Ljava/lang/Object;)Lnet/sf/saxon/s9api/XsltExecutable;");
	}
	if (!cNodemID) {
		std::cerr << "Error: "<< getDllname() << ".compileFromXdmNode"
				<< " not found\n" << std::endl;
		return NULL;

	} else {
		JParameters comboArrays;
		comboArrays = SaxonProcessor::createParameterJArray2(parameters);
		jobject executableObject = (jobject)(
				SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, cNodemID,
						SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXT.c_str()),
						node->getUnderlyingValue(), jitCompilation, comboArrays.stringArray, comboArrays.objectArray));
		if (!executableObject) {
			proc->checkAndCreateException(cppClass);
			return NULL;
		}
		if (comboArrays.stringArray != NULL) {
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.stringArray);
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.objectArray);
		}

        XsltExecutable * executable = new XsltExecutable(SaxonProcessor::sxn_environ->env->NewGlobalRef(executableObject));
        SaxonProcessor::sxn_environ->env->DeleteLocalRef(executableObject);
		return executable;
	}

}

XsltExecutable * Xslt30Processor::compileFromAssociatedFile(const char* source) {
	static jmethodID cFilemID = NULL;
    if(cFilemID == NULL) {	
	    cFilemID = (jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"compileFromAssociatedFile",
					"(Ljava/lang/String;Ljava/lang/String;Z;[Ljava/lang/String;[Ljava/lang/Object;)Lnet/sf/saxon/s9api/XsltExecutable;");
	}
	if (!cFilemID) {
		std::cerr << "Error: "<<getDllname() << ".compileFromAssociatedFile"
				<< " not found\n" << std::endl;
		return NULL;

	} else {
		
		if(source == NULL) {
			std::cerr << "Error in compileFromFile method - The Stylesheet file is NULL" <<std::endl;
			return;
		}
		JParameters comboArrays;
		comboArrays = SaxonProcessor::createParameterJArray2(parameters);
		jobject executableObject = (jobject)(
				SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, cFilemID,
						SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXT.c_str()),
						SaxonProcessor::sxn_environ->env->NewStringUTF(source), jitCompilation, comboArrays.stringArray, comboArrays.objectArray));
		if (!executableObject) {
			proc->checkAndCreateException(cppClass);
     		return NULL;
		}
		if (comboArrays.stringArray != NULL) {
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.stringArray);
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.objectArray);
		}

        XsltExecutable * executable = new XsltExecutable(SaxonProcessor::sxn_environ->env->NewGlobalRef(executableObject));
        SaxonProcessor::sxn_environ->env->DeleteLocalRef(executableObject);
		return executable;
	}

}


XsltExecutable * Xslt30Processor::compileFromFile(const char* stylesheet) {
	static jmethodID cFilemID = NULL;
	if(cFilemID == NULL) {
	    cFilemID = (jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"compileFromFile",
					"(Ljava/lang/String;Ljava/lang/String;Z;[Ljava/lang/String;[Ljava/lang/Object;)Lnet/sf/saxon/s9api/XsltExecutable;");
	}
	if (!cFilemID) {
		std::cerr << "Error: "<<getDllname() << ".compileFromFile"
				<< " not found\n" << std::endl;
		return NULL;

	} else {
		
		if(stylesheet == NULL) {
			std::cerr << "Error in compileFromFile method - The Stylesheet file is NULL" <<std::endl;
			return;
		}
		JParameters comboArrays;
		comboArrays = SaxonProcessor::createParameterJArray2(parameters);
		jobject executableObject = (jobject)(
				SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, cFilemID,
						SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXT.c_str()),
						SaxonProcessor::sxn_environ->env->NewStringUTF(stylesheet), jitCompilation, comboArrays.stringArray, comboArrays.objectArray));
		if (!executableObject) {
			proc->checkAndCreateException(cppClass);
			return NULL;
     		
		}
		if (comboArrays.stringArray != NULL) {
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.stringArray);
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.objectArray);
		}

        XsltExecutable * executable = new XsltExecutable(SaxonProcessor::sxn_environ->env->NewGlobalRef(executableObject));
        SaxonProcessor::sxn_environ->env->DeleteLocalRef(executableObject);
		return executable;
	}

}




XdmValue * Xslt30Processor::transformFileToValue(const char* sourcefile,
		const char* stylesheetfile) {

	if(exceptionOccurred()) {
		//Possible error detected in the compile phase. Processor not in a clean state.
		//Require clearing exception.
		return NULL;	
	}

	if(sourcefile == NULL && stylesheetfile == NULL){
	
		return NULL;
	}

	setProperty("resources", proc->getResourcesDirectory());
	static jmethodID mtvID = NULL;

	if(mtvID == NULL) {
			mtvID = (jmethodID) SaxonProcessor::sxn_environ->env->GetStaticMethodID(cppClass,
					"transformToValue",
					"(Ljava/lang/String;Lnet/sf/saxon/option/cpp/Xslt30Processor;Lnet/sf/saxon/s9api/XsltExecutable;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)Lnet/sf/saxon/s9api/XdmValue;");
	}
	if (!mtvID) {
		std::cerr << "Error: "<< getDllname() << ".transformtoValue" << " not found\n"
				<< std::endl;

	} else {
	    JParameters comboArrays;
		comboArrays = SaxonProcessor::createParameterJArray2(parameters);

		jobject result = (jobject)(
				SaxonProcessor::sxn_environ->env->CallStaticObjectMethod(cppClass, mtvID,
						SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXT.c_str()), cppXT, NULL,
						(sourcefile != NULL ?SaxonProcessor::sxn_environ->env->NewStringUTF(sourcefile) : NULL),
						(stylesheetfile != NULL ?
								SaxonProcessor::sxn_environ->env->NewStringUTF(
										stylesheetfile) :
								NULL), comboArrays.stringArray, comboArrays.objectArray));
		if (comboArrays.stringArray != NULL) {
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.stringArray);
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.objectArray);
		}
		if (result) {
			jclass atomicValueClass = lookForClass(SaxonProcessor::sxn_environ->env, "net/sf/saxon/s9api/XdmAtomicValue");
          		jclass nodeClass = lookForClass(SaxonProcessor::sxn_environ->env, "net/sf/saxon/s9api/XdmNode");
          		jclass functionItemClass = lookForClass(SaxonProcessor::sxn_environ->env, "net/sf/saxon/s9api/XdmFunctionItem");
			XdmValue * value = NULL;
          		XdmItem * xdmItem = NULL;


          			if(SaxonProcessor::sxn_environ->env->IsInstanceOf(result, atomicValueClass)           == JNI_TRUE) {
          				xdmItem = new XdmAtomicValue(SaxonProcessor::sxn_environ->env->NewGlobalRef(result));
                        xdmItem->setProcessor(proc);
                        SaxonProcessor::sxn_environ->env->DeleteLocalRef(result);
                        return xdmItem;

          			} else if(SaxonProcessor::sxn_environ->env->IsInstanceOf(result, nodeClass)           == JNI_TRUE) {
          				xdmItem = new XdmNode(SaxonProcessor::sxn_environ->env->NewGlobalRef(result));
                        xdmItem->setProcessor(proc);
                        SaxonProcessor::sxn_environ->env->DeleteLocalRef(result);
                        return xdmItem;
#if CVERSION_API_NO >= 123
          			} else if (SaxonProcessor::sxn_environ->env->IsInstanceOf(result, functionItemClass)           == JNI_TRUE) {
                        xdmItem =  new XdmFunctionItem(SaxonProcessor::sxn_environ->env->NewGlobalRef(result));
                        xdmItem->setProcessor(proc);
                        SaxonProcessor::sxn_environ->env->DeleteLocalRef(result);
                        return xdmItem;
#endif
          			} else {
					value = new XdmValue(result, true);
					value->setProcessor(proc);
					for(int z=0;z<value->size();z++) {
						value->itemAt(z)->setProcessor(proc);
					}
					SaxonProcessor::sxn_environ->env->DeleteLocalRef(result);
					return value;
				}
				value = new XdmValue();
				value->setProcessor(proc);
          	    value->addUnderlyingValue(result);

          		SaxonProcessor::sxn_environ->env->DeleteLocalRef(result);
          		return value;
		}else {
	
			exception = proc->checkAndCreateException(cppClass);
	   		
     		}
	}
	return NULL;

}


void Xslt30Processor::transformFileToFile(const char* source,
		const char* stylesheet, const char* outputfile) {

	if(exceptionOccurred()) {
		//Possible error detected in the compile phase. Processor not in a clean state.
		//Require clearing exception.
		return;	
	}
	if(stylesheet==NULL){
		std::cerr<< "Error: stylesheet has not been set."<<std::endl;
		return;
	}
	//setProperty("resources", proc->getResourcesDirectory());
	static jmethodID mtfID = NULL;

	if(mtfID == NULL) {
		mtfID = (jmethodID) SaxonProcessor::sxn_environ->env->GetStaticMethodID(cppClass,
					"transformToFile",
					"(Ljava/lang/String;Lnet/sf/saxon/option/cpp/Xslt30Processor;Lnet/sf/saxon/s9api/XsltExecutable;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)V");
	}
	if (!mtfID) {
		std::cerr << "Error: "<<getDllname() << "transformToFile" << " not found\n"
				<< std::endl;

	} else {
	    JParameters comboArrays;
        comboArrays = SaxonProcessor::createParameterJArray2(parameters);

		SaxonProcessor::sxn_environ->env->CallStaticObjectMethod(cppClass, mtfID,
								SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXT.c_str()), cppXT, NULL,
								(source != NULL ? SaxonProcessor::sxn_environ->env->NewStringUTF(source) : NULL),
								SaxonProcessor::sxn_environ->env->NewStringUTF(stylesheet),	(outputfile != NULL ? SaxonProcessor::sxn_environ->env->NewStringUTF(outputfile) :NULL),
								comboArrays.stringArray, comboArrays.objectArray);
		if (comboArrays.stringArray!= NULL) {
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.stringArray);
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.objectArray);
		}
		}
		exception = proc->checkAndCreateException(cppClass);
	}


const char * Xslt30Processor::transformFileToString(const char* source,
		const char* stylesheet) {

	if(exceptionOccurred()) {
		//Possible error detected in the compile phase. Processor not in a clean state.
		//Require clearing exception.
		return NULL;	
	}
	if(source == NULL && stylesheet == NULL){
		std::cerr<< "Error: NULL file name found in transformFiletoString."<<std::endl;
		return NULL;
	}
	setProperty("resources", proc->getResourcesDirectory());
	static jmethodID mtsID =  NULL;

	if(mtsID == NULL) {
			mtsID = (jmethodID) SaxonProcessor::sxn_environ->env->GetStaticMethodID(cppClass,
					"transformToString",
					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;");
	}
	if (!mtsID) {
		std::cerr << "Error: "<<getDllname() << "transformFileToString" << " not found\n"
				<< std::endl;

	} else {
    JParameters comboArrays;
    comboArrays = SaxonProcessor::createParameterJArray2(parameters);

	jstring result = NULL;
	jobject obj = SaxonProcessor::sxn_environ->env->CallStaticObjectMethod(cppClass, mtsID,
								SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXT.c_str()), cppXT, NULL,
						(source != NULL ? SaxonProcessor::sxn_environ->env->NewStringUTF(
												source) : NULL),
								(stylesheet != NULL ? SaxonProcessor::sxn_environ->env->NewStringUTF(stylesheet) : NULL),
								comboArrays.stringArray, comboArrays.objectArray);
    if (comboArrays.stringArray!= NULL) {
        SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.stringArray);
        SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.objectArray);
    }
	if(obj) {
			result = (jstring)obj;
	}

	if (result) {
			const char * str = SaxonProcessor::sxn_environ->env->GetStringUTFChars(result,
					NULL);
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(obj);
			return str;
		} else  {
			exception =proc->checkAndCreateException(cppClass);
	   		
     		}
	}
	return NULL;
}



const char * Xslt30Processor::getErrorMessage(){
 	if(exception == NULL) {return NULL;}
 	return exception->getErrorMessage();
 }

