////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.tree.iter;

import net.sf.saxon.om.GroundedValue;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.value.SequenceExtent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * A SequenceIterator is used to iterate over a sequence. An UnfailingIterator
 * is a SequenceIterator that throws no checked exceptions.
 */

public interface UnfailingIterator<T extends Item<?>> extends SequenceIterator<T> {

    /**
     * Get the next item in the sequence.
     *
     * @return the next Item. If there are no more items, return null.
     */

    T next();

    //@Override
    default void forEach(Consumer<T> consumer) {
        T item;
        while ((item = next()) != null) {
            consumer.accept(item);
        }
    }

    default Optional<T> firstWith(Predicate<? super T> condition) {
        T item;
        while ((item = next()) != null) {
            if (condition.test(item)) {
                close();
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    default List<T> toList() {
        List<T> list = new ArrayList<>();
        forEach(list::add);
        return list;
    }

    default GroundedValue<T> toGroundedValue() {
        return SequenceExtent.makeSequenceExtent(toList());
    }
}

