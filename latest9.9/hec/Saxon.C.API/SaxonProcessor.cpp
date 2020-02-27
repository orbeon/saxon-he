#ifndef __linux__ 
#ifndef __APPLE__
	//#include "stdafx.h"
	#include <Tchar.h>
#endif
#endif


//#include "stdafx.h"
#include "SaxonProcessor.h"
#include "XdmValue.h"
#include "XdmItem.h"
#include "XdmNode.h"
#include "XdmAtomicValue.h"
#if CVERSION_API_NO >= 123
    #include "XdmFunctionItem.h"
    #include "XdmMap.h"
    #include "XdmArray.h"
#endif

//#define DEBUG
#ifdef DEBUG
#include <signal.h>
#endif
#include <stdio.h>


//jobject cpp;
const char * failure;
sxnc_environment * SaxonProcessor::sxn_environ = 0;
int SaxonProcessor::refCount = 0;
int SaxonProcessor::jvmCreatedCPP=0;

bool SaxonProcessor::exceptionOccurred(){
	bool found = SaxonProcessor::sxn_environ->env->ExceptionCheck();
	return found;
}

const char* SaxonProcessor::checkException(jobject cpp) {
		const char * message = NULL;
		message = checkForException(sxn_environ, cpp);
		return message;
	}

SaxonApiException * SaxonProcessor::checkAndCreateException(jclass cppClass){
		if(exceptionOccurred()) {
		    SaxonApiException * exception = checkForExceptionCPP(SaxonProcessor::sxn_environ->env, cppClass, NULL);
#ifdef DEBUG
		    SaxonProcessor::sxn_environ->env->ExceptionDescribe();
#endif
		    return exception;
		}
		return NULL;
	}

void SaxonProcessor::exceptionClear(){
	SaxonProcessor::sxn_environ->env->ExceptionClear();
	/* if(exception != NULL && clearCPPException) {
		delete exception;
	} */
}



SaxonProcessor::SaxonProcessor() {
    licensei = false;
    SaxonProcessor(false);
}



SaxonApiException * SaxonProcessor::checkForExceptionCPP(JNIEnv* env, jclass callingClass,  jobject callingObject){

    if (env->ExceptionCheck()) {
	std::string result1 = "";
	std::string errorCode = "";
	jthrowable exc = env->ExceptionOccurred();

#ifdef DEBUG	
	env->ExceptionDescribe();
#endif
	 jclass exccls(env->GetObjectClass(exc));
        jclass clscls(env->FindClass("java/lang/Class"));

        jmethodID getName(env->GetMethodID(clscls, "getName", "()Ljava/lang/String;"));
        jstring name(static_cast<jstring>(env->CallObjectMethod(exccls, getName)));
        char const* utfName(env->GetStringUTFChars(name, 0));
	    result1 = (std::string(utfName));
	    env->ReleaseStringUTFChars(name, utfName);

	 jmethodID  getMessage(env->GetMethodID(exccls, "getMessage", "()Ljava/lang/String;"));
	if(getMessage) {

		jstring message((jstring)(env->CallObjectMethod(exc, getMessage)));
		char const* utfMessage = NULL;		
		if(!message) {
			utfMessage = "";
			return NULL;
		} else {
        		utfMessage = (env->GetStringUTFChars(message, 0));
		}
		if(utfMessage != NULL) {
			result1 = (result1 + " : ") + utfMessage;
		} 
		
		env->ReleaseStringUTFChars(message,utfMessage);

		if(callingObject != NULL && result1.compare(0,43, "net.sf.saxon.s9api.SaxonApiException", 43) == 0){

			jclass saxonApiExceptionClass(env->FindClass("net/sf/saxon/s9api/SaxonApiException"));
			static jmethodID lineNumID = NULL;
			if(lineNumID == NULL) {
			    lineNumID = env->GetMethodID(saxonApiExceptionClass, "getLinenumber", "()I");
			}
			static jmethodID ecID = NULL;
			if(ecID == NULL) {
			    ecID = env->GetMethodID(saxonApiExceptionClass, "getErrorCode", "()Ljnet/sf/saxon/s9api/QName;");
			}
			static jmethodID esysID = NULL;
			if(esysID == NULL) {
			    esysID = env->GetMethodID(saxonApiExceptionClass, "getSystemId", "()Ljava/lang/String;");
			}


			jobject errCodeQName = (jobject)(env->CallObjectMethod(exc, ecID));
			jstring errSystemID = (jstring)(env->CallObjectMethod(exc, esysID));
			int linenum = env->CallIntMethod(exc, lineNumID);

			jclass qnameClass(env->FindClass("net/sf/saxon/s9api/QName"));
            static jmethodID qnameStrID = NULL;
            if(qnameStrID == NULL) {
                qnameStrID = env->GetMethodID(qnameClass, "toString", "()Ljava/lang/String;");
            }

            jstring qnameStr = (jstring)(env->CallObjectMethod(errCodeQName, qnameStrID));


			SaxonApiException * saxonExceptions = new SaxonApiException(result1.c_str(), (qnameStr ? env->GetStringUTFChars(qnameStr,0) : NULL)  ,(errSystemID ? env->GetStringUTFChars(errSystemID,0) : NULL),linenum);

            if(errCodeQName) {
			    env->DeleteLocalRef(errCodeQName);
			}
			if(errSystemID) {
			    env->DeleteLocalRef(errSystemID);
			}
			if(qnameStr) {
			    env->DeleteLocalRef(qnameStr);
			}

			if(message) {
           	    env->DeleteLocalRef(message);
   			}
			env->ExceptionClear();
			return saxonExceptions;
		}
	}
	SaxonApiException * saxonExceptions = new SaxonApiException(result1.c_str());
	//env->ExceptionDescribe();
	env->ExceptionClear();
	return saxonExceptions;
     }
	return NULL;

}



