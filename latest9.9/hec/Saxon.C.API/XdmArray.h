////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2019 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#ifndef SAXON_XDM_ARRAY_h
#define SAXON_XDM_ARRAY_h


#include "XdmFunctionItem.h"
#include <string>

#include <stdlib.h>
#include <string.h>

class XdmArray : public XdmFunctionItem {

public:

    XdmArray();

    XdmArray(const XdmArray &d);


    virtual ~XdmArray(){
	//std::cerr<<"destructor called fpr XdmFunctionItem"<<std::endl;

    }

    XdmArray(jobject);

    XdmArray(XdmValue ** values, int length);

    int arrayLength();

    XdmValue get(int n);

    XdmArray put(int n, XdmValue * value);

    XdmArray addMember(XdmValue value);

    XdmArray concat(XdmValue value);    

    std::List asList();

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
