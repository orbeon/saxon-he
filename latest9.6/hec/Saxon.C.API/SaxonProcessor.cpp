#ifndef __linux__ 
#ifndef __APPLE__
	#include "stdafx.h"
	#include <Tchar.h>
#endif
#endif


//#include "stdafx.h"
#include "SaxonProcessor.h"
#include "XdmValue.h"
#include "XdmItem.h"
#include "XdmNode.h"
#include "XdmAtomicValue.h"

#ifndef CPP_ONLY
/*#include "XsltProcessor.cpp"
#include "XQueryProcessor.cpp"
#include "XdmValue.cpp"*/
//#include "php_saxon.cpp"
#endif
//#define DEBUG
#ifdef DEBUG
#include <signal.h>
#endif
#include <stdio.h>


//jobject cpp;
const char * failure;
sxnc_environment * SaxonProcessor::environ = 0;
int SaxonProcessor::refCount = 0;

bool SaxonProcessor::exceptionOccurred(){
	return environ->env->ExceptionCheck();
}

void SaxonProcessor::exceptionClear(){
	environ->env->ExceptionClear();
}

SaxonApiException * SaxonProcessor::getException(){
	return exception;
}

SaxonProcessor::SaxonProcessor() {
    licensei = false;
    SaxonProcessor(licensei);
}

