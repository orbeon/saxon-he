////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2019 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#ifndef SAXON_XDMFUNCTIONITEM_h
#define SAXON_XDMFUNCTIONITEM_h


#include "XdmItem.h"
#include <string>

#include <stdlib.h>
#include <string.h>

class XdmFunctionItem : public XdmItem {
    
public:

    XdmFunctionItem();

    XdmFunctionItem(const XdmFunctionItem &d);


    virtual ~XdmFunctionItem(){
	//std::cerr<<"destructor called fpr XdmFunctionItem"<<std::endl;
        if(!fname.empty()) {
        		fname.clear();
        	}
    }

    XdmFunctionItem(jobject);

    const char* getName();

    int getArity();

    static XdmFunctionItem * getSystemFunction(const char * name, int arity);

    XdmValue * call(XdmValue ** arguments, int argument_length);
    
    bool isAtomic(){
        return false;
    }

	/**
	* Get the type of the object
	*/
	XDM_TYPE getType() {
		return XDM_FUNCTION_ITEM;
	}
    
    
private:
     

    std::string fname;
    int arity;

};




#endif
