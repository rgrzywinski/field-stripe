package net.agkn.field_stripe.stripe;

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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.agkn.field_stripe.decode.FieldStripeDecoderFactory;
import net.agkn.field_stripe.decode.RepeatedParentStripeDecoderTest;
import net.agkn.field_stripe.decode.RootFieldStripeDecoder;
import net.agkn.field_stripe.encode.FieldStripeEncoderFactory;
import net.agkn.field_stripe.encode.RootFieldStripeEncoder;
import net.agkn.field_stripe.encode.SchemaBuilder;
import net.agkn.field_stripe.record.FieldQualifier;
import net.agkn.field_stripe.record.ICompositeType;
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.record.Path;
import net.agkn.field_stripe.record.PrimitiveType;
import net.agkn.field_stripe.record.protobuf.ProtobufCompositeType;
import net.agkn.field_stripe.record.protobuf.ProtobufField;
import net.agkn.field_stripe.record.reader.IRecordReader;
import net.agkn.field_stripe.record.reader.SmartJsonArrayRecordReader;
import net.agkn.field_stripe.record.writer.IRecordWriter;
import net.agkn.field_stripe.record.writer.JsonArrayRecordWriter;

import org.testng.annotations.Test;

/**
 * Unit tests for {@link BinaryVLenFieldStripeReader}. Rather than testing against
 * explicit input binary data, this uses the {@link BinaryVLenFieldStripeWriter writer}
 * to generate valid binary and asserts that it can be read. Where necessary to
 * test a particular case, explicit binary is used. (The assumption is that the
 * writer should be validated first.) 
 *
 * @author rgrzywinski
 */
public class BinaryVLenFieldStripeReaderTest {
    /**
     * Manually builds a {@link ICompositeType schema} that covers all known
     * {@link PrimitiveType primitive types} in a flat structure and assert the 
     * header of each.
     * 
     * @see BinaryVLenFieldStripeWriterTest#allTypesHeaderTest()
     */
    @Test
    @SuppressWarnings("serial")
    public void allTypesHeaderTest() throws Exception {
        // NOTE:  this exploits the Protobuf record implementation for simplicity
        // NOTE:  an explicit schema is needed since Protobuf (*.proto) does
        //        not map to all known types (e.g. short)
        final ProtobufCompositeType schema = new ProtobufCompositeType("Message", new ArrayList<IField>() {{
            add(new ProtobufField(1/*index*/, FieldQualifier.ONE/*required*/, PrimitiveType.BYTE, "byte_field"));
            add(new ProtobufField(2/*index*/, FieldQualifier.ZERO_OR_ONE/*optional*/, PrimitiveType.SHORT, "short_field"));
            add(new ProtobufField(3/*index*/, FieldQualifier.ZERO_OR_MORE/*repeated*/, PrimitiveType.INT, "int_field"));
            add(new ProtobufField(4/*index*/, FieldQualifier.ONE/*required*/, PrimitiveType.LONG, "long_field"));
            add(new ProtobufField(5/*index*/, FieldQualifier.ZERO_OR_ONE/*optional*/, PrimitiveType.FLOAT, "float_field"));
            add(new ProtobufField(6/*index*/, FieldQualifier.ZERO_OR_MORE/*repeated*/, PrimitiveType.DOUBLE, "double_field"));
            add(new ProtobufField(7/*index*/, FieldQualifier.ONE/*required*/, PrimitiveType.BOOLEAN, "boolean_field"));
            add(new ProtobufField(8/*index*/, FieldQualifier.ZERO_OR_ONE/*optional*/, PrimitiveType.STRING, "string_field"));
        }});
        schema.resolve(new Path()/*root*/)/*resolve by contract*/;

        // ....................................................................
        // encoder / output
        final TestBinaryVLenFieldStripeWriterFactory fieldStripeWriterFactory = new TestBinaryVLenFieldStripeWriterFactory(4096/*bufferSize*/);
        final FieldStripeEncoderFactory encoderFactory = new FieldStripeEncoderFactory(fieldStripeWriterFactory);
        final RootFieldStripeEncoder rootEncoder = encoderFactory.createEncoderTree(schema);
        assertNotNull(rootEncoder, "Root field stripe encoder");
        
        // stripe a single value for each field (JSON-array-based)
        final String jsonRecord = "[1,2,[3],4,5.01,[6.01],true,\"string\"]";
        final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(jsonRecord));
        final int recordCount = 1/*only one*/;
        assertTrue(rootEncoder.encode(recordReader), "Record encoded");
        fieldStripeWriterFactory.closeAllWriters()/*by contract*/;

