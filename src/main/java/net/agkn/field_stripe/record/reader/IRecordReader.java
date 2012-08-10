package net.agkn.field_stripe.record.reader;

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
import net.agkn.field_stripe.record.ICompositeType;

import org.apache.commons.lang.mutable.MutableBoolean;

/**
 * A streaming (SAX-like) reader for records.<p/>
 * 
 * Because the application knows what the structure of the data should look like
 * from the {@link ICompositeType schema), the paradigm used here is at its 
 * core an iterator/cursor but removes the check-the-next-token-and-switch-based-on-it 
 * approach. More specifically, the application asks for what it wants to see 
 * (e.g. {@link #readNextField()}) and the reader will throw an exception if 
 * that's not what it sees next. The only case where the application doesn't 
 * know what's next is at the end of a row -- {@link #hasMoreRecords()} informs 
 * the application if there are more records (rows) to read.<p/>
 *
 * @author rgrzywinski
 * @see IRecordWriter
 * @see ICompositeType
 */
public interface IRecordReader {
    /**
     * Starts the current record (row).
     * 
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called or before a corresponding {@link #endRecord()} has been 
     *         called.
     * @throws OperationFailedException if the start of the record could not be  
     *         read for any reason.
     * @see #hasMoreRecords()
     * @see #endRecord()
     */
    void startRecord()
        throws IllegalStateException, OperationFailedException;

    /**
     * Ends the current record (row).
     * 
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called or if called without a corresponding {@link #startRecord() start}.
     * @throws InvalidDataException if the end of the record could not be  
     *         read for any reason (such as the reader isn't at the end of the
     *         record)
     * @see #startRecord()
     * @see #hasMoreRecords()
     */
    // CHECK:  provide another method (or a boolean on this one) to allow for
    //         a premature "just end this record" for clean-up cases?
    void endRecord()
        throws IllegalStateException, InvalidDataException;

    /**
     * @return <code>true</code> if and only if there are more records (rows)
     *         to be read from this reader.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called or if a record has been started. (This can only be called
     *         after a record has {@link #endRecord() ended} and before one has
     *         been {@link #startRecord() started}.
     * @throws InvalidDataException if unexpected data (such as a newline or
     *         carriage return) was encountered and typically indicates a poorly  
     *         formed or incorrect record.
     * @see #startRecord()
     * @see #endRecord()
     */
    boolean hasMoreRecords()
        throws IllegalStateException, InvalidDataException;

    // ........................................................................
    /**
     * Reads and confirms that the start of a nested structure is next in the
     * stream. A record (row) does <i>not</i> start with a nested structure.
     * 
     * @return <code>true</code> if and only if the next field is a structure
     *         that has a value. <code>false</code> if the next field is a 
     *         structure but is unset.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws InvalidDataException if the start of a nested structure is not
     *         next in the stream and typically indicates a poorly formed or 
     *         incorrect record.
     * @see #endStructure()
     */
    boolean startStructure()
        throws IllegalStateException, InvalidDataException;

    /**
     * Reads and confirms that the end of a nested structure is next in the
     * stream. A record (row) does <i>not</i> end a nested structure. If no
     * structure existed (i.e. {@link #startStructure()} returned <code>false</code>)
     * then this <i>must not</i> be called (unlike arrays).
     * 
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called or if called without a corresponding call to {@link #startStructure()}.
     * @throws InvalidDataException if the end of a nested structure is not
     *         next in the stream and typically indicates a poorly formed or 
     *         incorrect record.
     * @see #startStructure()
     */
    void endStructure()
        throws IllegalStateException, InvalidDataException;

    // ........................................................................
    /**
     * Starts an array of elements. {@link #endArray()} must be called to end 
     * the array of elements. The existence of an array does <i>not</i> imply 
     * that there are elements in that array. {@link #hasMoreElements()} must
     * always be checked.
     *
     * @return <code>true</code> if and only if the next field is a non-empty
     *         array. <code>false</code> if the next field is an array but is
     *         empty -- {@link #endArray()} must be called regardless.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws InvalidDataException if the end of an array is not next in the 
     *         stream and typically indicates a poorly formed or incorrect record.
     * @see #hasMoreElements()
     * @see #endArray()
     */
    boolean startArray()
        throws IllegalStateException, InvalidDataException;

