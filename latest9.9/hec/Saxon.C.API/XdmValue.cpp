#include "XdmValue.h"
#include "XdmItem.h"
#include "XdmAtomicValue.h"
#include "XdmNode.h"

XdmValue::XdmValue(const XdmValue &other) {
	//SaxonProcessor *proc = other.proc; //TODO
	valueType = other.valueType;

	xdmSize = other.xdmSize;
	jValues = other.jValues;
	toStringValue = other.toStringValue;
	values.resize(0);//TODO memory issue might occur here

	for (int i = 0; i < xdmSize; i++) {
		addXdmItem(other.values[i]);
	}
	
}

const char * XdmValue::toString() {
	if (toStringValue == NULL) {
		jclass xdmValueClass = lookForClass(SaxonProcessor::sxn_environ->env, "net/sf/saxon/s9api/XdmValue");
		jmethodID strMID2 = (jmethodID)SaxonProcessor::sxn_environ->env->GetMethodID(xdmValueClass,
			"toString",
			"()Ljava/lang/String;");
		if (!strMID2) {
			std::cerr << "Error: Saxonc.XdmValue." << "toString"
				<< " not found\n" << std::endl;
			return NULL;
		}
		else {
			std::string resultS = "";
			for(int i=0; i<size();i++) {

			
				jstring result = (jstring)(SaxonProcessor::sxn_environ->env->CallObjectMethod(itemAt(i)->getUnderlyingValue(), strMID2));
				if (result) {
					resultS = SaxonProcessor::sxn_environ->env->GetStringUTFChars(result, NULL);
				
				}
			}

			toStringValue = resultS.c_str();
			return toStringValue;			
		}
			
		
	}
	else {
		return toStringValue;
	}

}

void XdmValue::setProcessor(SaxonProcessor *p) { 
		proc = p;
	}


int XdmValue::size() {
	return xdmSize;
}

XdmValue::XdmValue(jobject val) {
	XdmItem * value = new XdmItem(val);
	values.resize(0);//TODO memory issue might occur here
	values.push_back(value);
	xdmSize++;
	jValues = NULL;
	valueType = NULL;
	toStringValue = NULL;
}


XdmValue::XdmValue(jobject val, bool arr){
	xdmSize = 0;
	values.resize(0);
	jValues = NULL;
	valueType = NULL;
	toStringValue = NULL;
	jclass xdmValueForcppClass = lookForClass(SaxonProcessor::sxn_environ->env, "net/sf/saxon/option/cpp/XdmValueForCpp");
	jmethodID xvfMID = SaxonProcessor::sxn_environ->env->GetStaticMethodID(xdmValueForcppClass, "makeArrayFromXdmValue", "(Lnet/sf/saxon/s9api/XdmValue;)[Lnet/sf/saxon/s9api/XdmItem;");

	if(!xvfMID){

		std::cerr << "Error: SaxonDll." << "makeArrayFromXdmValue"
				<< " not found\n" << std::endl;
			return ;
	}
	
	jobjectArray results = (jobjectArray) SaxonProcessor::sxn_environ->env->CallStaticObjectMethod(xdmValueForcppClass, xvfMID, val);
	if(results){
	int sizex = SaxonProcessor::sxn_environ->env->GetArrayLength(results);
	if (sizex>0) {
		jclass atomicValueClass = lookForClass(SaxonProcessor::sxn_environ->env, "net/sf/saxon/s9api/XdmAtomicValue");
		jclass nodeClass = lookForClass(SaxonProcessor::sxn_environ->env, "net/sf/saxon/s9api/XdmNode");
		jclass functionItemClass = lookForClass(SaxonProcessor::sxn_environ->env, "net/sf/saxon/s9api/XdmFunctionItem");

		//XdmValue * value = new XdmValue();
		//value->setProcessor(proc);
		XdmItem * xdmItem = NULL;
		for (int p=0; p < sizex; ++p) 
		{
			jobject resulti = SaxonProcessor::sxn_environ->env->GetObjectArrayElement(results, p);
			//value->addUnderlyingValue(resulti);

			if(SaxonProcessor::sxn_environ->env->IsInstanceOf(resulti, atomicValueClass)           == JNI_TRUE) {
				xdmItem = new XdmAtomicValue(resulti);
				

			} else if(SaxonProcessor::sxn_environ->env->IsInstanceOf(resulti, nodeClass)           == JNI_TRUE) {
				xdmItem = new XdmNode(resulti);


			} else if (SaxonProcessor::sxn_environ->env->IsInstanceOf(resulti, functionItemClass)           == JNI_TRUE) {
				continue;
			}
			//xdmItem->setProcessor(proc);
			addXdmItem(xdmItem);
		}
	}
		SaxonProcessor::sxn_environ->env->DeleteLocalRef(results);
	}	
}


XdmValue::~XdmValue() {
	//proc->env->ReleaseObject
	for (size_t i = 0; i < values.size(); i++) {
		if (values[i]->getRefCount() < 1) {
			delete values[i];
		}
	}
	values.clear();
	if (valueType != NULL) { delete valueType; }
	if (jValues && proc != NULL) {
		SaxonProcessor::sxn_environ->env->DeleteLocalRef(jValues);
	}
	xdmSize = 0;
	if(toStringValue != NULL) {
		delete toStringValue;
	}
}

void XdmValue::addXdmItem(XdmItem* val) {
	if (val != NULL) {
		values.push_back(val);
		xdmSize++;
		jValues = NULL; //TODO clear jni array from memory if needed
	}
}


void XdmValue::addUnderlyingValue(jobject val) {
	XdmItem * valuei = new XdmItem(val);
	valuei->setProcessor(proc);
	values.push_back(valuei);
	xdmSize++;
	jValues = NULL; //TODO clear jni array from memory if needed

}




XdmItem * XdmValue::getHead() {
	if (values.size() > 0) {
		return values[0];
	}
	else {
		return NULL;
	}
}

jobject XdmValue::getUnderlyingValue() {
	if (jValues == NULL) {
		int i;
		JNIEnv *env = SaxonProcessor::sxn_environ->env;
		int count = values.size();
		if (count == 0) {
			return NULL;
		}
		jclass objectClass = lookForClass(env,
			"net/sf/saxon/s9api/XdmItem");
		jValues = (jobjectArray)env->NewObjectArray((jint)count, objectClass, 0);

		for (i = 0; i < count; i++) {
			env->SetObjectArrayElement(jValues, i, values[i]->getUnderlyingValue());
		}
	} 
	return (jobject)jValues;
}

void XdmValue::releaseXdmValue() {



}

XdmItem * XdmValue::itemAt(int n) {
	if (n >= 0 && (unsigned int)n < values.size()) {
		return values[n];
	}
	return NULL;
}

/**
* Get the type of the object
*/
XDM_TYPE XdmValue::getType() {
	return XDM_VALUE;
}




