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

import static net.agkn.field_stripe.record.PathTest.assertPath;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import net.agkn.field_stripe.record.FieldQualifier;
import net.agkn.field_stripe.record.ICompositeType;
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.record.IFieldType;
import net.agkn.field_stripe.record.PrimitiveType;

import org.testng.annotations.Test;

import com.dyuproject.protostuff.parser.Proto;

/**
 * Unit tests for {@link ProtobufFieldTypeFactory}.
 *
 * @author rgrzywinski
 */
public class ProtobufFieldTypeFactoryTest {
    /**
     * Validates the construction of a {@link IFieldType field type} from a
     * flat (non-nested) Protobuf definition.
     */
    @Test
    public void flatSimpleDefinitionTest() throws Exception {
        final String protobufText =
            // NOTE:  Protostuff requires a package in the .proto
            "package package_name;\n" +
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
            // NOTE:  not currently supported
            //"    repeated bytes    bytes_field = 15;\n" +
            "}\n";
        final Proto protobufDefinition = StringProtoLoader.parseProto(protobufText);
        final List<Proto> protobufDefinitions = Collections.singletonList(protobufDefinition);

        // ....................................................................
        final ProtobufFieldTypeFactory fieldTypeFactory = new ProtobufFieldTypeFactory();
        final IFieldType fieldType = fieldTypeFactory.createFieldType(protobufDefinitions, "Message");
        assertNotNull(fieldType, "Field type from Protobuf definition");

        assertTrue(fieldType.isComposite(), "Composite field type");
        final ICompositeType compositeType = (ICompositeType)fieldType;
        assertEquals(compositeType.getName(), "package_name.Message", "Message name");
        assertEquals(compositeType.getFields().size(), 14, "Number of fields");
        assertPrimitiveField(compositeType.getField(0), 1/*index*/, FieldQualifier.ZERO_OR_ONE, PrimitiveType.DOUBLE, "double_field");
        assertPath(compositeType.getField(0).getPath(), compositeType.getField(0));
        assertPrimitiveField(compositeType.getField(1), 2/*index*/, FieldQualifier.ONE, PrimitiveType.FLOAT, "float_field");
        assertPath(compositeType.getField(1).getPath(), compositeType.getField(1));
        assertPrimitiveField(compositeType.getField(2), 3/*index*/, FieldQualifier.ZERO_OR_MORE, PrimitiveType.INT, "int32_field");
        assertPath(compositeType.getField(2).getPath(), compositeType.getField(2));
        assertPrimitiveField(compositeType.getField(3), 4/*index*/, FieldQualifier.ZERO_OR_ONE, PrimitiveType.LONG, "int64_field");
        assertPath(compositeType.getField(3).getPath(), compositeType.getField(3));
        assertPrimitiveField(compositeType.getField(4), 5/*index*/, FieldQualifier.ONE, PrimitiveType.INT, "uint32_field");
        assertPath(compositeType.getField(4).getPath(), compositeType.getField(4));
        assertPrimitiveField(compositeType.getField(5), 6/*index*/, FieldQualifier.ZERO_OR_MORE, PrimitiveType.LONG, "uint64_field");
        assertPath(compositeType.getField(5).getPath(), compositeType.getField(5));
        assertPrimitiveField(compositeType.getField(6), 7/*index*/, FieldQualifier.ZERO_OR_ONE, PrimitiveType.INT, "sint32_field");
        assertPath(compositeType.getField(6).getPath(), compositeType.getField(6));
        assertPrimitiveField(compositeType.getField(7), 8/*index*/, FieldQualifier.ONE, PrimitiveType.LONG, "sint64_field");
        assertPath(compositeType.getField(7).getPath(), compositeType.getField(7));
        assertPrimitiveField(compositeType.getField(8), 9/*index*/, FieldQualifier.ZERO_OR_MORE, PrimitiveType.INT, "fixed32_field");
        assertPath(compositeType.getField(8).getPath(), compositeType.getField(8));
        assertPrimitiveField(compositeType.getField(9), 10/*index*/, FieldQualifier.ZERO_OR_ONE, PrimitiveType.LONG, "fixed64_field");
        assertPath(compositeType.getField(9).getPath(), compositeType.getField(9));
        assertPrimitiveField(compositeType.getField(10), 11/*index*/, FieldQualifier.ONE, PrimitiveType.INT, "sfixed32_field");
        assertPath(compositeType.getField(10).getPath(), compositeType.getField(10));
        assertPrimitiveField(compositeType.getField(11), 12/*index*/, FieldQualifier.ZERO_OR_MORE, PrimitiveType.LONG, "sfixed64_field");
        assertPath(compositeType.getField(11).getPath(), compositeType.getField(11));
        assertPrimitiveField(compositeType.getField(12), 13/*index*/, FieldQualifier.ZERO_OR_ONE, PrimitiveType.BOOLEAN, "bool_field");
        assertPath(compositeType.getField(12).getPath(), compositeType.getField(12));
        assertPrimitiveField(compositeType.getField(13), 14/*index*/, FieldQualifier.ONE, PrimitiveType.STRING, "string_field");
        assertPath(compositeType.getField(13).getPath(), compositeType.getField(13));
    }

