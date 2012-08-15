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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import net.agkn.field_stripe.exception.OperationFailedException;
import net.agkn.field_stripe.record.FieldQualifier;
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.record.Path;
import net.agkn.field_stripe.record.PrimitiveType;
import net.agkn.field_stripe.stripe.Instruction.Kind;

import com.google.protobuf.CodedOutputStream;

/**
 * A variable length (v-len) binary field-stripe writer. {@link #createFactory(File)}
 * is provided for creating the {@link IFieldStripeWriterFactory factory} for 
 * this writer.</p>
 * 
 * The structure of the file header is:<p/>
 * <pre>
 *   5 bytes: magic number ('fstrp')
 *   1 byte: version number (0 is reserved)
 *   1 byte: 0x00=required, 0x01=optional, 0x02=repeated
 *   1 byte: {@link PrimitiveType} ordinal (from TypeConstant)
 *   vlen uint32: total path length (including this field)
 *   vlen uint32: number of repeated parents (not including this field)
 *   vlen uint32: number of optional parents (not including this field)
 * </pre>
 * 
 * The {@link Instruction instruction} encoding is:<p/>
 * <ul>
 *   <li>{@link Kind#UNSET} (0): &lt;vlen uint32&gt;</li>
 *   <li>{@link Kind#VALUE} (1): &lt;vlen uint32&gt; &lt;value in field type&gt;</li>
 *   <li>{@link Kind#REPEATED_VALUE} (2): &lt;vlen uint32&gt;</li>
 *   <li>{@link Kind#REPEATED_PARENT} (3): &lt;vlen uint32 ((number-of-repeated-parents << 3) | 3bits)&gt;</li>
 *   <li>{@link Kind#UNSET_PARENT} (4): &lt;vlen uint32 ((number-of-repeated-or-optional-parents << 3) | 3bits)&gt;</li>
 * </ul>
 * 
 * All values except the magic number and version number are written using the
 * <a href="https://developers.google.com/protocol-buffers/docs/encoding">Protobuf encoding format</a>.
 *
 * @author rgrzywinski
 * @see BinaryVLenFieldStripeReader
 */
public class BinaryVLenFieldStripeWriter implements IFieldStripeWriter {
    // the magic number used to identify binary file-stripes
    public static final byte[] MAGIC = { 'f', 's', 't', 'r', 'p' };

    // the version of this writer
    public static final byte VERSION = 1/*0 is reserved*/;

    // ------------------------------------------------------------------------
    // types for the instructions
    public static final int UNSET = 0;
    public static final int VALUE = 1;
    public static final int REPEATED_VALUE = 2;
    public static final int REPEATED_PARENT = 3;
    public static final int UNSET_PARENT = 4;

    // constants for types
    public static enum TypeConstant { BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BOOLEAN, STRING };
    @SuppressWarnings("serial")
    public static final Map<PrimitiveType, TypeConstant> primitiveTypeToConstantMap = new HashMap<PrimitiveType, TypeConstant>() {{
        put(PrimitiveType.BYTE, TypeConstant.BYTE);
        put(PrimitiveType.SHORT, TypeConstant.SHORT);
        put(PrimitiveType.INT, TypeConstant.INT);
        put(PrimitiveType.LONG, TypeConstant.LONG);
        put(PrimitiveType.FLOAT, TypeConstant.FLOAT);
        put(PrimitiveType.DOUBLE, TypeConstant.DOUBLE);
        put(PrimitiveType.BOOLEAN, TypeConstant.BOOLEAN);
        put(PrimitiveType.STRING, TypeConstant.STRING);
    }};

    // ------------------------------------------------------------------------
    // the name of the file extension
    public static final String EXTENSION = ".fstrp";

    // ************************************************************************
    private final CodedOutputStream protobufOutput;
    private final OutputStream outputStream/*the underlying stream*/;
    private boolean closed = false/*not closed until #close()*/;

    // ........................................................................
    private final IField field; 

    // offsets to convert from parent depth to smaller depth:
    //  unset: number of optional or repeated parents (offset = required)
    //  repeated: number of repeated parents (offset = required or optional)
    private final int unsetOffset/*number of parents that are required*/;
    private final int repeatedOffset/*number of parents that are required or optional*/;

    // identifies if the associated field is not required or has a repeated or
    // optional parent. If false, no meta-data is added before a value is written.
    private final boolean notRequiredOnly;

