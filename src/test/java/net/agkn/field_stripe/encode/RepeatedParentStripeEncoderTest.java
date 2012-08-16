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

import static net.agkn.field_stripe.encode.NoRepeatedParentStripeEncoderTest.assertFieldStripe;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.StringReader;

import net.agkn.field_stripe.exception.OperationFailedException;
import net.agkn.field_stripe.record.ICompositeType;
import net.agkn.field_stripe.record.reader.IRecordReader;
import net.agkn.field_stripe.record.reader.SmartJsonArrayRecordReader;
import net.agkn.field_stripe.stripe.TestXMLFieldStripeWriterFactory;
import net.agkn.field_stripe.stripe.XMLFieldStripeWriter;

import org.testng.annotations.Test;

/**
 * Unit tests of structures that have repeated parents. This makes use of the
 * {@link XMLFieldStripeWriter XML writer} for simplicity and human readability.
 *
 * @author rgrzywinski
 */
public class RepeatedParentStripeEncoderTest {
    /**
     * Tests encoding a nested structure that is one level deep (with repeated 
     * parents).
     *  
     * @see FieldStripeEncoderFactoryTest#oneLevelNestedSchemaRepeatedParentTest()
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

        // ....................................................................
        // stripe a series of records (JSON-array-based)
        final String jsonRecord = 
            "[11.0,[121],[[1311.0,1312,[true]]],[\"string141\"]]\n" +
            "[null,[],[],[]]\n"/*all missing or unset fields*/ +
            "[21.0,null,[],[\"string241\"]]\n"/*array exists but has no elements*/ +
            "[31.0,[],[[null,3312,[]]],[]]\n"/*one element of array that has null values*/ +
            "[41.0,[421,422,423],[[4311.0,4312,[false,true]]],[\"string441\",\"string442\"]]\n"/*one full value for each*/ +
            "[51.0,[521,522],[[5311.0,5312,[]],[null,5322,[false,true]],[5331.0,5332,[true]]],[\"string541\"]]\n"/*more than one value for nested structures*/;
        final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(jsonRecord));
        while(root.encode(recordReader));

        // validate the stripe of each field
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>11.0</Value>\n" + 
                "<Unset/>\n" + 
                "<Value>21.0</Value>\n" + 
                "<Value>31.0</Value>\n" + 
                "<Value>41.0</Value>\n" + 
                "<Value>51.0</Value>\n"; 
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 0/*double_field*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>121</Value>\n" + 
                "<Unset/>\n" + 
                "<Unset/>\n" + 
                "<Unset/>\n" + 
                "<Value>421</Value>\n" + 
                "<RepeatedValue/>\n" + 
                "<Value>422</Value>\n" + 
                "<RepeatedValue/>\n" + 
                "<Value>423</Value>\n" + 
                "<Value>521</Value>\n" + 
                "<RepeatedValue/>\n" + 
                "<Value>522</Value>\n"; 
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 1/*int64_field*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>1311.0</Value>\n" + 
                "<ParentIsUnset>1</ParentIsUnset>\n" + 
                "<ParentIsUnset>1</ParentIsUnset>\n" +
                "<Unset/>\n" +
                "<Value>4311.0</Value>\n" +
                "<Value>5311.0</Value>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<Unset/>\n" +
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<Value>5331.0</Value>\n";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 2/*nested_message_repeated_field*/, 0/*float_field*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>1312</Value>\n" + 
                "<ParentIsUnset>1</ParentIsUnset>\n" + 
                "<ParentIsUnset>1</ParentIsUnset>\n" + 
                "<Value>3312</Value>\n" + 
                "<Value>4312</Value>\n" +
                "<Value>5312</Value>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<Value>5322</Value>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<Value>5332</Value>\n";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 2/*nested_message_repeated_field*/, 1/*int32_field*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>true</Value>\n" + 
                "<ParentIsUnset>1</ParentIsUnset>\n" + 
                "<ParentIsUnset>1</ParentIsUnset>\n" + 
                "<Unset/>\n" +
                "<Value>false</Value>\n" + 
                "<RepeatedValue/>\n" + 
                "<Value>true</Value>\n" +
                "<Unset/>\n" +
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<Value>false</Value>\n" + 
                "<RepeatedValue/>\n" + 
                "<Value>true</Value>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<Value>true</Value>\n";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 2/*nested_message_repeated_field*/, 2/*bool_field*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>string141</Value>\n" + 
                "<Unset/>\n" + 
                "<Value>string241</Value>\n" + 
                "<Unset/>\n" + 
                "<Value>string441</Value>\n" + 
                "<RepeatedValue/>\n" + 
                "<Value>string442</Value>\n" +
                "<Value>string541</Value>\n";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 3/*string_field*/);
        }
    }

    /**
     * Tests encoding a nested structure that is two levels deep (with repeated 
     * parents).
     * 
     * @see FieldStripeEncoderFactoryTest#twoLevelNestedSchemaRepeatedParentTest()
     * @see #twoLevelNestedSchemaTest2()
     */
    @Test
    public void twoLevelNestedSchemaTest() throws Exception {
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
        final TestXMLFieldStripeWriterFactory fieldStripeWriterFactory = new TestXMLFieldStripeWriterFactory();
        final FieldStripeEncoderFactory encoderFactory = new FieldStripeEncoderFactory(fieldStripeWriterFactory);
        final RootFieldStripeEncoder root = encoderFactory.createEncoderTree(schema);
        assertNotNull(root, "Root field stripe encoder");

        // ....................................................................
        // stripe a series of records (JSON-array-based)
        final String jsonRecord = 
            "[11,12,[],1.4,\"first15\",\"last16\"]\n" +
            "[21,22,[[2311,\"name2312\",[]]],2.4,\"first25\",\"last26\"]\n" +
            "[31,32,[[3311,\"name3312\",[[\"building33131\",[]]]]],3.4,\"first35\",\"last36\"]\n" +
            "[41,42,[[4311,\"name4312\",[[\"building43131\",[431321]]]]],4.4,\"first45\",\"last46\"]\n" +
            "[51,52,[[5311,\"name5312\",[[\"building53131\",[5313211]],[\"building53132\",[5313211,5313212]]]]],5.4,\"first55\",\"last56\"]\n" +
            "[61,62,[[6311,\"name6312\",[[\"building63131\",[631321]]]],[6321,\"name6322\",[]]],6.4,\"first65\",\"last66\"]\n" +
            "[71,72,[[7311,\"name7312\",[[\"building73131\",[731321]]]],[7321,\"name7322\",[[\"building73231\",[]]]]],7.4,\"first75\",\"last76\"]\n" +
            "[81,82,[[8311,\"name8312\",[[\"building83131\",[831321]]]],[8321,\"name8322\",[[\"building83231\",[832321]]]]],8.4,\"first85\",\"last86\"]\n" +
            // NOTE: an attempt to match the "Nested Repeated Field Schema" (s4) case
            "[91,92,[[9311,\"name9312\",[[\"building93131\",null],[\"building93132\",null]]],[9321,null,[[\"building93231\",[932321,932322]]]]],9.4,\"first95\",\"last96\"]\n";
        final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(jsonRecord));
        while(root.encode(recordReader));

        // validate the stripe of each field
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>11</Value>\n" +
                "<Value>21</Value>\n" +
                "<Value>31</Value>\n" +
                "<Value>41</Value>\n" +
                "<Value>51</Value>\n" +
                "<Value>61</Value>\n" +
                "<Value>71</Value>\n" +
                "<Value>81</Value>\n" +
                "<Value>91</Value>\n";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 0/*RecId*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>12</Value>\n" +
                "<Value>22</Value>\n" +
                "<Value>32</Value>\n" +
                "<Value>42</Value>\n" +
                "<Value>52</Value>\n" +
                "<Value>62</Value>\n" +
                "<Value>72</Value>\n" +
                "<Value>82</Value>\n" +
                "<Value>92</Value>\n";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 1/*EmpId*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<ParentIsUnset>1</ParentIsUnset>\n" + 
                "<Value>2311</Value>\n" + 
                "<Value>3311</Value>\n" + 
                "<Value>4311</Value>\n" + 
                "<Value>5311</Value>\n" + 
                "<Value>6311</Value>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<Value>6321</Value>\n" + 
                "<Value>7311</Value>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<Value>7321</Value>\n" + 
                "<Value>8311</Value>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<Value>8321</Value>\n" + 
                "<Value>9311</Value>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<Value>9321</Value>\n";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 2/*Dept*/, 0/*DeptId*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<ParentIsUnset>1</ParentIsUnset>\n" + 
                "<Value>name2312</Value>\n" + 
                "<Value>name3312</Value>\n" + 
                "<Value>name4312</Value>\n" + 
                "<Value>name5312</Value>\n" + 
                "<Value>name6312</Value>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<Value>name6322</Value>\n" + 
                "<Value>name7312</Value>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<Value>name7322</Value>\n" + 
                "<Value>name8312</Value>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<Value>name8322</Value>\n" + 
                "<Value>name9312</Value>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" +
                "<Unset/>\n";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 2/*Dept*/, 1/*Name*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<ParentIsUnset>1</ParentIsUnset>\n" + 
                "<ParentIsUnset>2</ParentIsUnset>\n" + 
                "<Value>building33131</Value>\n" + 
                "<Value>building43131</Value>\n" + 
                "<Value>building53131</Value>\n" + 
                "<RepeatedParent>2</RepeatedParent>\n" + 
                "<Value>building53132</Value>\n" + 
                "<Value>building63131</Value>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<ParentIsUnset>2</ParentIsUnset>\n" + 
                "<Value>building73131</Value>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<Value>building73231</Value>\n" + 
                "<Value>building83131</Value>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<Value>building83231</Value>\n" + 
                "<Value>building93131</Value>\n" + 
                "<RepeatedParent>2</RepeatedParent>\n" + 
                "<Value>building93132</Value>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<Value>building93231</Value>\n";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 2/*Dept*/, 2/*Loc*/, 0/*Building*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<ParentIsUnset>1</ParentIsUnset>\n" + 
                "<ParentIsUnset>2</ParentIsUnset>\n" + 
                "<Unset/>\n" +
                "<Value>431321</Value>\n" + 
                "<Value>5313211</Value>\n" + 
                "<RepeatedParent>2</RepeatedParent>\n" + 
                "<Value>5313211</Value>\n" + 
                "<RepeatedValue/>\n" + 
                "<Value>5313212</Value>\n" + 
                "<Value>631321</Value>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<ParentIsUnset>2</ParentIsUnset>\n" + 
                "<Value>731321</Value>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<Unset/>\n" +
                "<Value>831321</Value>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<Value>832321</Value>\n" + 
                "<Unset/>\n" +
                "<RepeatedParent>2</RepeatedParent>\n" + 
                "<Unset/>\n" +
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<Value>932321</Value>\n" + 
                "<RepeatedValue/>\n" + 
                "<Value>932322</Value>\n";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 2/*Dept*/, 2/*Loc*/, 1/*Floor*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>1.4</Value>\n" +
                "<Value>2.4</Value>\n" +
                "<Value>3.4</Value>\n" +
                "<Value>4.4</Value>\n" +
                "<Value>5.4</Value>\n" +
                "<Value>6.4</Value>\n" +
                "<Value>7.4</Value>\n" +
                "<Value>8.4</Value>\n" +
                "<Value>9.4</Value>\n";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 3/*BonusRate*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>first15</Value>\n" +
                "<Value>first25</Value>\n" +
                "<Value>first35</Value>\n" +
                "<Value>first45</Value>\n" +
                "<Value>first55</Value>\n" +
                "<Value>first65</Value>\n" +
                "<Value>first75</Value>\n" +
                "<Value>first85</Value>\n" +
                "<Value>first95</Value>\n";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 4/*FirstName*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>last16</Value>\n" +
                "<Value>last26</Value>\n" +
                "<Value>last36</Value>\n" +
                "<Value>last46</Value>\n" +
                "<Value>last56</Value>\n" +
                "<Value>last66</Value>\n" +
                "<Value>last76</Value>\n" +
                "<Value>last86</Value>\n" +
                "<Value>last96</Value>\n";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 5/*LastName*/);
        }
    }

    /**
     * Another set of tests which encode a nested structure that is two levels 
     * deep (with repeated parents). This focuses on repeated and <var>Unset</var>
     * parents.<p/> 
     * 
     * This is split from {@link #twoLevelNestedSchemaTest()} simply to keep 
     * the field stripe lengths manageable.
     * 
     * @see FieldStripeEncoderFactoryTest#twoLevelNestedSchemaRepeatedParentTest()
     * @see #twoLevelNestedSchemaTest()
     */
    @Test
    public void twoLevelNestedSchemaTest2() throws Exception {
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
        final TestXMLFieldStripeWriterFactory fieldStripeWriterFactory = new TestXMLFieldStripeWriterFactory();
        final FieldStripeEncoderFactory encoderFactory = new FieldStripeEncoderFactory(fieldStripeWriterFactory);
        final RootFieldStripeEncoder root = encoderFactory.createEncoderTree(schema);
        assertNotNull(root, "Root field stripe encoder");

        // ....................................................................
        // stripe a series of records (JSON-array-based)
        final String jsonRecord = 
            "[11,12,[],1.4,\"first15\",\"last16\"]\n" +
            "[21,22,[[2311,\"name2312\",[]]],2.4,\"first25\",\"last26\"]\n" +
            "[31,32,[[3311,\"name3312\",[]],[3321,\"name3322\",[]]],3.4,\"first35\",\"last36\"]\n";
        final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(jsonRecord));
        while(root.encode(recordReader));

        // validate the stripe of each field
        // NOTE:  the +""'s at the end are just for convenience when adding more
        //        cases (they're just easy to remove when a new line is appended
        //        below it)
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>11</Value>\n" + 
                "<Value>21</Value>\n" +
                "<Value>31</Value>\n" + "";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 0/*RecId*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>12</Value>\n" +
                "<Value>22</Value>\n" +
                "<Value>32</Value>\n" + "";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 1/*EmpId*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<ParentIsUnset>1</ParentIsUnset>\n" + 
                "<Value>2311</Value>\n" + 
                "<Value>3311</Value>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<Value>3321</Value>\n" + ""; 
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 2/*Dept*/, 0/*DeptId*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<ParentIsUnset>1</ParentIsUnset>\n" + 
                "<Value>name2312</Value>\n" + 
                "<Value>name3312</Value>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<Value>name3322</Value>\n" + ""; 
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 2/*Dept*/, 1/*Name*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<ParentIsUnset>1</ParentIsUnset>\n" + 
                "<ParentIsUnset>2</ParentIsUnset>\n" + 
                "<ParentIsUnset>2</ParentIsUnset>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<ParentIsUnset>2</ParentIsUnset>\n" + ""; 
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 2/*Dept*/, 2/*Loc*/, 0/*Building*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<ParentIsUnset>1</ParentIsUnset>\n" + 
                "<ParentIsUnset>2</ParentIsUnset>\n" + 
                "<ParentIsUnset>2</ParentIsUnset>\n" + 
                "<RepeatedParent>1</RepeatedParent>\n" + 
                "<ParentIsUnset>2</ParentIsUnset>\n" + ""; 
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 2/*Dept*/, 2/*Loc*/, 1/*Floor*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>1.4</Value>\n" +
                "<Value>2.4</Value>\n" +
                "<Value>3.4</Value>\n" + "";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 3/*BonusRate*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>first15</Value>\n" +
                "<Value>first25</Value>\n" +
                "<Value>first35</Value>\n" + "";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 4/*FirstName*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>last16</Value>\n" +
                "<Value>last26</Value>\n" +
                "<Value>last36</Value>\n" + "";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 5/*LastName*/);
        }
    }

    // ........................................................................
    // negative tests

    /**
     * Tests that a <code>null</code> value in a repeated field is not allowed.
     */
    @Test
    public void nullInRepeatedValueTest() throws Exception {
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
        final TestXMLFieldStripeWriterFactory fieldStripeWriterFactory = new TestXMLFieldStripeWriterFactory();
        final FieldStripeEncoderFactory encoderFactory = new FieldStripeEncoderFactory(fieldStripeWriterFactory);
        final RootFieldStripeEncoder root = encoderFactory.createEncoderTree(schema);
        assertNotNull(root, "Root field stripe encoder");

        // SEE:  #oneLevelNestedSchemaTest() for validation of the schema

        // ....................................................................
        // null in primitive field's array
        try {
            // stripe a series of records (JSON-array-based)
            final String jsonRecord = "[11.0,[null],[[1311.0,1312,[true]]],[\"string141\"]]\n"/*null in int64_field*/;
            final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(jsonRecord));
            root.encode(recordReader);
            assert false : "An exception was expected.";
        } catch(final OperationFailedException ofe) {
            assertEquals(ofe.getLocalizedMessage(), "Repeated field \"int64_field\" has an unset value in its collection.", "Exception");
        }
        try {
            // stripe a series of records (JSON-array-based)
            final String jsonRecord = "[11.0,[121,null],[[1311.0,1312,[true]]],[\"string141\"]]\n"/*null in int64_field*/;
            final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(jsonRecord));
            root.encode(recordReader);
            assert false : "An exception was expected.";
        } catch(final OperationFailedException ofe) {
            assertEquals(ofe.getLocalizedMessage(), "Repeated field \"int64_field\" has an unset value in its collection.", "Exception");
        }

        // null in nested message field's array
        try {
            // stripe a series of records (JSON-array-based)
            final String jsonRecord = "[11.0,[121],[null],[\"string141\"]]\n"/*null in nested_message_repeated_field*/;
            final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(jsonRecord));
            root.encode(recordReader);
            assert false : "An exception was expected.";
        } catch(final OperationFailedException ofe) {
            assertEquals(ofe.getLocalizedMessage(), "Repeated nested field \"nested_message_repeated_field\" has an unset value in its collection.", "Exception");
        }
        try {
            // stripe a series of records (JSON-array-based)
            final String jsonRecord = "[11.0,[121],[[1311.0,1312,[true]],null],[\"string141\"]]\n"/*null in nested_message_repeated_field*/;
            final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(jsonRecord));
            root.encode(recordReader);
            assert false : "An exception was expected.";
        } catch(final OperationFailedException ofe) {
            assertEquals(ofe.getLocalizedMessage(), "Repeated nested field \"nested_message_repeated_field\" has an unset value in its collection.", "Exception");
        }
    }    
}