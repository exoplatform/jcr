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
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;

/**
 * <code>NSRegistryBasedNamespaceMappings</code> implements a namespace mapping based on the stable
 * index prefix provided by the namespace registry.
 */
public class NSRegistryBasedNamespaceMappings
   implements NamespaceMappings
{

   /**
    * The namespace registry.
    */
   private final NamespaceRegistryImpl nsReg;

   /**
    * The name resolver used to translate the qualified name to JCR name
    */
   private final LocationFactory nameResolver;

   /**
    * Creates a new <code>NSRegistryBasedNamespaceMappings</code>.
    * 
    * @param nsReg
    *          the namespace registry of the repository.
    */
   NSRegistryBasedNamespaceMappings(NamespaceRegistryImpl nsReg)
   {
      this.nsReg = nsReg;
      this.nameResolver = new LocationFactory(nsReg);
   }

   // -------------------------------< NamespaceMappings >----------------------

   /**
    * {@inheritDoc}
    */
   public String translatePropertyName(InternalQName qName) throws IllegalNameException
   {
      try
      {
         return nameResolver.createJCRName(qName).getAsString();
      }
      catch (RepositoryException e)
      {
         // should never happen actually, there is always a stable index
         // prefix for a known namespace uri
         throw new IllegalNameException("Internal error.", e);
      }
   }

   public String[] getAllNamespacePrefixes() throws RepositoryException
   {
      return nsReg.getPrefixes();
   }

   public String getNamespacePrefixByURI(String uri) throws NamespaceException, RepositoryException
   {
      return nsReg.getPrefix(uri);
   }

   public String getNamespaceURIByPrefix(String prefix) throws NamespaceException, RepositoryException
   {
      try
      {
         return nsReg.getURI(prefix);
      }
      catch (NumberFormatException e)
      {
         throw new NamespaceException("Unknown prefix: " + prefix);
      }
   }
}
