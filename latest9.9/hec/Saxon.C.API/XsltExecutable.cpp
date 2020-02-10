// XsltExecutable.cpp : Defines the exported functions for the DLL application.
//

#include "XsltExecutable.h"
#include "XdmValue.h"
#include "XdmItem.h"
#include "XdmNode.h"
#include "XdmAtomicValue.h"
#ifdef DEBUG
#include <typeinfo> //used for testing only
#endif


XsltExecutable::XsltExecutable(std::string curr, jobject executableObject) {



	/*
	 * Look for class.
	 */
	cppClass = lookForClass(SaxonProcessor::sxn_environ->env,
			"net/sf/saxon/option/cpp/Xslt30Processor");
			
#ifdef DEBUG
	jmethodID debugMID = SaxonProcessor::sxn_environ->env->GetStaticMethodID(cppClass, "setDebugMode", "(Z)V");
	SaxonProcessor::sxn_environ->env->CallStaticVoidMethod(cppClass, debugMID, (jboolean)true);

#endif
	tunnel = false;
	selection = NULL;
	selectionV=NULL;

    cwdXE = curr;
}

     XsltExecutable::~XsltExecutable(){
	clearProperties();
	clearParameters();
	if(selectionV != NULL) {
	  selectionV->decrementRefCount();
	  if(selectionV->getRefCount() == 0) {
		delete selectionV;
	  }
	}
	SaxonProcessor::sxn_environ->env->DeleteGlobalRef(executableObject);

     }


