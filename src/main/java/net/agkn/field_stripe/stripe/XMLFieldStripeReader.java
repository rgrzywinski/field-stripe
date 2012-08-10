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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import net.agkn.common.exception.DeveloperException;
import net.agkn.common.exception.OperationFailedException;
import net.agkn.field_stripe.record.IField;
import net.agkn.field_stripe.record.PrimitiveType;
import net.agkn.field_stripe.stripe.Instruction.Kind;
import net.jcip.annotations.NotThreadSafe;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 * An XML-based {@link IFieldStripeReader} that reads {@link PrimitiveType primitive}
 * based leaf-fields. This is used primarily for debugging as it is easily 
 * human and machine readable. It is hand-rolled due to its simplicity and to 
 * limit the dependencies (rather than using a third-party XML library). All 
 * instructions <i>must</i> be newline delimited.<p/>
 * 
 * @author rgrzywinski
 * @see XMLFieldStripeWriter
 */
@NotThreadSafe
public class XMLFieldStripeReader implements IFieldStripeReader {
    // the field for which this is a reader and the primitive type of the field
    private final IField field;
    private final PrimitiveType fieldType;

    // ------------------------------------------------------------------------
    private final BufferedReader reader;

    // ========================================================================
    /**
     * @param field the {@link IField} for which this reader is reading. This
     *        cannot be <code>null</code> and its type must be a {@link PrimitiveType}.
     * @param reader the {@link Reader} from which the XML is read. This cannot
     *        be <code>null</code>.
     */
    public XMLFieldStripeReader(final IField field, final Reader reader) {
        this.field = field;
        this.fieldType = (PrimitiveType)field.getType();

        this.reader = new BufferedReader(reader);
    }

    // ========================================================================
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeReader#readNextState()
     */
    @Override
    public Instruction readInstruction() 
            throws OperationFailedException {
        // read the next line. If there isn't one then return null (by contract)
        final String line;
        try {
            line = reader.readLine();
            if(StringUtils.isBlank(line)) return null/*end-of-stream (by contract)*/;
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }

        // extract the element name (between "<" and either "/>" or ">")
        if(line.length() < 3) throw new OperationFailedException("Invalid instruction in field \"" + field.getName() + "\": " + line);
        final int endIndex = line.indexOf('>');
        if(endIndex < 0) throw new OperationFailedException("Invalid instruction in field \"" + field.getName() + "\": " + line);
        final String elementName = line.substring(0, (endIndex + 1/*include the '>'*/));

        // determine the instruction and based on it extract any value that
        // might exist
        if(XMLFieldStripeWriter.UNSET.equals(elementName))
            return Instruction.UNSET;
        else if(XMLFieldStripeWriter.PARENT_IS_UNSET_START.equals(elementName))
            return new Instruction(Kind.UNSET_PARENT, Integer.parseInt(extractValue(line)));
        else if(XMLFieldStripeWriter.REPEATED_PARENT_START.equals(elementName))
            return new Instruction(Kind.REPEATED_PARENT, Integer.parseInt(extractValue(line)));
        else if(XMLFieldStripeWriter.REPEATED_VALUE.equals(elementName))
            return Instruction.REPEATED_VALUE;
        else if(XMLFieldStripeWriter.VALUE_START.equals(elementName))
            return new Instruction(Kind.VALUE, parseValue(extractValue(line)));
        else /*unknown element name*/
            throw new OperationFailedException("Unknown instruction in field \"" + field.getName() + "\": " + elementName);
    }

    /**
     * Extracts and returns the value from the specified XML instruction string.
     */
    private String extractValue(final String instructionString) 
            throws OperationFailedException {
        // the value exists between the begin and end XML elements (specifically,
        // between the first ">" and "<")
        final int beginIndex = instructionString.indexOf('>');
        if(beginIndex < 0) throw new OperationFailedException("Invalid instruction in field \"" + field.getName() + "\": " + instructionString);
        final int endIndex = instructionString.indexOf('<', beginIndex);
        if(endIndex < 0) throw new OperationFailedException("Invalid instruction in field \"" + field.getName() + "\": " + instructionString);
        return instructionString.substring((beginIndex + 1/*after '>'*/), endIndex);
    }

    /**
     * Parses the specified non-<code>null</code> string value into the appropriate
     * type based on the {@link IField field's} {@link IField#getType() type}.
     */
    private Object parseValue(final String stringValue) {
        switch(fieldType) {
            case BYTE:
                return Byte.parseByte(stringValue);
            case SHORT:
                return Short.parseShort(stringValue);
            case INT:
                return Integer.parseInt(stringValue);
            case LONG:
                return Long.parseLong(stringValue);
            case FLOAT:
                return Float.parseFloat(stringValue);
            case DOUBLE:
                return Double.parseDouble(stringValue);
            case BOOLEAN:
                return Boolean.parseBoolean(stringValue);
            case STRING:
                return StringEscapeUtils.unescapeXml(stringValue);
            
            default:
                throw new DeveloperException("Unknown field type in field \"" + field.getName() + ".");
        }
    }
}