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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import net.agkn.common.exception.NoSuchObjectException;
import net.agkn.common.exception.OperationFailedException;
import net.agkn.field_stripe.record.IField;

/**
 * A {@link IFieldStripeReaderFactory} that creates {@link BinaryVLenFieldStripeReader}
 * that reads from a {@link java.io.InputStream input streams} (keyed by {@link IField})
 * specified on {@link #TestBinaryVLenFieldStripeReaderFactory(Map) construction}. 
 * This is provided to facilitate testing. 
 *
 * @author rgrzywinski
 */
public class TestBinaryVLenFieldStripeReaderFactory implements IFieldStripeReaderFactory {
    private final Map<IField, InputStream> fieldToInputStreamMap = new HashMap<IField, InputStream>();

    // ========================================================================
    /**
     * @param fieldToInputStreamMap the map of {@link IField field} to {@link java.io.InputStream}
     *        that defines the set of field stripes available to be read. This
     *        cannot be <code>null</code> though it may be empty. A read-only
     *        shallow copy of the map will be made.
     */
    public TestBinaryVLenFieldStripeReaderFactory(final Map<IField, InputStream> fieldToInputStreamMap) {
        this.fieldToInputStreamMap.putAll(fieldToInputStreamMap);
    }

    // ========================================================================
    /**
     * Creates an {@link BinaryVLenFieldStripeReader} backed by the {@link java.io.InputStream}
     * specified on construction that is mapped so the specified {@link IField}. 
     * 
     * @see net.agkn.field_stripe.stripe.IFieldStripeReaderFactory#createFieldStripeReader(net.agkn.field_stripe.record.IField)
     */
    @Override
    public IFieldStripeReader createFieldStripeReader(final IField field)
            throws OperationFailedException {
        final InputStream inputStream = fieldToInputStreamMap.get(field);
        if(inputStream == null) throw new NoSuchObjectException("There is no input stream for field \"" + field.getName() + "\".");
        return new BinaryVLenFieldStripeReader(inputStream, field);
    }
}