#include "XdmValue.h"
#include "XdmItem.h"

XdmValue::XdmValue(const XdmValue &other) {
	//SaxonProcessor *proc = other.proc; //TODO
	valueType = other.valueType;

	xdmSize = other.xdmSize;
	jValues = other.jValues;

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
		if (!bmID) {
			std::cerr << "Error: Saxonc.XdmValue." << "toString"
				<< " not found\n" << std::endl;
			return NULL;
		}
		else {
			jstring result = (jstring)(SaxonProcessor::sxn_environ->env->CallObjectMethod(value->xdmvalue, strMID2));
			if (result) {
				const char * str = (*(environi->env))->GetStringUTFChars(SaxonProcessor::sxn_environ->env, result, NULL);
				toStringValue = str;
				return str;
			}
			return NULL;
		}
	}
	else {
		return toStringValue;
	}

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
			"java/lang/Object");
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




