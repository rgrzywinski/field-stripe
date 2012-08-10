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

import static net.agkn.field_stripe.stripe.BinaryVLenFieldStripeWriter.EXTENSION;
import static net.agkn.field_stripe.stripe.BinaryVLenFieldStripeWriter.MAGIC;
import static net.agkn.field_stripe.stripe.BinaryVLenFieldStripeWriter.REPEATED_PARENT;
import static net.agkn.field_stripe.stripe.BinaryVLenFieldStripeWriter.REPEATED_VALUE;
import static net.agkn.field_stripe.stripe.BinaryVLenFieldStripeWriter.UNSET;
import static net.agkn.field_stripe.stripe.BinaryVLenFieldStripeWriter.UNSET_PARENT;
import static net.agkn.field_stripe.stripe.BinaryVLenFieldStripeWriter.VALUE;
import static net.agkn.field_stripe.stripe.BinaryVLenFieldStripeWriter.VERSION;
import static net.agkn.field_stripe.stripe.BinaryVLenFieldStripeWriter.primitiveTypeToConstantMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.agkn.common.exception.DeveloperException;
import net.agkn.common.exception.InvalidDataException;
import net.agkn.common.exception.OperationFailedException;
import net.agkn.field_stripe.record.FieldQualifier;
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.record.Path;
import net.agkn.field_stripe.record.PrimitiveType;
import net.agkn.field_stripe.stripe.Instruction.Kind;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;

/**
 * A variable length (v-len) binary field-stripe reader. Refer to {@link BinaryVLenFieldStripeWriter}
 * for information on the file format.
 *
 * @author rgrzywinski
 * @see BinaryVLenFieldStripeWriter
 */
public class BinaryVLenFieldStripeReader implements IFieldStripeReader {
    // TODO:  more intelligently use CodedInputStream's size limits to ensure
    //        that no record / instruction blows out the size limit

    private final CodedInputStream protobufInput;

    // ........................................................................
    // the field for which this is a reader and the primitive type of the field
    private final IField field;
    private final PrimitiveType fieldType;

    // offsets to convert from parent depth to smaller depth:
    //  unset: number of optional or repeated parents (offset = required)
    //  repeated: number of repeated parents (offset = required or optional)
    private final int unsetOffset/*number of parents that are required*/;
    private final int repeatedOffset/*number of parents that are required or optional*/;

    // identifies if the associated field is required and had no repeated or
    // optional parents. If true, then the stream consists of only values (with
    // no meta-data).
    private final boolean requiredOnly;

    // ========================================================================
    /**
     * @param  fsPath the base path to where the field-stripes are. The path 
     *         must exist. This cannot be <code>null</code>
     * @return a {@link IFieldStripeReaderFactory} that reads binary field-stripe
     *         files in the specified path. This will never be <code>null</code>.
     * @throws OperationFailedException if the specified path does not exist 
     *         or is a file.
     */
    public static IFieldStripeReaderFactory createFactory(final File fsPath) 
            throws OperationFailedException {
        // ensure that the path exists and is a directory
        if(!fsPath.isDirectory()) throw new OperationFailedException("The path does not exist or is file: " + fsPath);

        return new IFieldStripeReaderFactory() {
            private final Map<IField, IFieldStripeReader> fieldToReaderMap = new HashMap<IField, IFieldStripeReader>();
            @Override
            public IFieldStripeReader createFieldStripeReader(final IField field)
                    throws OperationFailedException {
                if(fieldToReaderMap.containsKey(field)) throw new OperationFailedException("A reader already exists for field " + field + ".");

                // walk the parent path to get to the field stripe
                final Path parentFieldPath = field.getPath().getParentPath();
                File fieldStripeFSPath = fsPath;
                for(final IField pathField : parentFieldPath) {
                    fieldStripeFSPath = new File(fieldStripeFSPath, pathField.getName());
                    if(!fieldStripeFSPath.exists())
                        fieldStripeFSPath.mkdir();
                    /* else -- the path exists */
                    if(!fieldStripeFSPath.isDirectory()) throw new OperationFailedException("The field stripe path does not exist or is file: " + fsPath);
                }
                fieldStripeFSPath = new File(fieldStripeFSPath, (field.getName() + EXTENSION));
                if(!fieldStripeFSPath.exists()) throw new OperationFailedException("The field stripe does not exist: " + fsPath);
                if(fieldStripeFSPath.isDirectory()) throw new OperationFailedException("The field stripe is not a file: " + fsPath);

                try {
                    final IFieldStripeReader reader = new BinaryVLenFieldStripeReader(new FileInputStream(fieldStripeFSPath), field);
                    fieldToReaderMap.put(field, reader);
                    return reader;
                } catch(final IOException ioe) {
                    throw new OperationFailedException(ioe);
                }
            }
        };
    }

    // ========================================================================
    /**
     * The header is read and parsed and compared with the specified field.
     * 
     * @param  inputStream the {@link java.io.InputStream} from the binary data
     *         is read. This cannot be <code>null</code>.
     * @param  field the {@link IField} for which this reader is reading. This
     *         cannot be <code>null</code> and its type must be a {@link PrimitiveType}.
     * @throws OperationFailedException if the field-stripe header cannot be
     *         read or its contents does not match that of the specified field.
     *         Subclasses may provide more information as to the nature of the 
     *         failure.
     */
    // NOTE:  constructed from the factory (and package for testing)
    /*package*/ BinaryVLenFieldStripeReader(final InputStream inputStream, final IField field) 
            throws OperationFailedException {
        this.protobufInput = CodedInputStream.newInstance(inputStream);

        this.field = field;
        this.fieldType = (PrimitiveType)field.getType();
        final Path fieldPath = field.getPath()/*for convenience*/;
        this.unsetOffset = fieldPath.getParentQualifierCount(FieldQualifier.ONE/*required*/);
        this.repeatedOffset = fieldPath.getParentQualifierCount(FieldQualifier.ONE/*required*/) +
                              fieldPath.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE/*optional*/);

