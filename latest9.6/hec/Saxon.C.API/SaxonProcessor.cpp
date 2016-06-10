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
sxnc_environment * sxn_environ = 0;
int refCount = 0;
int jvmCreatedCPP=0;

bool SaxonProcessor::exceptionOccurred(){
	return sxn_environ->env->ExceptionCheck();
}

void SaxonProcessor::exceptionClear(){
	sxn_environ->env->ExceptionClear();
}

SaxonApiException * SaxonProcessor::getException(){
	return exception;
}

SaxonProcessor::SaxonProcessor() {
    licensei = false;
    SaxonProcessor(licensei);
}



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
		
		//env->ReleaseStringUTFChars(message,utfMessage);
		if(callingObject != NULL && result1.compare(0,36, "net.sf.saxon.option.cpp.SaxonCException", 36) == 0){
			jmethodID  getErrorCodeID(env->GetMethodID(callingClass, "getExceptions", "()[Lnet/sf/saxon/option/cpp/SaxonCException;"));
			jclass saxonExceptionClass(env->FindClass("net/sf/saxon/option/cpp/SaxonCException"));
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


     if(jvmCreatedCPP == 0){
	jvmCreatedCPP=1;
    sxn_environ= new sxnc_environment;//(sxnc_environment *)malloc(sizeof(sxnc_environment));


    /*
     * First of all, load required component.
     * By the time of JET initialization, all components should be loaded.
     */

    sxn_environ->myDllHandle = loadDefaultDll ();

    /*
     * Initialize JET run-time.
     * The handle of loaded component is used to retrieve Invocation API.
     */
    initDefaultJavaRT (&sxn_environ); 
    } else {
#ifdef DEBUG
     std::cerr<<"SaxonProc constructor: jvm exists!"<<std::endl;
#endif

}

 
    versionClass = lookForClass(sxn_environ->env, "net/sf/saxon/Version");
    procClass = lookForClass(sxn_environ->env, "net/sf/saxon/s9api/Processor");
    saxonCAPIClass = lookForClass(sxn_environ->env, "net/sf/saxon/option/cpp/SaxonCAPI");
    
    proc = createSaxonProcessor (sxn_environ->env, procClass, "(Z)V", NULL, licensei);
	if(!proc) {
		std::cout<<"proc is NULL in SaxonProcessor constructor"<<std::endl;
	}

    xdmAtomicClass = lookForClass(sxn_environ->env, "net/sf/saxon/s9api/XdmAtomicValue");
}

SaxonProcessor::SaxonProcessor(const char * configFile){
    cwd="";
    versionStr = NULL;
    refCount++;

    if(jvmCreatedCPP == 0){
	jvmCreatedCPP=1;
    //sxn_environ= new sxnc_environment;
	sxn_environ= (sxnc_environment *)malloc(sizeof(sxnc_environment));

    /*
     * First of all, load required component.
     * By the time of JET initialization, all components should be loaded.
     */

    sxn_environ->myDllHandle = loadDefaultDll ();

    /*
     * Initialize JET run-time.
     * The handle of loaded component is used to retrieve Invocation API.
     */
    initDefaultJavaRT (&sxn_environ); 
    }
 
    versionClass = lookForClass(sxn_environ->env, "net/sf/saxon/Version");

    procClass = lookForClass(sxn_environ->env, "net/sf/saxon/s9api/Processor");
    saxonCAPIClass = lookForClass(sxn_environ->env, "net/sf/saxon/option/cpp/SaxonCAPI");

     static jmethodID mIDcreateProc = (jmethodID)sxn_environ->env->GetStaticMethodID(saxonCAPIClass,"createSaxonProcessor",
					"(Ljava/lang/String;)Lnet/sf/saxon/s9api/Processor;");
		if (!mIDcreateProc) {
			std::cerr << "Error: SaxonDll." << "getPrimitiveTypeName"
				<< " not found\n" << std::endl;
			return ;
		}
	proc = sxn_environ->env->CallStaticObjectMethod(saxonCAPIClass, mIDcreateProc,sxn_environ->env->NewStringUTF(configFile));
		
	if(!proc) {
		std::cerr << "Error: SaxonDll." << "processor is NULL in constructor(configFile)"<< std::endl;
		return ;	
	}
	
   
#ifdef DEBUG

     std::cerr<<"SaxonProc constructor(configFile)"<<std::endl;
#endif
    xdmAtomicClass = lookForClass(sxn_environ->env, "net/sf/saxon/s9api/XdmAtomicValue");
}

    SaxonProcessor::~SaxonProcessor(){
	clearConfigurationProperties();
	if(versionStr != NULL) {
		delete versionStr;
	}
	refCount--;	//This might be redundant due to the bug fix 2670
   }



