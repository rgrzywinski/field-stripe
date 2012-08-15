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
 * An expected operation has failed and cannot be recovered from. This 
 * exception is typically thrown as the result of an internal exception (such 
 * as {@link java.io.IOException}) which, to provide a meaningful interface, 
 * should not be bubbled out to a user. In other words, the user doesn't or 
 * shouldn't care that a particular operation failed due to an IO exception -- 
 * the user only cares that the operation failed <i>for some reason</i>.<p/>
 * 
 * Subclasses will provide explicit information as to the nature of the
 * failure.<p/> 
 * 
 * @author rgrzywinski
 */
// CHECK:  in all cases should the exception be logged with the error?  Currently
//         its only done in the cases where there is a cause to the exception.
// NOTE:   "log" is passed as the first parameter (where it is used) simply for
//         aesthetics and it is much easier to ensure that it is present in the
//         case where a long message is used.
public class OperationFailedException extends Exception {
    private static final long serialVersionUID = -4413011733804852751L;

    // ************************************************************************
    /**
     * @see java.lang.Exception#Exception()
     */
    public OperationFailedException() {
        super();
    }

    /**
     * @param  log the <code>Logger</code> to which the exception is logged.
     *         This cannot be <code>null</code>.
     * @see    java.lang.Exception#Exception()
     */
    public OperationFailedException(final Logger log) {
        super();
        log.error("An unspecified error has occurred.");
    }

    /**
     * @see java.lang.Exception#Exception(java.lang.String)
     */
    public OperationFailedException(final String message) {
        super(message);
    }

    /**
     * @param  log the <code>Logger</code> to which the exception is logged.
     *         This cannot be <code>null</code>.
     * @param  message the detail message (which is saved for later retrieval
     *         by the {@link #getMessage()} method).
     * @see    java.lang.Exception#Exception(java.lang.String)
     */
    public OperationFailedException(final Logger log, final String message) {
        super(message);
        log.error(message);
    }

    /**
     * @see java.lang.Exception#Exception(java.lang.String, java.lang.Throwable)
     */
    public OperationFailedException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * @param  log the <code>Logger</code> to which the exception is logged.
     *         This cannot be <code>null</code>.
     * @param  message the detail message (which is saved for later retrieval
     *         by the {@link #getMessage()} method).
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method). (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     * @see    java.lang.Exception#Exception(java.lang.String, java.lang.Throwable)
     */
    public OperationFailedException(final Logger log, final String message, 
                                    final Throwable cause) {
        super(message, cause);
        log.error(message, cause);
    }
    
    /**
     * @see java.lang.Exception#Exception(java.lang.Throwable)
     */
    public OperationFailedException(final Throwable cause) {
        super(cause);
    }

    /**
     * @param  log the <code>Logger</code> to which the exception is logged.
     *         This cannot be <code>null</code>.
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method). (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     * @see    java.lang.Exception#Exception(java.lang.Throwable)
     */
    public OperationFailedException(final Logger log, final Throwable cause) {
        super(cause);
        log.error(cause, cause);
    }
}