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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;

import org.testng.annotations.Test;

import com.dyuproject.protostuff.parser.Field;
import com.dyuproject.protostuff.parser.Field.Modifier;
import com.dyuproject.protostuff.parser.Message;
import com.dyuproject.protostuff.parser.Proto;

/**
 * Unit tests for the local integration of the <a href="http://code.google.com/p/protostuff/">protostuff</a>
 * parser. These are sanity tests to ensure that {@link Proto} and its children
 * are well understood.
 *
 * @author rgrzywinski
 */
public class ProtoTest {
    /**
     * Parsers a flat definition to ensure that the output of the protostuff
     * parser (i.e. the {@link Proto}) is understood.
     */
    @Test
    public void flatDefinitionParserTest() {
        final String protobufText =
            "package test;\n" + 
            "message Message {\n" + 
            "    optional double   double_field = 1;\n" +
            "    required float    float_field = 2;\n" +
            "    repeated int32    int32_field = 3;\n" +
            "    optional int64    int64_field = 4;\n" +
            "    required uint32   uint32_field = 5;\n" +
            "    repeated uint64   uint64_field = 6;\n" +
            "    optional sint32   sint32_field = 7;\n" +
            "    required sint64   sint64_field = 8;\n" +
            "    repeated fixed32  fixed32_field = 9;\n" +
            "    optional fixed64  fixed64_field = 10;\n" +
            "    required sfixed32 sfixed32_field = 11;\n" +
            "    repeated sfixed64 sfixed64_field = 12;\n" +
            "    optional bool     bool_field = 13;\n" +
            "    required string   string_field = 14;\n" +
            // NOTE:  not currently supported throughout
            //"    repeated bytes    bytes_field = 15;\n" +
            "}\n";
        final Proto proto = StringProtoLoader.parseProto(protobufText);

        assertEquals(proto.getSourcePath(), StringProtoLoader.DEFAULT_PATH, "Source path");
        assertEquals(proto.getMessages().size(), 1, "Number of messages");
        assertNotNull(proto.getMessage("Message"), "Message 'Message'");
        final Message message = proto.getMessage("Message");
            assertEquals(message.getFieldCount(), 14, "Field count");
            assertPrimitiveField(message.getField("double_field"), "double_field", Modifier.OPTIONAL, Field.Double.class, 1/*index*/);
            assertPrimitiveField(message.getField("float_field"), "float_field", Modifier.REQUIRED, Field.Float.class, 2/*index*/);
            assertPrimitiveField(message.getField("int32_field"), "int32_field", Modifier.REPEATED, Field.Int32.class, 3/*index*/);
            assertPrimitiveField(message.getField("int64_field"), "int64_field", Modifier.OPTIONAL, Field.Int64.class, 4/*index*/);
            assertPrimitiveField(message.getField("uint32_field"), "uint32_field", Modifier.REQUIRED, Field.UInt32.class, 5/*index*/);
            assertPrimitiveField(message.getField("uint64_field"), "uint64_field", Modifier.REPEATED, Field.UInt64.class, 6/*index*/);
            assertPrimitiveField(message.getField("sint32_field"), "sint32_field", Modifier.OPTIONAL, Field.SInt32.class, 7/*index*/);
            assertPrimitiveField(message.getField("sint64_field"), "sint64_field", Modifier.REQUIRED, Field.SInt64.class, 8/*index*/);
            assertPrimitiveField(message.getField("fixed32_field"), "fixed32_field", Modifier.REPEATED, Field.Fixed32.class, 9/*index*/);
            assertPrimitiveField(message.getField("fixed64_field"), "fixed64_field", Modifier.OPTIONAL, Field.Fixed64.class, 10/*index*/);
            assertPrimitiveField(message.getField("sfixed32_field"), "sfixed32_field", Modifier.REQUIRED, Field.SFixed32.class, 11/*index*/);
            assertPrimitiveField(message.getField("sfixed64_field"), "sfixed64_field", Modifier.REPEATED, Field.SFixed64.class, 12/*index*/);
            assertPrimitiveField(message.getField("bool_field"), "bool_field", Modifier.OPTIONAL, Field.Bool.class, 13/*index*/);
            assertPrimitiveField(message.getField("string_field"), "string_field", Modifier.REQUIRED, Field.String.class, 14/*index*/);
    }

    // TODO:  add nested and multiple Message cases

    // ========================================================================
    /**
     * A sanity test for performing a {@link StringProto#lookupMessage(String) lookup}.
     */
    @Test
    public void lookupTest() throws Exception {
        final String protobufText =
            "package test;\n" + 
            "message Message {\n" + 
            "    optional double   double_field = 1;\n" +
            "}\n";
        @SuppressWarnings("serial")
        final StringProtoLoader loader = new StringProtoLoader(new HashMap<String, String>() {{ put("Message.proto", protobufText); }});
        final StringProto proto = (StringProto)loader.load("Message.proto", null/*no importer*/);
        assertNotNull(LookupUtil.lookupMessage(proto, "Message"), "Lookup message");

        // TODO:  extend to nested messages (though ProtobufFieldTypeFactoryTest
        //        covers these at a higher level)
    }

    // ************************************************************************
    /**
     * Asserts that the specified {@link Field} exists and has the specified 
     * properties.
     */
    public static void assertPrimitiveField(final Field<?> field, final String name, 
                                            final Modifier modifier, final Class<?> type,
                                            final int index) {
        assertNotNull(field, "Field");
        assertEquals(field.getName(), name, "Field name");
        assertEquals(field.getModifier(), modifier, "Field modifier");
        assertTrue(type.isAssignableFrom(field.getClass()), "Field type");
        assertEquals(field.getNumber(), index, "Field index");
    }
}