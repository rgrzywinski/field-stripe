package net.agkn.field_stripe.record.writer;

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

import java.io.StringWriter;
import java.util.ArrayList;

import net.agkn.field_stripe.record.FieldQualifier;
import net.agkn.field_stripe.record.ICompositeType;
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.record.PrimitiveType;
import net.agkn.field_stripe.record.protobuf.ProtobufCompositeType;
import net.agkn.field_stripe.record.protobuf.ProtobufField;
import net.agkn.field_stripe.record.reader.SmartJsonArrayRecordReaderTest;

import org.testng.annotations.Test;

/**
 * Unit tests for {@link PGTextRecordWriter}  -- a {@link IRecordWriter} for
 * PG 'text' based records.
 *
 * @author rgrzywinski
 */
public class PGTextRecordWriterTest {
    /**
     * Tests multiple records (flat structure for simplicity).
     */
    @Test
    public void multipleRecordTest() throws Exception {
        // dummy fields for testing
        final IField intField = new ProtobufField(0/*index*/, FieldQualifier.ONE, PrimitiveType.INT, "int_field");

        // empty record
        { // locally scoped for sanity
            final StringWriter writer = new StringWriter();
            final IRecordWriter recordWriter = new PGTextRecordWriter(writer);

            recordWriter.startRecord();
                // NOTE:  intentionally blank
            recordWriter.endRecord();

            recordWriter.close()/*by contract*/;

            final String expectedText = "\n";
            assertEquals(expectedText, writer.toString());
        }

        // one (simple) record
        { // locally scoped for sanity
            final StringWriter writer = new StringWriter();
            final IRecordWriter recordWriter = new PGTextRecordWriter(writer);

            recordWriter.startRecord();
                recordWriter.writeField(intField, 1);
            recordWriter.endRecord();

            recordWriter.close()/*by contract*/;

            final String expectedText = "1\n";
            assertEquals(expectedText, writer.toString());
        }

        // multiple (simple) records
        // one (simple) record
        { // locally scoped for sanity
            final StringWriter writer = new StringWriter();
            final IRecordWriter recordWriter = new PGTextRecordWriter(writer);

            recordWriter.startRecord();
                recordWriter.writeField(intField, 1);
            recordWriter.endRecord();
            recordWriter.startRecord();
                recordWriter.writeUnsetField(intField);
            recordWriter.endRecord();
            recordWriter.startRecord();
                recordWriter.writeField(intField, 3);
            recordWriter.endRecord();

            recordWriter.close()/*by contract*/;

            final String expectedText = 
                "1\n" +
                "\\N\n" +
                "3\n";
            assertEquals(expectedText, writer.toString());
        }
    }

    /**
     * Tests all primitive values in a flat structure. 
     */
    @Test
    public void flatTest() throws Exception {
        // dummy fields for testing (explicit since not all are available via
        // say, Protobuf)
        final IField byteField = new ProtobufField(0/*index*/, FieldQualifier.ONE, PrimitiveType.BYTE, "byte_field");
        final IField shortField = new ProtobufField(1/*index*/, FieldQualifier.ONE, PrimitiveType.SHORT, "short_field");
        final IField intField = new ProtobufField(2/*index*/, FieldQualifier.ONE, PrimitiveType.INT, "int_field");
        final IField longField = new ProtobufField(3/*index*/, FieldQualifier.ONE, PrimitiveType.LONG, "long_field");
        final IField floatField = new ProtobufField(4/*index*/, FieldQualifier.ONE, PrimitiveType.FLOAT, "float_field");
        final IField doubleField = new ProtobufField(5/*index*/, FieldQualifier.ONE, PrimitiveType.DOUBLE, "double_field");
        final IField booleanField = new ProtobufField(6/*index*/, FieldQualifier.ONE, PrimitiveType.BOOLEAN, "boolean_field");
        final IField stringField = new ProtobufField(7/*index*/, FieldQualifier.ONE, PrimitiveType.STRING, "string_field");

        // all values
        { // locally scoped for sanity
            final StringWriter writer = new StringWriter();
            final IRecordWriter recordWriter = new PGTextRecordWriter(writer);

            recordWriter.startRecord();
                recordWriter.writeField(byteField, (byte)1);
                recordWriter.writeField(shortField, (short)2);
                recordWriter.writeField(intField, (int)3);
                recordWriter.writeField(longField, (long)4L);
                recordWriter.writeField(floatField, (float)5.01f);
                recordWriter.writeField(doubleField, (double)6.01);
                recordWriter.writeField(booleanField, (boolean)true);
                recordWriter.writeField(stringField, "string");
            recordWriter.endRecord();

            recordWriter.close()/*by contract*/;

            final String expectedText = "1,2,3,4,5.01,6.01,true,string\n";
            assertEquals(expectedText, writer.toString());
        }

        // some null
        { // locally scoped for sanity
            final StringWriter writer = new StringWriter();
            final IRecordWriter recordWriter = new PGTextRecordWriter(writer);

            recordWriter.startRecord();
                recordWriter.writeField(byteField, (byte)1);
                recordWriter.writeUnsetField(shortField);
                recordWriter.writeField(intField, (int)3);
                recordWriter.writeUnsetField(longField);
                recordWriter.writeField(floatField, (float)5.01f);
                recordWriter.writeUnsetField(doubleField);
                recordWriter.writeField(booleanField, (boolean)true);
                recordWriter.writeUnsetField(stringField);
            recordWriter.endRecord();

            recordWriter.close()/*by contract*/;

            final String expectedText = "1,\\N,3,\\N,5.01,\\N,true,\\N\n";
            assertEquals(expectedText, writer.toString());
        }
    }

