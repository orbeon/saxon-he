package net.sf.saxon.option.sql;
import com.saxonica.xsltextn.ExtensionElementFactory;

/**
  * Class SQLElementFactory.
  * A "Factory" for SQL extension nodes in the stylesheet tree.
  * <p>Note: despite its package name, this class is not part of Saxon-HE. Rather, it is part of an
  * open-source plug-in to Saxon-PE and Saxon-EE. This accounts for the reference to code in the
  * com.saxonica package.</p>
 *
 * <p>From Saxon 9.2 the standard namespace associated with this extension is "http://saxon.sf.net/sql".
 * However, it can be registered under a different namespace if required.</p>
  */

// Note: despite its package name, this class is not part of Saxon-HE. Rather, it is part of an
// open-source

public class SQLElementFactory implements ExtensionElementFactory {

    /**
    * Identify the class to be used for stylesheet elements with a given local name.
    * The returned class must extend net.sf.saxon.style.StyleElement
    * @return null if the local name is not a recognised element type in this
    * namespace.
    */

    public Class getExtensionClass(String localname)  {
        if (localname.equals("connect")) return SQLConnect.class;
        if (localname.equals("insert")) return SQLInsert.class;
        if (localname.equals("update")) return SQLUpdate.class;
        if (localname.equals("delete")) return SQLDelete.class;        
        if (localname.equals("column")) return SQLColumn.class;
        if (localname.equals("close")) return SQLClose.class;
        if (localname.equals("query")) return SQLQuery.class;
        return null;
    }

}

// Copyright (c) Saxonica Limited 2009. All rights reserved.

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by __ are Copyright (C) __. All Rights Reserved.
//
// Contributor(s): 	None
//