    /**
     * Are there more elements in this array. This may be called even in the
     * case where it is known that there are no elements in the array (i.e.
     * {@link #startArray()} returned <code>false</code>).
     * 
     * @return <code>true</code> if and only if there are more array elements
     *         to be read from this reader.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called or if a record has been started. (This can only be called
     *         after an array has {@link #startArray() started} and before it 
     *         has {@link #endArray() ended}.
     * @throws InvalidDataException if an unexpected character is encountered 
     *         or the array could not be read for any reason. Either typically 
     *         indicate a poorly formed or incorrect record.
     * @see #startArray()
     * @see #endArray()
     */
    boolean hasMoreElements()
        throws IllegalStateException, InvalidDataException;

    /**
     * Ends an array of elements.
     *
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called or if called without a corresponding {@link #startArray() start}.
     * @throws InvalidDataException if the start of an array is not next in the 
     *         stream and typically indicates a poorly formed or incorrect record.
     * @see #startArray()
     * @see #hasMoreElements()
     */
    void endArray()
        throws IllegalStateException, InvalidDataException;

    // ........................................................................
    /**
     * Reads and returns the next <code>byte</code> field (column). A 
     * {@link MutableBoolean mutable boolean} is used to indicate if the value
     * was set or not.
     * 
     * @param  isSet a {@link MutableBoolean mutable boolean} that indicates
     *         if the read value was set or not. This cannot be <code>null</code> 
     *         and the existing boolean value will be overridden.
     * @return the next field as a <code>byte</code>. If <code>isSet</code> is
     *         <code>false</code> then this value has no meaning.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws InvalidDataException if there are no more fields to be read or
     *         if the field could not be converted into a <code>byte</code>.
     */
    byte readByteField(MutableBoolean isSet)
        throws IllegalStateException, InvalidDataException;

    /**
     * Reads and returns the next <code>short</code> field (column). A 
     * {@link MutableBoolean mutable boolean} is used to indicate if the value
     * was set or not.
     * 
     * @param  isSet a {@link MutableBoolean mutable boolean} that indicates
     *         if the read value was set or not. This cannot be <code>null</code> 
     *         and the existing boolean value will be overridden.
     * @return the next field as a <code>short</code>. If <code>isSet</code> 
     *         is <code>false</code> then this value has no meaning.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws InvalidDataException if there are no more fields to be read or
     *         if the field could not be converted into a <code>short</code>.
     */
    short readShortField(MutableBoolean isSet)
        throws IllegalStateException, InvalidDataException;

    /**
     * Reads and returns the next <code>int</code> field (column). A 
     * {@link MutableBoolean mutable boolean} is used to indicate if the value
     * was set or not.
     * 
     * @param  isSet a {@link MutableBoolean mutable boolean} that indicates
     *         if the read value was set or not. This cannot be <code>null</code> 
     *         and the existing boolean value will be overridden.
     * @return the next field as an <code>int</code>. If <code>isSet</code> is 
     *         <code>false</code> then this value has no meaning.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws InvalidDataException if there are no more fields to be read or
     *         if the field could not be converted into an <code>int</code>.
     */
    int readIntField(MutableBoolean isSet)
        throws IllegalStateException, InvalidDataException;

    /**
     * Reads and returns the next <code>long</code> field (column). A 
     * {@link MutableBoolean mutable boolean} is used to indicate if the value
     * was set or not.
     * 
     * @param  isSet a {@link MutableBoolean mutable boolean} that indicates
     *         if the read value was set or not. This cannot be <code>null</code> 
     *         and the existing boolean value will be overridden.
     * @return the next field as a <code>long</code>. If <code>isSet</code> 
     *         is <code>false</code> then this value has no meaning.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws InvalidDataException if there are no more fields to be read or
     *         if the field could not be converted into a <code>long</code>.
     */
    long readLongField(MutableBoolean isSet)
        throws IllegalStateException, InvalidDataException;

