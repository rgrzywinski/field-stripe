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

import java.io.StringReader;
import java.util.ArrayList;

import net.agkn.field_stripe.encode.FieldStripeEncoderFactory;
import net.agkn.field_stripe.encode.RepeatedParentStripeEncoderTest;
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

import org.testng.annotations.Test;

/**
 * Unit tests for {@link BinaryVLenFieldStripeWriter}. The writer tests test
 * the expected binary output. This ensures that compatibility with known binary
 * outputs is maintained.
 *
 * @author rgrzywinski
 */
public class BinaryVLenFieldStripeWriterTest {
    /**
     * Manually builds a {@link ICompositeType schema} that covers all known
     * {@link PrimitiveType primitive types} in a flat structure and assert the 
     * header of each.
     * 
     * @see BinaryVLenFieldStripeReaderTest#allTypesHeaderTest()
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
        final TestBinaryVLenFieldStripeWriterFactory fieldStripeWriterFactory = new TestBinaryVLenFieldStripeWriterFactory(4096/*buffer size*/);
        final FieldStripeEncoderFactory encoderFactory = new FieldStripeEncoderFactory(fieldStripeWriterFactory);
        final RootFieldStripeEncoder rootEncoder = encoderFactory.createEncoderTree(schema);
        assertNotNull(rootEncoder, "Root field stripe encoder");
        
        // ....................................................................
        // stripe a single value for each field (JSON-array-based)
        final String jsonRecord = "[1,2,[3],4,5.01,[6.01],true,\"string\"]\n";
        final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(jsonRecord));
        assertTrue(rootEncoder.encode(recordReader), "Record encoded");

        // ....................................................................
        // NOTE:  the headers are written when the field stripe writers are
        //        created (which occurred when FieldStripeEncoderFactory#createEncoderTree()
        //        was called). Close the writers to ensure that all data is
        //        written to the stripe (by contract)
        fieldStripeWriterFactory.closeAllWriters();

        { // byte_field
            final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 0/*byte_field*/);
            assertEquals(fieldStripe.length, 12, "Written bytes");
            assertSlice(fieldStripe,  0/*start*/, 5/*len*/, new byte[] { 'f', 's', 't', 'r', 'p'})/*magic number*/;
            assertSlice(fieldStripe,  5/*start*/, 1/*len*/, new byte[] { 1 })/*version*/;
            assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 0/*required*/ })/*field qualifier*/;
            assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 0/*byte*/ })/*field type*/;
            assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 1 })/*path length*/;
            assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 0 })/*# repeated parents*/;
            assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 0 })/*# optional parents*/;

            assertSlice(fieldStripe, 11/*start*/, 1/*len*/, new byte[] { 2/*zigzag(1)*/ })/*value*/;
        }
        { // short_field
            final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 1/*short_field*/);
            assertEquals(fieldStripe.length, 13, "Written bytes");
            assertSlice(fieldStripe,  0/*start*/, 5/*len*/, new byte[] { 'f', 's', 't', 'r', 'p'})/*magic number*/;
            assertSlice(fieldStripe,  5/*start*/, 1/*len*/, new byte[] { 1 })/*version*/;
            assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 1/*optional*/ })/*field qualifier*/;
            assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 1/*short*/ })/*field type*/;
            assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 1 })/*path length*/;
            assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 0 })/*# repeated parents*/;
            assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 0 })/*# optional parents*/;

            assertSlice(fieldStripe, 11/*start*/, 1/*len*/, new byte[] { 1/*VALUE*/ })/*meta-data*/;
            assertSlice(fieldStripe, 12/*start*/, 1/*len*/, new byte[] { 4/*zigzag(2)*/ })/*value*/;
        }
        { // int_field
            final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 2/*int_field*/);
            assertEquals(fieldStripe.length, 13, "Written bytes");
            assertSlice(fieldStripe,  0/*start*/, 5/*len*/, new byte[] { 'f', 's', 't', 'r', 'p'})/*magic number*/;
            assertSlice(fieldStripe,  5/*start*/, 1/*len*/, new byte[] { 1 })/*version*/;
            assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 2/*repeated*/ })/*field qualifier*/;
            assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 2/*int*/ })/*field type*/;
            assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 1 })/*path length*/;
            assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 0 })/*# repeated parents*/;
            assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 0 })/*# optional parents*/;

            assertSlice(fieldStripe, 11/*start*/, 1/*len*/, new byte[] { 1/*VALUE*/ })/*meta-data*/;
            assertSlice(fieldStripe, 12/*start*/, 1/*len*/, new byte[] { 6/*zigzag(3)*/ })/*value*/;
        }
        { // long_field
            final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 3/*long_field*/);
            assertEquals(fieldStripe.length, 12, "Written bytes");
            assertSlice(fieldStripe,  0/*start*/, 5/*len*/, new byte[] { 'f', 's', 't', 'r', 'p'})/*magic number*/;
            assertSlice(fieldStripe,  5/*start*/, 1/*len*/, new byte[] { 1 })/*version*/;
            assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 0/*required*/ })/*field qualifier*/;
            assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 3/*long*/ })/*field type*/;
            assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 1 })/*path length*/;
            assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 0 })/*# repeated parents*/;
            assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 0 })/*# optional parents*/;

            assertSlice(fieldStripe, 11/*start*/, 1/*len*/, new byte[] { 8/*zigzag(4)*/ })/*value*/;
        }
        { // float_field
            final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 4/*float_field*/);
            assertEquals(fieldStripe.length, 16, "Written bytes");
            assertSlice(fieldStripe,  0/*start*/, 5/*len*/, new byte[] { 'f', 's', 't', 'r', 'p'})/*magic number*/;
            assertSlice(fieldStripe,  5/*start*/, 1/*len*/, new byte[] { 1 })/*version*/;
            assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 1/*optional*/ })/*field qualifier*/;
            assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 4/*float*/ })/*field type*/;
            assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 1 })/*path length*/;
            assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 0 })/*# repeated parents*/;
            assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 0 })/*# optional parents*/;

            assertSlice(fieldStripe, 11/*start*/, 1/*len*/, new byte[] { 1/*VALUE*/ })/*meta-data*/;
            assertSlice(fieldStripe, 12/*start*/, 4/*len*/, new byte[] { (byte)0xEC, 0x51, (byte)0xA0, 0x40 })/*value*/;
        }
        { // double_field
            final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 5/*double_field*/);
            assertEquals(fieldStripe.length, 20, "Written bytes");
            assertSlice(fieldStripe,  0/*start*/, 5/*len*/, new byte[] { 'f', 's', 't', 'r', 'p'})/*magic number*/;
            assertSlice(fieldStripe,  5/*start*/, 1/*len*/, new byte[] { 1 })/*version*/;
            assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 2/*repeated*/ })/*field qualifier*/;
            assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 5/*double*/ })/*field type*/;
            assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 1 })/*path length*/;
            assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 0 })/*# repeated parents*/;
            assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 0 })/*# optional parents*/;

            assertSlice(fieldStripe, 11/*start*/, 1/*len*/, new byte[] { 1/*VALUE*/ })/*meta-data*/;
            assertSlice(fieldStripe, 12/*start*/, 8/*len*/, new byte[] { 0x0A, (byte)0xD7, (byte)0xA3, 0x70, 0x3D, 0x0A, 0x18, 0x40 })/*value*/;
        }
        { // boolean_field
            final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 6/*boolean_field*/);
            assertEquals(fieldStripe.length, 12, "Written bytes");
            assertSlice(fieldStripe,  0/*start*/, 5/*len*/, new byte[] { 'f', 's', 't', 'r', 'p'})/*magic number*/;
            assertSlice(fieldStripe,  5/*start*/, 1/*len*/, new byte[] { 1 })/*version*/;
            assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 0/*required*/ })/*field qualifier*/;
            assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 6/*boolean*/ })/*field type*/;
            assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 1 })/*path length*/;
            assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 0 })/*# repeated parents*/;
            assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 0 })/*# optional parents*/;

            assertSlice(fieldStripe, 11/*start*/, 1/*len*/, new byte[] { 1 })/*value*/;
        }
        { // string_field
            final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 7/*string_field*/);
            assertEquals(fieldStripe.length, 19, "Written bytes");
            assertSlice(fieldStripe,  0/*start*/, 5/*len*/, new byte[] { 'f', 's', 't', 'r', 'p'})/*magic number*/;
            assertSlice(fieldStripe,  5/*start*/, 1/*len*/, new byte[] { 1 })/*version*/;
            assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 1/*optional*/ })/*field qualifier*/;
            assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 7/*string*/ })/*field type*/;
            assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 1 })/*path length*/;
            assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 0 })/*# repeated parents*/;
            assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 0 })/*# optional parents*/;

            assertSlice(fieldStripe, 11/*start*/, 1/*len*/, new byte[] { 1 })/*value*/;
            assertSlice(fieldStripe, 12/*start*/, 6/*len*/, new byte[] { 0x06/*length*/, 's', 't', 'r', 'i', 'n', 'g' })/*value*/;
        }
    }

    /**
     * Tests the path, required offset and optional offsets in the header of 
     * fields from a nested structure.
     */
    @Test
    public void nestedStrutureHeaderTest() throws Exception {
        final String protobufText =
            "package package_name;\n"/*required by Protostuff*/ +
            "message Message {\n" + 
            "    required int64    int64_field = 1;\n" +
            "    message NestedMessage {\n" +
            "        required int32    int32_field = 1;\n" +
            "        optional int64    int64_field = 2;\n" +
            "        repeated double   double_field = 3;\n" +
            "        message NestedNestedMessage {\n" +
            "            required string   string_field = 1;\n" +
            "            optional int32    int32_field = 2;\n" +
            "            repeated int64    int64_field = 3;\n" +
            "        }\n" +
            "        required NestedNestedMessage nested_required_field = 4;\n" +
            "        optional NestedNestedMessage nested_optional_field = 5;\n" +
            "        repeated NestedNestedMessage nested_repeated_field = 6;\n" +
            "    }\n" +
            "    required NestedMessage required_field = 2;\n" +
            "    optional NestedMessage optional_field = 3;\n" +
            "    repeated NestedMessage repeated_field = 4;\n" +
            "}\n";
        final SchemaBuilder schemaBuilder = new SchemaBuilder();
        final ICompositeType schema = schemaBuilder.createSchema(protobufText, "Message");
        
        // ....................................................................
        final TestBinaryVLenFieldStripeWriterFactory fieldStripeWriterFactory = new TestBinaryVLenFieldStripeWriterFactory(4096/*buffer size*/);
        final FieldStripeEncoderFactory encoderFactory = new FieldStripeEncoderFactory(fieldStripeWriterFactory);
        final RootFieldStripeEncoder rootEncoder = encoderFactory.createEncoderTree(schema);
        assertNotNull(rootEncoder, "Root field stripe encoder");
        
        // ....................................................................
        // NOTE:  the headers are written when the field stripe writers are
        //        created (which occurred when FieldStripeEncoderFactory#createEncoderTree()
        //        was called). Close the writers to ensure that all data is
        //        written to the stripe (by contract)
        fieldStripeWriterFactory.closeAllWriters();

        // focuses on the path and qualifier parts of the header
        { // int64_field
            final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 0/*int64_field*/);
            assertEquals(fieldStripe.length, 11, "Written bytes");
            assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 0/*required*/ })/*field qualifier*/;
            assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 3/*long*/ })/*field type*/;
            assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 1 })/*path length*/;
            assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 0 })/*# repeated parents*/;
            assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 0 })/*# optional parents*/;
        }
          { // required_field.int32_field
              final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 1/*required_field*/, 0/*int32_field*/);
              assertEquals(fieldStripe.length, 11, "Written bytes");
              assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 0/*required*/ })/*field qualifier*/;
              assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 2/*int*/ })/*field type*/;
              assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 2 })/*path length*/;
              assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 0 })/*# repeated parents*/;
              assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 0 })/*# optional parents*/;
          }
          { // required_field.int64_field
              final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 1/*required_field*/, 1/*int64_field*/);
              assertEquals(fieldStripe.length, 11, "Written bytes");
              assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 1/*optional*/ })/*field qualifier*/;
              assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 3/*long*/ })/*field type*/;
              assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 2 })/*path length*/;
              assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 0 })/*# repeated parents*/;
              assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 0 })/*# optional parents*/;
          }
          { // required_field.double_field
              final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 1/*required_field*/, 2/*double_field*/);
              assertEquals(fieldStripe.length, 11, "Written bytes");
              assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 2/*repeated*/ })/*field qualifier*/;
              assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 5/*double*/ })/*field type*/;
              assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 2 })/*path length*/;
              assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 0 })/*# repeated parents*/;
              assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 0 })/*# optional parents*/;
          }
            // only testing one field of NestedNestedMessage for brevity
            { // required_field.nested_required_field.string_field
                final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 1/*required_field*/, 3/*nested_required_field*/, 0/*string_field*/);
                assertEquals(fieldStripe.length, 11, "Written bytes");
                assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 0/*required*/ })/*field qualifier*/;
                assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 7/*string*/ })/*field type*/;
                assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 3 })/*path length*/;
                assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 0 })/*# repeated parents*/;
                assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 0 })/*# optional parents*/;
            }
            { // required_field.nested_optional_field.string_field
                final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 1/*required_field*/, 4/*nested_optional_field*/, 0/*string_field*/);
                assertEquals(fieldStripe.length, 11, "Written bytes");
                assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 0/*required*/ })/*field qualifier*/;
                assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 7/*string*/ })/*field type*/;
                assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 3 })/*path length*/;
                assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 0 })/*# repeated parents*/;
                assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 1 })/*# optional parents*/;
            }
            { // required_field.nested_repeated_field.string_field
                final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 1/*required_field*/, 5/*nested_repeated_field*/, 0/*string_field*/);
                assertEquals(fieldStripe.length, 11, "Written bytes");
                assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 0/*required*/ })/*field qualifier*/;
                assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 7/*string*/ })/*field type*/;
                assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 3 })/*path length*/;
                assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 1 })/*# repeated parents*/;
                assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 0 })/*# optional parents*/;
            }
          { // optional_field.int32_field
              final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 2/*optional_field*/, 0/*int32_field*/);
              assertEquals(fieldStripe.length, 11, "Written bytes");
              assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 0/*required*/ })/*field qualifier*/;
              assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 2/*int*/ })/*field type*/;
              assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 2 })/*path length*/;
              assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 0 })/*# repeated parents*/;
              assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 1 })/*# optional parents*/;
          }
          { // optional_field.int64_field
              final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 2/*optional_field*/, 1/*int64_field*/);
              assertEquals(fieldStripe.length, 11, "Written bytes");
              assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 1/*optional*/ })/*field qualifier*/;
              assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 3/*long*/ })/*field type*/;
              assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 2 })/*path length*/;
              assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 0 })/*# repeated parents*/;
              assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 1 })/*# optional parents*/;
          }
          { // optional_field.double_field
              final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 2/*optional_field*/, 2/*double_field*/);
              assertEquals(fieldStripe.length, 11, "Written bytes");
              assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 2/*repeated*/ })/*field qualifier*/;
              assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 5/*double*/ })/*field type*/;
              assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 2 })/*path length*/;
              assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 0 })/*# repeated parents*/;
              assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 1 })/*# optional parents*/;
          }
            // only testing one field of NestedNestedMessage for brevity
            { // optional_field.nested_required_field.int32_field
                final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 2/*optional_field*/, 3/*nested_required_field*/, 1/*int32_field*/);
                assertEquals(fieldStripe.length, 11, "Written bytes");
                assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 1/*optional*/ })/*field qualifier*/;
                assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 2/*int*/ })/*field type*/;
                assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 3 })/*path length*/;
                assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 0 })/*# repeated parents*/;
                assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 1 })/*# optional parents*/;
            }
            { // optional_field.nested_optional_field.int32_field
                final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 2/*optional_field*/, 4/*nested_optional_field*/, 1/*int32_field*/);
                assertEquals(fieldStripe.length, 11, "Written bytes");
                assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 1/*optional*/ })/*field qualifier*/;
                assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 2/*int*/ })/*field type*/;
                assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 3 })/*path length*/;
                assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 0 })/*# repeated parents*/;
                assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 2 })/*# optional parents*/;
            }
            { // optional_field.nested_repeated_field.int32_field
                final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 2/*optional_field*/, 5/*nested_repeated_field*/, 1/*int32_field*/);
                assertEquals(fieldStripe.length, 11, "Written bytes");
                assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 1/*optional*/ })/*field qualifier*/;
                assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 2/*int*/ })/*field type*/;
                assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 3 })/*path length*/;
                assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 1 })/*# repeated parents*/;
                assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 1 })/*# optional parents*/;
            }
          { // repeated_field.int32_field
              final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 3/*repeated_field*/, 0/*int32_field*/);
              assertEquals(fieldStripe.length, 11, "Written bytes");
              assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 0/*required*/ })/*field qualifier*/;
              assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 2/*int*/ })/*field type*/;
              assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 2 })/*path length*/;
              assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 1 })/*# repeated parents*/;
              assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 0 })/*# optional parents*/;
          }
          { // repeated_field.int64_field
              final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 3/*repeated_field*/, 1/*int64_field*/);
              assertEquals(fieldStripe.length, 11, "Written bytes");
              assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 1/*optional*/ })/*field qualifier*/;
              assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 3/*long*/ })/*field type*/;
              assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 2 })/*path length*/;
              assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 1 })/*# repeated parents*/;
              assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 0 })/*# optional parents*/;
          }
          { // repeated_field.double_field
              final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 3/*repeated_field*/, 2/*double_field*/);
              assertEquals(fieldStripe.length, 11, "Written bytes");
              assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 2/*repeated*/ })/*field qualifier*/;
              assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 5/*double*/ })/*field type*/;
              assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 2 })/*path length*/;
              assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 1 })/*# repeated parents*/;
              assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 0 })/*# optional parents*/;
          }
            // only testing one field of NestedNestedMessage for brevity
            { // repeated_field.nested_required_field.int32_field
                final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 3/*repeated_field*/, 3/*nested_required_field*/, 2/*int64_field*/);
                assertEquals(fieldStripe.length, 11, "Written bytes");
                assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 2/*repeated*/ })/*field qualifier*/;
                assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 3/*long*/ })/*field type*/;
                assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 3 })/*path length*/;
                assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 1 })/*# repeated parents*/;
                assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 0 })/*# optional parents*/;
            }
            { // repeated_field.nested_optional_field.int32_field
                final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 3/*repeated_field*/, 4/*nested_optional_field*/, 2/*int64_field*/);
                assertEquals(fieldStripe.length, 11, "Written bytes");
                assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 2/*repeated*/ })/*field qualifier*/;
                assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 3/*long*/ })/*field type*/;
                assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 3 })/*path length*/;
                assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 1 })/*# repeated parents*/;
                assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 1 })/*# optional parents*/;
            }
            { // repeated_field.nested_repeated_field.int32_field
                final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 3/*repeated_field*/, 5/*nested_repeated_field*/, 2/*int64_field*/);
                assertEquals(fieldStripe.length, 11, "Written bytes");
                assertSlice(fieldStripe,  6/*start*/, 1/*len*/, new byte[] { 2/*repeated*/ })/*field qualifier*/;
                assertSlice(fieldStripe,  7/*start*/, 1/*len*/, new byte[] { 3/*long*/ })/*field type*/;
                assertSlice(fieldStripe,  8/*start*/, 1/*len*/, new byte[] { 3 })/*path length*/;
                assertSlice(fieldStripe,  9/*start*/, 1/*len*/, new byte[] { 2 })/*# repeated parents*/;
                assertSlice(fieldStripe, 10/*start*/, 1/*len*/, new byte[] { 0 })/*# optional parents*/;
            }
    }

    // ========================================================================
    /**
     * Tests the meta-data and values emitted for encoding records.
     * 
     * @see RepeatedParentStripeEncoderTest#twoLevelNestedSchemaTest()
     */
    @Test
    public void metaDataValueTest() throws Exception {
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
        final TestBinaryVLenFieldStripeWriterFactory fieldStripeWriterFactory = new TestBinaryVLenFieldStripeWriterFactory(4096/*buffer size*/);
        final FieldStripeEncoderFactory encoderFactory = new FieldStripeEncoderFactory(fieldStripeWriterFactory);
        final RootFieldStripeEncoder rootEncoder = encoderFactory.createEncoderTree(schema);
        assertNotNull(rootEncoder, "Root field stripe encoder");
        
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
        while(rootEncoder.encode(recordReader));
        fieldStripeWriterFactory.closeAllWriters()/*by contract*/;

        // validate the stripe of each field
        { // RecId
            final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 0/*RecId*/);
            assertEquals(fieldStripe.length, 23, "Written bytes");

            assertSlice(fieldStripe, 11/*start*/, 1/*len*/, new byte[] { 0x16/*zigzag(11)*/ })/*value*/;
            assertSlice(fieldStripe, 12/*start*/, 1/*len*/, new byte[] { 0x2A/*zigzag(21)*/ })/*value*/;
            assertSlice(fieldStripe, 13/*start*/, 1/*len*/, new byte[] { 0x3E/*zigzag(31)*/ })/*value*/;
            assertSlice(fieldStripe, 14/*start*/, 1/*len*/, new byte[] { 0x52/*zigzag(41)*/ })/*value*/;
            assertSlice(fieldStripe, 15/*start*/, 1/*len*/, new byte[] { 0x66/*zigzag(51)*/ })/*value*/;
            assertSlice(fieldStripe, 16/*start*/, 1/*len*/, new byte[] { 0x7A/*zigzag(61)*/ })/*value*/;
            assertSlice(fieldStripe, 17/*start*/, 2/*len*/, new byte[] { (byte)0x8E, 0x01/*zigzag(71)*/ })/*value*/;
            assertSlice(fieldStripe, 19/*start*/, 2/*len*/, new byte[] { (byte)0xA2, 0x01/*zigzag(81)*/ })/*value*/;
            assertSlice(fieldStripe, 21/*start*/, 2/*len*/, new byte[] { (byte)0xB6, 0x01/*zigzag(91)*/ })/*value*/;
        }
        // NOTE:  nothing new to be learned from EmpId so skipped
        { // Dept.DeptId
            final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 2/*Dept*/, 0/*DeptId*/);
            assertEquals(fieldStripe.length, 56, "Written bytes");

            assertSlice(fieldStripe, 11/*start*/, 1/*len*/, new byte[] { 0x0C/*1=depth, 4=ParentUnset*/ })/*meta-data*/;
            assertSlice(fieldStripe, 12/*start*/, 3/*len*/, new byte[] { 0x01, (byte)0x8E, 0x24/*zigzag(2311)*/ })/*value*/;
            assertSlice(fieldStripe, 15/*start*/, 3/*len*/, new byte[] { 0x01, (byte)0xDE, 0x33/*zigzag(3311)*/ })/*value*/;
            assertSlice(fieldStripe, 18/*start*/, 3/*len*/, new byte[] { 0x01, (byte)0xAE, 0x43/*zigzag(4311)*/ })/*value*/;
            assertSlice(fieldStripe, 21/*start*/, 3/*len*/, new byte[] { 0x01, (byte)0xFE, 0x52/*zigzag(5311)*/ })/*value*/;
            assertSlice(fieldStripe, 24/*start*/, 3/*len*/, new byte[] { 0x01, (byte)0xCE, 0x62/*zigzag(6311)*/ })/*value*/;
            assertSlice(fieldStripe, 27/*start*/, 1/*len*/, new byte[] { 0x0B/*1=depth, 3=RepeatedParent*/ })/*meta-data*/;
            assertSlice(fieldStripe, 28/*start*/, 3/*len*/, new byte[] { 0x01, (byte)0xE2, 0x62/*zigzag(6321)*/ })/*value*/;
            assertSlice(fieldStripe, 31/*start*/, 3/*len*/, new byte[] { 0x01, (byte)0x9E, 0x72/*zigzag(7311)*/ })/*value*/;
            assertSlice(fieldStripe, 34/*start*/, 1/*len*/, new byte[] { 0x0B/*1=depth, 3=RepeatedParent*/ })/*meta-data*/;
            assertSlice(fieldStripe, 35/*start*/, 3/*len*/, new byte[] { 0x01, (byte)0xB2, 0x72/*zigzag(7321)*/ })/*value*/;
            assertSlice(fieldStripe, 38/*start*/, 4/*len*/, new byte[] { 0x01, (byte)0xEE, (byte)0x81, 0x01/*zigzag(8311)*/ })/*value*/;
            assertSlice(fieldStripe, 42/*start*/, 1/*len*/, new byte[] { 0x0B/*1=depth, 3=RepeatedParent*/ })/*meta-data*/;
            assertSlice(fieldStripe, 43/*start*/, 4/*len*/, new byte[] { 0x01, (byte)0x82, (byte)0x82, 0x01/*zigzag(8321)*/ })/*value*/;
            assertSlice(fieldStripe, 47/*start*/, 4/*len*/, new byte[] { 0x01, (byte)0xBE, (byte)0x91, 0x01/*zigzag(9311)*/ })/*value*/;
            assertSlice(fieldStripe, 51/*start*/, 1/*len*/, new byte[] { 0x0B/*1=depth, 3=RepeatedParent*/ })/*meta-data*/;
            assertSlice(fieldStripe, 52/*start*/, 4/*len*/, new byte[] { 0x01, (byte)0xD2, (byte)0x91, 0x01/*zigzag(9321)*/ })/*value*/;
        }
        { // Dept.Name
            final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 2/*Dept*/, 1/*Name*/);
            assertEquals(fieldStripe.length, 127, "Written bytes");

            assertSlice(fieldStripe,  11/*start*/,  1/*len*/, new byte[] { 0x0C/*1=depth, 4=ParentUnset*/ })/*meta-data*/;
            assertSlice(fieldStripe,  12/*start*/, 10/*len*/, new byte[] { 0x01, 0x08/*length*/, 'n', 'a', 'm', 'e', '2', '3', '1', '2' })/*value*/;
            assertSlice(fieldStripe,  22/*start*/, 10/*len*/, new byte[] { 0x01, 0x08/*length*/, 'n', 'a', 'm', 'e', '3', '3', '1', '2' })/*value*/;
            assertSlice(fieldStripe,  32/*start*/, 10/*len*/, new byte[] { 0x01, 0x08/*length*/, 'n', 'a', 'm', 'e', '4', '3', '1', '2' })/*value*/;
            assertSlice(fieldStripe,  42/*start*/, 10/*len*/, new byte[] { 0x01, 0x08/*length*/, 'n', 'a', 'm', 'e', '5', '3', '1', '2' })/*value*/;
            assertSlice(fieldStripe,  52/*start*/, 10/*len*/, new byte[] { 0x01, 0x08/*length*/, 'n', 'a', 'm', 'e', '6', '3', '1', '2' })/*value*/;
            assertSlice(fieldStripe,  62/*start*/,  1/*len*/, new byte[] { 0x0B/*1=depth, 3=RepeatedParent*/ })/*meta-data*/;
            assertSlice(fieldStripe,  63/*start*/, 10/*len*/, new byte[] { 0x01, 0x08/*length*/, 'n', 'a', 'm', 'e', '6', '3', '2', '2' })/*value*/;
            assertSlice(fieldStripe,  73/*start*/, 10/*len*/, new byte[] { 0x01, 0x08/*length*/, 'n', 'a', 'm', 'e', '7', '3', '1', '2' })/*value*/;
            assertSlice(fieldStripe,  83/*start*/,  1/*len*/, new byte[] { 0x0B/*1=depth, 3=RepeatedParent*/ })/*meta-data*/;
            assertSlice(fieldStripe,  84/*start*/, 10/*len*/, new byte[] { 0x01, 0x08/*length*/, 'n', 'a', 'm', 'e', '7', '3', '2', '2' })/*value*/;
            assertSlice(fieldStripe,  94/*start*/, 10/*len*/, new byte[] { 0x01, 0x08/*length*/, 'n', 'a', 'm', 'e', '8', '3', '1', '2' })/*value*/;
            assertSlice(fieldStripe, 104/*start*/,  1/*len*/, new byte[] { 0x0B/*1=depth, 3=RepeatedParent*/ })/*meta-data*/;
            assertSlice(fieldStripe, 105/*start*/, 10/*len*/, new byte[] { 0x01, 0x08/*length*/, 'n', 'a', 'm', 'e', '8', '3', '2', '2' })/*value*/;
            assertSlice(fieldStripe, 115/*start*/, 10/*len*/, new byte[] { 0x01, 0x08/*length*/, 'n', 'a', 'm', 'e', '9', '3', '1', '2' })/*value*/;
            assertSlice(fieldStripe, 125/*start*/,  1/*len*/, new byte[] { 0x0B/*1=depth, 3=RepeatedParent*/ })/*meta-data*/;
            assertSlice(fieldStripe, 126/*start*/,  1/*len*/, new byte[] { 0x00/*Unset*/ })/*meta-data*/;
        }
        { // Dept.Loc.Building
            final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 2/*Dept*/, 2/*Loc*/, 0/*Building*/);
            assertEquals(fieldStripe.length, 200, "Written bytes");

            assertSlice(fieldStripe,  11/*start*/,  1/*len*/, new byte[] { 0x0C/*1=depth, 4=ParentUnset*/ })/*meta-data*/;
            assertSlice(fieldStripe,  12/*start*/,  1/*len*/, new byte[] { 0x14/*2=depth, 4=ParentUnset*/ })/*meta-data*/;
            assertSlice(fieldStripe,  13/*start*/, 15/*len*/, new byte[] { 0x01, 0x0D/*length*/, 'b', 'u', 'i', 'l', 'd', 'i', 'n', 'g', '3', '3', '1', '3', '1' })/*value*/;
            assertSlice(fieldStripe,  28/*start*/, 15/*len*/, new byte[] { 0x01, 0x0D/*length*/, 'b', 'u', 'i', 'l', 'd', 'i', 'n', 'g', '4', '3', '1', '3', '1' })/*value*/;
            assertSlice(fieldStripe,  43/*start*/, 15/*len*/, new byte[] { 0x01, 0x0D/*length*/, 'b', 'u', 'i', 'l', 'd', 'i', 'n', 'g', '5', '3', '1', '3', '1' })/*value*/;
            assertSlice(fieldStripe,  58/*start*/,  1/*len*/, new byte[] { 0x13/*2=depth, 3=RepeatedParent*/ })/*meta-data*/;
            assertSlice(fieldStripe,  59/*start*/, 15/*len*/, new byte[] { 0x01, 0x0D/*length*/, 'b', 'u', 'i', 'l', 'd', 'i', 'n', 'g', '5', '3', '1', '3', '2' })/*value*/;
            assertSlice(fieldStripe,  74/*start*/, 15/*len*/, new byte[] { 0x01, 0x0D/*length*/, 'b', 'u', 'i', 'l', 'd', 'i', 'n', 'g', '6', '3', '1', '3', '1' })/*value*/;
            assertSlice(fieldStripe,  89/*start*/,  1/*len*/, new byte[] { 0x0B/*1=depth, 3=RepeatedParent*/ })/*meta-data*/;
            assertSlice(fieldStripe,  90/*start*/,  1/*len*/, new byte[] { 0x14/*2=depth, 4=ParentUnset*/ })/*meta-data*/;
            assertSlice(fieldStripe,  91/*start*/, 15/*len*/, new byte[] { 0x01, 0x0D/*length*/, 'b', 'u', 'i', 'l', 'd', 'i', 'n', 'g', '7', '3', '1', '3', '1' })/*value*/;
            assertSlice(fieldStripe, 106/*start*/,  1/*len*/, new byte[] { 0x0B/*1=depth, 3=RepeatedParent*/ })/*meta-data*/;
            assertSlice(fieldStripe, 107/*start*/, 15/*len*/, new byte[] { 0x01, 0x0D/*length*/, 'b', 'u', 'i', 'l', 'd', 'i', 'n', 'g', '7', '3', '2', '3', '1' })/*value*/;
            assertSlice(fieldStripe, 122/*start*/, 15/*len*/, new byte[] { 0x01, 0x0D/*length*/, 'b', 'u', 'i', 'l', 'd', 'i', 'n', 'g', '8', '3', '1', '3', '1' })/*value*/;
            assertSlice(fieldStripe, 137/*start*/,  1/*len*/, new byte[] { 0x0B/*1=depth, 3=RepeatedParent*/ })/*meta-data*/;
            assertSlice(fieldStripe, 138/*start*/, 15/*len*/, new byte[] { 0x01, 0x0D/*length*/, 'b', 'u', 'i', 'l', 'd', 'i', 'n', 'g', '8', '3', '2', '3', '1' })/*value*/;
            assertSlice(fieldStripe, 153/*start*/, 15/*len*/, new byte[] { 0x01, 0x0D/*length*/, 'b', 'u', 'i', 'l', 'd', 'i', 'n', 'g', '9', '3', '1', '3', '1' })/*value*/;
            assertSlice(fieldStripe, 168/*start*/,  1/*len*/, new byte[] { 0x13/*2=depth, 3=RepeatedParent*/ })/*meta-data*/;
            assertSlice(fieldStripe, 169/*start*/, 15/*len*/, new byte[] { 0x01, 0x0D/*length*/, 'b', 'u', 'i', 'l', 'd', 'i', 'n', 'g', '9', '3', '1', '3', '2' })/*value*/;
            assertSlice(fieldStripe, 184/*start*/,  1/*len*/, new byte[] { 0x0B/*1=depth, 3=RepeatedParent*/ })/*meta-data*/;
            assertSlice(fieldStripe, 185/*start*/, 15/*len*/, new byte[] { 0x01, 0x0D/*length*/, 'b', 'u', 'i', 'l', 'd', 'i', 'n', 'g', '9', '3', '2', '3', '1' })/*value*/;
        }
        { // Dept.Loc.Floor
            final byte[] fieldStripe = getFieldStripe(fieldStripeWriterFactory, schema, 2/*Dept*/, 2/*Loc*/, 1/*Floor*/);
            assertEquals(fieldStripe.length, 69, "Written bytes");

            assertSlice(fieldStripe, 11/*start*/, 1/*len*/, new byte[] { 0x0C/*1=depth, 4=ParentUnset*/ })/*meta-data*/;
            assertSlice(fieldStripe, 12/*start*/, 1/*len*/, new byte[] { 0x14/*2=depth, 4=ParentUnset*/ })/*meta-data*/;
            assertSlice(fieldStripe, 13/*start*/, 1/*len*/, new byte[] { 0x00/*Unset*/ })/*meta-data*/;
            assertSlice(fieldStripe, 14/*start*/, 4/*len*/, new byte[] { 0x01, (byte)0xB2, (byte)0xD3, 0x34/*zigzag(431321)*/ })/*value*/;
            assertSlice(fieldStripe, 18/*start*/, 5/*len*/, new byte[] { 0x01, (byte)0xF6, (byte)0xCA, (byte)0x88, 0x05/*zigzag(5313211)*/ })/*value*/;
            assertSlice(fieldStripe, 23/*start*/, 1/*len*/, new byte[] { 0x13/*2=depth, 3=RepeatedParent*/ })/*meta-data*/;
            assertSlice(fieldStripe, 24/*start*/, 5/*len*/, new byte[] { 0x01, (byte)0xF6, (byte)0xCA, (byte)0x88, 0x05/*zigzag(5313211)*/ })/*value*/;
            assertSlice(fieldStripe, 29/*start*/, 1/*len*/, new byte[] { 0x02/*RepeatedValue*/ })/*meta-data*/;
            assertSlice(fieldStripe, 30/*start*/, 5/*len*/, new byte[] { 0x01, (byte)0xF8, (byte)0xCA, (byte)0x88, 0x05/*zigzag(5313212)*/ })/*value*/;
            assertSlice(fieldStripe, 35/*start*/, 4/*len*/, new byte[] { 0x01, (byte)0xB2, (byte)0x88, 0x4D/*zigzag(631321)*/ })/*value*/;
            assertSlice(fieldStripe, 39/*start*/, 1/*len*/, new byte[] { 0x0B/*1=depth, 3=RepeatedParent*/ })/*meta-data*/;
            assertSlice(fieldStripe, 40/*start*/, 1/*len*/, new byte[] { 0x14/*2=depth, 4=ParentUnset*/ })/*meta-data*/;
            assertSlice(fieldStripe, 41/*start*/, 4/*len*/, new byte[] { 0x01, (byte)0xF2, (byte)0xA2, 0x59/*zigzag(731321)*/ })/*value*/;
            assertSlice(fieldStripe, 45/*start*/, 1/*len*/, new byte[] { 0x0B/*1=depth, 3=RepeatedParent*/ })/*meta-data*/;
            assertSlice(fieldStripe, 46/*start*/, 1/*len*/, new byte[] { 0x00/*Unset*/ })/*meta-data*/;
            assertSlice(fieldStripe, 47/*start*/, 4/*len*/, new byte[] { 0x01, (byte)0xB2, (byte)0xBD, 0x65/*zigzag(831321)*/ })/*value*/;
            assertSlice(fieldStripe, 51/*start*/, 1/*len*/, new byte[] { 0x0B/*1=depth, 3=RepeatedParent*/ })/*meta-data*/;
            assertSlice(fieldStripe, 52/*start*/, 4/*len*/, new byte[] { 0x01, (byte)0x82, (byte)0xCD, 0x65/*zigzag(832321)*/ })/*value*/;
            assertSlice(fieldStripe, 56/*start*/, 1/*len*/, new byte[] { 0x00/*Unset*/ })/*meta-data*/;
            assertSlice(fieldStripe, 57/*start*/, 1/*len*/, new byte[] { 0x13/*2=depth, 3=RepeatedParent*/ })/*meta-data*/;
            assertSlice(fieldStripe, 58/*start*/, 1/*len*/, new byte[] { 0x00/*Unset*/ })/*meta-data*/;
            assertSlice(fieldStripe, 59/*start*/, 1/*len*/, new byte[] { 0x0B/*1=depth, 3=RepeatedParent*/ })/*meta-data*/;
            assertSlice(fieldStripe, 60/*start*/, 4/*len*/, new byte[] { 0x01, (byte)0xC2, (byte)0xE7, 0x71/*zigzag(932321)*/ })/*value*/;
            assertSlice(fieldStripe, 64/*start*/, 1/*len*/, new byte[] { 0x02/*RepeatedValue*/ })/*meta-data*/;
            assertSlice(fieldStripe, 65/*start*/, 4/*len*/, new byte[] { 0x01, (byte)0xC4, (byte)0xE7, 0x71/*zigzag(932322)*/ })/*value*/;
        }
        // NOTE:  nothing new to be learned from BonusRate, FirstName or LastName so skipped
    }
    
    // ************************************************************************
    /**
     * Retrieves the field-stripe <code>byte</code> array from the specified
     * {@link TestBinaryVLenFieldStripeWriterFactory} for the field identified
     * by the specified path (with 0-based indexes from the root) from the 
     * specified non-<code>null</code> schema.
     */
    public static byte[] getFieldStripe(final TestBinaryVLenFieldStripeWriterFactory writerFactory, final ICompositeType schema, final int... path) {
        final IField field = schema.getField(path);
        assertNotNull(field, "Field");

        final byte[] fieldStripe = writerFactory.getByteArray(field);
        assertNotNull(fieldStripe, "Field stripe byte array");
        return fieldStripe;
    }

    // ========================================================================
    /**
     * Asserts that the specified slice of the specified non-<code>null</code>
     * <code>byte</code> array has the specified value.
     */
    public static void assertSlice(final byte[] actual, final int startOffset, final int length, final byte[] expected) {
        assertTrue((startOffset < actual.length), "Start offset " + startOffset + " less than length " + actual.length);
        assertTrue(((startOffset + length) <= actual.length), "End offset " + (startOffset + length) + " less than length " + actual.length);
        assertTrue((length <= expected.length), "Length " + length + " less than expected length " + expected.length);
        for(int i=0; i<length; i++)
            assertEquals(actual[startOffset + i], expected[i], "Equal byte values (" + Integer.toHexString(expected[i]) + "," +  Integer.toHexString(actual[startOffset + i]) + ")");
    }
}