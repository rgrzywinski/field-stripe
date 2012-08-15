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

import net.agkn.field_stripe.exception.OperationFailedException;
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.record.PrimitiveType;

/**
 * A factory for creating primitive {@link IFieldStripeWriter field-stripe writers}.
 * The implementation itself dictates when the underlying resources are allocated
 * (e.g. when {@link #createFieldStripeWriter(IField)} is called).<p/>
 * 
 * {@link #closeAllWriters()} <i>must</i> be called to {@link IFieldStripeWriter#close() close}
 * the underlying writers. 
 *
 * @author rgrzywinski
 */
// NOTE:  at this time, writers are only created for leaf / primitive-typed
//        fields
public interface IFieldStripeWriterFactory {
    /**
     * @param  field the {@link PrimitiveType primitive} {@link IField} for 
     *         which the {@link IFieldStripeWriter} is to be created. This will 
     *         never be <code>null</code>. Each field in a tree is guaranteed
     *         to be unique.
     * @return this will never be <code>null</code>
     * @throws OperationFailedException if the writer could not be created for
     *         any reason (e.g. underlying resources could not be allocated).
     *         Subclasses may provide more information as to the nature of the
     *         failure.
     */
    // CHECK:  how to best express this interface so that all of the information
    //         that can be used for possible optimizations is made available?
    IFieldStripeWriter createFieldStripeWriter(IField field)
        throws OperationFailedException;

    // ------------------------------------------------------------------------
    /**
     * Closes all {@link IFieldStripeWriter field-stripe writers} that have been
     * {@link #createFieldStripeWriter(IField) created} by this factory and haven't
     * already been closed. Closing a closed writer has no effect. 
     * 
     * @see #createFieldStripeWriter(IField)
     * @see IFieldStripeWriter#close()
     */
    // CHECK:  create a #closeWriter(IField)?
    // CHECK:  what is the best paradigm to close the writers?
    void closeAllWriters()
        throws OperationFailedException;
}