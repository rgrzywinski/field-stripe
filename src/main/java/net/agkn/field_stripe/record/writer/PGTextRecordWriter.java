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

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Stack;

import net.agkn.common.exception.InvalidDataException;
import net.agkn.common.exception.OperationFailedException;
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.record.reader.PGTextRecordReader;
import net.jcip.annotations.NotThreadSafe;

/**
 * A {@link IRecordWriter writer} for records that are in the PostgreSQL 'text' 
 * COPY format. See <a href="http://www.postgresql.org/docs/9.0/static/sql-copy.html"><code>COPY</code></a>
 * for more information on the format. There is no meta-data that is output with
 * the data as there is in CSV output (for example) that contains a header. As 
 * such, all {@link IField field} information is ignored. Writing is done as 
 * pure strings for simplicity. This precludes any pretty-printing or other such 
 * features at this time.<p/>
 * 
 * Note that this parser is <i>not</i> thread-safe. Also note that no exceptions
 * are logged.
 *
 * @author rgrzywinski
 * @see PGTextRecordReader
 */
@NotThreadSafe
public class PGTextRecordWriter implements IRecordWriter {
    private final PrintWriter writer;
    private boolean isClosed = false/*by default not closed*/;

    // ------------------------------------------------------------------------
    // a stack of what is currently being nested (record, array or structure)
    private final Stack<State> nestingStack = new Stack<State>();

    // ========================================================================
    /**
     * @param  writer the <code>Writer</code> to which the snapshot is written.
     *         This cannot be <code>null</code>.
     */
    public PGTextRecordWriter(final Writer writer) {
        this.writer = new PrintWriter(writer);
    }

    // ========================================================================

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#startRecord()
     */
    @Override
    public void startRecord() 
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The snapshot writer has already been closed.")/*by contract*/;
        if(!nestingStack.isEmpty()) throw new IllegalStateException("A record has already been started in the snapshot writer.")/*by contract*/;
        nestingStack.push(new RecordState())/*by definition*/;

