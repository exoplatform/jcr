/*
 * Copyright 2009 Day Management AG, Switzerland. All rights reserved.
 */
package org.exoplatform.services.jcr.impl.core.nodetype;

import javax.jcr.RepositoryException;

/**
 * Exception thrown when an attempt is made to register a node type that already
 * exists, and <code>allowUpdate</code> has not been set to <code>true</code>.
 *
 * @since JCR 2.0
 */
public class NodeTypeExistsException extends RepositoryException {
    /**
     * Constructs a new instance of this class with <code>null</code> as its
     * detail message.
     */
    public NodeTypeExistsException() {
        super();
    }

    /**
     * Constructs a new instance of this class with the specified detail
     * message.
     *
     * @param message the detail message. The detail message is saved for later
     *                retrieval by the {@link #getMessage()} method.
     */
    public NodeTypeExistsException(String message) {
        super(message);
    }

    /**
     * Constructs a new instance of this class with the specified detail message
     * and root cause.
     *
     * @param message   the detail message. The detail message is saved for later
     *                  retrieval by the {@link #getMessage()} method.
     * @param rootCause root failure cause
     */
    public NodeTypeExistsException(String message, Throwable rootCause) {
        super(message, rootCause);
    }

    /**
     * Constructs a new instance of this class with the specified root cause.
     *
     * @param rootCause root failure cause
     */
    public NodeTypeExistsException(Throwable rootCause) {
        super(rootCause);
    }
}