    // ========================================================================
    /**
     * @param  fsPath the base path to where the field-stripes are to be created.
     *         The path must exist. A directory is created for each node field 
     *         and a file is created for each leaf field. This cannot be <code>null</code>
     * @return a {@link IFieldStripeWriterFactory} that creates binary field-stripe
     *         files in the specified path. This will never be <code>null</code>.
     * @throws OperationFailedException if the specified path does not exist 
     *         or is a file.
     */
    public static IFieldStripeWriterFactory createFactory(final File fsPath) 
            throws OperationFailedException {
        // ensure that the path exists and is a directory
        if(!fsPath.isDirectory()) throw new OperationFailedException("The path does not exist or is file: " + fsPath);

        return new IFieldStripeWriterFactory() {
            private final Map<IField, IFieldStripeWriter> fieldToWriterMap = new HashMap<IField, IFieldStripeWriter>();
            @Override
            public IFieldStripeWriter createFieldStripeWriter(final IField field)
                    throws OperationFailedException {
                if(fieldToWriterMap.containsKey(field)) throw new OperationFailedException("A writer already exists for field " + field + ".");

                // walk the parent path and create the necessary directories
                final Path parentFieldPath = field.getPath().getParentPath();
                File fieldStripeFSPath = fsPath;
                for(final IField pathField : parentFieldPath) {
                    fieldStripeFSPath = new File(fieldStripeFSPath, pathField.getName());
                    if(!fieldStripeFSPath.exists())
                        fieldStripeFSPath.mkdir();
                    /* else -- the path exists */
                    if(!fieldStripeFSPath.isDirectory()) throw new OperationFailedException("The path does not exist or is file: " + fsPath);
                }
                fieldStripeFSPath = new File(fieldStripeFSPath, (field.getName() + EXTENSION));

                try {
                    final IFieldStripeWriter writer = new BinaryVLenFieldStripeWriter(new FileOutputStream(fieldStripeFSPath), field);
                    fieldToWriterMap.put(field, writer);
                    return writer;
                } catch(final IOException ioe) {
                    throw new OperationFailedException(ioe);
                }
            }

            @Override
            public void closeAllWriters() throws OperationFailedException {
                for(final IFieldStripeWriter writer : fieldToWriterMap.values()) 
                    writer.close();
            }
        };
    }

    // ========================================================================
    /**
     * The file-stripe header is written on construction.
     * 
     * @param  writer the {@link OutputStream} to which the field-stripe is written. 
     *         This cannot be <code>null</code>. The data is only guaranteed to 
     *         be completely written after {@link #close()} is called.
     * @param  field the {@link IField} for which this is a writer. This cannot
     *         be <code>null</code> and its {@link IField#getType() type} must
     *         be {@link PrimitiveType}.
     * @throws OperationFailedException if the file-stripe header could not be
     *         written for any reason. Subclasses may contain additional  
     *         information as to the nature of the failure. 
     */
    // NOTE:  constructed from the factory (and package for testing)
    /*package*/ BinaryVLenFieldStripeWriter(final OutputStream outputStream, final IField field) 
            throws OperationFailedException {
        this.outputStream = outputStream;
        // CHECK:  is the default buffer size (4096) a good choice? 
        this.protobufOutput = CodedOutputStream.newInstance(outputStream);
        this.field = field;
        final Path fieldPath = field.getPath()/*for convenience*/;
        this.unsetOffset = fieldPath.getParentQualifierCount(FieldQualifier.ONE/*required*/);
        this.repeatedOffset = fieldPath.getParentQualifierCount(FieldQualifier.ONE/*required*/) +
                              fieldPath.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE/*optional*/);

        this.notRequiredOnly = !((field.getQualifier() == FieldQualifier.ONE) && (unsetOffset == fieldPath.getParentPath().getDepth())/*only 'required' in parent path*/);

