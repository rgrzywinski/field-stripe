package net.agkn.field_stripe.exception;

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

import org.apache.log4j.Logger;

/**
 * Indicates that data being operated on is not valid or recognized. This 
 * exception is typically thrown as the result of an internal exception (such 
 * as {@link java.text.NumberFormatException}) which, to provide a meaningful 
 * interface, should not be bubbled out to a user. In other words, the user 
 * doesn't or shouldn't care that a particular operation failed due to an 
 * invalid number exception -- the user only cares that the operation failed 
 * <i>for some reason</i> due to invalid data.<p/>
 *
 * @author rgrzywinski
 */
public class InvalidDataException extends OperationFailedException {
    private static final long serialVersionUID = -4832801416943075498L;

    // ************************************************************************
    /**
     * @see OperationFailedException#OperationFailedException()
     */
    public InvalidDataException() {
        super();
    }

    /**
     * @see OperationFailedException#OperationFailedException(org.apache.log4j.Logger)
     */
    public InvalidDataException(final Logger log) {
        super(log);
    }

    /**
     * @see OperationFailedException#OperationFailedException(java.lang.String)
     */
    public InvalidDataException(final String message) {
        super(message);
    }

    /**
     * @see OperationFailedException#OperationFailedException(org.apache.log4j.Logger, java.lang.String)
     */
    public InvalidDataException(final Logger log, final String message) {
        super(log, message);
    }

    /**
     * @see OperationFailedException#OperationFailedException(java.lang.String, java.lang.Throwable)
     */
    public InvalidDataException(final String message, final Throwable cause) {
        super(message, cause);
    }
    
    /**
     * @see OperationFailedException#OperationFailedException(org.apache.log4j.Logger, java.lang.String, java.lang.Throwable)
     */
    public InvalidDataException(final Logger log, final String message, 
                                final Throwable cause) {
        super(log, message, cause);
    }
    
    /**
     * @see OperationFailedException#OperationFailedException(java.lang.Throwable)
     */
    public InvalidDataException(final Throwable cause) {
        super(cause);
    }

    /**
     * @see OperationFailedException#OperationFailedException(org.apache.log4j.Logger, java.lang.Throwable)
     */
    public InvalidDataException(final Logger log, final Throwable cause) {
        super(log, cause);
    }
}