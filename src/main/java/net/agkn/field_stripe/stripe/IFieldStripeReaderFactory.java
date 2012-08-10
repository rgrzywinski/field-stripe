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
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.record.PrimitiveType;

/**
 * A factory for creating primitive {@link IFieldStripeReader field-stripe readers}.
 * The implementation itself dictates when the underlying resources are allocated
 * (e.g. when {@link #createFieldStripeReader(IField)} is called).
 *
 * @author rgrzywinski
 */
public interface IFieldStripeReaderFactory {
    /**
     * @param  field the {@link PrimitiveType primitive} {@link IField} for 
     *         which the {@link IFieldStripeReader} is to be created. This will 
     *         never be <code>null</code>. Each field in a tree is guaranteed
     *         to be unique.
     * @return this will never be <code>null</code>
     * @throws OperationFailedException if the writer could not be created for
     *         any reason (e.g. underlying resources could not be allocated or
     *         the field is not known to this factory). Subclasses may provide 
     *         more information as to the nature of the failure.
     */
    IFieldStripeReader createFieldStripeReader(IField field)
        throws OperationFailedException;
}