XsltExecutable::XsltExecutable(const XsltExecutable &other) {

	/*
	 * Look for class.
	 */
	cppClass = lookForClass(SaxonProcessor::sxn_environ->env,
			"net/sf/saxon/option/cpp/Xslt30Processor");
			
	executableObject = other.executableObject;
	selectionV = other.selectionV;
	if(selectionV != NULL) {
	    setInitialMatchSelection(other.selectionV);
	} else {
	    selection = other.selection;
	}
	tunnel = other.tunnel;

	std::map<std::string, XdmValue*>::const_iterator paramIter = other.parameters.begin();
    while(paramIter != other.parameters.end())
    {

       XdmValue * valuei = paramIter->second;
       if(valuei == NULL) {
    	 	cerr<<"Error in XsltExecutable copy constructor"<<endl;
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


bool XsltExecutable::exceptionOccurred() {
	return proc->exceptionOccurred();   //TODO change to static call
}

const char * XsltExecutable::getErrorCode(int i) {
 if(proc->exception == NULL) {return NULL;}
 return proc->exception->getErrorCode(i);
 }



void XsltExecutable::setGlobalContextItem(XdmItem * value){
    if(value != NULL){
      value->incrementRefCount();
      parameters["node"] = value;
    }
}

void XsltExecutable::setGlobalContextFromFile(const char * ifile) {
	if(ifile != NULL) {
		setProperty("s", ifile);
	}
}

void XsltExecutable::setInitialMatchSelection(XdmValue * _selection){
     if(_selection != NULL) {
      _selection->incrementRefCount();
      selectionV = _selection;
      selection = _selection->getUnderlyingValue();
    }
}


void XsltExecutable::setInitialMatchSelectionAsFile(const char * filename){
    if(filename != NULL) {
      selection = SaxonProcessor::sxn_environ->env->NewStringUTF(filename);
    }
}

void XsltExecutable::setOutputFile(const char * ofile) {
	setProperty("o", ofile);
}

void XsltExecutable::setBaseOutputURI(const char * baseURI) {
	if(baseURI != NULL) {
  	    setProperty("baseoutput", baseURI);
	}
}


void XsltExecutable::setParameter(const char* name, XdmValue * value, bool _static) {
	if(value != NULL && name != NULL){
		value->incrementRefCount();
		int s = parameter.size();
		std::String skey = (_static ? "sparam:"+std::string(name) : "param:"+std::string(name));
		parameters[skey] = value;
		if(s == parameter.size()) {
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

    void XsltExecutable::setInitialTemplateParameters(std::map<std::string,XdmValue*> _itparameters, bool _tunnel){
	for(std::map<std::string, XdmValue*>::iterator itr = _itparameters.begin(); itr != _itparameters.end(); itr++){
		parameters["itparam:"+std::string(itr->first)] = itr->second;
	}
	tunnel = _tunnel;
	if(tunnel) {
		setProperty("tunnel", "true");
    	}
   }

XdmValue* XsltExecutable::getParameter(const char* name) {
        std::map<std::string, XdmValue*>::iterator it;
        it = parameters.find("param:"+std::string(name));
        if (it != parameters.end())
          return it->second;
        else {
          it = parameters.find("sparam:"+std::string(name));
        if (it != parameters.end())
	  return it->second;
	  }
	return NULL;
}

bool XsltExecutable::removeParameter(const char* name) {
	return (bool)(parameters.erase("param:"+std::string(name)));
}

void XsltExecutable::setJustInTimeCompilation(bool jit){
    jitCompilation = jit;
}

void XsltExecutable::setResultAsRawValue(bool option) {
	if(option) {
		setProperty("outvalue", "yes");
	}
 }

void XsltExecutable::setProperty(const char* name, const char* value) {
	if(name != NULL) {
	    int s = properties.size();
		std:string skey = std::string(name);
		properties.insert(std::pair<std::string, std::string>(skey, std::string((value == NULL ? "" : value))));

		if(s == properties.size()) {
            std::map<std::string, std::string>::iterator it;
            it = properties.find(skey);
            if (it != properties.end()) {
                properties.erase(skey);
                properties[skey] = std::string((value == NULL ? "" : value));
            }
		}
	}
}

const char* XsltExecutable::getProperty(const char* name) {
        std::map<std::string, std::string>::iterator it;
        it = properties.find(std::string(name));
        if (it != properties.end())
          return it->second.c_str();
	return NULL;
}

void XsltExecutable::clearParameters(bool delValues) {
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

		SaxonProcessor::sxn_environ->env->DeleteLocalRef(selection);
		selection = NULL;
	} else {
for(std::map<std::string, XdmValue*>::iterator itr = parameters.begin(); itr != parameters.end(); itr++){

			XdmValue * value = itr->second;
			value->decrementRefCount();

        	}

	selection = NULL;
	}
	parameters.clear();


}

void XsltExecutable::clearProperties() {
	properties.clear();

}



std::map<std::string,XdmValue*>& XsltExecutable::getParameters(){
	std::map<std::string,XdmValue*>& ptr = parameters;
	return ptr;
}

std::map<std::string,std::string>& XsltExecutable::getProperties(){
	std::map<std::string,std::string> &ptr = properties;
	return ptr;
}

void XsltExecutable::exceptionClear(){
 if(proc->exception != NULL) {
 	delete proc->exception;
 	proc->exception = NULL;
	SaxonProcessor::sxn_environ->env->ExceptionClear();
 }

 }

   void XsltExecutable::setcwd(const char* dir){
    if (dir!= NULL) {
        cwdXE = std::string(dir);
    }
   }

const char* XsltExecutable::checkException() {
	/*if(proc->exception == NULL) {
	 proc->exception = proc->checkForException(environi, cpp);
	 }
	 return proc->exception;*/
	return proc->checkException(cppXT);
}

int XsltExecutable::exceptionCount(){
 if(proc->exception != NULL){
 return proc->exception->count();
 }
 return 0;
 }


    void XsltExecutable::export(const char * filename) {
        jclass cppClass = lookForClass(SaxonProcessor::sxn_environ->env,
        			"net/sf/saxon/option/cpp/Xslt30Processor");

        static jmethodID exportmID == NULL;

        if(!exportmID) {
            exportmID = SaxonProcessor::sxn_environ->env->GetStaticMethodID(cppClass, "save",
                                     "(Ljava/lang/String;Lnet/sf/saxon/s9api/XsltExecutable;Ljava/lang/String;)V");
        }

        if(filename == NULL) {
            std::cerr<< "Error: Error: export file name is NULL"<<std::endl;
            return;
        }
        SaxonProcessor::sxn_environ->env->CallStaticVoidMethod(cppClass, exportmID, SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXE.c_str()),
                                     executableObject, SaxonProcessor::sxn_environ->env->NewStringUTF(filename));

    }

    void XsltExecutable::applyTemplatesReturningFile(const char * stylesheetfile, const char* output_filename){

	if(selection == NULL) {
	   std::cerr<< "Error: The initial match selection has not been set. Please set it using setInitialMatchSelection or setInitialMatchSelectionFile."<<std::endl;
       		return;
	}


	setProperty("resources", proc->getResourcesDirectory());
	static jmethodID atmID = NULL;

	if(atmID == NULL) {
			(jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"applyTemplatesReturningFile",
					"(Ljava/lang/String;Lnet/sf/saxon/s9api/XsltExecutable;Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)V");

	}

	if (!atmID) {
		std::cerr << "Error: "<< getDllname() << "applyTemplatesAsFile" << " not found\n"
				<< std::endl;

	} else {
        JParameters comboArrays;
		comboArrays = SaxonProcessor::createParameterJArray(parameters, properties);
		SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, atmID,
						SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXE.c_str()), executableObject ,selection,
						(output_filename != NULL ? SaxonProcessor::sxn_environ->env->NewStringUTF(
                                       						output_filename) : NULL),
                                comboArrays.stringArray, comboArrays.objectArray);
		if (comboArrays.stringArray != NULL) {
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.stringArray);
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.objectArray);
		}
		proc->checkAndCreateException(cppClass);

	}
	return;

}

