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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.agkn.common.exception.DeveloperException;
import net.agkn.common.exception.InvalidDataException;
import net.agkn.common.exception.NoSuchObjectException;
import net.agkn.common.exception.OperationFailedException;
import net.agkn.field_stripe.record.FieldQualifier;
import net.agkn.field_stripe.record.ICompositeType;
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.record.IFieldType;
import net.agkn.field_stripe.record.Path;
import net.agkn.field_stripe.record.PrimitiveType;
import net.agkn.protobuf.parser.MessageDefinition;
import net.agkn.protobuf.parser.MessageDefinition.FieldLabel;
import net.agkn.protobuf.parser.MessageDefinition.FieldType;
import net.agkn.protobuf.parser.MessageMatcher;
import net.agkn.protobuf.parser.ProtobufDefinition;

/**
 * A combination factory and builder for {@link IFieldType field types} (both
 * {@link PrimitiveType primitive} and {@link ICompositeType composite}) from
 * {@link ProtobufDefinition Protobuf definitions}. {@link #createFieldType(List, String)}
 * is used to create a field type from a list of Protobuf definitions for a
 * specific fully-qualified message definition name.
 *
 * @author rgrzywinski
 */
public class ProtobufFieldTypeFactory {
    /**
     * @param  protobufDefinitions the collection of {@link ProtobufDefinition#validate() valid}
     *         (though not necessarily normalized) {@link ProtobufDefinition Protobuf definitions}
     *         in which the specified definition name must exist and from which
     *         all dependencies are resolved. This cannot be <code>null</code>
     *         or empty.
     * @param  definitionName the fully-qualified name of the message definition 
     *         (using "<code>.</code>" qualifiers) for which the {@link IFieldType field type}  
     *         is desired. This cannot be <code>null</code> or blank.
     * @return the {@link ICompositeType composite type} for the specified 
     *         message definition. Each field in the tree is guaranteed to be
     *         its own unique instance.
     * @throws OperationFailedException if circular dependencies were discovered
     *         when resolving all types.
     * @throws NoSuchObjectException if the specified definition name or any of
     *         its dependencies cannot be resolved within the specified list of 
     *         Protobuf definitions. 
     */
    public IFieldType createFieldType(final List<ProtobufDefinition> protobufDefinitions, final String definitionName)
            throws OperationFailedException, NoSuchObjectException {
        // normalize all MessageDefinitions for consistency in the remainder of
        // the construction process
        for(final ProtobufDefinition protobufDefinition : protobufDefinitions)
            for(final MessageDefinition messageDefinition : protobufDefinition.messageDefinitions)
                messageDefinition.normalize();

        // find the MessageDefinition for the specified name
        final MessageMatcher matcher = new MessageMatcher(protobufDefinitions);
        final MessageDefinition messageDefinition = matcher.search(definitionName);
        if(messageDefinition == null) throw new NoSuchObjectException("There is no message that matches the name \"" + definitionName + "\".");
        return build(matcher, messageDefinition);
    }

    // ========================================================================
    /**
     * Builds a {@link IFieldType field type} for the specified {@link MessageDefinition message definition},
     * recursively builds its dependencies and then {@link ProtobufCompositeType#resolve(net.agkn.field_stripe.record.Path) resolves}
     * the {@link Path paths}.  
     * 
     * @param  matcher the {@link MessageMatcher matcher} used to resolve message
     *         definition field types. This cannot be <code>null</code>.
     * @param  messageDefinition the <code>MessageDefinition</code> that defines
     *         the type to be built. This cannot be <code>null</code> and must 
     *         have been validated and normalized.
     * @return The <code>IFieldType</code> for the specified message definition.
     *         This will never be <code>null</code>.
     * @throws OperationFailedException if the definitions are not aligned such
     *         that the type could be produced.
     */
    // NOTE:  this method is broken out simply to provide a more sane interface
    //        into the builder aspect of this factory
    private IFieldType build(final MessageMatcher matcher, final MessageDefinition messageDefinition)
            throws OperationFailedException {
        // recursively build the field types for the message definition
        // NOTE:  by definition a MessageDefinition is represented by a
        //        ICompositeType (rather than, say, a PrimitiveType)
        final Map<MessageDefinition, ProtobufCompositeType> messageToFieldTypeMap = new HashMap<MessageDefinition, ProtobufCompositeType>();
        final ProtobufCompositeType fieldType = build(messageToFieldTypeMap, matcher, messageDefinition);

        // resolve the paths starting at the root
        fieldType.resolve(new Path(/*root path*/));
        
        return fieldType;
    }

