package net.agkn.field_stripe.decode;

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

import static org.powermock.reflect.Whitebox.getInternalState;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.List;

import net.agkn.field_stripe.encode.SchemaBuilder;
import net.agkn.field_stripe.exception.DeveloperException;
import net.agkn.field_stripe.record.ICompositeType;
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.stripe.IFieldStripeReader;
import net.agkn.field_stripe.stripe.IFieldStripeReaderFactory;
import net.agkn.field_stripe.stripe.Instruction;

import org.testng.annotations.Test;

/**
 * Unit tests for {@link FieldStripeDecoderFactory} -- specifically for the
 * process of {@link FieldStripeDecoderFactory#createDecoderTree(net.agkn.field_stripe.record.IFieldType) creating decoder trees}
 * from a {@link ICompositeType}.
 *
 * @author rgrzywinski
 */
public class FieldStripeDecoderFactoryTest {
    /**
     * Tests creating an decoder tree from a flat structure with no parents 
     * (the simplest case). 
     * 
     * @see NoRepeatedParentStripeDecoderTest#flatSchemaTest()
     */
    @Test
    public void flatSchemaTest() throws Exception {
        final String protobufText =
            "package package_name;\n"/*required by Protostuff*/ +
            "message Message {\n" + 
            "    optional double   double_field = 1;\n" +
            "    required float    float_field = 2;\n" +
            "    repeated int32    int32_field = 3;\n" +
            "    optional int64    int64_field = 4;\n" +
            "    required bool     bool_field = 5;\n" +
            "    repeated string   string_field = 6;\n" +
            "}\n";
        final SchemaBuilder schemaBuilder = new SchemaBuilder();
        final ICompositeType schema = schemaBuilder.createSchema(protobufText, "Message");

        // ....................................................................
        final MockFieldStriptReaderFactory fieldStripeReaderFactory = new MockFieldStriptReaderFactory();
        final FieldStripeDecoderFactory decoderFactory = new FieldStripeDecoderFactory(fieldStripeReaderFactory);
        final RootFieldStripeDecoder root = decoderFactory.createDecoderTree(schema);
        assertNotNull(root, "Root field stripe decoder");

        // ensure that the tree of decoders matches what is expected
        { // locally scoped for sanity
            final List<IFieldStripeDecoder> childDecoders = getInternalState(root, "childDecoders");
            assertEquals(childDecoders.size(), 6, "Child fields / decoders");
            assertTrue(childDecoders.get(0/*double_field*/) instanceof LeafFieldStripeDecoder, "'double_field' is a primitive");
            assertTrue(childDecoders.get(1/*float_field*/) instanceof LeafFieldStripeDecoder, "'float_field' is a primitive");
            assertTrue(childDecoders.get(2/*int32_field*/) instanceof LeafFieldStripeDecoder, "'int32_field' is a primitive");
            assertTrue(childDecoders.get(3/*int64_field*/) instanceof LeafFieldStripeDecoder, "'int64_field' is a primitive");
            assertTrue(childDecoders.get(4/*bool_field*/) instanceof LeafFieldStripeDecoder, "'bool_field' is a primitive");
            assertTrue(childDecoders.get(5/*string_field*/) instanceof LeafFieldStripeDecoder, "'string_field' is a primitive");
        }
    }    

