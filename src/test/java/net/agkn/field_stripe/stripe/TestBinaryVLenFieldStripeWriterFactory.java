package net.agkn.field_stripe.stripe;

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

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import net.agkn.field_stripe.exception.OperationFailedException;
import net.agkn.field_stripe.record.IField;

/**
 * A {@link IFieldStripeWriterFactory} that creates {@link BinaryVLenFieldStripeWriter}
 * that write to {@link java.io.ByteArrayOutputStream byte-array-backed writers}  
 * to facilitate testing. The data are retrieved by {@link IField field} via
 * {@link #getByteArray(IField)}. 
 *
 * @author rgrzywinski
 */
public class TestBinaryVLenFieldStripeWriterFactory implements IFieldStripeWriterFactory {
    private final Map<IField, IFieldStripeWriter> fieldToFieldStripeWriterMap = new HashMap<IField, IFieldStripeWriter>();
    private final Map<IField, ByteArrayOutputStream> fieldToOutputStreamMap = new HashMap<IField, ByteArrayOutputStream>();
    private final int bufferSize;

    // ========================================================================
    /**
     * @param  bufferSize the size of the backing <code>byte</code> buffer. This
     *         cannot be negative.
     */
    public TestBinaryVLenFieldStripeWriterFactory(final int bufferSize) { this.bufferSize = bufferSize; }

    // ========================================================================
    /**
     * @param  field the {@link IField field} for which the {@link java.io.ByteArrayOutputStream}
     *         is desired. This cannot be <code>null</code>.
     * @return the {@link java.io.StringWriter} for the specified {@link IField field}.
     *         If the field is not known to this factory (i.e. it has never had
     *         {@link #createFieldStripeWriter(IField)} called on it) then 
     *         <code>null</code> will be returned.
     */
    public byte[] getByteArray(final IField field) {
        return fieldToOutputStreamMap.get(field).toByteArray();
    }

    // ========================================================================
    /**
     * Creates an {@link XMLFieldStripeWriter} backed by a {@link java.io.ByteArrayOutputStream}.
     * If the {@link IField field} is already known to this factory (i.e. this
     * method has already been called on the field) then {@link OperationFailedException}
     * will be thrown. 
     * 
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriterFactory#createFieldStripeWriter(net.agkn.field_stripe.record.IField)
     */
    @Override
    public IFieldStripeWriter createFieldStripeWriter(final IField field)
            throws OperationFailedException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(bufferSize);
        final BinaryVLenFieldStripeWriter fieldStripeWriter = new BinaryVLenFieldStripeWriter(outputStream, field);
        if(fieldToFieldStripeWriterMap.put(field, fieldStripeWriter) != null) throw new OperationFailedException("A binary field-stripe writer has already been created for field " + field + ".");
        fieldToOutputStreamMap.put(field, outputStream);

        return fieldStripeWriter;
    }

    // ========================================================================
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriterFactory#closeAllWriters()
     */
    @Override
    public void closeAllWriters() 
            throws OperationFailedException {
        // NOTE:  by contract, closing a closed writer has no effect
        for(final IFieldStripeWriter writer : fieldToFieldStripeWriterMap.values())
            writer.close();
    }
}