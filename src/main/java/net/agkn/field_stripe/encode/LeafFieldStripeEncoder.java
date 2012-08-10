package net.agkn.field_stripe.encode;

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

import org.apache.commons.lang.mutable.MutableBoolean;

import net.agkn.common.exception.DeveloperException;
import net.agkn.common.exception.OperationFailedException;
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.record.IFieldType;
import net.agkn.field_stripe.record.PrimitiveType;
import net.agkn.field_stripe.record.reader.IRecordReader;
import net.agkn.field_stripe.stripe.IFieldStripeWriter;
import net.jcip.annotations.NotThreadSafe;

/**
 * A field-stripe encoder for a leaf field -- a field whose {@link IField#getType() type}
 * is {@link PrimitiveType}.<p/>
 * 
 * The following states are emitted:
 * <ul>
 *   <li><var>value</var>: valid for any non-null primitive field.</li>
 *   <li><var>UNSET</var>: valid for any unset primitive field (that is either
 *       optional or repeated).</li>
 *   <li><var>repeated-value</var>: only valid if the primitive field is 
 *       zero-or-more and precedes each <i>repeated</i> value.</li>
 *   <li><var>parent-is-UNSET #</var>: valid for any primitive field (that has
 *       parents that are either zero-or-one or zero-or-more) and indicates the
 *       depth of the <var>UNSET</var> parent.</li>
 *   <li><var>repeated-parent #</var>: valid for any primitive field (that has
 *       parent a parent that is zero-or-more) and indicates the depth of the 
 *       last repeated parent.</li>
 * </ul>
 *
 * @author rgrzywinski
 */
@NotThreadSafe
public class LeafFieldStripeEncoder extends AbstractEncoder 
                                 implements IFieldStripeEncoder {
    // the "is set" parameter passed to IRecordReader#readXYZField()
    private static final MutableBoolean IS_SET = new MutableBoolean();

    // ************************************************************************
    private final IFieldStripeWriter writer;

    // ========================================================================
    /**
     * {@inheritDoc}
     */
    public LeafFieldStripeEncoder(final IFieldStripeWriter writer, final IField field) {
        super(field, field.getPath().getDepth());
        this.writer = writer;
    }

    // ========================================================================
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.encode.IFieldStripeEncoder#encode(net.agkn.field_stripe.record.reader.IRecordReader)
     */
    @Override
    public void encode(final IRecordReader recordReader)
            throws OperationFailedException {
        final IFieldType type = field.getType();
        if(type.isComposite()) throw new DeveloperException("A primitive field cannot be a composite type (" + type + ").");
        if(multiValue) {
            // NOTE:  the awkwardness of this structure is due to:
            //        1.  if there are no values then the UNSET state is emitted
            //        2.  the "repeated-value" marker must precede any repeated
            //            values (but it must not be used if there is only one value)
            //        3.  #endArray() must be called (even if there are no elements)
            // SEE:  CompositeFieldStripeEncoder#write()
            final boolean hasValues = recordReader.startArray();
            if(!hasValues || !recordReader.hasMoreElements()) 
                writer.writeUnset()/*explicit 'unset' if there are no values*/;
            else {/*there is at least one value*/
                // NOTE:  by contract 'null' values are not allowed within a
                //        repeated field. An error is thrown if they're encountered.
                if(!writeValue(recordReader)) throw new OperationFailedException("Repeated field \"" + field.getName() + "\" has an unset value in its collection.");
                while(recordReader.hasMoreElements()) {
                    writer.writeRepeated();
                    if(!writeValue(recordReader)) throw new OperationFailedException("Repeated field \"" + field.getName() + "\" has an unset value in its collection.");
                }
            }
            recordReader.endArray()/*by contract*/;
        } else {/*single-valued*/
            writeValue(recordReader);
        }
    }

    /**
     * Writes a single value from the specified non-<code>null</code> {@link IRecordReader}
     * and writes it to the appropriate {@link IFieldStripeWriter} writer method.
     * An unset value will cause {@link IFieldStripeWriter#writeUnset()} to be 
     * called and <code>false</code> to be returned.
     */
    private boolean writeValue(final IRecordReader recordReader)
            throws OperationFailedException {
        final IFieldType type = field.getType();
        switch((PrimitiveType)type) {
            case BYTE: {
                final byte value = recordReader.readByteField(IS_SET);
                if(IS_SET.isTrue()) {
                    writer.writeValue(value);
                    return true/*value is set*/;
                } else {/*the value is unset*/
                    writer.writeUnset();
                    return false/*value is unset*/;
                }
            }
            case SHORT: {
                final short value = recordReader.readShortField(IS_SET);
                if(IS_SET.isTrue()) {
                    writer.writeValue(value);
                    return true/*value is set*/;
                } else {/*the value is unset*/
                    writer.writeUnset();
                    return false/*value is unset*/;
                }
            }
            case INT: {
                final int value = recordReader.readIntField(IS_SET);
                if(IS_SET.isTrue()) {
                    writer.writeValue(value);
                    return true/*value is set*/;
                } else {/*the value is unset*/
                    writer.writeUnset();
                    return false/*value is unset*/;
                }
            }
            case LONG: {
                final long value = recordReader.readLongField(IS_SET);
                if(IS_SET.isTrue()) {
                    writer.writeValue(value);
                    return true/*value is set*/;
                } else {/*the value is unset*/
                    writer.writeUnset();
                    return false/*value is unset*/;
                }
            }
            case FLOAT: {
                final float value = recordReader.readFloatField(IS_SET);
                if(IS_SET.isTrue()) {
                    writer.writeValue(value);
                    return true/*value is set*/;
                } else {/*the value is unset*/
                    writer.writeUnset();
                    return false/*value is unset*/;
                }
            }
            case DOUBLE: {
                final double value = recordReader.readDoubleField(IS_SET);
                if(IS_SET.isTrue()) {
                    writer.writeValue(value);
                    return true/*value is set*/;
                } else {/*the value is unset*/
                    writer.writeUnset();
                    return false/*value is unset*/;
                }
            }
            case BOOLEAN: {
                final boolean value = recordReader.readBooleanField(IS_SET);
                if(IS_SET.isTrue()) {
                    writer.writeValue(value);
                    return true/*value is set*/;
                } else {/*the value is unset*/
                    writer.writeUnset();
                    return false/*value is unset*/;
                }
            }
            case STRING: {
                final String value = recordReader.readStringField(IS_SET);
                if(IS_SET.isTrue()) {
                    writer.writeValue(value);
                    return true/*value is set*/;
                } else {/*the value is unset*/
                    writer.writeUnset();
                    return false/*value is unset*/;
                }
            }

            default:
                throw new DeveloperException("Unknown primitive field type \"" + type + "\"."); 
        }
    }

    // ------------------------------------------------------------------------
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.encode.IFieldStripeEncoder#writeUnsetParent(int)
     */
    @Override
    public void writeUnsetParent(final int fieldDepth) 
            throws OperationFailedException {
        writer.writeUnsetParent(fieldDepth);
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.encode.IFieldStripeEncoder#writeRepeatedParent(int)
     */
    @Override
    public void writeRepeatedParent(final int fieldDepth)
            throws OperationFailedException {
        writer.writeRepeatedParent(fieldDepth);
    }
}