SaxonProcessor::SaxonProcessor(bool l){

    cwd="";
    licensei = l;
    versionStr = NULL;
    SaxonProcessor::refCount++;

     if(SaxonProcessor::jvmCreatedCPP == 0){
	SaxonProcessor::jvmCreatedCPP=1;
    SaxonProcessor::sxn_environ= new sxnc_environment;//(sxnc_environment *)malloc(sizeof(sxnc_environment));


    /*
     * First of all, load required component.
     * By the time of JET initialization, all components should be loaded.
     */

    SaxonProcessor::sxn_environ->myDllHandle = loadDefaultDll ();
	
    /*
     * Initialize JET run-time.
     * The handle of loaded component is used to retrieve Invocation API.
     */
    initDefaultJavaRT (SaxonProcessor::sxn_environ);
    } else {
#ifdef DEBUG
     std::cerr<<"SaxonProc constructor: jvm exists! jvmCreatedCPP="<<jvmCreatedCPP<<std::endl;
#endif

}

 
    versionClass = lookForClass(SaxonProcessor::sxn_environ->env, "net/sf/saxon/Version");
    procClass = lookForClass(SaxonProcessor::sxn_environ->env, "net/sf/saxon/s9api/Processor");
    saxonCAPIClass = lookForClass(SaxonProcessor::sxn_environ->env, "net/sf/saxon/option/cpp/SaxonCAPI");
    
    proc = createSaxonProcessor (SaxonProcessor::sxn_environ->env, procClass, "(Z)V", NULL, licensei);
	if(!proc) {
		std::cout<<"proc is NULL in SaxonProcessor constructor"<<std::endl;
	}

    xdmAtomicClass = lookForClass(SaxonProcessor::sxn_environ->env, "net/sf/saxon/s9api/XdmAtomicValue");
#ifdef DEBUG
	jmethodID debugMID = SaxonProcessor::sxn_environ->env->GetStaticMethodID(saxonCAPIClass, "setDebugMode", "(Z)V");
	SaxonProcessor::sxn_environ->env->CallStaticVoidMethod(saxonCAPIClass, debugMID, (jboolean)true);
#endif
}

SaxonProcessor::SaxonProcessor(const char * configFile){
    cwd="";
    versionStr = NULL;
    SaxonProcessor::refCount++;

    if(SaxonProcessor::jvmCreatedCPP == 0){
	SaxonProcessor::jvmCreatedCPP=1;
    //SaxonProcessor::sxn_environ= new sxnc_environment;
	SaxonProcessor::sxn_environ= (sxnc_environment *)malloc(sizeof(sxnc_environment));

    /*
     * First of all, load required component.
     * By the time of JET initialization, all components should be loaded.
     */

    SaxonProcessor::sxn_environ->myDllHandle = loadDefaultDll ();

    /*
     * Initialize JET run-time.
     * The handle of loaded component is used to retrieve Invocation API.
     */
    initDefaultJavaRT (SaxonProcessor::sxn_environ); 
    }
 
    versionClass = lookForClass(SaxonProcessor::sxn_environ->env, "net/sf/saxon/Version");

    procClass = lookForClass(SaxonProcessor::sxn_environ->env, "net/sf/saxon/s9api/Processor");
    saxonCAPIClass = lookForClass(SaxonProcessor::sxn_environ->env, "net/sf/saxon/option/cpp/SaxonCAPI");

     static jmethodID mIDcreateProc = (jmethodID)SaxonProcessor::sxn_environ->env->GetStaticMethodID(saxonCAPIClass,"createSaxonProcessor",
					"(Ljava/lang/String;)Lnet/sf/saxon/s9api/Processor;");
		if (!mIDcreateProc) {
			std::cerr << "Error: SaxonDll." << "getPrimitiveTypeName"
				<< " not found\n" << std::endl;
			return ;
		}
	proc = SaxonProcessor::sxn_environ->env->CallStaticObjectMethod(saxonCAPIClass, mIDcreateProc,SaxonProcessor::sxn_environ->env->NewStringUTF(configFile));
		
	if(!proc) {
		checkAndCreateException(saxonCAPIClass);
		std::cerr << "Error: "<<getDllname() << ". processor is NULL in constructor(configFile)"<< std::endl;
		return ;	
	}
	
     licensei = true;
#ifdef DEBUG

     std::cerr<<"SaxonProc constructor(configFile)"<<std::endl;
#endif
    xdmAtomicClass = lookForClass(SaxonProcessor::sxn_environ->env, "net/sf/saxon/s9api/XdmAtomicValue");
}

    SaxonProcessor::~SaxonProcessor(){
	clearConfigurationProperties();
	if(versionStr != NULL) {
		delete versionStr;
	}
	SaxonProcessor::refCount--;	//This might be redundant due to the bug fix 2670
   }


bool SaxonProcessor::isSchemaAwareProcessor(){
	if(!licensei) {
		return false;
	} else {
		static jmethodID MID_schema = (jmethodID)SaxonProcessor::sxn_environ->env->GetMethodID(procClass, "isSchemaAware", "()Z");
    		if (!MID_schema) {
        		std::cerr<<"\nError: Saxonc "<<"SaxonProcessor.isSchemaAware()"<<" not found"<<std::endl;
        		return false;
    		}

    		licensei = (jboolean)(SaxonProcessor::sxn_environ->env->CallBooleanMethod(proc, MID_schema));
        	return licensei;

	}

}

