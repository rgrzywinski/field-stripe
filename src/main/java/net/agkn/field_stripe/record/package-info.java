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
 * A record is defined by its schema. A schema consists of one or more {@link IField fields}. 
 * Each field has a unique integer identifier that indicates the field position,
 * a {@link FieldQualifier field qualifier} ({@link FieldQualifier#ONE one} 
 * ("required"), {@link FieldQualifier#ZERO_OR_ONE zero-or-one} ("optional") or 
 * {@link FieldQualifier#ZERO_OR_MORE zero-or-more} ("repeated")), a {@link IFieldType type} 
 * and a symbolic name. Types are either nested (called {@link ICompositeType composite types}
 * or node types) or {@link PrimitiveType primitive} (called leaf types). A
 * schema cannot have circular dependencies -- it is a strict tree.<p/>
 * 
 * Each {@link IField field} instance in a schema tree is guaranteed to be 
 * unique. This was done to allow the fields to be used as keys in lookups. 
 * Typically a field only has its name to be identified by and that name is only 
 * guaranteed to be unique within it's immediate parent. Without using the full 
 * parent context it would be otherwise impossible to uniquely identify a field.<p/>
 *
 * @author rgrzywinski
 */