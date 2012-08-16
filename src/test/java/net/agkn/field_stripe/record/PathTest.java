package net.agkn.field_stripe.record;

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

import java.util.ArrayList;
import java.util.Iterator;

import net.agkn.field_stripe.encode.SchemaBuilder;

import org.testng.annotations.Test;

/**
 * Unit tests for {@link Path}.
 *
 * @author rgrzywinski
 */
public class PathTest {
    /**
     * Creates and validates paths of various lengths.
     */
    @SuppressWarnings("serial")
    @Test
    public void pathTest() {
        // example schema for convenience
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

        // empty path
        { // locally scoped for sanity
            final Path path = new Path(new ArrayList<IField>()/*empty*/);
            assertEquals(path.getDepth(), 0, "Path depth/length");
            assertEquals(path.getParentPath().getDepth(), 0, "Parent path");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ONE), 0, "No parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE), 0, "No parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE), 0, "No parents");
            assertFalse(path.iterator().hasNext(), "No next field");
        }

        // path with only one field
        { // leaf
            final Path path = new Path(new ArrayList<IField>() {{ add(schema.getField(0/*double_field*/)); }});
            assertEquals(path.getDepth(), 1, "Path depth/length");
            assertEquals(path.getParentPath().getDepth(), 0, "Parent path");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ONE), 0, "No parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE), 0, "No parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE), 0, "No parents");
            assertEquals(path.getField(0).getName(), "double_field", "double_field");
            assertFalse(path.getField(0).getType().isComposite(), "Leaf field");
            assertEquals(path.getField(0), schema.getField(0/*double_field*/), "double_field");

            assertPath(path, schema.getField(0/*double_field*/));
        }
        { // node
            final Path path = new Path(new ArrayList<IField>() {{ add(schema.getField(2/*nested_message_repeated_field*/)); }});
            assertEquals(path.getDepth(), 1, "Path depth/length");
            assertEquals(path.getParentPath().getDepth(), 0, "Parent path");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ONE), 0, "No parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE), 0, "No parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE), 0, "No parents");
            assertEquals(path.getField(0).getName(), "nested_message_repeated_field", "nested_message_repeated_field");
            assertTrue(path.getField(0).getType().isComposite(), "Composite (node) field");
            assertEquals(path.getField(0), schema.getField(2/*nested_message_repeated_field*/), "nested_message_repeated_field");

            assertPath(path, schema.getField(2/*nested_message_repeated_field*/));
        }

        // path with two fields
        { // locally scoped for sanity
            final Path path = new Path(new ArrayList<IField>() {{ add(schema.getField(2/*nested_message_repeated_field*/)); add(schema.getField(2/*nested_message_repeated_field*/, 0/*float_field*/)); }});
            assertEquals(path.getDepth(), 2, "Path depth/length");
            assertEquals(path.getParentPath().getDepth(), 1, "Parent path");
            assertEquals(path.getParentPath().getField(0), schema.getField(2/*nested_message_repeated_field*/), "Parent path");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ONE), 0, "Required parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE), 0, "Optional parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE), 1, "Repeated parents");
            assertEquals(path.getField(0).getName(), "nested_message_repeated_field", "nested_message_repeated_field");
            assertTrue(path.getField(0).getType().isComposite(), "Composite (node) field");
            assertEquals(path.getField(0), schema.getField(2/*nested_message_repeated_field*/), "nested_message_repeated_field");
            assertEquals(path.getField(1).getName(), "float_field", "float_field");
            assertFalse(path.getField(1).getType().isComposite(), "Leaf field");
            assertEquals(path.getField(1), schema.getField(2/*nested_message_repeated_field*/, 0/*float_field*/), "float_field");

            assertPath(path, schema.getField(2/*nested_message_repeated_field*/), schema.getField(2/*nested_message_repeated_field*/, 0/*float_field*/));
        }
    }

    /**
     * Compares various subpaths.
     */
    @SuppressWarnings("serial")
    @Test
    public void subpathTest() {
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

        // empty path compared with an empty path
        { // locally scoped for sanity
            final Path pathA = new Path(new ArrayList<IField>()/*empty*/);
            final Path pathB = new Path(new ArrayList<IField>()/*empty*/);

            assertEquals(pathA.getCommonPathLength(pathB), 0, "No common sub-path");
            assertEquals(pathB.getCommonPathLength(pathA), 0, "No common sub-path")/*reverse for sanity*/;

            assertEquals(pathA.getCommonSubpath(pathB).getDepth(), 0, "No common sub-path");
            assertEquals(pathB.getCommonSubpath(pathA).getDepth(), 0, "No common sub-path")/*reverse for sanity*/;

            assertPath(pathA, (IField)null/*root path*/);
            assertPath(pathB, (IField)null/*root path*/);

            assertEquals(pathA, pathB, "Two empty paths are equal");
        }

        // empty path compared with a non-empty path
        { // locally scoped for sanity
            final Path pathA = new Path(new ArrayList<IField>()/*empty*/);
            final Path pathB = new Path(new ArrayList<IField>() {{ add(schema.getField(0/*RecId*/)); }});

            assertEquals(pathA.getCommonPathLength(pathB), 0, "No common sub-path");
            assertEquals(pathB.getCommonPathLength(pathA), 0, "No common sub-path")/*reverse for sanity*/;

            assertEquals(pathA.getCommonSubpath(pathB).getDepth(), 0, "No common sub-path");
            assertEquals(pathB.getCommonSubpath(pathA).getDepth(), 0, "No common sub-path")/*reverse for sanity*/;

            assertFalse(pathA.equals(pathB), "Two paths are not equal");
            assertFalse(pathB.equals(pathA), "Two paths are not equal")/*reverse for sanity*/;
        }
        
        // two non-empty paths with no common sub-path
        { // locally scoped for sanity
            final Path pathA = new Path(new ArrayList<IField>() {{ add(schema.getField(0/*RecId*/)); }});
            final Path pathB = new Path(new ArrayList<IField>() {{ add(schema.getField(2/*Dept*/)); add(schema.getField(2/*Dept*/, 0/*DeptId*/)); }});

            assertEquals(pathA.getCommonPathLength(pathB), 0, "No common sub-path");
            assertEquals(pathB.getCommonPathLength(pathA), 0, "No common sub-path")/*reverse for sanity*/;

            assertEquals(pathA.getCommonSubpath(pathB).getDepth(), 0, "No common sub-path");
            assertEquals(pathB.getCommonSubpath(pathA).getDepth(), 0, "No common sub-path")/*reverse for sanity*/;
        }

        // two non-empty paths with a common sub-path
        { // locally scoped for sanity
            final Path pathA = new Path(new ArrayList<IField>() {{ add(schema.getField(2/*Dept*/)); }});
            final Path pathB = new Path(new ArrayList<IField>() {{ add(schema.getField(2/*Dept*/)); add(schema.getField(2/*Dept*/, 0/*DeptId*/)); }});

            assertEquals(pathA.getCommonPathLength(pathB), 1, "Common sub-path of 'Dept'");
            assertEquals(pathB.getCommonPathLength(pathA), 1, "Common sub-path of 'Dept'")/*reverse for sanity*/;

            assertEquals(pathA.getCommonSubpath(pathB).getDepth(), 1, "Common sub-path of 'Dept'");
            assertPath(pathA.getCommonSubpath(pathB), schema.getField(2/*Dept*/));
            assertEquals(pathB.getCommonSubpath(pathA).getDepth(), 1, "Common sub-path of 'Dept'")/*reverse for sanity*/;
            assertPath(pathB.getCommonSubpath(pathA), schema.getField(2/*Dept*/))/*reverse for sanity*/;

            assertFalse(pathA.equals(pathB), "Two paths are not equal");
            assertFalse(pathB.equals(pathA), "Two paths are not equal")/*reverse for sanity*/;
        }
        { // locally scoped for sanity
            final Path pathA = new Path(new ArrayList<IField>() {{ add(schema.getField(2/*Dept*/)); add(schema.getField(2/*Dept*/, 2/*Loc*/)); add(schema.getField(2/*Dept*/, 2/*Loc*/, 0/*Building*/)); }});
            final Path pathB = new Path(new ArrayList<IField>() {{ add(schema.getField(2/*Dept*/)); add(schema.getField(2/*Dept*/, 2/*Loc*/)); add(schema.getField(2/*Dept*/, 2/*Loc*/, 1/*Floor*/)); }});

            assertEquals(pathA.getCommonPathLength(pathB), 2, "Common sub-path of 'Dept.Loc'");
            assertEquals(pathB.getCommonPathLength(pathA), 2, "Common sub-path of 'Dept.Loc'")/*reverse for sanity*/;

            final Path subPathAB = pathA.getCommonSubpath(pathB);
            assertEquals(subPathAB.getDepth(), 2, "Common sub-path of 'Dept.Loc'");
            assertPath(subPathAB, schema.getField(2/*Dept*/), schema.getField(2/*Dept*/, 2/*Loc*/));
            final Path subPathBA = pathB.getCommonSubpath(pathA);
            assertEquals(subPathBA.getDepth(), 2, "Common sub-path of 'Dept.Loc'")/*reverse for sanity*/;
            assertPath(subPathBA, schema.getField(2/*Dept*/), schema.getField(2/*Dept*/, 2/*Loc*/))/*reverse for sanity*/;

            assertFalse(pathA.equals(pathB), "Two paths are not equal");
            assertFalse(pathB.equals(pathA), "Two paths are not equal")/*reverse for sanity*/;
        }
        { // same path
            final Path pathA = new Path(new ArrayList<IField>() {{ add(schema.getField(2/*Dept*/)); add(schema.getField(2/*Dept*/, 2/*Loc*/)); add(schema.getField(2/*Dept*/, 2/*Loc*/, 0/*Building*/)); }});
            final Path pathB = new Path(new ArrayList<IField>() {{ add(schema.getField(2/*Dept*/)); add(schema.getField(2/*Dept*/, 2/*Loc*/)); add(schema.getField(2/*Dept*/, 2/*Loc*/, 0/*Building*/)); }});

            assertEquals(pathA.getCommonPathLength(pathB), 3, "Common sub-path of 'Dept.Loc.Building'");
            assertEquals(pathB.getCommonPathLength(pathA), 3, "Common sub-path of 'Dept.Loc.Building'")/*reverse for sanity*/;

            final Path subPathAB = pathA.getCommonSubpath(pathB);
            assertEquals(subPathAB.getDepth(), 3, "Common sub-path of 'Dept.Loc.Building'");
            assertPath(subPathAB, schema.getField(2/*Dept*/), schema.getField(2/*Dept*/, 2/*Loc*/), schema.getField(2/*Dept*/, 2/*Loc*/, 0/*Building*/));
            final Path subPathBA = pathB.getCommonSubpath(pathA);
            assertEquals(subPathBA.getDepth(), 3, "Common sub-path of 'Dept.Loc.Building'")/*reverse for sanity*/;
            assertPath(subPathBA, schema.getField(2/*Dept*/), schema.getField(2/*Dept*/, 2/*Loc*/), schema.getField(2/*Dept*/, 2/*Loc*/, 0/*Building*/))/*reverse for sanity*/;

            assertEquals(pathA, pathB, "Two paths are equal");
            assertEquals(pathB, pathA, "Two paths are equal")/*reverse for sanity*/;
        }
    }

    /**
     * Tests various paths for {@link Path#getParentQualifierCount(FieldQualifier)}.
     */
    @Test
    public void parentQualifierCountTest() {
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
            "            repeated int32    int32_field = 2;\n" +
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

        { // required_field.int32_field
            final Path path = schema.getField(1/*required_field*/, 0/*int32_field*/).getPath();
            assertEquals(path.getParentQualifierCount(FieldQualifier.ONE), 1, "Required parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE), 0, "Optional parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE), 0, "Repeated parents");
        }
        { // required_field.int64_field
            final Path path = schema.getField(1/*required_field*/, 1/*int64_field*/).getPath();
            assertEquals(path.getParentQualifierCount(FieldQualifier.ONE), 1, "Required parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE), 0, "Optional parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE), 0, "Repeated parents");
        }
        { // required_field.double_field
            final Path path = schema.getField(1/*required_field*/, 2/*double_field*/).getPath();
            assertEquals(path.getParentQualifierCount(FieldQualifier.ONE), 1, "Required parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE), 0, "Optional parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE), 0, "Repeated parents");
        }

        { // required_field.int32_field
            final Path path = schema.getField(2/*optional_field*/, 0/*int32_field*/).getPath();
            assertEquals(path.getParentQualifierCount(FieldQualifier.ONE), 0, "Required parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE), 1, "Optional parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE), 0, "Repeated parents");
        }
        { // required_field.int64_field
            final Path path = schema.getField(2/*optional_field*/, 1/*int64_field*/).getPath();
            assertEquals(path.getParentQualifierCount(FieldQualifier.ONE), 0, "Required parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE), 1, "Optional parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE), 0, "Repeated parents");
        }
        { // required_field.double_field
            final Path path = schema.getField(2/*optional_field*/, 2/*double_field*/).getPath();
            assertEquals(path.getParentQualifierCount(FieldQualifier.ONE), 0, "Required parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE), 1, "Optional parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE), 0, "Repeated parents");
        }
        
        { // required_field.int32_field
            final Path path = schema.getField(3/*repeated_field*/, 0/*int32_field*/).getPath();
            assertEquals(path.getParentQualifierCount(FieldQualifier.ONE), 0, "Required parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE), 0, "Optional parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE), 1, "Repeated parents");
        }
        { // required_field.int64_field
            final Path path = schema.getField(3/*repeated_field*/, 1/*int64_field*/).getPath();
            assertEquals(path.getParentQualifierCount(FieldQualifier.ONE), 0, "Required parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE), 0, "Optional parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE), 1, "Repeated parents");
        }
        { // required_field.double_field
            final Path path = schema.getField(3/*repeated_field*/, 2/*double_field*/).getPath();
            assertEquals(path.getParentQualifierCount(FieldQualifier.ONE), 0, "Required parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE), 0, "Optional parents");
            assertEquals(path.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE), 1, "Repeated parents");
        }

        // only testing one field for brevity
        { // required_field.*.string_field
            final Path requiredPath = schema.getField(1/*required_field*/, 3/*nested_required_field*/, 0/*string_field*/).getPath();
            assertEquals(requiredPath.getParentQualifierCount(FieldQualifier.ONE), 2, "Required parents");
            assertEquals(requiredPath.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE), 0, "Optional parents");
            assertEquals(requiredPath.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE), 0, "Repeated parents");

            final Path optionalPath = schema.getField(1/*required_field*/, 4/*nested_optional_field*/, 0/*string_field*/).getPath();
            assertEquals(optionalPath.getParentQualifierCount(FieldQualifier.ONE), 1, "Required parents");
            assertEquals(optionalPath.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE), 1, "Optional parents");
            assertEquals(optionalPath.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE), 0, "Repeated parents");
            
            final Path repeatedPath = schema.getField(1/*required_field*/, 5/*nested_repeated_field*/, 0/*string_field*/).getPath();
            assertEquals(repeatedPath.getParentQualifierCount(FieldQualifier.ONE), 1, "Required parents");
            assertEquals(repeatedPath.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE), 0, "Optional parents");
            assertEquals(repeatedPath.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE), 1, "Repeated parents");
        }
        { // optional_field.*.string_field
            final Path requiredPath = schema.getField(2/*optional_field*/, 3/*nested_required_field*/, 0/*string_field*/).getPath();
            assertEquals(requiredPath.getParentQualifierCount(FieldQualifier.ONE), 1, "Required parents");
            assertEquals(requiredPath.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE), 1, "Optional parents");
            assertEquals(requiredPath.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE), 0, "Repeated parents");

            final Path optionalPath = schema.getField(2/*optional_field*/, 4/*nested_optional_field*/, 0/*string_field*/).getPath();
            assertEquals(optionalPath.getParentQualifierCount(FieldQualifier.ONE), 0, "Required parents");
            assertEquals(optionalPath.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE), 2, "Optional parents");
            assertEquals(optionalPath.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE), 0, "Repeated parents");

            final Path repeatedPath = schema.getField(2/*optional_field*/, 5/*nested_repeated_field*/, 0/*string_field*/).getPath();
            assertEquals(repeatedPath.getParentQualifierCount(FieldQualifier.ONE), 0, "Required parents");
            assertEquals(repeatedPath.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE), 1, "Optional parents");
            assertEquals(repeatedPath.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE), 1, "Repeated parents");
        }
        { // repeated_field.*.string_field
            final Path requiredPath = schema.getField(3/*repeated_field*/, 3/*nested_required_field*/, 0/*string_field*/).getPath();
            assertEquals(requiredPath.getParentQualifierCount(FieldQualifier.ONE), 1, "Required parents");
            assertEquals(requiredPath.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE), 0, "Optional parents");
            assertEquals(requiredPath.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE), 1, "Repeated parents");

            final Path optionalPath = schema.getField(3/*repeated_field*/, 4/*nested_optional_field*/, 0/*string_field*/).getPath();
            assertEquals(optionalPath.getParentQualifierCount(FieldQualifier.ONE), 0, "Required parents");
            assertEquals(optionalPath.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE), 1, "Optional parents");
            assertEquals(optionalPath.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE), 1, "Repeated parents");

            final Path repeatedPath = schema.getField(3/*repeated_field*/, 5/*nested_repeated_field*/, 0/*string_field*/).getPath();
            assertEquals(repeatedPath.getParentQualifierCount(FieldQualifier.ONE), 0, "Required parents");
            assertEquals(repeatedPath.getParentQualifierCount(FieldQualifier.ZERO_OR_ONE), 0, "Optional parents");
            assertEquals(repeatedPath.getParentQualifierCount(FieldQualifier.ZERO_OR_MORE), 2, "Repeated parents");
        }
    }

    // ************************************************************************
    /**
     * Asserts that the specified non-<code>null</code> path has the specified
     * list of fields as its path. A null path in this case implies the root
     * path.
     */
    public static void assertPath(final Path path, final IField... fields) {
        final Iterator<IField> pathFields = path.iterator();
        for(final IField field : fields) {
            if(field == null) break/*special 'root' case*/;

            assertTrue(pathFields.hasNext(), "Another field in the path");
            assertEquals(pathFields.next(), field, "The field and the path do not match");
        }
        assertFalse(pathFields.hasNext(), "No more path fields");
    }
}