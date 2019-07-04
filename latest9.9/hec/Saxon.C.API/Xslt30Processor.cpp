// Xslt30Processor.cpp : Defines the exported functions for the DLL application.
//

#include "Xslt30Processor.h"
#include "XdmValue.h"
#include "XdmItem.h"
#include "XdmNode.h"
#include "XdmAtomicValue.h"
#ifdef DEBUG
#include <typeinfo> //used for testing only
#endif

Xslt30Processor::Xslt30Processor() {

	SaxonProcessor *p = new SaxonProcessor(false);
	Xslt30Processor(p, "");

}

Xslt30Processor::Xslt30Processor(SaxonProcessor * p, const char* curr) {

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
	nodeCreated = false;
	tunnel = false;
	proc->exception = NULL;
	outputfile1 = "";
	if(!(proc->cwd.empty()) && (curr == NULL && strlen(curr)==0)){
		cwdXT = proc->cwd;
	} else {
		cwdXT = curr;
	}

}

bool Xslt30Processor::exceptionOccurred() {
	return proc->exceptionOccurred();
}

const char * Xslt30Processor::getErrorCode(int i) {
 if(proc->exception == NULL) {return NULL;}
 return proc->exception->getErrorCode(i);
 }

void Xslt30Processor::setSourceFromXdmItem(XdmItem * value) {
    if(value != NULL){
      value->incrementRefCount();
      parameters["node"] = value;
    }
}

void Xslt30Processor::setSourceFromFile(const char * ifile) {
	if(ifile != NULL) {
		setProperty("s", ifile);
	}
}

void Xslt30Processor::setOutputFile(const char * ofile) {
	outputfile1 = std::string(ofile);
	setProperty("o", ofile);
}

void Xslt30Processor::setParameter(const char* name, XdmValue * value, bool _static) {
	if(value != NULL && name != NULL){
		value->incrementRefCount();
		if (_static) {
			parameters["sparam:"+std::string(name)] = value;
		
		} else {
			parameters["param:"+std::string(name)] = value;
		}
	 }
}

    void Xslt30Processor::setInitialTemplateParameters(std::map<std::string,XdmValue*> _itparameters, bool _tunnel){
	for(std::map<std::string, XdmValue*>::iterator itr = _itparameters.begin(); itr != _itparameters.end(); itr++){
		parameters["itparam:"+std::string(itr->first)] = itr->second;	
	}
	tunnel = _tunnel;
    }

XdmValue* Xslt30Processor::getParameter(const char* name) {
        std::map<std::string, XdmValue*>::iterator it;
        it = parameters.find("param:"+std::string(name));
        if (it != parameters.end())
          return it->second;
        else {
          it = parameters.find("sparam:"+std::string(name));
        if (it != parameters.end())
	  return it->second;
	return NULL;
}

bool Xslt30Processor::removeParameter(const char* name) {
	return (bool)(parameters.erase("param:"+std::string(name)));
}

void Xslt30Processor::setJustInTimeCompilation(bool jit){
	static jmethodID jitmID =
			(jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"setJustInTimeCompilation",
					"(Z)V");
	if (!jitmID) {
		std::cerr << "Error: "<<getDllname() << ".setJustInTimeCompilation"
				<< " not found\n" << std::endl;

	} else {
		SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, jitmID, jit);

		proc->checkAndCreateException(cppClass);
	}
}

void Xslt30Processor::setProperty(const char* name, const char* value) {	
	if(name != NULL) {
		properties.insert(std::pair<std::string, std::string>(std::string(name), std::string((value == NULL ? "" : value))));
	}
}

