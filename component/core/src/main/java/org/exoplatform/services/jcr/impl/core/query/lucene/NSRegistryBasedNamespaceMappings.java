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

import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;

/**
 * <code>NSRegistryBasedNamespaceMappings</code> implements a namespace mapping
 * based on the stable index prefix provided by the namespace registry.
 */
public class NSRegistryBasedNamespaceMappings extends AbstractNamespaceMappings {

    /**
     * The namespace registry.
     */
    private final NamespaceRegistryImpl nsReg;

    /**
     * Creates a new <code>NSRegistryBasedNamespaceMappings</code>.
     *
     * @param nsReg the namespace registry of the repository.
     */
    NSRegistryBasedNamespaceMappings(NamespaceRegistryImpl nsReg) {
        this.nsReg = nsReg;
    }

    //-------------------------------< NamespaceResolver >----------------------

    /**
     * {@inheritDoc}
     */
    public String getNamespaceURIByPrefix(String prefix) throws NamespaceException {
        try {
//            int index = Integer.parseInt(prefix);
//            return nsReg.indexToString(index);
           return nsReg.getURI(prefix);
        } catch (IllegalArgumentException e) {
            throw new NamespaceException(
                    "Unknown namespace prefix: " + prefix, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespacePrefixByURI(String uri) throws NamespaceException {
        try {
          //  return String.valueOf(nsReg.stringToIndex(uri));
           return nsReg.getPrefix(uri);
        } catch (IllegalArgumentException e) {
            throw new NamespaceException(
                    "Unknown namespace URI: " + uri, e);
        }
    }

   public String[] getAllNamespacePrefixes() throws RepositoryException
   {
      return nsReg.getPrefixes();
   }
}
