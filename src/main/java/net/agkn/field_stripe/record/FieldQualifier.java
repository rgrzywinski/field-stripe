package net.agkn.field_stripe.record;

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
 * The enumeration of possible {@link IField field} qualifiers.
 *
 * @author rgrzywinski
 */
public enum FieldQualifier {
    // NOTE:  this order cannot be changed without possibly affecting the 
    //        serialization format
    ONE(false/*single-valued*/)/*"required"*/,
    ZERO_OR_ONE(false/*single-valued*/)/*"optional"*/,
    ZERO_OR_MORE(true/*multi-valued*/)/*"repeated"*/;

    // ************************************************************************
    // NOTE:  purely for convenience so that a switch-case block is not needed
    private final boolean multiValue;

    // ------------------------------------------------------------------------
    /**
     * @param multiValue <code>true</code> if and only if this qualifier 
     *        represents the opportunity for more than one value. <code>false</code>
     *        otherwise
     */
    private FieldQualifier(final boolean multiValue) { this.multiValue = multiValue; }

    /**
     * @return <code>true</code> if and only if this qualifier represents the 
     *         opportunity for more than one value. <code>false</code> otherwise.
     */
    public final boolean isMultiValue() { return multiValue; }
}