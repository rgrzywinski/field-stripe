package net.agkn.field_stripe.record.writer;

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

import net.agkn.common.exception.InvalidDataException;
import net.agkn.common.exception.OperationFailedException;
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.record.reader.IRecordReader;

/**
 * A streaming (SAX-like) writer for records.<p/>
 *
 * When finished using a writer it must be {@link #close() closed}.
 *
 * @author rgrzywinski
 * @see IRecordReader
 */
public interface IRecordWriter {
    /**
     * Starts the next record (row).
     * 
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called or before a corresponding {@link #endRecord()} has been 
     *         called.
     * @throws InvalidDataException if the field could not be written for any reason.
     * @see #endRecord()
     */
    void startRecord()
        throws IllegalStateException, InvalidDataException;
    
    /**
     * Ends the current record (row).
     * 
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called or if called without a corresponding {@link #startRecord() start}.
     * @throws InvalidDataException if the field could not be written for any reason.
     * @see #startRecord()
     */
    void endRecord()
        throws IllegalStateException, InvalidDataException;

    // ........................................................................
    /**
     * Begins the start of a structure block. A record (row) does <i>not</i>  
     * start with a structure. {@link #endStructure()} must be called to end
     * a structure.
     * 
     * @param  field the <code>IField</code> whose value is the structure being 
     *         started. This cannot be <code>null</code>.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws InvalidDataException if the field could not be written for any reason.
     * @see #endStructure()
     */
    void startStructure(IField field)
        throws IllegalStateException, InvalidDataException;

    /**
     * Ends a structure block. A record (row) does <i>not</i> end a structure.
     *
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called or if called without a corresponding {@link #startStructure(IField) start}.
     * @throws InvalidDataException if the field could not be written for any reason.
     * @see #startStructure(IField)
     */
    void endStructure()
        throws IllegalStateException, InvalidDataException;

    // ........................................................................
    /**
     * Starts an array of elements. {@link #endArray()} must be called to end 
     * the array of elements.
     *
     * @param  field the <code>IField</code> whose value is the array being 
     *         started. This cannot be <code>null</code>.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws InvalidDataException if the field could not be written for any reason.
     * @see #endArray()
     */
    void startArray(IField field)
        throws IllegalStateException, InvalidDataException;

    /**
     * Ends an array of elements.
     *
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called or if called without a corresponding {@link #startArray(IField) start}.
     * @throws InvalidDataException if the field could not be written for any reason.
     * @see #startArray(IField)
     */
    void endArray()
        throws IllegalStateException, InvalidDataException;

    // ........................................................................
    /**
     * Writes that the next field (column) is unset to the record.
     * 
     * @param  field the <code>IField</code> whose value is being written. This 
     *         cannot be <code>null</code>.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called or if called before the {@link #startRecord() record has been started}. 
     * @throws OperationFailedException if the field could not be written for 
     *         any reason.
     */
    void writeUnsetField(IField field)
        throws IllegalStateException, OperationFailedException;

    /**
     * Writes the next <code>byte</code> field (column) to the record.
     * 
     * @param  field the <code>IField</code> whose value is being written. This 
     *         cannot be <code>null</code>.
     * @param  value the next field value as a string to be written. 
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called or if called before the {@link #startRecord() record has been started}. 
     * @throws OperationFailedException if the field could not be written for 
     *         any reason.
     * @see #writeUnsetField(IField)
     */
    void writeField(IField field, byte value)
        throws IllegalStateException, OperationFailedException;

    /**
     * Writes the next <code>short</code> field (column) to the record.
     * 
     * @param  field the <code>IField</code> whose value is being written. This 
     *         cannot be <code>null</code>.
     * @param  value the next field value as a string to be written. 
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called or if called before the {@link #startRecord() record has been started}. 
     * @throws OperationFailedException if the field could not be written for 
     *         any reason.
     * @see #writeUnsetField(IField)
     */
    void writeField(IField field, short value)
        throws IllegalStateException, OperationFailedException;

    /**
     * Writes the next <code>int</code> field (column) to the record.
     * 
     * @param  field the <code>IField</code> whose value is being written. This 
     *         cannot be <code>null</code>.
     * @param  value the next field value as a string to be written. 
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called or if called before the {@link #startRecord() record has been started}. 
     * @throws OperationFailedException if the field could not be written for 
     *         any reason.
     * @see #writeUnsetField(IField)
     */
    void writeField(IField field, int value)
        throws IllegalStateException, OperationFailedException;

    /**
     * Writes the next <code>long</code> field (column) to the record.
     * 
     * @param  field the <code>IField</code> whose value is being written. This 
     *         cannot be <code>null</code>.
     * @param  value the next field value as a string to be written. 
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called or if called before the {@link #startRecord() record has been started}. 
     * @throws OperationFailedException if the field could not be written for 
     *         any reason.
     * @see #writeUnsetField(IField)
     */
    void writeField(IField field, long value)
        throws IllegalStateException, OperationFailedException;

    /**
     * Writes the next <code>float</code> field (column) to the record.
     * 
     * @param  field the <code>IField</code> whose value is being written. This 
     *         cannot be <code>null</code>.
     * @param  value the next field value as a string to be written. 
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called or if called before the {@link #startRecord() record has been started}. 
     * @throws OperationFailedException if the field could not be written for 
     *         any reason.
     * @see #writeUnsetField(IField)
     */
    void writeField(IField field, float value)
        throws IllegalStateException, OperationFailedException;

    /**
     * Writes the next <code>double</code> field (column) to the record.
     * 
     * @param  field the <code>IField</code> whose value is being written. This 
     *         cannot be <code>null</code>.
     * @param  value the next field value as a string to be written. 
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called or if called before the {@link #startRecord() record has been started}. 
     * @throws OperationFailedException if the field could not be written for 
     *         any reason.
     * @see #writeUnsetField(IField)
     */
    void writeField(IField field, double value)
        throws IllegalStateException, OperationFailedException;

    /**
     * Writes the next <code>boolean</code> field (column) to the record.
     * 
     * @param  field the <code>IField</code> whose value is being written. This 
     *         cannot be <code>null</code>.
     * @param  value the next field value as a string to be written. 
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called or if called before the {@link #startRecord() record has been started}. 
     * @throws OperationFailedException if the field could not be written for 
     *         any reason.
     * @see #writeUnsetField(IField)
     */
    void writeField(IField field, boolean value)
        throws IllegalStateException, OperationFailedException;

    /**
     * Writes the next <code>String</code> field (column) to the record.
     * 
     * @param  field the <code>IField</code> whose value is being written. This 
     *         cannot be <code>null</code>.
     * @param  value the next field value as a string to be written. 
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called or if called before the {@link #startRecord() record has been started}. 
     * @throws OperationFailedException if the field could not be written for 
     *         any reason.
     * @see #writeUnsetField(IField)
     */
    void writeField(IField field, String value)
        throws IllegalStateException, OperationFailedException;

    // ========================================================================
    /**
     * Flushes and closes this writer. The writer cannot be used after it has
     * been closed. Closing a closed writer has no effect.
     */
    void close();
}