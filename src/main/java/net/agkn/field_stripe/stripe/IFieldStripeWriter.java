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

import net.agkn.field_stripe.encode.IFieldStripeEncoder;
import net.agkn.field_stripe.exception.OperationFailedException;

/**
 * A writer for field stripes. The interface exposes all possible instructions
 * (and values) of an encoded field stripe. It is assumed that the {@link IFieldStripeEncoder encoders}
 * have any logic for optimizing away unnecessary states and that the writer
 * implementations are completely dumb.<p/>
 * 
 * The writers may require meta-data on construction as to the depth of the 
 * corresponding field. This can be used to provide fixed width fields for the
 * cases where a parent depth is specified.<p/>
 * 
 * A writer must be {@link #close() closed} in order to ensure that its contents
 * have been written to the underlying storage. 
 *
 * @author rgrzywinski
 * @see IFieldStripeReader
 */
public interface IFieldStripeWriter {
    // life-cycle

    /**
     * Closes this writer. The writer cannot be used after it has been closed. 
     * Closing a closed writer has no effect.
     * 
     * @throws OperationFailedException if the writer could not be closed for
     *         any reason. The writer should not be closed again (or, more
     *         specifically, closing a failed writer is undefined).
     */
    void close()
        throws OperationFailedException;

    // ========================================================================
    // meta-data

    /**
     * Writes an explicit <var>UNSET</var> marker.
     * 
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws OperationFailedException if the meta-data could not be written 
     *         for any reason. Subclasses may contain additional information as 
     *         to the nature of the failure.
     */
    void writeUnset()
        throws IllegalStateException, OperationFailedException;

    /**
     * @param  fieldDepth the depth of the <var>UNSET</var> parent. This cannot
     *         be negative or zero.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws OperationFailedException if the meta-data could not be written 
     *         for any reason. Subclasses may contain additional information as 
     *         to the nature of the failure.
     */
    void writeUnsetParent(int fieldDepth)
        throws IllegalStateException, OperationFailedException;

    /**
     * Writes that a field's value is repeated.
     * 
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws OperationFailedException if the meta-data could not be written 
     *         for any reason. Subclasses may contain additional information as 
     *         to the nature of the failure.
     */
    void writeRepeated()
        throws IllegalStateException, OperationFailedException;

    /**
     * @param  fieldDepth the depth of the parent field that is has repeated. 
     *         This will never be negative or zero.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws OperationFailedException if the meta-data could not be written 
     *         for any reason. Subclasses may contain additional information as 
     *         to the nature of the failure.
     */
    void writeRepeatedParent(int fieldDepth)
        throws IllegalStateException, OperationFailedException;

    // ========================================================================
    // primitive values

    /**
     * @param  value the <code>byte</code> value to be written. This can take
     *         on any value.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws OperationFailedException if the value could not be written for
     *         any reason. Subclasses may contain additional information as to
     *         the nature of the failure.
     */
    void writeValue(byte value)
        throws IllegalStateException, OperationFailedException;

    /**
     * @param  value the <code>short</code> value to be written. This can take
     *         on any value.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws OperationFailedException if the value could not be written for
     *         any reason. Subclasses may contain additional information as to
     *         the nature of the failure.
     */
    void writeValue(short value)
        throws IllegalStateException, OperationFailedException;

    /**
     * @param  value the <code>int</code> value to be written. This can take on
     *         any value.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws OperationFailedException if the value could not be written for
     *         any reason. Subclasses may contain additional information as to
     *         the nature of the failure.
     */
    void writeValue(int value)
        throws IllegalStateException, OperationFailedException;

    /**
     * @param  value the <code>long</code> value to be written. This can take
     *         on any value.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws OperationFailedException if the value could not be written for
     *         any reason. Subclasses may contain additional information as to
     *         the nature of the failure.
     */
    void writeValue(long value)
        throws IllegalStateException, OperationFailedException;

    /**
     * @param  value the <code>float</code> value to be written. This can take 
     *         on any value.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws OperationFailedException if the value could not be written for
     *         any reason. Subclasses may contain additional information as to
     *         the nature of the failure.
     */
    void writeValue(float value)
        throws IllegalStateException, OperationFailedException;

    /**
     * @param  value the <code>double</code> value to be written. This can take 
     *         on any value.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws OperationFailedException if the value could not be written for
     *         any reason. Subclasses may contain additional information as to
     *         the nature of the failure.
     */
    void writeValue(double value)
        throws IllegalStateException, OperationFailedException;

    /**
     * @param  value the <code>boolean</code> value to be written 
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws OperationFailedException if the value could not be written for
     *         any reason. Subclasses may contain additional information as to
     *         the nature of the failure.
     */
    void writeValue(boolean value)
        throws IllegalStateException, OperationFailedException;

    /**
     * @param  value the <code>String</code> value to be written. This can take 
     *         on any value but it cannot be <code>null</code> (in which case
     *         {@link #writeUnset()} must be used).
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws OperationFailedException if the value could not be written for
     *         any reason. Subclasses may contain additional information as to
     *         the nature of the failure.
     */
    void writeValue(String value)
        throws IllegalStateException, OperationFailedException;

    // CHECK:  provide for a byte blob?
}