        this.requiredOnly = ((field.getQualifier() == FieldQualifier.ONE) && (unsetOffset == fieldPath.getParentPath().getDepth())/*only 'required' in parent path*/);

        // TODO:  set the CodedInputStream's size limit so that at most the
        //        header's data will fit (which guarantees that the instructions
        //        will fit)

        readValidateHeader();
    }

    /**
     * Reads and validates that the header matches the {@link IField field}
     * associated with this reader.
     * 
     * @throws OperationFailedException if the field-stripe header cannot be
     *         read or its contents does not match that of the specified field.
     *         Subclasses may provide more information as to the nature of the 
     *         failure.
     * @see BinaryVLenFieldStripeWriter
     */
    private void readValidateHeader() 
            throws OperationFailedException {
        try {
            final byte[] magicNumber = protobufInput.readRawBytes(MAGIC.length/*length*/);
            if(!Arrays.equals(magicNumber, MAGIC)) throw new InvalidDataException("Unexpected file-stripe header: " + magicNumber[0] + " " + magicNumber[1] + " " + magicNumber[2] + " " + magicNumber[3] + " " + magicNumber[4]);
            final int version = protobufInput.readRawByte();
            if(version != VERSION) throw new InvalidDataException("Unknown version number (" + version + " != " + VERSION + ").");

            final Path fieldPath = field.getPath()/*for convenience*/;
            final int qualifierOrdinal = protobufInput.readRawByte();
            if(qualifierOrdinal != field.getQualifier().ordinal()) throw new InvalidDataException("Unexpected field qualifier ordinal (" + qualifierOrdinal + " != " + field.getQualifier().ordinal() + ").");
            final int primitiveTypeConstant = protobufInput.readRawByte();
            if(primitiveTypeConstant != primitiveTypeToConstantMap.get(field.getType()).ordinal()) throw new InvalidDataException();
            final int depth = protobufInput.readInt32();
            if(depth != fieldPath.getDepth()) throw new InvalidDataException("Unexpected path depth (" + depth + " != " + fieldPath.getDepth() + ").");
            final int repeatedParentCount = protobufInput.readInt32();
            if(repeatedParentCount != fieldPath.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE/*repeated*/)) throw new InvalidDataException("Unexpected repeated parent count (" + repeatedParentCount + ").");
            final int repeatedOptionalParentCount = protobufInput.readInt32();
            if(repeatedOptionalParentCount != fieldPath.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE/*optional*/)) throw new InvalidDataException("Unexpected repeated-and-optional parent count (" + repeatedOptionalParentCount + ").");
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    // ========================================================================
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeReader#readInstruction()
     */
    @Override
    public Instruction readInstruction() 
            throws OperationFailedException {
        protobufInput.resetSizeCounter()/*reset for each instruction (see TODO at top)*/;
        try {
            if(protobufInput.isAtEnd()) return null/*end-of-stream (by contract)*/;

            // two modes: either requiredOnly is true and all entries are simply
            // values (with no meta-data) or it is false and a read is needed 
            // to determine the instruction type
            if(requiredOnly) /*no meta-data*/
                return new Instruction(Kind.VALUE/*by definition*/, readValue());
            /* else -- there is meta-data from which the kind of instruction is derived */
            final int tag = protobufInput.readRawVarint32()/*cannot use readTag() as Protobuf limits the tag such that it cannot be zero*/;
            final int instructionKind = /*WireFormat.getTagWireType()*/tag & 0x07;
            final int instructionValue = WireFormat.getTagFieldNumber(tag);
            switch(instructionKind) {
                case UNSET:
                    return Instruction.UNSET;
                case VALUE:
                    return new Instruction(Kind.VALUE/*by definition*/, readValue());
                case REPEATED_VALUE:
                    return Instruction.REPEATED_VALUE;
                case REPEATED_PARENT:
                    return new Instruction(Kind.REPEATED_PARENT, (instructionValue + repeatedOffset));
                case UNSET_PARENT:
                    return new Instruction(Kind.UNSET_PARENT, (instructionValue + unsetOffset));

                default:
                    throw new OperationFailedException("Unknown instruction kind " + instructionKind + " for field " + field.getName() + "."); 
            }
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    /**
     * Reads and returns the next value based on the {@link IField field's} 
     * {@link IField#getType() type}.
     */
    private Object readValue() 
            throws IOException {
        switch(fieldType) {
            case BYTE:
                return (byte)protobufInput.readSInt32()/*encoded as an v-sint32*/;
            case SHORT:
                return (short)protobufInput.readSInt32()/*encoded as an v-sint32*/;
            case INT:
                return protobufInput.readSInt32();
            case LONG:
                return protobufInput.readSInt64();
            case FLOAT:
                return protobufInput.readFloat();
            case DOUBLE:
                return protobufInput.readDouble();
            case BOOLEAN:
                return protobufInput.readBool();
            case STRING:
                return protobufInput.readString();

            default:
                throw new DeveloperException("Unknown field type in field \"" + field.getName() + ".");
        }
    }
}