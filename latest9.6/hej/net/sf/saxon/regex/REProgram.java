////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Originally part of Apache's Jakarta project (downloaded January 2012),
 * this file has been extensively modified for integration into Saxon by
 * Michael Kay, Saxonica.
 */

package net.sf.saxon.regex;

import net.sf.saxon.z.IntPredicate;

/**
 * A class that holds compiled regular expressions.
 */

public class REProgram {
    static final int OPT_HASBACKREFS = 1;
    static final int OPT_HASBOL = 2;

    Operation operation;
    REFlags flags;
    UnicodeString prefix;              // Prefix string optimization
    IntPredicate initialCharClass;
    int minimumLength = 0;
    int fixedLength = -1;
    int optimizationFlags;      // Optimization flags (REProgram.OPT_*)
    int maxParens = -1;

    /**
     * Constructs a program object from a character array
     * @param operation Array with RE opcode instructions in it. The "next"
     * @param parens       Count of parens in the program
     *                     pointers within the operations must already have been converted to absolute
     *                     offsets.
     * @param flags the regular expression flags
     */
    public REProgram(Operation operation, int parens, REFlags flags) {
        this.flags = flags;
        setOperation(operation);
        this.maxParens = parens;
    }

    /**
     * Sets a new regular expression program to run.  It is this method which
     * performs any special compile-time search optimizations.  Currently only
     * two optimizations are in place - one which checks for backreferences
     * (so that they can be lazily allocated) and another which attempts to
     * find an prefix anchor string so that substantial amounts of input can
     * potentially be skipped without running the actual program.
     *
     * @param operation Program instruction buffer
     */
    private void setOperation(Operation operation) {
        // Save reference to instruction array
        this.operation = operation;

        // Initialize other program-related variables
        this.optimizationFlags = 0;
        this.prefix = null;

        this.operation = operation.optimize(this, flags);

        // Try various compile-time optimizations

        if (operation instanceof Operation.OpSequence) {
            Operation first = ((Operation.OpSequence)operation).getOperations().get(0);
            if (first instanceof Operation.OpBOL) {
                optimizationFlags |= REProgram.OPT_HASBOL;
            } else if (first instanceof Operation.OpAtom) {
                prefix = ((Operation.OpAtom)first).getAtom();
            } else if (first instanceof Operation.OpCharClass) {
                initialCharClass = ((Operation.OpCharClass)first).getPredicate();
            }
        }

        minimumLength = operation.getMinimumMatchLength();
        fixedLength = operation.getMatchLength();

    }

    /**
     * Ask whether the regular expression matches a zero length string
     *
     * @return true if the regex matches a zero length string
     */

    public boolean isNullable() {
        return operation.matchesEmptyString();
    }

    /**
     * Returns a copy of the prefix of current regular expression program
     * in a character array.  If there is no prefix, or there is no program
     * compiled yet, <code>getPrefix</code> will return null.
     *
     * @return A copy of the prefix of current compiled RE program
     */
    public UnicodeString getPrefix() {
        return prefix;
    }


}
