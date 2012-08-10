package net.agkn.field_stripe.encode;

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

import org.testng.annotations.Test;

import net.agkn.field_stripe.record.ICompositeType;
import net.agkn.field_stripe.stripe.TestXMLFieldStripeWriterFactory;

/**
 * Unit tests for {@link FieldStripeEncoderFactory} -- specifically for the
 * process of {@link FieldStripeEncoderFactory#createEncoderTree(net.agkn.field_stripe.record.IFieldType) creating encoder trees}
 * from a {@link ICompositeType}.
 *
 * @author rgrzywinski
 */
public class FieldStripeEncoderFactoryTest {
    /**
     * Tests creating an encoder tree from a flat structure with no parents 
     * (the simplest case). 
     * 
     * @see NoRepeatedParentStripeEncoderTest#flatSchemaTest()
     */
    @Test
    public void flatSchemaTest() throws Exception {
        final String protobufText =
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
        final TestXMLFieldStripeWriterFactory fieldStripeWriterFactory = new TestXMLFieldStripeWriterFactory();
        final FieldStripeEncoderFactory encoderFactory = new FieldStripeEncoderFactory(fieldStripeWriterFactory);
        final RootFieldStripeEncoder root = encoderFactory.createEncoderTree(schema);
        assertNotNull(root, "Root field stripe encoder");

        // ensure that the tree of encoders matches what is expected
        { // locally scoped for sanity
            final List<IFieldStripeEncoder> childEncoders = getInternalState(root, "childEncoders");
            assertEquals(childEncoders.size(), 6, "Child fields / encoders");
            assertTrue(childEncoders.get(0/*double_field*/) instanceof LeafFieldStripeEncoder, "'double_field' is a primitive");
            assertTrue(childEncoders.get(1/*float_field*/) instanceof LeafFieldStripeEncoder, "'float_field' is a primitive");
            assertTrue(childEncoders.get(2/*int32_field*/) instanceof LeafFieldStripeEncoder, "'int32_field' is a primitive");
            assertTrue(childEncoders.get(3/*int64_field*/) instanceof LeafFieldStripeEncoder, "'int64_field' is a primitive");
            assertTrue(childEncoders.get(4/*bool_field*/) instanceof LeafFieldStripeEncoder, "'bool_field' is a primitive");
            assertTrue(childEncoders.get(5/*string_field*/) instanceof LeafFieldStripeEncoder, "'string_field' is a primitive");
        }
    }    

