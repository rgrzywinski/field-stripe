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

import java.util.List;

/**
 * A composite {@link IFieldType field type}. There are two restrictions on 
 * composite types:
 * <ul>
 *   <li>it must form a tree rather than a graph. Specifically, it cannot have 
 *       circular types;</li>
 *   <li>it must have <i>at least</i> one field;</li>
 * </ul>
 *
 * @author rgrzywinski
 */
public interface ICompositeType extends IFieldType {
    /**
     * @return the name of this composite type. This will never be <code>null</code> 
     *         or blank.
     */
    String getName();

    // CHECK:  does the namespace or full-qualified name need to be present?

    // ------------------------------------------------------------------------
    /**
     * @param  path the sequence of dense indexes (as would be found in {@link #getFields()})
     *         identifying the desired {@link IField}.
     * @return the {@link IField} at the specified path. This will be <code>null</code>
     *         if the specified path does not exist. 
     */
    // TODO:  add #getFieldByFieldIndex() that uses IField#getIndex()
    IField getField(int... path);

    /**
     * @return the <i>read-only</i> collection of {@link IField fields} ordered 
     *         by {@link IField#getIndex() field index}. This collection will
     *         be dense but the {@link IField#getIndex() indexes} may be sparse.
     *         This will never be <code>null</code> nor will it be empty (as at 
     *         least one field is required).
     */
    List<IField> getFields();
}