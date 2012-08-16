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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.agkn.field_stripe.exception.DeveloperException;
import net.agkn.field_stripe.exception.NoSuchObjectException;

import com.dyuproject.protostuff.parser.HasName;
import com.dyuproject.protostuff.parser.Message;
import com.dyuproject.protostuff.parser.Proto;

/**
 * A specialization of {@link Proto} to string-based Protobuf definitions.
 * The changes all focus on paths. A public {@link #lookupMessage(String) lookup}
 * method is provided for convenience.
 *
 * @author rgrzywinski
 */
public class StringProto extends Proto {
    private final String path;

    // ========================================================================
    /**
     * @param  path the filename and path of the Protobuf definition that this
     *         object represents. This cannot be <code>null</code> or blank.
     * @param  loader the <code>Loader</code> from which the contents of this
     *         Protobuf are loaded. This cannot be <code>null</code>.
     * @param  importer
     * @see #getSourcePath()
     */
    public StringProto(final String path, final Loader loader, final Proto importer) {
        super((File)null, loader, importer);
        this.path = path;
    }

    // ------------------------------------------------------------------------
    /* (non-Javadoc)
     * @see com.dyuproject.protostuff.parser.Proto#getSourcePath()
     */
    @Override
    public String getSourcePath() { return path; }
}