    /**
     * Tests creating an encoder tree a nested structure that is one level deep 
     * (with no repeated parents).
     * 
     * @see NoRepeatedParentStripeEncoderTest#oneLevelNestedSchemaTest()
     */
    @Test
    public void oneLevelNestedSchemaTest() throws Exception {
        final String protobufText =
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
        final TestXMLFieldStripeWriterFactory fieldStripeWriterFactory = new TestXMLFieldStripeWriterFactory();
        final FieldStripeEncoderFactory encoderFactory = new FieldStripeEncoderFactory(fieldStripeWriterFactory);
        final RootFieldStripeEncoder root = encoderFactory.createEncoderTree(schema);
        assertNotNull(root, "Root field stripe encoder");

        // ensure that the tree of encoders matches what is expected
        { // locally scoped for sanity
            final List<IFieldStripeEncoder> childEncoders = getInternalState(root, "childEncoders");
            assertEquals(childEncoders.size(), 5, "Child fields / encoders");
            assertTrue(childEncoders.get(0/*double_field*/) instanceof LeafFieldStripeEncoder, "'double_field' is a primitive");
            assertTrue(childEncoders.get(1/*int64_field*/) instanceof LeafFieldStripeEncoder, "'int64_field' is a primitive");
            assertTrue(childEncoders.get(2/*nested_message_optional_field*/) instanceof NodeFieldStripeEncoder, "'nested_message_optional_field' is a composite");
            { // locally scoped for sanity
                final NodeFieldStripeEncoder nestedEncoder = (NodeFieldStripeEncoder)childEncoders.get(2/*nested_message_optional_field*/);
                final List<IFieldStripeEncoder> nestedEncoders = getInternalState(nestedEncoder, "childEncoders");
                assertEquals(nestedEncoders.size(), 3, "Nested fields / encoders");
                assertTrue(nestedEncoders.get(0/*float_field*/) instanceof LeafFieldStripeEncoder, "'float_field' is a primitive");
                assertTrue(nestedEncoders.get(1/*int32_field*/) instanceof LeafFieldStripeEncoder, "'int32_field' is a primitive");
                assertTrue(nestedEncoders.get(2/*bool_field*/) instanceof LeafFieldStripeEncoder, "'bool_field' is a primitive");
            }
            assertTrue(childEncoders.get(3/*nested_message_required_field*/) instanceof NodeFieldStripeEncoder, "'nested_message_required_field' is a composite");
            { // locally scoped for sanity
                final NodeFieldStripeEncoder nestedEncoder = (NodeFieldStripeEncoder)childEncoders.get(3/*nested_message_required_field*/);
                final List<IFieldStripeEncoder> nestedEncoders = getInternalState(nestedEncoder, "childEncoders");
                assertEquals(nestedEncoders.size(), 3, "Nested fields / encoders");
                assertTrue(nestedEncoders.get(0/*float_field*/) instanceof LeafFieldStripeEncoder, "'float_field' is a primitive");
                assertTrue(nestedEncoders.get(1/*int32_field*/) instanceof LeafFieldStripeEncoder, "'int32_field' is a primitive");
                assertTrue(nestedEncoders.get(2/*bool_field*/) instanceof LeafFieldStripeEncoder, "'bool_field' is a primitive");
            }
            assertTrue(childEncoders.get(4/*string_field*/) instanceof LeafFieldStripeEncoder, "'string_field' is a primitive");
        }
    }

    /**
     * Tests creating an encoder tree from a nested structure that is one level 
     * deep (with repeated parents).
     * 
     * @see RepeatedParentStripeEncoderTest#oneLevelNestedSchemaTest()
     */
    @Test
    public void oneLevelNestedSchemaRepeatedParentTest() throws Exception {
        final String protobufText =
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
        final TestXMLFieldStripeWriterFactory fieldStripeWriterFactory = new TestXMLFieldStripeWriterFactory();
        final FieldStripeEncoderFactory encoderFactory = new FieldStripeEncoderFactory(fieldStripeWriterFactory);
        final RootFieldStripeEncoder root = encoderFactory.createEncoderTree(schema);
        assertNotNull(root, "Root field stripe encoder");

        // ensure that the tree of encoders matches what is expected
        { // locally scoped for sanity
            final List<IFieldStripeEncoder> childEncoders = getInternalState(root, "childEncoders");
            assertEquals(childEncoders.size(), 4, "Child fields / encoders");
            assertTrue(childEncoders.get(0/*double_field*/) instanceof LeafFieldStripeEncoder, "'double_field' is a primitive");
            assertTrue(childEncoders.get(1/*int64_field*/) instanceof LeafFieldStripeEncoder, "'int64_field' is a primitive");
            assertTrue(childEncoders.get(2/*nested_message_repeated_field*/) instanceof NodeFieldStripeEncoder, "'nested_message_optional_field' is a composite");
            { // locally scoped for sanity
                final NodeFieldStripeEncoder nestedEncoder = (NodeFieldStripeEncoder)childEncoders.get(2/*nested_message_repeated_field*/);
                final List<IFieldStripeEncoder> nestedEncoders = getInternalState(nestedEncoder, "childEncoders");
                assertEquals(nestedEncoders.size(), 3, "Nested fields / encoders");
                assertTrue(nestedEncoders.get(0/*float_field*/) instanceof LeafFieldStripeEncoder, "'float_field' is a primitive");
                assertTrue(nestedEncoders.get(1/*int32_field*/) instanceof LeafFieldStripeEncoder, "'int32_field' is a primitive");
                assertTrue(nestedEncoders.get(2/*bool_field*/) instanceof LeafFieldStripeEncoder, "'bool_field' is a primitive");
            }
            assertTrue(childEncoders.get(3/*string_field*/) instanceof LeafFieldStripeEncoder, "'string_field' is a primitive");
        }
    }