const char* XsltExecutable::applyTemplatesReturningString(){

	if(selection == NULL) {
	   std::cerr<< "Error: The initial match selection has not been set. Please set it using setInitialMatchSelection or setInitialMatchSelectionFile."<<std::endl;
       		return NULL;
	}

	setProperty("resources", proc->getResourcesDirectory());
	jmethodID atsmID =
			(jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"applyTemplatesReturningString",
					"(Ljava/lang/String;Lnet/sf/saxon/s9api/XsltExecutable;Ljava/lang/Object;[Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;");
	if (!atsmID) {
		std::cerr << "Error: "<<getDllname() << "applyTemplatesAsString" << " not found\n"
				<< std::endl;

	} else {
	    JParameters comboArrays;
		comboArrays = SaxonProcessor::createParameterJArray(parameters, properties);

	jstring result = NULL;
	jobject obj = (SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, atsmID,
								SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXE.c_str()), executableObject
								selection,
								comboArrays.stringArray, comboArrays.objectArray));

		if(obj) {
			result = (jstring)obj;
		}
		if (comboArrays.stringArray != NULL) {
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.stringArray);
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.objectArray);
		}
		if (result) {
			const char * str = SaxonProcessor::sxn_environ->env->GetStringUTFChars(result,
					NULL);
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(obj);
			return str;
		} else  {
			proc->checkAndCreateException(cppClass);

     		}
	}
	return NULL;


}

