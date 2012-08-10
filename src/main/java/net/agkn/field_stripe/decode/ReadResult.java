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

/**
 * The result of a call to a {@link IFieldStripeDecoder field stripe decoder}
 * {@link IFieldStripeDecoder#decode(net.agkn.field_stripe.record.writer.IRecordWriter) decode}.
 * It is immutable and is implemented as a C-style struct purely for convenience. 
 *
 * @author rgrzywinski
 * @see IFieldStripeDecoder
 */
/*package*/ class ReadResult {
    /**
     * The depth of the parent that is <var>UNSET</var>. A value less than one
     * indicates that there is no <var>UNSET</var> parent. 
     */
    public final int unsetParentDepth;

    /**
     * The depth of the parent that is repeated. A value less than one indicates
     * that no parent is repeated. 
     */
    public final int repeatedParentDepth;

    // ========================================================================
    /**
     * @param  unsetParentDepth the depth of the parent that is <var>UNSET</var>.
     *         A value less than one indicates that there is no <var>UNSET</var> 
     *         parent.
     * @param  repeatedParentDepth the depth of the parent that is repeated. A
     *         value less than one indicates that there is no repeated parent.
     * @see #ReadResult(ResultType)
     */
    public ReadResult(final int unsetParentDepth, final int repeatedParentDepth) {
        this.unsetParentDepth = unsetParentDepth;
        this.repeatedParentDepth = repeatedParentDepth;
    }
}