////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.parser;

import com.saxonica.ee.stream.Posture;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ErrorType;
import net.sf.saxon.type.ItemType;

/**
 * A data structure that represents the required type of the context item, together
 * with information about whether it is known to be present or absent or whether it
 * is not known statically whether it is present or absent.
 */


public class ContextItemStaticInfo {


    private ItemType itemType;
    private boolean contextMaybeUndefined;
    private Expression contextSettingExpression;
//#ifdefined STREAM
    private Posture explicitPosture;
//#endif

    /**
     * Create a ContextItemStaticInfo
     * @param itemType the item type of the context item. If the context item is absent, set this to
     * {@link net.sf.saxon.type.ErrorType#getInstance()}.
     * @param maybeUndefined set to true if it is possible (or certain) that the context item will be absent.
     */

    public ContextItemStaticInfo(ItemType itemType, boolean maybeUndefined) {
        this.itemType = itemType;
        this.contextMaybeUndefined = maybeUndefined;
//#ifdefined STREAM
        this.explicitPosture = Posture.ROAMING;
//#endif
    }


    /**
     * Create a ContextItemStaticInfo
     * @param itemType the item type of the context item. If the context item is absent, set this to
     * {@link net.sf.saxon.type.ErrorType#getInstance()}.
     * @param maybeUndefined set to true if it is possible (or certain) that the context item will be absent.
     * @param contextSettingExpression The expression that controls the context item.
     */

    public ContextItemStaticInfo(ItemType itemType, boolean maybeUndefined, Expression contextSettingExpression) {
        this.itemType = itemType;
        this.contextMaybeUndefined = maybeUndefined;
        this.contextSettingExpression = contextSettingExpression;
    }

    /**
     * Create a ContextItemStaticInfo
     * @param itemType the item type of the context item. If the context item is absent, set this to
     * {@link net.sf.saxon.type.ErrorType#getInstance()}.
     * @param maybeUndefined set to true if it is possible (or certain) that the context item will be absent.
     * @param isStriding True if the explicit posture is Striding (otherwise Roaming).
     */

    public ContextItemStaticInfo(ItemType itemType, boolean maybeUndefined, boolean isStriding) {
        this.itemType = itemType;
        this.contextMaybeUndefined = maybeUndefined;
//#ifdefined STREAM
        this.explicitPosture = isStriding ? Posture.STRIDING : Posture.ROAMING;
//#endif
    }

//#ifdefined STREAM
    /**
     * Create a ContextItemStaticInfo
     * @param itemType the item type of the context item. If the context item is absent, set this to
     * {@link net.sf.saxon.type.ErrorType#getInstance()}.
     * @param maybeUndefined set to true if it is possible (or certain) that the context item will be absent.
     * @param contextItemPosture the context item posture.
     */

    public ContextItemStaticInfo(ItemType itemType, boolean maybeUndefined, Posture contextItemPosture) {
        this.itemType = itemType;
        this.contextMaybeUndefined = maybeUndefined;
        this.explicitPosture = contextItemPosture;
    }

    public void setContextPostureStriding() {
        this.explicitPosture = Posture.STRIDING;
    }
//#endif

    /**
     * Set the function that will be called to get the posture of the context item
     * @param function the PostureGetter function
     */

//    public void setPostureGetter(PostureGetter function) {
//        this.postureGetter = function;
//    }

    /**
     * Get the static type of the context item. If the context item is known to be undefined, the
     * returned value is
     * @return the static context item type
     */

    public ItemType getItemType() {
        return itemType;
    }

    /**
     * Ask whether it is possible that the context item is absent
     * @return true if the context item might be absent
     */

    public boolean isPossiblyAbsent() {
        return contextMaybeUndefined;
    }

//#ifdefined STREAM
    /**
     * Get the context item posture
     * @return the posture of the expression that sets the context item
     */

    public Posture getContextItemPosture() {
        if (explicitPosture == null) {
            return contextSettingExpression.getPosture();
        } else {
            return explicitPosture;
        }
    }
//#endif

    /**
     * Default information when nothing else is known
     */

    public final static ContextItemStaticInfo DEFAULT =
            new ContextItemStaticInfo(AnyItemType.getInstance(), true, true);

    public final static ContextItemStaticInfo ABSENT =
            new ContextItemStaticInfo(ErrorType.getInstance(), true, true);

}