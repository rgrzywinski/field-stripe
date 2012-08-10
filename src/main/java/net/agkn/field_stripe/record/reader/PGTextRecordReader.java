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

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.Stack;

import net.agkn.common.exception.DeveloperException;
import net.agkn.common.exception.InvalidDataException;
import net.agkn.common.exception.OperationFailedException;
import net.jcip.annotations.NotThreadSafe;

import org.apache.commons.lang.mutable.MutableBoolean;

/**
 * A {@link IRecordReader reader} for records that are in the PostgreSQL 'text' 
 * COPY format. See <a href="http://www.postgresql.org/docs/9.0/static/sql-copy.html"><code>COPY</code></a>
 * for more information on the format.<p/>
 * 
 * Note that this parser is <i>not</i> thread-safe. Also note that no exceptions
 * are logged.
 * 
 * @author rgrzywinski
 */
// CHECK:  how to best pull this implementation from the common implementation
//         that exists within AK?
@NotThreadSafe
public class PGTextRecordReader implements IRecordReader {
    // NOTE: from T&E with PostgreSQL's COPY FROM, there cannot be any blank 
    //       records (records that only contain the newline or carriage return
    //       character)

    // ------------------------------------------------------------------------
    private static final int EOS = -1/*end-of-stream*/;

    private static final int CR = '\r';
    private static final int LF = '\n';

    // ************************************************************************
    // NOTE:  a PushbackReader is buffered by definition
    private final PushbackReader reader;
    private boolean isClosed = false/*by default not closed*/;

    // ------------------------------------------------------------------------
    // a stack of what is currently being nested (record, array or structure)
    private final Stack<IState> nestingStack = new Stack<IState>();

    // ========================================================================
    /**
     * @param  reader the <code>Reader</code> from which the record is read. 
     *         This cannot be <code>null</code>.
     */
    public PGTextRecordReader(final Reader reader) {
        this.reader = new PushbackReader(reader);
    }

    // ========================================================================
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.reader.IRecordReader#startRecord()
     */
    @Override
    public final void startRecord() 
            throws IllegalStateException, OperationFailedException {
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(!nestingStack.isEmpty()) throw new IllegalStateException("A record has already been started in the record reader.")/*by contract*/;
        nestingStack.push(new RecordState(reader))/*by definition*/;
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.reader.IRecordReader#endRecord()
     */
    @Override
    public final void endRecord() 
            throws IllegalStateException, InvalidDataException {
        final RecordState state;
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty() || !(nestingStack.peek() instanceof RecordState)) throw new IllegalStateException("A record has not been started or is nested in the record reader.")/*by contract*/;
        state = (RecordState)nestingStack.pop()/*by definition*/;

        // ensure that the end-of-record was reached
        state.confirmEndRecord();
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.reader.IRecordReader#hasMoreRecords()
     */
    @Override
    public final boolean hasMoreRecords() 
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(!nestingStack.isEmpty()) throw new IllegalStateException("A record has been started in the record reader. This can only be called after a record has finished.")/*by contract*/;

        // peek ahead at the next character. If it is EOS then there are no more
        // records. If it is a newline or carriage return then an error is thrown.
        try {
            final int character = reader.read();
            if(character == EOS) return false/*no more records*/;
            if( (character == CR) || (character == LF) ) throw new InvalidDataException("Encountered unexpected newline or carriage return.");
            reader.unread(character)/*put the character back -- part of next record*/;
        } catch(final IOException ioe) {
            throw new InvalidDataException("Could not read the next character.", ioe);
        }

        return true/*there are more records*/;
    }

    // ------------------------------------------------------------------------
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.reader.IRecordReader#startStructure()
     */
    @Override
    public final boolean startStructure() 
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the record reader.")/*by contract*/;

        // get the next field from the current state and apply it to the nested
        // structure state
        final IState currentState = nestingStack.peek();
        final CharSequence field = currentState.readField();
        if(field == null) return false/*by contract*/;
        nestingStack.push(new StructureState(field))/*by definition*/;