void SaxonProcessor::applyConfigurationProperties(){
	if(configProperties.size()>0) {
		int size = configProperties.size();
		jclass stringClass = lookForClass(sxn_environ->env, "java/lang/String");
		jobjectArray stringArray1 = sxn_environ->env->NewObjectArray( (jint)size, stringClass, 0 );
		jobjectArray stringArray2 = sxn_environ->env->NewObjectArray( (jint)size, stringClass, 0 );
		static jmethodID mIDappConfig = NULL;
		if(mIDappConfig == NULL) {
			mIDappConfig = (jmethodID) sxn_environ->env->GetStaticMethodID(saxonCAPIClass,"applyToConfiguration",
					"(Lnet/sf/saxon/s9api/Processor;[Ljava/lang/String;[Ljava/lang/String;)V");
			if (!mIDappConfig) {
				std::cerr << "Error: SaxonDll." << "applyToConfiguration"
				<< " not found\n" << std::endl;
				return;
			}
		}
		int i=0;
		for(map<std::string, std::string >::iterator iter=configProperties.begin(); iter!=configProperties.end(); ++iter, i++) {
	     		sxn_environ->env->SetObjectArrayElement( stringArray1, i, sxn_environ->env->NewStringUTF( (iter->first).c_str()  ));
	     		sxn_environ->env->SetObjectArrayElement( stringArray2, i, sxn_environ->env->NewStringUTF((iter->second).c_str()) );
	   }
		
		sxn_environ->env->CallStaticObjectMethod(saxonCAPIClass, mIDappConfig,proc, stringArray1,stringArray2);
		if (exceptionOccurred()) {
	   		exception= checkForExceptionCPP(sxn_environ->env, saxonCAPIClass, NULL);
			exceptionClear();
      		 }
   
 	  sxn_environ->env->DeleteLocalRef(stringArray1);
	  sxn_environ->env->DeleteLocalRef(stringArray2);
		
	}
}


SaxonProcessor& SaxonProcessor::operator=( const SaxonProcessor& other ){
	versionClass = other.versionClass;
	procClass = other.procClass;
	saxonCAPIClass = other.saxonCAPIClass;
	cwd = other.cwd;
	proc = other.proc;
	//sxn_environ= other.environ;
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
     	static jmethodID MID_version = (jmethodID)sxn_environ->env->GetStaticMethodID(saxonCAPIClass, "getProductVersion", "(Lnet/sf/saxon/s9api/Processor;)Ljava/lang/String;");
    	if (!MID_version) {
        	std::cerr<<"\nError: MyClassInDll "<<"SaxonCAPI.getProductVersion()"<<" not found"<<std::endl;
        	return NULL;
    	}

    	jstring jstr = (jstring)(sxn_environ->env->CallStaticObjectMethod(saxonCAPIClass, MID_version, proc));
    	versionStr = sxn_environ->env->GetStringUTFChars(jstr, NULL);
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
	return getResourceDirectory();
}


XdmNode * SaxonProcessor::parseXmlFromString(const char* source){
	
    jmethodID mID = (jmethodID)sxn_environ->env->GetStaticMethodID(saxonCAPIClass, "parseXmlString", "(Lnet/sf/saxon/s9api/Processor;Lnet/sf/saxon/s9api/SchemaValidator;Ljava/lang/String;)Lnet/sf/saxon/s9api/XdmNode;");
    if (!mID) {
	cerr<<"\nError: MyClassInDll "<<"parseXmlString()"<<" not found"<<endl;
        return NULL;
    }
//TODO SchemaValidator

   jobject xdmNodei = sxn_environ->env->CallStaticObjectMethod(saxonCAPIClass, mID, proc, NULL, sxn_environ->env->NewStringUTF(source));
	if(xdmNodei) {
		XdmNode * value = new XdmNode(xdmNodei);
		value->setProcessor(this);
		return value;
	}   else if (exceptionOccurred()) {
	   exception= checkForExceptionCPP(sxn_environ->env, saxonCAPIClass, NULL);
		exceptionClear();
       }
   
#ifdef DEBUG
	sxn_environ->env->ExceptionDescribe();
#endif
 
   return NULL;
}