XdmValue * XsltExecutable::applyTemplatesReturningValue(){

	if(selection == NULL) {
	   std::cerr<< "Error: The initial match selection has not been set. Please set it using setInitialMatchSelection or setInitialMatchSelectionFile."<<std::endl;
       		return NULL;
	}

	setProperty("resources", proc->getResourcesDirectory());
	static jmethodID atsmID = NULL;
	if (atsmID == NULL) {
	    atsmID = (jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"applyTemplatesReturningValue",
					"(Ljava/lang/String;Lnet/sf/saxon/s9api/XsltExecutable;Ljava/lang/Object;[Ljava/lang/String;[Ljava/lang/Object;)Lnet/sf/saxon/s9api/XdmValue;");
	}
	if (!atsmID) {
		std::cerr << "Error: "<<getDllname() << "applyTemplatesAsValue" << " not found\n"
				<< std::endl;

	} else {
	    JParameters comboArrays;
		comboArrays = SaxonProcessor::createParameterJArray(parameters, properties);


	   // jstring result = NULL;
	    jobject result = (jobject)(SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, atsmID,
								SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXE.c_str()),
								executableObject, selection,
								comboArrays.stringArray, comboArrays.objectArray));
		/*if(obj) {
			result = (jobject)obj;
		}*/
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
				xdmItem =  new XdmAtomicValue(result);
				xdmItem->setProcessor(proc);
				SaxonProcessor::sxn_environ->env->DeleteLocalRef(result);
				return xdmItem;

			} else if(SaxonProcessor::sxn_environ->env->IsInstanceOf(result, nodeClass)           == JNI_TRUE) {
				xdmItem =  new XdmNode(result);
				xdmItem->setProcessor(proc);
				SaxonProcessor::sxn_environ->env->DeleteLocalRef(result);
				return xdmItem;
			} else if (SaxonProcessor::sxn_environ->env->IsInstanceOf(result, functionItemClass)           == JNI_TRUE) {
                xdmItem =  new XdmFunctionItem(result);
                xdmItem->setProcessor(proc);
                SaxonProcessor::sxn_environ->env->DeleteLocalRef(result);
                return xdmItem;
			} else {
				value = new XdmValue(result, true);
				value->setProcessor(proc);
				for(int z=0;z<value->size();z++) {
					value->itemAt(z)->setProcessor(proc);
				}
				SaxonProcessor::sxn_environ->env->DeleteLocalRef(result);
				return value;
			}
		} else  {
			proc->checkAndCreateException(cppClass);

     		}
	}
	return NULL;

}



    void XsltExecutable::callFunctionReturningFile(const char* functionName, XdmValue ** arguments, int argument_length, const char* outfile){        

        	setProperty("resources", proc->getResourcesDirectory());
        	static jmethodID afmID = NULL;

        	if(afmID == NULL) {
        			afmID = (jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
        					"callFunctionReturningFile",
        					"(Ljava/lang/String;Lnet/sf/saxon/s9api/XsltExecutable;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;[Ljava/lang/String;[Ljava/lang/Object;)V");
        	}

        	if (!afmID) {
        		std::cerr << "Error: "<< getDllname() << "callFunctionReturningFile" << " not found\n"
        				<< std::endl;
                 return;
        	} else {
                JParameters comboArrays;
        		comboArrays = SaxonProcessor::createParameterJArray(parameters, properties);

        		jobjectArray argumentJArray = SaxonProcessor::createJArray(arguments, argument_length);

        		SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, afmID,
        						SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXE.c_str()),
        						executableObject,
        						(functionName != NULL ?
        								SaxonProcessor::sxn_environ->env->NewStringUTF(functionName) :
        								NULL), argumentJArray,
        								(outfile != NULL ?
                                        			SaxonProcessor::sxn_environ->env->NewStringUTF(outfile) :
                             					NULL),
        								comboArrays.stringArray, comboArrays.objectArray);
        		if (comboArrays.stringArray != NULL) {
        			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.stringArray);
        			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.objectArray);
        		}
        		if(argumentJArray != NULL) {
        		    SaxonProcessor::sxn_environ->env->DeleteLocalRef(argumentJArray);
        		}
        		proc->checkAndCreateException(cppClass);

        	}
        	return;




    }

    const char * XsltExecutable::callFunctionReturningString(const char* functionName, XdmValue ** arguments, int argument_length){

    	setProperty("resources", proc->getResourcesDirectory());
    	static jmethodID afsmID =
    			(jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
    					"callFunctionReturningString",
    					"(Ljava/lang/String;Lnet/sf/saxon/s9api/XsltExecutable;Ljava/lang/String;[Ljava/lang/Object;[Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;");
    	if (!afsmID) {
    		std::cerr << "Error: "<<getDllname() << "callFunctionReturningString" << " not found\n"
    				<< std::endl;

    	} else {
    	    JParameters comboArrays;
    		comboArrays = SaxonProcessor::createParameterJArray(parameters, properties);
            jobjectArray argumentJArray = SaxonProcessor::createJArray(arguments, argument_length);

    	jstring result = NULL;
    	jobject obj = (SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, afsmID,
    								SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXE.c_str()),  executableObject
    								(functionName != NULL ? SaxonProcessor::sxn_environ->env->NewStringUTF(functionName) : NULL),
    								argumentJArray, comboArrays.stringArray, comboArrays.objectArray));
    		if(obj) {
    			result = (jstring)obj;
    		}
    		if (comboArrays.stringArray != NULL) {
    			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.stringArray);
    			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.objectArray);
    		}
    		if (result) {
    			const char * str = SaxonProcessor::sxn_environ->env->GetStringUTFChars(result,
    					NULL);
    			SaxonProcessor::sxn_environ->env->DeleteLocalRef(obj);
    			return str;
    		} else  {
    			proc->checkAndCreateException(cppClass);

         		}
    	}
    	return NULL;
    }



    XdmValue * XsltExecutable::callFunctionReturningValue(const char* functionName, XdmValue ** arguments, int argument_length){

          	setProperty("resources", proc->getResourcesDirectory());
          	static jmethodID cfvmID = NULL;
          	if(cfvmID == NULL) {
          	    cfvmId = (jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
          					"callFunctionReturningValue",
          					"(Ljava/lang/String;Lnet/sf/saxon/s9api/XsltExecutable;Ljava/lang/String;[Ljava/lang/Object;[Ljava/lang/String;[Ljava/lang/Object;)Lnet/sf/saxon/s9api/XdmValue;");
          	}
          	if (!cfvmID) {
          		std::cerr << "Error: "<<getDllname() << "callFunctionReturningValue" << " not found\n"
          				<< std::endl;

          	} else {
          	    JParameters comboArrays;
          		comboArrays = SaxonProcessor::createParameterJArray(parameters, properties);
                jobjectArray argumentJArray = SaxonProcessor::createJArray(arguments, argument_length);

          	    jobject result = (jobject)(SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, cfvmID,
          								SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXE.c_str()),executableObject,
                                        (functionName != NULL ? SaxonProcessor::sxn_environ->env->NewStringUTF(functionName) : NULL),
          								argumentJArray, comboArrays.stringArray, comboArrays.objectArray));

          		if (comboArrays.stringArray != NULL) {
          			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.stringArray);
          			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.objectArray);
          		}
			if(argumentJArray != NULL) {
          			SaxonProcessor::sxn_environ->env->DeleteLocalRef(argumentJArray);
			}
                  if (result) {
          		jclass atomicValueClass = lookForClass(SaxonProcessor::sxn_environ->env, "net/sf/saxon/s9api/XdmAtomicValue");
          		jclass nodeClass = lookForClass(SaxonProcessor::sxn_environ->env, "net/sf/saxon/s9api/XdmNode");
          		jclass functionItemClass = lookForClass(SaxonProcessor::sxn_environ->env, "net/sf/saxon/s9api/XdmFunctionItem");
                 	XdmValue * value = NULL;
          		XdmItem * xdmItem = NULL;


          			if(SaxonProcessor::sxn_environ->env->IsInstanceOf(result, atomicValueClass)           == JNI_TRUE) {
					    xdmItem = new XdmAtomicValue(result);
					    xdmItem->setProcessor(proc);
					    SaxonProcessor::sxn_environ->env->DeleteLocalRef(result);
					    return xdmItem;
          			} else if(SaxonProcessor::sxn_environ->env->IsInstanceOf(result, nodeClass)           == JNI_TRUE) {
	  				    xdmItem = new XdmNode(result);
					    xdmItem->setProcessor(proc);
					    SaxonProcessor::sxn_environ->env->DeleteLocalRef(result);
					    return xdmItem;

          			} else if (SaxonProcessor::sxn_environ->env->IsInstanceOf(result, functionItemClass)           == JNI_TRUE) {
          				 xdmItem =  new XdmFunctionItem(result);
                         xdmItem->setProcessor(proc);
                         SaxonProcessor::sxn_environ->env->DeleteLocalRef(result);
                         return xdmItem;
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
          	} else  {
          		proc->checkAndCreateException(cppClass);

               	}
          }
          return NULL;

    }


    void XsltExecutable::callTemplateReturningFile(const char* templateName, const char* outfile){

	if(stylesheetfile == NULL && !stylesheetObject){
		std::cerr<< "Error: No stylesheet found. Please compile stylesheet before calling callFunctionReturningFile or check exceptions"<<std::endl;
		return;
	}

	setProperty("resources", proc->getResourcesDirectory());
	static jmethodID ctmID =
			(jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"callTemplateReturningFile",
					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)V");
	if (!ctmID) {
		std::cerr << "Error: "<< getDllname() << "callTemplateReturningFile" << " not found\n"
				<< std::endl;

	} else {
        JParameters comboArrays;
		comboArrays = SaxonProcessor::createParameterJArray(parameters, properties);
		SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, ctmID,
						SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXE.c_str()),
						(stylesheetfile != NULL ?
								SaxonProcessor::sxn_environ->env->NewStringUTF(
										stylesheetfile) :
								NULL),
						(templateName != NULL ?
								SaxonProcessor::sxn_environ->env->NewStringUTF(templateName) :
								NULL),
								(outfile != NULL ?
                                			SaxonProcessor::sxn_environ->env->NewStringUTF(outfile) :
                     					NULL),
								comboArrays.stringArray, comboArrays.objectArray);
		if (comboArrays.stringArray != NULL) {
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.stringArray);
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.objectArray);
		}
		proc->checkAndCreateException(cppClass);

	}
	return;


    }




    const char* XsltExecutable::callTemplateReturningString(const char * stylesheet, const char* templateName){

	setProperty("resources", proc->getResourcesDirectory());
	jmethodID ctsmID =
			(jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"callTemplateReturningString",
					"(Ljava/lang/String;Lnet/sf/saxon/s9api/XsltExecutable;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;");
	if (!ctsmID) {
		std::cerr << "Error: "<<getDllname() << "callTemplateReturningString" << " not found\n"
				<< std::endl;

	} else {
	    JParameters comboArrays;
		comboArrays = SaxonProcessor::createParameterJArray(parameters, properties);


	jstring result = NULL;
	jobject obj =(jobject)(SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, ctsmID,
								SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXE.c_str()), executableObject,
								(templateName != NULL ? SaxonProcessor::sxn_environ->env->NewStringUTF(templateName) : NULL),
								comboArrays.stringArray, comboArrays.objectArray));
		if(obj) {
			result = (jstring)obj;
		}
		if (comboArrays.stringArray != NULL) {
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.stringArray);
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.objectArray);
		}
		if (result) {
			const char * str = SaxonProcessor::sxn_environ->env->GetStringUTFChars(result,
					NULL);
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(obj);
			return str;
		} else  {
			proc->checkAndCreateException(cppClass);

     		}
	}
	return NULL;


    }

    XdmValue* XsltExecutable::callTemplateReturningValue(const char* templateName){

          	setProperty("resources", proc->getResourcesDirectory());
          	static jmethodID ctsmID = NULL;
          	if (ctsmID == NULL) {
          	    ctsmID = (jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
          					"callTemplateReturningValue",
          					"(Ljava/lang/String;Lnet/sf/saxon/s9api/XsltExecutable;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)Lnet/sf/saxon/s9api/XdmValue;");
          	}
          	if (!ctsmID) {
          		std::cerr << "Error: "<<getDllname() << "callTemplateReturningValue" << " not found\n"
          				<< std::endl;

          	} else {
          	    JParameters comboArrays;
          		comboArrays = SaxonProcessor::createParameterJArray(parameters, properties);


          	    jstring result = NULL;
          	    jobject obj = (jobject)(SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, ctsmID,
          								SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXE.c_str()), executableObject,
                                        (templateName != NULL ? SaxonProcessor::sxn_environ->env->NewStringUTF(templateName) : NULL),
          								comboArrays.stringArray, comboArrays.objectArray));
          		if(obj) {
          			result = (jstring)obj;
          		}
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
          				xdmItem =  new XdmAtomicValue(result);
					    xdmItem->setProcessor(proc);
					    SaxonProcessor::sxn_environ->env->DeleteLocalRef(result);
					    return xdmItem;

          			} else if(SaxonProcessor::sxn_environ->env->IsInstanceOf(result, nodeClass)           == JNI_TRUE) {
          				xdmItem = new XdmNode(result);
					    xdmItem->setProcessor(proc);
					    SaxonProcessor::sxn_environ->env->DeleteLocalRef(result);
					    return xdmItem;

          			} else if (SaxonProcessor::sxn_environ->env->IsInstanceOf(result, functionItemClass)           == JNI_TRUE) {
                        xdmItem =  new XdmFunctionItem(result);
                        xdmItem->setProcessor(proc);
                        SaxonProcessor::sxn_environ->env->DeleteLocalRef(result);
                        return xdmItem;
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
         	} else  {
          		proc->checkAndCreateException(cppClass);
               	}
          }
        return NULL;
    }




