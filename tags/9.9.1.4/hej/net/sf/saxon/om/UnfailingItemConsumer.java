////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.om;

import java.util.function.Consumer;

/**
 * A FunctionalInterface for any method that processes individual items. It
 * differs from the superclass {@link ItemConsumer} in that it cannot throw
 * checked exceptions
 * @param <T> the type of items to be processed
 */

@FunctionalInterface
public interface UnfailingItemConsumer<T extends Item> extends ItemConsumer<T>, Consumer<T> {
    /**
     * Process one item
     * @param item the item to be processed
     */
    void accept(T item);
}

