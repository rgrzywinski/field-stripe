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

import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import net.agkn.field_stripe.exception.DeveloperException;
import net.agkn.field_stripe.exception.InvalidDataException;
import net.agkn.field_stripe.exception.OperationFailedException;
import net.jcip.annotations.NotThreadSafe;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import org.apache.commons.lang.mutable.MutableBoolean;

/**
 * A {@link IRecordReader reader} for records that are in JSON-array format.
 * Parsing is performed using the <a href="http://code.google.com/p/json-smart">JSON Smart</a>
 * parser. All structures are represented as JSON arrays and <i>not</i> JSON 
 * objects (or JSON structures).<p/>
 * 
 * Note that this parser is <i>not</i> thread-safe. Also note that no exceptions
 * are logged.
 *
 * @author rgrzywinski
 */
// CHECK:  how to best pull this implementation from the common implementation
//         that exists within AK?
@NotThreadSafe
public class SmartJsonArrayRecordReader implements IRecordReader {
    // the approach is to use JSON-Smart to parse the text into an object and
    // then walk and validate that object in the read methods

    private final JSONParser JSON_PARSER = new JSONParser(JSONParser.MODE_RFC4627/*strict*/);

    private final BufferedReader reader;
    private boolean isClosed = false/*by default not closed*/;

    // 'rawRecord' is the next record to be parsed. This will be null if the
    // next record is to be read (or if there are no more records). 'parseRecord'
    // is the JSON object of 'rawRecord'.
    private String rawRecord/*updated in #hasMoreRecords()*/ = null/*no records to start*/;
    private Object parsedRecord/*updated each #startRecord()*/ = null/*matches 'rawRecord'*/;

    // this is only populated after #startEvent() is called
    private final Stack<IParseState> stack = new ObjectArrayList<IParseState>();
    private IParseState currentState/*purely for convenience -- it equals stack.top()*/ = null/*none by default*/;

    // ========================================================================
    /**
     * @param  reader the <code>Reader</code> from which the record is read. 
     *         This cannot be <code>null</code>.
     * @throws InvalidDataException if the specified raw event string could not
     *         be parsed as a JSON event. The exception will have been logged
     *         to the "invalid-event" log.
     */
    public SmartJsonArrayRecordReader(final Reader reader) {
        this.reader = new BufferedReader(reader);
    }

    // ========================================================================
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.reader.IRecordReader#startRecord()
     */
    @SuppressWarnings("unchecked")
    @Override
    public final void startRecord() 
            throws IllegalStateException, InvalidDataException {
        // NOTE: #hasMoreRecords() ensures that 'rawRecord' is set (if there are
        //       more records)
        if(!hasMoreRecords()) throw new InvalidDataException("There are no more records to read.");

        // parse the record from the current raw record
        try {
            this.parsedRecord = JSON_PARSER.parse(rawRecord);
        } catch(final ParseException pe) {
            throw new InvalidDataException(pe);
        }

        // push the state onto the stack
        if(!(parsedRecord instanceof List)) throw new InvalidDataException("Expected the record to start with a JSON array.");
        stack.push(currentState = new ParseState((List<Object>)parsedRecord));
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.reader.IRecordReader#endRecord()
     */
    @Override
    public final void endRecord() 
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(stack.isEmpty()) throw new IllegalStateException("A record has not been started in the record reader.")/*by contract*/;
        if(!currentState.reachedEnd()) throw new InvalidDataException("The end of the record has not been reached.");

        // pop the current state off of the stack. The result should be an empty
        // stack.
        stack.pop();
        if(!stack.isEmpty()) throw new IllegalStateException("Improperly nested record.");
        currentState = null/*by contract*/;
        rawRecord = null/*by contract*/;
        parsedRecord = null/*by contract (to match 'rawRecord')*/;
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.reader.IRecordReader#hasMoreRecords()
     */
    @Override
    public final boolean hasMoreRecords() 
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(!stack.isEmpty()) throw new IllegalStateException("A record has already been started in the record reader.")/*by contract*/;

        // since this cannot be called within a record (see stack check above)
        // then a non-null 'rawRecord' indicates that this method was already
        // called and there was another record read.
        if(rawRecord != null) return true/*by definition*/;

        // read the next record. If there is a non-null result then there is a record
        try {
            rawRecord = reader.readLine();
            return (rawRecord != null);
        } catch(final IOException ioe) {
            throw new InvalidDataException(ioe.getLocalizedMessage());
        }
    }