    /**
     * Validates the construction of a {@link IFieldType field type} from a
     * nested {@link ProtobufDefinition} that is defined in a single file.
     */
    @Test
    public void nestedDefinitionOneFileTest() throws Exception {
        final String protobufText =
            "package package_name;\n" +
            "message Message {\n" + 
            "    optional double   double_field = 1;\n" +
            "    required float    float_field = 2;\n" +
            "    repeated int32    int32_field = 3;\n" +
            "    message NestedMessage {\n" +
            "        optional int64    int64_field = 1;\n" +
            "        required uint32   uint32_field = 2;\n" +
            "        repeated uint64   uint64_field = 3;\n" +
            "        message NestedNestedMessage {\n" +
            "            optional sint32   sint32_field = 1;\n" +
            "            required sint64   sint64_field = 2;\n" +
            "            repeated fixed32  fixed32_field = 3;\n" +
            "        }\n" +
            "        required NestedNestedMessage nestedNestedMessage = 4;\n" +
            "    }\n" +
            "    required NestedMessage required_nestedMessage = 4;\n" +
            "    optional NestedMessage optional_nestedMessage = 5;\n"/*2nd field with same type to ensure tree cloning works*/ +
            "}\n";
        final Proto protobufDefinition = StringProtoLoader.parseProto(protobufText);
        final List<Proto> protobufDefinitions = Collections.singletonList(protobufDefinition);

        // ....................................................................
        // create a type from the outer Message
        { // locally scoped for sanity
            final ProtobufFieldTypeFactory fieldTypeFactory = new ProtobufFieldTypeFactory();
            final IFieldType fieldType = fieldTypeFactory.createFieldType(protobufDefinitions, "package_name.Message");
            assertNotNull(fieldType, "Field type from Protobuf definition");

            assertTrue(fieldType.isComposite(), "Composite field type");
            final ICompositeType compositeType = (ICompositeType)fieldType;
            assertEquals(compositeType.getName(), "package_name.Message", "Message name");
            assertEquals(compositeType.getFields().size(), 5, "Number of fields");
            assertPrimitiveField(compositeType.getField(0), 1/*index*/, FieldQualifier.ZERO_OR_ONE, PrimitiveType.DOUBLE, "double_field");
            assertPath(compositeType.getField(0).getPath(), compositeType.getField(0));
            assertPrimitiveField(compositeType.getField(1), 2/*index*/, FieldQualifier.ONE, PrimitiveType.FLOAT, "float_field");
            assertPath(compositeType.getField(1).getPath(), compositeType.getField(1));
            assertPrimitiveField(compositeType.getField(2), 3/*index*/, FieldQualifier.ZERO_OR_MORE, PrimitiveType.INT, "int32_field");
            assertPath(compositeType.getField(2).getPath(), compositeType.getField(2));
            assertCompositeField(compositeType.getField(3), 4/*index*/, FieldQualifier.ONE, "package_name.Message.NestedMessage", "required_nestedMessage");
            assertPath(compositeType.getField(3).getPath(), compositeType.getField(3));
            assertCompositeField(compositeType.getField(4), 5/*index*/, FieldQualifier.ZERO_OR_ONE, "package_name.Message.NestedMessage", "optional_nestedMessage");
            assertPath(compositeType.getField(4).getPath(), compositeType.getField(4));

              // NOTE:  if the deep clone of a field and its type was not 
              //        successful on build then this would have the wrong path
              //        (since the type hierarchy would have been shared between
              //        two fields)
              final ICompositeType requiredNestedCompositeType = (ICompositeType)compositeType.getFields().get(3/*required_nestedMessage*/).getType();
              assertEquals(requiredNestedCompositeType.getName(), "package_name.Message.NestedMessage", "Message name");
              assertEquals(requiredNestedCompositeType.getFields().size(), 4, "Number of fields");
              assertPrimitiveField(compositeType.getField(3/*required_nestedMessage*/, 0), 1/*index*/, FieldQualifier.ZERO_OR_ONE, PrimitiveType.LONG, "int64_field");
              assertPath(compositeType.getField(3/*required_nestedMessage*/, 0).getPath(), compositeType.getField(3/*required_nestedMessage*/), compositeType.getField(3/*required_nestedMessage*/, 0));
              assertPrimitiveField(compositeType.getField(3/*required_nestedMessage*/, 1), 2/*index*/, FieldQualifier.ONE, PrimitiveType.INT, "uint32_field");
              assertPath(compositeType.getField(3/*required_nestedMessage*/, 1).getPath(), compositeType.getField(3/*required_nestedMessage*/), compositeType.getField(3/*required_nestedMessage*/, 1));
              assertPrimitiveField(compositeType.getField(3/*required_nestedMessage*/, 2), 3/*index*/, FieldQualifier.ZERO_OR_MORE, PrimitiveType.LONG, "uint64_field");
              assertPath(compositeType.getField(3/*required_nestedMessage*/, 2).getPath(), compositeType.getField(3/*required_nestedMessage*/), compositeType.getField(3/*required_nestedMessage*/, 2));
              assertCompositeField(compositeType.getField(3/*required_nestedMessage*/, 3), 4/*index*/, FieldQualifier.ONE, "package_name.Message.NestedMessage.NestedNestedMessage", "nestedNestedMessage");
              assertPath(compositeType.getField(3/*required_nestedMessage*/, 3).getPath(), compositeType.getField(3/*required_nestedMessage*/), compositeType.getField(3/*required_nestedMessage*/, 3));

                final ICompositeType requiredNestedNestedCompositeType = (ICompositeType)requiredNestedCompositeType.getFields().get(3/*nestedNestedMessage*/).getType();
                assertEquals(requiredNestedNestedCompositeType.getName(), "package_name.Message.NestedMessage.NestedNestedMessage", "Message name");
                assertEquals(requiredNestedNestedCompositeType.getFields().size(), 3, "Number of fields");
                assertPrimitiveField(compositeType.getField(3/*required_nestedMessage*/, 3/*nestedNestedMessage*/, 0), 1/*index*/, FieldQualifier.ZERO_OR_ONE, PrimitiveType.INT, "sint32_field");
                assertPath(compositeType.getField(3/*required_nestedMessage*/, 3/*nestedNestedMessage*/, 0).getPath(), compositeType.getField(3/*required_nestedMessage*/), compositeType.getField(3/*required_nestedMessage*/, 3/*nestedNestedMessage*/), compositeType.getField(3/*required_nestedMessage*/, 3/*nestedNestedMessage*/, 0));
                assertPrimitiveField(compositeType.getField(3/*required_nestedMessage*/, 3/*nestedNestedMessage*/, 1), 2/*index*/, FieldQualifier.ONE, PrimitiveType.LONG, "sint64_field");
                assertPath(compositeType.getField(3/*required_nestedMessage*/, 3/*nestedNestedMessage*/, 1).getPath(), compositeType.getField(3/*required_nestedMessage*/), compositeType.getField(3/*required_nestedMessage*/, 3/*nestedNestedMessage*/), compositeType.getField(3/*required_nestedMessage*/, 3/*nestedNestedMessage*/, 1));
                assertPrimitiveField(compositeType.getField(3/*required_nestedMessage*/, 3/*nestedNestedMessage*/, 2), 3/*index*/, FieldQualifier.ZERO_OR_MORE, PrimitiveType.INT, "fixed32_field");
                assertPath(compositeType.getField(3/*required_nestedMessage*/, 3/*nestedNestedMessage*/, 2).getPath(), compositeType.getField(3/*required_nestedMessage*/), compositeType.getField(3/*required_nestedMessage*/, 3/*nestedNestedMessage*/), compositeType.getField(3/*required_nestedMessage*/, 3/*nestedNestedMessage*/, 2));

              final ICompositeType optionalNestedCompositeType = (ICompositeType)compositeType.getFields().get(4/*optional_nestedMessage*/).getType();
              assertEquals(optionalNestedCompositeType.getName(), "package_name.Message.NestedMessage", "Message name");
              assertEquals(optionalNestedCompositeType.getFields().size(), 4, "Number of fields");
              assertPrimitiveField(compositeType.getField(4/*optional_nestedMessage*/, 0), 1/*index*/, FieldQualifier.ZERO_OR_ONE, PrimitiveType.LONG, "int64_field");
              assertPath(compositeType.getField(4/*optional_nestedMessage*/, 0).getPath(), compositeType.getField(4/*optional_nestedMessage*/), compositeType.getField(4/*optional_nestedMessage*/, 0));
              assertPrimitiveField(compositeType.getField(4/*optional_nestedMessage*/, 1), 2/*index*/, FieldQualifier.ONE, PrimitiveType.INT, "uint32_field");
              assertPath(compositeType.getField(4/*optional_nestedMessage*/, 1).getPath(), compositeType.getField(4/*optional_nestedMessage*/), compositeType.getField(4/*optional_nestedMessage*/, 1));
              assertPrimitiveField(compositeType.getField(4/*optional_nestedMessage*/, 2), 3/*index*/, FieldQualifier.ZERO_OR_MORE, PrimitiveType.LONG, "uint64_field");
              assertPath(compositeType.getField(4/*optional_nestedMessage*/, 2).getPath(), compositeType.getField(4/*optional_nestedMessage*/), compositeType.getField(4/*optional_nestedMessage*/, 2));
              assertCompositeField(compositeType.getField(4/*optional_nestedMessage*/, 3), 4/*index*/, FieldQualifier.ONE, "package_name.Message.NestedMessage.NestedNestedMessage", "nestedNestedMessage");
              assertPath(compositeType.getField(4/*optional_nestedMessage*/, 3).getPath(), compositeType.getField(4/*optional_nestedMessage*/), compositeType.getField(4/*optional_nestedMessage*/, 3));

                final ICompositeType optionalNestedNestedCompositeType = (ICompositeType)optionalNestedCompositeType.getFields().get(3/*nestedNestedMessage*/).getType();
                assertEquals(optionalNestedNestedCompositeType.getName(), "package_name.Message.NestedMessage.NestedNestedMessage", "Message name");
                assertEquals(optionalNestedNestedCompositeType.getFields().size(), 3, "Number of fields");
                assertPrimitiveField(compositeType.getField(4/*optional_nestedMessage*/, 3/*nestedNestedMessage*/, 0), 1/*index*/, FieldQualifier.ZERO_OR_ONE, PrimitiveType.INT, "sint32_field");
                assertPath(compositeType.getField(4/*optional_nestedMessage*/, 3/*nestedNestedMessage*/, 0).getPath(), compositeType.getField(4/*optional_nestedMessage*/), compositeType.getField(4/*optional_nestedMessage*/, 3/*nestedNestedMessage*/), compositeType.getField(4/*optional_nestedMessage*/, 3/*nestedNestedMessage*/, 0));
                assertPrimitiveField(compositeType.getField(4/*optional_nestedMessage*/, 3/*nestedNestedMessage*/, 1), 2/*index*/, FieldQualifier.ONE, PrimitiveType.LONG, "sint64_field");
                assertPath(compositeType.getField(4/*optional_nestedMessage*/, 3/*nestedNestedMessage*/, 1).getPath(), compositeType.getField(4/*optional_nestedMessage*/), compositeType.getField(4/*optional_nestedMessage*/, 3/*nestedNestedMessage*/), compositeType.getField(4/*optional_nestedMessage*/, 3/*nestedNestedMessage*/, 1));
                assertPrimitiveField(compositeType.getField(4/*optional_nestedMessage*/, 3/*nestedNestedMessage*/, 2), 3/*index*/, FieldQualifier.ZERO_OR_MORE, PrimitiveType.INT, "fixed32_field");
                assertPath(compositeType.getField(4/*optional_nestedMessage*/, 3/*nestedNestedMessage*/, 2).getPath(), compositeType.getField(4/*optional_nestedMessage*/), compositeType.getField(4/*optional_nestedMessage*/, 3/*nestedNestedMessage*/), compositeType.getField(4/*optional_nestedMessage*/, 3/*nestedNestedMessage*/, 2));
        }

        // create a type from the nested NestedMessage
        { // locally scoped for sanity
            final ProtobufFieldTypeFactory fieldTypeFactory = new ProtobufFieldTypeFactory();
            final IFieldType fieldType = fieldTypeFactory.createFieldType(protobufDefinitions, "package_name.Message.NestedMessage");
            assertNotNull(fieldType, "Field type from Protobuf definition");

            assertTrue(fieldType.isComposite(), "Composite field type");
            final ICompositeType compositeType = (ICompositeType)fieldType;
            assertEquals(compositeType.getName(), "package_name.Message.NestedMessage", "Message name");
            assertEquals(compositeType.getFields().size(), 4, "Number of fields");
            assertPrimitiveField(compositeType.getField(0), 1/*index*/, FieldQualifier.ZERO_OR_ONE, PrimitiveType.LONG, "int64_field");
            assertPath(compositeType.getField(0).getPath(), compositeType.getField(0));
            assertPrimitiveField(compositeType.getField(1), 2/*index*/, FieldQualifier.ONE, PrimitiveType.INT, "uint32_field");
            assertPath(compositeType.getField(1).getPath(), compositeType.getField(1));
            assertPrimitiveField(compositeType.getField(2), 3/*index*/, FieldQualifier.ZERO_OR_MORE, PrimitiveType.LONG, "uint64_field");
            assertPath(compositeType.getField(2).getPath(), compositeType.getField(2));
            assertCompositeField(compositeType.getField(3), 4/*index*/, FieldQualifier.ONE, "package_name.Message.NestedMessage.NestedNestedMessage", "nestedNestedMessage");
            assertPath(compositeType.getField(3).getPath(), compositeType.getField(3));

            final ICompositeType nestedCompositeType = (ICompositeType)compositeType.getFields().get(3/*nestedNestedMessage*/).getType();
            assertEquals(nestedCompositeType.getName(), "package_name.Message.NestedMessage.NestedNestedMessage", "Message name");
            assertEquals(nestedCompositeType.getFields().size(), 3, "Number of fields");
            assertPrimitiveField(compositeType.getField(3/*nestedeNestedMessage*/, 0), 1/*index*/, FieldQualifier.ZERO_OR_ONE, PrimitiveType.INT, "sint32_field");
            assertPath(compositeType.getField(3/*nestedeNestedMessage*/, 0).getPath(), compositeType.getField(3/*nestedeNestedMessage*/), compositeType.getField(3/*nestedeNestedMessage*/, 0));
            assertPrimitiveField(compositeType.getField(3/*nestedeNestedMessage*/, 1), 2/*index*/, FieldQualifier.ONE, PrimitiveType.LONG, "sint64_field");
            assertPath(compositeType.getField(3/*nestedeNestedMessage*/, 1).getPath(), compositeType.getField(3/*nestedeNestedMessage*/), compositeType.getField(3/*nestedeNestedMessage*/, 1));
            assertPrimitiveField(compositeType.getField(3/*nestedeNestedMessage*/, 2), 3/*index*/, FieldQualifier.ZERO_OR_MORE, PrimitiveType.INT, "fixed32_field");
            assertPath(compositeType.getField(3/*nestedeNestedMessage*/, 2).getPath(), compositeType.getField(3/*nestedeNestedMessage*/), compositeType.getField(3/*nestedeNestedMessage*/, 2));
        }
        
        // create a type from the nested NestedNestedMessage
        { // locally scoped for sanity
            final ProtobufFieldTypeFactory fieldTypeFactory = new ProtobufFieldTypeFactory();
            final IFieldType fieldType = fieldTypeFactory.createFieldType(protobufDefinitions, "package_name.Message.NestedMessage.NestedNestedMessage");
            assertNotNull(fieldType, "Field type from Protobuf definition");

            assertTrue(fieldType.isComposite(), "Composite field type");
            final ICompositeType compositeType = (ICompositeType)fieldType;
            assertEquals(compositeType.getName(), "package_name.Message.NestedMessage.NestedNestedMessage", "Message name");
            assertEquals(compositeType.getFields().size(), 3, "Number of fields");
            assertPrimitiveField(compositeType.getField(0), 1/*index*/, FieldQualifier.ZERO_OR_ONE, PrimitiveType.INT, "sint32_field");
            assertPath(compositeType.getField(0).getPath(), compositeType.getField(0));
            assertPrimitiveField(compositeType.getField(1), 2/*index*/, FieldQualifier.ONE, PrimitiveType.LONG, "sint64_field");
            assertPath(compositeType.getField(1).getPath(), compositeType.getField(1));
            assertPrimitiveField(compositeType.getField(2), 3/*index*/, FieldQualifier.ZERO_OR_MORE, PrimitiveType.INT, "fixed32_field");
            assertPath(compositeType.getField(2).getPath(), compositeType.getField(2));
        }
    }