//int SaxonProcessor::jvmCreated=0;
//TODO - Exception handling in Saxon/C on both Java side and C++ side does not correctly handle multiple exceptions
//This needs working on in the next release
SaxonApiException * SaxonProcessor::checkForExceptionCPP(JNIEnv* env, jclass callingClass,  jobject callingObject){

    if (env->ExceptionCheck()) {
	string result1 = "";
	string errorCode = "";
	jthrowable exc = env->ExceptionOccurred();
       //env->ExceptionDescribe();
#ifdef DEBUG	
	env->ExceptionDescribe();
#endif
	 jclass exccls(env->GetObjectClass(exc));
        jclass clscls(env->FindClass("java/lang/Class"));

        jmethodID getName(env->GetMethodID(clscls, "getName", "()Ljava/lang/String;"));
        jstring name(static_cast<jstring>(env->CallObjectMethod(exccls, getName)));
        char const* utfName(env->GetStringUTFChars(name, 0));
	result1 = (string(utfName));
	//env->ReleaseStringUTFChars(name, utfName);

	 jmethodID  getMessage(env->GetMethodID(exccls, "getMessage", "()Ljava/lang/String;"));
	if(getMessage) {

		jstring message(static_cast<jstring>(env->CallObjectMethod(exc, getMessage)));
        	char const* utfMessage(env->GetStringUTFChars(message, 0));
		if(utfMessage != NULL) {
			result1 = (result1 + " : ") + utfMessage;
		}
		//cout<<"ExceptionMessage ZZZ: "<<result1<<endl;
		//env->ReleaseStringUTFChars(message,utfMessage);
		if(callingObject != NULL && result1.compare(0,36, "net.sf.saxon.s9api.SaxonApiException", 36) == 0){
			jmethodID  getErrorCodeID(env->GetMethodID(callingClass, "getExceptions", "()[Lnet/sf/saxon/option/cpp/SaxonExceptionForCpp;"));
			jclass saxonExceptionClass(env->FindClass("net/sf/saxon/option/cpp/SaxonExceptionForCpp"));
				if(getErrorCodeID){	
					jobjectArray saxonExceptionObject((jobjectArray)(env->CallObjectMethod(callingObject, getErrorCodeID)));
					if(saxonExceptionObject) {
						jmethodID lineNumID = env->GetMethodID(saxonExceptionClass, "getLinenumber", "()I");
						jmethodID ecID = env->GetMethodID(saxonExceptionClass, "getErrorCode", "()Ljava/lang/String;");
						jmethodID emID = env->GetMethodID(saxonExceptionClass, "getErrorMessage", "()Ljava/lang/String;");
						jmethodID typeID = env->GetMethodID(saxonExceptionClass, "isTypeError", "()Z");
						jmethodID staticID = env->GetMethodID(saxonExceptionClass, "isStaticError", "()Z");
						jmethodID globalID = env->GetMethodID(saxonExceptionClass, "isGlobalError", "()Z");


						int exLength = (int)env->GetArrayLength(saxonExceptionObject);
						SaxonApiException * saxonExceptions = new SaxonApiException();
						for(int i=0; i<exLength;i++){
							jobject exObj = env->GetObjectArrayElement(saxonExceptionObject, i);

							jstring errCode = (jstring)(env->CallObjectMethod(exObj, ecID));
							jstring errMessage = (jstring)(env->CallObjectMethod(exObj, emID));
							jboolean isType = (env->CallBooleanMethod(exObj, typeID));
							jboolean isStatic = (env->CallBooleanMethod(exObj, staticID));
							jboolean isGlobal = (env->CallBooleanMethod(exObj, globalID));
							saxonExceptions->add((errCode ? env->GetStringUTFChars(errCode,0) : NULL )  ,(errMessage ? env->GetStringUTFChars(errMessage,0) : NULL),(int)(env->CallIntMethod(exObj, lineNumID)), (bool)isType, (bool)isStatic, (bool)isGlobal);
							//env->ExceptionDescribe();
						}
						//env->ExceptionDescribe();
						env->ExceptionClear();
						return saxonExceptions;
					}
				}
		}
	}
	SaxonApiException * saxonExceptions = new SaxonApiException(NULL, result1.c_str());
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
    refCount++;
     if(!environ){
    environ = new sxnc_environment;//(sxnc_environment *)malloc(sizeof(sxnc_environment));

    /*
     * First of all, load required component.
     * By the time of JET initialization, all components should be loaded.
     */

    environ->myDllHandle = loadDefaultDll ();

    /*
     * Initialize JET run-time.
     * The handle of loaded component is used to retrieve Invocation API.
     */
    initDefaultJavaRT (&environ); 
    }
 
    versionClass = lookForClass(environ->env, "net/sf/saxon/Version");
    procClass = lookForClass(environ->env, "net/sf/saxon/s9api/Processor");
    saxonCAPIClass = lookForClass(environ->env, "net/sf/saxon/option/cpp/SaxonCAPI");
    
    proc = createSaxonProcessor (environ->env, procClass, "(Z)V", NULL, licensei);
	if(!proc) {
		std::cout<<"proc is NULL in SaxonProcessor constructor"<<std::endl;
	}
#ifdef DEBUG
     std::cerr<<"SaxonProc constructor: End"<<std::endl;
#endif
    xdmAtomicClass = lookForClass(environ->env, "net/sf/saxon/s9api/XdmAtomicValue");
}

SaxonProcessor::SaxonProcessor(const char * configFile){
    cwd="";
    versionStr = NULL;
    refCount++;
     if(!environ){
    environ = new sxnc_environment;//(sxnc_environment *)malloc(sizeof(sxnc_environment));

    /*
     * First of all, load required component.
     * By the time of JET initialization, all components should be loaded.
     */

    environ->myDllHandle = loadDefaultDll ();

    /*
     * Initialize JET run-time.
     * The handle of loaded component is used to retrieve Invocation API.
     */
    initDefaultJavaRT (&environ); 
    }
 
    versionClass = lookForClass(environ->env, "net/sf/saxon/Version");

    procClass = lookForClass(environ->env, "net/sf/saxon/s9api/Processor");
    saxonCAPIClass = lookForClass(environ->env, "net/sf/saxon/option/cpp/SaxonCAPI");

     static jmethodID mIDcreateProc = (jmethodID)environ->env->GetStaticMethodID(saxonCAPIClass,"createSaxonProcessor",
					"(Ljava/lang/String;)Lnet/sf/saxon/s9api/Processor;");
		if (!mIDcreateProc) {
			std::cerr << "Error: SaxonDll." << "getPrimitiveTypeName"
				<< " not found\n" << std::endl;
			return ;
		}
	proc = environ->env->CallStaticObjectMethod(saxonCAPIClass, mIDcreateProc,environ->env->NewStringUTF(configFile));
		
	if(!proc) {
		std::cerr << "Error: SaxonDll." << "processor is NULL in constructor(configFile)"<< std::endl;
		return ;	
	}
	
   
#ifdef DEBUG

     std::cerr<<"SaxonProc constructor(configFile)"<<std::endl;
#endif
    xdmAtomicClass = lookForClass(environ->env, "net/sf/saxon/s9api/XdmAtomicValue");
}

    SaxonProcessor::~SaxonProcessor(){
	clearConfigurationProperties();
	if(refCount<= 1) {
	 release();
	delete environ;
  	} else  {
		refCount--;	
	}

   }



void SaxonProcessor::applyConfigurationProperties(){
	if(configProperties.size()>0) {
		int size = configProperties.size();
		jclass stringClass = lookForClass(environ->env, "java/lang/String");
		jobjectArray stringArray1 = environ->env->NewObjectArray( (jint)size, stringClass, 0 );
		jobjectArray stringArray2 = environ->env->NewObjectArray( (jint)size, stringClass, 0 );
		static jmethodID mIDappConfig = NULL;
		if(mIDappConfig == NULL) {
			mIDappConfig = (jmethodID) environ->env->GetStaticMethodID(saxonCAPIClass,"applyToConfiguration",
					"(Lnet/sf/saxon/s9api/Processor;[Ljava/lang/String;[Ljava/lang/String;)V");
			if (!mIDappConfig) {
				std::cerr << "Error: SaxonDll." << "applyToConfiguration"
				<< " not found\n" << std::endl;
				return;
			}
		}
		int i=0;
		for(map<std::string, std::string >::iterator iter=configProperties.begin(); iter!=configProperties.end(); ++iter, i++) {
	     		environ->env->SetObjectArrayElement( stringArray1, i, environ->env->NewStringUTF( (iter->first).c_str()  ));
	     		environ->env->SetObjectArrayElement( stringArray2, i, environ->env->NewStringUTF((iter->second).c_str()) );
	   }
		
		environ->env->CallStaticObjectMethod(saxonCAPIClass, mIDappConfig,proc, stringArray1,stringArray2);
		if (exceptionOccurred()) {
	   		exception= checkForExceptionCPP(environ->env, saxonCAPIClass, NULL);
			exceptionClear();
      		 }
   

		
	}
}


SaxonProcessor& SaxonProcessor::operator=( const SaxonProcessor& other ){
	versionClass = other.versionClass;
	procClass = other.procClass;
	saxonCAPIClass = other.saxonCAPIClass;
	cwd = other.cwd;
	proc = other.proc;
	environ = other.environ;
	parameters = other.parameters;
	configProperties = other.configProperties;
	licensei = other.licensei;
	exception = other.exception;
	return *this;
}

XsltProcessor * SaxonProcessor::newXsltProcessor(){
    applyConfigurationProperties();
    return (new XsltProcessor(this, cwd));
}

XQueryProcessor * SaxonProcessor::newXQueryProcessor(){
    applyConfigurationProperties();
    return (new XQueryProcessor(this,cwd));
}

XPathProcessor * SaxonProcessor::newXPathProcessor(){
    applyConfigurationProperties();
    return (new XPathProcessor(this, cwd));
}

SchemaValidator * SaxonProcessor::newSchemaValidator(){
	if(licensei) {
 		applyConfigurationProperties();
		return (new SchemaValidator(this, cwd));
	} else {
		std::cerr<<"\nError: Processor is not licensed for schema processing!"<<std::endl;
		return NULL;
	}
}



const char * SaxonProcessor::version() {

     if(versionStr == NULL) {
     	jmethodID MID_version;

    	MID_version = (jmethodID)environ->env->GetStaticMethodID(saxonCAPIClass, "getProductVersion", "(Lnet/sf/saxon/s9api/Processor;)Ljava/lang/String;");
    	if (!MID_version) {
        	std::cerr<<"\nError: MyClassInDll "<<"SaxonCAPI.getProductVersion()"<<" not found"<<std::endl;
        	return NULL;
    	}

    	jstring jstr = (jstring)(environ->env->CallStaticObjectMethod(saxonCAPIClass, MID_version, proc));
    	versionStr = environ->env->GetStringUTFChars(jstr, NULL);
    }
    return versionStr;
}

void SaxonProcessor::setcwd(const char* dir){
    cwd = std::string(dir);
}

void SaxonProcessor::setResourcesDirectory(const char* dir){
	memset(&resources_dir[0], 0, sizeof(resources_dir));
	strncat(resources_dir, dir, strlen(dir));
}

const char * SaxonProcessor::getResourcesDirectory(){
	return resources_dir;
}


XdmNode * SaxonProcessor::parseXmlFromString(const char* source){
	
    jmethodID mID = (jmethodID)environ->env->GetStaticMethodID(saxonCAPIClass, "parseXmlString", "(Lnet/sf/saxon/s9api/Processor;Lnet/sf/saxon/s9api/SchemaValidator;Ljava/lang/String;)Lnet/sf/saxon/s9api/XdmNode;");
    if (!mID) {
	cerr<<"\nError: MyClassInDll "<<"parseXmlString()"<<" not found"<<endl;
        return NULL;
    }
//TODO SchemaValidator

   jobject xdmNodei = environ->env->CallStaticObjectMethod(saxonCAPIClass, mID, proc, NULL, environ->env->NewStringUTF(source));
	if(xdmNodei) {
		XdmNode * value = new XdmNode(xdmNodei);
		value->setProcessor(this);
		return value;
	}   else if (exceptionOccurred()) {
	   exception= checkForExceptionCPP(environ->env, saxonCAPIClass, NULL);
		exceptionClear();
       }
   
#ifdef DEBUG
	environ->env->ExceptionDescribe();
#endif
 
   return NULL;
}

int SaxonProcessor::getNodeKind(jobject obj){
	jclass xdmNodeClass = lookForClass(environ->env, "Lnet/sf/saxon/s9api/XdmNode;");
	jmethodID mID = (jmethodID) environ->env->GetMethodID(xdmNodeClass,"getNodeKind", "()Lnet/sf/saxon/s9api/XdmNodeKind;");
	if (!mID) {
		cerr << "Error: MyClassInDll." << "getNodeKind" << " not found\n"
				<< endl;
		return 0;
	} 

	jobject nodeKindObj = (environ->env->CallObjectMethod(obj, mID));
	if(!nodeKindObj) {
		std::cout<<"saxonPRoc nodeKind error"<<std::endl;
		return 0;
	}
	jclass xdmUtilsClass = lookForClass(environ->env, "Lnet/sf/saxon/option/cpp/XdmUtils;");


	jmethodID mID2 = (jmethodID) environ->env->GetStaticMethodID(xdmUtilsClass,"convertNodeKindType", "(Lnet/sf/saxon/s9api/XdmNodeKind;)I");

	if (!mID2) {
		cerr << "Error: MyClassInDll." << "convertNodeKindType" << " not found\n"
				<< endl;
		return 0;
	} 
	if(!nodeKindObj){
		return 0;	
	}
	int nodeKind = (long)(environ->env->CallStaticObjectMethod(xdmUtilsClass, mID2, nodeKindObj));

	return nodeKind;
}

XdmNode * SaxonProcessor::parseXmlFromFile(const char* source){

    jmethodID mID = (jmethodID)environ->env->GetStaticMethodID(saxonCAPIClass, "parseXmlFile", "(Lnet/sf/saxon/s9api/Processor;Ljava/lang/String;Lnet/sf/saxon/s9api/SchemaValidator;Ljava/lang/String;)Lnet/sf/saxon/s9api/XdmNode;");
    if (!mID) {
	cerr<<"\nError: MyClassInDll "<<"parseXmlFile()"<<" not found"<<endl;
        return NULL;
    }
//TODO SchemaValidator
   jobject xdmNodei = environ->env->CallStaticObjectMethod(saxonCAPIClass, mID, proc, environ->env->NewStringUTF(cwd.c_str()),  NULL, environ->env->NewStringUTF(source));
     if(exceptionOccurred()) {
	   exception= checkForExceptionCPP(environ->env, saxonCAPIClass, NULL);
	   exceptionClear();
	   		
     } else {

	XdmNode * value = new XdmNode(xdmNodei);
	value->setProcessor(this);
	return value;
   }
   return NULL;
}

XdmNode * SaxonProcessor::parseXmlFromUri(const char* source){

    jmethodID mID = (jmethodID)environ->env->GetStaticMethodID(saxonCAPIClass, "parseXmlFile", "(Lnet/sf/saxon/s9api/Processor;Ljava/lang/String;Ljava/lang/String;)Lnet/sf/saxon/s9api/XdmNode;");
    if (!mID) {
	cerr<<"\nError: MyClassInDll "<<"parseXmlFromUri()"<<" not found"<<endl;
        return NULL;
    }
   jobject xdmNodei = environ->env->CallStaticObjectMethod(saxonCAPIClass, mID, proc, environ->env->NewStringUTF(""), environ->env->NewStringUTF(source));
     if(exceptionOccurred()) {
	   exception= checkForExceptionCPP(environ->env, saxonCAPIClass, NULL);
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
	configProperties.insert(std::pair<std::string, std::string>(std::string(name), std::string(value)));
    }

   void SaxonProcessor::clearConfigurationProperties(){
	configProperties.clear();
   }


void SaxonProcessor::release(){
 	finalizeJavaRT (environ->jvm);
#ifdef DEBUG
     std::cerr<<"SaxonProc: release called"<<std::endl;
#endif
	/*delete environ;
	//free(proc);
	clearParameters();
	clearProperties();*/
}




/* ========= Factory method for Xdm ======== */

    XdmAtomicValue * SaxonProcessor::makeStringValue(std::string str){
	jobject obj = getJavaStringValue(*environ, str.c_str());
	jmethodID mID_atomic = (jmethodID)(environ->env->GetMethodID (xdmAtomicClass, "<init>", "(Ljava/lang/String;)V"));
	jobject obj2 = (jobject)(environ->env->NewObject(xdmAtomicClass, mID_atomic, obj));
	XdmAtomicValue * value = new XdmAtomicValue(obj2, "xs:string");
	value->setProcessor(this);
	return value;
    }

    XdmAtomicValue * SaxonProcessor::makeStringValue(const char * str){
	jobject obj = getJavaStringValue(*environ, str);
	jmethodID mID_atomic = (jmethodID)(environ->env->GetMethodID (xdmAtomicClass, "<init>", "(Ljava/lang/String;)V"));
	jobject obj2 = (jobject)(environ->env->NewObject(xdmAtomicClass, mID_atomic, obj));
	XdmAtomicValue * value = new XdmAtomicValue(obj2, "xs:string");
	value->setProcessor(this);
	return value;
    }

    XdmAtomicValue * SaxonProcessor::makeIntegerValue(int i){
	//jobject obj = integerValue(*environ, i);
	jmethodID mID_atomic = (jmethodID)(environ->env->GetMethodID (xdmAtomicClass, "<init>", "(J)V"));
	

	jobject obj = (jobject)(environ->env->NewObject(xdmAtomicClass, mID_atomic, (jlong)i));
	XdmAtomicValue * value = new XdmAtomicValue(obj, "Q{http://www.w3.org/2001/XMLSchema}integer");
	value->setProcessor(this);
	return value;
    }

    XdmAtomicValue * SaxonProcessor::makeDoubleValue(double d){
	//jobject obj = doubleValue(*environ, d);
	jmethodID mID_atomic = (jmethodID)(environ->env->GetMethodID (xdmAtomicClass, "<init>", "(D)V"));
	jobject obj = (jobject)(environ->env->NewObject(xdmAtomicClass, mID_atomic, (jdouble)d));
	XdmAtomicValue * value = new XdmAtomicValue(obj, "Q{http://www.w3.org/2001/XMLSchema}double");
	value->setProcessor(this);
	return value;
    }

    XdmAtomicValue * SaxonProcessor::makeFloatValue(float d){
	//jobject obj = doubleValue(*environ, d);
	jmethodID mID_atomic = (jmethodID)(environ->env->GetMethodID (xdmAtomicClass, "<init>", "(F)V"));
	jobject obj = (jobject)(environ->env->NewObject(xdmAtomicClass, mID_atomic, (jfloat)d));
	XdmAtomicValue * value = new XdmAtomicValue(obj, "Q{http://www.w3.org/2001/XMLSchema}float");
	value->setProcessor(this);
	return value;
    }

    XdmAtomicValue * SaxonProcessor::makeLongValue(long l){
	//jobject obj = longValue(*environ, l);
	jmethodID mID_atomic = (jmethodID)(environ->env->GetMethodID (xdmAtomicClass, "<init>", "(J)V"));
	jobject obj = (jobject)(environ->env->NewObject(xdmAtomicClass, mID_atomic, (jlong)l));
	XdmAtomicValue * value = new XdmAtomicValue(obj, "Q{http://www.w3.org/2001/XMLSchema}long");
	value->setProcessor(this);
	return value;
    }

    XdmAtomicValue * SaxonProcessor::makeBooleanValue(bool b){
	//jobject obj = booleanValue(*environ, b);
	jmethodID mID_atomic = (jmethodID)(environ->env->GetMethodID (xdmAtomicClass, "<init>", "(Z)V"));
	jobject obj = (jobject)(environ->env->NewObject(xdmAtomicClass, mID_atomic, (jboolean)b));
	XdmAtomicValue * value = new XdmAtomicValue(obj, "Q{http://www.w3.org/2001/XMLSchema}boolean");
	value->setProcessor(this);
	return value;
    }

    XdmAtomicValue * SaxonProcessor::makeQNameValue(std::string str){
	jobject val = xdmValueAsObj(*environ, "QName", str.c_str());
	XdmAtomicValue * value = new XdmAtomicValue(val, "QName");
	value->setProcessor(this);
	return value;
    }

    XdmAtomicValue * SaxonProcessor::makeAtomicValue(std::string typei, std::string strValue){
	jobject obj = xdmValueAsObj(*environ, typei.c_str(), strValue.c_str());
	XdmAtomicValue * value = new XdmAtomicValue(obj, typei);
	value->setProcessor(this);
	return value;
    }

    const char * SaxonProcessor::getStringValue(XdmItem * item){
	const char *result = stringValue(*environ, item->getUnderlyingValue(this));
#ifdef DEBUG
	if(result == NULL) {
		std::cout<<"getStringValue of XdmItem is NULL"<<std::endl;
	} else {
		std::cout<<"getStringValue of XdmItem is OK"<<std::endl;
	}
#endif
	return result;
    }




