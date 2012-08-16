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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.StringReader;
import java.io.StringWriter;

import net.agkn.field_stripe.record.ICompositeType;
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.record.reader.IRecordReader;
import net.agkn.field_stripe.record.reader.SmartJsonArrayRecordReader;
import net.agkn.field_stripe.stripe.TestXMLFieldStripeWriterFactory;
import net.agkn.field_stripe.stripe.XMLFieldStripeWriter;

import org.testng.annotations.Test;

/**
 * Unit tests of structures that have no repeated parents. This makes use of
 * the {@link XMLFieldStripeWriter XML writer} for simplicity and human 
 * readability.
 *
 * @author rgrzywinski
 */
public class NoRepeatedParentStripeEncoderTest {
    /**
     * Tests encoding a flat structure with no parents (the simplest case).
     * 
     * @see FieldStripeEncoderFactoryTest#flatSchemaTest()
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
        final TestXMLFieldStripeWriterFactory fieldStripeWriterFactory = new TestXMLFieldStripeWriterFactory();
        final FieldStripeEncoderFactory encoderFactory = new FieldStripeEncoderFactory(fieldStripeWriterFactory);
        final RootFieldStripeEncoder root = encoderFactory.createEncoderTree(schema);
        assertNotNull(root, "Root field stripe encoder");

        // ....................................................................
        // stripe a series of records (JSON-array-based)
        final String jsonRecord = 
            "[11.0,12.0,[131],14,true,[\"string151\"]]\n" +
            "[null,null,null,null,null,null]\n"/*all missing or Unset fields*/ +
            "[31.0,32.0,[331,332,333],34,false,[\"string351\",\"string352\"]]\n" +
            "[41.0,42.0,[],44,true,[]]\n"/*unset repeated fields*/ +
            "[51.0,52.0,null,54,false,null]]\n"/*explicit null repeated fields*/;
        final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(jsonRecord));
        while(recordReader.hasMoreRecords())
            root.encode(recordReader);

        // validate the stripe of each field
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>11.0</Value>\n" + 
                "<Unset/>\n" + 
                "<Value>31.0</Value>\n" +
                "<Value>41.0</Value>\n" +
                "<Value>51.0</Value>\n";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 0/*double_field*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>12.0</Value>\n" + 
                "<Unset/>\n" + 
                "<Value>32.0</Value>\n" +
                "<Value>42.0</Value>\n" +
                "<Value>52.0</Value>\n";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 1/*float_field*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>131</Value>\n" + 
                "<Unset/>\n" + 
                "<Value>331</Value>\n" + 
                "<RepeatedValue/>\n" + 
                "<Value>332</Value>\n" + 
                "<RepeatedValue/>\n" + 
                "<Value>333</Value>\n" + 
                "<Unset/>\n" + 
                "<Unset/>\n"; 
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 2/*int32_field*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>14</Value>\n" + 
                "<Unset/>\n" + 
                "<Value>34</Value>\n" +
                "<Value>44</Value>\n" +
                "<Value>54</Value>\n";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 3/*int64_field*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>true</Value>\n" + 
                "<Unset/>\n" + 
                "<Value>false</Value>\n" +
                "<Value>true</Value>\n" +
                "<Value>false</Value>\n";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 4/*bool_field*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>string151</Value>\n" + 
                "<Unset/>\n" + 
                "<Value>string351</Value>\n" + 
                "<RepeatedValue/>\n" + 
                "<Value>string352</Value>\n" +
                "<Unset/>\n" + 
                "<Unset/>\n"; 
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 5/*string_field*/);
        }
    }

    /**
     * Tests encoding a nested structure that is one level deep (with no repeated 
     * parents).
     * 
     * @see FieldStripeEncoderFactoryTest#oneLevelNestedSchemaTest()
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
        final TestXMLFieldStripeWriterFactory fieldStripeWriterFactory = new TestXMLFieldStripeWriterFactory();
        final FieldStripeEncoderFactory encoderFactory = new FieldStripeEncoderFactory(fieldStripeWriterFactory);
        final RootFieldStripeEncoder root = encoderFactory.createEncoderTree(schema);
        assertNotNull(root, "Root field stripe encoder");

        // ....................................................................
        // stripe a series of records (JSON-array-based)
        final String jsonRecord = 
            "[11.0,[121],[1311.0,1312,[true]],[1411.0,1412,[false]],[\"string151\"]]\n" +
            "[null,[],null,null,[]]\n"/*all missing or Unset fields*/ +
            "[21.0,[221,222,223],[2311.0,2312,[false,true]],[2411.0,2412,[true,false]],[\"string251\",\"string252\"]]\n";
        final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(jsonRecord));
        while(recordReader.hasMoreRecords())
            root.encode(recordReader);

        // validate the stripe of each field
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>11.0</Value>\n" + 
                "<Unset/>\n" + 
                "<Value>21.0</Value>\n";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 0/*double_field*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>121</Value>\n" + 
                "<Unset/>\n" + 
                "<Value>221</Value>\n" + 
                "<RepeatedValue/>\n" + 
                "<Value>222</Value>\n" + 
                "<RepeatedValue/>\n" + 
                "<Value>223</Value>\n"; 
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 1/*int32_field*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>1311.0</Value>\n" + 
                "<ParentIsUnset>1</ParentIsUnset>\n" + 
                "<Value>2311.0</Value>\n"; 
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 2/*nested_message_optional_field*/, 0/*float_field*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>1312</Value>\n" + 
                "<ParentIsUnset>1</ParentIsUnset>\n" + 
                "<Value>2312</Value>\n"; 
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 2/*nested_message_optional_field*/, 1/*int32_field*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>true</Value>\n" + 
                "<ParentIsUnset>1</ParentIsUnset>\n" + 
                "<Value>false</Value>\n" + 
                "<RepeatedValue/>\n" + 
                "<Value>true</Value>\n"; 
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 2/*nested_message_optional_field*/, 2/*bool_field*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>1411.0</Value>\n" + 
                "<ParentIsUnset>1</ParentIsUnset>\n" + 
                "<Value>2411.0</Value>\n"; 
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 3/*nested_message_required_field*/, 0/*float_field*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>1412</Value>\n" + 
                "<ParentIsUnset>1</ParentIsUnset>\n" + 
                "<Value>2412</Value>\n"; 
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 3/*nested_message_required_field*/, 1/*int32_field*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>false</Value>\n" + 
                "<ParentIsUnset>1</ParentIsUnset>\n" + 
                "<Value>true</Value>\n" + 
                "<RepeatedValue/>\n" + 
                "<Value>false</Value>\n"; 
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 3/*nested_message_required_field*/, 2/*bool_field*/);
        }
        { // locally scoped for sanity
            final String expectedFieldString = 
                "<Value>string151</Value>\n" + 
                "<Unset/>\n" + 
                "<Value>string251</Value>\n" + 
                "<RepeatedValue/>\n" + 
                "<Value>string252</Value>\n";
            assertFieldStripe(fieldStripeWriterFactory, expectedFieldString, schema, 4/*string_field*/);
        }
    }

    // ************************************************************************
    /**
     * Asserts that the {@link IField} identified by the arbitrary path of 0-based
     * indexed fields (from the root) was striped into the specified value in 
     * the specified {@link TestXMLFieldStripeWriterFactory}.
     */
    public static void assertFieldStripe(final TestXMLFieldStripeWriterFactory writerFactory, final String expectedFieldString, final ICompositeType schema, final int... path) {
        final IField field = schema.getField(path);
        assertNotNull(field, "Field");

        final StringWriter writer = writerFactory.getStringWriter(field);
        assertEquals(writer.toString(), expectedFieldString, "'" + field.getName() + "' stripe");
    }
}