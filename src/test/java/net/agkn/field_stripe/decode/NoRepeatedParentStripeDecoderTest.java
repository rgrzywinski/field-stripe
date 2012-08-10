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
public class NoRepeatedParentStripeDecoderTest {
    /**
     * Tests encoding a flat structure with no parents (the simplest case) and
     * then decoding all of its field stripes back into the original records.
     * 
     * @see FieldStripeDecoderFactoryTest#flatSchemaTest()
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
        final RootFieldStripeEncoder rootEncoder = encoderFactory.createEncoderTree(schema);
        assertNotNull(rootEncoder, "Root field stripe encoder");

        // ....................................................................
        // stripe a series of records (JSON-array-based)
        final String jsonRecord = 
            "[11.0,12.0,[131],14,true,[\"string151\"]]\n" +
            "[null,null,[],null,null,[]]\n"/*all unset fields*/ +
            // NOTE:  the JSON-array format doesn't support the output below
            // "[null,null,null,null,null,null]\n"/*all unset fields (using explicit 'null')*/ +
            "[31.0,32.0,[331,332,333],34,false,[\"string351\",\"string352\"]]";
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
     * Tests encoding a nested structure that is one level deep (with no repeated 
     * parents) and then decoding all of its field stripes back into the original
     * records.
     * 
     * @see FieldStripeDecoderFactoryTest#oneLevelNestedSchemaTest()
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
        final RootFieldStripeEncoder rootEncoder = encoderFactory.createEncoderTree(schema);
        assertNotNull(rootEncoder, "Root field stripe encoder");

        // ....................................................................
        // stripe a series of records (JSON-array-based)
        final String jsonRecord = 
            "[11.0,[121],[1311.0,1312,[true]],[1411.0,1412,[false]],[\"string151\"]]\n" +
            "[null,[],null,null,[]]\n"/*all missing or Unset fields*/ +
            "[21.0,[221,222,223],[2311.0,2312,[false,true]],[2411.0,2412,[true,false]],[\"string251\",\"string252\"]]";
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