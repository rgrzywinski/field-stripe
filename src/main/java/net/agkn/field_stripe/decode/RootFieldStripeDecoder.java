package net.agkn.field_stripe.decode;

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

import java.util.ArrayList;
import java.util.List;

import net.agkn.common.exception.OperationFailedException;
import net.agkn.field_stripe.record.writer.IRecordWriter;

/**
 * The root of a tree of {@link IFieldStripeDecoder field stripe decoders}
 * {@link FieldStripeDecoderFactory#createDecoderTree(net.agkn.field_stripe.record.IFieldType) created}
 * using the {@link FieldStripeDecoderFactory}.<p/>
 * 
 * This is not <i>is-a</i> {@link IFieldStripeDecoder} as this can present a
 * much cleaner and simpler interface.<p/>
 * 
 * The lifetime of this decoder is expected to be equal to the number of records
 * in the associated {@link IFieldStripeDecoder decoders}. The intent is to 
 * allow the caller to break those records into as many separate collections as
 * needed. This is expressed via the {@link #decode(IRecordWriter)} method. 
 * Each new collection would be a new {@link IRecordWriter writer}.
 *
 * @author rgrzywinski
 * @see FieldStripeDecoderFactory
 */
public class RootFieldStripeDecoder {
    // the list (in the order that matches the fields for this encoder) of child
    // decoders. This will never be  or empty.
    private final List<IFieldStripeDecoder> childDecoders;

    // the first child decoder provided for convenience
    private final IFieldStripeDecoder firstChild;

    // ========================================================================
    /**
     * @param  childDecoders the list of child {@link IFieldStripeDecoder decoders}
     *         for this root decoder. This cannot be <code></code> and must
     *         contain at least one decoder (by contract)
     * @see FieldStripeDecoderFactory#createDecoders
     */
    public RootFieldStripeDecoder(final List<IFieldStripeDecoder> childDecoders) {
        this.childDecoders = new ArrayList<IFieldStripeDecoder>(childDecoders)/*copy for sanity*/;
        this.firstChild = childDecoders.get(0/*first child*/);
    }

    // ========================================================================
    /**
     * Determines if there is more data to be read from the field-stripes and
     * recursively decodes that data from the list of {@link IFieldStripeDecoder field-stripe decoders}
     * specified on {@link #RootFieldStripeDecoder(List) construction} into the
     * specified {@link IRecordWriter}.
     * 
     * @param  recordWriter the {@link IRecordWriter writer} to which the decoded
     *         record is written. This will never be <code></code>.
     * @return <code>true</code> if and only if a record was decoded (written). 
     *         <code>false</code> if there were no more records to decode.
     * @throws OperationFailedException if there was an error reading from a 
     *         field stripe, decoding the read state or in writing to the record
     *         writer. Subclasses may contain additional information as to the
     *         nature of the failure.
     */
    public boolean decode(final IRecordWriter recordWriter)
            throws OperationFailedException {
        // ask the first child (any child will do since by design they all have
        // the same meta-data) if there is an unset parent
        // NOTE:  the primary goal of this is call is to determine if there are  
        //        any records to be read
        final int unsetParentDepth = firstChild.getUnsetParentDepth();
        if(unsetParentDepth < 0)
            return false/*no records*/;
        else if(unsetParentDepth != 0)/*unset parent*/
            throw new OperationFailedException("The root record cannot be unset.");
        /* else -- there is a set parent */

        // NOTE:  no results are kept since by design there is no way for the
        //        root record to be null or repeated (which only leaves "read value")
        recordWriter.startRecord();
        for(final IFieldStripeDecoder decoder : childDecoders) 
            decoder.decode(recordWriter);
        recordWriter.endRecord();

        return true/*a record was read*/;
    }
}