const char* Xslt30Processor::getProperty(const char* name) {
        std::map<std::string, std::string>::iterator it;
        it = properties.find(std::string(name));
        if (it != properties.end())
          return it->second.c_str();
	return NULL;
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

void Xslt30Processor::clearProperties() {
	properties.clear();
}



std::map<std::string,XdmValue*>& Xslt30Processor::getParameters(){
	std::map<std::string,XdmValue*>& ptr = parameters;
	return ptr;
}

std::map<std::string,std::string>& Xslt30Processor::getProperties(){
	std::map<std::string,std::string> &ptr = properties;
	return ptr;
}

void Xslt30Processor::exceptionClear(){
 if(proc->exception != NULL) {
 	delete proc->exception;
 	proc->exception = NULL;
	SaxonProcessor::sxn_environ->env->ExceptionClear();
 }
  
 }

   void Xslt30Processor::setcwd(const char* dir){
    cwdXT = std::string(dir);
   }

const char* Xslt30Processor::checkException() {
	/*if(proc->exception == NULL) {
	 proc->exception = proc->checkForException(environi, cpp);
	 }
	 return proc->exception;*/
	return proc->checkException(cppXT);
}

int Xslt30Processor::exceptionCount(){
 if(proc->exception != NULL){
 return proc->exception->count();
 }
 return 0;
 }


    void Xslt30Processor::compileFromXdmNodeAndSave(XdmNode * node, const char* filename) {
	static jmethodID cAndSNodemID =
			(jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"compileFromXdmNodeAndSave",
					"(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;)V");
	if (!cAndSNodemID) {
		std::cerr << "Error: "<<getDllname() << ".compileFromStringAndSave"
				<< " not found\n" << std::endl;

	} else {

		
		SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, cAndSNodemID,
						SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXT.c_str()),
						node->getUnderlyingValue(), 							SaxonProcessor::sxn_environ->env->NewStringUTF(filename));
		
		proc->checkAndCreateException(cppClass);		

    }



}

    void Xslt30Processor::compileFromStringAndSave(const char* stylesheetStr, const char* filename){
	static jmethodID cAndSStringmID =
			(jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"compileFromStringAndSave",
					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
	if (!cAndSStringmID) {
		std::cerr << "Error: "<<getDllname() << ".compileFromStringAndSave"
				<< " not found\n" << std::endl;

	} else {

		
		SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, cAndSStringmID,
						SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXT.c_str()),
						SaxonProcessor::sxn_environ->env->NewStringUTF(stylesheetStr), 							SaxonProcessor::sxn_environ->env->NewStringUTF(filename));
		
		proc->checkAndCreateException(cppClass);		

    }
}



    void Xslt30Processor::compileFromFileAndSave(const char* xslFilename, const char* filename){
	static jmethodID cAndFStringmID =
			(jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"compileFromFileAndSave",
					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
	if (!cAndFStringmID) {
		std::cerr << "Error: "<<getDllname() << ".compileFromFileAndSave"
				<< " not found\n" << std::endl;

	} else {

		
		SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, cAndFStringmID,
						SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXT.c_str()),
						SaxonProcessor::sxn_environ->env->NewStringUTF(xslFilename),SaxonProcessor::sxn_environ->env->NewStringUTF(filename));
		
		proc->checkAndCreateException(cppClass);


     }
}

void Xslt30Processor::compileFromString(const char* stylesheetStr) {
	static jmethodID cStringmID =
			(jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"createStylesheetFromString",
					"(Ljava/lang/String;Ljava/lang/String;)Lnet/sf/saxon/s9api/XsltExecutable;");
	if (!cStringmID) {
		std::cerr << "Error: "<<getDllname() << "createStylesheetFromString"
				<< " not found\n" << std::endl;

	} else {

		stylesheetObject = (jobject)(
				SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, cStringmID,
						SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXT.c_str()),
						SaxonProcessor::sxn_environ->env->NewStringUTF(stylesheetStr)));
		if (!stylesheetObject) {
			proc->checkAndCreateException(cppClass);
		}
	}

}