        // ....................................................................
        // decoder / input
        final Map<IField, InputStream> fieldToInputStreamMap = new HashMap<IField, InputStream>() {{
            put(schema.getField(0/*byte_field*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(0/*byte_field*/))));
            put(schema.getField(1/*short_field*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(1/*short_field*/))));
            put(schema.getField(2/*int_field*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(2/*int_field*/))));
            put(schema.getField(3/*long_field*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(3/*long_field*/))));
            put(schema.getField(4/*float_field*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(4/*float_field*/))));
            put(schema.getField(5/*double_field*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(5/*double_field*/))));
            put(schema.getField(6/*boolean_field*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(6/*boolean_field*/))));
            put(schema.getField(7/*string_field*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(7/*string_field*/))));
        }};
        final TestBinaryVLenFieldStripeReaderFactory fieldStripeReaderFactory = new TestBinaryVLenFieldStripeReaderFactory(fieldToInputStreamMap);
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

    // ========================================================================
    // SEE:  RepeatedParentStripeDecoderTest

    /**
     * Tests encoding a nested structure that is one level deep (with repeated 
     * parents) and then decoding all of its field stripes back into the original
     * records.
     * 
     * @see RepeatedParentStripeDecoderTest#oneLevelNestedSchemaTest()
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
        // encoder / output
        final TestBinaryVLenFieldStripeWriterFactory fieldStripeWriterFactory = new TestBinaryVLenFieldStripeWriterFactory(4096/*bufferSize*/);
        final FieldStripeEncoderFactory encoderFactory = new FieldStripeEncoderFactory(fieldStripeWriterFactory);
        final RootFieldStripeEncoder rootEncoder = encoderFactory.createEncoderTree(schema);
        assertNotNull(rootEncoder, "Root field stripe encoder");

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
        fieldStripeWriterFactory.closeAllWriters()/*by contract*/;

        // ....................................................................
        // decoder / input
        @SuppressWarnings("serial")
        final Map<IField, InputStream> fieldToInputStreamMap = new HashMap<IField, InputStream>() {{
            put(schema.getField(0/*double_field*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(0/*double_field*/))));
            put(schema.getField(1/*int64_field*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(1/*int64_field*/))));
            put(schema.getField(2/*nested_message_repeated_field*/, 0/*float_field*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(2/*nested_message_repeated_field*/, 0/*float_field*/))));
            put(schema.getField(2/*nested_message_repeated_field*/, 1/*int32_field*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(2/*nested_message_repeated_field*/, 1/*int32_field*/))));
            put(schema.getField(2/*nested_message_repeated_field*/, 2/*bool_field*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(2/*nested_message_repeated_field*/, 2/*bool_field*/))));
            put(schema.getField(3/*string_field*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(3/*string_field*/))));
        }};
        final TestBinaryVLenFieldStripeReaderFactory fieldStripeReaderFactory = new TestBinaryVLenFieldStripeReaderFactory(fieldToInputStreamMap);
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
     * @see RepeatedParentStripeDecoderTest#twoLevelNestedSchemaTest()
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
        // encoder / output
        final TestBinaryVLenFieldStripeWriterFactory fieldStripeWriterFactory = new TestBinaryVLenFieldStripeWriterFactory(4096/*bufferSize*/);
        final FieldStripeEncoderFactory encoderFactory = new FieldStripeEncoderFactory(fieldStripeWriterFactory);
        final RootFieldStripeEncoder rootEncoder = encoderFactory.createEncoderTree(schema);
        assertNotNull(rootEncoder, "Root field stripe encoder");

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
        fieldStripeWriterFactory.closeAllWriters()/*by contract*/;

        // ....................................................................
        // decoder / input
        @SuppressWarnings("serial")
        final Map<IField, InputStream> fieldToInputStreamMap = new HashMap<IField, InputStream>() {{
            put(schema.getField(0/*RecId*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(0/*RecId*/))));
            put(schema.getField(1/*EmpId*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(1/*EmpId*/))));
            put(schema.getField(2/*Dept*/, 0/*DeptId*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(2/*Dept*/, 0/*DeptId*/))));
            put(schema.getField(2/*Dept*/, 1/*Name*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(2/*Dept*/, 1/*Name*/))));
            put(schema.getField(2/*Dept*/, 2/*Loc*/, 0/*Building*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(2/*Dept*/, 2/*Loc*/, 0/*Building*/))));
            put(schema.getField(2/*Dept*/, 2/*Loc*/, 1/*Floor*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(2/*Dept*/, 2/*Loc*/, 1/*Floor*/))));
            put(schema.getField(3/*BonusRate*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(3/*BonusRate*/))));
            put(schema.getField(4/*FirstName*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(4/*FirstName*/))));
            put(schema.getField(5/*LastName*/), new ByteArrayInputStream(fieldStripeWriterFactory.getByteArray(schema.getField(5/*LastName*/))));
        }};
        final TestBinaryVLenFieldStripeReaderFactory fieldStripeReaderFactory = new TestBinaryVLenFieldStripeReaderFactory(fieldToInputStreamMap);
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