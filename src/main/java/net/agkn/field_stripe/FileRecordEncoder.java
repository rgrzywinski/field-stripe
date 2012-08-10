package net.agkn.field_stripe;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.agkn.common.exception.NoSuchObjectException;
import net.agkn.common.exception.OperationFailedException;
import net.agkn.field_stripe.encode.FieldStripeEncoderFactory;
import net.agkn.field_stripe.encode.RootFieldStripeEncoder;
import net.agkn.field_stripe.record.ICompositeType;
import net.agkn.field_stripe.record.IFieldType;
import net.agkn.field_stripe.record.protobuf.ProtobufFieldTypeFactory;
import net.agkn.field_stripe.record.reader.IRecordReader;
import net.agkn.field_stripe.record.reader.SmartJsonArrayRecordReader;
import net.agkn.field_stripe.stripe.BinaryVLenFieldStripeWriter;
import net.agkn.field_stripe.stripe.IFieldStripeWriter;
import net.agkn.field_stripe.stripe.IFieldStripeWriterFactory;
import net.agkn.protobuf.parser.ParseException;
import net.agkn.protobuf.parser.ProtobufDefinition;
import net.agkn.protobuf.parser.ProtobufParser;

import org.apache.commons.io.FileUtils;

/**
 * An command-line entry point into a file-based record encoder that uses 
 * Protobuf-based IDL to define the schema. The following information is passed:<p/>
 * <ul>
 *   <li>The base path to the schema IDL (*.proto). This directory must exist
 *       (and be a directory);</li>
 *   <li>The fully qualified name of the message definition (within the Protobuf
 *       IDL) for the encoded data;</li>
 *   <li>The path and filename to the JSON record data to be encoded;</li>
 *   <li>The base path to the output encoded field stripes. This directory must
 *       exist (and be a directory);</li>
 * </ul>
 *
 * @author rgrzywinski
 * @see FileRecordDecoder
 */
public class FileRecordEncoder {
    /**
     * @param  args refer to the {@link FileRecordEncoder class JavaDoc} for the
     *         required parameters. This can never be <code>null</code>.
     */
    public static void main(final String[] args) {
        if(args.length != 4) {
            showUsage();
            System.exit(1/*EXIT_FAILURE*/);
            return;
        } /* else -- there are the expected number of arguments */

        // validate all input parameters
        final File idlBasePath = new File(args[0]);
        if(!idlBasePath.exists()) { System.err.println("The IDL base path does not exist: " + args[0]); System.exit(1/*EXIT_FAILURE*/); }
        if(!idlBasePath.isDirectory()) { System.err.println("The IDL base path is not a directory: " + args[0]); System.exit(1/*EXIT_FAILURE*/); }
        final String fqMessageName = args[1];
        final File jsonInputRecord = new File(args[2]);
        if(!jsonInputRecord.exists()) { System.err.println("The input JSON record file does not exist: " + args[2]); System.exit(1/*EXIT_FAILURE*/); }
        if(jsonInputRecord.isDirectory()) { System.err.println("The input JSON record is not a file: " + args[2]); System.exit(1/*EXIT_FAILURE*/); }
        final File outputPath = new File(args[3]);
        if(!outputPath.exists()) { System.err.println("The output base path does not exist: " + args[3]); System.exit(1/*EXIT_FAILURE*/); }
        if(!outputPath.isDirectory()) { System.err.println("The output base path is not a directory: " + args[3]); System.exit(1/*EXIT_FAILURE*/); }

        IFieldStripeWriterFactory fieldStripeWriterFactory = null/*none to start*/; 
        try {
            final ICompositeType schema = createSchema(idlBasePath, fqMessageName);
            final IRecordReader recordReader = createRecordReader(jsonInputRecord);
            fieldStripeWriterFactory = createFieldStripeWriterFactory(outputPath);
            final RootFieldStripeEncoder rootEncoder = createEncoderTree(schema, fieldStripeWriterFactory);

            // encode each record
            while(rootEncoder.encode(recordReader));
        } catch(final OperationFailedException ofe) {
            System.err.println("An error occurred while encoding records into field-stripes: " + ofe.getLocalizedMessage());
            System.exit(1/*EXIT_FAILURE*/);
        } finally {
            try {
                if(fieldStripeWriterFactory != null) fieldStripeWriterFactory.closeAllWriters();
            } catch(final OperationFailedException ofe) {
                System.err.println("An error occurred while closing field-stripe writers: " + ofe.getLocalizedMessage());
                System.exit(1/*EXIT_FAILURE*/);
            }
        }

        System.exit(0/*EXIT_SUCCESS*/);
    }