void SaxonProcessor::applyConfigurationProperties(){
	if(configProperties.size()>0) {
		int size = configProperties.size();
		jclass stringClass = lookForClass(SaxonProcessor::sxn_environ->env, "java/lang/String");
		jobjectArray stringArray1 = SaxonProcessor::sxn_environ->env->NewObjectArray( (jint)size, stringClass, 0 );
		jobjectArray stringArray2 = SaxonProcessor::sxn_environ->env->NewObjectArray( (jint)size, stringClass, 0 );
		static jmethodID mIDappConfig = NULL;
		if(mIDappConfig == NULL) {
			mIDappConfig = (jmethodID) SaxonProcessor::sxn_environ->env->GetStaticMethodID(saxonCAPIClass,"applyToConfiguration",
					"(Lnet/sf/saxon/s9api/Processor;[Ljava/lang/String;[Ljava/lang/String;)V");
			if (!mIDappConfig) {
				std::cerr << "Error: SaxonDll." << "applyToConfiguration"
				<< " not found\n" << std::endl;
				return;
			}
		}
		int i=0;
		std::map<std::string, std::string >::iterator iter =configProperties.begin();
		for(iter=configProperties.begin(); iter!=configProperties.end(); ++iter, i++) {
	     		SaxonProcessor::sxn_environ->env->SetObjectArrayElement( stringArray1, i, SaxonProcessor::sxn_environ->env->NewStringUTF( (iter->first).c_str()  ));
	     		SaxonProcessor::sxn_environ->env->SetObjectArrayElement( stringArray2, i, SaxonProcessor::sxn_environ->env->NewStringUTF((iter->second).c_str()) );
	   }
		SaxonProcessor::sxn_environ->env->CallStaticObjectMethod(saxonCAPIClass, mIDappConfig,proc, stringArray1,stringArray2);
		if (exceptionOccurred()) {
	   		exception = checkAndCreateException(saxonCAPIClass);
			exceptionClear();
      		 }
 	  SaxonProcessor::sxn_environ->env->DeleteLocalRef(stringArray1);
	  SaxonProcessor::sxn_environ->env->DeleteLocalRef(stringArray2);
		
	}
}


  jobjectArray SaxonProcessor::createJArray(XdmValue ** values, int length){
    jobjectArray valueArray = NULL;

    jclass xdmValueClass = lookForClass(SaxonProcessor::sxn_environ->env,
                   				"net/sf/saxon/s9api/XdmValue");


    valueArray = SaxonProcessor::sxn_environ->env->NewObjectArray((jint) length,
                   					xdmValueClass, 0);

    for (int i=0; i<length; i++) {
#ifdef DEBUG
				std::string s1 = typeid(values[i]).name();
				std::cerr<<"In createJArray\nType of itr:"<<s1<<std::endl;


				jobject xx = values[i]->getUnderlyingValue();

				if(xx == NULL) {
					std::cerr<<"value failed"<<std::endl;
				} else {

					std::cerr<<"Type of value:"<<(typeid(xx).name())<<std::endl;
				}
				if(values[i]->getUnderlyingValue() == NULL) {
					std::cerr<<"value["<<i<<"]->getUnderlyingValue() is NULL"<<std::endl;
				}
#endif
        SaxonProcessor::sxn_environ->env->SetObjectArrayElement(valueArray, i,values[i]->getUnderlyingValue());
    }
    return valueArray;

  }


JParameters SaxonProcessor::createParameterJArray(std::map<std::string,XdmValue*> parameters, std::map<std::string,std::string> properties){
		JParameters comboArrays;
		comboArrays.stringArray = NULL;
		comboArrays.objectArray = NULL;
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
		    
			comboArrays.objectArray = SaxonProcessor::sxn_environ->env->NewObjectArray((jint) size,
					objectClass, 0);
			comboArrays.stringArray = SaxonProcessor::sxn_environ->env->NewObjectArray((jint) size,
					stringClass, 0);
			int i = 0;
			for (std::map<std::string, XdmValue*>::iterator iter =
					parameters.begin(); iter != parameters.end(); ++iter, i++) {

#ifdef DEBUG
				std::cerr<<"map 1"<<std::endl;
				std::cerr<<"iter->first"<<(iter->first).c_str()<<std::endl;
#endif
				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(comboArrays.stringArray, i,
						SaxonProcessor::sxn_environ->env->NewStringUTF(
								(iter->first).c_str()));
#ifdef DEBUG
				std::string s1 = typeid(iter->second).name();
				std::cerr<<"Type of itr:"<<s1<<std::endl;

				if((iter->second) == NULL) {std::cerr<<"iter->second is null"<<std::endl;
				} else {
					std::cerr<<"getting underlying value"<<std::endl;
				jobject xx = (iter->second)->getUnderlyingValue();

				if(xx == NULL) {
					std::cerr<<"value failed"<<std::endl;
				} else {

					std::cerr<<"Type of value:"<<(typeid(xx).name())<<std::endl;
				}
				if((iter->second)->getUnderlyingValue() == NULL) {
					std::cerr<<"(iter->second)->getUnderlyingValue() is NULL"<<std::endl;
				}}
#endif

				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(comboArrays.objectArray, i,
						(iter->second)->getUnderlyingValue());

			}

			for (std::map<std::string, std::string>::iterator iter =
					properties.begin(); iter != properties.end(); ++iter, i++) {
				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(comboArrays.stringArray, i,
						SaxonProcessor::sxn_environ->env->NewStringUTF(
								(iter->first).c_str()));
				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(comboArrays.objectArray, i,
						SaxonProcessor::sxn_environ->env->NewStringUTF(
								(iter->second).c_str()));
			}

			 return comboArrays;

		} else {
		    return comboArrays;
		}
    }

JParameters SaxonProcessor::createParameterJArray2(std::map<std::string,XdmValue*> parameters){
		JParameters comboArrays;
		comboArrays.stringArray = NULL;
		comboArrays.objectArray = NULL;
		jclass objectClass = lookForClass(SaxonProcessor::sxn_environ->env,
				"java/lang/Object");
		jclass stringClass = lookForClass(SaxonProcessor::sxn_environ->env,
				"java/lang/String");

		int size = parameters.size();
#ifdef DEBUG
		std::cerr<<"Parameter size: "<<parameters.size()<<std::endl;
#endif
		if (size > 0) {

			comboArrays.objectArray = SaxonProcessor::sxn_environ->env->NewObjectArray((jint) size,
					objectClass, 0);
			comboArrays.stringArray = SaxonProcessor::sxn_environ->env->NewObjectArray((jint) size,
					stringClass, 0);
			int i = 0;
			for (std::map<std::string, XdmValue*>::iterator iter =
					parameters.begin(); iter != parameters.end(); ++iter, i++) {

#ifdef DEBUG
				std::cerr<<"map 1"<<std::endl;
				std::cerr<<"iter->first"<<(iter->first).c_str()<<std::endl;
#endif
				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(comboArrays.stringArray, i,
						SaxonProcessor::sxn_environ->env->NewStringUTF(
								(iter->first).c_str()));
#ifdef DEBUG
				std::string s1 = typeid(iter->second).name();
				std::cerr<<"Type of itr:"<<s1<<std::endl;

				if((iter->second) == NULL) {std::cerr<<"iter->second is null"<<std::endl;
				} else {
					std::cerr<<"getting underlying value"<<std::endl;
				jobject xx = (iter->second)->getUnderlyingValue();

				if(xx == NULL) {
					std::cerr<<"value failed"<<std::endl;
				} else {

					std::cerr<<"Type of value:"<<(typeid(xx).name())<<std::endl;
				}
				if((iter->second)->getUnderlyingValue() == NULL) {
					std::cerr<<"(iter->second)->getUnderlyingValue() is NULL"<<std::endl;
				}}
#endif

				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(comboArrays.objectArray, i,
						(iter->second)->getUnderlyingValue());

			}


			 return comboArrays;

		} else {
		    return comboArrays;
		}
    }


