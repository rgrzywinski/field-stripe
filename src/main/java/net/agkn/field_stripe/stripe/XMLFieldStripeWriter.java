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

import java.io.IOException;
import java.io.Writer;

import net.agkn.field_stripe.exception.OperationFailedException;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * An XML-based {@link IFieldStripeWriter}. This is used primarily for debugging
 * as it is easily human and machine readable. It is hand-rolled due to its
 * simplicity and to limit the dependencies (rather than using a third-party
 * XML library). The goal is to be as "bytecode"-like in representation as 
 * possible.<p/>
 * 
 * Calling {@link #close()} will {@link Writer#close() close} the underlying
 * writer.
 *
 * @author rgrzywinski
 * @see XMLFieldStripeReader
 */
public class XMLFieldStripeWriter implements IFieldStripeWriter {
    // meta-data
    /*package*/ static final String UNSET = "<Unset/>";
    /*package*/ static final String PARENT_IS_UNSET_START = "<ParentIsUnset>";
    /*package*/ static final String PARENT_IS_UNSET_END = "</ParentIsUnset>";
    /*package*/ static final String REPEATED_VALUE = "<RepeatedValue/>";
    /*package*/ static final String REPEATED_PARENT_START = "<RepeatedParent>";
    /*package*/ static final String REPEATED_PARENT_END = "</RepeatedParent>";

    // values
    /*package*/ static final String VALUE_START = "<Value>";
    /*package*/ static final String VALUE_END = "</Value>";

    // ************************************************************************
    private final Writer writer;
    private boolean closed = false/*not closed until #close()*/;

    // ========================================================================
    /**
     * @param writer the {@link Writer} to which the XML is written. This cannot
     *        be <code>null</code>. The data is only guaranteed to be completely
     *        written after {@link #close()} is called.
     */
    public XMLFieldStripeWriter(final Writer writer) { this.writer = writer; }

    // ========================================================================
    // life-cycle
    
    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#close()
     */
    @Override
    public void close() 
            throws OperationFailedException {
        if(closed) return;
        try {
            writer.close();
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        } finally {
            this.closed = true/*by definition*/;
        }
    }

    // ========================================================================
    // meta-data

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeUnset()
     */
    @Override
    public void writeUnset() 
            throws IllegalStateException, OperationFailedException {
        if(closed) throw new IllegalStateException("The XML field stripe writer has already been closed.")/*by contract*/;
        try {
            writer.write(UNSET);
            writer.write('\n');
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeUnsetParent(int)
     */
    @Override
    public void writeUnsetParent(final int fieldDepth) 
            throws IllegalStateException, OperationFailedException {
        if(closed) throw new IllegalStateException("The XML field stripe writer has already been closed.")/*by contract*/;
        try {
            writer.write(PARENT_IS_UNSET_START);
            writer.write(String.valueOf(fieldDepth));
            writer.write(PARENT_IS_UNSET_END);
            writer.write('\n');
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeRepeated()
     */
    @Override
    public void writeRepeated() 
            throws IllegalStateException, OperationFailedException {
        if(closed) throw new IllegalStateException("The XML field stripe writer has already been closed.")/*by contract*/;
        try {
            writer.write(REPEATED_VALUE);
            writer.write('\n');
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeRepeatedParent(int)
     */
    @Override
    public void writeRepeatedParent(final int fieldDepth)
            throws IllegalStateException, OperationFailedException {
        if(closed) throw new IllegalStateException("The XML field stripe writer has already been closed.")/*by contract*/;
        try {
            writer.write(REPEATED_PARENT_START);
            writer.write(String.valueOf(fieldDepth));
            writer.write(REPEATED_PARENT_END);
            writer.write('\n');
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    // ========================================================================
    // primitive values 

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeValue(byte)
     */
    @Override
    public void writeValue(final byte value) 
            throws IllegalStateException, OperationFailedException {
        if(closed) throw new IllegalStateException("The XML field stripe writer has already been closed.")/*by contract*/;
        try {
            writer.write(VALUE_START);
            writer.write(String.valueOf(value));
            writer.write(VALUE_END);
            writer.write('\n');
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeValue(short)
     */
    @Override
    public void writeValue(final short value) 
            throws IllegalStateException, OperationFailedException {
        if(closed) throw new IllegalStateException("The XML field stripe writer has already been closed.")/*by contract*/;
        try {
            writer.write(VALUE_START);
            writer.write(String.valueOf(value));
            writer.write(VALUE_END);
            writer.write('\n');
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeValue(int)
     */
    @Override
    public void writeValue(final int value)
            throws IllegalStateException, OperationFailedException {
        if(closed) throw new IllegalStateException("The XML field stripe writer has already been closed.")/*by contract*/;
        try {
            writer.write(VALUE_START);
            writer.write(String.valueOf(value));
            writer.write(VALUE_END);
            writer.write('\n');
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeValue(long)
     */
    @Override
    public void writeValue(final long value)
            throws IllegalStateException, OperationFailedException {
        if(closed) throw new IllegalStateException("The XML field stripe writer has already been closed.")/*by contract*/;
        try {
            writer.write(VALUE_START);
            writer.write(String.valueOf(value));
            writer.write(VALUE_END);
            writer.write('\n');
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeValue(float)
     */
    @Override
    public void writeValue(final float value)
            throws IllegalStateException, OperationFailedException {
        if(closed) throw new IllegalStateException("The XML field stripe writer has already been closed.")/*by contract*/;
        try {
            writer.write(VALUE_START);
            writer.write(String.valueOf(value));
            writer.write(VALUE_END);
            writer.write('\n');
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeValue(double)
     */
    @Override
    public void writeValue(final double value)
            throws IllegalStateException, OperationFailedException {
        if(closed) throw new IllegalStateException("The XML field stripe writer has already been closed.")/*by contract*/;
        try {
            writer.write(VALUE_START);
            writer.write(String.valueOf(value));
            writer.write(VALUE_END);
            writer.write('\n');
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeValue(boolean)
     */
    @Override
    public void writeValue(final boolean value) 
            throws IllegalStateException, OperationFailedException {
        if(closed) throw new IllegalStateException("The XML field stripe writer has already been closed.")/*by contract*/;
        try {
            writer.write(VALUE_START);
            writer.write(String.valueOf(value));
            writer.write(VALUE_END);
            writer.write('\n');
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.field_stripe.stripe.IFieldStripeWriter#writeValue(java.lang.String)
     */
    @Override
    public void writeValue(final String value)
            throws IllegalStateException, OperationFailedException {
        if(closed) throw new IllegalStateException("The XML field stripe writer has already been closed.")/*by contract*/;
        try {
            writer.write(VALUE_START);
            writer.write(StringEscapeUtils.escapeXml(value));
            writer.write(VALUE_END);
            writer.write('\n');
        } catch(final IOException ioe) {
            throw new OperationFailedException(ioe);
        }
    }
}