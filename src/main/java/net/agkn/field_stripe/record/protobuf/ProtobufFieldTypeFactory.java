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

import net.agkn.field_stripe.exception.DeveloperException;
import net.agkn.field_stripe.exception.InvalidDataException;
import net.agkn.field_stripe.exception.NoSuchObjectException;
import net.agkn.field_stripe.exception.OperationFailedException;
import net.agkn.field_stripe.record.FieldQualifier;
import net.agkn.field_stripe.record.ICompositeType;
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.record.IFieldType;
import net.agkn.field_stripe.record.Path;
import net.agkn.field_stripe.record.PrimitiveType;

import com.dyuproject.protostuff.parser.Field;
import com.dyuproject.protostuff.parser.Field.Modifier;
import com.dyuproject.protostuff.parser.Message;
import com.dyuproject.protostuff.parser.MessageField;
import com.dyuproject.protostuff.parser.Proto;

/**
 * A combination factory and builder for {@link IFieldType field types} (both
 * {@link PrimitiveType primitive} and {@link ICompositeType composite}) from
 * {@link Proto Protobuf definitions}. {@link #createFieldType(List, String)}
 * is used to create a field type from a list of Protobuf definitions for a
 * specific fully-qualified message definition name.
 *
 * @author rgrzywinski
 */