    /**
     * Tests creating an decoder tree a nested structure that is one level deep 
     * (with no repeated parents).
     * 
     * @see NoRepeatedParentStripeDecoderTest#oneLevelNestedSchemaTest()
     */
    @Test
    public void oneLevelNestedSchemaTest() throws Exception {
        final String protobufText =
            "package package_name;\n"/*required by Protostuff*/ +
            "message Message {\n" + 
            "    optional double   double_field = 1;\n" +
            "    repeated int64    int64_field = 2;\n" +
            "    message NestedMessage {\n" +
            "        optional float    float_field = 1;\n" +
            "        required int32    int32_field = 2;\n" +
            "        repeated bool     bool_field = 3;\n" +
            "    }\n" +
            "    optional NestedMessage nested_message_optional_field = 3;\n" +
            "    required NestedMessage nested_message_required_field = 4;\n" +
            "    repeated string   string_field = 5;\n" +
            "}\n";
        final SchemaBuilder schemaBuilder = new SchemaBuilder();
        final ICompositeType schema = schemaBuilder.createSchema(protobufText, "Message");

        // ....................................................................
        final MockFieldStriptReaderFactory fieldStripeReaderFactory = new MockFieldStriptReaderFactory();
        final FieldStripeDecoderFactory decoderFactory = new FieldStripeDecoderFactory(fieldStripeReaderFactory);
        final RootFieldStripeDecoder root = decoderFactory.createDecoderTree(schema);
        assertNotNull(root, "Root field stripe decoder");

        // ensure that the tree of decoders matches what is expected
        { // locally scoped for sanity
            final List<IFieldStripeDecoder> childDecoders = getInternalState(root, "childDecoders");
            assertEquals(childDecoders.size(), 5, "Child fields / decoders");
            assertTrue(childDecoders.get(0/*double_field*/) instanceof LeafFieldStripeDecoder, "'double_field' is a primitive");
            assertTrue(childDecoders.get(1/*int64_field*/) instanceof LeafFieldStripeDecoder, "'int64_field' is a primitive");
            assertTrue(childDecoders.get(2/*nested_message_optional_field*/) instanceof NodeFieldStripeDecoder, "'nested_message_optional_field' is a composite");
            { // locally scoped for sanity
                final NodeFieldStripeDecoder nestedDecoder = (NodeFieldStripeDecoder)childDecoders.get(2/*nested_message_optional_field*/);
                final List<IFieldStripeDecoder> nestedDecoders = getInternalState(nestedDecoder, "childDecoders");
                assertEquals(nestedDecoders.size(), 3, "Nested fields / decoders");
                assertTrue(nestedDecoders.get(0/*float_field*/) instanceof LeafFieldStripeDecoder, "'float_field' is a primitive");
                assertTrue(nestedDecoders.get(1/*int32_field*/) instanceof LeafFieldStripeDecoder, "'int32_field' is a primitive");
                assertTrue(nestedDecoders.get(2/*bool_field*/) instanceof LeafFieldStripeDecoder, "'bool_field' is a primitive");
            }
            assertTrue(childDecoders.get(3/*nested_message_required_field*/) instanceof NodeFieldStripeDecoder, "'nested_message_required_field' is a composite");
            { // locally scoped for sanity
                final NodeFieldStripeDecoder nestedDecoder = (NodeFieldStripeDecoder)childDecoders.get(3/*nested_message_required_field*/);
                final List<IFieldStripeDecoder> nestedDecoders = getInternalState(nestedDecoder, "childDecoders");
                assertEquals(nestedDecoders.size(), 3, "Nested fields / decoders");
                assertTrue(nestedDecoders.get(0/*float_field*/) instanceof LeafFieldStripeDecoder, "'float_field' is a primitive");
                assertTrue(nestedDecoders.get(1/*int32_field*/) instanceof LeafFieldStripeDecoder, "'int32_field' is a primitive");
                assertTrue(nestedDecoders.get(2/*bool_field*/) instanceof LeafFieldStripeDecoder, "'bool_field' is a primitive");
            }
            assertTrue(childDecoders.get(4/*string_field*/) instanceof LeafFieldStripeDecoder, "'string_field' is a primitive");
        }
    }