int SaxonProcessor::getNodeKind(jobject obj){
	jclass xdmNodeClass = lookForClass(sxn_environ->env, "Lnet/sf/saxon/s9api/XdmNode;");
	static jmethodID nodeKindMID = (jmethodID) sxn_environ->env->GetMethodID(xdmNodeClass,"getNodeKind", "()Lnet/sf/saxon/s9api/XdmNodeKind;");
	if (!nodeKindMID) {
		cerr << "Error: MyClassInDll." << "getNodeKind" << " not found\n"
				<< endl;
		return 0;
	} 

	jobject nodeKindObj = (sxn_environ->env->CallObjectMethod(obj, nodeKindMID));
	if(!nodeKindObj) {
		std::cout<<"saxonProc nodeKind error"<<std::endl;
		return 0;
	}
	jclass xdmUtilsClass = lookForClass(sxn_environ->env, "Lnet/sf/saxon/option/cpp/XdmUtils;");


	jmethodID mID2 = (jmethodID) sxn_environ->env->GetStaticMethodID(xdmUtilsClass,"convertNodeKindType", "(Lnet/sf/saxon/s9api/XdmNodeKind;)I");

	if (!mID2) {
		cerr << "Error: MyClassInDll." << "convertNodeKindType" << " not found\n"
				<< endl;
		return 0;
	} 
	if(!nodeKindObj){
		return 0;	
	}
	int nodeKind = (long)(sxn_environ->env->CallStaticObjectMethod(xdmUtilsClass, mID2, nodeKindObj));

	return nodeKind;
}

XdmNode * SaxonProcessor::parseXmlFromFile(const char* source){

    jmethodID mID = (jmethodID)sxn_environ->env->GetStaticMethodID(saxonCAPIClass, "parseXmlFile", "(Lnet/sf/saxon/s9api/Processor;Ljava/lang/String;Lnet/sf/saxon/s9api/SchemaValidator;Ljava/lang/String;)Lnet/sf/saxon/s9api/XdmNode;");
    if (!mID) {
	cerr<<"\nError: MyClassInDll "<<"parseXmlFile()"<<" not found"<<endl;
        return NULL;
    }
//TODO SchemaValidator
   jobject xdmNodei = sxn_environ->env->CallStaticObjectMethod(saxonCAPIClass, mID, proc, sxn_environ->env->NewStringUTF(cwd.c_str()),  NULL, sxn_environ->env->NewStringUTF(source));
     if(exceptionOccurred()) {
	   exception= checkForExceptionCPP(sxn_environ->env, saxonCAPIClass, NULL);
	   exceptionClear();
	   		
     } else {

	XdmNode * value = new XdmNode(xdmNodei);
	value->setProcessor(this);
	return value;
   }
   return NULL;
}

