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
 * This unchecked exception is thrown when a developer has failed to meet a
 * contract set forth by an interface. The environment should be setup such that
 * these exceptions are caught at a high level and handled appropriately.<p/>
 *
 * This is not and should not be used as a replacement for such already
 * defined exceptions as {@link java.lang.IllegalArgumentException}.<p/> 
 * 
 * @author rgrzywinski
 */
public class DeveloperException extends RuntimeException {
    /**
     * The required <code>serialVersionUID</code>.
     */
    private static final long serialVersionUID = 3400424093144651804L;

    // ************************************************************************
    /**
     * @see java.lang.RuntimeException#RuntimeException()
     */
    public DeveloperException() {
        super();
    }

    /**
     * @param  log the <code>Logger</code> to which the exception is logged.
     *         This cannot be <code>null</code>.
     * @see    java.lang.RuntimeException#RuntimeException()
     */
    public DeveloperException(final Logger log) {
        super();
        log.error("An unspecified error has occurred.");
    }

    /**
     * @see java.lang.RuntimeException#RuntimeException(java.lang.String)
     */
    public DeveloperException(final String message) {
        super(message);
    }

    /**
     * @param  log the <code>Logger</code> to which the exception is logged.
     *         This cannot be <code>null</code>.
     * @param  message the detail message (which is saved for later retrieval
     *         by the {@link #getMessage()} method).
     * @see    java.lang.RuntimeException#RuntimeException(java.lang.String)
     */
    public DeveloperException(final Logger log, final String message) {
        super(message);
        log.error(message);
    }

    /**
     * @see java.lang.RuntimeException#RuntimeException(java.lang.String, java.lang.Throwable)
     */
    public DeveloperException(final String message, final Throwable cause) {
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
     * @see    java.lang.RuntimeException#RuntimeException(java.lang.String, java.lang.Throwable)
     */
    public DeveloperException(final Logger log, final String message, 
                              final Throwable cause) {
        super(message, cause);
        log.error(message, cause);
    }

    /**
     * @see java.lang.RuntimeException#RuntimeException(java.lang.Throwable)
     */
    public DeveloperException(final Throwable cause) {
        super(cause);
    }

    /**
     * @param  log the <code>Logger</code> to which the exception is logged.
     *         This cannot be <code>null</code>.
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method). (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     * @see    java.lang.RuntimeException#RuntimeException(java.lang.Throwable)
     */
    public DeveloperException(final Logger log, final Throwable cause) {
        super(cause);
        log.error(cause, cause);
    }
}