    /**
     * Tests creating an decoder tree from a nested structure that is one level 
     * deep (with repeated parents).
     * 
     * @see RepeatedParentStripeDecoderTest#oneLevelNestedSchemaTest()
     */
    @Test
    public void oneLevelNestedSchemaRepeatedParentTest() throws Exception {
        final String protobufText =
            "package package_name;\n"/*required by Protostuff*/ +
            "message Message {\n" + 
            "    optional double   double_field = 1;\n" +
            "    repeated int64    int64_field = 2;\n" +
            "    message NestedMessage {\n" +
            "        optional float    float_field = 1;\n" +
            "        required int32    int32_field = 2;\n" +
            "        repeated bool     bool_field = 3;\n" +
            "    }\n" +
            "    repeated NestedMessage nested_message_repeated_field = 3;\n" +
            "    repeated string   string_field = 4;\n" +
            "}\n";
        final SchemaBuilder schemaBuilder = new SchemaBuilder();
        final ICompositeType schema = schemaBuilder.createSchema(protobufText, "Message");

        // ....................................................................
        final MockFieldStriptReaderFactory fieldStripeReaderFactory = new MockFieldStriptReaderFactory();
        final FieldStripeDecoderFactory decoderFactory = new FieldStripeDecoderFactory(fieldStripeReaderFactory);
        final RootFieldStripeDecoder root = decoderFactory.createDecoderTree(schema);
        assertNotNull(root, "Root field stripe decoder");

        // ensure that the tree of decoders matches what is expected
        { // locally scoped for sanity
            final List<IFieldStripeDecoder> childDecoders = getInternalState(root, "childDecoders");
            assertEquals(childDecoders.size(), 4, "Child fields / decoders");
            assertTrue(childDecoders.get(0/*double_field*/) instanceof LeafFieldStripeDecoder, "'double_field' is a primitive");
            assertTrue(childDecoders.get(1/*int64_field*/) instanceof LeafFieldStripeDecoder, "'int64_field' is a primitive");
            assertTrue(childDecoders.get(2/*nested_message_repeated_field*/) instanceof NodeFieldStripeDecoder, "'nested_message_optional_field' is a composite");
            { // locally scoped for sanity
                final NodeFieldStripeDecoder nestedDecoder = (NodeFieldStripeDecoder)childDecoders.get(2/*nested_message_repeated_field*/);
                final List<IFieldStripeDecoder> nestedDecoders = getInternalState(nestedDecoder, "childDecoders");
                assertEquals(nestedDecoders.size(), 3, "Nested fields / decoders");
                assertTrue(nestedDecoders.get(0/*float_field*/) instanceof LeafFieldStripeDecoder, "'float_field' is a primitive");
                assertTrue(nestedDecoders.get(1/*int32_field*/) instanceof LeafFieldStripeDecoder, "'int32_field' is a primitive");
                assertTrue(nestedDecoders.get(2/*bool_field*/) instanceof LeafFieldStripeDecoder, "'bool_field' is a primitive");
            }
            assertTrue(childDecoders.get(3/*string_field*/) instanceof LeafFieldStripeDecoder, "'string_field' is a primitive");
        }
    }