        try {
            writeHeader();
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    /**
     * Writes the file-stripe header to the stream.
     */
    private void writeHeader() 
            throws IOException {
        protobufOutput.writeRawBytes(MAGIC);
        protobufOutput.writeRawByte(VERSION);

        final Path fieldPath = field.getPath()/*for convenience*/;
        protobufOutput.writeRawByte(field.getQualifier().ordinal());
        protobufOutput.writeRawByte(primitiveTypeToConstantMap.get(field.getType()).ordinal());
        protobufOutput.writeInt32NoTag(fieldPath.getDepth());
        protobufOutput.writeInt32NoTag(fieldPath.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE/*repeated*/));
        protobufOutput.writeInt32NoTag(fieldPath.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE/*optional*/));
    }

    // ========================================================================
    // life-cycle

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#close()
     */
    @Override
    public void close() 
            throws OperationFailedException {
        if(closed) return;
        try {
            protobufOutput.flush()/*write remainder to output stream*/;
            outputStream.close();
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        } finally {
            this.closed = true/*by definition*/;
        }
    }

    // ========================================================================
    // meta-data

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeUnset()
     */
    @Override
    public void writeUnset() 
            throws IllegalStateException, OperationFailedException {
        if(closed) throw new IllegalStateException("The binary field stripe writer has already been closed.")/*by contract*/;
        try {
            protobufOutput.writeTag(0/*not used*/, UNSET);
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeUnsetParent(int)
     */
    @Override
    public void writeUnsetParent(final int fieldDepth) 
            throws IllegalStateException, OperationFailedException {
        if(closed) throw new IllegalStateException("The binary field stripe writer has already been closed.")/*by contract*/;
        try {
            protobufOutput.writeTag((fieldDepth - unsetOffset), UNSET_PARENT);
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeRepeated()
     */
    @Override
    public void writeRepeated() 
            throws IllegalStateException, OperationFailedException {
        if(closed) throw new IllegalStateException("The binary field stripe writer has already been closed.")/*by contract*/;
        try {
            protobufOutput.writeTag(0/*not used*/, REPEATED_VALUE);
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeRepeatedParent(int)
     */
    @Override
    public void writeRepeatedParent(final int fieldDepth)
            throws IllegalStateException, OperationFailedException {
        if(closed) throw new IllegalStateException("The binary field stripe writer has already been closed.")/*by contract*/;
        try {
            protobufOutput.writeTag((fieldDepth - repeatedOffset), REPEATED_PARENT);
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    // ========================================================================
    // primitive values 

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeValue(byte)
     */
    @Override
    public void writeValue(final byte value) 
            throws IllegalStateException, OperationFailedException {
        writeValue((int)value)/*cast to int since using var-len*/;
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeValue(short)
     */
    @Override
    public void writeValue(final short value) 
            throws IllegalStateException, OperationFailedException {
        writeValue((int)value)/*cast to int since using var-len*/;
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeValue(int)
     */
    @Override
    public void writeValue(final int value)
            throws IllegalStateException, OperationFailedException {
        if(closed) throw new IllegalStateException("The binary field stripe writer has already been closed.")/*by contract*/;
        try {
            if(notRequiredOnly) protobufOutput.writeTag(0/*not used*/, VALUE);
            protobufOutput.writeSInt32NoTag(value);
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeValue(long)
     */
    @Override
    public void writeValue(final long value)
            throws IllegalStateException, OperationFailedException {
        if(closed) throw new IllegalStateException("The binary field stripe writer has already been closed.")/*by contract*/;
        try {
            if(notRequiredOnly) protobufOutput.writeTag(0/*not used*/, VALUE);
            protobufOutput.writeSInt64NoTag(value);
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeValue(float)
     */
    @Override
    public void writeValue(final float value)
            throws IllegalStateException, OperationFailedException {
        if(closed) throw new IllegalStateException("The binary field stripe writer has already been closed.")/*by contract*/;
        try {
            if(notRequiredOnly) protobufOutput.writeTag(0/*not used*/, VALUE);
            protobufOutput.writeFloatNoTag(value);
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeValue(double)
     */
    @Override
    public void writeValue(final double value)
            throws IllegalStateException, OperationFailedException {
        if(closed) throw new IllegalStateException("The binary field stripe writer has already been closed.")/*by contract*/;
        try {
            if(notRequiredOnly) protobufOutput.writeTag(0/*not used*/, VALUE);
            protobufOutput.writeDoubleNoTag(value);
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeValue(boolean)
     */
    @Override
    public void writeValue(final boolean value) 
            throws IllegalStateException, OperationFailedException {
        if(closed) throw new IllegalStateException("The binary field stripe writer has already been closed.")/*by contract*/;
        try {
            if(notRequiredOnly) protobufOutput.writeTag(0/*not used*/, VALUE);
            protobufOutput.writeBoolNoTag(value);
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeValue(java.lang.String)
     */
    @Override
    public void writeValue(final String value)
            throws IllegalStateException, OperationFailedException {
        if(closed) throw new IllegalStateException("The binary field stripe writer has already been closed.")/*by contract*/;
        try {
            if(notRequiredOnly) protobufOutput.writeTag(0/*not used*/, VALUE);
            protobufOutput.writeStringNoTag(value);
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }
}