    // TODO:  a test based on messages defined in two different Protobuf files

    // ************************************************************************
    /**
     * Asserts that the specified non-<code>null</code> primitive-based field
     * has the specified values.
     */
    public static void assertPrimitiveField(final IField field, final int index, final FieldQualifier qualifier, final IFieldType type, final String name) {
        assertEquals(field.getIndex(), index, "Primitive field index");
        assertEquals(field.getQualifier(), qualifier, "Primitive field qualifier");
        assertTrue((field.getType() instanceof PrimitiveType), "Primitive field type");
        assertEquals(field.getType(), type, "Primitive field type");
        assertEquals(field.getName(), name, "Primitive field name");
    }

    /**
     * Asserts that the specified non-<code>null</code> component-based field
     * has the specified values. The type is only validated by its name. Futher
     * assertions must be performed externally.
     */
    public static void assertCompositeField(final IField field, final int index, final FieldQualifier qualifier, final String typeName, final String name) {
        assertEquals(field.getIndex(), index, "Composite field index");
        assertEquals(field.getQualifier(), qualifier, "Composite field qualifier");
        assertTrue((field.getType() instanceof ICompositeType), "Composite field type");
        assertEquals(((ICompositeType)field.getType()).getName(), typeName, "Composite field type name");
        assertEquals(field.getName(), name, "Composite field name");
    }
}