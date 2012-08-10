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
 * A field in a {@link ICompositeType schema} is composed of an index,
 * qualifier, type, and name. There are two kinds of types: leaf types which   
 * the are atoms (primitives, scalars, etc) and node types which are composite 
 * types.<p/>
 * 
 * Each {@link IField field} instance in a schema tree is guaranteed to be 
 * unique. This was done to allow the fields to be used as keys in lookups. 
 * Typically a field only has its name to be identified by and that name is only 
 * guaranteed to be unique within it's immediate parent. Without using the full 
 * parent context it would be otherwise impossible to uniquely identify a field.
 *
 * @author rgrzywinski
 */
public interface IField {
    /**
     * @return the {@link Path} of this field within the parent schema. The path
     *         includes this field as well as any parents. This will never be 
     *         <code>null</code> nor will the path ever be empty (as this field 
     *         is included in the path).
     */
    Path getPath();

    // ------------------------------------------------------------------------
    /**
     * @return the <i>unique</i> index of this field within a parent {@link ICompositeType schema}.
     *         It may be a positive integer or zero. Indexes may be sparse 
     *         within a schema.
     */
    int getIndex();

    /**
     * @return the {@link FieldQualifier qualifier} of the field. This will 
     *         never be <code>null</code>. 
     */
    FieldQualifier getQualifier();

    /**
     * @return the {@link IFieldType type} of this field. This will never be
     *         <code>null</code>.
     */
    IFieldType getType();

    /**
     * @return the <i>unique</i> name of this field within a parent {@link ICompositeType schema}. 
     *         This will never be <code>null</code> or blank.
     * @see ICompositeType#getFields()
     */
    String getName();
}