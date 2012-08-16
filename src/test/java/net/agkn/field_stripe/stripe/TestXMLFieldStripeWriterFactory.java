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

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import net.agkn.field_stripe.exception.OperationFailedException;
import net.agkn.field_stripe.record.IField;

/**
 * A {@link IFieldStripeWriterFactory} that creates {@link XMLFieldStripeWriter}
 * that write to {@link java.io.StringWriter string-backed writers} to 
 * facilitate testing. The writers are retrieved by {@link IField field} via
 * {@link #getStringWriter(IField)}. 
 *
 * @author rgrzywinski
 */
public class TestXMLFieldStripeWriterFactory implements IFieldStripeWriterFactory {
    private final Map<IField, IFieldStripeWriter> fieldToFieldStripeWriterMap = new HashMap<IField, IFieldStripeWriter>();
    private final Map<IField, StringWriter> fieldToWriterMap = new HashMap<IField, StringWriter>();

    // ========================================================================
    /**
     * @param  field the {@link IField field} for which the {@link java.io.StringWriter}
     *         is desired. This cannot be <code>null</code>.
     * @return the {@link java.io.StringWriter} for the specified {@link IField field}.
     *         If the field is not known to this factory (i.e. it has never had
     *         {@link #createFieldStripeWriter(IField)} called on it) then 
     *         <code>null</code> will be returned.
     */
    public StringWriter getStringWriter(final IField field) {
        return fieldToWriterMap.get(field);
    }

    // ========================================================================
    /**
     * Creates an {@link XMLFieldStripeWriter} backed by a {@link java.io.StringWriter}.
     * If the {@link IField field} is already known to this factory (i.e. this
     * method has already been called on the field) then {@link OperationFailedException}
     * will be thrown. 
     * 
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriterFactory#createFieldStripeWriter(net.agkn.field_stripe.record.IField)
     */
    @Override
    public IFieldStripeWriter createFieldStripeWriter(final IField field)
            throws OperationFailedException {
        final StringWriter stringWriter = new StringWriter();
        final XMLFieldStripeWriter fieldStripeWriter = new XMLFieldStripeWriter(stringWriter);
        if(fieldToFieldStripeWriterMap.put(field, fieldStripeWriter) != null) throw new OperationFailedException("An XML field-stripe writer has already been created for field " + field + ".");
        fieldToWriterMap.put(field, stringWriter);

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