    /**
     * Reads and returns the next <code>float</code> field (column). A 
     * {@link MutableBoolean mutable boolean} is used to indicate if the value
     * was set or not.
     * 
     * @param  isSet a {@link MutableBoolean mutable boolean} that indicates
     *         if the read value was set or not. This cannot be <code>null</code> 
     *         and the existing boolean value will be overridden.
     * @return the next field as a <code>float</code>. If <code>isSet</code> 
     *         is <code>false</code> then this value has no meaning.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws InvalidDataException if there are no more fields to be read or
     *         if the field could not be converted into a <code>float</code>.
     */
    float readFloatField(MutableBoolean isSet)
        throws IllegalStateException, InvalidDataException;

    /**
     * Reads and returns the next <code>double</code> field (column). A 
     * {@link MutableBoolean mutable boolean} is used to indicate if the value
     * was set or not.
     * 
     * @param  isSet a {@link MutableBoolean mutable boolean} that indicates
     *         if the read value was set or not. This cannot be <code>null</code> 
     *         and the existing boolean value will be overridden.
     * @return the next field as a <code>double</code>. If <code>isSet</code> 
     *         is <code>false</code> then this value has no meaning.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws InvalidDataException if there are no more fields to be read or
     *         if the field could not be converted into a <code>double</code>.
     */
    double readDoubleField(MutableBoolean isSet)
        throws IllegalStateException, InvalidDataException;

    /**
     * Reads and returns the next <code>boolean</code> field (column). A 
     * {@link MutableBoolean mutable boolean} is used to indicate if the value
     * was set or not.
     * 
     * @param  isSet a {@link MutableBoolean mutable boolean} that indicates
     *         if the read value was set or not. This cannot be <code>null</code> 
     *         and the existing boolean value will be overridden.
     * @return the next field as a <code>boolean</code>. If <code>isSet</code> 
     *         is <code>false</code> then this value has no meaning.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws InvalidDataException if there are no more fields to be read or
     *         if the field could not be converted into a <code>boolean</code>.
     */
    boolean readBooleanField(MutableBoolean isSet)
        throws IllegalStateException, InvalidDataException;

    /**
     * Reads and returns the next <code>String</code> field (column). A 
     * {@link MutableBoolean mutable boolean} is used to indicate if the value
     * was set or not.
     * 
     * @param  isSet a {@link MutableBoolean mutable boolean} that indicates
     *         if the read value was set or not. This cannot be <code>null</code> 
     *         and the existing boolean value will be overridden.
     * @return the next field as a <code>String</code>. If <code>isSet</code> 
     *         is <code>false</code> then this value has no meaning and may be
     *         (but isn't necessarily) <code>null</code>.
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws InvalidDataException if there are no more fields to be read or
     *         if the field could not be converted into a <code>String</code>.
     */
    // NOTE:  rather than returning null this continues the mutable boolean
    //        paradigmn for consistency
    String readStringField(MutableBoolean isSet)
        throws IllegalStateException, InvalidDataException;

    // ........................................................................
    /**
     * Skips the next field (column) in the record regardless of its type. This 
     * will attempt to determine the type so that composite fields (such as 
     * structures or arrays) can be skipped in their entirety.
     * 
     * @throws IllegalStateException if called after {@link #close()} has been
     *         called.
     * @throws InvalidDataException if there are no more fields to be read from 
     *         the record or an unknown field type is encountered. This 
     *         typically indicate a poorly formed or incorrect record.
     */
    void skipField()
        throws IllegalStateException, InvalidDataException;

    // ========================================================================
    /**
     * Closes this reader. The reader cannot be used after it has been closed. 
     * Closing a closed reader has no effect.
     * 
     * @throws OperationFailedException if the reader could not be closed for
     *         any reason. The reader should not be closed again (or, more
     *         specifically, closing a failed reader is undefined).
     */
    void close()
        throws OperationFailedException;
}