using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace TestRunner
{
   


public class ErrorCollector {

    private HashSet<String> errorCodes = new HashSet<String>();

    
    public void error(Exception exception) {
        addErrorCode(exception);
        //super.error(exception);
    }

    
    public void fatalError(Exception exception)  {
        addErrorCode(exception);
        //super.fatalError(exception);
    }

    /**
     * Make a clean copy of this ErrorListener. This is necessary because the
     * standard error listener is stateful (it remembers how many errors there have been)
     *
     * @param hostLanguage the host language (not used by this implementation)
     * @return a copy of this error listener
     */
    
    public ErrorCollector makeAnother(int hostLanguage) {
        return this;
    }

    private void addErrorCode(Exception exception) {
        /*if (exception instanceof XPathException) {
            String errorCode = ((XPathException) exception).getErrorCodeLocalPart();
            if (errorCode != null) {
                String ns = ((XPathException) exception).getErrorCodeNamespace();
                if (ns != null && !NamespaceConstant.ERR.equals(ns)) {
                    errorCode = "Q{" + ns + "}" + errorCode;
                }
                errorCodes.add(errorCode);
            } else {
                errorCodes.add("ZZZZ9999");
            }
        }*/
    }

    public HashSet<String> getErrorCodes() {
        return errorCodes;
    }
}
}
