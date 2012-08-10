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

/**
 * An instruction as it is read from a {@link IFieldStripeReader}. It consists
 * of a {@link Kind}, an optional value and an optional parent depth (both
 * based on the kind). This is implemented as a C-style struct for convenience. 
 * The two instructions that are not associated with a parent depth 
 * ({@link Instruction#UNSET} and {@link Instruction#REPEATED_VALUE}) are 
 * provided for convenience.
 *
 * @author rgrzywinski
 */
public class Instruction {
    // all instructions not associated with a parent depth are provided for convenience
    public static final Instruction REPEATED_VALUE = new Instruction(Kind.REPEATED_VALUE);
    public static final Instruction UNSET = new Instruction(Kind.UNSET);

    // ************************************************************************
    /**
     * The possible kinds of instructions.
     */
    public static enum Kind {
        REPEATED_VALUE(false/*not associated with parent depth*/, false/*not associated with value*/),
        REPEATED_PARENT(true/*associated with parent depth*/, false/*not associated with value*/),
        UNSET(false/*not associated with parent depth*/, false/*not associated with value*/),
        UNSET_PARENT(true/*associated with parent depth*/, false/*not associated with value*/),
        VALUE(false/*not associated with parent depth*/, true/*associated with value*/);

        // --------------------------------------------------------------------
        private final boolean hasParentDepth;
        private final boolean hasValue;
        public boolean hasParentDepth() { return hasParentDepth; }
        public boolean hasValue() { return hasValue; }
        private Kind(final boolean hasParentDepth, final boolean hasValue) { 
            this.hasParentDepth = hasParentDepth;
            this.hasValue = hasValue; 
        }
    };

    // ************************************************************************
    /**
     * The {@link Kind} of instruction. This will never be <code>null</code>.
     */
    public final Kind kind;

    /**
     * The optional parent depth if the {@link Kind} is associated with a
     * {@link Kind#hasParentDepth()}. This is undefined if the kind is not
     * associated with a parent depth.
     */
    public final int parentDepth;

    /**
     * The optional value {@link Kind} is associated with a {@link Kind#hasValue()}.
     * This will never be <code>null</code> in the case where a kind is associated
     * with a value. This is undefined if the kind is not associated with a 
     * value (and may be <code>null</code>).
     */
    public final Object value;

    // ========================================================================
    /**
     * @param  kind the {@link Kind} of instruction. This cannot be <code>null</code>.
     */
    private Instruction(final Kind kind) {
        this.kind = kind;
        this.parentDepth = -1/*undefined*/;
        this.value = null/*undefined*/;
    }

    /**
     * @param  kind the {@link Kind} of instruction. This cannot be <code>null</code>.
     * @param  parentDepth the parent depth associated with this instruction.
     */
    public Instruction(final Kind kind, final int parentDepth) {
        this.kind = kind;
        this.parentDepth = parentDepth;
        this.value = null/*undefined*/;
    }

    /**
     * @param  kind the {@link Kind} of instruction. This cannot be <code>null</code>.
     * @param  value the value associated with this instruction. This cannot be
     *         <code>null</code>.
     */
    public Instruction(final Kind kind, final Object value) {
        this.kind = kind;
        this.parentDepth = -1/*undefined*/;
        this.value = value;
    }
}