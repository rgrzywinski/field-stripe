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
import net.agkn.field_stripe.record.ICompositeType;
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.record.IFieldType;
import net.agkn.field_stripe.record.PrimitiveType;
import net.agkn.field_stripe.stripe.IFieldStripeReader;
import net.agkn.field_stripe.stripe.IFieldStripeReaderFactory;

/**
 * A combination factory and builder for {@link IFieldStripeDecoder field-stripe decoders}
 * from a {@link IFieldType#isComposite() composite} {@link IFieldType field type}.
 *
 * @author rgrzywinski
 */
public class FieldStripeDecoderFactory {
    private final IFieldStripeReaderFactory fieldStripeReaderFactory;

    // ========================================================================
    /**
     * @param  fieldStripeReaderFactory the {@link IFieldStripeReaderFactory}
     *         that will be used to create {@link IFieldStripeReader field-stripe readers}
     *         for specified {@link PrimitiveType primitive-type} (leaf) {@link IField fields}.
     *         This cannot be <code>null</code>.
     */
    public FieldStripeDecoderFactory(final IFieldStripeReaderFactory fieldStripeReaderFactory) {
        this.fieldStripeReaderFactory = fieldStripeReaderFactory;
    }

    // ========================================================================
    /**
     * Creates the tree of {@link IFieldStripeDecoder field-stripe decoders}
     * from the specified {@link IFieldType type}. For each {@link PrimitiveType}
     * (leaf) {@link IField} a {@link IFieldStripeReader} will be created. It
     * is left to the {@link IFieldStripeReaderFactory} to determine if any 
     * underlying resources are allocated at that time.
     * 
     * @param  fieldType the {@link IFieldType field type} from which the decoder
     *         tree is built. The type must be a {@link IFieldType#isComposite() composite}.
     *         This cannot be <code>null</code>.
     * @return the {@link RootFieldStripeDecoder} for the tree of {@link IFieldStripeDecoder field-stripe decoders}.
     *         This will never be <code>null</code>.
     * @throws OperationFailedException if {@link IFieldStripeReaderFactory#createFieldStripeReader(IField)}
     *         failed for any reason.
     */
    // TODO:  allow a whitelist / blacklist of fields to be specified
    public RootFieldStripeDecoder createDecoderTree(final IFieldType fieldType)
            throws OperationFailedException {
        final ICompositeType compositeType = (ICompositeType)fieldType;

        // the tree is built depth-first (recurse-descent)
        final List<IFieldStripeDecoder> childDecoders = new ArrayList<IFieldStripeDecoder>();
        for(final IField childField : compositeType.getFields()) {
            final IFieldStripeDecoder childDecoder = build(childField);
            childDecoders.add(childDecoder);
        }

        return new RootFieldStripeDecoder(childDecoders);
    }

    // ========================================================================
    /**
     * Builds a {@link IFieldStripeDecoder} for the specified {@link IField}
     * recursively depth-first.
     * 
     * @param  field the {@link IField} for which the {@link IFieldStripeDecoder}
     *         is to be built. This cannot be <code>null</code>.
     * @return the {@link IFieldStripeDecoder} for the specified {@link IField}.
     *         This will never be <code>null</code>.
     * @throws OperationFailedException if {@link IFieldStripeReaderFactory#createFieldStripeReader(IField)}
     *         failed for any reason.
     * @see #buildComposite(IField)
     * @see #buildPrimitive(IField)
     */
    private IFieldStripeDecoder build(final IField field) 
            throws OperationFailedException {
        final IFieldType type = field.getType();
        if(type.isComposite())
            return buildComposite(field);
        else/*not composite (i.e. primitive)*/
            return buildPrimitive(field);
    }

    /**
     * @param  field the {@link IField} for which the {@link IFieldStripeDecoder}
     *         is desired. This cannot be <code>null</code> and its {@link IFieldType type} 
     *         must be a {@link ICompositeType}.
     * @return this will never be <code>null</code>
     * @throws OperationFailedException if {@link IFieldStripeReaderFactory#createFieldStripeReader(IField)}
     *         failed for any reason.
     * @see #buildPrimitive(IField, int)
     */
    private IFieldStripeDecoder buildComposite(final IField field) 
            throws OperationFailedException {
        // recurse depth-first to create the children (before this parent is
        // created)
        final ICompositeType type = (ICompositeType)field.getType()/*by contract*/;
        final List<IFieldStripeDecoder> childDecoders = new ArrayList<IFieldStripeDecoder>();
        for(final IField childField : type.getFields())
            childDecoders.add(build(childField));

        return new NodeFieldStripeDecoder(childDecoders, field);
    }

    /**
     * @param  field the {@link IField} for which the {@link IFieldStripeDecoder}
     *         is desired. This cannot be <code>null</code> and its {@link IFieldType type} 
     *         must be a {@link PrimitiveType}.
     * @return this will never be <code>null</code>
     * @throws OperationFailedException if {@link IFieldStripeReaderFactory#createFieldStripeReader(IField)}
     *         failed for any reason.
     * @see #buildComposite(IField, int)
     */
    private IFieldStripeDecoder buildPrimitive(final IField field)
            throws OperationFailedException {
        final IFieldStripeReader reader = fieldStripeReaderFactory.createFieldStripeReader(field);
        return new LeafFieldStripeDecoder(reader, field);
    }
}