    /**
     * Tests creating an encoder tree from a nested structure that is two levels 
     * deep (with repeated parents).
     * 
     * @see RepeatedParentStripeEncoderTest#twoLevelNestedSchemaTest()
     */
    @Test
    public void twoLevelNestedSchemaRepeatedParentTest() throws Exception {
        // NOTE:  this matches the "Employee" example in the "Efficient Field-
        //        Striped, Nested Disk-backed Record Storage" document
        final String protobufText =
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
        final TestXMLFieldStripeWriterFactory fieldStripeWriterFactory = new TestXMLFieldStripeWriterFactory();
        final FieldStripeEncoderFactory encoderFactory = new FieldStripeEncoderFactory(fieldStripeWriterFactory);
        final RootFieldStripeEncoder root = encoderFactory.createEncoderTree(schema);
        assertNotNull(root, "Root field stripe encoder");

        // ensure that the tree of encoders matches what is expected
        { // locally scoped for sanity
            final List<IFieldStripeEncoder> childEncoders = getInternalState(root, "childEncoders");
            assertEquals(childEncoders.size(), 6, "Child fields / encoders");
            assertTrue(childEncoders.get(0/*RecId*/) instanceof LeafFieldStripeEncoder, "'RecId' is a primitive");
            assertTrue(childEncoders.get(1/*EmpId*/) instanceof LeafFieldStripeEncoder, "'EmpId' is a primitive");
            assertTrue(childEncoders.get(2/*Loc*/) instanceof NodeFieldStripeEncoder, "'Loc' is a composite");
            { // locally scoped for sanity
                final NodeFieldStripeEncoder departmentEncoder = (NodeFieldStripeEncoder)childEncoders.get(2/*Loc*/);
                final List<IFieldStripeEncoder> departmentEncoders = getInternalState(departmentEncoder, "childEncoders");
                assertEquals(departmentEncoders.size(), 3, "Nested fields / encoders");
                assertTrue(departmentEncoders.get(0/*DeptId*/) instanceof LeafFieldStripeEncoder, "'DeptId' is a primitive");
                assertTrue(departmentEncoders.get(1/*Name*/) instanceof LeafFieldStripeEncoder, "'Name' is a primitive");
                assertTrue(departmentEncoders.get(2/*Loc*/) instanceof NodeFieldStripeEncoder, "'Loc' is a composite");
                { // locally scoped for sanity
                    final NodeFieldStripeEncoder locationEncoder = (NodeFieldStripeEncoder)departmentEncoders.get(2/*nested_message_repeated_field*/);
                    final List<IFieldStripeEncoder> locationEncoders = getInternalState(locationEncoder, "childEncoders");
                    assertEquals(locationEncoders.size(), 2, "Nested fields / encoders");
                    assertTrue(locationEncoders.get(0/*Building*/) instanceof LeafFieldStripeEncoder, "'Building' is a primitive");
                    assertTrue(locationEncoders.get(1/*Floor*/) instanceof LeafFieldStripeEncoder, "'Floor' is a primitive");
                }
            }
            assertTrue(childEncoders.get(3/*BonusRate*/) instanceof LeafFieldStripeEncoder, "'BonusRate' is a primitive");
            assertTrue(childEncoders.get(4/*FirstName*/) instanceof LeafFieldStripeEncoder, "'FirstName' is a primitive");
            assertTrue(childEncoders.get(5/*LastName*/) instanceof LeafFieldStripeEncoder, "'LastName' is a primitive");
        }
    }
}