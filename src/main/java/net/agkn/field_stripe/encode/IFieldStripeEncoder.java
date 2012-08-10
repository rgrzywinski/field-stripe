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

import net.agkn.common.exception.OperationFailedException;
import net.agkn.field_stripe.record.FieldQualifier;
import net.agkn.field_stripe.record.reader.IRecordReader;

/**
 * All possible encoding states are represented in this interface. A tree of
 * encoders is {@link FieldStripeEncoderFactory#createEncoderTree(net.agkn.field_stripe.record.IFieldType) created}
 * using the {@link FieldStripeEncoderFactory factory}. For all intents and
 * purposes this is an internal interface and the external interface is defined
 * by {@link RootFieldStripeEncoder}.
 *
 * @author rgrzywinski
 * @see FieldStripeEncoderFactory
 */
public interface IFieldStripeEncoder {
    /**
     * Encodes (writes) the value of the field from the {@link IRecordReader}
     * into a field stripe. Given the SAX-like style of the reader, it is 
     * assumed that the encoder knows the type of field that is to be read. 
     * This covers all {@link FieldQualifier} cases. 
     * 
     * @param  recordReader the {@link IRecordReader record} whose value is to
     *         be encoded. This will never be <code>null</code>.
     * @throws OperationFailedException if there was an error reading from the
     *         record reader, encoding the value or writing to the field stripe.
     *         Subclasses may contain additional information as to the nature 
     *         of the failure.
     */
    void encode(IRecordReader recordReader) 
        throws OperationFailedException;

    // ........................................................................
    // meta-data

    /**
     * A parent of the field associated with this encoder has an <var>UNSET</var>
     * value. The depth of the <var>UNSET</var> parent is identified.
     * 
     * @param  fieldDepth the depth of the parent field that is <var>UNSET</var>.
     *         This will never be negative or zero.
     * @throws OperationFailedException if the state could not be written for
     *         any reason. Subclasses may contain additional information as to
     *         the nature of the failure.
     */
    void writeUnsetParent(int fieldDepth)
        throws OperationFailedException;

    /**
     * A parent of the field associated with this encoder has had a repeated
     * value. The depth of the repeated parent is identified. This may not be 
     * called in all configurations such as the case where all parent fields 
     * are required. 
     * 
     * @param  fieldDepth the depth of the parent field that is has (last) 
     *         repeated. This will never be negative or zero.
     * @throws OperationFailedException if the state could not be written for
     *         any reason. Subclasses may contain additional information as to
     *         the nature of the failure.
     */
    void writeRepeatedParent(int fieldDepth)
        throws OperationFailedException;
}