XdmValue * XsltExecutable::transformFileToValue(const char* sourcefile) {

	if(sourcefile == NULL){

		return NULL;
	}

	setProperty("resources", proc->getResourcesDirectory());
	static jmethodID tfvMID = NULL;
	
	if(tfvMID == NULL) {
			tfvMID = (jmethodID) SaxonProcessor::sxn_environ->env->GetStaticMethodID(cppClass,
					"transformToValue",
					"(Ljava/lang/String;Lnet/sf/saxon/option/cpp/Xslt30Processor;Lnet/sf/saxon/s9api/XsltExecutable;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)Lnet/sf/saxon/s9api/XdmValue;");
	}
	if (!tfvMID) {
		std::cerr << "Error: "<< getDllname() << ".transformtoValue" << " not found\n"
				<< std::endl;

	} else {
	    JParameters comboArrays;
		comboArrays = SaxonProcessor::createParameterJArray(parameters, properties);

		jobject result = (jobject)(
				SaxonProcessor::sxn_environ->env->CallStaticObjectMethod(cppClass, tfvMID,
						SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXE.c_str()), NULL, executableObject,
						(sourcefile != NULL ?SaxonProcessor::sxn_environ->env->NewStringUTF(sourcefile) : NULL),
								 comboArrays.stringArray, comboArrays.objectArray));
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
          				xdmItem = new XdmAtomicValue(result);
                        xdmItem->setProcessor(proc);
                        SaxonProcessor::sxn_environ->env->DeleteLocalRef(result);
                        return xdmItem;

          			} else if(SaxonProcessor::sxn_environ->env->IsInstanceOf(result, nodeClass)           == JNI_TRUE) {
          				xdmItem = new XdmNode(result);
                        xdmItem->setProcessor(proc);
                        SaxonProcessor::sxn_environ->env->DeleteLocalRef(result);
                        return xdmItem;

          			} else if (SaxonProcessor::sxn_environ->env->IsInstanceOf(result, functionItemClass)           == JNI_TRUE) {
                        xdmItem =  new XdmFunctionItem(result);
                        xdmItem->setProcessor(proc);
                        SaxonProcessor::sxn_environ->env->DeleteLocalRef(result);
                        return xdmItem;
          			} else {
					value = new XdmValue(result, true);
					value->setProcessor(proc);
					for(int z=0;z<value->size();z++) {
						value->itemAt(z)->setProcessor(proc);
					}
					return value;
				}
				value = new XdmValue();
				value->setProcessor(proc);
          	    value->addUnderlyingValue(result);

          		SaxonProcessor::sxn_environ->env->DeleteLocalRef(result);
          		return value;
		}else {

			proc->checkAndCreateException(cppClass);

     		}
	}
	return NULL;

}