XdmNode * SaxonProcessor::parseXmlFromUri(const char* source){

    jmethodID mID = (jmethodID)sxn_environ->env->GetStaticMethodID(saxonCAPIClass, "parseXmlFile", "(Lnet/sf/saxon/s9api/Processor;Ljava/lang/String;Ljava/lang/String;)Lnet/sf/saxon/s9api/XdmNode;");
    if (!mID) {
	cerr<<"\nError: MyClassInDll "<<"parseXmlFromUri()"<<" not found"<<endl;
        return NULL;
    }
   jobject xdmNodei = sxn_environ->env->CallStaticObjectMethod(saxonCAPIClass, mID, proc, sxn_environ->env->NewStringUTF(""), sxn_environ->env->NewStringUTF(source));
     if(exceptionOccurred()) {
	   exception= checkForExceptionCPP(sxn_environ->env, saxonCAPIClass, NULL);
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
	if(jvmCreatedCPP!=0) {
		jvmCreatedCPP =0;

 		finalizeJavaRT (sxn_environ->jvm);

		//delete sxn_environ;
	/*clearParameters();
	clearProperties();*/
} else {
#ifdef DEBUG
     std::cerr<<"SaxonProc: JVM finalize not called!"<<std::endl;
#endif
}
}




/* ========= Factory method for Xdm ======== */

    XdmAtomicValue * SaxonProcessor::makeStringValue(std::string str){
	jobject obj = getJavaStringValue(*sxn_environ, str.c_str());
	jmethodID mID_atomic = (jmethodID)(sxn_environ->env->GetMethodID (xdmAtomicClass, "<init>", "(Ljava/lang/String;)V"));
	jobject obj2 = (jobject)(sxn_environ->env->NewObject(xdmAtomicClass, mID_atomic, obj));
	XdmAtomicValue * value = new XdmAtomicValue(obj2, "xs:string");
	value->setProcessor(this);
	return value;
    }

    XdmAtomicValue * SaxonProcessor::makeStringValue(const char * str){
	jobject obj = getJavaStringValue(*sxn_environ, str);
	jmethodID mID_atomic = (jmethodID)(sxn_environ->env->GetMethodID (xdmAtomicClass, "<init>", "(Ljava/lang/String;)V"));
	jobject obj2 = (jobject)(sxn_environ->env->NewObject(xdmAtomicClass, mID_atomic, obj));
	XdmAtomicValue * value = new XdmAtomicValue(obj2, "xs:string");
	value->setProcessor(this);
	return value;
    }

    XdmAtomicValue * SaxonProcessor::makeIntegerValue(int i){
	//jobject obj = integerValue(*sxn_environ, i);
	jmethodID mID_atomic = (jmethodID)(sxn_environ->env->GetMethodID (xdmAtomicClass, "<init>", "(J)V"));
	

	jobject obj = (jobject)(sxn_environ->env->NewObject(xdmAtomicClass, mID_atomic, (jlong)i));
	XdmAtomicValue * value = new XdmAtomicValue(obj, "Q{http://www.w3.org/2001/XMLSchema}integer");
	value->setProcessor(this);
	return value;
    }

    XdmAtomicValue * SaxonProcessor::makeDoubleValue(double d){
	//jobject obj = doubleValue(*sxn_environ, d);
	jmethodID mID_atomic = (jmethodID)(sxn_environ->env->GetMethodID (xdmAtomicClass, "<init>", "(D)V"));
	jobject obj = (jobject)(sxn_environ->env->NewObject(xdmAtomicClass, mID_atomic, (jdouble)d));
	XdmAtomicValue * value = new XdmAtomicValue(obj, "Q{http://www.w3.org/2001/XMLSchema}double");
	value->setProcessor(this);
	return value;
    }

    XdmAtomicValue * SaxonProcessor::makeFloatValue(float d){
	//jobject obj = doubleValue(*sxn_environ, d);
	jmethodID mID_atomic = (jmethodID)(sxn_environ->env->GetMethodID (xdmAtomicClass, "<init>", "(F)V"));
	jobject obj = (jobject)(sxn_environ->env->NewObject(xdmAtomicClass, mID_atomic, (jfloat)d));
	XdmAtomicValue * value = new XdmAtomicValue(obj, "Q{http://www.w3.org/2001/XMLSchema}float");
	value->setProcessor(this);
	return value;
    }

    XdmAtomicValue * SaxonProcessor::makeLongValue(long l){
	//jobject obj = longValue(*sxn_environ, l);
	jmethodID mID_atomic = (jmethodID)(sxn_environ->env->GetMethodID (xdmAtomicClass, "<init>", "(J)V"));
	jobject obj = (jobject)(sxn_environ->env->NewObject(xdmAtomicClass, mID_atomic, (jlong)l));
	XdmAtomicValue * value = new XdmAtomicValue(obj, "Q{http://www.w3.org/2001/XMLSchema}long");
	value->setProcessor(this);
	return value;
    }

    XdmAtomicValue * SaxonProcessor::makeBooleanValue(bool b){
	//jobject obj = booleanValue(*sxn_environ, b);
	jmethodID mID_atomic = (jmethodID)(sxn_environ->env->GetMethodID (xdmAtomicClass, "<init>", "(Z)V"));
	jobject obj = (jobject)(sxn_environ->env->NewObject(xdmAtomicClass, mID_atomic, (jboolean)b));
	XdmAtomicValue * value = new XdmAtomicValue(obj, "Q{http://www.w3.org/2001/XMLSchema}boolean");
	value->setProcessor(this);
	return value;
    }

    XdmAtomicValue * SaxonProcessor::makeQNameValue(std::string str){
	jobject val = xdmValueAsObj(*sxn_environ, "QName", str.c_str());
	XdmAtomicValue * value = new XdmAtomicValue(val, "QName");
	value->setProcessor(this);
	return value;
    }

    XdmAtomicValue * SaxonProcessor::makeAtomicValue(std::string typei, std::string strValue){
	jobject obj = xdmValueAsObj(*sxn_environ, typei.c_str(), strValue.c_str());
	XdmAtomicValue * value = new XdmAtomicValue(obj, typei);
	value->setProcessor(this);
	return value;
    }

    const char * SaxonProcessor::getStringValue(XdmItem * item){
	const char *result = stringValue(*sxn_environ, item->getUnderlyingValue(this));
#ifdef DEBUG
	if(result == NULL) {
		std::cout<<"getStringValue of XdmItem is NULL"<<std::endl;
	} else {
		std::cout<<"getStringValue of XdmItem is OK"<<std::endl;
	}
#endif
	return result;
    }




