/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exoplatform.services.jcr.impl.core.query.lucene;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.core.LocationFactory;


/**
 * <code>AbstractNamespaceMappings</code> is the base class for index internal
 * namespace mappings.
 */
public abstract class AbstractNamespaceMappings
        implements NamespaceMappings {

    /**
     * The name resolver used to translate the qualified name to JCR name
     */
    private final LocationFactory resolver;

    public AbstractNamespaceMappings() {
        this.resolver = new LocationFactory(this);
    }

    //----------------------------< NamespaceMappings >-------------------------

    /**
     * {@inheritDoc}
     */
    public String translateName(InternalQName qName)
            throws IllegalNameException {
        try {
            return resolver.createJCRName(qName).getAsString();
        } catch (RepositoryException e) {
            // should never happen actually, because we create yet unknown
            // uri mappings on the fly.
            throw new IllegalNameException("Internal error.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String translatePath(QPath path) throws IllegalNameException {
        try {
            return resolver.createJCRPath(path).getAsString(false);
        } catch (RepositoryException e) {
            // should never happen actually, because we create yet unknown
            // uri mappings on the fly.
            throw new IllegalNameException("Internal error.", e);
        }
    }

}
