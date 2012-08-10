package net.agkn.field_stripe.encode;

/*
 * Copyright 2012 Aggregate Knowledge, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

import net.agkn.field_stripe.record.IField;

/**
 * An abstract super-class for all encoders that contains the model information
 * such as which {@link IField field} this is a encoder for and what depth that
 * field exists at in the schema. This effectively represents the "model" of 
 * the encoder.
 *
 * @author rgrzywinski
 */
// NOTE:  this isn't is-a IFieldStripeEncoder as this is simply the 'model' of
//        a encoder. It isn't a decoder.
public abstract class AbstractEncoder {
    // configuration / structure

    // the IField that this is an encoder for
    protected final IField field;

    // the depth of the *field* that correspond with this encoder (in contrast
    // to the depth of this encoder in the tree of encoders)
    // NOTE:  this is broken out of the field's path purely for convenience
    protected final int fieldDepth;

    // is associated type repeated (i.e. multi-valued)?
    // NOTE:  this is broken out from the field purely for convenience
    protected final boolean multiValue;

    // ========================================================================
    /**
     * @param  field the field to which the encoder belongs. This cannot be <code>null</code>.
     * @param  fieldDepth the depth of the fields that this encoder encodes 
     *         (where the root of the tree has depth <code>0</code>). This
     *         cannot be negative or zero.
     */
    public AbstractEncoder(final IField field, final int fieldDepth) {
        this.field = field;
        this.fieldDepth = fieldDepth;
        this.multiValue = field.getQualifier().isMultiValue();
    }
}