void XsltExecutable::transformFileToFile(const char* source, const char* outputfile) {

	setProperty("resources", proc->getResourcesDirectory());
	static jmethodID tffMID = NULL;

	if(tffMID == NULL) {
			tffMID = (jmethodID) SaxonProcessor::sxn_environ->env->GetStaticMethodID(cppClass,
					"transformToFile",
					"(Ljava/lang/String;Lnet/sf/saxon/option/cpp/Xslt30Processor;Lnet/sf/saxon/s9api/XsltExecutable;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)V");
	}
	if (!tffMID) {
		std::cerr << "Error: "<<getDllname() << "transformToFile" << " not found\n"
				<< std::endl;

	} else {
	    JParameters comboArrays;
        comboArrays = SaxonProcessor::createParameterJArray(parameters, properties);

		SaxonProcessor::sxn_environ->env->CallStaticVoidMethod(cppClass, tffMID,
								SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXE.c_str()), NULL, executableObject
								(source != NULL ? SaxonProcessor::sxn_environ->env->NewStringUTF(source) : NULL), NULL,
								(outputfile != NULL ? SaxonProcessor::sxn_environ->env->NewStringUTF(outputfile) :NULL),
								comboArrays.stringArray, comboArrays.objectArray);
		if (comboArrays.stringArray!= NULL) {
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.stringArray);
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(comboArrays.objectArray);
		}
		}
		proc->checkAndCreateException(cppClass);
	}





