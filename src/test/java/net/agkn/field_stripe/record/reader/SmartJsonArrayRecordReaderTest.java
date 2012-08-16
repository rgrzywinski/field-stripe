package net.agkn.field_stripe.record.reader;

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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.StringReader;

import net.agkn.field_stripe.exception.InvalidDataException;
import net.agkn.field_stripe.record.writer.JsonArrayRecordWriterTest;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link SmartJsonArrayRecordReader} -- a {@link IRecordReader}
 * for JSON-array based records.
 *
 * @author rgrzywinski
 * @see JsonArrayRecordWriterTest
 */
public class SmartJsonArrayRecordReaderTest {
    /**
     * Tests multiple records (flat structure for simplicity).
     */
    @Test
    public void multipleRecordTest() throws Exception {
        // no record
        { // locally scoped for sanity
            final String input = ""/*no record*/;
            final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(input));

            try {
                recordReader.startRecord();
                assert false : "Expected an exception.";
            } catch(final InvalidDataException ide) {
                assertEquals(ide.getLocalizedMessage(), "There are no more records to read.");
            }
        }
        { // locally scoped for sanity
            final String input = ""/*no record*/;
            final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(input));

            assertFalse(recordReader.hasMoreRecords(), "No records to read");
            assertFalse(recordReader.hasMoreRecords(), "No records to read")/*called again for sanity*/;
        }

        // one record with no trailing newline
        { // locally scoped for sanity
            final String input = "[1]";
            final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(input));

            assertTrue(recordReader.hasMoreRecords(), "There is a record to read");
            assertTrue(recordReader.hasMoreRecords(), "There is a record to read")/*called again for sanity*/;

            final MutableBoolean isSet = new MutableBoolean();
            recordReader.startRecord();
                assertEquals(recordReader.readIntField(isSet), 1); assertEquals(isSet.booleanValue(), true/*is set*/);
            recordReader.endRecord();

            assertFalse(recordReader.hasMoreRecords(), "No more records to read");
            assertFalse(recordReader.hasMoreRecords(), "No more records to read")/*called again for sanity*/;
        }
        // one record with trailing newline
        { // locally scoped for sanity
            final String input = "[2]\n"/*trailing newline*/;
            final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(input));

            assertTrue(recordReader.hasMoreRecords(), "There is a record to read");
            assertTrue(recordReader.hasMoreRecords(), "There is a record to read")/*called again for sanity*/;

            final MutableBoolean isSet = new MutableBoolean();
            recordReader.startRecord();
                assertEquals(recordReader.readIntField(isSet), 2); assertEquals(isSet.booleanValue(), true/*is set*/);
            recordReader.endRecord();

            assertFalse(recordReader.hasMoreRecords(), "No more records to read");
            assertFalse(recordReader.hasMoreRecords(), "No more records to read")/*called again for sanity*/;
        }

        // multiple records
        { // locally scoped for sanity
            // CHECK:  is the case "[1]\nnull\n[3]" supported?
            final String input = "[1]\n" +
                                 "[2]\n" +
                                 "[3]";
            final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(input));

            assertTrue(recordReader.hasMoreRecords(), "There is a record to read");
            assertTrue(recordReader.hasMoreRecords(), "There is a record to read")/*called again for sanity*/;

            final MutableBoolean isSet = new MutableBoolean();
            recordReader.startRecord();
                assertEquals(recordReader.readIntField(isSet), 1); assertEquals(isSet.booleanValue(), true/*is set*/);
            recordReader.endRecord();
            assertTrue(recordReader.hasMoreRecords(), "There is a record to read");
            assertTrue(recordReader.hasMoreRecords(), "There is a record to read")/*called again for sanity*/;

            recordReader.startRecord();
                assertEquals(recordReader.readIntField(isSet), 2); assertEquals(isSet.booleanValue(), true/*is set*/);
            recordReader.endRecord();
            assertTrue(recordReader.hasMoreRecords(), "There is a record to read");
            assertTrue(recordReader.hasMoreRecords(), "There is a record to read")/*called again for sanity*/;

            recordReader.startRecord();
                assertEquals(recordReader.readIntField(isSet), 3); assertEquals(isSet.booleanValue(), true/*is set*/);
            recordReader.endRecord();
            assertFalse(recordReader.hasMoreRecords(), "No more records to read");
            assertFalse(recordReader.hasMoreRecords(), "No more records to read")/*called again for sanity*/;
        }
    }

    /**
     * Tests all primitive values in a flat structure. 
     */
    @Test
    public void flatTest() throws Exception {
        // all values
        { // locally scoped for sanity
            final String input = "[1,2,3,4,5.01,6.01,true,\"string\"]";
            final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(input));

            final MutableBoolean isSet = new MutableBoolean();
            recordReader.startRecord();
                assertEquals(recordReader.readByteField(isSet), 1); assertEquals(isSet.booleanValue(), true/*is set*/);
                assertEquals(recordReader.readShortField(isSet), 2); assertEquals(isSet.booleanValue(), true/*is set*/);
                assertEquals(recordReader.readIntField(isSet), 3); assertEquals(isSet.booleanValue(), true/*is set*/);
                assertEquals(recordReader.readLongField(isSet), 4L); assertEquals(isSet.booleanValue(), true/*is set*/);
                assertEquals(recordReader.readFloatField(isSet), 5.01f); assertEquals(isSet.booleanValue(), true/*is set*/);
                assertEquals(recordReader.readDoubleField(isSet), 6.01); assertEquals(isSet.booleanValue(), true/*is set*/);
                assertEquals(recordReader.readBooleanField(isSet), true); assertEquals(isSet.booleanValue(), true/*is set*/);
                assertEquals(recordReader.readStringField(isSet), "string"); assertEquals(isSet.booleanValue(), true/*is set*/);
            recordReader.endRecord();
        }

        // half-null (ensures that isSet is reset each time, etc)
        { // locally scoped for sanity
            final String input = "[1,null,3,null,5.01,null,false,null]";
            final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(input));

            final MutableBoolean isSet = new MutableBoolean();
            recordReader.startRecord();
                assertEquals(recordReader.readByteField(isSet), 1); assertEquals(isSet.booleanValue(), true/*is set*/);
                assertEquals(recordReader.readShortField(isSet), -1/*any value*/); assertEquals(isSet.booleanValue(), false/*is unset*/);
                assertEquals(recordReader.readIntField(isSet), 3); assertEquals(isSet.booleanValue(), true/*is set*/);
                assertEquals(recordReader.readLongField(isSet), -1L/*any value*/); assertEquals(isSet.booleanValue(), false/*is unset*/);
                assertEquals(recordReader.readFloatField(isSet), 5.01f); assertEquals(isSet.booleanValue(), true/*is set*/);
                assertEquals(recordReader.readDoubleField(isSet), -1.0/*any value*/); assertEquals(isSet.booleanValue(), false/*is unset*/);
                assertEquals(recordReader.readBooleanField(isSet), false); assertEquals(isSet.booleanValue(), true/*is set*/);
                assertEquals(recordReader.readStringField(isSet), ""/*any value*/); assertEquals(isSet.booleanValue(), false/*is unset*/);
            recordReader.endRecord();
        }        
        { // locally scoped for sanity
            final String input = "[null,2,null,4,null,6.01,null,\"string\"]";
            final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(input));

            final MutableBoolean isSet = new MutableBoolean();
            recordReader.startRecord();
                assertEquals(recordReader.readByteField(isSet), -1/*any value*/); assertEquals(isSet.booleanValue(), false/*is unset*/);
                assertEquals(recordReader.readShortField(isSet), 2); assertEquals(isSet.booleanValue(), true/*is set*/);
                assertEquals(recordReader.readIntField(isSet), -1/*any value*/); assertEquals(isSet.booleanValue(), false/*is unset*/);
                assertEquals(recordReader.readLongField(isSet), 4L); assertEquals(isSet.booleanValue(), true/*is set*/);
                assertEquals(recordReader.readFloatField(isSet), -1.0f/*any value*/); assertEquals(isSet.booleanValue(), false/*is unset*/);
                assertEquals(recordReader.readDoubleField(isSet), 6.01); assertEquals(isSet.booleanValue(), true/*is set*/);
                assertEquals(recordReader.readBooleanField(isSet), false/*any value*/); assertEquals(isSet.booleanValue(), false/*is unset*/);
                assertEquals(recordReader.readStringField(isSet), "string"); assertEquals(isSet.booleanValue(), true/*is set*/);
            recordReader.endRecord();
        }
    }

    /**
     * Tests all primitive values each in their own array.
     * 
     * @see JsonArrayRecordWriterTest#arrayTest()
     */
    @Test
    public void arrayTest() throws Exception {
        // non-null values
        { // locally scoped for sanity
            final String input = "[[111,112],[121,122],[131,132],[141,142],[15.01,15.02],[16.01,16.02],[true,false],[\"string181\",\"string182\"]]";
            final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(input));

            final MutableBoolean isSet = new MutableBoolean();
            recordReader.startRecord();
                assertTrue(recordReader.startArray());
                    assertTrue(recordReader.hasMoreElements());
                    assertEquals(recordReader.readByteField(isSet), 111); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertTrue(recordReader.hasMoreElements());
                    assertEquals(recordReader.readByteField(isSet), 112); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertFalse(recordReader.hasMoreElements());
                recordReader.endArray();

                assertTrue(recordReader.startArray());
                    assertTrue(recordReader.hasMoreElements());
                    assertEquals(recordReader.readShortField(isSet), 121); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertTrue(recordReader.hasMoreElements());
                    assertEquals(recordReader.readShortField(isSet), 122); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertFalse(recordReader.hasMoreElements());
                recordReader.endArray();

                assertTrue(recordReader.startArray());
                    assertTrue(recordReader.hasMoreElements());
                    assertEquals(recordReader.readIntField(isSet), 131); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertTrue(recordReader.hasMoreElements());
                    assertEquals(recordReader.readIntField(isSet), 132); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertFalse(recordReader.hasMoreElements());
                recordReader.endArray();

                assertTrue(recordReader.startArray());
                    assertTrue(recordReader.hasMoreElements());
                    assertEquals(recordReader.readLongField(isSet), 141L); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertTrue(recordReader.hasMoreElements());
                    assertEquals(recordReader.readLongField(isSet), 142L); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertFalse(recordReader.hasMoreElements());
                recordReader.endArray();

                assertTrue(recordReader.startArray());
                    assertTrue(recordReader.hasMoreElements());
                    assertEquals(recordReader.readFloatField(isSet), 15.01f); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertTrue(recordReader.hasMoreElements());
                    assertEquals(recordReader.readFloatField(isSet), 15.02f); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertFalse(recordReader.hasMoreElements());
                recordReader.endArray();

                assertTrue(recordReader.startArray());
                    assertTrue(recordReader.hasMoreElements());
                    assertEquals(recordReader.readDoubleField(isSet), 16.01); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertTrue(recordReader.hasMoreElements());
                    assertEquals(recordReader.readDoubleField(isSet), 16.02); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertFalse(recordReader.hasMoreElements());
                recordReader.endArray();

                assertTrue(recordReader.startArray());
                    assertTrue(recordReader.hasMoreElements());
                    assertEquals(recordReader.readBooleanField(isSet), true); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertTrue(recordReader.hasMoreElements());
                    assertEquals(recordReader.readBooleanField(isSet), false); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertFalse(recordReader.hasMoreElements());
                recordReader.endArray();

                assertTrue(recordReader.startArray());
                    assertTrue(recordReader.hasMoreElements());
                    assertEquals(recordReader.readStringField(isSet), "string181"); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertTrue(recordReader.hasMoreElements());
                    assertEquals(recordReader.readStringField(isSet), "string182"); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertFalse(recordReader.hasMoreElements());
                recordReader.endArray();
            recordReader.endRecord();
        }

        // null arrays and null array values
        { // locally scoped for sanity
            final String input = "[null,[],[null]]";
            final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(input));

            final MutableBoolean isSet = new MutableBoolean();
            recordReader.startRecord();
                assertFalse(recordReader.startArray());
                    assertFalse(recordReader.hasMoreElements());
                recordReader.endArray();

                assertTrue(recordReader.startArray());
                    assertFalse(recordReader.hasMoreElements());
                recordReader.endArray();

                assertTrue(recordReader.startArray());
                    assertTrue(recordReader.hasMoreElements());
                    assertEquals(recordReader.readIntField(isSet), -1/*any value*/); assertEquals(isSet.booleanValue(), false/*is unset*/);
                    assertFalse(recordReader.hasMoreElements());
                recordReader.endArray();
            recordReader.endRecord();
        }
    }

    /**
     * Tests all primitive values each in their own structure. 
     * 
     * @see JsonArrayRecordWriterTest#structureTest()
     */
    @Test
    public void structureTest() throws Exception {
        { // locally scoped for sanity
            final String input = "[[111,112,113,114,115.01,116.01,true,\"string118\"]]\n" +
                                 "[null]\n";
            final IRecordReader recordReader = new SmartJsonArrayRecordReader(new StringReader(input));

            final MutableBoolean isSet = new MutableBoolean();
            recordReader.startRecord();
                assertTrue(recordReader.startStructure());
                    assertEquals(recordReader.readByteField(isSet), 111); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertEquals(recordReader.readShortField(isSet), 112); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertEquals(recordReader.readIntField(isSet), 113); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertEquals(recordReader.readLongField(isSet), 114L); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertEquals(recordReader.readFloatField(isSet), 115.01f); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertEquals(recordReader.readDoubleField(isSet), 116.01); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertEquals(recordReader.readBooleanField(isSet), true); assertEquals(isSet.booleanValue(), true/*is set*/);
                    assertEquals(recordReader.readStringField(isSet), "string118"); assertEquals(isSet.booleanValue(), true/*is set*/);
                recordReader.endStructure();
            recordReader.endRecord();

            assertTrue(recordReader.hasMoreRecords());

            recordReader.startRecord();
                assertFalse(recordReader.startStructure());
            recordReader.endRecord();

            assertFalse(recordReader.hasMoreRecords());
        }
    }

    // TODO:  array + structure (and structure + array) tests
}