        return true/*non-null structure*/;
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.reader.IRecordReader#endStructure()
     */
    @Override
    public final void endStructure() 
            throws IllegalStateException, InvalidDataException {
        final StructureState state;
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty() || !(nestingStack.peek() instanceof StructureState)) throw new IllegalStateException("A record has not been started or nested in the record reader.")/*by contract*/;
        state = (StructureState)nestingStack.pop()/*by definition*/;

        // ensure that the end-of-structure delimiter is found
        state.confirmEndStructure();
    }

    // ------------------------------------------------------------------------
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.reader.IRecordReader#startArray()
     */
    @Override
    public final boolean startArray() 
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the record reader.")/*by contract*/;

        // get the next field from the current state and apply it to the nested
        // array state
        final IState currentState = nestingStack.peek();
        final CharSequence field = currentState.readField();
        if(field == null) {
            nestingStack.push(new EmptyArrayState())/*by definition*/;
            return false/*by contract*/;
        } /* else -- there is a field that contains the array */
        nestingStack.push(new ArrayState(field))/*by definition*/;
        return true/*non-null array*/;
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.reader.IRecordReader#hasMoreElements()
     */
    @Override
    public final boolean hasMoreElements() 
            throws IllegalStateException, InvalidDataException {
        final IArrayState state;
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty() || !(nestingStack.peek() instanceof IArrayState)) throw new IllegalStateException("An array has not been started in the record reader.")/*by contract*/;
        state = (IArrayState)nestingStack.peek();

        return state.hasMoreElements();
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.reader.IRecordReader#endArray()
     */
    @Override
    public final void endArray() 
            throws IllegalStateException, InvalidDataException {
        final IArrayState state;
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty() || !(nestingStack.peek() instanceof IArrayState)) throw new IllegalStateException("An array has not been started in the record reader.")/*by contract*/;
        state = (IArrayState)nestingStack.pop()/*by definition*/;

        // ensure that the end-of-array delimiter is found
        state.confirmEndArray();
    }

    // ------------------------------------------------------------------------
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.IRecordReader#readByteField(org.apache.commons.lang.mutable.MutableBoolean)
     */
    @Override
    public byte readByteField(final MutableBoolean isSet)
            throws IllegalStateException, InvalidDataException {
        final String fieldString = readStringField();
        if((fieldString == null) || (fieldString.length() == 0)) {
            isSet.setValue(false/*not set*/);
            return -1/*any value allowed*/;
        } /* else -- a non-null value */
        try {
            isSet.setValue(true/*set*/);
            return Byte.parseByte(fieldString);
        } catch(final NumberFormatException nfe) {
            throw new InvalidDataException("Could not convert \"" + fieldString + "\" to a byte.", nfe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.IRecordReader#readShortField(org.apache.commons.lang.mutable.MutableBoolean)
     */
    @Override
    public short readShortField(final MutableBoolean isSet)
            throws IllegalStateException, InvalidDataException {
        final String fieldString = readStringField();
        if((fieldString == null) || (fieldString.length() == 0)) {
            isSet.setValue(false/*not set*/);
            return -1/*any value allowed*/;
        } /* else -- a non-null value */
        try {
            isSet.setValue(true/*set*/);
            return Short.parseShort(fieldString);
        } catch(final NumberFormatException nfe) {
            throw new InvalidDataException("Could not convert \"" + fieldString + "\" to a short.", nfe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.IRecordReader#readIntField(org.apache.commons.lang.mutable.MutableBoolean)
     */
    @Override
    public int readIntField(final MutableBoolean isSet)
            throws IllegalStateException, InvalidDataException {
        final String fieldString = readStringField();
        if((fieldString == null) || (fieldString.length() == 0)) {
            isSet.setValue(false/*not set*/);
            return -1/*any value allowed*/;
        } /* else -- a non-null value */
        try {
            isSet.setValue(true/*set*/);
            return Integer.parseInt(fieldString);
        } catch(final NumberFormatException nfe) {
            throw new InvalidDataException("Could not convert \"" + fieldString + "\" to an integer.", nfe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.IRecordReader#readLongField(org.apache.commons.lang.mutable.MutableBoolean)
     */
    @Override
    public long readLongField(final MutableBoolean isSet)
            throws IllegalStateException, InvalidDataException {
        final String fieldString = readStringField();
        if((fieldString == null) || (fieldString.length() == 0)) {
            isSet.setValue(false/*not set*/);
            return -1L/*any value allowed*/;
        } /* else -- a non-null value */
        try {
            isSet.setValue(true/*set*/);
            return Long.parseLong(fieldString);
        } catch(final NumberFormatException nfe) {
            throw new InvalidDataException("Could not convert \"" + fieldString + "\" to a long.", nfe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.IRecordReader#readFloatField(org.apache.commons.lang.mutable.MutableBoolean)
     */
    @Override
    public float readFloatField(final MutableBoolean isSet)
            throws IllegalStateException, InvalidDataException {
        final String fieldString = readStringField();
        if((fieldString == null) || (fieldString.length() == 0)) {
            isSet.setValue(false/*not set*/);
            return -1.0f/*any value allowed*/;
        } /* else -- a non-null value */
        try {
            isSet.setValue(true/*set*/);
            return Float.parseFloat(fieldString);
        } catch(final NumberFormatException nfe) {
            throw new InvalidDataException("Could not convert \"" + fieldString + "\" to a float.", nfe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.IRecordReader#readDoubleField(org.apache.commons.lang.mutable.MutableBoolean)
     */
    @Override
    public double readDoubleField(final MutableBoolean isSet)
            throws IllegalStateException, InvalidDataException {
        final String fieldString = readStringField();
        if((fieldString == null) || (fieldString.length() == 0)) {
            isSet.setValue(false/*not set*/);
            return -1.0/*any value allowed*/;
        } /* else -- a non-null value */
        try {
            isSet.setValue(true/*set*/);
            return Double.parseDouble(fieldString);
        } catch(final NumberFormatException nfe) {
            throw new InvalidDataException("Could not convert \"" + fieldString + "\" to a double.", nfe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.IRecordReader#readBooleanField(org.apache.commons.lang.mutable.MutableBoolean)
     */
    @Override
    public boolean readBooleanField(final MutableBoolean isSet)
            throws IllegalStateException, InvalidDataException {
        final String fieldString = readStringField();
        if((fieldString == null) || (fieldString.length() == 0)) {
            isSet.setValue(false/*not set*/);
            return false/*any value allowed*/;
        } /* else -- a non-null value */
        try {
            // CHECK:  are there more values that PG allows for "true"?
            isSet.setValue(true/*set*/);
            return Boolean.parseBoolean(fieldString);
        } catch(final NumberFormatException nfe) {
            throw new InvalidDataException("Could not convert \"" + fieldString + "\" to a boolean.", nfe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.IRecordReader#readStringField(org.apache.commons.lang.mutable.MutableBoolean)
     */
    @Override
    public String readStringField(final MutableBoolean isSet)
            throws IllegalStateException, InvalidDataException {
        final String fieldString = readStringField();
        if((fieldString == null) || (fieldString.length() == 0)) {
            isSet.setValue(false/*not set*/);
            return ""/*any value allowed*/;
        } /* else -- a non-null value */

        isSet.setValue(true/*set*/);
        return fieldString;
    }

    // ........................................................................
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.reader.IRecordReader#readStringField()
     */
    private final String readStringField() 
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the record reader.")/*by contract*/;
        final IState state = nestingStack.peek();

        final CharSequence field = state.readField();
        if(field == null) return null/*by definition*/;
        return field.toString();
    }

    // ------------------------------------------------------------------------
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.reader.IRecordReader#skipField()
     */
    @Override
    public void skipField() 
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record reader has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the record reader.")/*by contract*/;

        // retrieve the field and do nothing with it
        final IState state = nestingStack.peek();
            state.readField();
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
     * Marker interface.
     */
    private interface IState { 
        /**
         * Reads the next field from the buffer and returns the results.
         * 
         * @return the <code>CharSequence</code> for the next field in the buffer.
         *         Empty means empty and <code>null</code> means <code>null</code>.
         *         If the result is non-<code>null</code> then it may be used
         *         directly by the caller without concern of it changing out
         *         from under it.
         * @throws InvalidDataException if there were no more fields to read.
         */
        CharSequence readField() 
            throws InvalidDataException;
    };

    /**
     * The state of a record.
     */
    private final static class RecordState implements IState {
        private final StringBuilder buffer = new StringBuilder();
        private int position = 0/*start at the beginning of the buffer for reads*/;

        // --------------------------------------------------------------------
        // reads an entire record into the buffer from the specified PushbackReader. 
        // The end-of-record separator is read but not added to the buffer.
        // SEE:  PostgreSQL src: src/backend/commands/copy.c: CopyFrom(), CopyReadLine(), CopyReadLineText()
        public RecordState(final PushbackReader reader)
                throws OperationFailedException {
            // read until a newline ('\r' and/or '\n') or the end-of-copy marker 
            // ('\.') is detected
            try {
                int character = reader.read();
                if(character == EOS) throw new InvalidDataException("There are no more records.");
                do {
                    if(character == CR) {
                        // peek ahead for '\n' (for "\r\n")
                        character = reader.read();
                        if((character == LF) || (character == EOS)) return/*"\r\n"*/;
                        reader.unread(character)/*put the character back -- part of next record*/;
                        return/*just '\r'*/;
                    } else if(character == LF)
                        return/*just '\n'*/;
                    else /*not CR or LF -- any other character*/
                        buffer.append((char)character);
                    // NOTE:  there is a ton of logic around '\.' (end-of-copy)
                    //        that is *not* replicated here
                } while((character = reader.read()) != EOS);
            } catch(final IOException ioe) {
                throw new OperationFailedException("Could not read the next character.", ioe);
            }
        }

        /* (non-Javadoc)
         * @see IState#readField()
         */
        @Override
        public CharSequence readField() 
                throws InvalidDataException {
            final int startPosition = position;
            final int recordLength = buffer.length()/*for convenience*/;
            final StringBuilder output = new StringBuilder();

            // SEE:  PostgreSQL src: src/backend/commands/copy.c: CopyReadAttributesText()
            int endPosition = startPosition/*start at starting position*/;
            char character;
            if(position >= recordLength) throw new InvalidDataException("There are no more fields to read.");
            while(position < recordLength) {
                character = buffer.charAt(position++);
                if(character == ','/*field delimiter*/)
                    break/*end-of-field reached*/;
                else if(character == '\\'/*escape*/) {
                    //     \b   Backspace (ASCII 8)
                    //     \f  Form feed (ASCII 12)
                    //     \n  Newline (ASCII 10)
                    //     \r  Carriage return (ASCII 13)
                    //     \t  Tab (ASCII 9)
                    //     \v  Vertical tab (ASCII 11)
                    //     \digits Backslash followed by one to three octal  
                    //             digits specifies the character with that 
                    //             numeric code
                    //     \xdigits Backslash x followed by one or two hex digits  
                    //              specifies the character with that numeric code
                    // "Any other backslashed character that is not mentioned  
                    //  in the above table will be taken to represent itself."
                    // SEE:  http://www.postgresql.org/docs/9.0/static/sql-copy.html
                    if(position >= recordLength) break/*end-of-field -- escape is ignored*/;
                    character = buffer.charAt(position++);
                    switch(character) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7': {
                            // TODO:  \0 - \7 => octal conversion
                            throw new DeveloperException("Unhandled octal escape sequence");
                        }

                        case 'x': {
                            // TODO:  \xNN => hex conversion
                            throw new DeveloperException("Unhandled hex escape sequence");
                        }

                        case 'b':
                            output.append('\b');
                            break;
                        case 'f':
                            output.append('\f');
                            break;
                        case 'n':
                            output.append('\n');
                            break;
                        case 'r':
                            output.append('\r');
                            break;
                        case 't':
                            output.append('\t');
                            break;
                        case 'v':
                            output.append((char)0x0B/*vertical tab*/);
                            break;

                        // NOTE:  '\N' *within* a string is to be treated literally
                        //        (i.e. as simply 'N')

                        // any other character is to be treated as a literal
                        // NOTE:  '\.' has been removed in #readRecord()
                        default:
                            output.append(character);
                            break;
                    }
                } else/*not ',' and not '\\'*/
                    output.append(character);

                endPosition = position/*get position before next char*/;
            }

            // now that the extent of the field has been found, determine if the
            // *raw* field was null (i.e. was exactly "\N")
            // NOTE: the relies on the fact that the end position is the position
            //       before the field delimiter. ('position' is currently pointing
            //       *after* the field delimiter -- if there was one). It correctly
            //       handles the case where the "\N" is the last field in the
            //       record.
            if( (endPosition - startPosition) == 2/*length("\N")*/) {
                if( (buffer.charAt(startPosition) == '\\') && (buffer.charAt(startPosition + 1) == 'N') )
                    return null/*the literal null ("\N") was found*/;
                /* else -- some other characters made up the field */
            } /* else -- could not have contained the null string ("\N") */
            
            return output;
        }

        /**
         * Confirms that the end of the record has been reached.
         * 
         * @throws InvalidDataException if the end of record has not yet been
         *         reached.
         */
        public void confirmEndRecord()
                throws InvalidDataException {
            final int recordLength = buffer.length()/*for convenience*/;
            if(position < recordLength) throw new InvalidDataException("The end-of-record has not yet been reached.");

            // NOTE:  there is no worry about trailing space, etc as the previous
            //        call to #readField() would have consumed it
        }
    }

    /**
     * The state of a structure.
     */
    private static final class StructureState implements IState {
        private final CharSequence buffer;
        private int position = 0/*start at the beginning of the buffer for reads*/;
        private boolean consumeComma = false/*set to true after reading first field*/;

        // --------------------------------------------------------------------
        /**
         * Creates the structure state from the specified field from its parent.
         * 
         * @param field the string that is the structure field. This may be
         *        <code>null</code> or blank (both of which will result in
         *        exceptions).
         * @throws InvalidDataException if the specified field string is <code>null</code>  
         *         or blank (i.e. only contains whitespace).
         */
        // NOTE:  the caller must explicitly handle the null case
        public StructureState(final CharSequence field) 
                throws InvalidDataException {
            if(field == null) throw new InvalidDataException("A structure field cannot be null.");

            // consume any leading whitespace and look for the leading '('
            final int fieldLength = field.length()/*for convenience*/;
            while((position < fieldLength) && Character.isWhitespace(field.charAt(position)))
                position++;
            if(field.charAt(position++) != '(') throw new InvalidDataException("Malformed structure does not start with '('.");

            this.buffer = field;
        }

        /* (non-Javadoc)
         * @see IState#readField()
         */
        @Override
        public CharSequence readField() 
                throws InvalidDataException {
            // SEE:  PostgreSQL src: src/backend/utils/adt/rowtypes.c: record_in()
            final int bufferLength = buffer.length()/*for convenience*/;
            final StringBuilder output = new StringBuilder();

            // SEE:  PostgreSQL src: src/backend/commands/copy.c: CopyReadAttributesText()
            if(position >= bufferLength) throw new InvalidDataException("There are no more structure fields to read.");
            if(consumeComma) {
                final char character = buffer.charAt(position/*don't advance yet*/);
                if(character == ',')
                    position++;
                else /*not the field delimiter ','*/
                    throw new InvalidDataException("Expected end-of-field delimiter ','. Found field data.");
            } /* else -- the field delimiter (comma) does not need to be consumed */

            boolean inQuote = false/*true if in double quotes*/;
            final int startPosition = position/*start of this field*/;
            char character;
            while(position < bufferLength) {
                character = buffer.charAt(position/*peek ahead but don't advance before check*/);
                if(!inQuote && ((character == ',') || (character == ')')))
                    break/*end-of-field delimiter found*/;
                /* else -- either in quotes or not the end-of-field delimiter */
                position++/*advance*/;

                if(character == '\\') {
                    if(position >= bufferLength) throw new InvalidDataException("Invalid escape sequence at unexpected end of field.");
                    output.append(buffer.charAt(position++));
                } else if(character == '\"') {
                    if(!inQuote)
                        inQuote = true/*by definition*/;
                    else if((position < bufferLength) && (buffer.charAt(position) == '\"')) {
                        // escaped double quote (double double-quote)
                        output.append(buffer.charAt(position++));
                    } else /*end-of-record or next char was not double quote*/
                        inQuote = false/*by definition*/;
                } else /*not end-of-field, backslash or double quote*/
                    output.append(character);
            }

            // CHECK: should there be an error thrown if 'inQuote' is true at
            //        this point? (implying that there is an unterminated string)

            // flag that a field has been read
            consumeComma = true/*by definition*/;

            // if exited from the above loop after peeking at the first char
            // then the field must have been empty which translates to 'null'
            // NOTE: the check above the loop ensures that 'position' is within
            //       the buffer
            if(position == startPosition) return null/*null field*/;

            return output;
        }

        /**
         * Confirms that the end of the structure has been reached.
         * 
         * @throws InvalidDataException if the end of structure has not yet
         *         been reached or doesn't exist.
         */
        public void confirmEndStructure()
                throws InvalidDataException {
            // there should be more characters and the next character must be
            // the end-of-structure character
            // SEE:  PostgreSQL src: src/backend/utils/adt/rowtypes.c: record_in()
            final int bufferLength = buffer.length()/*for convenience*/;
            if(position >= bufferLength) throw new InvalidDataException("End of structure reached without finding the ')' delimiter.");
            final char character = buffer.charAt(position++);
            if(character != ')') throw new InvalidDataException("Expected the end-of-structure delimiter but found '" + character + "'.");

            // consume any remaining whitespace
            while((position < bufferLength) && Character.isWhitespace(buffer.charAt(position)))
                position++;
            if(position < bufferLength) throw new InvalidDataException("Non-whitespace found after end-of-structure delimiter ')'.");
        }
    }

    /**
     * The state of an array. Only a single dimensioned array is supported at
     * this time.
     */
    private interface IArrayState extends IState {
        /**
         * @return <code>true</code> if and only if there are more elements to
         *         be read in the array. <code>false</code> otherwise. 
         * @throws InvalidDataException if an unexpected character is encountered
         *         there there is an error reading.
         */
        boolean hasMoreElements()
                throws InvalidDataException;

        /**
         * Confirms that the end of the array has been reached.
         * 
         * @throws InvalidDataException if the end of array has not yet been
         *         reached or doesn't exist.
         */
        void confirmEndArray()
                throws InvalidDataException;
    }

    /**
     * The state of an array. Only a single dimensioned array is supported at
     * this time.
     */
    private static class ArrayState implements IArrayState {
        private final CharSequence buffer;
        private int position = 0/*start at the beginning of the buffer for reads*/;

        // --------------------------------------------------------------------
        /**
         * Creates the array state from the specified field from its parent.
         * 
         * @param field the string that is the array field. This may be <code>null</code>
         *        or blank (both of which will result in exceptions).
         * @throws InvalidDataException if the specified field string is <code>null</code>  
         *         or blank (i.e. only contains whitespace).
         */
        // NOTE:  the caller must explicitly handle the null case
        public ArrayState(final CharSequence field) 
                throws InvalidDataException {
            if(field == null) throw new InvalidDataException("An array field cannot be null.");

            // consume any leading whitespace and look for the leading '{'
            final int fieldLength = field.length()/*for convenience*/;
            while((position < fieldLength) && Character.isWhitespace(field.charAt(position)))
                position++;
            if(field.charAt(position++) != '{') throw new InvalidDataException("Malformed array does not start with '{'.");

            this.buffer = field;
        }

        /* (non-Javadoc)
         * @see IState#readField()
         */
        @Override
        public CharSequence readField() 
                throws InvalidDataException {
            // NOTE: this combines the logic from the two following sources
            //       (most of the parsing logic is in the former while the latter
            //       is much more lax)
            // SEE:  PostgreSQL src: src/backend/utils/adt/arrayfuncs.c: ArrayCount()
            // SEE:  PostgreSQL src: src/backend/utils/adt/arrayfuncs.c: ReadArrayStr()

            final int bufferLength = buffer.length()/*for convenience*/;
            final StringBuilder output = new StringBuilder();

            // leading whitespace is ignored on each field
            while((position < bufferLength) && Character.isWhitespace(buffer.charAt(position)))
                position++;
            if(position >= bufferLength) throw new InvalidDataException("End of array reached without finding the '}' delimiter.");

            // basic rules:
            // 1.  a field can start with either double quotes or a non-whitespace
            //     char;
            // 2.  once a field is started (via some character other than 
            //     whitespace and double quotes) then double quotes are not 
            //     allowed;
            // 3.  if a field is not surrounded by double quotes then it cannot
            //     contain:
            //     a.  whitespace (which means that any trailing whitespace; 
            //         found can be consumed and ignored)
            //     b.  a comma;
            //     c.  the array delimiters ('{' or '}');
            // 4.  anything (including whitespace) that is escaped with a 
            //     backslash is treated literally;
            // 5.  the literal "NULL" (not in double quotes) is mapped to null;

            boolean inQuote = false/*true if in double quotes*/;
            boolean startedValue = false/*true after first non-whitespace (prevents double quotes after field start)*/;
            boolean canBeNull = true/*false if '\' or '"' is encountered (which precludes from being literal "NULL")*/;
            char character;
            while(position < bufferLength) {
                character = buffer.charAt(position/*peek ahead but don't advance before check*/);
                if(!inQuote && ((character == ',') || (character == '}')))
                    break/*end-of-field delimiter found*/;
                /* else -- either in quotes or not the end-of-field delimiter */
                position++/*advance*/;

                if(character == '\\') {
                    // the character after a backslash is taken literally
                    if(position >= bufferLength) throw new InvalidDataException("Invalid escape sequence at unexpected end of array field.");
                    output.append(buffer.charAt(position++));

                    startedValue = true/*non-whitespace found*/;
                    canBeNull = false/*backslash implies cannot be null*/;
                } else if(character == '\"') {
                    if(!inQuote && startedValue) throw new InvalidDataException("Array field cannot contain embedded double quotes.");
                    inQuote = !inQuote/*swap state*/;
                    startedValue = true/*non-whitespace found*/;
                    canBeNull = false/*double quote implies cannot be null*/;
                } else { /*not end-of-field, backslash or double quote*/
                    if(inQuote)
                        output.append(character);
                    else if(Character.isWhitespace(character))/*not in double quotes and is whitespace*/
                        break/*end of non-quoted field (that has trailing whitespace)*/;
                    else {/*not in double quotes and is not whitespace*/
                        output.append(character);
                        startedValue = true/*non-whitespace found*/;
                    }
                }
            }

            // exited above on the following cases:
            // 1.  no more data in field (i.e. the position is currently at 
            //     either end-of-field delimiter (',' or '}')) -- error case
            // 2.  end-of-field delimiter (',' or '}') or first whitespace char
            //     after non-double quoted value -- consume any trailing 
            //     whitespace. If next char is ',' then consume it (leave '}'  
            //     so that #confirmEndArray() can validate) otherwise:
            //     a.  if the next char is something other than the end-of-field 
            //         delimiter -- error
            //     b.  canBeNull = true and the buffer is exactly "NULL" -- null
            //     c.  canBeNull = false and the buffer contains anything --
            //         the buffer is returned

            // trailing whitespace is ignored
            if(position >= bufferLength) throw new InvalidDataException("End of array reached without finding the '}' delimiter.");
            while((position < bufferLength) && Character.isWhitespace(buffer.charAt(position)))
                position++;

            character = buffer.charAt(position);
            if(buffer.charAt(position) == ',') position++/*consume it*/;
            else if(buffer.charAt(position) != '}') throw new InvalidDataException("Encountered '" + character + "' after end of array field.");

            if(canBeNull && (output.length() == 4/*length("NULL")*/) && "NULL".contentEquals(output))
                return null/*the literal null ("NULL") was found*/;
            /* else -- some other characters made up the field */

            return output;
        }

        /* (non-Javadoc)
         * @see IArrayState#hasMoreElements()
         */
        @Override
        public boolean hasMoreElements()
                throws InvalidDataException {
            // consume any leading whitespace
            // NOTE: it's OK to consume this here since #readField() would
            //       consume and disregard anyway
            final int bufferLength = buffer.length()/*for convenience*/;
            while((position < bufferLength) && Character.isWhitespace(buffer.charAt(position)))
                position++;
            if(position >= bufferLength) throw new InvalidDataException("End of array reached without finding the '}' delimiter.");

            // The possible next states are: '}' (no elements) or any non-whitespace 
            // char (there are more elements) excluding ',' (which is an error
            // since blank fields are not allowed in arrays)            
            final char character = buffer.charAt(position/*peek ahead*/);
            if(character == '}')
                return false/*no elements (specifically, an empty array)*/;
            else if(character == ',')
                throw new InvalidDataException("Empty elements are not allowed in an array.");
            else /*some non-whitespace char that is not '}' or ','*/
                return true/*must be an element*/;
        }

        /* (non-Javadoc)
         * @see IArrayState#confirmEndArray()
         */
        @Override
        public void confirmEndArray()
                throws InvalidDataException {
            // there should be more characters and the next character must be
            // the end-of-array character
            // SEE:  PostgreSQL src: src/backend/utils/adt/arrayfuncs.c: array_in()
            final int bufferLength = buffer.length()/*for convenience*/;
            if(position >= bufferLength) throw new InvalidDataException("End of array reached without finding the '}' delimiter.");
            final char character = buffer.charAt(position++);
            if(character != '}') throw new InvalidDataException("Expected the end-of-array delimiter but found '" + character + "'.");

            // consume any remaining whitespace
            while((position < bufferLength) && Character.isWhitespace(buffer.charAt(position)))
                position++;
            if(position < bufferLength) throw new InvalidDataException("Non-whitespace found after end-of-array delimiter '}'.");
        }
    }

    /**
     * The state of an empty or <code>null</code> array. Only a single 
     * dimensioned array is supported at this time.
     */
    // CHECK:  is there a difference between a null and empty array?
    private static final class EmptyArrayState implements IArrayState {
        /* (non-Javadoc)
         * @see IState#readField()
         */
        @Override
        public CharSequence readField()
                throws IllegalStateException {
            throw new IllegalStateException("There are no fields in an empty array.");
        }

        /* (non-Javadoc)
         * @see IArrayState#hasMoreElements()
         */
        @Override
        public boolean hasMoreElements() { return false/*by definition*/; }

        /* (non-Javadoc)
         * @see IArrayState#confirmEndArray()
         */
        @Override
        public void confirmEndArray() { /*nothing to do since always at the end*/ }
    }
}