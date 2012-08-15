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

import net.agkn.field_stripe.exception.OperationFailedException;
import net.agkn.field_stripe.record.FieldQualifier;
import net.agkn.field_stripe.record.writer.IRecordWriter;

/**
 * An element (node or leaf) of a tree of field stripe decoders. A tree of
 * decoders is {@link FieldStripeDecoderFactory#createDecoderTree(net.agkn.field_stripe.record.IFieldType) created}
 * using the {@link FieldStripeDecoderFactory factory}. For all intents and
 * purposes this is an internal interface and the external interface is defined
 * by {@link RootFieldStripeDecoder}.
 *
 * @author rgrzywinski
 * @see FieldStripeDecoderFactory
 */
public interface IFieldStripeDecoder {
    /**
     * @return zero if there is an <var>UNSET</var> parent. Greater than zero 
     *         for the depth of the <var>UNSET</var> parent. Less than zero if 
     *         there is no more data to be read.
     * @throws OperationFailedException if there was an error reading from a 
     *         field stripe or decoding that value. Subclasses may contain 
     *         additional information as to the nature of the failure.
     */
    int getUnsetParentDepth()
        throws OperationFailedException;

    /**
     * Decodes (writes) the value from a field stripe to a {@link IRecordWriter}.
     * Given the SAX-like style of the writer, it is assumed that the decoder
     * knows the type of field that is to be read. This covers all {@link FieldQualifier}
     * cases.<p/>
     * 
     * Elements have the following responsibilities:</p>
     * <ul>
     *   <li>a child if repeated must iterate and decode all of the current
     *       record's value before returning.</li>
     * </ul>
     * 
     * @param  recordWriter the {@link IRecordWriter writer} to which the decoded
     *         record is written. This will never be <code>null</code>.
     * @return the {@link ReadResult result} of the operation. This will be 
     *         <code>null</code> no record could be read.
     * @throws OperationFailedException if there was an error reading from a 
     *         field stripe, decoding the read state or in writing to the record
     *         writer. Subclasses may contain additional information as to the
     *         nature of the failure.
     */
    ReadResult decode(IRecordWriter recordWriter)
        throws OperationFailedException;
}