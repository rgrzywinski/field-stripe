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

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.dyuproject.protostuff.parser.Proto;
import com.dyuproject.protostuff.parser.ProtoUtil;

/**
 * A {@link Proto.Loader Protobuf loader} for content stored in strings. It is
 * assumed that all strings are "UTF-8" encoded.
 *
 * @author rgrzywinski
 */
// NOTE:  a Proto.Loader is used by Proto#importProto() to import a Protobuf
//        definition given a path
public class StringProtoLoader implements Proto.Loader {
    /**
     * The default path (and 'filename') of a Protobuf definition if it is not
     * explicitly given one.
     * @see #parseProto(String)
     */
    public static final String DEFAULT_PATH = "/PROTO.proto";

    // ************************************************************************
    // TODO:  handle the nuisances of leading "/"'s, "..", ".", etc.

    // the map of path (including 'filename') to Protobuf definition string
    // NOTE:  null values (definitions) are not allowed. Any null's passed in
    //        (on construction) are turned into empty strings. This prevents 
    //        the need to disambiguate between the key existing with a null
    //        value and the key not existing.
    private final Map<String/*path*/, String/*Protobuf def*/> pathToProtobufDefMap = new HashMap<String, String>();

    // ========================================================================
    /**
     * Creates a string-based Protobuf loader where it is assumed that the 
     * path to the Protobuf definition is {@link #DEFAULT_PATH}.
     * 
     * @param  protoString the Protobuf definition as a string. This may be 
     *         <code>null</code> or blank (both of which imply that there is
     *         no definition to be loaded).
     * @see #StringProtoLoader(Map)
     */
    public StringProtoLoader(final String protoString) {
        pathToProtobufDefMap.put(DEFAULT_PATH, protoString);
    }

    /**
     * Creates a string-based Protobuf loader backed by the specified map of
     * path-to-string-based-Protobuf-definition.
     * 
     * @param  definitions the map of path (including 'filename') to string-based
     *         Protobuf definition. Any blank / <code>null</code> paths are
     *         explicitly excluded and any blank / <code>null</code> definitions
     *         are treated as blank.
     * @see #StringProtoLoader(String)
     */
    public StringProtoLoader(final Map<String/*path*/, String/*Protobuf def*/> definitions) {
        for(final Map.Entry<String, String> entry : definitions.entrySet()) {
            if(StringUtils.isBlank(entry.getKey()))
                continue/*explicitly exclude*/;
            /* else -- the key is not blank */
            pathToProtobufDefMap.put(entry.getKey(), ((entry.getValue() == null) ? "" : entry.getValue())/*by contract*/);
        }
    }

    // TODO:  extend this to have an empty constructor and provide #add(String, String)
    //        methods

    // ========================================================================
    /* (non-Javadoc)
     * @see com.dyuproject.protostuff.parser.Proto.Loader#load(java.lang.String, com.dyuproject.protostuff.parser.Proto)
     */
    // NOTE:  "importer" is the Protobuf definition that is requesting a Protobuf 
    //        definition (at the specified path) to be loaded
    @Override
    public Proto load(final String path, final Proto importer) 
            throws Exception {
        final String definition = pathToProtobufDefMap.get(path);
        if(definition == null)
            throw new IllegalStateException("Imported proto " + path + " not found.");
        /* else -- the definition is non-null (i.e. exists) */

        final Proto targetProto = new StringProto(path, this, importer);
            parseProto(definition, targetProto);
        return targetProto;
    }

    // ************************************************************************
    /**
     * A convenience method to parse the Protobuf definition that is specified
     * as a string. It is assumed that there are no dependencies (otherwise they
     * would be need to be specified via {@link #parse()}). The definition will
     * be given the implicit path of {@link #DEFAULT_PATH}.
     * 
     * @param  protoString the Protobuf as a string that is to be parsed. This 
     *         may be <code>null</code> or blank (both of which will result in
     *         an "empty" {@link Proto}).
     * @return a {@link Proto} loaded from the specified string. This will never
     *         be <code>null</code>.
     */
    public static Proto parseProto(final String protoString) {
        // NOTE:  approach follows from ProtoUtil#parseProto(File)
        final Proto targetProto = new StringProto(DEFAULT_PATH, new StringProtoLoader(protoString), null/*no importer*/);
            parseProto(protoString, targetProto);
        return targetProto;
    }

    /**
     * A convenience method to parse the Protobuf definition that is specified
     * as a string into the specified {@link Proto}.
     * 
     * @param  protoString the Protobuf as a string that is to be parsed. This 
     *         may be <code>null</code> or blank (both of which will result in
     *         an "empty" {@link Proto}). It is assumed that this string is 
     *         "UTF-8" encoded.
     * @param  targetProto the <code>Proto</code> that the contents are to be 
     *         parsed into. This cannot be <code>null</code>.
     */
    private static void parseProto(final String protoString, final Proto targetProto) {
        // NOTE:  approach follows from ProtoUtil#parseProto(File)
        try {
            ProtoUtil.loadFrom(new ByteArrayInputStream(protoString.getBytes("UTF-8")), targetProto);
        } catch(final Exception e) {
            throw new RuntimeException(e);
        }
    }
}