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

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import net.agkn.field_stripe.exception.NoSuchObjectException;
import net.agkn.field_stripe.exception.OperationFailedException;
import net.agkn.field_stripe.record.ICompositeType;
import net.agkn.field_stripe.record.IField;

/**
 * A {@link IFieldStripeReaderFactory} that creates {@link XMLFieldStripeReader}
 * that reads from a {@link java.io.Reader reader} (keyed by {@link IField})
 * specified on {@link #TestXMLFieldStripeReaderFactory(Map) construction}. 
 * This is provided to facilitate testing. 
 *
 * @author rgrzywinski
 */
public class TestXMLFieldStripeReaderFactory implements IFieldStripeReaderFactory {
    private final Map<IField, Reader> fieldToReaderMap = new HashMap<IField, Reader>();

    // ========================================================================
    /**
     * @param fieldToReaderMap the map of {@link IField field} to {@link java.io.Reader reader}
     *        that defines the set of field stripes available to be read. This
     *        cannot be <code>null</code> though it may be empty. A read-only
     *        shallow copy of the map will be made.
     */
    public TestXMLFieldStripeReaderFactory(final Map<IField, Reader> fieldToReaderMap) {
        this.fieldToReaderMap.putAll(fieldToReaderMap);
    }

    // ========================================================================
    /**
     * Creates an {@link XMLFieldStripeReader} backed by the {@link java.io.Reader}
     * specified on construction that is mapped so the specified {@link IField}. 
     * 
     * @see net.agkn.field_stripe.stripe.IFieldStripeReaderFactory#createFieldStripeReader(net.agkn.field_stripe.record.IField)
     */
    @Override
    public IFieldStripeReader createFieldStripeReader(final IField field)
            throws OperationFailedException {
        final Reader reader = fieldToReaderMap.get(field);
        if(reader == null) throw new NoSuchObjectException("There is no reader for field \"" + field.getName() + "\".");
        return new XMLFieldStripeReader(field, reader);
    }

    // ************************************************************************
    // convenience method

    /**
     * A convenience method for creating a {@link TestXMLFieldStripeReaderFactory}
     * that reads the field-stripes from the specified {@link TestXMLFieldStripeWriterFactory}
     * for the specified schema.
     * 
     * @param  schema the schema that defines the {@link IField fields} that 
     *         have been striped in the {@link TestXMLFieldStripeWriterFactory}.
     *         This cannot be <code>null</code>.
     * @param  fieldStripeWriterFactory the {@link TestXMLFieldStripeWriterFactory}
     *         from which the field-stripes are obtained. It must correspond to
     *         the specified factory. This cannot be <code>null</code>.
     * @return a {@link TestXMLFieldStripeReaderFactory} that creates {@link IFieldStripeReader readers}
     *         for the specified schema and whose field-stripe data is obtained
     *         from the specified {@link TestXMLFieldStripeWriterFactory}. This
     *         will never be <code>null</code>.
     * @throws OperationFailedException if any of the fields in the specified 
     *         schema cannot be found in the specified {@link TestXMLFieldStripeWriterFactory}
     *         or if a duplicate field is found. 
     */
    public static TestXMLFieldStripeReaderFactory createReaderFactory(final ICompositeType schema, final TestXMLFieldStripeWriterFactory fieldStripeWriterFactory) 
            throws OperationFailedException {
        // walk the fields in the schema, retrieve the StringWriter from the
        // writer factory, create a StringReader for it and add it to the 
        // field-to-reader map (for the reader factory).
        final Map<IField, Reader> fieldToReaderMap = new HashMap<IField, Reader>();
        populateFieldToReaderMap(schema, fieldStripeWriterFactory, fieldToReaderMap);

        return new TestXMLFieldStripeReaderFactory(fieldToReaderMap);
    }

    /**
     * Recursive method for walking the specified {@link ICompositeType schema},
     * retrieving the corresponding {@link java.io.StringWriter} from the
     * {@link TestXMLFieldStripeWriterFactory}, creating a {@link java.io.StringReader}
     * for it and adding it to the specified map.
     * 
     * @throws NoSuchObjectException if any of the fields in the specified 
     *         schema cannot be found in the specified {@link TestXMLFieldStripeWriterFactory}.
     * @throws OperationFailedException if a field already exists in the specified
     *         field-to-reader map.
     * @see #createReaderFactory(ICompositeType, TestXMLFieldStripeWriterFactory)
     */
    private static void populateFieldToReaderMap(final ICompositeType schema, final TestXMLFieldStripeWriterFactory fieldStripeWriterFactory, final Map<IField, Reader> fieldToReaderMap)
            throws NoSuchObjectException, OperationFailedException {
        for(final IField field : schema.getFields()) {
            if(field.getType() instanceof ICompositeType) 
                populateFieldToReaderMap((ICompositeType)field.getType(), fieldStripeWriterFactory, fieldToReaderMap);
            else {/*primitive field*/
                final StringWriter stringWriter = fieldStripeWriterFactory.getStringWriter(field);
                if(stringWriter == null) throw new NoSuchObjectException("No field-stripe for field: " + field.getName());
                if(fieldToReaderMap.put(field, new StringReader(stringWriter.toString())) != null) throw new OperationFailedException("Already mapped field: " + field.getName());
            }
        }
    }
}