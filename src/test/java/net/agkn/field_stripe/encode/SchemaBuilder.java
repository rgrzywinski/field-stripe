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

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import net.agkn.field_stripe.record.ICompositeType;
import net.agkn.field_stripe.record.IFieldType;
import net.agkn.field_stripe.record.protobuf.ProtobufFieldTypeFactory;
import net.agkn.field_stripe.record.protobuf.StringProtoLoader;

import com.dyuproject.protostuff.parser.Proto;

/**
 * A convenience class for building a {@link ICompositeType} from some IDL
 * (e.g. Protobuf).
 *
 * @author rgrzywinski
 */
public class SchemaBuilder {
    /**
     * Creates (and asserts that there were no errors while creating) a 
     * ({@link ICompositeType composite-type}) {@link IFieldType schema} from 
     * the specified Protobuf definition.
     */
    public ICompositeType createSchema(final String protobufText, final String messageName) {
        try {
            final Proto protobufDefinition = StringProtoLoader.parseProto(protobufText);
            final List<Proto> protobufDefinitions = Collections.singletonList(protobufDefinition);

            final ProtobufFieldTypeFactory fieldTypeFactory = new ProtobufFieldTypeFactory();
            final IFieldType fieldType = fieldTypeFactory.createFieldType(protobufDefinitions, messageName);
            assertNotNull(fieldType, "Field type (schema)");
            assertTrue(fieldType instanceof ICompositeType, "Composite field type (schema)");

            return (ICompositeType)fieldType;
        } catch(final Exception e) {
            assert false : "Failed to create schema from Protobuf. " + e.getLocalizedMessage();
            return null/*not reached*/;
        }
    }
}