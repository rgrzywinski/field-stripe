package net.agkn.field_stripe.record.protobuf;

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

/**
 * An implementation of the record interfaces using the <a href="http://code.google.com/p/protostuff/">protostuff</a>
 * parser. The primary entry point is {@link ProtobufFieldTypeFactory#createFieldType(java.util.List, String)}
 * which creates a {@link IFieldType field type} from a collection of 
 * {@link com.dyuproject.protostuff.parser.Proto Protobuf definitions}.<p/>
 * 
 * At this time this implementation cannot be mixed with other implementations.
 * Specifically the {@link Path path} {@link ProtobufCompositeType#resolve(Path) resolution}
 * requires a homogeneous tree. 
 *
 * @author rgrzywinski
 */