    /**
     * Tests all primitive values each in their own array. 
     * 
     * @see SmartJsonArrayRecordReaderTest#arrayTest()
     */
    @Test
    public void arrayTest() throws Exception {
        // dummy fields for testing (explicit since not all are available via
        // say, Protobuf)
        // NOTE:  the cardinalities have been updated to match (but they're not
        //        actually used in the writer)
        final IField byteField = new ProtobufField(0/*index*/, FieldQualifier.ZERO_OR_MORE, PrimitiveType.BYTE, "byte_field");
        final IField shortField = new ProtobufField(1/*index*/, FieldQualifier.ZERO_OR_MORE, PrimitiveType.SHORT, "short_field");
        final IField intField = new ProtobufField(2/*index*/, FieldQualifier.ZERO_OR_MORE, PrimitiveType.INT, "int_field");
        final IField longField = new ProtobufField(3/*index*/, FieldQualifier.ZERO_OR_MORE, PrimitiveType.LONG, "long_field");
        final IField floatField = new ProtobufField(4/*index*/, FieldQualifier.ZERO_OR_MORE, PrimitiveType.FLOAT, "float_field");
        final IField doubleField = new ProtobufField(5/*index*/, FieldQualifier.ZERO_OR_MORE, PrimitiveType.DOUBLE, "double_field");
        final IField booleanField = new ProtobufField(6/*index*/, FieldQualifier.ZERO_OR_MORE, PrimitiveType.BOOLEAN, "boolean_field");
        final IField stringField = new ProtobufField(7/*index*/, FieldQualifier.ZERO_OR_MORE, PrimitiveType.STRING, "string_field");

        // non-null values
        // SEE:  matches SmartJsonArrayRecordReaderTest#arrayTest()
        { // locally scoped for sanity
            final StringWriter writer = new StringWriter();
            final IRecordWriter recordWriter = new PGTextRecordWriter(writer);

            recordWriter.startRecord();
                recordWriter.startArray(byteField);
                    recordWriter.writeField(byteField, (byte)111);
                    recordWriter.writeField(byteField, (byte)112);
                recordWriter.endArray();

                recordWriter.startArray(byteField);
                    recordWriter.writeField(shortField, (short)121);
                    recordWriter.writeField(shortField, (short)122);
                recordWriter.endArray();

                recordWriter.startArray(byteField);
                    recordWriter.writeField(intField, (int)131);
                    recordWriter.writeField(intField, (int)132);
                recordWriter.endArray();

                recordWriter.startArray(byteField);
                    recordWriter.writeField(longField, (long)141L);
                    recordWriter.writeField(longField, (long)142L);
                recordWriter.endArray();

                recordWriter.startArray(byteField);
                    recordWriter.writeField(floatField, (float)15.01f);
                    recordWriter.writeField(floatField, (float)15.02f);
                recordWriter.endArray();

                recordWriter.startArray(byteField);
                    recordWriter.writeField(doubleField, (double)16.01);
                    recordWriter.writeField(doubleField, (double)16.02);
                recordWriter.endArray();

                recordWriter.startArray(byteField);
                    recordWriter.writeField(booleanField, (boolean)true);
                    recordWriter.writeField(booleanField, (boolean)false);
                recordWriter.endArray();

                recordWriter.startArray(byteField);
                    recordWriter.writeField(stringField, "string181");
                    recordWriter.writeField(stringField, "string182");
                recordWriter.endArray();
            recordWriter.endRecord();

            recordWriter.close()/*by contract*/;

            final String expectedText = "{111\\,112},{121\\,122},{131\\,132},{141\\,142},{15.01\\,15.02},{16.01\\,16.02},{true\\,false},{string181\\,string182}\n";
            assertEquals(expectedText, writer.toString());
        }

        // null arrays and null array values
        // SEE:  matches SmartJsonArrayRecordReaderTest#arrayTest()
        { // locally scoped for sanity
            final StringWriter writer = new StringWriter();
            final IRecordWriter recordWriter = new PGTextRecordWriter(writer);

            recordWriter.startRecord();
                recordWriter.writeUnsetField(byteField);

                recordWriter.startArray(shortField);
                    // NOTE:  intentionally empty
                recordWriter.endArray();

                // NOTE:  this isn't used in field-striping (specifically, one
                //        cannot have unset / null fields *within* an array)
                recordWriter.startArray(intField);
                    recordWriter.writeUnsetField(intField);
                recordWriter.endArray();
            recordWriter.endRecord();

            recordWriter.close()/*by contract*/;

            final String expectedText = "\\N,{},{NULL}\n";
            assertEquals(expectedText, writer.toString());
        }        
    }