void XsltExecutable::setupXslMessage(bool show, const char *filename=NULL) {
    if(show) {
        if(filename == NULL) {
            setProperty("m", "on");
        } else {
            setProperty("m", std::string(filename));
        }
    } else {
        setProperty("m", "off");
    }
}


const char * XsltExecutable::transformFileToString(const char* source) {

	setProperty("resources", proc->getResourcesDirectory());
	static jmethodID tftMID = tftMID
	if(tftMID == NULL) {
	    tftMID = (jmethodID) SaxonProcessor::sxn_environ->env->GetStaticMethodID(cppClass,
					"transformToString",
					"(Ljava/lang/String;Lnet/sf/saxon/option/cpp/Xslt30Processor;Lnet/sf/saxon/s9api/XsltExecutable;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;");
	}
	if (!tftMID) {
		std::cerr << "Error: "<<getDllname() << "transformFileToString" << " not found\n"
				<< std::endl;

	} else {
	JParameters comboArrays;
    comboArrays = SaxonProcessor::createParameterJArray(parameters, properties);

	jstring result = NULL;
	jobject obj = SaxonProcessor::sxn_environ->env->CallStaticObjectMethod(cppClass, tftMID,
								SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXE.c_str()), NULL, executableObject,
						(source != NULL ? SaxonProcessor::sxn_environ->env->NewStringUTF(source) : NULL),
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
			proc->checkAndCreateException(cppClass);

     		}
	}
	return NULL;
}


   const char * XsltExecutable::transformToString(XdmNode * source){

	if(source != NULL){
      		source->incrementRefCount();
      		parameters["node"] = source;
    	}
	return transformFileToString(NULL, NULL);
   }


    XdmValue * XsltExecutable::transformToValue(XdmNode * source){

	if(source != NULL){
      		source->incrementRefCount();
      		parameters["node"] = source;
    	}
	return transformFileToValue(NULL, NULL);
   }

    void XsltExecutable::transformToFile(XdmNode * source){

	if(source != NULL){
      		source->incrementRefCount();
      		parameters["node"] = source;
    	}
	transformFileToFile(NULL, NULL, NULL);
   }

const char * XsltExecutable::getErrorMessage(int i ){
 	if(proc->exception == NULL) {return NULL;}
 	return proc->exception->getErrorMessage(i);
 }
