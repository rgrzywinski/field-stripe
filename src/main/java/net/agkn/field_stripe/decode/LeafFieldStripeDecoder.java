package net.agkn.field_stripe.decode;

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

import net.agkn.common.exception.DeveloperException;
import net.agkn.common.exception.OperationFailedException;
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.record.IFieldType;
import net.agkn.field_stripe.record.PrimitiveType;
import net.agkn.field_stripe.record.writer.IRecordWriter;
import net.agkn.field_stripe.stripe.IFieldStripeReader;
import net.agkn.field_stripe.stripe.Instruction;

/**
 * A leaf element corresponding to a {@link PrimitiveType primitive-typed} 
 * {@link IField field} in a tree of {@link IFieldStripeDecoder field stripe decoders}.
 * This decoder is generic in that it handles repeated, optional and required
 * parents and fields.<p/>
 *
 * @author rgrzywinski
 */
// NOTE:  simplified versions of this decoder can be written for the cases where
//        no parent is repeated, no parent is repeated or optional or if the 
//        leaf itself is not repeated.
public class LeafFieldStripeDecoder extends AbstractDecoder
                                 implements IFieldStripeDecoder {
    private final IFieldStripeReader reader;

    // ------------------------------------------------------------------------
    // local state

    // the look-ahead instruction. If null then either there is no data or the 
    // look-ahead hasn't been read yet (both cases mean "read the next instruction").
    private Instruction lookAhead = null/*none to start*/;

    // ========================================================================
    /**
     * {@inheritDoc}
     * @param  reader this cannot be <code>null</code>
     */
    public LeafFieldStripeDecoder(final IFieldStripeReader reader, final IField field) {
        super(field, field.getPath().getDepth());
        this.reader = reader;
    }

    // ========================================================================
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.decode.IFieldStripeDecoder#getUnsetParentDepth()
     */
    @Override
    public int getUnsetParentDepth() 
            throws OperationFailedException {
        final Instruction instruction;
        if(lookAhead == null)
            instruction = lookAhead = reader.readInstruction();
        else /*there is a look-ahead instruction, simply use it*/
            instruction = lookAhead;

        if(instruction == null) return -1/*no more data*/;
        switch(instruction.kind) {
            case UNSET_PARENT:
                return instruction.parentDepth;

            default:
                // all other cases say nothing about if there's an unset parent
                return 0/*set parent*/;
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.decode.IFieldStripeDecoder#decode(net.agkn.field_stripe.record.writer.IRecordWriter)
     */
    @Override
    public ReadResult decode(final IRecordWriter recordWriter) 
            throws OperationFailedException {
        // if this is a decoder for a multi-valued field then this is used to
        // indicate if IRecordWriter#startArray() was called to know if the
        // corresponding IRecordWriter#endArray() needs to be called on exit.
        // NOTE:  the array can only be started if the parent wasn't found to
        //        be unset
        boolean arrayStarted = false/*set to true if IRecordWriter#startArray() was called*/;

        // NOTE:  only the last depth is used as they will all be of the same
        //        type by design (e.g. if one reports a repeated parent then
        //        all of them will report the exact same repeated parent) 
        int repeatedParentDepth = -1/*default to none-repeated*/;
        int unsetParentDepth = -1/*  >0: depth of unset parent
                                  * <=0: not an unset parent*/;

        do {
            final Instruction instruction = (lookAhead == null) ? reader.readInstruction() : lookAhead;
            lookAhead = null/*clear/reset always (since either just used or already null)s*/;
            if(instruction == null) return null/*no record to read*/;
            switch(instruction.kind) {
                case VALUE:
                    if(multiValue && !arrayStarted) {
                        recordWriter.startArray(field);
                        arrayStarted = true/*by definition*/;
                    } /* else -- either it's not a multi-valued field or the array has already been started */
                    writeValue(instruction, recordWriter);
                    break;

                case UNSET:
                    // NOTE:  unset values cannot exist within a repeated field.
                    //        Any unset values that exist simply indicate that
                    //        the field itself is unset. By definition, there 
                    //        can be only be one unset value if multi-valued.
                    recordWriter.writeUnsetField(field);
                    break;

                case UNSET_PARENT:
                    // NOTE:  can't exit early as the look-ahead for repeated 
                    //        values is still necessary
                    unsetParentDepth = instruction.parentDepth;
                    break;

                default:
                    throw new OperationFailedException("Unexpected field stripe decoder instruction: " + instruction);
            }
        } while((repeatedParentDepth = getRepeatedParentDepth()) == 0/*leaf is repeated*/);

        if(arrayStarted) recordWriter.endArray();

        return new ReadResult(unsetParentDepth, repeatedParentDepth);
    }

    /**
     * @return <code>&gt;0</code> the depth of the repeated parent; <code>0</code>
     *         if this leaf is repeated; <code>&lt;0</code> if there is no
     *         repetition.
     */
    private int getRepeatedParentDepth() 
            throws OperationFailedException {
        int repeatedParentDepth = -1/*default to none-repeated*/;

        // look ahead to determine if there is a repeated value
        // NOTE:  this does not return NO_DATA immediately if there is no more  
        //        data since that would imply that there was no data above 
        //        (which is not the case by definition)
        final Instruction instruction = reader.readInstruction();
        if(instruction != null) {
            switch(instruction.kind) {
                case REPEATED_VALUE:
                    repeatedParentDepth = 0/*this leaf*/;
                    break;

                case REPEATED_PARENT:
                    repeatedParentDepth = instruction.parentDepth;
                    break;

                case VALUE:
                case UNSET:
                case UNSET_PARENT:
                    // implies the next record (i.e. that there isn't a repeated
                    // value). Keep as a look-ahead.
                    lookAhead = instruction;
                    break;

                default:
                    throw new OperationFailedException("Unknown field stripe decoder instruction: " + instruction);
            }
        } /* else -- there are no more instructions */

        return repeatedParentDepth;
    }

    // ========================================================================
    /**
     * Extracts the value from the specified non-<code>null</code> {@link Instruction instruction}
     * and writes it to specified non-<code>null</code> {@link IRecordWriter}. 
     * The <var>UNSET</var> case is handled by the caller.
     */
    private void writeValue(final Instruction instruction, final IRecordWriter recordWriter)
            throws OperationFailedException {
        final IFieldType type = field.getType();
        switch((PrimitiveType)type) {
            case BYTE:
                recordWriter.writeField(field, (Byte)instruction.value);
                break;

            case SHORT:
                recordWriter.writeField(field, (Short)instruction.value);
                break;

            case INT:
                recordWriter.writeField(field, (Integer)instruction.value);
                break;

            case LONG:
                recordWriter.writeField(field, (Long)instruction.value);
                break;

            case FLOAT:
                recordWriter.writeField(field, (Float)instruction.value);
                break;

            case DOUBLE:
                recordWriter.writeField(field, (Double)instruction.value);
                break;

            case BOOLEAN:
                recordWriter.writeField(field, (Boolean)instruction.value);
                break;

            case STRING:
                recordWriter.writeField(field, (String)instruction.value);
                break;

            default:
                throw new DeveloperException("Unknown primitive field type \"" + type + "\"."); 
        }
    }
}