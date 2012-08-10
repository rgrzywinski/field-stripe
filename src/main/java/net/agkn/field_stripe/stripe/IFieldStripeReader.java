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

import net.agkn.common.exception.OperationFailedException;
import net.agkn.field_stripe.decode.IFieldStripeDecoder;

/**
 * A reader for field stripes. Field stripes are written as a series of 
 * {@link Instruction instructions} and their associated values. Because of the 
 * structure of a field it may not be possible to determine the next instruction 
 * <i>a priori</i> (though the current instruction does dictate the possible  
 * set of next instructions). The {@link #readInstruction()} method informs the 
 * {@link IFieldStripeDecoder decoder} as to the next instruction.<p/>
 * 
 * The reader may require meta-data on construction as to the depth of the 
 * corresponding field. This can be used to provide fixed width fields for the
 * cases where a parent depth is specified.<p/>
 *
 * @author rgrzywinski
 * @see IFieldStripeWriter
 */
public interface IFieldStripeReader {
    /**
     * Reads and returns the next {@link Instruction instruction}.
     * 
     * @return the instruction (including any parent depths that may have been
     *         associated with it) that was read. This will be <code>null</code> 
     *         if there are no more instructions to be read (i.e. the end-of-stripe 
     *         has been reached).
     * @throws OperationFailedException if there was an error reading the next 
     *         instruction. Subclasses may contain additional information as to  
     *         the nature of the failure (e.g. error reading the file or unknown 
     *         instruction).
     */
    Instruction readInstruction()
        throws OperationFailedException;
}