SaxonProcessor& SaxonProcessor::operator=( const SaxonProcessor& other ){
	versionClass = other.versionClass;
	procClass = other.procClass;
	saxonCAPIClass = other.saxonCAPIClass;
	cwd = other.cwd;
	proc = other.proc;
	//SaxonProcessor::sxn_environ= other.environ;
	parameters = other.parameters;
	configProperties = other.configProperties;
	licensei = other.licensei;
	exception = other.exception;
	return *this;
}

SaxonProcessor::SaxonProcessor(const SaxonProcessor &other) {
	versionClass = other.versionClass;
	procClass = other.procClass;
	saxonCAPIClass = other.saxonCAPIClass;
	cwd = other.cwd;
	proc = other.proc;
	//SaxonProcessor::sxn_environ= other.environ;
	parameters = other.parameters;
	configProperties = other.configProperties;
	licensei = other.licensei;
	exception = other.exception;
}

XsltProcessor * SaxonProcessor::newXsltProcessor(){
    return (new XsltProcessor(this, cwd));
}

Xslt30Processor * SaxonProcessor::newXslt30Processor(){
    return (new Xslt30Processor(this, cwd));
}

XQueryProcessor * SaxonProcessor::newXQueryProcessor(){
    return (new XQueryProcessor(this,cwd));
}

XPathProcessor * SaxonProcessor::newXPathProcessor(){
    return (new XPathProcessor(this, cwd));
}

SchemaValidator * SaxonProcessor::newSchemaValidator(){
	if(licensei) {
		return (new SchemaValidator(this, cwd));
	} else {
		std::cerr<<"\nError: Processor is not licensed for schema processing!"<<std::endl;
		return NULL;
	}
}



const char * SaxonProcessor::version() {
     if(versionStr == NULL) {

     	static jmethodID MID_version = (jmethodID)SaxonProcessor::sxn_environ->env->GetStaticMethodID(saxonCAPIClass, "getProductVersion", "(Lnet/sf/saxon/s9api/Processor;)Ljava/lang/String;");
    	if (!MID_version) {
        	std::cerr<<"\nError: MyClassInDll "<<"SaxonCAPI.getProductVersion()"<<" not found"<<std::endl;
        	return NULL;
    	}

    	jstring jstr = (jstring)(SaxonProcessor::sxn_environ->env->CallStaticObjectMethod(saxonCAPIClass, MID_version, proc));
         const char * tempVersionStr = SaxonProcessor::sxn_environ->env->GetStringUTFChars(jstr, NULL);
         int verLen = strlen(tempVersionStr)+22+strlen(CVERSION)+1;
         versionStr =new char [verLen];
         snprintf(versionStr, verLen, "Saxon/C %s %s %s", CVERSION, "running with", tempVersionStr);
         delete tempVersionStr;

    }

    return versionStr;
}

void SaxonProcessor::setcwd(const char* dir){
    cwd = std::string(dir);
}

const char* SaxonProcessor::getcwd(){
	return cwd.c_str();
}

void SaxonProcessor::setResourcesDirectory(const char* dir){
	//memset(&resources_dir[0], 0, sizeof(resources_dir));
 #if defined(__linux__) || defined (__APPLE__)
	strncat(_getResourceDirectory(), dir, strlen(dir));
 #else
	int destSize = strlen(dir) + strlen(dir);
	strncat_s(_getResourceDirectory(), destSize,dir, strlen(dir));

 #endif
}


void SaxonProcessor::setCatalog(const char* catalogFile, bool isTracing){
	jclass xmlResolverClass = lookForClass(SaxonProcessor::sxn_environ->env, "Lnet/sf/saxon/trans/XmlCatalogResolver;");
	static jmethodID catalogMID = SaxonProcessor::sxn_environ->env->GetStaticMethodID(xmlResolverClass, "setCatalog", "(Ljava/lang/String;Lnet/sf/saxon/Configuration;Z)V");
	
	if (!catalogMID) {
		std::cerr<<"\nError: Saxonc."<<"setCatalog()"<<" not found"<<std::endl;
        return;
        }
	if(catalogFile == NULL) {
		
		return;
	}
	static jmethodID configMID = SaxonProcessor::sxn_environ->env->GetMethodID(procClass, "getUnderlyingConfiguration", "()Lnet/sf/saxon/Configuration;");
	
	if (!configMID) {
		std::cerr<<"\nError: Saxonc."<<"getUnderlyingConfiguration()"<<" not found"<<std::endl;
        return;
        }


	if(!proc) {
		std::cout<<"proc is NULL in SaxonProcessorsetCatalog"<<std::endl;
		return;
	}

 	jobject configObj = SaxonProcessor::sxn_environ->env->CallObjectMethod(proc, configMID);
  	
	if(!configObj) {
		std::cout<<"proc is NULL in SaxonProcessor setcatalog - config obj"<<std::endl;
		return;
	}
	SaxonProcessor::sxn_environ->env->CallStaticVoidMethod(xmlResolverClass, catalogMID, SaxonProcessor::sxn_environ->env->NewStringUTF(catalogFile), configObj ,(jboolean)isTracing);
#ifdef DEBUG
	SaxonProcessor::sxn_environ->env->ExceptionDescribe();
#endif
}

