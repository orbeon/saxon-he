package net.sf.saxon.event;

/**
  * The AllElementStripper refines the Stripper class to do stripping of
  * all whitespace nodes in a document
  * @author Michael H. Kay
  */

public class AllElementStripper extends Stripper {

    private static AllElementStripper anInstance = new AllElementStripper();

    /**
     * Get a singular instance of the class. Note that this class is NOT a singleton and is NOT immutable.
     * This method should only be used (a) if the filtering behaviour of the class is not being used, or (b)
     * if the getAnother() method is used to clone the class before use as a filter.
     * @return a singular instance of the class, which the caller must not modify.
     */

    public static AllElementStripper getInstance() {
        return anInstance;
    }

    /**
     * Constructor: create an instance of the class. This instance can be used as a filter in a pipeline,
     * and can be freely modified by the caller before use.
     */

    public AllElementStripper() {
    }

    /**
     * Create a copy of this Stripper. The result can be freely modified by the caller before use.
     * @return a copy of this Stripper. The new copy shares the same PipelineConfiguration
     * as the original, but the underlying receiver (that is, the destination for post-stripping
     * events) is left uninitialized.
     */

    public Stripper getAnother() {
        Stripper clone = new AllElementStripper();
        clone.setPipelineConfiguration(getPipelineConfiguration());
        return clone;
    }

    /**
    * Decide whether an element is in the set of white-space preserving element types
    * @param fingerprint identifies the element being tested
    * @return STRIP_DEFAULT: strip spaces unless xml:space tells you not to.
    */

    public byte isSpacePreserving(int fingerprint) {
        return STRIP_DEFAULT;
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
