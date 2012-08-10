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
import static org.testng.Assert.assertNull;

import java.io.StringReader;

import net.agkn.field_stripe.record.FieldQualifier;
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.record.PrimitiveType;
import net.agkn.field_stripe.record.protobuf.ProtobufField;
import net.agkn.field_stripe.stripe.Instruction.Kind;

import org.testng.annotations.Test;

/**
 * Unit tests for {@link XMLFieldStripeReader}.
 *
 * @author rgrzywinski
 */
public class XMLFieldStripeReaderTest {
    /**
     * Validates a sequence of instructions.
     */
    @Test
    public void instructionSequenceTest() throws Exception {
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

        // NOTE:  no attempt was made to ensure that the sequences themselves
        //        are valid. From the perspective of the IFieldStripeReader it
        //        doesn't matter (i.e. any / all sequences are valid)

        // no / empty stripe
        { // locally scoped for sanity
            final String xmlFieldStripe = "";
            final IFieldStripeReader fieldStripeReader = new XMLFieldStripeReader(byteField, new StringReader(xmlFieldStripe));

            assertNull(fieldStripeReader.readInstruction(), "Next instruction");
            assertNull(fieldStripeReader.readInstruction(), "Next instruction")/*call again for sanity*/;
        }

        // all instructions
        { // locally scoped for sanity
            final String xmlFieldStripe = 
                "<Unset/>\n" +
                "<ParentIsUnset>1</ParentIsUnset>\n" +
                "<RepeatedParent>2</RepeatedParent>\n" + 
                "<RepeatedValue/>\n";
            final IFieldStripeReader fieldStripeReader = new XMLFieldStripeReader(byteField, new StringReader(xmlFieldStripe));

            assertInstruction(fieldStripeReader.readInstruction(), Kind.UNSET);
            assertInstruction(fieldStripeReader.readInstruction(), Kind.UNSET_PARENT, 1/*parent depth*/);
            assertInstruction(fieldStripeReader.readInstruction(), Kind.REPEATED_PARENT, 2/*parent depth*/);
            assertInstruction(fieldStripeReader.readInstruction(), Kind.REPEATED_VALUE);

            assertNull(fieldStripeReader.readInstruction(), "No next instruction");
            assertNull(fieldStripeReader.readInstruction(), "No next instruction")/*call again for sanity*/;
        }
        { // byte
            final String xmlFieldStripe = 
                "<Value>1</Value>\n";
            final IFieldStripeReader fieldStripeReader = new XMLFieldStripeReader(byteField, new StringReader(xmlFieldStripe));
            assertValueInstruction(fieldStripeReader.readInstruction(), (byte)1);
            assertNull(fieldStripeReader.readInstruction(), "No next instruction");
            assertNull(fieldStripeReader.readInstruction(), "No next instruction")/*call again for sanity*/;
        }
        { // short
            final String xmlFieldStripe = 
                "<Value>2</Value>\n";
            final IFieldStripeReader fieldStripeReader = new XMLFieldStripeReader(shortField, new StringReader(xmlFieldStripe));
            assertValueInstruction(fieldStripeReader.readInstruction(), (short)2);
            assertNull(fieldStripeReader.readInstruction(), "No next instruction");
            assertNull(fieldStripeReader.readInstruction(), "No next instruction")/*call again for sanity*/;
        }
        { // int
            final String xmlFieldStripe = 
                "<Value>3</Value>\n";
            final IFieldStripeReader fieldStripeReader = new XMLFieldStripeReader(intField, new StringReader(xmlFieldStripe));
            assertValueInstruction(fieldStripeReader.readInstruction(), (int)3);
            assertNull(fieldStripeReader.readInstruction(), "No next instruction");
            assertNull(fieldStripeReader.readInstruction(), "No next instruction")/*call again for sanity*/;
        }
        { // long
            final String xmlFieldStripe = 
                "<Value>4</Value>\n";
            final IFieldStripeReader fieldStripeReader = new XMLFieldStripeReader(longField, new StringReader(xmlFieldStripe));
            assertValueInstruction(fieldStripeReader.readInstruction(), (long)4L);
            assertNull(fieldStripeReader.readInstruction(), "No next instruction");
            assertNull(fieldStripeReader.readInstruction(), "No next instruction")/*call again for sanity*/;
        }
        { // float
            final String xmlFieldStripe = 
                "<Value>5.01</Value>\n";
            final IFieldStripeReader fieldStripeReader = new XMLFieldStripeReader(floatField, new StringReader(xmlFieldStripe));
            assertValueInstruction(fieldStripeReader.readInstruction(), (float)5.01f);
            assertNull(fieldStripeReader.readInstruction(), "No next instruction");
            assertNull(fieldStripeReader.readInstruction(), "No next instruction")/*call again for sanity*/;
        }
        { // double
            final String xmlFieldStripe = 
                "<Value>6.01</Value>\n";
            final IFieldStripeReader fieldStripeReader = new XMLFieldStripeReader(doubleField, new StringReader(xmlFieldStripe));
            assertValueInstruction(fieldStripeReader.readInstruction(), (double)6.01);
            assertNull(fieldStripeReader.readInstruction(), "No next instruction");
            assertNull(fieldStripeReader.readInstruction(), "No next instruction")/*call again for sanity*/;
        }
        { // boolean
            final String xmlFieldStripe = 
                "<Value>true</Value>\n";
            final IFieldStripeReader fieldStripeReader = new XMLFieldStripeReader(booleanField, new StringReader(xmlFieldStripe));
            assertValueInstruction(fieldStripeReader.readInstruction(), (boolean)true);
            assertNull(fieldStripeReader.readInstruction(), "No next instruction");
            assertNull(fieldStripeReader.readInstruction(), "No next instruction")/*call again for sanity*/;
        }
        { // string
            final String xmlFieldStripe = 
                "<Value>string</Value>\n";
            final IFieldStripeReader fieldStripeReader = new XMLFieldStripeReader(stringField, new StringReader(xmlFieldStripe));
            assertValueInstruction(fieldStripeReader.readInstruction(), "string");
            assertNull(fieldStripeReader.readInstruction(), "No next instruction");
            assertNull(fieldStripeReader.readInstruction(), "No next instruction")/*call again for sanity*/;
        }
    }

    // CHECK:  are there any negative tests?

    // ************************************************************************
    /**
     * Asserts that the specified non-<code>null</code> {@link Instruction} has
     * the specified non-<code>null</code> {@link Kind}. 
     */
    public static void assertInstruction(final Instruction instruction, final Kind kind) {
        assertNotNull(instruction, "Instruction");
        assertEquals(instruction.kind, kind, "Instruction kind");
    }

    /**
     * Asserts that the specified non-<code>null</code> {@link Instruction} has
     * the specified non-<code>null</code> {@link Kind} and parent depth. 
     */
    public static void assertInstruction(final Instruction instruction, final Kind kind, final int parentDepth) {
        assertInstruction(instruction, kind);
        assertEquals(instruction.parentDepth, parentDepth, "Instruction parent depth");
    }

    /**
     * Asserts that the specified non-<code>null</code> {@link Kind#VALUE} 
     * {@link Instruction} has the specified value.
     */
    public static void assertValueInstruction(final Instruction instruction, final Object value) {
        assertInstruction(instruction, Kind.VALUE);
        assertEquals(instruction.value, value, "Instruction value");
    }
}