const char * SaxonProcessor::getResourcesDirectory(){
	return _getResourceDirectory();
}


XdmNode * SaxonProcessor::parseXmlFromString(const char* source){
	
    jmethodID mID = (jmethodID)SaxonProcessor::sxn_environ->env->GetStaticMethodID(saxonCAPIClass, "parseXmlString", "(Ljava/lang/String;Lnet/sf/saxon/s9api/Processor;Lnet/sf/saxon/s9api/SchemaValidator;Ljava/lang/String;)Lnet/sf/saxon/s9api/XdmNode;");
    if (!mID) {
	std::cerr<<"\nError: Saxonc."<<"parseXmlString()"<<" not found"<<std::endl;
        return NULL;
    }
//TODO SchemaValidator

   jobject xdmNodei = SaxonProcessor::sxn_environ->env->CallStaticObjectMethod(saxonCAPIClass, mID, SaxonProcessor::sxn_environ->env->NewStringUTF(cwd.c_str()), proc, NULL, SaxonProcessor::sxn_environ->env->NewStringUTF(source));
	if(xdmNodei) {
		XdmNode * value = new XdmNode(xdmNodei);
		value->setProcessor(this);
		return value;
	}   else if (exceptionOccurred()) {
	   	exception = checkAndCreateException(saxonCAPIClass);
		exceptionClear();
       }
   
#ifdef DEBUG
	SaxonProcessor::sxn_environ->env->ExceptionDescribe();
#endif
 
   return NULL;
}

int SaxonProcessor::getNodeKind(jobject obj){
	jclass xdmNodeClass = lookForClass(SaxonProcessor::sxn_environ->env, "Lnet/sf/saxon/s9api/XdmNode;");
	static jmethodID nodeKindMID = (jmethodID) SaxonProcessor::sxn_environ->env->GetMethodID(xdmNodeClass,"getNodeKind", "()Lnet/sf/saxon/s9api/XdmNodeKind;");
	if (!nodeKindMID) {
		std::cerr << "Error: MyClassInDll." << "getNodeKind" << " not found\n"
				<< std::endl;
		return 0;
	} 

	jobject nodeKindObj = (SaxonProcessor::sxn_environ->env->CallObjectMethod(obj, nodeKindMID));
	if(!nodeKindObj) {
		
		return 0;
	}
	jclass xdmUtilsClass = lookForClass(SaxonProcessor::sxn_environ->env, "Lnet/sf/saxon/option/cpp/XdmUtils;");

	jmethodID mID2 = (jmethodID) SaxonProcessor::sxn_environ->env->GetStaticMethodID(xdmUtilsClass,"convertNodeKindType", "(Lnet/sf/saxon/s9api/XdmNodeKind;)I");

	if (!mID2) {
		std::cerr << "Error: MyClassInDll." << "convertNodeKindType" << " not found\n"
				<< std::endl;
		return 0;
	} 
	if(!nodeKindObj){
		return 0;	
	}
	int nodeKind = (int)(SaxonProcessor::sxn_environ->env->CallStaticIntMethod(xdmUtilsClass, mID2, nodeKindObj));
	return nodeKind;
}



XdmNode * SaxonProcessor::parseXmlFromFile(const char* source){

    jmethodID mID = (jmethodID)SaxonProcessor::sxn_environ->env->GetStaticMethodID(saxonCAPIClass, "parseXmlFile", "(Lnet/sf/saxon/s9api/Processor;Ljava/lang/String;Lnet/sf/saxon/s9api/SchemaValidator;Ljava/lang/String;)Lnet/sf/saxon/s9api/XdmNode;");
    if (!mID) {
	std::cerr<<"\nError: Saxonc.Dll "<<"parseXmlFile()"<<" not found"<<std::endl;
        return NULL;
    }
//TODO SchemaValidator
   jobject xdmNodei = SaxonProcessor::sxn_environ->env->CallStaticObjectMethod(saxonCAPIClass, mID, proc, SaxonProcessor::sxn_environ->env->NewStringUTF(cwd.c_str()),  NULL, SaxonProcessor::sxn_environ->env->NewStringUTF(source));
     if(exceptionOccurred()) {
	 	exception = checkAndCreateException(saxonCAPIClass);
	   exceptionClear();
	   		
     } else {

	XdmNode * value = new XdmNode(xdmNodei);
	value->setProcessor(this);
	return value;
   }
   return NULL;
}

XdmNode * SaxonProcessor::parseXmlFromUri(const char* source){

    jmethodID mID = (jmethodID)SaxonProcessor::sxn_environ->env->GetStaticMethodID(saxonCAPIClass, "parseXmlFile", "(Lnet/sf/saxon/s9api/Processor;Ljava/lang/String;Ljava/lang/String;)Lnet/sf/saxon/s9api/XdmNode;");
    if (!mID) {
	std::cerr<<"\nError: Saxonc.Dll "<<"parseXmlFromUri()"<<" not found"<<std::endl;
        return NULL;
    }
   jobject xdmNodei = SaxonProcessor::sxn_environ->env->CallStaticObjectMethod(saxonCAPIClass, mID, proc, SaxonProcessor::sxn_environ->env->NewStringUTF(""), SaxonProcessor::sxn_environ->env->NewStringUTF(source));
     if(exceptionOccurred()) {
	   exception = checkAndCreateException(saxonCAPIClass);
     } else {
	XdmNode * value = new XdmNode(xdmNodei);
	value->setProcessor(this);
	return value;
   }
   return NULL;
}


  /**
     * Set a configuration property.
     *
     * @param name of the property
     * @param value of the property
     */
    void SaxonProcessor::setConfigurationProperty(const char * name, const char * value){
	if(name != NULL){
		configProperties.insert(std::pair<std::string, std::string>(std::string(name), std::string((value == NULL ? "" : value))));
	}
    }

   void SaxonProcessor::clearConfigurationProperties(){
	configProperties.clear();
   }