    /**
     * Tests all primitive values each in their own structure. 
     * 
     * @see SmartJsonArrayRecordReaderTest#structureTest()
     */
    @SuppressWarnings("serial")
    @Test
    public void structureTest() throws Exception {
        // dummy fields for testing (explicit since not all are available via
        // say, Protobuf)
        final IField byteField = new ProtobufField(0/*index*/, FieldQualifier.ZERO_OR_ONE, PrimitiveType.BYTE, "byte_field");
        final IField shortField = new ProtobufField(1/*index*/, FieldQualifier.ZERO_OR_ONE, PrimitiveType.SHORT, "short_field");
        final IField intField = new ProtobufField(2/*index*/, FieldQualifier.ZERO_OR_ONE, PrimitiveType.INT, "int_field");
        final IField longField = new ProtobufField(3/*index*/, FieldQualifier.ZERO_OR_ONE, PrimitiveType.LONG, "long_field");
        final IField floatField = new ProtobufField(4/*index*/, FieldQualifier.ZERO_OR_ONE, PrimitiveType.FLOAT, "float_field");
        final IField doubleField = new ProtobufField(5/*index*/, FieldQualifier.ZERO_OR_ONE, PrimitiveType.DOUBLE, "double_field");
        final IField booleanField = new ProtobufField(6/*index*/, FieldQualifier.ZERO_OR_ONE, PrimitiveType.BOOLEAN, "boolean_field");
        final IField stringField = new ProtobufField(7/*index*/, FieldQualifier.ZERO_OR_ONE, PrimitiveType.STRING, "string_field");
        final ICompositeType compositeType = new ProtobufCompositeType("Structure", new ArrayList<IField>() {{ add(byteField); add(shortField); add(intField); add(longField); add(floatField); add(doubleField); add(booleanField); add(stringField); }});
        final IField structureField = new ProtobufField(0/*index*/, FieldQualifier.ZERO_OR_ONE, compositeType, "structure_field");
        
        // SEE:  matches SmartJsonArrayRecordReaderTest#structureTest()
        { // locally scoped for sanity
            final StringWriter writer = new StringWriter();
            final IRecordWriter recordWriter = new PGTextRecordWriter(writer);

            recordWriter.startRecord();
                recordWriter.startStructure(structureField);
                    recordWriter.writeField(byteField, (byte)111);
                    recordWriter.writeField(shortField, (short)112);
                    recordWriter.writeField(intField, (int)113);
                    recordWriter.writeField(longField, (long)114L);
                    recordWriter.writeField(floatField, (float)115.01f);
                    recordWriter.writeField(doubleField, (double)116.01);
                    recordWriter.writeField(booleanField, (boolean)true);
                    recordWriter.writeField(stringField, "string118");
                recordWriter.endStructure();
            recordWriter.endRecord();
            recordWriter.startRecord();
                recordWriter.writeUnsetField(structureField);
            recordWriter.endRecord();

            recordWriter.close()/*by contract*/;

            final String expectedText = "(111\\,112\\,113\\,114\\,115.01\\,116.01\\,true\\,string118)\n" +
                                        "\\N\n";
            assertEquals(expectedText, writer.toString());
        }
    }

    // TODO:  array + structure (and structure + array) tests

    // TODO:  negative tests (missing end, etc)
}