    // ------------------------------------------------------------------------
    // NOTE:  since this format forces events, arrays and objects all to be
    //        represented in the same way, the code is identical (except for
    //        the exception message)
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.reader.IRecordReader#startStructure()
     */
    @SuppressWarnings("unchecked")
    @Override
    public final boolean startStructure() 
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(stack.isEmpty()) throw new IllegalStateException("A record has not been started in the record reader.")/*by contract*/;

        // NOTE:  structures are arrays
        // there are three options for the field at this point:
        // 1.  null (allowed);
        // 2.  not an array (error);
        // 3.  an object array (expected);
        final Object value = currentState.getField();
        if(value == null) return false/*by contract*/;
        if(!(value instanceof List)) throw new InvalidDataException("Expected a JSON array but found \"" + value +"\".");

        stack.push(currentState = new ParseState((List<Object>)value));

        return true/*by contract*/;
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.reader.IRecordReader#endStructure()
     */
    @Override
    public final void endStructure() 
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(stack.isEmpty()) throw new IllegalStateException("A record has not been started in the record reader.")/*by contract*/;
        if(!currentState.reachedEnd()) throw new InvalidDataException("Expected the structure to end with a JSON array.");

        // pop the current state off of the stack. The result should be an empty
        // stack.
        stack.pop();
        if(stack.isEmpty()) throw new IllegalStateException("Improperly nested record.");
        currentState = stack.top();
    }

    // ------------------------------------------------------------------------
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.reader.IRecordReader#startArray()
     */
    @SuppressWarnings("unchecked")
    @Override
    public final boolean startArray() 
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(stack.isEmpty()) throw new IllegalStateException("A record has not been started in the record reader.")/*by contract*/;

        // there are three options for the field at this point:
        // 1.  null (allowed);
        // 2.  not an array (error);
        // 3.  an object array (expected);
        final Object value = currentState.getField();
        if(value == null) {
            stack.push(currentState = new EmptyParseState());
            return false/*by contract*/;
        } /* else -- there is a value*/;
        if(!(value instanceof List)) throw new InvalidDataException("Expected a JSON array but found \"" + value +"\".");

        stack.push(currentState = new ParseState((List<Object>)value));
        return true/*by contract*/;
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.reader.IRecordReader#hasMoreElements()
     */
    @Override
    public final boolean hasMoreElements() 
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(stack.isEmpty()) throw new IllegalStateException("A record has not been started in the record reader.")/*by contract*/;
        // NOTE: this does not track if #startArray() has been called as there's
        //       no obvious way to do that (without using different objects on
        //       the stack -- which is a possibility)

        return !currentState.reachedEnd();
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.reader.IRecordReader#endArray()
     */
    @Override
    public final void endArray() 
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(stack.isEmpty()) throw new IllegalStateException("A record has not been started in the record reader.")/*by contract*/;
        if(!currentState.reachedEnd()) throw new InvalidDataException("Expected the array to end with a JSON array.");

        // pop the current state off of the stack. The result should be an empty
        // stack.
        stack.pop();
        if(stack.isEmpty()) throw new IllegalStateException("Improperly nested event.");
        currentState = stack.top();
    }

    // ------------------------------------------------------------------------
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.IRecordReader#readByteField(org.apache.commons.lang.mutable.MutableBoolean)
     */
    @Override
    public byte readByteField(final MutableBoolean isSet)
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(stack.isEmpty()) throw new IllegalStateException("A record has not been started in the record reader.")/*by contract*/;

        final Object jsonObject = currentState.getField();
        if(jsonObject == null) {
            isSet.setValue(false/*not set*/);
            return -1/*any value allowed*/;
        } /* else -- a non-null value */
        if(!(jsonObject instanceof Number)) throw new InvalidDataException("Expected a byte field but found \"" + jsonObject + "\".");
        isSet.setValue(true/*set*/);
        return ((Number)jsonObject).byteValue();
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.IRecordReader#readShortField(org.apache.commons.lang.mutable.MutableBoolean)
     */
    @Override
    public short readShortField(final MutableBoolean isSet)
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(stack.isEmpty()) throw new IllegalStateException("A record has not been started in the record reader.")/*by contract*/;

        final Object jsonObject = currentState.getField();
        if(jsonObject == null) {
            isSet.setValue(false/*not set*/);
            return -1/*any value allowed*/;
        } /* else -- a non-null value */
        if(!(jsonObject instanceof Number)) throw new InvalidDataException("Expected a short field but found \"" + jsonObject + "\".");
        isSet.setValue(true/*set*/);
        return ((Number)jsonObject).shortValue();
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.IRecordReader#readIntField(org.apache.commons.lang.mutable.MutableBoolean)
     */
    @Override
    public int readIntField(final MutableBoolean isSet)
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(stack.isEmpty()) throw new IllegalStateException("A record has not been started in the record reader.")/*by contract*/;

        final Object jsonObject = currentState.getField();
        if(jsonObject == null) {
            isSet.setValue(false/*not set*/);
            return -1/*any value allowed*/;
        } /* else -- a non-null value */
        if(!(jsonObject instanceof Number)) throw new InvalidDataException("Expected an integer field but found \"" + jsonObject + "\".");
        isSet.setValue(true/*set*/);
        return ((Number)jsonObject).intValue();
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.IRecordReader#readLongField(org.apache.commons.lang.mutable.MutableBoolean)
     */
    @Override
    public long readLongField(final MutableBoolean isSet)
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(stack.isEmpty()) throw new IllegalStateException("A record has not been started in the record reader.")/*by contract*/;

        final Object jsonObject = currentState.getField();
        if(jsonObject == null) {
            isSet.setValue(false/*not set*/);
            return -1L/*any value allowed*/;
        } /* else -- a non-null value */
        if(!(jsonObject instanceof Number)) throw new InvalidDataException("Expected a long field but found \"" + jsonObject + "\".");
        isSet.setValue(true/*set*/);
        return ((Number)jsonObject).longValue();
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.IRecordReader#readFloatField(org.apache.commons.lang.mutable.MutableBoolean)
     */
    @Override
    public float readFloatField(final MutableBoolean isSet)
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(stack.isEmpty()) throw new IllegalStateException("A record has not been started in the record reader.")/*by contract*/;

        final Object jsonObject = currentState.getField();
        if(jsonObject == null) {
            isSet.setValue(false/*not set*/);
            return -1.0f/*any value allowed*/;
        } /* else -- a non-null value */
        if(!(jsonObject instanceof Number)) throw new InvalidDataException("Expected a float field but found \"" + jsonObject + "\".");
        isSet.setValue(true/*set*/);
        return ((Number)jsonObject).floatValue();
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.IRecordReader#readDoubleField(org.apache.commons.lang.mutable.MutableBoolean)
     */
    @Override
    public double readDoubleField(final MutableBoolean isSet)
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(stack.isEmpty()) throw new IllegalStateException("A record has not been started in the record reader.")/*by contract*/;

        final Object jsonObject = currentState.getField();
        if(jsonObject == null) {
            isSet.setValue(false/*not set*/);
            return -1.0/*any value allowed*/;
        } /* else -- a non-null value */
        if(!(jsonObject instanceof Number)) throw new InvalidDataException("Expected a double field but found \"" + jsonObject + "\".");
        isSet.setValue(true/*set*/);
        return ((Number)jsonObject).doubleValue();
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.IRecordReader#readBooleanField(org.apache.commons.lang.mutable.MutableBoolean)
     */
    @Override
    public boolean readBooleanField(final MutableBoolean isSet)
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(stack.isEmpty()) throw new IllegalStateException("A record has not been started in the record reader.")/*by contract*/;

        final Object jsonObject = currentState.getField();
        if(jsonObject == null) {
            isSet.setValue(false/*not set*/);
            return false/*any value allowed*/;
        } /* else -- a non-null value */
        if(!(jsonObject instanceof Boolean)) throw new InvalidDataException("Expected a boolean field but found \"" + jsonObject + "\".");
        isSet.setValue(true/*set*/);
        return (Boolean)jsonObject;
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.IRecordReader#readStringField(org.apache.commons.lang.mutable.MutableBoolean)
     */
    @Override
    public String readStringField(final MutableBoolean isSet)
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(stack.isEmpty()) throw new IllegalStateException("A record has not been started in the record reader.")/*by contract*/;

        final Object jsonObject = currentState.getField();
        if(jsonObject == null) {
            isSet.setValue(false/*not set*/);
            return ""/*any value allowed*/;
        } /* else -- a non-null value */
        if(!(jsonObject instanceof String)) throw new InvalidDataException("Expected a string field but found \"" + jsonObject + "\".");
        isSet.setValue(true/*set*/);
        return (String)jsonObject;
    }

    // ------------------------------------------------------------------------
    /* (non-Javadoc)
     * @see net.agkn.summarizer.event.parser.IInputEventParser#skipField()
     */
    @Override
    public final void skipField()
            throws InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(stack.isEmpty()) throw new IllegalStateException("A record has not been started in the record reader.")/*by contract*/;

        // retrieve the field and do nothing with it
        currentState.getField();
    }

    // ========================================================================
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.reader.IRecordReader#close()
     */
    @Override
    public void close() 
            throws OperationFailedException {
        if(isClosed) return/*by contract*/;
        isClosed = true/*by definition*/;

        try {
            reader.close();
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    // ************************************************************************
    /**
     * The current parsing state stored on a stack to support nested structures
     * and arrays.
     */
    private static interface IParseState {
        /**
         * Retrieves the object at the current index and advances the index to
         * prepare for the next read.
         * 
         * @return the object at the current index position. This may be <code>null</code>.
         */
        Object getField();

        /**
         * @return <code>true</code> if and only if the index points to the 
         *         end of the objects -- more specifically, if it points to an
         *         index that is equals to the number of objects.
         */
        boolean reachedEnd();
    }
    
    /**
     * The current parsing state based on an array of objects.
     */
    private static final class ParseState implements IParseState {
        // the list of objects that is the current level of nesting
        private final List<Object> objects;

        // the index of the object that is to be read next
        private int index;

        // --------------------------------------------------------------------
        /**
         * Sets {@link #objects} equal to the specified object. The index 
         * defaults to <code>0</code>.
         * 
         * @param  objects the objects that are the current parse state. This
         *         cannot be <code>null</code>.
         */
        public ParseState(final List<Object> objects) {
            this.objects = objects;
            this.index = 0/*by contract*/;
        }

        // --------------------------------------------------------------------
        /* (non-Javadoc)
         * @see net.agkn.field_stripe.record.reader.SmartJsonArrayRecordReader.IParseState#getField()
         */
        @Override
        public final Object getField() { return objects.get(index++); }

        /* (non-Javadoc)
         * @see net.agkn.field_stripe.record.reader.SmartJsonArrayRecordReader.IParseState#reachedEnd()
         */
        @Override
        public final boolean reachedEnd() { return (index == objects.size()); }
    }

    /**
     * An always-empty parsing state.
     */
    private static final class EmptyParseState implements IParseState {
        /* (non-Javadoc)
         * @see net.agkn.field_stripe.record.reader.SmartJsonArrayRecordReader.IParseState#getField()
         */
        @Override
        public final Object getField() { throw new DeveloperException("There are no objects to return."); }

        /* (non-Javadoc)
         * @see net.agkn.field_stripe.record.reader.SmartJsonArrayRecordReader.IParseState#reachedEnd()
         */
        @Override
        public final boolean reachedEnd() { return true/*by contract*/; }
    }
}