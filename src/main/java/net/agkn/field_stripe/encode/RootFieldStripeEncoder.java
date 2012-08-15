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

import java.util.List;

import net.agkn.field_stripe.exception.OperationFailedException;
import net.agkn.field_stripe.record.reader.IRecordReader;

/**
 * The root of a tree of {@link IFieldStripeEncoder field stripe encoders}
 * {@link FieldStripeEncoderFactory#createEncoderTree(net.agkn.field_stripe.record.IFieldType) created}
 * using the {@link FieldStripeEncoderFactory}.<p/>
 *  
 * This is not <i>is-a</i> {@link IFieldStripeEncoder} as most of the fields
 * (specifically the meta-data fields) would be no-op or exception cases (since
 * this has no parent).
 *
 * @author rgrzywinski
 * @see FieldStripeEncoderFactory
 */
public class RootFieldStripeEncoder {
    // the list (in the order that matches the fields for this encoder) of child
    // encoders. This will never be null or empty.
    private final List<IFieldStripeEncoder> childEncoders;

    // ========================================================================
    /**
     * @param  childEncoders the children {@link IFieldStripeEncoder} for this
     *         root encoder. This cannot be <code>null</code> or empty (there
     *         must be at least one field to be valid by contract).
     * @see FieldStripeEncoderFactory#createEncoderTree(net.agkn.field_stripe.record.IFieldType)
     */
    public RootFieldStripeEncoder(final List<IFieldStripeEncoder> childEncoders) {
        this.childEncoders = childEncoders;
    }

    // ========================================================================
    /**
     * {@link IRecordReader#startRecord() Starts a new record} and recursively
     * encodes all of the field values from the specified {@link IRecordReader}
     * into the tree of {@link IFieldStripeEncoder field-stripe encoders} 
     * specified on {@link #RootFieldStripeEncoder(List) construction}.
     * 
     * @param  recordReader the {@link IRecordReader reader} whose record is to
     *         be encoded in field stripes. This will never be <code>null</code>.
     * @return <code>true</code> if and only if a record was encoded. <code>false</code>
     *         if there were no records to encode.
     * @throws OperationFailedException if there was an error reading from the
     *         record reader, encoding the value or writing to the field stripe.
     *         Subclasses may contain additional information as to the nature 
     *         of the failure.
     */
    public boolean encode(IRecordReader recordReader)
            throws OperationFailedException {
        if(!recordReader.hasMoreRecords()) return false/*no record to encode*/;

        recordReader.startRecord();

        for(final IFieldStripeEncoder encoder : childEncoders)
            encoder.encode(recordReader);

        recordReader.endRecord();

        return true/*a record was encoded*/;
    }
}