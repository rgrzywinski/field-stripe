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

import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Stack;

import net.agkn.field_stripe.exception.InvalidDataException;
import net.agkn.field_stripe.exception.OperationFailedException;
import net.agkn.field_stripe.record.IField;
import net.jcip.annotations.NotThreadSafe;
import net.minidev.json.JSONStyle;

/**
 * A {@link IRecordWriter writer} for records that are in JSON-array format.
 * Writing is done as pure strings for simplicity. This precludes any pretty-
 * printing or other such features at this time.<p/>
 * 
 * Note that this parser is <i>not</i> thread-safe. Also note that no exceptions
 * are logged.
 *
 * @author rgrzywinski
 */
@NotThreadSafe
public class JsonArrayRecordWriter implements IRecordWriter {
    // the string appended by #indent() for each level of nesting
    private static final String INDENT = "    ";
    private static final String NEWLINE = "\n";

    // ************************************************************************
    private final PrintWriter writer;
    private boolean isClosed = false/*by default not closed*/;

    private final JSONStyle jsonStringEncoder = JSONStyle.NO_COMPRESS/*for RFC 4627*/;

    private final boolean prettyPrint;

    // ------------------------------------------------------------------------
    // a stack of what is currently being nested (record, array or structure)
    private final Stack<State> nestingStack = new Stack<State>();

    // ========================================================================
    /**
     * @param  writer the <code>Writer</code> to which the records are written.
     *         This cannot be <code>null</code>.
     * @param  prettyPrint <code>true</code> if and only if pretty-printing is
     *         to be used (e.g. newlines and indenting). <code>false</code>
     *         otherwise.
     */
    public JsonArrayRecordWriter(final Writer writer, final boolean prettyPrint) {
        this.writer = new PrintWriter(new BufferedWriter(writer));
        this.prettyPrint = prettyPrint;

        // start the document state
        nestingStack.push(new DocumentState());
    }
    
    // ========================================================================
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#startRecord()
     */
    @Override
    public void startRecord() 
            throws IllegalStateException, InvalidDataException {
        final State previousState;
        if(isClosed) throw new IllegalStateException("The record writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty() || !(nestingStack.peek() instanceof DocumentState)) throw new IllegalStateException("A record has not been started or is nested in the record writer.")/*by contract*/;
        previousState = nestingStack.peek();

        // NOTE:  for JSON array format, records are newline delimited 
        if(previousState.appendComma) writer.print('\n');
        previousState.appendComma = true/*by definition*/;

        if(prettyPrint) indent(true/*prepend newline*/);

        nestingStack.push(new RecordState())/*by definition*/;

        writer.print('[');
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#endRecord()
     */
    @Override
    public void endRecord() 
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty() || !(nestingStack.peek() instanceof RecordState)) throw new IllegalStateException("A record has not been started or is nested in the record writer.")/*by contract*/;
        nestingStack.pop()/*by definition*/;

        if(prettyPrint) indent(true/*prepend newline*/);
        writer.print(']');
    }

    // ------------------------------------------------------------------------
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#startStructure(net.agkn.field_stripe.record.IField)
     */
    @Override
    public void startStructure(final IField field) 
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the record writer.")/*by contract*/;
        final State state = nestingStack.peek();

        if(state.appendComma) writer.print(',');
        state.appendComma = true/*by definition*/;

        if(prettyPrint) indent(true/*prepend newline*/);
        writer.append('[');

