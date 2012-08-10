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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A means for identifying a traversal of nodes to a {@link IField field} in a
 * schema tree. The root of the tree is not explicitly expressed in the path and
 * can always be implied. Depth and length are used interchangeably throughout.
 * This object is read-only / immutable. No effort is made in this class to 
 * ensure that the path is valid (e.g. ensuring that all parents are nodes or
 * that a leaf is not a node).
 *
 * @author rgrzywinski
 */
public class Path implements Iterable<IField> {
    private final ArrayList<IField> fieldPath;

    // ========================================================================
    /**
     * Creates an empty (root only) path.
     */
    public Path() {
        this.fieldPath = new ArrayList<IField>()/*empty*/;
    }

    /**
     * @param  parentPath the parent <code>Path</code>. This cannot be <code>null</code>  
     *         though it may be empty to indicate an empty (only the root) path.  
     *         (A shallow copy will be made.)
     * @param  field the {@link IField field} to be added after the parent
     *         path. This cannot be <code>null</code>.
     */
    public Path(final Path parentPath, final IField field) {
        this.fieldPath = new ArrayList<IField>(parentPath.getDepth() + 1/*field*/);
        this.fieldPath.addAll(parentPath.fieldPath);
        this.fieldPath.add(field)/*added last*/;
    }

    /**
     * @param  parentPath the list of {@link IField fields} that composes the 
     *         parent path. This cannot be <code>null</code> though it may be 
     *         empty to indicate an empty (only the root) path. (A shallow copy 
     *         will be made.)
     * @param  field the {@link IField field} to be added after the parent
     *         path. This cannot be <code>null</code>.
     */
    public Path(final List<IField> parentPath, final IField field) {
        this.fieldPath = new ArrayList<IField>(parentPath.size() + 1/*field*/);
        this.fieldPath.addAll(parentPath);
        this.fieldPath.add(field)/*added last*/;
    }

    /**
     * @param  path the list of {@link IField fields} that composes the path.
     *         This cannot be <code>null</code> though it may be empty to indicate
     *         an empty (only the root) path. (A shallow copy will be made.)
     */
    public Path(final List<IField> path) {
        this.fieldPath = new ArrayList<IField>(path);
    }

    // ========================================================================
    /**
     * @return the depth or length of the path. This will never be negative 
     *         though it may be zero (a root-only path).
     */
    public int getDepth() { return fieldPath.size(); }

    /**
     * @param  depth the 0-based depth of the path whose {@link IField field} 
     *         is desired. This cannot be negative and must be less than the 
     *         {@link #getDepth() path's depth}.
     * @return the {@link IField field} at the desired depth.
     * @throws ArrayIndexOutOfBoundsException if the specified depth is not
     *         within the allowed bounds.
     */
    public IField getField(final int depth) { return fieldPath.get(depth); }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<IField> iterator() { return fieldPath.iterator(); }

    // ========================================================================
    /**
     * @param  path the <code>Path</code> that is to be compared against this
     *         one. This cannot be <code>null</code> (though it may be empty).
     * @return the length of the common sub-path when matching from the root.
     *         <code>0</code> is returned if the two paths differ completely.
     *         A negative value is never returned. Matching is done using the
     *         {@link IField#equals(Object)} method.
     * @see #getCommonSubpath(Path)
     */
    public int getCommonPathLength(final Path path) {
        final int maxLength = Math.min(path.getDepth(), getDepth());
        for(int i=0; i<maxLength; i++) {
            if(!path.getField(i).equals(getField(i)))
                return i;
            /* else -- the two fields match */
        }
        return maxLength;
    }

    /**
     * @param  path the <code>Path</code> that is to be compared against this
     *         one. This cannot be <code>null</code> (though it may be empty).
     * @return the common sub-path between this and the specified path when 
     *         matching from the root. This will never be <code>null</code> 
     *         though it may be empty to indicate that the paths only had the
     *         root in common. Matching is done using the {@link IField#equals(Object)} 
     *         method.
     * @see #getCommonPathLength(Path)      
     */
    public Path getCommonSubpath(final Path path) {
        final int maxLength = Math.min(path.getDepth(), getDepth());
        final List<IField> subpath = new ArrayList<IField>(maxLength);
        for(int i=0; i<maxLength; i++) {
            final IField field = getField(i);
            if(!path.getField(i).equals(field))
                break;
            /* else -- the two fields match */
            subpath.add(field);
        }
        return new Path(subpath);
    }

    // ========================================================================
    // convenience methods

    /**
     * @return the {@link Path} that is the parent to this path. If this path
     *         is an empty path then an empty path is returned. If this path
     *         only contains one field then an empty path is returned. This will
     *         never be <code>null</code>.
     */
    public Path getParentPath() {
        if(getDepth() <= 1) return new Path()/*new/empty path by contract*/;
        return new Path(fieldPath.subList(0, (getDepth() - 1/*remove last*/)));
    }

    /**
     * @param  qualifier this cannot be <code>null</code>
     * @return the number of parent fields in this path that have the specified
     *         {@link FieldQualifier field qualifier}. This will never be negative.
     */
    public int getParentQualifierCount(final FieldQualifier fieldQualifier) {
        int count = 0/*none to start*/;
        final int parentLength = getDepth() - 1/*excluding the last field (i.e. only parents)*/;
        for(int i=0; i<parentLength; i++) {
            final IField parentField = fieldPath.get(i);
            if(parentField.getQualifier() == fieldQualifier)
                count++;
            /* else -- not the desired qualifier */
        }
        return count;
    }

    // ========================================================================
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return 31  + fieldPath.hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object object) {
        if(this == object) return true;
        if(object == null) return false;
        if(getClass() != object.getClass()) return false;
        final Path path = (Path)object;
        return fieldPath.equals(path.fieldPath); 
    }
}