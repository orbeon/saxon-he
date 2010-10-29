package net.sf.saxon.trans;

/**
 * Exception thrown when there are problems with the license file
 */
public class LicenseException extends RuntimeException {

    private int reason;

    public static final int EXPIRED = 1;
    public static final int INVALID = 2;
    public static final int NOT_FOUND = 3;
    public static final int WRONG_FEATURES = 4;
    public static final int CANNOT_READ = 5;

    public LicenseException(String message, int reason) {
        super(message);
        this.reason = reason;
    }

    public void setReason(int reason) {
        this.reason = reason;
    }

    public int getReason() {
        return reason;
    }
}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): changes to allow source and/or stylesheet from stdin contributed by
// Gunther Schadow [gunther@aurora.regenstrief.org]
//