void Xslt30Processor::compileFromXdmNode(XdmNode * node) {
	static jmethodID cNodemID =
			(jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"createStylesheetFromFile",
					"(Ljava/lang/String;Lnet/sf/saxon/s9api/XdmNode;)Lnet/sf/saxon/s9api/XsltExecutable;");
	if (!cNodemID) {
		std::cerr << "Error: "<< getDllname() << "createStylesheetFromXdmNode"
				<< " not found\n" << std::endl;

	} else {
		releaseStylesheet();
		stylesheetObject = (jobject)(
				SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, cNodemID,
						SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXT.c_str()),
						node->getUnderlyingValue()));
		if (!stylesheetObject) {
			proc->checkAndCreateException(cppClass);
			//cout << "Error in compileFromXdmNode" << endl;
		}
	}

}

void Xslt30Processor::compileFromFile(const char* stylesheet) {
	static jmethodID cFilemID =
			(jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"createStylesheetFromFile",
					"(Ljava/lang/String;Ljava/lang/String;)Lnet/sf/saxon/s9api/XsltExecutable;");
	if (!cFilemID) {
		std::cerr << "Error: "<<getDllname() << "createStylesheetFromFile"
				<< " not found\n" << std::endl;

	} else {
		releaseStylesheet();

		stylesheetObject = (jobject)(
				SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, cFilemID,
						SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXT.c_str()),
						SaxonProcessor::sxn_environ->env->NewStringUTF(stylesheet)));
		if (!stylesheetObject) {
			proc->checkAndCreateException(cppClass);
     		
		}
		//SaxonProcessor::sxn_environ->env->NewGlobalRef(stylesheetObject);
	}

}

void Xslt30Processor::releaseStylesheet() {

	stylesheetObject = NULL;
	
}

    void Xslt30Processor::applyTemplateToFile(const char * input_filename, const char* outfile){
	if(exceptionOccurred()) {
		//Possible error detected in the compile phase. Processor not in a clean state.
		//Require clearing exception.
		return NULL;	
	}

	if(stylesheetfile == NULL && !stylesheetObject && ){
	
		return NULL;
	}

	setProperty("resources", proc->getResourcesDirectory());
	static jmethodID atmID =
			(jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"applyTemplateToFile",
					"(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)Lnet/sf/saxon/s9api/XdmNode;");
	if (!atmID) {
		std::cerr << "Error: "<< getDllname() << "applyTemplateToFile" << " not found\n"
				<< std::endl;

	} else {
		jobjectArray stringArray = NULL;
		jobjectArray objectArray = NULL;
		jclass objectClass = lookForClass(SaxonProcessor::sxn_environ->env,
				"java/lang/Object");
		jclass stringClass = lookForClass(SaxonProcessor::sxn_environ->env,
				"java/lang/String");

		int size = parameters.size() + properties.size();
		if (size > 0) {
			objectArray = SaxonProcessor::sxn_environ->env->NewObjectArray((jint) size,
					objectClass, 0);
			stringArray = SaxonProcessor::sxn_environ->env->NewObjectArray((jint) size,
					stringClass, 0);
			int i = 0;
			for (std::map<std::string, XdmValue*>::iterator iter =
					parameters.begin(); iter != parameters.end(); ++iter, i++) {
				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(stringArray, i,
						SaxonProcessor::sxn_environ->env->NewStringUTF(
								(iter->first).c_str()));
				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(objectArray, i,
						(iter->second)->getUnderlyingValue());
			}
			for (std::map<std::string, std::string>::iterator iter =
					properties.begin(); iter != properties.end(); ++iter, i++) {
				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(stringArray, i,
						SaxonProcessor::sxn_environ->env->NewStringUTF(
								(iter->first).c_str()));
				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(objectArray, i,
						SaxonProcessor::sxn_environ->env->NewStringUTF(
								(iter->second).c_str()));
			}
		}
		SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, atmID,
						SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXT.c_str()),
						(input_filename != NULL ?
								SaxonProcessor::sxn_environ->env->NewStringUTF(input_filename) :
								NULL),
						(stylesheetfile != NULL ?
								SaxonProcessor::sxn_environ->env->NewStringUTF(
										stylesheetfile) :
								NULL), stringArray, objectArray));
		if (size > 0) {
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(stringArray);
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(objectArray);
		}
		proc->checkAndCreateException(cppClass);
	   	
	}
	return NULL;

}

