#include "SaxonCGlue.h"

jobject cpp;
/*
* Set Dll name. Also set the saxon resources directory. 
* If the SAXONC_HOME sxnc_environmental variable is set then use that as base.
*/
void setDllname(){
	if(getenv("SAXONC_HOME")!= NULL) {

		char * env = getenv("SAXONC_HOME");
		size_t env_len = strlen(env);
		size_t name_len =  15;
		size_t rDir_len = 11;
		//dllname =malloc(sizeof(char)*name_len);
		//resources_dir =malloc(sizeof(char)*rDir_len);
		memset(&dllname[0], 0, sizeof(dllname));
		memset(&resources_dir[0], 0, sizeof(resources_dir));
		strncat(resources_dir, env,  env_len);
		strncat(resources_dir, "/saxon-data", rDir_len);
		strncat(dllname, env, env_len);
		strncat(dllname, "/libsaxonhec.so", name_len); //rename according to product edition (-hec or -pec)
#ifdef DEBUG	
		printf("resources_dir: %s\n", resources_dir);	
		printf("envDir: %s\n", env);
		printf("size of env %i\n", strlen(env));
		printf("size of dllname %i\n", strlen(dllname));
		printf("dllName: %s\n", dllname);
		printf("resources_dir: %s\n", resources_dir);
#endif
	}
#ifdef DEBUG	
		printf("resources_dir: %s\n", resources_dir);	
		printf("size of dllname %i\n", strlen(dllname));
		printf("dllName: %s\n", dllname);
		printf("resources_dir: %s\n", resources_dir);
		printf("size of resources dir %i\n", strlen(resources_dir));
#endif 	

}

char * getDllname(){
	return dllname;
}

/*
 * Load dll using the default setting in Saxon/C
 * Recommended method to use to load library
 */
HANDLE loadDefaultDll(){
	return loadDll(NULL);
}


/*
 * Load dll.
 */
HANDLE loadDll(char* name)
{
	if(name == NULL) {
		setDllname();
		name = getDllname();
		//perror("Error1: ");
	}

#if !(defined (__linux__) || (defined (__APPLE__) && defined(__MACH__)))
    HANDLE hDll = LoadLibrary (_T(name)); // Used for windows only
#else
    HANDLE hDll = LoadLibrary(name);
#endif

    if (!hDll) {
        printf ("Unable to load %s\n", name);
	perror("Error: ");
        exit(1);
    }
#ifdef DEBUG
    printf ("%s loaded\n", name);
#endif

    return hDll;
}



jint (JNICALL * JNI_GetDefaultJavaVMInitArgs_func) (void *args);
jint (JNICALL * JNI_CreateJavaVM_func) (JavaVM **pvm, void **penv, void *args);



void initDefaultJavaRT(sxnc_environment ** env){
	sxnc_environment *environ = *env;
	initJavaRT((environ->myDllHandle), &(environ->jvm), &(environ->env));
}

/*
 * Initialize JET run-time.
 */
