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

import java.util.ArrayList;
import java.util.List;

import net.agkn.field_stripe.exception.OperationFailedException;
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.record.writer.IRecordWriter;

/**
 * A node {@link IFieldStripeDecoder}. Nodes are purely synthetic constructs 
 * in that they do not map to physical field stripes (e.g. to a file) and are
 * used to maintain state in the decoder tree.
 *
 * @author rgrzywinski
 */
// NOTE:  simplified versions of this decoder can be written for the cases where
//        no parent is repeated, no parent is repeated or optional or if the 
//        node itself is not repeated.
public class NodeFieldStripeDecoder extends AbstractDecoder
                                 implements IFieldStripeDecoder {
    // the list (in the order that matches the fields for this encoder) of child
    // decoders. This will never be null or empty.
    private final List<IFieldStripeDecoder> childDecoders;

    // the first child decoder provided for convenience
    private final IFieldStripeDecoder firstChild;

    // ========================================================================
    /**
     * {@inheritDoc}
     */
    public NodeFieldStripeDecoder(final List<IFieldStripeDecoder> childDecoders, final IField field) {
        super(field, field.getPath().getDepth());

        this.childDecoders = new ArrayList<IFieldStripeDecoder>(childDecoders)/*copy for sanity*/;
        this.firstChild = childDecoders.get(0/*first child*/);
    }

    // ========================================================================
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.decode.IFieldStripeDecoder#getUnsetParentDepth()
     */
    @Override
    public int getUnsetParentDepth() 
            throws OperationFailedException {
        // ask the first child (any child will do since by design they all have
        // the same meta-data) if there is an unset parent
        return firstChild.getUnsetParentDepth();
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.decode.IFieldStripeDecoder#decode(net.agkn.field_stripe.record.writer.IRecordWriter)
     */
    @Override
    @SuppressWarnings("null") /*'childResult'*/
    public ReadResult decode(final IRecordWriter recordWriter)
            throws OperationFailedException {
        final int unsetParentDepth = getUnsetParentDepth();
        if(unsetParentDepth < 0)
            return null/*no record to read*/;
        else if(unsetParentDepth == 0/*set parent*/) {
            if(multiValue) recordWriter.startArray(field);
        } else if(unsetParentDepth == fieldDepth)/*there is an unset parent and it's this parent*/ 
            recordWriter.writeUnsetField(field);
        /* else -- there is an unset field and it's some other parent */

        // NOTE:  there is at least one child by contract
        // NOTE:  even if the child indicated that a parent is unset, each child
        //        still needs to be called to keep all decoders in sync and to
        //        check for a repeated parent.
        // NOTE:  only the last result is used as they will all be of the same
        //        type by design (e.g. if one reports a repeated parent then
        //        all of them will report the exact same repeated parent) 
        ReadResult childResult = null/*none to start and set for each child*/;
        do {
            if(unsetParentDepth == 0/*set parent*/) recordWriter.startStructure(field);

            for(final IFieldStripeDecoder decoder : childDecoders) 
                childResult = decoder.decode(recordWriter);

            if(unsetParentDepth == 0/*set parent*/) recordWriter.endStructure();
        } while(childResult.repeatedParentDepth == fieldDepth)/*while the repeated parent is this parent*/;

        if(unsetParentDepth == 0/*set parent*/) {
            if(multiValue) recordWriter.endArray();
        } /* else -- a parent is unset (or there is no data) */

        return childResult;
    }
}