void SaxonProcessor::release(){
 	if(SaxonProcessor::jvmCreatedCPP!=0) {
		SaxonProcessor::jvmCreatedCPP =0; 
		//std::cerr<<"SaxonProc: JVM finalized calling !"<<std::endl;
 		finalizeJavaRT (SaxonProcessor::sxn_environ->jvm);
 		
		//delete SaxonProcessor::sxn_environ;
	/*clearParameters();
	clearProperties();*/
} else {
//#ifdef DEBUG
     std::cerr<<"SaxonProc: JVM finalize not called!"<<std::endl;
//#endif
}
}




/* ========= Factory method for Xdm ======== */

    XdmAtomicValue * SaxonProcessor::makeStringValue(const char * str){
	    jobject obj = getJavaStringValue(SaxonProcessor::sxn_environ, str);
	    static jmethodID mssID_atomic = NULL;
	    if(mssID_atomic == NULL) {
	        mssID_atomic = (jmethodID)(SaxonProcessor::sxn_environ->env->GetMethodID (xdmAtomicClass, "<init>", "(Ljava/lang/String;)V"));
	    }
	    if(!mssID_atomic) {
	        std::cerr<<"XdmAtomic constructor (String)"<<std::endl;
	        return NULL;
	    }
	    jobject obj2 = (jobject)(SaxonProcessor::sxn_environ->env->NewObject(xdmAtomicClass, mssID_atomic, obj));
	    XdmAtomicValue * value = new XdmAtomicValue(obj2, "xs:string");
	    value->setProcessor(this);
	    return value;
    }

    XdmAtomicValue * SaxonProcessor::makeStringValue(std::string str){
	    jobject obj = getJavaStringValue(SaxonProcessor::sxn_environ, str.c_str());
	    static jmethodID msID_atomic = NULL;
	    if(msID_atomic == NULL) {
	        msID_atomic = (jmethodID)(SaxonProcessor::sxn_environ->env->GetMethodID (xdmAtomicClass, "<init>", "(Ljava/lang/String;)V"));
	    }
	    if(!msID_atomic) {
	        std::cerr<<"XdmAtomic constructor (String)"<<std::endl;
	        return NULL;
	    }
	    jobject obj2 = (jobject)(SaxonProcessor::sxn_environ->env->NewObject(xdmAtomicClass, msID_atomic, obj));
	    XdmAtomicValue * value = new XdmAtomicValue(obj2, "xs:string");
	    value->setProcessor(this);
	    return value;
    }

    XdmAtomicValue * SaxonProcessor::makeIntegerValue(int i){
	    //jobject obj = integerValue(*SaxonProcessor::sxn_environ, i);
	    static jmethodID miiID_atomic = NULL;
	    if(miiID_atomic == NULL) {
	        miiID_atomic = (jmethodID)(SaxonProcessor::sxn_environ->env->GetMethodID (xdmAtomicClass, "<init>", "(J)V"));
	    }
	    if(!miiID_atomic) {
	        std::cerr<<"XdmAtomic constructor (J)"<<std::endl;
	        return NULL;
	    }
	    jobject obj = (jobject)(SaxonProcessor::sxn_environ->env->NewObject(xdmAtomicClass, miiID_atomic, (jlong)i));
	    XdmAtomicValue * value = new XdmAtomicValue(obj, "Q{http://www.w3.org/2001/XMLSchema}integer");
	    value->setProcessor(this);
	    return value;
    }

    XdmAtomicValue * SaxonProcessor::makeDoubleValue(double d){
	    //jobject obj = doubleValue(*SaxonProcessor::sxn_environ, d);
	    static jmethodID mdID_atomic = NULL;
	    if(mdID_atomic == NULL) {
	        mdID_atomic = (jmethodID)(SaxonProcessor::sxn_environ->env->GetMethodID (xdmAtomicClass, "<init>", "(D)V"));
	    }
	    jobject obj = (jobject)(SaxonProcessor::sxn_environ->env->NewObject(xdmAtomicClass, mdID_atomic, (jdouble)d));
	    XdmAtomicValue * value = new XdmAtomicValue(obj, "Q{http://www.w3.org/2001/XMLSchema}double");
	    value->setProcessor(this);
	    return value;
    }

    XdmAtomicValue * SaxonProcessor::makeFloatValue(float d){
	    //jobject obj = doubleValue(*SaxonProcessor::sxn_environ, d);
	    static jmethodID mfID_atomic = NULL;
	    if(mfID_atomic == NULL) {
	        mfID_atomic = (jmethodID)(SaxonProcessor::sxn_environ->env->GetMethodID (xdmAtomicClass, "<init>", "(F)V"));
	    }
	    jobject obj = (jobject)(SaxonProcessor::sxn_environ->env->NewObject(xdmAtomicClass, mfID_atomic, (jfloat)d));
	    XdmAtomicValue * value = new XdmAtomicValue(obj, "Q{http://www.w3.org/2001/XMLSchema}float");
	    value->setProcessor(this);
	    return value;
    }

    XdmAtomicValue * SaxonProcessor::makeLongValue(long l){
	    //jobject obj = longValue(*SaxonProcessor::sxn_environ, l);
	    static jmethodID mlID_atomic = NULL;
	    if(mlID_atomic == NULL) {
	        mlID_atomic = (jmethodID)(SaxonProcessor::sxn_environ->env->GetMethodID (xdmAtomicClass, "<init>", "(J)V"));
        }
	    jobject obj = (jobject)(SaxonProcessor::sxn_environ->env->NewObject(xdmAtomicClass, mlID_atomic, (jlong)l));
	    XdmAtomicValue * value = new XdmAtomicValue(obj, "Q{http://www.w3.org/2001/XMLSchema}long");
	    value->setProcessor(this);
	    return value;
    }

    XdmAtomicValue * SaxonProcessor::makeBooleanValue(bool b){
	    //jobject obj = booleanValue(*SaxonProcessor::sxn_environ, b);
	    static jmethodID mID_atomic = NULL;
	    if(mID_atomic == NULL) {
	        mID_atomic = (jmethodID)(SaxonProcessor::sxn_environ->env->GetMethodID (xdmAtomicClass, "<init>", "(Z)V"));
	    }
	    jobject obj = (jobject)(SaxonProcessor::sxn_environ->env->NewObject(xdmAtomicClass, mID_atomic, (jboolean)b));
	    XdmAtomicValue * value = new XdmAtomicValue(obj, "Q{http://www.w3.org/2001/XMLSchema}boolean");
	    value->setProcessor(this);
	    return value;
    }

    XdmAtomicValue * SaxonProcessor::makeQNameValue(const char* str){
	    jobject val = xdmValueAsObj(SaxonProcessor::sxn_environ, "QName", str);
	    XdmAtomicValue * value = new XdmAtomicValue(val, "QName");
	    value->setProcessor(this);
	    return value;
    }

    XdmAtomicValue * SaxonProcessor::makeAtomicValue(const char * typei, const char * strValue){
	    jobject obj = xdmValueAsObj(SaxonProcessor::sxn_environ, typei, strValue);
	    XdmAtomicValue * value = new XdmAtomicValue(obj, typei);
	    value->setProcessor(this);
	    return value;
    }

    const char * SaxonProcessor::getStringValue(XdmItem * item){
	const char *result = stringValue(SaxonProcessor::sxn_environ, item->getUnderlyingValue());
#ifdef DEBUG
	if(result == NULL) {
		std::cout<<"getStringValue of XdmItem is NULL"<<std::endl;
	} else {
		std::cout<<"getStringValue of XdmItem is OK"<<std::endl;
	}
#endif
    
	return result;

   }