    /**
     * Recursively builds {@link IFieldType field types} starting with the 
     * specified {@link MessageDefinition message definition}. As the message 
     * definitions are built (or started to be built) they are added to the 
     * specified map of message definition to field type. There are three states:<p/>
     * 
     * <ul>
     *   <li>a message definition is not present in the map: the message 
     *       definition has not started to be built;</li>
     *   <li>a message definition is present in the map but has a <code>null</code>
     *       value: the message definition has started to be built but a
     *       dependency had to be built;</li>
     *   <li>both a message definition and its field type are present in the map: 
     *       the type is completely built;</li>
     * </ul> 
     * 
     * @param  messageToFieldTypeMap the map of <code>MessageDefinition</code> 
     *         to its built <code>ICompositeType</code> (including all of its 
     *         dependencies). See the above JavaDoc for more information. This
     *         cannot be <code>null</code> though it may be empty.
     * @param  matcher the {@link MessageMatcher matcher} used to resolve message
     *         definition field types. This cannot be <code>null</code>.
     * @param  messageDefinition the <code>MessageDefinition</code> whose field  
     *         type is to be built. This must have already been validated and 
     *         normalized. This cannot be <code>null</code>.
     * @return the completely built {@link IFieldType field type} (specifically
     *         a {@link ProtobufCompositeType}). This will never be <code>null</code>.
     * @throws OperationFailedException if the definitions are not aligned such
     *         that a field type could be produced.
     */
    private ProtobufCompositeType build(final Map<MessageDefinition, ProtobufCompositeType> messageToFieldTypeMap,
                                        final MessageMatcher matcher, final MessageDefinition messageDefinition)
            throws OperationFailedException {
        // if the message definition exists in the map:
        // 1.  if there is a non-null field type (value) then it has been
        //     completely built -- there's nothing to do (other than to clone
        //     the composite type by contract);
        // 2.  if there is a null field type (value) then there is a circular 
        //     dependency as the only way that it would be in the map with a 
        //     null value at this point is if a dependency for the definition
        //     had to be built -- an exception is thrown;
        // NOTE:  even if the message definitions are validated it is still
        //        possible given a collection of independent Protobuf definitions
        //        to create a circular dependency (i.e. the circular dependency
        //        may exist between messages defined in different Protobuf files)
        if(messageToFieldTypeMap.containsKey(messageDefinition)) {
            final ProtobufCompositeType fieldType =  messageToFieldTypeMap.get(messageDefinition);
            if(fieldType != null) return fieldType.clone()/*completely built (and clone by contract)*/;
            throw new InvalidDataException("Circular dependency in message definition \"" + messageDefinition.fqName + "\".");
        } /* else -- the message definition has not yet started being built */

        // the message definition is added to the map (with no value to start)
        // to define that it has been started being built
        // NOTE:  java.util.HashMap allows for null keys
        messageToFieldTypeMap.put(messageDefinition, null/*no field type yet*/)/*by contract*/;

        // build the fields of the type then assemble the fields themselves and
        // finally add it to the map to signify that it is complete
        final List<IField> fields = assembleFields(messageToFieldTypeMap, matcher, messageDefinition);
        final ProtobufCompositeType fieldType = new ProtobufCompositeType(messageDefinition.fqName, fields);
        messageToFieldTypeMap.put(messageDefinition, fieldType)/*finished being built*/;

        return fieldType;
    }

