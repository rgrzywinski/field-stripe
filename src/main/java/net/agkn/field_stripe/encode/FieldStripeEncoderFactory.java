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

import java.util.ArrayList;
import java.util.List;

import net.agkn.field_stripe.exception.OperationFailedException;
import net.agkn.field_stripe.record.ICompositeType;
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.record.IFieldType;
import net.agkn.field_stripe.record.PrimitiveType;
import net.agkn.field_stripe.stripe.IFieldStripeWriter;
import net.agkn.field_stripe.stripe.IFieldStripeWriterFactory;

/**
 * A combination factory and builder for {@link IFieldStripeEncoder field-stripe encoders}
 * from a {@link IFieldType#isComposite() composite} {@link IFieldType field type}.
 *
 * @author rgrzywinski
 */
public class FieldStripeEncoderFactory {
    private final IFieldStripeWriterFactory fieldStripeWriterFactory;

    // ========================================================================
    /**
     * @param  fieldStripeWriterFactory the {@link IFieldStripeWriterFactory}
     *         that will be used to create {@link IFieldStripeWriter field-stripe writers}
     *         for all {@link PrimitiveType primitive-type} (leaf) {@link IField fields}.
     *         This cannot be <code>null</code>.
     */
    public FieldStripeEncoderFactory(final IFieldStripeWriterFactory fieldStripeWriterFactory) {
        this.fieldStripeWriterFactory = fieldStripeWriterFactory;
    }

    // ========================================================================
    /**
     * Creates the tree of {@link IFieldStripeEncoder field-stripe encoders}
     * from the specified {@link IFieldType type}. For each {@link PrimitiveType}
     * (leaf) {@link IField} a {@link IFieldStripeWriter} will be created. It
     * is left to the {@link IFieldStripeWriterFactory} to determine if any 
     * underlying resources are allocated at that time.
     * 
     * @param  fieldType the {@link IFieldType field type} from which the encoder
     *         tree is built. The type must be a {@link IFieldType#isComposite() composite}.
     *         This cannot be <code>null</code>.
     * @return the {@link RootFieldStripeEncoder} for the tree of {@link IFieldStripeEncoder field-stripe encoders}.
     *         This will never be <code>null</code>.
     * @throws OperationFailedException if {@link IFieldStripeWriterFactory#createFieldStripeWriter(IField)}
     *         failed for any reason.
     */
    public RootFieldStripeEncoder createEncoderTree(final IFieldType fieldType)
            throws OperationFailedException {
        final ICompositeType compositeType = (ICompositeType)fieldType;

        // the tree is built depth-first (recurse-descent)
        final List<IFieldStripeEncoder> childEncoders = new ArrayList<IFieldStripeEncoder>();
        for(final IField childField : compositeType.getFields()) {
            final IFieldStripeEncoder childEncoder = build(childField);
            childEncoders.add(childEncoder);
        }

        return new RootFieldStripeEncoder(childEncoders);
    }

    // ========================================================================
    /**
     * Builds a {@link IFieldStripeEncoder} for the specified {@link IField}
     * recursively depth-first.
     * 
     * @param  field the {@link IField} for which the {@link IFieldStripeEncoder}
     *         is to be built. This cannot be <code>null</code>.
     * @return the {@link IFieldStripeEncoder} for the specified {@link IField}.
     *         This will never be <code>null</code>.
     * @throws OperationFailedException if {@link IFieldStripeWriterFactory#createFieldStripeWriter(IField)}
     *         failed for any reason.
     * @see #buildComposite(IField)
     * @see #buildPrimitive(IField)
     */
    private IFieldStripeEncoder build(final IField field) 
            throws OperationFailedException {
        final IFieldType type = field.getType();
        if(type.isComposite())
            return buildComposite(field);
        else/*not composite (i.e. primitive)*/
            return buildPrimitive(field);
    }

    /**
     * @param  field the {@link IField} for which the {@link IFieldStripeEncoder}
     *         is desired. This cannot be <code>null</code> and its {@link IFieldType type} 
     *         must be a {@link ICompositeType}.
     * @return this will never be <code>null</code>
     * @throws OperationFailedException if {@link IFieldStripeWriterFactory#createFieldStripeWriter(IField)}
     *         failed for any reason.
     * @see #buildPrimitive(IField, int)
     */
    private IFieldStripeEncoder buildComposite(final IField field) 
            throws OperationFailedException {
        // recurse depth-first to create the children (before this parent is
        // created)
        final ICompositeType type = (ICompositeType)field.getType()/*by contract*/;
        final List<IFieldStripeEncoder> childEncoders = new ArrayList<IFieldStripeEncoder>();
        for(final IField childField : type.getFields())
            childEncoders.add(build(childField));

        return new NodeFieldStripeEncoder(childEncoders, field);
    }

    /**
     * @param  field the {@link IField} for which the {@link IFieldStripeEncoder}
     *         is desired. This cannot be <code>null</code> and its {@link IFieldType type} 
     *         must be a {@link PrimitiveType}.
     * @return this will never be <code>null</code>
     * @throws OperationFailedException if {@link IFieldStripeWriterFactory#createFieldStripeWriter(IField)}
     *         failed for any reason.
     * @see #buildComposite(IField, int)
     */
    private IFieldStripeEncoder buildPrimitive(final IField field)
            throws OperationFailedException {
        final IFieldStripeWriter writer = fieldStripeWriterFactory.createFieldStripeWriter(field);
        return new LeafFieldStripeEncoder(writer, field);
    }
}