    /**
     * Tests creating an decoder tree from a nested structure that is two levels 
     * deep (with repeated parents).
     * 
     * @see RepeatedParentStripeDecoderTest#twoLevelNestedSchemaTest()
     */
    @Test
    public void twoLevelNestedSchemaRepeatedParentTest() throws Exception {
        // NOTE:  this matches the "Employee" example in the "Efficient Field-
        //        Striped, Nested Disk-backed Record Storage" document
        final String protobufText =
            "package package_name;\n"/*required by Protostuff*/ +
            "message Employee {\n" + 
            "    required int64    RecId = 1;\n" +
            "    required int64    EmpId = 2;\n" +
            "    message Department {\n" +
            "        optional int64    DeptId = 1;\n" +
            "        required string   Name = 2;\n" +
            "        message Location {\n" +
            "            required string   Building = 1;\n" +
            "            repeated int32    Floor = 2;\n" +
            "        }\n" +
            "        repeated Location Loc = 3;\n" +
            "    }\n" +
            "    repeated Department Dept = 3;\n" +
            "    optional float      BonusRate = 4;\n" +
            "    required string     FirstName = 5;\n" +
            "    required string     LastName = 6;\n" +
            "}\n";
        final SchemaBuilder schemaBuilder = new SchemaBuilder();
        final ICompositeType schema = schemaBuilder.createSchema(protobufText, "Employee");

        // ....................................................................
        final MockFieldStriptReaderFactory fieldStripeReaderFactory = new MockFieldStriptReaderFactory();
        final FieldStripeDecoderFactory decoderFactory = new FieldStripeDecoderFactory(fieldStripeReaderFactory);
        final RootFieldStripeDecoder root = decoderFactory.createDecoderTree(schema);
        assertNotNull(root, "Root field stripe decoder");

        // ensure that the tree of decoders matches what is expected
        { // locally scoped for sanity
            final List<IFieldStripeDecoder> childDecoders = getInternalState(root, "childDecoders");
            assertEquals(childDecoders.size(), 6, "Child fields / decoders");
            assertTrue(childDecoders.get(0/*RecId*/) instanceof LeafFieldStripeDecoder, "'RecId' is a primitive");
            assertTrue(childDecoders.get(1/*EmpId*/) instanceof LeafFieldStripeDecoder, "'EmpId' is a primitive");
            assertTrue(childDecoders.get(2/*Loc*/) instanceof NodeFieldStripeDecoder, "'Loc' is a composite");
            { // locally scoped for sanity
                final NodeFieldStripeDecoder departmentDecoder = (NodeFieldStripeDecoder)childDecoders.get(2/*Loc*/);
                final List<IFieldStripeDecoder> departmentDecoders = getInternalState(departmentDecoder, "childDecoders");
                assertEquals(departmentDecoders.size(), 3, "Nested fields / decoders");
                assertTrue(departmentDecoders.get(0/*DeptId*/) instanceof LeafFieldStripeDecoder, "'DeptId' is a primitive");
                assertTrue(departmentDecoders.get(1/*Name*/) instanceof LeafFieldStripeDecoder, "'Name' is a primitive");
                assertTrue(departmentDecoders.get(2/*Loc*/) instanceof NodeFieldStripeDecoder, "'Loc' is a composite");
                { // locally scoped for sanity
                    final NodeFieldStripeDecoder locationDecoder = (NodeFieldStripeDecoder)departmentDecoders.get(2/*nested_message_repeated_field*/);
                    final List<IFieldStripeDecoder> locationDecoders = getInternalState(locationDecoder, "childDecoders");
                    assertEquals(locationDecoders.size(), 2, "Nested fields / decoders");
                    assertTrue(locationDecoders.get(0/*Building*/) instanceof LeafFieldStripeDecoder, "'Building' is a primitive");
                    assertTrue(locationDecoders.get(1/*Floor*/) instanceof LeafFieldStripeDecoder, "'Floor' is a primitive");
                }
            }
            assertTrue(childDecoders.get(3/*BonusRate*/) instanceof LeafFieldStripeDecoder, "'BonusRate' is a primitive");
            assertTrue(childDecoders.get(4/*FirstName*/) instanceof LeafFieldStripeDecoder, "'FirstName' is a primitive");
            assertTrue(childDecoders.get(5/*LastName*/) instanceof LeafFieldStripeDecoder, "'LastName' is a primitive");
        }
    }

    // ************************************************************************
    /**
     * A mock {@link IFieldStripeReaderFactory} that returns a dummy {@link IFieldStripeReader readers}
     * that throws if used.
     */
    private static final class MockFieldStriptReaderFactory implements IFieldStripeReaderFactory {
        /**
         * {@inheritDoc}
         * 
         * Returns a mock {@link IFieldStripeReader} regardless of the {@link IField}
         * specified. 
         */
        @Override
        public IFieldStripeReader createFieldStripeReader(final IField field) {
            return new IFieldStripeReader() {
                @Override
                public Instruction readInstruction() { throw new DeveloperException("Called readInstruction() on a mock IFieldStripeReader."); }
            };
        }
    }
}