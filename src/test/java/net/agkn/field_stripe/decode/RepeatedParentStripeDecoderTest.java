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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.StringReader;
import java.io.StringWriter;

import net.agkn.field_stripe.encode.FieldStripeEncoderFactory;
import net.agkn.field_stripe.encode.RootFieldStripeEncoder;
import net.agkn.field_stripe.encode.SchemaBuilder;
import net.agkn.field_stripe.record.ICompositeType;
import net.agkn.field_stripe.record.reader.IRecordReader;
import net.agkn.field_stripe.record.reader.SmartJsonArrayRecordReader;
import net.agkn.field_stripe.record.writer.IRecordWriter;
import net.agkn.field_stripe.record.writer.JsonArrayRecordWriter;
import net.agkn.field_stripe.stripe.TestXMLFieldStripeReaderFactory;
import net.agkn.field_stripe.stripe.TestXMLFieldStripeWriterFactory;

import org.testng.annotations.Test;

/**
 * Unit tests for decoding structures that have repeated parents. This makes use 
 * of the XML-based field stripe readers and writers for simplicity and human 
 * readability when debugging. These tests do not validate that the field stripes
 * are encoded properly. That is left up to the various encoder tests.
 *
 * @author rgrzywinski
 */
public class RepeatedParentStripeDecoderTest {
    /**
     * Tests encoding a nested structure that is one level deep (with repeated 
     * parents) and then decoding all of its field stripes back into the original
     * records.
     * 
     * @see FieldStripeDecoderFactoryTest#oneLevelNestedSchemaRepeatedParentTest()
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
            "    repeated NestedMessage nested_message_repeated_field = 3;\n" +
            "    repeated string   string_field = 4;\n" +
            "}\n";
        final SchemaBuilder schemaBuilder = new SchemaBuilder();
        final ICompositeType schema = schemaBuilder.createSchema(protobufText, "Message");

        // ....................................................................
        final TestXMLFieldStripeWriterFactory fieldStripeWriterFactory = new TestXMLFieldStripeWriterFactory();
        final FieldStripeEncoderFactory encoderFactory = new FieldStripeEncoderFactory(fieldStripeWriterFactory);
        final RootFieldStripeEncoder rootEncoder = encoderFactory.createEncoderTree(schema);
        assertNotNull(rootEncoder, "Root field stripe encoder");

        // ....................................................................
        // stripe a series of records (JSON-array-based)
        final String jsonRecord = 
            "[11.0,[121],[[1311.0,1312,[true]]],[\"string141\"]]\n" +
            "[null,[],[],[]]\n"/*all missing or unset fields*/ +
            "[21.0,[],[],[\"string241\"]]\n"/*array exists but has no elements*/ +
            "[31.0,[],[[null,3312,[]]],[]]\n"/*one element of array that has null values*/ +
            "[41.0,[421,422,423],[[4311.0,4312,[false,true]]],[\"string441\",\"string442\"]]\n"/*one full value for each*/ +
            "[51.0,[521,522],[[5311.0,5312,[]],[null,5322,[false,true]],[5331.0,5332,[true]]],[\"string541\"]]"/*more than one value for nested structures*/;
        final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(jsonRecord));
        int recordCount = 0/*none to start*/;
        while(rootEncoder.encode(recordReader))
            recordCount++;

        // ....................................................................
        final TestXMLFieldStripeReaderFactory fieldStripeReaderFactory = TestXMLFieldStripeReaderFactory.createReaderFactory(schema, fieldStripeWriterFactory);
        final FieldStripeDecoderFactory decoderFactory = new FieldStripeDecoderFactory(fieldStripeReaderFactory);
        final RootFieldStripeDecoder rootDecoder = decoderFactory.createDecoderTree(schema);
        assertNotNull(rootEncoder, "Root field stripe decoder");

        // ....................................................................
        // decode the stripes into records and validate the number of records
        // and their contents
        final StringWriter records = new StringWriter();
        final IRecordWriter recordWriter = new JsonArrayRecordWriter(records, false/*no pretty-print*/);
        int recordDecodeCount = 0/*none to start*/;
        while(rootDecoder.decode(recordWriter)) 
            recordDecodeCount++;
        recordWriter.close()/*by contract*/;
        assertEquals(recordDecodeCount, recordCount, "Decoded record count");
        assertEquals(records.toString(), jsonRecord, "Decoded records");
    }

    /**
     * Tests encoding a nested structure that is two level deep (with repeated 
     * parents) and then decoding all of its field stripes back into the original
     * records.
     * 
     * @see FieldStripeDecoderFactoryTest#twoLevelNestedSchemaRepeatedParentTest()
     * @see #twoLevelNestedSchemaTest()
     */
    @Test
    public void twoLevelNestedSchemaTest() throws Exception {
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
        final RootFieldStripeEncoder rootEncoder = encoderFactory.createEncoderTree(schema);
        assertNotNull(rootEncoder, "Root field stripe encoder");

        // SEE:  #twoLevelNestedSchemaTest() for validation of the schema

        // ....................................................................
        // stripe a series of records (JSON-array-based)
        final String jsonRecord =
            // SEE:  RepeatedParentStripeEncoderTest#twoLevelNestedSchemaTest()
            "[11,12,[],1.4,\"first15\",\"last16\"]\n" +
            "[21,22,[[2311,\"name2312\",[]]],2.4,\"first25\",\"last26\"]\n" +
            "[31,32,[[3311,\"name3312\",[[\"building33131\",[]]]]],3.4,\"first35\",\"last36\"]\n" +
            "[41,42,[[4311,\"name4312\",[[\"building43131\",[431321]]]]],4.4,\"first45\",\"last46\"]\n" +
            "[51,52,[[5311,\"name5312\",[[\"building53131\",[5313211]],[\"building53132\",[5313211,5313212]]]]],5.4,\"first55\",\"last56\"]\n" +
            "[61,62,[[6311,\"name6312\",[[\"building63131\",[631321]]]],[6321,\"name6322\",[]]],6.4,\"first65\",\"last66\"]\n" +
            "[71,72,[[7311,\"name7312\",[[\"building73131\",[731321]]]],[7321,\"name7322\",[[\"building73231\",[]]]]],7.4,\"first75\",\"last76\"]\n" +
            "[81,82,[[8311,\"name8312\",[[\"building83131\",[831321]]]],[8321,\"name8322\",[[\"building83231\",[832321]]]]],8.4,\"first85\",\"last86\"]\n" +
            "[91,92,[[9311,\"name9312\",[[\"building93131\",[]],[\"building93132\",[]]]],[9321,null,[[\"building93231\",[932321,932322]]]]],9.4,\"first95\",\"last96\"]\n" +
            // SEE:  RepeatedParentStripeEncoderTest#twoLevelNestedSchemaTest2()
            "[11,12,[],1.4,\"first15\",\"last16\"]\n" +
            "[21,22,[[2311,\"name2312\",[]]],2.4,\"first25\",\"last26\"]\n" +
            "[31,32,[[3311,\"name3312\",[]],[3321,\"name3322\",[]]],3.4,\"first35\",\"last36\"]";
        final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(jsonRecord));
        int recordCount = 0/*none to start*/;
        while(rootEncoder.encode(recordReader))
            recordCount++;

        // ....................................................................
        final TestXMLFieldStripeReaderFactory fieldStripeReaderFactory = TestXMLFieldStripeReaderFactory.createReaderFactory(schema, fieldStripeWriterFactory);
        final FieldStripeDecoderFactory decoderFactory = new FieldStripeDecoderFactory(fieldStripeReaderFactory);
        final RootFieldStripeDecoder rootDecoder = decoderFactory.createDecoderTree(schema);
        assertNotNull(rootEncoder, "Root field stripe decoder");

        // ....................................................................
        // decode the stripes into records and validate the number of records
        // and their contents
        final StringWriter records = new StringWriter();
        final IRecordWriter recordWriter = new JsonArrayRecordWriter(records, false/*no pretty-print*/);
        int recordDecodeCount = 0/*none to start*/;
        while(rootDecoder.decode(recordWriter)) 
            recordDecodeCount++;
        recordWriter.close()/*by contract*/;
        assertEquals(recordDecodeCount, recordCount, "Decoded record count");
        assertEquals(records.toString(), jsonRecord, "Decoded records");
    }
}