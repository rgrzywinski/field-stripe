package net.agkn.field_stripe.record.protobuf;

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
import java.util.Collections;
import java.util.List;

import net.agkn.field_stripe.exception.DeveloperException;
import net.agkn.field_stripe.record.ICompositeType;
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.record.IFieldType;
import net.agkn.field_stripe.record.Path;

/**
 * A {@link ICompositeType} built from a Protobuf {@link com.dyuproject.protostuff.parser.Message}.
 * All fields are not valid until {@link #resolve(Path)} has been called on this
 * object.
 *
 * @author rgrzywinski
 */
public final class ProtobufCompositeType implements ICompositeType {
    private final String name;
    private final List<IField> fields;

    // ========================================================================
    /**
     * @param  name the name of the type. This cannot be <code>null</code> or
     *         blank.
     * @param  fields the list of {@link IField fields} for this composite type.
     *         This cannot be <code>null</code> or empty (as at least one field
     *         is required by contract). A shallow copy of the list is made. 
     */
    public ProtobufCompositeType(final String name, final List<IField> fields) {
        this.name = name;
        this.fields = Collections.unmodifiableList(new ArrayList<IField>(fields)/*a copy is made by contract*/);
    }

    // ========================================================================
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.ICompositeType#getName()
     */
    @Override
    public final String getName() { return name; }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.ICompositeType#getField(int[])
     */
    @Override
    public final IField getField(final int... path) {
        if(path.length < 1) return null/*no path was specified*/;
        if(path[0] >= fields.size()) return null/*not in path*/;
        final IField childField = fields.get(path[0]);
        if(path.length == 1) return childField;
        if(!(childField.getType() instanceof ICompositeType)) return null/*can't be in path as child isn't a composite*/;

        // walk the path
        final int[] subpath = new int[path.length - 1];
        System.arraycopy(path, 1, subpath, 0, (path.length - 1));
        return ((ICompositeType)childField.getType()).getField(subpath);
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.ICompositeType#getFields()
     */
    @Override
    public final List<IField> getFields() { return fields; }

    // ------------------------------------------------------------------------
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.record.IFieldType#isComposite()
     */
    @Override
    public final boolean isComposite() { return true/*by definition*/; }

    // ========================================================================
    /**
     * Resolves and sets all of the {@link IField#getPath() paths} on the children
     * {@link IField fields}.
     * 
     * @param  path the path of the field that contains this type. This cannot
     *         be <code>null</code> though it may be empty (the root).
     */
    public void resolve(final Path path) {
        for(final IField childField : fields) {
            if(!(childField instanceof ProtobufField)) throw new DeveloperException("A ProtobufCompositeType must have ProtobufField children.");
            ((ProtobufField)childField).setPath(new Path(path, childField)/*path must include field*/);

            // NOTE:  the recursion is done here in the composite type rather
            //        the cluttering / obfuscating the field implementation
            final IFieldType childFieldType = childField.getType(); 
            if(childFieldType.isComposite()) {
                if(!(childFieldType instanceof ProtobufCompositeType)) throw new DeveloperException("A composite ProtobufField must have a ProtobufCompositeType.");
                ((ProtobufCompositeType)childFieldType).resolve(new Path(path, childField));
            } /* else -- the field is a leaf */
        }
    }

    // ========================================================================
    /**
     * @return a deep clone of this type. This will never be <code>null</code>.
     */
    @Override
    public ProtobufCompositeType clone() {
        final List<IField> clonedFields = new ArrayList<IField>(fields.size());
        for(final IField field : fields)
            clonedFields.add(((ProtobufField)field).clone());
        return new ProtobufCompositeType(name, clonedFields);
    }
}