        nestingStack.push(new StructureState())/*by definition*/;
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#endStructure()
     */
    @Override
    public void endStructure() 
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty() || !(nestingStack.peek() instanceof StructureState)) throw new IllegalStateException("A record has not been started or nested in the record writer.")/*by contract*/;
        nestingStack.pop()/*by definition*/;

        if(prettyPrint) indent(true/*prepend newline*/);
        writer.print(']');
    }

    // ------------------------------------------------------------------------
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#startArray(net.agkn.field_stripe.record.IField)
     */
    @Override
    public void startArray(final IField field) 
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the record writer.")/*by contract*/;
        final State state = nestingStack.peek();

        if(state.appendComma) writer.print(',');
        state.appendComma = true/*by definition*/;

        if(prettyPrint) indent(true/*prepend newline*/);
        writer.append('[');

        nestingStack.push(new ArrayState())/*by definition*/;
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#endArray()
     */
    @Override
    public void endArray() 
            throws IllegalStateException, InvalidDataException {
        if(isClosed) throw new IllegalStateException("The record writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty() || !(nestingStack.peek() instanceof ArrayState)) throw new IllegalStateException("A record or array has not been started in the record writer.")/*by contract*/;
        nestingStack.pop()/*by definition*/;

        if(prettyPrint) indent(true/*prepend newline*/);
        writer.print(']');
    }

    // ------------------------------------------------------------------------
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#writeUnsetField(net.agkn.field_stripe.record.IField)
     */
    @Override
    public void writeUnsetField(final IField field) 
            throws IllegalStateException, OperationFailedException {
        if(isClosed) throw new IllegalStateException("The record writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the record writer.")/*by contract*/;
        final State state = nestingStack.peek();

        if(state.appendComma) writer.print(',');
        state.appendComma = true/*by definition*/;

        if(prettyPrint) indent(true/*prepend newline*/);
        // NOTE:  it was decided that AK would represent unset multi-valued
        //        (aka array) fields as "[]" rather than "null"
        if(field.getQualifier().isMultiValue())
            writer.print("[]"/*unset array is literal "[]"*/);
        else /*not a multi-valued field*/
            writer.print("null"/*unset non-array is a literal 'null'*/);
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#writeField(net.agkn.field_stripe.record.IField, byte)
     */
    @Override
    public void writeField(final IField field, final byte value)
            throws IllegalStateException, OperationFailedException {
        if(isClosed) throw new IllegalStateException("The record writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the record writer.")/*by contract*/;
        final State state = nestingStack.peek();

        if(state.appendComma) writer.print(',');
        state.appendComma = true/*by definition*/;

        if(prettyPrint) indent(true/*prepend newline*/);
        writer.print(value);
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#writeField(net.agkn.field_stripe.record.IField, short)
     */
    @Override
    public void writeField(final IField field, final short value)
            throws IllegalStateException, OperationFailedException {
        if(isClosed) throw new IllegalStateException("The record writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the record writer.")/*by contract*/;
        final State state = nestingStack.peek();

        if(state.appendComma) writer.print(',');
        state.appendComma = true/*by definition*/;

        if(prettyPrint) indent(true/*prepend newline*/);
        writer.print(value);
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#writeField(net.agkn.field_stripe.record.IField, int)
     */
    @Override
    public void writeField(final IField field, final int value)
            throws IllegalStateException, OperationFailedException {
        if(isClosed) throw new IllegalStateException("The record writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the record writer.")/*by contract*/;
        final State state = nestingStack.peek();

        if(state.appendComma) writer.print(',');
        state.appendComma = true/*by definition*/;

        if(prettyPrint) indent(true/*prepend newline*/);
        writer.print(value);
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#writeField(net.agkn.field_stripe.record.IField, long)
     */
    @Override
    public void writeField(final IField field, final long value)
            throws IllegalStateException, OperationFailedException {
        if(isClosed) throw new IllegalStateException("The record writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the record writer.")/*by contract*/;
        final State state = nestingStack.peek();

        if(state.appendComma) writer.print(',');
        state.appendComma = true/*by definition*/;

        if(prettyPrint) indent(true/*prepend newline*/);
        writer.print(value);
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#writeField(net.agkn.field_stripe.record.IField, float)
     */
    @Override
    public void writeField(final IField field, final float value)
            throws IllegalStateException, OperationFailedException {
        if(isClosed) throw new IllegalStateException("The record writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the record writer.")/*by contract*/;
        final State state = nestingStack.peek();

        if(state.appendComma) writer.print(',');
        state.appendComma = true/*by definition*/;

        if(prettyPrint) indent(true/*prepend newline*/);
        writer.print(value);
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#writeField(net.agkn.field_stripe.record.IField, double)
     */
    @Override
    public void writeField(final IField field, final double value)
            throws IllegalStateException, OperationFailedException {
        if(isClosed) throw new IllegalStateException("The record writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the record writer.")/*by contract*/;
        final State state = nestingStack.peek();

        if(state.appendComma) writer.print(',');
        state.appendComma = true/*by definition*/;

        if(prettyPrint) indent(true/*prepend newline*/);
        writer.print(value);
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#writeField(net.agkn.field_stripe.record.IField, boolean)
     */
    @Override
    public void writeField(final IField field, final boolean value)
            throws IllegalStateException, OperationFailedException {
        if(isClosed) throw new IllegalStateException("The record writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the record writer.")/*by contract*/;
        final State state = nestingStack.peek();

        if(state.appendComma) writer.print(',');
        state.appendComma = true/*by definition*/;

        if(prettyPrint) indent(true/*prepend newline*/);
        writer.print(value);
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.writer.IRecordWriter#writeField(net.agkn.field_stripe.record.IField, java.lang.String)
     */
    @Override
    public void writeField(final IField field, final String value)
            throws IllegalStateException, OperationFailedException {
        if(isClosed) throw new IllegalStateException("The record writer has already been closed.")/*by contract*/;
        if(nestingStack.isEmpty()) throw new IllegalStateException("A record has not been started in the record writer.")/*by contract*/;
        final State state = nestingStack.peek();

        if(state.appendComma) writer.print(',');
        state.appendComma = true/*by definition*/;

        if(prettyPrint) indent(true/*prepend newline*/);
        writer.append('\"');
            jsonStringEncoder.escape(value, writer);
        writer.append('\"');
    }

    // ========================================================================
    /**
     * Intents (writes spaces to the {@link java.io.Writer writer}) based on 
     * the current nesting stack.
     * 
     * @param  prependNewline <code>true</code> if a newline should be appended
     *         before indenting. <code>false</code> if no newline is desired.
     */
    private void indent(final boolean prependNewline) {
        if(prependNewline) writer.append(NEWLINE);

        final int depth = nestingStack.size();
        for(int i=0; i<depth; i++)
            writer.append(INDENT);
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

        if(nestingStack.isEmpty() || !(nestingStack.peek() instanceof DocumentState)) throw new IllegalStateException("A record, structure or array has been impropertly nested in the record writer.")/*by contract*/;
        nestingStack.pop()/*by definition*/;

        writer.close();

// CHECK: what's the best way to log?
//        if(writer.checkError()) log.warn("An error occurred when the output writer was closed.");
    }

    // ************************************************************************
    /**
     * Marker interface.
     */
    // NOTE:  see NOTE below on implementation
    private static abstract class State {
        /*protected*/ boolean appendComma = false/*set to true after first record*/;
    };

    // NOTE: specific subclasses are created solely so that the ISnapshotSummaryWriter
    //       methods can be sure that the state is the expected one. The same
    //       effect could be achieved by using a single State class that has
    //       a "type". The current approach was chosen as it closely mirrors
    //       that of TextSnapshotSummaryWriter and a 'diff' could be used to
    //       ensure that the two implementations stay in sync.

    /**
     * The state of a document.
     */
    private static final class DocumentState extends State { /*nothing to add*/ }

    /**
     * The state of a record.
     */
    private static final class RecordState extends State { /*nothing to add*/ }

    /**
     * The state of a structure.
     */
    private static final class StructureState extends State { /*nothing to add*/ }
 
    /**
     * The state of an array. Only a single dimensioned array is supported at
     * this time.
     */
    private static final class ArrayState extends State { /*nothing to add*/ }
}