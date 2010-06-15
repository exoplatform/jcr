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

import org.exoplatform.services.jcr.impl.util.SecurityHelper;
import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;

/**
 * The class <code>NamespaceMappings</code> implements a
 * {@link NamespaceResolver} that holds a namespace
 * mapping that is used internally in the search index. Storing paths with the
 * full uri of a namespace would require too much space in the search index.
 * <p/>
 * Whenever a yet unknown namespace uri to prefix mapping is requested, a new
 * prefix is created on the fly and associated with the namespace. Known
 * namespace mappings are stored in a properties file.
 */
public class FileBasedNamespaceMappings extends AbstractNamespaceMappings
{

   /**
    * Default logger instance for this class
    */
   private static Logger log = LoggerFactory.getLogger("exo.jcr.component.core.NamespaceMappings");

   /**
    * Location of the file that persists the uri / prefix mappings
    */
   private final File storage;

   /**
    * Map of uris indexed by prefixes
    */
   private Map prefixToURI = new HashMap();

   /**
    * Map of prefixes indexed by uris
    */
   private Map uriToPrefix = new HashMap();

   /**
    * Current prefix count.
    */
   private int prefixCount;

   /**
    * Creates <code>NamespaceMappings</code> instance. Initial mappings are
    * loaded from <code>file</code>.
    *
    * @param file the <code>File</code> to load initial mappings.
    * @throws IOException if an error occurs while reading initial namespace
    *                     mappings from <code>file</code>.
    */
   public FileBasedNamespaceMappings(File file) throws IOException
   {
      storage = file;
      load();
   }

   /**
    * Returns a namespace uri for a <code>prefix</code>.
    *
    * @param prefix the namespace prefix.
    * @return the namespace uri.
    * @throws NamespaceException if no namespace uri is registered for
    *                            <code>prefix</code>.
    */
   public synchronized String getNamespaceURIByPrefix(String prefix) throws NamespaceException
   {
      if (!prefixToURI.containsKey(prefix))
      {
         throw new NamespaceException(prefix + ": is not a registered namespace prefix.");
      }
      return (String)prefixToURI.get(prefix);
   }

   /**
    * Returns a prefix for the namespace <code>uri</code>. If a namespace
    * mapping exists, the already known prefix is returned; otherwise a new
    * prefix is created and assigned to the namespace uri.
    *
    * @param uri the namespace uri.
    * @return the prefix for the namespace uri.
    * @throws NamespaceException if an yet unknown namespace uri / prefix
    *                            mapping could not be stored.
    */
   public synchronized String getNamespacePrefixByURI(String uri) throws NamespaceException
   {
      String prefix = (String)uriToPrefix.get(uri);
      if (prefix == null)
      {
         // make sure prefix is not taken
         while (prefixToURI.get(String.valueOf(prefixCount)) != null)
         {
            prefixCount++;
         }
         prefix = String.valueOf(prefixCount);
         prefixToURI.put(prefix, uri);
         uriToPrefix.put(uri, prefix);
         log.debug("adding new namespace mapping: " + prefix + " -> " + uri);
         try
         {
            store();
         }
         catch (IOException e)
         {
            throw new NamespaceException("Could not obtain a prefix for uri: " + uri, e);
         }
      }
      return prefix;
   }

   //-----------------------< internal >---------------------------------------

   /**
    * Loads currently known mappings from a .properties file.
    *
    * @throws IOException if an error occurs while reading from the file.
    */
   private void load() throws IOException
   {
      SecurityHelper.doPriviledgedIOExceptionAction(new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            if (storage.exists())
            {
               InputStream in = PrivilegedFileHelper.fileInputStream(storage);
               try
               {
                  Properties props = new Properties();
                  log.debug("loading namespace mappings...");
                  props.load(in);

                  // read mappings from properties
                  Iterator iter = props.keySet().iterator();
                  while (iter.hasNext())
                  {
                     String prefix = (String)iter.next();
                     String uri = props.getProperty(prefix);
                     log.debug(prefix + " -> " + uri);
                     prefixToURI.put(prefix, uri);
                     uriToPrefix.put(uri, prefix);
                  }
                  prefixCount = props.size();
                  log.debug("namespace mappings loaded.");
               }
               finally
               {
                  in.close();
               }
            }
            return null;
         }
      });
   }

   /**
    * Writes the currently known mappings into a .properties file.
    *
    * @throws IOException if an error occurs while writing the file.
    */
   private void store() throws IOException
   {
      SecurityHelper.doPriviledgedIOExceptionAction(new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            Properties props = new Properties();

            // store mappings in properties
            Iterator iter = prefixToURI.keySet().iterator();
            while (iter.hasNext())
            {
               String prefix = (String)iter.next();
               String uri = (String)prefixToURI.get(prefix);
               props.setProperty(prefix, uri);
            }

            OutputStream out = PrivilegedFileHelper.fileOutputStream(storage);
            try
            {
               out = new BufferedOutputStream(out);
               props.store(out, null);
            }
            finally
            {
               // make sure stream is closed
               out.close();
            }
            return null;
         }
      });
   }

   public String[] getAllNamespacePrefixes() throws RepositoryException
   {
      return (String[])prefixToURI.keySet().toArray(new String[prefixToURI.keySet().size()]);
   }
}