public class ProtobufFieldTypeFactory {
    /**
     * @param  protobufDefinitions the collection of {@link Proto Protobuf definitions}
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
    // NOTE:  theoretically if the Proto's are built there should not be circular
    //        dependencies. (Though an actual test of Protostuff's parser has
    //        not been performed to make sure.)
    public IFieldType createFieldType(final List<Proto> protobufDefinitions, final String definitionName)
            throws OperationFailedException, NoSuchObjectException {
        // find the Message for the specified name by doing a lookup on each
        // Protobuf definition
        Message messageDefinition = null/*none to start*/;
        for(final Proto protobufDefinition : protobufDefinitions) {
            if((messageDefinition = LookupUtil.lookupMessage(protobufDefinition, definitionName)) != null)
                break/*found!*/;
            /* else -- not a match in this Protobuf, keep looking */
        }
        if(messageDefinition == null) throw new NoSuchObjectException("There is no message that matches the name \"" + definitionName + "\".");
        return build(messageDefinition);
    }

    // ========================================================================
    /**
     * Builds a {@link IFieldType field type} for the specified {@link Message message definition},
     * recursively builds its dependencies and then {@link ProtobufCompositeType#resolve(net.agkn.field_stripe.record.Path) resolves}
     * the {@link Path paths}.  
     * 
     * @param  messageDefinition the <code>Message</code> that defines the type
     *         to be built. This cannot be <code>null</code>.
     * @return The <code>IFieldType</code> for the specified message definition.
     *         This will never be <code>null</code>.
     * @throws OperationFailedException if the definitions are not aligned such
     *         that the type could be produced.
     */
    // NOTE:  this method is broken out simply to provide a more sane interface
    //        into the builder aspect of this factory
    private IFieldType build(final Message messageDefinition)
            throws OperationFailedException {
        // recursively build the field types for the message definition
        // NOTE:  by definition a Message is represented by a ICompositeType 
        //        (rather than, say, a PrimitiveType)
        final Map<Message, ProtobufCompositeType> messageToFieldTypeMap = new HashMap<Message, ProtobufCompositeType>();
        final ProtobufCompositeType fieldType = build(messageToFieldTypeMap, messageDefinition);

        // resolve the paths starting at the root
        fieldType.resolve(new Path(/*root path*/));
        
        return fieldType;
    }

    /**
     * Recursively builds {@link IFieldType field types} starting with the 
     * specified {@link Message message definition}. As the message definitions
     * are built (or started to be built) they are added to the specified map 
     * of message definition to field type. There are three states:<p/>
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
     * @param  messageToFieldTypeMap the map of <code>Message</code> to its
     *         built <code>ICompositeType</code> (including all of its dependencies). 
     *         See the above JavaDoc for more information. This cannot be 
     *         <code>null</code> though it may be empty.
     * @param  messageDefinition the <code>Message</code> whose field type is 
     *         to be built. This must have already been validated and normalized. 
     *         This cannot be <code>null</code>.
     * @return the completely built {@link IFieldType field type} (specifically
     *         a {@link ProtobufCompositeType}). This will never be <code>null</code>.
     * @throws OperationFailedException if the definitions are not aligned such
     *         that a field type could be produced.
     */
    private ProtobufCompositeType build(final Map<Message, ProtobufCompositeType> messageToFieldTypeMap,
                                        final Message messageDefinition)
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
            throw new InvalidDataException("Circular dependency in message definition \"" + messageDefinition.getFullName() + "\".");
        } /* else -- the message definition has not yet started being built */

        // the message definition is added to the map (with no value to start)
        // to define that it has been started being built
        // NOTE:  java.util.HashMap allows for null keys
        messageToFieldTypeMap.put(messageDefinition, null/*no field type yet*/)/*by contract*/;

        // build the fields of the type then assemble the fields themselves and
        // finally add it to the map to signify that it is complete
        final List<IField> fields = assembleFields(messageToFieldTypeMap, messageDefinition);
        final ProtobufCompositeType fieldType = new ProtobufCompositeType(messageDefinition.getFullName(), fields);
        messageToFieldTypeMap.put(messageDefinition, fieldType)/*finished being built*/;

        return fieldType;
    }

    // ........................................................................
    /**
     * Assembles {@link IField fields} from the specified {@link Message message definition}.
     *
     * @param  messageToFieldTypeMap the map of <code>Message</code> to its
     *         built <code>ICompositeType</code> (including all of its dependencies)
     *         used to resolve composite fields. This cannot be <code>null</code>
     *         though it may be empty.
     * @param  messageDefinition the <code>Message</code> whose fields are to
     *         be resolved into <code>IFieldType</code>s. This cannot be <code>null</code>.
     * @return the list of <code>IFieldType</code>s for the specified message 
     *         definition. This will never be <code>null</code> though it may 
     *         be empty.
     * @throws OperationFailedException if a message field type was not supported
     *         or was not known.
     */
    private List<IField> assembleFields(final Map<Message, ProtobufCompositeType> messageToFieldTypeMap,
                                        final Message messageDefinition) 
            throws OperationFailedException {
        // resolve each message definition field
        final List<IField> fields = new ArrayList<IField>(messageDefinition.getFieldCount());
        for(final Field<?> field : messageDefinition.getFields()) {
            final IFieldType resolvedFieldType;
            if(field.isMessageField()) {/*composite*/
                final MessageField messageField = (MessageField)field/*by #isMessageField() definition*/;
                resolvedFieldType = build(messageToFieldTypeMap, messageField.getMessage());
            } else { /*primitive*/
                resolvedFieldType = mapPrimitiveType(field.getClass());
            }
            fields.add(new ProtobufField(field.getNumber()/*index*/, mapFieldModifier(field.getModifier()), resolvedFieldType, field.getName())); 
        }

        return fields;
    }

    // ========================================================================
    /**
     * A convenience method that maps a {@link Field.Modifier field modifier}
     * to a {@link FieldQualifier}.
     */
    private static FieldQualifier mapFieldModifier(final Modifier fieldModifier) {
        switch(fieldModifier) {
            case REQUIRED:
                return FieldQualifier.ONE;
            case OPTIONAL:
                return FieldQualifier.ZERO_OR_ONE;
            case REPEATED:
                return FieldQualifier.ZERO_OR_MORE;

            default:
                throw new DeveloperException("Unknown Protobuf field modifier \"" + fieldModifier + "\".");
        }
    }

    /**
     * A convenience method that maps a primitive field type to a {@link PrimitiveType}. 
     * Conversion is performed according to the
     * <a href="https://developers.google.com/protocol-buffers/docs/proto">Scalar Value Types</a>
     * section.
     * 
     * @param  fieldType the type of field for which the {@link PrimitiveType} 
     *         is desired. This cannot be <code>null</code>.
     * @return the {@link PrimitiveType} for the specified field type. This 
     *         will never be <code>null</code>.
     */
    private static PrimitiveType mapPrimitiveType(final Class<?> fieldType) {
        if(Field.Double.class.isAssignableFrom(fieldType))
            return PrimitiveType.DOUBLE;
        if(Field.Float.class.isAssignableFrom(fieldType))
            return PrimitiveType.FLOAT;

        if(Field.Int32.class.isAssignableFrom(fieldType) ||
           Field.UInt32.class.isAssignableFrom(fieldType) ||
           Field.SInt32.class.isAssignableFrom(fieldType) ||
           Field.Fixed32.class.isAssignableFrom(fieldType) ||
           Field.SFixed32.class.isAssignableFrom(fieldType) )
            return PrimitiveType.INT;

        if(Field.Int64.class.isAssignableFrom(fieldType) ||
           Field.UInt64.class.isAssignableFrom(fieldType) ||
           Field.SInt64.class.isAssignableFrom(fieldType) ||
           Field.Fixed64.class.isAssignableFrom(fieldType) ||
           Field.SFixed64.class.isAssignableFrom(fieldType) )
            return PrimitiveType.LONG;

        if(Field.Bool.class.isAssignableFrom(fieldType))
            return PrimitiveType.BOOLEAN;
        if(Field.String.class.isAssignableFrom(fieldType))
            return PrimitiveType.STRING;
        if(Field.Bytes.class.isAssignableFrom(fieldType))
            throw new UnsupportedOperationException("Protobuf FieldType \"bytes\" is not currently supported.");

        throw new DeveloperException("Unknown Protobuf FieldType \"" + fieldType + "\".");
    }
}