    // ........................................................................
    /**
     * Assembles {@link IField fields} from the specified {@link MessageDefinition message definition}.
     * All {@link IFieldType field types} will have been resolved.
     *
     * @param  messageToFieldTypeMap the map of <code>MessageDefinition</code> 
     *         to its built <code>ICompositeType</code> (including all of its  
     *         dependencies) used to resolve composite fields. This cannot be  
     *         <code>null</code> though it may be empty.
     * @param  matcher the {@link MessageMatcher matcher} used to resolve message
     *         definition field types. This cannot be <code>null</code>.
     * @param  messageDefinition the <code>MessageDefinition</code> whose fields
     *         are to be resolved into <code>IFieldType</code>s. This must have 
     *         already been validated and normalized. This cannot be <code>null</code>.
     * @return the list of <code>IFieldType</code>s for the specified message 
     *         definition. This will never be <code>null</code> though it may 
     *         be empty.
     * @throws OperationFailedException if a message field type was not supported
     *         or was not known.
     */
    private List<IField> assembleFields(final Map<MessageDefinition, ProtobufCompositeType> messageToFieldTypeMap,
                                        final MessageMatcher matcher, final MessageDefinition messageDefinition) 
            throws OperationFailedException {
        // resolve each message definition field
        // NOTE:  the fields have already been normalized by contract
        final List<IField> fields = new ArrayList<IField>(messageDefinition.fields.size());
        for(final MessageDefinition.Field field : messageDefinition.fields) {
            // determine the field type -- either primitive or composite. If
            // the type string matches a primitive then it is a primitive.
            // Otherwise the matcher is used to resolve the type.
            final MessageDefinition.FieldType type = MessageDefinition.FieldType.get(field.typeString);
            final IFieldType resolvedFieldType;
            if(type == null/*was not a primitive (is a composite)*/) {
                final MessageDefinition resolvedDefinition = matcher.search(messageDefinition, field.typeString);
                if(resolvedDefinition == null/*not found*/) throw new NoSuchObjectException("Could not resolve type \"" + field.typeString + "\" for field \"" + field.name + "\" in message definition \"" + messageDefinition.fqName + "\".");
                resolvedFieldType = build(messageToFieldTypeMap, matcher, resolvedDefinition);
            } else { /*type != null -- was a primitive*/
                resolvedFieldType = mapPrimitiveType(type);
            }
            fields.add(new ProtobufField(field.index, mapFieldLabel(field.label), resolvedFieldType, field.name)); 
        }

        return fields;
    }

    // ========================================================================
    /**
     * A convenience method that maps a {@link MessageDefinition.FieldLabel field label}
     * to a {@link FieldQualifier}.
     */
    private static FieldQualifier mapFieldLabel(final FieldLabel fieldLabel) {
        switch(fieldLabel) {
            case REQUIRED:
                return FieldQualifier.ONE;
            case OPTIONAL:
                return FieldQualifier.ZERO_OR_ONE;
            case REPEATED:
                return FieldQualifier.ZERO_OR_MORE;

            default:
                throw new DeveloperException("Unknown Protobuf field label \"" + fieldLabel + "\".");
        }
    }

    /**
     * A convenience method that maps a primitive {@link FieldType field type}
     * to a {@link PrimitiveType}. Conversion is performed according to the
     * <a href="https://developers.google.com/protocol-buffers/docs/proto">Scalar Value Types</a>
     * section.
     * 
     * @param  fieldType the {@link MessageDefinition.FieldType} for which the
     *         {@link PrimitiveType} is desired. This cannot be <code>null</code>.
     * @return the {@link PrimitiveType} for the specified {@link MessageDefinition.FieldType}.
     *         This will never be <code>null</code>.
     */
    private static PrimitiveType mapPrimitiveType(final FieldType fieldType) {
        switch(fieldType) {
            case DOUBLE:
                return PrimitiveType.DOUBLE;
            case FLOAT:
                return PrimitiveType.FLOAT;

            case INT32:
            case UINT32:
            case SINT32:
            case FIXED32:
            case SFIXED32:
                return PrimitiveType.INT;

            case INT64:
            case UINT64:
            case SINT64:
            case FIXED64:
            case SFIXED64:
                return PrimitiveType.LONG;

            case BOOL:
                return PrimitiveType.BOOLEAN;
            case STRING:
                return PrimitiveType.STRING;
            case BYTES:
                throw new UnsupportedOperationException("Protobuf FieldType \"bytes\" is not currently supported.");

            default:
                throw new DeveloperException("Unknown Protobuf FieldType \"" + fieldType + "\".");
        }
    }
}