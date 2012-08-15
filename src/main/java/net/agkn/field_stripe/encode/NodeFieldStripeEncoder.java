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

import java.util.List;

import net.agkn.field_stripe.exception.OperationFailedException;
import net.agkn.field_stripe.record.ICompositeType;
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.record.reader.IRecordReader;
import net.jcip.annotations.NotThreadSafe;

/**
 * A field-stripe encoder for a node field -- a field whose {@link IField#getType() type}
 * is {@link ICompositeType}. 
 *
 * @author rgrzywinski
 */
@NotThreadSafe
public class NodeFieldStripeEncoder extends AbstractEncoder
                                 implements IFieldStripeEncoder {
    // the list (in the order that matches the fields for this encoder) of child
    // encoders. This will never be null or empty.
    private final List<IFieldStripeEncoder> childEncoders;

    // ========================================================================
    /**
     * {@inheritDoc}
     */
    public NodeFieldStripeEncoder(final List<IFieldStripeEncoder> childEncoders, final IField field) {
        super(field, field.getPath().getDepth());
        this.childEncoders = childEncoders;
    }

    // ========================================================================
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.encode.IFieldStripeEncoder#encode(net.agkn.field_stripe.record.reader.IRecordReader)
     */
    @Override
    public void encode(final IRecordReader recordReader)
            throws OperationFailedException {
        if(multiValue) {
            // NOTE:  the awkwardness of this structure is due to:
            //        1.  if there are no values then an explicit parent-is-UNSET 
            //            must be written
            //        2.  the "repeated-value" marker must precede any repeated
            //            values (but it must not be used if there is only one value)
            //        3.  #endArray() must be called (even if there are no elements)
            // SEE:  PrimitiveFieldStripeEncoder#write()
            final boolean hasArray = recordReader.startArray();
            if(!hasArray || !recordReader.hasMoreElements()) 
                writeUnsetParent(fieldDepth)/*explicit 'unset' if there are no values*/;
            else {/*there is at least one value*/
                // NOTE:  by contract 'null' values are not allowed within a
                //        repeated field. An error is thrown if they're encountered.
                if(!writeChildren(recordReader)) throw new OperationFailedException("Repeated nested field \"" + field.getName() + "\" has an unset value in its collection.");
                while(recordReader.hasMoreElements()) {
                    writeRepeatedParent(fieldDepth);
                    if(!writeChildren(recordReader)) throw new OperationFailedException("Repeated nested field \"" + field.getName() + "\" has an unset value in its collection.");
                }
            }
            recordReader.endArray()/*by contract*/;
        } else {/*single-valued*/
            writeChildren(recordReader);
        }
    }

    /**
     * Writes the structure that are the children and returns <code>true</code>
     * if a structure was written. An unset value will cause {@link #writeUnsetParent(int)} 
     *  to be called and <code>false</code> to be returned.
     */
    private boolean writeChildren(final IRecordReader recordReader)
            throws OperationFailedException {
        final boolean hasStructure = recordReader.startStructure();
        if(hasStructure) {
            // allow each child to write out its own value(s)
            for(final IFieldStripeEncoder childEncoder : childEncoders)
                childEncoder.encode(recordReader);

            recordReader.endStructure()/*by contract*/;

            return true/*value is set*/;
        } else {/*the structure is unset (i.e. it doesn't exist)*/
            writeUnsetParent(fieldDepth/*the depth of the field associated with this encoder*/);
            return false/*value is unset*/;
        }
    }
    
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public void writeUnsetParent(final int fieldDepth) 
            throws OperationFailedException {
        for(final IFieldStripeEncoder childEncoder : childEncoders)
            childEncoder.writeUnsetParent(fieldDepth);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeRepeatedParent(final int fieldDepth)
            throws OperationFailedException {
        for(final IFieldStripeEncoder childEncoder : childEncoders)
            childEncoder.writeRepeatedParent(fieldDepth);
    }
}