const char* Xslt30Processor::applyTemplateToString(const char * source){
	if(exceptionOccurred()) {
		//Possible error detected in the compile phase. Processor not in a clean state.
		//Require clearing exception.
		return NULL;	
	}
	if(source == NULL ){
		std::cerr<< "Error: The most recent StylesheetObject failed. Please check exceptions"<<std::endl;
		return NULL;
	}
	if(stylesheet == NULL && !stylesheetObject){
		std::cerr<< "Error: The most recent StylesheetObject failed. Please check exceptions"<<std::endl;
		return NULL;
	}
	setProperty("resources", proc->getResourcesDirectory());
	jmethodID atsmID =
			(jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"applyTemplateToString",
					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;");
	if (!atsmID) {
		std::cerr << "Error: "<<getDllname() << "applyTemplateToString" << " not found\n"
				<< std::endl;

	} else {
		jobjectArray stringArray = NULL;
		jobjectArray objectArray = NULL;
		jclass objectClass = lookForClass(SaxonProcessor::sxn_environ->env,
				"java/lang/Object");
		jclass stringClass = lookForClass(SaxonProcessor::sxn_environ->env,
				"java/lang/String");

		int size = parameters.size() + properties.size();
#ifdef DEBUG
		std::cerr<<"Properties size: "<<properties.size()<<std::endl;
		std::cerr<<"Parameter size: "<<parameters.size()<<std::endl;
#endif
		if (size > 0) {
			objectArray = SaxonProcessor::sxn_environ->env->NewObjectArray((jint) size,
					objectClass, 0);
			stringArray = SaxonProcessor::sxn_environ->env->NewObjectArray((jint) size,
					stringClass, 0);
			int i = 0;
			for (std::map<std::string, XdmValue*>::iterator iter =
					parameters.begin(); iter != parameters.end(); ++iter, i++) {

#ifdef DEBUG
				std::cerr<<"map 1"<<std::endl;
				std::cerr<<"iter->first"<<(iter->first).c_str()<<std::endl;
#endif
				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(stringArray, i,
						SaxonProcessor::sxn_environ->env->NewStringUTF(
								(iter->first).c_str()));
#ifdef DEBUG
				std::string s1 = typeid(iter->second).name();
				std::cerr<<"Type of itr:"<<s1<<std::endl;


				jobject xx = (iter->second)->getUnderlyingValue();

				if(xx == NULL) {
					std::cerr<<"value failed"<<std::endl;
				} else {

					std::cerr<<"Type of value:"<<(typeid(xx).name())<<std::endl;
				}
				if((iter->second)->getUnderlyingValue() == NULL) {
					std::cerr<<"(iter->second)->getUnderlyingValue() is NULL"<<std::endl;
				}
#endif

				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(objectArray, i,
						(iter->second)->getUnderlyingValue());

			}

			for (std::map<std::string, std::string>::iterator iter =
					properties.begin(); iter != properties.end(); ++iter, i++) {
				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(stringArray, i,
						SaxonProcessor::sxn_environ->env->NewStringUTF(
								(iter->first).c_str()));
				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(objectArray, i,
						SaxonProcessor::sxn_environ->env->NewStringUTF(
								(iter->second).c_str()));
			}
		}

	jstring result = NULL;
	jobject obj =
				(
						SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, atsmID,
								SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXT.c_str()),
								(source != NULL ?
										SaxonProcessor::sxn_environ->env->NewStringUTF(
												source) :
										NULL),
								SaxonProcessor::sxn_environ->env->NewStringUTF(stylesheet),
								stringArray, objectArray));
		if(obj) {
			result = (jstring)obj;
		}		
		if (size > 0) {
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(stringArray);
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(objectArray);
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

XdmValue * Xslt30Processor::applyTemplateToValue(const char * input_filename){

}

void Xslt30Processor::applyTemplateToFile(XdmValue* _input, const char* outfile){}

const char* Xslt30Processor::applyTemplateToString(XdmValue* _input){}

XdmValue * Xslt30Processor::applyTemplateToValue(XdmValue* _input){}


XdmValue * Xslt30Processor::transformFileToValue(const char* sourcefile,
		const char* stylesheetfile) {

	if(exceptionOccurred()) {
		//Possible error detected in the compile phase. Processor not in a clean state.
		//Require clearing exception.
		return NULL;	
	}

	if(sourcefile == NULL && stylesheetfile == NULL && !stylesheetObject){
	
		return NULL;
	}

	setProperty("resources", proc->getResourcesDirectory());
	static jmethodID mID =
			(jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"transformToNode",
					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)Lnet/sf/saxon/s9api/XdmNode;");
	if (!mID) {
		std::cerr << "Error: "<< getDllname() << "transformtoNode" << " not found\n"
				<< std::endl;

	} else {
		jobjectArray stringArray = NULL;
		jobjectArray objectArray = NULL;
		jclass objectClass = lookForClass(SaxonProcessor::sxn_environ->env,
				"java/lang/Object");
		jclass stringClass = lookForClass(SaxonProcessor::sxn_environ->env,
				"java/lang/String");

		int size = parameters.size() + properties.size();
		if (size > 0) {
			objectArray = SaxonProcessor::sxn_environ->env->NewObjectArray((jint) size,
					objectClass, 0);
			stringArray = SaxonProcessor::sxn_environ->env->NewObjectArray((jint) size,
					stringClass, 0);
			int i = 0;
			for (std::map<std::string, XdmValue*>::iterator iter =
					parameters.begin(); iter != parameters.end(); ++iter, i++) {
				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(stringArray, i,
						SaxonProcessor::sxn_environ->env->NewStringUTF(
								(iter->first).c_str()));
				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(objectArray, i,
						(iter->second)->getUnderlyingValue());
			}
			for (std::map<std::string, std::string>::iterator iter =
					properties.begin(); iter != properties.end(); ++iter, i++) {
				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(stringArray, i,
						SaxonProcessor::sxn_environ->env->NewStringUTF(
								(iter->first).c_str()));
				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(objectArray, i,
						SaxonProcessor::sxn_environ->env->NewStringUTF(
								(iter->second).c_str()));
			}
		}
		jobject result = (jobject)(
				SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, mID,
						SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXT.c_str()),
						(sourcefile != NULL ?
								SaxonProcessor::sxn_environ->env->NewStringUTF(sourcefile) :
								NULL),
						(stylesheetfile != NULL ?
								SaxonProcessor::sxn_environ->env->NewStringUTF(
										stylesheetfile) :
								NULL), stringArray, objectArray));
		if (size > 0) {
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(stringArray);
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(objectArray);
		}
		if (result) {
			XdmNode * node = new XdmNode(result);
			node->setProcessor(proc);
			return node;
		}else {
	
			proc->checkAndCreateException(cppClass);
	   		
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
	if(source == NULL && outputfile == NULL && !stylesheetObject){
		
		return;
	}
	setProperty("resources", proc->getResourcesDirectory());
	jmethodID mID =
			(jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"transformToFile",
					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)V");
	if (!mID) {
		std::cerr << "Error: "<<getDllname() << "transformToFile" << " not found\n"
				<< std::endl;

	} else {
		jobjectArray stringArray = NULL;
		jobjectArray objectArray = NULL;
		jclass objectClass = lookForClass(SaxonProcessor::sxn_environ->env,
				"java/lang/Object");
		jclass stringClass = lookForClass(SaxonProcessor::sxn_environ->env,
				"java/lang/String");

		int size = parameters.size() + properties.size();
#ifdef DEBUG
		std::cerr<<"Properties size: "<<properties.size()<<std::endl;
		std::cerr<<"Parameter size: "<<parameters.size()<<std::endl;
		std::cerr<<"size:"<<size<<std::endl;
#endif
		if (size > 0) {
			objectArray = SaxonProcessor::sxn_environ->env->NewObjectArray((jint) size,
					objectClass, 0);
			stringArray = SaxonProcessor::sxn_environ->env->NewObjectArray((jint) size,
					stringClass, 0);
		
			int i = 0;
			for (std::map<std::string, XdmValue*>::iterator iter =
					parameters.begin(); iter != parameters.end(); ++iter, i++) {

#ifdef DEBUG
				std::cerr<<"map 1"<<std::endl;
				std::cerr<<"iter->first"<<(iter->first).c_str()<<std::endl;
#endif
				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(stringArray, i,
						SaxonProcessor::sxn_environ->env->NewStringUTF(
								(iter->first).c_str()));
#ifdef DEBUG
				std::string s1 = typeid(iter->second).name();
				std::cerr<<"Type of itr:"<<s1<<std::endl;
				jobject xx = (iter->second)->getUnderlyingValue();
				if(xx == NULL) {
					std::cerr<<"value failed"<<std::endl;
				} else {

					std::cerr<<"Type of value:"<<(typeid(xx).name())<<std::endl;
				}
				if((iter->second)->getUnderlyingValue() == NULL) {
					std::cerr<<"(iter->second)->getUnderlyingValue() is NULL"<<std::endl;
				}
#endif
				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(objectArray, i,
						(iter->second)->getUnderlyingValue());

			}

			for (std::map<std::string, std::string>::iterator iter =
					properties.begin(); iter != properties.end(); ++iter, i++) {
				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(stringArray, i,
						SaxonProcessor::sxn_environ->env->NewStringUTF(
								(iter->first).c_str()));
				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(objectArray, i,
						SaxonProcessor::sxn_environ->env->NewStringUTF(
								(iter->second).c_str()));
			}
		}
		SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, mID,
								SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXT.c_str()),
								(source != NULL ?
										SaxonProcessor::sxn_environ->env->NewStringUTF(
												source) :
										NULL),
								SaxonProcessor::sxn_environ->env->NewStringUTF(stylesheet),NULL,
								stringArray, objectArray);
		if (size > 0) {
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(stringArray);
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(objectArray);
		}
		
	}

	proc->checkAndCreateException(cppClass);
}