    // ------------------------------------------------------------------------
    /**
     * Parses the IDL at the specified path and finds the message definition 
     * with the specified fully-qualified name. This is converted into a
     * {@link ICompositeType schema} and returned.
     */
    public static ICompositeType createSchema(final File protobufPath, final String fqMessageName) {
        final List<ProtobufDefinition> protobufDefinitions = parseProtobufDefinitions(protobufPath);
        final ProtobufFieldTypeFactory fieldTypeFactory = new ProtobufFieldTypeFactory();
        try {
            final IFieldType fieldType = fieldTypeFactory.createFieldType(protobufDefinitions, fqMessageName);
            if(!(fieldType instanceof ICompositeType)) {
                System.err.println("The message definition was not a top-level message: " + fqMessageName);
                System.exit(1/*EXIT_FAILURE*/);
                return null/*never occurs*/;
            } /* else -- the message was of the expected type */
            return (ICompositeType)fieldType;
        } catch(final NoSuchObjectException nsoe) {
            System.err.println("The message definition could not be found: " + fqMessageName);
            System.exit(1/*EXIT_FAILURE*/);
            return null/*never occurs*/;
        } catch(final OperationFailedException ofe) {
            System.err.println("An error occurred converting the message definition into an encoding schema: " + fqMessageName);
            System.exit(1/*EXIT_FAILURE*/);
            return null/*never occurs*/;
        }
    }

    /**
     * Parse all Protobuf (*.proto) files in the specified path.
     */
    public static List<ProtobufDefinition> parseProtobufDefinitions(final File protobufPath) {
        final ProtobufParser parser = new ProtobufParser();

        final List<ProtobufDefinition> protobufDefinitions = new ArrayList<ProtobufDefinition>();
        final Iterator<File> protobufFiles = FileUtils.iterateFiles(protobufPath, new String[] { "proto" }, true/*recursively*/);
        while(protobufFiles.hasNext()) {
            final File protobufFile = protobufFiles.next();
            if(protobufFile.isDirectory()) continue/*ignore directories*/;

            // CHECK:  should this continue if there's an error parsing?
            try {
                final Reader fileReader = new FileReader(protobufFile);
                protobufDefinitions.add(parser.parse(protobufFile.getAbsolutePath(), fileReader));
            } catch(final FileNotFoundException fnfe) {
                // NOTE:  the only way that this could occur is if the file was 
                //        moved / deleted between the earlier check and this call
                System.err.println("Protobuf IDL file moved / deleted: " + protobufFile.getAbsolutePath());
            } catch(final ParseException pe) {
                System.err.println("An error occurred while parsing: " + protobufFile.getAbsolutePath());
                System.err.println(pe.getLocalizedMessage());
            }
        }
        return protobufDefinitions;
    }

    // ------------------------------------------------------------------------
    /**
     * Creates and returns a {@link IRecordReader} from the specified JSON-array-based
     * file.
     */
    private static IRecordReader createRecordReader(final File jsonInputRecord) {
        try {
            return new SmartJsonArrayRecordReader(new FileReader(jsonInputRecord));
        } catch(final FileNotFoundException fnfe) {
            // NOTE:  the only way that this could occur is if the file was 
            //        moved / deleted between the earlier check and this call
            System.err.println("JSON record file moved / deleted: " + jsonInputRecord.getAbsolutePath());
            System.exit(1/*EXIT_FAILURE*/);
            return null/*never occurs*/;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Creates the {@link IFieldStripeWriterFactory} for the specified output
     * path.
     */
    private static IFieldStripeWriterFactory createFieldStripeWriterFactory(final File outputPath) {
        try {
            return BinaryVLenFieldStripeWriter.createFactory(outputPath);
        } catch(final OperationFailedException ofe) {
            // NOTE:  the only way that this could occur is if the file was 
            //        moved / deleted between the earlier check and this call
            System.err.println("Output path moved / deleted: " + outputPath.getAbsolutePath());
            System.exit(1/*EXIT_FAILURE*/);
            return null/*never occurs*/;
        }
    }

    /**
     * Creates the tree of {@link IFieldStripeWriter field-stripe writers} using  
     * the specified {@link IFieldStripeWriterFactory} based on the specified 
     * {@link ICompositeType schema} and returns the {@link RootFieldStripeEncoder}. 
     */
    private static RootFieldStripeEncoder createEncoderTree(final ICompositeType schema, final IFieldStripeWriterFactory fieldStripeWriterFactory) {
        try {
            final FieldStripeEncoderFactory encoderFactory = new FieldStripeEncoderFactory(fieldStripeWriterFactory);
            return encoderFactory.createEncoderTree(schema);
        } catch(final OperationFailedException ofe) {
            System.err.println("An error occurred creating the output field stripe writers: " + ofe.getLocalizedMessage());
            System.exit(1/*EXIT_FAILURE*/);
            return null/*never occurs*/;
        }
    }

    // ========================================================================
    /**
     * Writes the CLI usage to standard out.
     */
    public static void showUsage() {
        System.out.println("Usage:");
        System.out.println("\tFileRecordEncoder <IDL base path> <fully-qualified message definition> <JSON input filename> <output field-stripe path>");
    }
}