void initJavaRT(HANDLE myDllHandle, JavaVM** pjvm, JNIEnv** penv)
{
    //perror ("initJavaRT - load Saxon/C library\n");
    int result;
    JavaVMInitArgs args;
    JNI_GetDefaultJavaVMInitArgs_func =
    (jint (JNICALL *) (void *args))
#if !(defined (__linux__) || (defined (__APPLE__) && defined(__MACH__)))
    GetProcAddress ((HMODULE)myDllHandle, "JNI_GetDefaultJavaVMInitArgs");
#else
    GetProcAddress (myDllHandle, "JNI_GetDefaultJavaVMInitArgs");
#endif
    
    JNI_CreateJavaVM_func =
    (jint (JNICALL *) (JavaVM **pvm, void **penv, void *args))
    
#if !(defined (__linux__) || (defined (__APPLE__) && defined(__MACH__)))
    GetProcAddress ((HMODULE)myDllHandle, "JNI_CreateJavaVM");
#else
    GetProcAddress (myDllHandle, "JNI_CreateJavaVM");
    
#endif
    
    if(!JNI_GetDefaultJavaVMInitArgs_func) {
        printf ("%s doesn't contain public JNI_GetDefaultJavaVMInitArgs\n", getDllname());
        exit (1);
    }
    
    if(!JNI_CreateJavaVM_func) {
        printf ("%s doesn't contain public JNI_CreateJavaVM\n", getDllname());
        exit (1);
    }
    
    memset (&args, 0, sizeof(args));
    
    args.version = JNI_VERSION_1_2;
    result = JNI_GetDefaultJavaVMInitArgs_func(&args);
    if (result != JNI_OK) {
        printf("JNI_GetDefaultJavaVMInitArgs() failed with result\n");
        exit(1);
    }  
    /*
     * NOTE: no JVM is actually created
     * this call to JNI_CreateJavaVM is intended for JET RT initialization
     */
    result = JNI_CreateJavaVM_func (pjvm, (void **)penv, &args);
    if (result != JNI_OK) {
        printf("JNI_CreateJavaVM() failed with result\n");
        exit(1);
    }

#ifdef DEBUG
    printf ("JET RT initialized\n");

#endif
    fflush (stdout);
}


/*
 * Look for class.
 */
jclass lookForClass (JNIEnv* penv, const char* name)
{
    jclass clazz = (*penv)->FindClass (penv, name);

    if (!clazz) {
        printf("Unable to find class %s\n", name);
	return NULL;
    }
#ifdef DEBUG
    printf ("Class %s found\n", name);
    fflush (stdout);
#endif

    return clazz;
}


/*
 * Create an object and invoke the instance method
 */
void invokeInstanceMethod (JNIEnv* penv, jclass myClassInDll, char * name, char * arguments)
{
    jmethodID MID_init, MID_name;
    jobject obj;

    MID_init = (*penv)->GetMethodID (penv, myClassInDll, "<init>", "()V");
    if (!MID_init) {
        printf("Error: MyClassInDll.<init>() not found\n");
        return;
    }

    obj = (*penv)->NewObject(penv, myClassInDll, MID_init);
    if (!obj) {
        printf("Error: failed to allocate an object\n");
        return;
    }

    MID_name = (*penv)->GetMethodID (penv, myClassInDll, name, arguments);

    if (!MID_name) {
        printf("Error: %s not found\n", name);
        return;
    }
    
    (*(penv))->CallVoidMethod (penv, obj, MID_name);
}




/*
 * Invoke the static method
 */
void invokeStaticMethod(JNIEnv* penv, jclass myClassInDll, char* name, char* arguments)
{
    jmethodID MID_name;

    MID_name = (*penv)->GetStaticMethodID(penv, myClassInDll, name, arguments);
    if (!MID_name) {
        printf("\nError: method %s not found\n", name);
        return;
    }
    
    (*penv)->CallStaticVoidMethod(penv, myClassInDll, MID_name);
}


/*
 * Find a constructor with a set arguments
 */
jmethodID findConstructor (JNIEnv* penv, jclass myClassInDll, char* arguments)
{
    jmethodID MID_initj;
    jobject obj;

    MID_initj = (jmethodID)(*penv)->GetMethodID (penv, myClassInDll, "<init>", arguments);
    if (!MID_initj) {
        printf("Error: MyClassInDll.<init>() not found\n");
	fflush (stdout);
        return 0;
    }

  return MID_initj;
}

/*
 * Create the Java SaxonProcessor
 */
jobject createSaxonProcessor (JNIEnv* penv, jclass myClassInDll, const char * arguments, jobject argument1, jboolean license)
{
    jmethodID MID_initi;

    jobject obj;

    MID_initi = (jmethodID)(*(penv))->GetMethodID (penv, myClassInDll, "<init>", arguments);
	if (!MID_initi) {
        	printf("Error: MyClassInDll.<init>() not found\n");
       	 return NULL;
        }
     
    if(argument1) {

	     obj = (jobject)(*(penv))->NewObject(penv, myClassInDll, MID_initi, argument1, license);
    } else {
	
      obj = (jobject)(*(penv))->NewObject(penv, myClassInDll, MID_initi, license);
     }
      if (!obj) {
        printf("Error: failed to allocate an object\n");
        return NULL;
      }
    return obj;
}


