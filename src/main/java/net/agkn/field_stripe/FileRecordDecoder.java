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

import static net.agkn.field_stripe.FileRecordEncoder.createSchema;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import net.agkn.field_stripe.decode.FieldStripeDecoderFactory;
import net.agkn.field_stripe.decode.RootFieldStripeDecoder;
import net.agkn.field_stripe.exception.OperationFailedException;
import net.agkn.field_stripe.record.ICompositeType;
import net.agkn.field_stripe.record.writer.IRecordWriter;
import net.agkn.field_stripe.record.writer.JsonArrayRecordWriter;
import net.agkn.field_stripe.stripe.BinaryVLenFieldStripeReader;
import net.agkn.field_stripe.stripe.IFieldStripeReader;
import net.agkn.field_stripe.stripe.IFieldStripeReaderFactory;

/**
 * An command-line entry point into a file-based record decoder that uses 
 * Protobuf-based IDL to define the schema and outputs JSON-array-based records. 
 * The following information is passed:<p/>
 * <ul>
 *   <li>The base path to the schema IDL (*.proto). This directory must exist
 *       (and be a directory);</li>
 *   <li>The fully qualified name of the message definition (within the Protobuf
 *       IDL) for the encoded data;</li>
 *   <li>The base path to the input encoded field stripes. This directory must
 *       exist (and be a directory);</li>
 * </ul>
 * 
 * An optional fourth parameter specifies an output file. If the output file is
 * not specified then the decoded records are written to standard out.
 *
 * @author rgrzywinski
 * @see FileRecordEncoder
 */
public class FileRecordDecoder {
    /**
     * @param  args refer to the {@link FileRecordEncoder class JavaDoc} for the
     *         required parameters. This can never be <code>null</code>.
     */
    public static void main(final String[] args) {
        if((args.length != 3) && (args.length != 4)) {
            showUsage();
            System.exit(1/*EXIT_FAILURE*/);
            return;
        } /* else -- there are the expected number of arguments */

        // validate all input parameters
        final File idlBasePath = new File(args[0]);
        if(!idlBasePath.exists()) { System.err.println("The IDL base path does not exist: " + args[0]); System.exit(1/*EXIT_FAILURE*/); }
        if(!idlBasePath.isDirectory()) { System.err.println("The IDL base path is not a directory: " + args[0]); System.exit(1/*EXIT_FAILURE*/); }
        final String fqMessageName = args[1];
        final File fieldStripePath = new File(args[2]);
        if(!fieldStripePath.exists()) { System.err.println("The field stripe path does not exist: " + args[2]); System.exit(1/*EXIT_FAILURE*/); }
        if(!fieldStripePath.isDirectory()) { System.err.println("The field stripe path is not a directory: " + args[2]); System.exit(1/*EXIT_FAILURE*/); }

        // creates the output writer based on the presence of the 4th arg
        final Writer outputWriter;
        try {
            if(args.length == 4) {
                outputWriter = new FileWriter(args[3]);
            } else/*output to standard out*/
                outputWriter = new OutputStreamWriter(System.out);
        } catch(final IOException ioe) {
            System.err.println("Could not write to output file: " + args[3]);
            System.exit(1/*EXIT_FAILURE*/);
            return/*not reached*/;
        }

        IFieldStripeReaderFactory fieldStripeReaderFactory = null/*none to start*/; 
        try {
            final ICompositeType schema = createSchema(idlBasePath, fqMessageName);
            fieldStripeReaderFactory = createFieldStripeReaderFactory(fieldStripePath);
            final RootFieldStripeDecoder rootDecoder = createDecoderTree(schema, fieldStripeReaderFactory);
            final IRecordWriter recordWriter = new JsonArrayRecordWriter(outputWriter, false/*no pretty-print*/);

            // decode each record
            while(rootDecoder.decode(recordWriter));

            recordWriter.close()/*close to flush by contract*/;
        } catch(final OperationFailedException ofe) {
            System.err.println("An error occurred while decoding records from field-stripes: " + ofe.getLocalizedMessage());
            System.exit(1/*EXIT_FAILURE*/);
        } finally {
            // CHECK:  explicitly close or flush any stream?
        }

        System.exit(0/*EXIT_SUCCESS*/);
    }

    // ------------------------------------------------------------------------
    /**
     * Creates the {@link IFieldStripeReaderFactory} for the specified field-
     * stripe path.
     */
    private static IFieldStripeReaderFactory createFieldStripeReaderFactory(final File inputPath) {
        try {
            return BinaryVLenFieldStripeReader.createFactory(inputPath);
        } catch(final OperationFailedException ofe) {
            // NOTE:  the only way that this could occur is if the file was 
            //        moved / deleted between the earlier check and this call
            System.err.println("Field-stripe path moved / deleted: " + inputPath.getAbsolutePath());
            System.exit(1/*EXIT_FAILURE*/);
            return null/*never occurs*/;
        }
    }

    /**
     * Creates the tree of {@link IFieldStripeReader field-stripe readers} using  
     * the specified {@link IFieldStripeREaderFactory} based on the specified 
     * {@link ICompositeType schema} and returns the {@link RootFieldStripeDecoder}. 
     */
    private static RootFieldStripeDecoder createDecoderTree(final ICompositeType schema, final IFieldStripeReaderFactory fieldStripeReaderFactory) {
        try {
            final FieldStripeDecoderFactory decoderFactory = new FieldStripeDecoderFactory(fieldStripeReaderFactory);
            return decoderFactory.createDecoderTree(schema);
        } catch(final OperationFailedException ofe) {
            System.err.println("An error occurred creating the field stripe readers: " + ofe.getLocalizedMessage());
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
        System.out.println("\tFileRecordDecoder <IDL base path> <fully-qualified message definition> <field-stripe path> [<output filename>]");
    }
}