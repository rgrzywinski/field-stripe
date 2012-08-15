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

import net.agkn.field_stripe.exception.DeveloperException;
import net.agkn.field_stripe.record.FieldQualifier;
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.record.IFieldType;
import net.agkn.field_stripe.record.Path;

/**
 * A {@link IField} used with Protobuf types. This is a simple container (POJO)
 * type. The {@link Path path} must be explicitly {@link #setPath(Path)} after
 * the tree is built. See {@link ProtobufCompositeType#resolve(Path)} for more
 * information.
 *
 * @author rgrzywinski
 */
public final class ProtobufField implements IField {
    // NOTE:  the path cannot be set on construction since the parent needs its
    //        children fields (i.e. this object) before it can construct its
    //        type. In other words, when this field is being constructed, the
    //        parent has not yet been constructed therefore the path cannot yet
    //        exist. This is set once and only once in #setPath().
    private Path path;

    private final int index;
    private final FieldQualifier qualifier;
    private final IFieldType type;
    private final String name;

    // ========================================================================
    /**
     * @param  index refer to {@link IField#getIndex()} for more information 
     * @param  qualifier refer to {@link IField#getQualifier()} for more information 
     * @param  type refer to {@link IField#getType()} for more information 
     * @param  name refer to {@link IField#getName()} for more information
     * @see #setPath(Path) 
     */
    public ProtobufField(final int index, final FieldQualifier qualifier,
                         final IFieldType type, final String name) {
        this.index = index;
        this.qualifier = qualifier;
        this.type = type;
        this.name = name;
    }

    // ========================================================================
    public void setPath(final Path path) { this.path = path; }
    @Override public final Path getPath() { return path; }

    @Override public final int getIndex() { return index; }
    @Override public final FieldQualifier getQualifier() { return qualifier; }
    @Override public final IFieldType getType() { return type; }
    @Override public final String getName() { return name; }

    // ========================================================================
    /**
     * Deep clones this {@link ProtobufField} (and its type). Only fields that  
     * have not yet had their {@link #setPath(Path) path's set} can be cloned.
     * 
     * @return a deep clone of this field. This will never be <code>null</code>.
     */
    @Override
    public ProtobufField clone() {
        if(path != null) throw new DeveloperException("Protobuf fields can only be cloned before their paths are resolved.");
        final IFieldType clonedFieldType = type.isComposite() ? ((ProtobufCompositeType)type).clone() : type/*don't need to clone primitives*/;
        return new ProtobufField(index, qualifier, clonedFieldType, name);
    }
}