XdmValue * Xslt30Processor::getXslMessages(){

jmethodID mID =   (jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"getXslMessages",
					"()[Lnet/sf/saxon/s9api/XdmValue;");
	if (!mID) {
		std::cerr << "Error: "<<getDllname() << "transformToString" << " not found\n"
				<< std::endl;

	} else {
jobjectArray results = (jobjectArray)(
			SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, mID));
	int sizex = SaxonProcessor::sxn_environ->env->GetArrayLength(results);

	if (sizex>0) {
		XdmValue * value = new XdmValue();
		
		for (int p=0; p < sizex; ++p) 
		{
			jobject resulti = SaxonProcessor::sxn_environ->env->GetObjectArrayElement(results, p);
			value->addUnderlyingValue(resulti);
		}
		SaxonProcessor::sxn_environ->env->DeleteLocalRef(results);
		return value;
	}
	proc->checkAndCreateException(cppClass);
    }
    return NULL;


}

const char * Xslt30Processor::transformFileToString(const char* source,
		const char* stylesheet) {

	if(exceptionOccurred()) {
		//Possible error detected in the compile phase. Processor not in a clean state.
		//Require clearing exception.
		return NULL;	
	}
	if(source == NULL && stylesheet == NULL && !stylesheetObject){
		std::cerr<< "Error: The most recent StylesheetObject failed. Please check exceptions"<<std::endl;
		return NULL;
	}
	setProperty("resources", proc->getResourcesDirectory());
	jmethodID mID =
			(jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(cppClass,
					"transformToString",
					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;");
	if (!mID) {
		std::cerr << "Error: "<<getDllname() << "transformFileToString" << " not found\n"
				<< std::endl;

	} else {
		jobjectArray stringArray = NULL;
		jobjectArray objectArray = NULL;
		jclass objectClass = lookForClass(SaxonProcessor::sxn_environ->env,
				"java/lang/Object");
		jclass stringClass = lookForClass(SaxonProcessor::sxn_environ->env,
				"java/lang/String");

		int size = parameters.size() + properties.size();
#ifdef DEBUG
		std::cerr<<"Properties size: "<<properties.size()<<std::endl;
		std::cerr<<"Parameter size: "<<parameters.size()<<std::endl;
#endif
		if (size > 0) {
			objectArray = SaxonProcessor::sxn_environ->env->NewObjectArray((jint) size,
					objectClass, 0);
			stringArray = SaxonProcessor::sxn_environ->env->NewObjectArray((jint) size,
					stringClass, 0);
			int i = 0;
			for (std::map<std::string, XdmValue*>::iterator iter =
					parameters.begin(); iter != parameters.end(); ++iter, i++) {

#ifdef DEBUG
				std::cerr<<"map 1"<<std::endl;
				std::cerr<<"iter->first"<<(iter->first).c_str()<<std::endl;
#endif
				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(stringArray, i,
						SaxonProcessor::sxn_environ->env->NewStringUTF(
								(iter->first).c_str()));
#ifdef DEBUG
				std::string s1 = typeid(iter->second).name();
				std::cerr<<"Type of itr:"<<s1<<std::endl;


				jobject xx = (iter->second)->getUnderlyingValue();

				if(xx == NULL) {
					std::cerr<<"value failed"<<std::endl;
				} else {

					std::cerr<<"Type of value:"<<(typeid(xx).name())<<std::endl;
				}
				if((iter->second)->getUnderlyingValue() == NULL) {
					std::cerr<<"(iter->second)->getUnderlyingValue() is NULL"<<std::endl;
				}
#endif

				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(objectArray, i,
						(iter->second)->getUnderlyingValue());

			}

			for (std::map<std::string, std::string>::iterator iter =
					properties.begin(); iter != properties.end(); ++iter, i++) {
				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(stringArray, i,
						SaxonProcessor::sxn_environ->env->NewStringUTF(
								(iter->first).c_str()));
				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(objectArray, i,
						SaxonProcessor::sxn_environ->env->NewStringUTF(
								(iter->second).c_str()));
			}
		}

	jstring result = NULL;
	jobject obj =
				(
						SaxonProcessor::sxn_environ->env->CallObjectMethod(cppXT, mID,
								SaxonProcessor::sxn_environ->env->NewStringUTF(cwdXT.c_str()),
								(source != NULL ?
										SaxonProcessor::sxn_environ->env->NewStringUTF(
												source) :
										NULL),
								SaxonProcessor::sxn_environ->env->NewStringUTF(stylesheet),
								stringArray, objectArray));
		if(obj) {
			result = (jstring)obj;
		}		
		if (size > 0) {
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(stringArray);
			SaxonProcessor::sxn_environ->env->DeleteLocalRef(objectArray);
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


   const char * Xslt30Processor::transformToString(){
	if(!stylesheetObject){
		std::cerr<< "Error: The most recent Stylesheet Object failed or has not been set."<<std::endl;
		return NULL;
	}
	return transformFileToString(NULL, NULL);
   }


    XdmValue * Xslt30Processor::transformToValue(){
	if(!stylesheetObject){
		std::cerr<< "Error: The most recent Stylesheet Object failed or has not been set."<<std::endl;
		return NULL;
	}
	return transformFileToValue(NULL, NULL);
   }

    void Xslt30Processor::transformToFile(){
	if(!stylesheetObject){
		std::cerr<< "Error: The most recent Stylesheet Object failed or has not been set."<<std::endl;
		return;
	}
	transformFileToFile(NULL, NULL, NULL);
   }

const char * Xslt30Processor::getErrorMessage(int i ){
 	if(proc->exception == NULL) {return NULL;}
 		return proc->exception->getErrorMessage(i);
 }

