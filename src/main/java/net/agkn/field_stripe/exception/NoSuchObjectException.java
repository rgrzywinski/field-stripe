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
 * Indicates that an operation does not possess the desired object.<p/>
 *
 * @author rgrzywinski
 */
public class NoSuchObjectException extends OperationFailedException {
    private static final long serialVersionUID = 2512510121465569192L;

    // ************************************************************************
    /**
     * @see OperationFailedException#OperationFailedException()
     */
    public NoSuchObjectException() {
        super();
    }

    /**
     * @see OperationFailedException#OperationFailedException(org.apache.log4j.Logger)
     */
    public NoSuchObjectException(final Logger log) {
        super(log);
    }

    /**
     * @see OperationFailedException#OperationFailedException(java.lang.String)
     */
    public NoSuchObjectException(final String message) {
        super(message);
    }

    /**
     * @see OperationFailedException#OperationFailedException(org.apache.log4j.Logger, java.lang.String)
     */
    public NoSuchObjectException(final Logger log, final String message) {
        super(log, message);
    }

    /**
     * @see OperationFailedException#OperationFailedException(java.lang.String, java.lang.Throwable)
     */
    public NoSuchObjectException(final String message, final Throwable cause) {
        super(message, cause);
    }
    
    /**
     * @see OperationFailedException#OperationFailedException(org.apache.log4j.Logger, java.lang.String, java.lang.Throwable)
     */
    public NoSuchObjectException(final Logger log, final String message, 
                                 final Throwable cause) {
        super(log, message, cause);
    }
    
    /**
     * @see OperationFailedException#OperationFailedException(java.lang.Throwable)
     */
    public NoSuchObjectException(final Throwable cause) {
        super(cause);
    }

    /**
     * @see OperationFailedException#OperationFailedException(org.apache.log4j.Logger, java.lang.Throwable)
     */
    public NoSuchObjectException(final Logger log, final Throwable cause) {
        super(log, cause);
    }
}