        // NOTE:  there is no explicit output format for a record start therefore
        //        there is nothing else to do
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#endRecord()
     */
    @Override
    public void endRecord() 
            throws IllegalStateException, InvalidDataException {
        final RecordState state;
        if(isClosed) throw new IllegalStateException("The snapshot writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty() || !(nestingStack.peek() instanceof RecordState)) throw new IllegalStateException("A record has not been started or is nested in the snapshot writer.")/*by contract*/;
        state = (RecordState)nestingStack.pop()/*by definition*/;

        // for consistency across platforms records (rows) are always terminated
        // with a unix-style newline ("\n"). (This corresponds to the "COPY TO STDOUT"
        // format of the COPY command -- http://www.postgresql.org/docs/9.0/static/sql-copy.html)
        state.buffer.append('\n');

        // write out the record
        writer.write(state.buffer.toString());
    }

    // ------------------------------------------------------------------------
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#startStructure(net.agkn.field_stripe.record.IField)
     */
    @Override
    public void startStructure(final IField field) 
            throws IllegalStateException, InvalidDataException {
        final StructureState state;
        if(isClosed) throw new IllegalStateException("The snapshot writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the snapshot writer.")/*by contract*/;
        nestingStack.push(state = new StructureState())/*by definition*/;

        state.buffer.append('(');
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#endStructure()
     */
    @Override
    public void endStructure() 
            throws IllegalStateException, InvalidDataException {
        final StructureState state;
        if(isClosed) throw new IllegalStateException("The snapshot writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty() || !(nestingStack.peek() instanceof StructureState)) throw new IllegalStateException("A record has not been started or nested in the snapshot writer.")/*by contract*/;
        state = (StructureState)nestingStack.pop()/*by definition*/;

        // terminate the structure and add the contents to the previous (now current) state
        state.buffer.append(')');
        nestingStack.peek().add(state.buffer);
    }

    // ------------------------------------------------------------------------
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#startArray(net.agkn.field_stripe.record.IField)
     */
    @Override
    public void startArray(IField field) 
            throws IllegalStateException, InvalidDataException {
        final ArrayState state;
        if(isClosed) throw new IllegalStateException("The snapshot writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the snapshot writer.")/*by contract*/;
        nestingStack.push(state = new ArrayState())/*by definition*/;

        state.buffer.append('{');
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#endArray()
     */
    @Override
    public void endArray() 
            throws IllegalStateException, InvalidDataException {
        final ArrayState state;
        if(isClosed) throw new IllegalStateException("The snapshot writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty() || !(nestingStack.peek() instanceof ArrayState)) throw new IllegalStateException("A record or array has not been started in the snapshot writer.")/*by contract*/;
        state = (ArrayState)nestingStack.pop()/*by definition*/;

        // terminate the array and add the contents to the previous (now current) state
        state.buffer.append('}');
        nestingStack.peek().add(state.buffer);
    }

    // ------------------------------------------------------------------------
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#writeUnsetField(net.agkn.field_stripe.record.IField)
     */
    @Override
    public void writeUnsetField(final IField field) 
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The snapshot writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the snapshot writer.")/*by contract*/;
        final State state = nestingStack.peek();

        state.addNull();
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#writeField(net.agkn.field_stripe.record.IField, byte)
     */
    @Override
    public void writeField(final IField field, final byte value)
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The snapshot writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the snapshot writer.")/*by contract*/;
        final State state = nestingStack.peek();

        state.add(value);
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#writeField(net.agkn.field_stripe.record.IField, short)
     */
    @Override
    public void writeField(final IField field, final short value)
            throws IllegalStateException, OperationFailedException {
        if(isClosed) throw new IllegalStateException("The snapshot writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the snapshot writer.")/*by contract*/;
        final State state = nestingStack.peek();

        state.add(value);
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#writeField(net.agkn.field_stripe.record.IField, int)
     */
    @Override
    public void writeField(final IField field, final int value)
            throws IllegalStateException, OperationFailedException {
        if(isClosed) throw new IllegalStateException("The snapshot writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the snapshot writer.")/*by contract*/;
        final State state = nestingStack.peek();

        state.add(value);
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#writeField(net.agkn.field_stripe.record.IField, long)
     */
    @Override
    public void writeField(final IField field, final long value)
            throws IllegalStateException, OperationFailedException {
        if(isClosed) throw new IllegalStateException("The snapshot writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the snapshot writer.")/*by contract*/;
        final State state = nestingStack.peek();

        state.add(value);
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#writeField(net.agkn.field_stripe.record.IField, float)
     */
    @Override
    public void writeField(final IField field, final float value)
            throws IllegalStateException, OperationFailedException {
        if(isClosed) throw new IllegalStateException("The snapshot writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the snapshot writer.")/*by contract*/;
        final State state = nestingStack.peek();

        state.add(value);
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#writeField(net.agkn.field_stripe.record.IField, double)
     */
    @Override
    public void writeField(final IField field, final double value)
            throws IllegalStateException, OperationFailedException {
        if(isClosed) throw new IllegalStateException("The snapshot writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the snapshot writer.")/*by contract*/;
        final State state = nestingStack.peek();

        state.add(value);
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#writeField(net.agkn.field_stripe.record.IField, boolean)
     */
    @Override
    public void writeField(final IField field, final boolean value)
            throws IllegalStateException, OperationFailedException {
        if(isClosed) throw new IllegalStateException("The snapshot writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the snapshot writer.")/*by contract*/;
        final State state = nestingStack.peek();

        state.add(value);
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#writeField(net.agkn.field_stripe.record.IField, java.lang.String)
     */
    @Override
    public void writeField(final IField field, final String value)
            throws IllegalStateException, OperationFailedException {
        if(isClosed) throw new IllegalStateException("The snapshot writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the snapshot writer.")/*by contract*/;
        final State state = nestingStack.peek();

        state.add(value);
    }

    // ========================================================================
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#close()
     */
    // CHECK:  is this even needed with the file writing delegated out?
    @Override
    public void close() {
        if(isClosed) return/*by contract*/;
        isClosed = true/*by definition*/;

        if(writer != null) {
            writer.close();

// CHECK: what's the best way to log?
//            if(writer.checkError()) snapshotLog.warn("An error occurred when the output writer was closed.");
        } /* else -- a writer was never created */
    }

    // ************************************************************************
    /**
     * Marker interface.
     */
    private static abstract class State { 
        /*protected*/ StringBuilder buffer = new StringBuilder();

        // adds the specified value to the buffer
        public abstract void addNull();
        public abstract void add(byte value);
        public abstract void add(short value);
        public abstract void add(int value);
        public abstract void add(long value);
        public abstract void add(float value);
        public abstract void add(double value);
        public abstract void add(boolean value);
        public abstract void add(CharSequence value);
    };

    /**
     * The state of a record.
     */
    private static final class RecordState extends State {
        public boolean comma = false/*set to true when an element is added*/;

        @Override public void addNull() { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append("\\N"/*literal null*/); }
        @Override public void add(final byte value) { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append(value); }
        @Override public void add(final short value) { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append(value); }
        @Override public void add(final int value) { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append(value); }
        @Override public void add(final long value) { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append(value); }
        @Override public void add(final float value) { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append(value); }
        @Override public void add(final double value) { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append(value); }
        @Override public void add(final boolean value) { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append(value); }
        @Override public void add(final CharSequence value) {
            if(comma) buffer.append(','); 
            comma = true/*by contract*/;

            // "...the following characters must be preceded by a backslash if 
            //  they appear as part of a column value: backslash itself, newline, 
            //  carriage return, and the current delimiter character."
            // The following escape sequences are emitted:
            //     \b   Backspace (ASCII 8)
            //     \f  Form feed (ASCII 12)
            //     \n  Newline (ASCII 10)
            //     \r  Carriage return (ASCII 13)
            //     \t  Tab (ASCII 9)
            //     \v  Vertical tab (ASCII 11)
            // "Presently, COPY TO will never emit an octal or hex-digits backslash 
            //  sequence, but it does use the other sequences listed above for 
            //  those control characters."
            // SEE:  http://www.postgresql.org/docs/9.0/static/sql-copy.html
            // SEE:  PostgreSQL src: ./src/backend/commands/copy.c: CopyAttributeOutText()
            if(value == null) {
                buffer.append("\\N");
                return/*nothing else can be done*/;
            } /* else -- the value is not null */
            final int length = value.length();
            for(int i=0; i<length; i++) {
                final char character = value.charAt(i);
                switch(character) {
                    case '\b':
                        buffer.append("\\b");
                        break;
                    case '\f':
                        buffer.append("\\f");
                        break;
                    case '\n':
                        buffer.append("\\n");
                        break;
                    case '\r':
                        buffer.append("\\r");
                        break;
                    case '\t':
                        buffer.append("\\t");
                        break;
                    case (char)0x0B/*'\v'*/:
                        buffer.append("\\v");
                        break;
                    case '\\':
                        buffer.append("\\\\");
                        break;

                    case ','/*delimiter character*/:
                        buffer.append('\\'/*escape*/).append(character);
                        break;

                    default:
                        buffer.append(character);
                        break;
                }
            }
        }
    }

    /**
     * The state of a structure.
     */
    private static final class StructureState extends State {
        public boolean comma = false/*set to true when an element is added*/;

        @Override public void addNull() { if(comma) buffer.append(','); comma = true/*by contract*/; /*nothing to addend*/ }
        @Override public void add(final byte value) { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append(value); }
        @Override public void add(final short value) { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append(value); }
        @Override public void add(final int value) { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append(value); }
        @Override public void add(final long value) { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append(value); }
        @Override public void add(final float value) { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append(value); }
        @Override public void add(final double value) { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append(value); }
        @Override public void add(final boolean value) { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append(value); }
        @Override public void add(final CharSequence value) {
            if(comma) buffer.append(','); 
            comma = true/*by contract*/;

            // SEE:  PostgreSQL src: src/backend/utils/adt/rowtypes.c: record_out()
            final int startingIndex = buffer.length()/*the start of this value*/;
            boolean surround = false/*set to true if is empty or contains " \ ( ) , or whitespace*/;

            // if the value is null then append nothing
            if(value == null) return/*nothing to be appended*/;

            // walk the sequence doubling up '"' and '\\'
            final int length = value.length();
            if(length < 1) surround = true/*by design*/;
            for(int i=0; i<length; i++) {
                final char character = value.charAt(i);
                if( (character == '"') || (character == '\\') ) {
                    buffer.append(character)/*double up*/;
                    surround = true/*by contract*/;
                } else if( (character == '(') || (character == ')') || (character == ',') ||
                           Character.isWhitespace(character)) {
                    surround = true/*by contract*/;
                } /* else -- not any of the characters to trigger surrounding in double quotes*/

                buffer.append(character);
            }

            if(surround) { 
                buffer.insert(startingIndex, '"')/*start at the start of the value*/;
                buffer.append('"');
            } /* else -- surrounding in double quotes is not needed */
        }
    }

    /**
     * The state of an array. Only a single dimensioned array is supported at
     * this time.
     */
    private static final class ArrayState extends State {
        public boolean comma = false/*set to true when an element is added*/;

        @Override public void addNull() { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append("NULL"/*null literal*/); }
        @Override public void add(final byte value) { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append(value); }
        @Override public void add(final short value) { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append(value); }
        @Override public void add(final int value) { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append(value); }
        @Override public void add(final long value) { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append(value); }
        @Override public void add(final float value) { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append(value); }
        @Override public void add(final double value) { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append(value); }
        @Override public void add(final boolean value) { if(comma) buffer.append(','); comma = true/*by contract*/; buffer.append(value); }
        @Override public void add(final CharSequence value) {
            if(comma) buffer.append(','); 
            comma = true/*by contract*/;

            // SEE:  PostgreSQL src: src/backend/utils/adt/arrayfuncs.c: array_out()
            final int startingIndex = buffer.length()/*the start of this value*/;
            boolean surround = false/*set to true if is empty, is "NULL" or contains " \ { } , or whitespace*/;

            // if the value is null then "NULL" is appended (without enclosing
            // in double quotes)
            if(value == null) {
                buffer.append("NULL"/*literal*/);
                return/*nothing else to append*/;
            } /* else -- the value is not null */

            // walk the sequence escaping '"' and '\\' with '\\'
            final int length = value.length();
            if(length < 1) surround = true/*by design*/;
            if((length == 4/*length("NULL")*/) && "NULL".contentEquals(value)) surround = true/*by design*/;
            for(int i=0; i<length; i++) {
                final char character = value.charAt(i);
                if( (character == '"') || (character == '\\') ) {
                    buffer.append('\\')/*escape*/;
                    surround = true/*by contract*/;
                } else if( (character == '{') || (character == '}') || (character == ',') ||
                           Character.isWhitespace(character)) {
                    surround = true/*by contract*/;
                } /* else -- not any of the characters to trigger surrounding in double quotes*/

                buffer.append(character);
            }

            if(surround) { 
                buffer.insert(startingIndex, '"')/*start at the start of the value*/;
                buffer.append('"');
            } /* else -- surrounding in double quotes is not needed */
        }
    }
}