#if CVERSION_API_NO >= 123

   XdmArray * SaxonProcessor::makeArray(short * input, int length){
         if(input == NULL) {
            std::cerr<<"Error found when converting string to XdmArray"<<std::endl;
            return NULL;
         }
         jclass xdmArrayClass = lookForClass(SaxonProcessor::sxn_environ->env, "Lnet/sf/saxon/s9api/XdmArray;");
         static jmethodID mmssID = NULL;
         if(mmssID == NULL) {
            mmssID = (jmethodID)SaxonProcessor::sxn_environ->env->GetStaticMethodID(xdmArrayClass, "makeArray", "([S)Lnet/sf/saxon/s9api/XdmArray;");
         }
         if (!mmssID) {
            std::cerr<<"\nError: Saxonc.Dll "<<"makeArray([S)"<<" not found"<<std::endl;
            return NULL;
         }


         jshortArray sArray = NULL;

         sArray = SaxonProcessor::sxn_environ->env->NewShortArray((jint) length);
         jshort fill[length];
         for (int i=0; i<length; i++) {
            fill[i] =input[i];
         }
         SaxonProcessor::sxn_environ->env->SetShortArrayRegion(sArray, 0, length, fill);

         jobject xdmArrayi = SaxonProcessor::sxn_environ->env->CallStaticObjectMethod(xdmArrayClass, mmssID, sArray);
         if(!xdmArrayi) {
            std::cerr<<"Error found when converting string to XdmArray";
            return NULL;
         }

         if(exceptionOccurred()) {
         	   exception = checkAndCreateException(saxonCAPIClass);
         } else {
         	XdmArray * value = new XdmArray(xdmArrayi, length);
         	value->setProcessor(this);
         	return value;
         }
         return NULL;
   }


   XdmArray * SaxonProcessor::makeArray(int * input, int length){
           if(input == NULL) {
                   std::cerr<<"Error found when converting string to XdmArray";
                   return NULL;
           }
         jclass xdmArrayClass = lookForClass(SaxonProcessor::sxn_environ->env, "Lnet/sf/saxon/s9api/XdmArray;");
         static jmethodID mmiiID = NULL;
         if(mmiiID == NULL) {
            mmiiID = (jmethodID)SaxonProcessor::sxn_environ->env->GetStaticMethodID(xdmArrayClass, "makeArray", "([I)Lnet/sf/saxon/s9api/XdmArray;");
         }
         if (!mmiiID) {
         	std::cerr<<"\nError: Saxonc.Dll "<<"makeArray([I)"<<" not found"<<std::endl;
                 return NULL;
             }


             jintArray iArray = NULL;

             iArray = SaxonProcessor::sxn_environ->env->NewIntArray((jint) length);
             jint fill[length];
             for (int i=0; i<length; i++) {
                fill[i] =input[i];
             }
             SaxonProcessor::sxn_environ->env->SetIntArrayRegion(iArray, 0, length, fill);


            jobject xdmArrayi = SaxonProcessor::sxn_environ->env->CallStaticObjectMethod(xdmArrayClass, mmiiID, iArray);
        if(!xdmArrayi) {
            std::cerr<<"Error found when converting string to XdmArray";
            return NULL;
        }
              if(exceptionOccurred()) {
         	   exception = checkAndCreateException(saxonCAPIClass);
              } else {
         	XdmArray * value = new XdmArray(xdmArrayi, length);
         	value->setProcessor(this);
         	return value;
            }
            return NULL;

   }

   XdmArray * SaxonProcessor::makeArray(long * input, int length){
           if(input == NULL) {
                   std::cerr<<"Error found when converting string to XdmArray";
                   return NULL;
           }
         jclass xdmArrayClass = lookForClass(SaxonProcessor::sxn_environ->env, "Lnet/sf/saxon/s9api/XdmArray;");
         static jmethodID mmiID = NULL;
         if(mmiID == NULL) {
            mmiID = (jmethodID)SaxonProcessor::sxn_environ->env->GetStaticMethodID(xdmArrayClass, "makeArray", "([J)Lnet/sf/saxon/s9api/XdmArray;");
         }
             if (!mmiID) {
         	std::cerr<<"\nError: Saxonc.Dll "<<"makeArray([J)"<<" not found"<<std::endl;
                 return NULL;
             }


             jlongArray lArray = NULL;

             lArray = SaxonProcessor::sxn_environ->env->NewLongArray((jint) length);
             jlong fill[length];
             for (int i=0; i<length; i++) {
                fill[i] =input[i];
             }
             SaxonProcessor::sxn_environ->env->SetLongArrayRegion(lArray, 0, length, fill);


            jobject xdmArrayi = SaxonProcessor::sxn_environ->env->CallStaticObjectMethod(xdmArrayClass, mmiID, lArray);
        if(!xdmArrayi) {
            std::cerr<<"Error found when converting string to XdmArray";
            return NULL;
        }
              if(exceptionOccurred()) {
         	   exception = checkAndCreateException(saxonCAPIClass);
              } else {
         	XdmArray * value = new XdmArray(xdmArrayi, length);
         	value->setProcessor(this);
         	return value;
            }
            return NULL;
   }



   XdmArray * SaxonProcessor::makeArray(bool * input, int length){
         if(input == NULL) {
                   std::cerr<<"Error found when converting string to XdmArray";
                   return NULL;
         }
         jclass xdmArrayClass = lookForClass(SaxonProcessor::sxn_environ->env, "Lnet/sf/saxon/s9api/XdmArray;");
         static jmethodID mmbID = NULL;
         if(mmbID == NULL) {
            mmbID = (jmethodID)SaxonProcessor::sxn_environ->env->GetStaticMethodID(xdmArrayClass, "makeArray", "([Z)Lnet/sf/saxon/s9api/XdmArray;");
         }
         if (!mmbID) {
         	std::cerr<<"\nError: Saxonc.Dll "<<"makeArray([Z)"<<" not found"<<std::endl;
            return NULL;
         }


         jbooleanArray bArray = NULL;

         bArray = SaxonProcessor::sxn_environ->env->NewBooleanArray((jint) length);
         jboolean fill[length];
         for (int i=0; i<length; i++) {
            fill[i] =input[i];
         }
         SaxonProcessor::sxn_environ->env->SetBooleanArrayRegion(bArray, 0, length, fill);


         jobject xdmArrayi = SaxonProcessor::sxn_environ->env->CallStaticObjectMethod(xdmArrayClass, mmbID, bArray);
         if(!xdmArrayi) {
            std::cerr<<"Error found when converting string to XdmArray";
            return NULL;
         }
         if(exceptionOccurred()) {
            exception = checkAndCreateException(saxonCAPIClass);
         } else {
         	XdmArray * value = new XdmArray(xdmArrayi, length);
         	value->setProcessor(this);
         	return value;
            }
            return NULL;


   }



   XdmArray * SaxonProcessor::makeArray(const char ** input, int length){
        if(input == NULL || length <= 0) {
                std::cerr<<"Error found when converting string to XdmArray";
                return NULL;
        }
       	jobject obj = NULL;
       	jclass xdmArrayClass = lookForClass(SaxonProcessor::sxn_environ->env, "Lnet/sf/saxon/s9api/XdmArray;");
       	jmethodID mmID = (jmethodID)SaxonProcessor::sxn_environ->env->GetStaticMethodID(xdmArrayClass, "makeArray", "([Ljava/lang/Object;)Lnet/sf/saxon/s9api/XdmArray;");

        jmethodID mID_atomic = (jmethodID)(SaxonProcessor::sxn_environ->env->GetMethodID (xdmAtomicClass, "<init>", "(Ljava/lang/String;)V"));
        jobjectArray valueArray = NULL;
        jobject obj2 = NULL;
        valueArray = SaxonProcessor::sxn_environ->env->NewObjectArray((jint) length, xdmAtomicClass, 0);
        for(int i = 0; i< length; i++) {
            if(input[i] == NULL) {
                std::cerr<<"Error found when converting string to XdmArray";
                return NULL;
            }
            obj = getJavaStringValue(SaxonProcessor::sxn_environ, input[i]);
            obj2 = (jobject)(SaxonProcessor::sxn_environ->env->NewObject(xdmAtomicClass, mID_atomic, obj));
            SaxonProcessor::sxn_environ->env->SetObjectArrayElement(valueArray, i,obj2);
        }

        jobject xdmArrayi = SaxonProcessor::sxn_environ->env->CallStaticObjectMethod(xdmArrayClass, mmID, valueArray);
        if(!xdmArrayi) {
            std::cerr<<"Error found when converting string to XdmArray";
            return NULL;
        }

        if(exceptionOccurred()) {
            checkAndCreateException(saxonCAPIClass);
        } else {
            XdmArray * value = new XdmArray(xdmArrayi, length);
       	    value->setProcessor(this);
            return value;
        }
        return NULL;
       }




    XdmMap * makeMap(std::map<XdmAtomicValue *, XdmValue*> dataMap) {
             		jobjectArray keyArray = NULL;
             		jobjectArray valueArray = NULL;
             		jclass objectClass = lookForClass(SaxonProcessor::sxn_environ->env,
             				"java/lang/Object");

             		int size = dataMap.size();

             		if (size > 0) {

             			keyArray = SaxonProcessor::sxn_environ->env->NewObjectArray((jint) size,
             					objectClass, 0);
             			valueArray = SaxonProcessor::sxn_environ->env->NewObjectArray((jint) size,
             					objectClass, 0);
             			int i = 0;
             			for (std::map<XdmAtomicValue *, XdmValue*>::iterator iter =
             					dataMap.begin(); iter != dataMap.end(); ++iter, i++) {


                             SaxonProcessor::sxn_environ->env->SetObjectArrayElement(keyArray, i,
             						(iter->first)->getUnderlyingValue());

             				SaxonProcessor::sxn_environ->env->SetObjectArrayElement(valueArray, i,
             						(iter->second)->getUnderlyingValue());

             			}
                        jclass xdmUtilsClass = lookForClass(SaxonProcessor::sxn_environ->env, "net/sf/saxon/option/cpp/XdmUtils");
                        	jmethodID xmID = (jmethodID) SaxonProcessor::sxn_environ->env->GetStaticMethodID(xdmUtilsClass,"makeXdmMap",
                        					"([Lnet/sf/saxon/s9api/XdmAtomicValue;[Lnet/sf/saxon/s9api/XdmValue;)Lnet/sf/saxon/s9api/XdmMap;");
                        	if (!xmID) {
                        			std::cerr << "Error: SaxonDll." << "makeXdmMap"
                        				<< " not found\n" << std::endl;
                        			return;
                        		}


                        	jobject results = (jobject)(SaxonProcessor::sxn_environ->env->CallStaticObjectMethod(xdmUtilsClass, xmID,keyArray, valueArray));

             		}


             }

#endif