/*
 * Create the Java SaxonProcessor without boolean license argument
 */
jobject createSaxonProcessor2 (JNIEnv* penv, jclass myClassInDll, const char * arguments, jobject argument1)
{
    jmethodID MID_initi;

    jobject obj;

    MID_initi = (jmethodID)(*(penv))->GetMethodID (penv, myClassInDll, "<init>", arguments);
	if (!MID_initi) {
        	printf("Error: MyClassInDll.<init>() not found\n");
       	 return NULL;
        }
     
    if(argument1) {

	     obj = (jobject)(*(penv))->NewObject(penv, myClassInDll, MID_initi, argument1);
    } else {
	
      obj = (jobject)(*(penv))->NewObject(penv, myClassInDll, MID_initi);
     }
      if (!obj) {
        printf("Error: failed to allocate an object\n");
        return NULL;
      }
    return obj;
}


/*
 * Callback to check for exceptions. When called it returns the exception as a string 
 */
const char * checkForException(sxnc_environment environ, jclass callingClass,  jobject callingObject){

    if ((*(environ.env))->ExceptionCheck(environ.env)) {

        jthrowable exc = (*(environ.env))->ExceptionOccurred(environ.env);
#ifdef DEBUG
        (*(environ.env))->ExceptionDescribe(environ.env);
#endif
 	if(exc) {
		printf("Exception Occurred!");
	}
        jclass exccls = (jclass) (*(environ.env))->GetObjectClass(environ.env, exc);

        jclass clscls = (jclass) (*(environ.env))->FindClass(environ.env, "java/lang/Class");
        
        jmethodID getName = (jmethodID)(*(environ.env))->GetMethodID(environ.env, clscls, "getName", "()Ljava/lang/String;");
        jstring name = (jstring)((*(environ.env))->CallObjectMethod(environ.env, exccls, getName));
        char const* utfName = (*(environ.env))->GetStringUTFChars(environ.env, name, NULL);
        
        if(callingObject != NULL && strcmp(utfName, "net.sf.saxon.s9api.SaxonApiException") == 0){
  		
		jclass saxonExcClass = (*(environ.env))->FindClass(environ.env, "java/lang/Throwable");
		
		jmethodID  getMessage =  (jmethodID)(*(environ.env))->GetMethodID(environ.env, exccls, "getMessage", "()Ljava/lang/String;");
		
        if(getMessage) {
            
            jstring message = (jstring)((*(environ.env))->CallObjectMethod(environ.env, exc, getMessage));
       
	if(!message){
	
	(*(environ.env))->ExceptionClear(environ.env);
	return 0;
	}
            char const* utfMessage= (*(environ.env))->GetStringUTFChars(environ.env, message, NULL);
      
            if(utfMessage != NULL) {
		(*(environ.env))->ReleaseStringUTFChars(environ.env, name, utfName);
            }
       
	    (*(environ.env))->ExceptionClear(environ.env);
	    return utfMessage;
	}
    }
     (*(environ.env))->ReleaseStringUTFChars(environ.env, name, utfName);      
    }
(*(environ.env))->ExceptionClear(environ.env);
    return NULL;
}


/*
 * Clean up and destroy Java VM to release memory used. 
 */
void finalizeJavaRT (JavaVM* jvm)
{

  if(!jvm){
	  printf("\njvm is null\n");	
  }
  (*jvm)->DestroyJavaVM (jvm);
}


/*
 * Get a parameter from list 
 */
jobject getParameter(sxnc_parameter *parameters,  int parLen, const char* namespacei, const char * name){
	int i =0;
	for(i =0; i< parLen;i++) {
		if(strcmp(parameters[i].name, name) == 0)
			return (jobject)parameters[i].value;			
	}
	return NULL;
}


/*
 * Get a property from list 
 */
char* getProperty(sxnc_property * properties, int propLen, const char* namespacei, const char * name){
	int i =0;
	for(i =0; i< propLen;i++) {
		if(strcmp(properties[i].name, name) == 0)
			return properties[i].value;			
	}
	return 0;
}


/*
 * set a parameter 
 */
void setParameter(sxnc_parameter **parameters, int * parLen, int * parCap, const char * namespacei, const char * name, jobject value){

	if(getParameter(*parameters, (*parLen), "", name) != 0){
		return;			
	}
	(*parLen)++;	
	if((*parLen) >= (*parCap)) {
		(*parCap) *=2; 
		sxnc_parameter* temp = (sxnc_parameter*)malloc(sizeof(sxnc_parameter)*(*parCap));	
		int i =0;
		for(i =0; i< (*parLen)-1;i++) {
			temp[i] = (*parameters)[i];			
		}
		free(parameters);
		parameters = &temp;
	}
	int nameLen = strlen(name)+7;	
	char *newName = malloc(sizeof(char)*nameLen);
  	snprintf(newName, nameLen, "%s%s", "param:", name );
	(*parameters)[(*parLen)-1].name = (char*)newName;	
	(*parameters)[(*parLen)-1].value = (jobject)value;
}


/*
 * set a property 
 */
void setProperty(sxnc_property ** properties, int *propLen, int *propCap, const char* name, const char* value){	
	if(getProperty(*properties, (*propLen), "", name) != 0){
		return;			
	}
		
	if(((*propLen)+1) >= (*propCap)) {
		(*propCap) *=2; 
		sxnc_property* temp = (sxnc_property*)malloc(sizeof(sxnc_property)*  (*propCap));
		int i =0;	
		for(i =0; i< (*propLen)-1;i++) {
			temp[i] = (*properties)[i];			
		}
		free(properties);
		properties = &temp;

	}
	int nameLen = strlen(name)+1;	
	char *newName = (char*)malloc(sizeof(char)*nameLen);
  	snprintf(newName, nameLen, "%s", name );
	char *newValue = (char*)malloc(sizeof(char)*strlen(value)+1);
  	snprintf(newValue, strlen(value)+1, "%s", value );
	(*properties)[(*propLen)].name = (char*)newName;	
	(*properties)[(*propLen)].value = (char*)newValue;
        (*propLen)++;
}

/*
 * clear parameter 
 */
void clearSettings(sxnc_parameter **parameters, int *parLen, sxnc_property ** properties, int *propLen){
	int i =0;
	free(*parameters);
	free(*parameters);
	/*for(i =0; i< parLen; i++){
		free((*parameters[i].name);
		free(parameters[i].value);
	}
	for(i =0; i< propLen; i++){
		free(properties[i].name);
		free(properties[i].value);
	}*/

    	*parameters = (sxnc_parameter *)calloc(10, sizeof(sxnc_parameter));
    	*properties = (sxnc_property *)calloc(10, sizeof(sxnc_property));
	(*parLen) = 0;
	(*propLen) = 0;
}


const char * stringValue(sxnc_environment environ, jobject value){
	jclass  objClass = lookForClass(environ.env, "java/lang/Object");
	static jmethodID strMID = NULL;
	if(!strMID) {
		strMID = (jmethodID)(*(environ.env))->GetMethodID(environ.env, objClass, "toString", "()Ljava/lang/String;");
		if(!strMID) {
	 		 printf("\nError: Object %s() not found\n","toString");
  	 		 fflush (stdout);
         		 return NULL;
		}
        }
	jstring result = (jstring)((*(environ.env))->CallObjectMethod(environ.env, value, strMID));
 	if(result) {
	       	const char * str = (*(environ.env))->GetStringUTFChars(environ.env, result, NULL);    
		return str;
        }

    //checkForException(environ, cppClass, cpp);
    return 0;
	
}




