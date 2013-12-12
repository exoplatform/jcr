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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Implements a synonym provider based on a properties file. Each line in the
 * properties file is treated as a synonym definition. Example:
 * <pre>
 * A=B
 * B=C
 * </pre>
 * This synonym provider will return B as a synonym for A and vice versa. The
 * same applies to B and C. However A is not considered a synonym for C, nor
 * C a synonym for A.
 */
public class PropertiesSynonymProvider implements SynonymProvider
{

   /**
    * An empty string array. Returned when no synonym is found.
    */
   private static final String[] EMPTY_ARRAY = new String[0];

   /**
    * Check at most every 10 seconds for configuration updates.
    */
   private static final long CHECK_INTERVAL = 10 * 1000;

   /**
    * The file system resource that contains the configuration.
    */
   private InputStream config;

   /**
    * Timestamp when the configuration was checked last.
    */
   private long lastCheck;

   /**
    * Contains the synonym mapping. Map&lt;String, String[]>
    */
   private Map<String, String[]> synonyms = new HashMap<String, String[]>();

   /**
    * {@inheritDoc}
    */
   public synchronized void initialize(InputStream fsr) throws IOException
   {
      if (fsr == null)
      {
         throw new IOException("PropertiesSynonymProvider requires a path configuration");
      }
      try
      {
         config = fsr;
         synonyms = getSynonyms(config);
         lastCheck = System.currentTimeMillis();
      }
      catch (IOException e)
      {
         throw e;
      }
   }

   /**
    * {@inheritDoc}
    */
   public String[] getSynonyms(String term)
   {
      checkConfigUpdated();
      term = term.toLowerCase();
      String[] syns;
      synchronized (this)
      {
         syns = (String[])synonyms.get(term);
      }
      if (syns == null)
      {
         syns = EMPTY_ARRAY;
      }
      return syns;
   }

   //---------------------------------< internal >-----------------------------

   /**
    * Checks if the synonym properties file has been updated and this provider
    * should reload the synonyms. This method performs the actual check at most
    * every {@link #CHECK_INTERVAL}. If reloading fails an error is logged and
    * this provider will retry after {@link #CHECK_INTERVAL}.
    */
   private synchronized void checkConfigUpdated()
   {
      if (lastCheck + CHECK_INTERVAL > System.currentTimeMillis())
      {
         return;
      }
      // check last modified
      //try
      //{
         //            if (configLastModified != config.lastModified()) {
         //                synonyms = getSynonyms(config);
         //                configLastModified = config.lastModified();
         //                log.info("Reloaded synonyms from {}", config.getPath());
         //            }
      //}
      //catch (Exception e)
      //{
      //log.error("Exception while reading synonyms", e);
      //}
      // update lastCheck timestamp, even if error occured (retry later)
      lastCheck = System.currentTimeMillis();
   }

   /**
    * Reads the synonym properties file and returns the contents as a synonym
    * Map.
    *
    * @param config the synonym properties file.
    * @return a Map containing the synonyms.
    * @throws IOException if an error occurs while reading from the file system
    *                     resource.
    */
   private static Map<String, String[]> getSynonyms(InputStream config) throws IOException
   {
      try
      {
         Map<String, String[]> synonyms = new HashMap<String, String[]>();
         Properties props = new Properties();
         props.load(config);
         Iterator<Map.Entry<Object, Object>> it = props.entrySet().iterator();
         while (it.hasNext())
         {
            Map.Entry<Object, Object> e = it.next();
            String key = (String)e.getKey();
            String value = (String)e.getValue();
            addSynonym(key, value, synonyms);
            addSynonym(value, key, synonyms);
         }
         return synonyms;
      }
      catch (Exception e)
      {
         throw Util.createIOException(e);
      }
   }

   /**
    * Adds a synonym definition to the map.
    *
    * @param term     the term
    * @param synonym  synonym for <code>term</code>.
    * @param synonyms the Map containing the synonyms.
    */
   private static void addSynonym(String term, String synonym, Map<String, String[]> synonyms)
   {
      term = term.toLowerCase();
      String[] syns = synonyms.get(term);
      if (syns == null)
      {
         syns = new String[]{synonym};
      }
      else
      {
         String[] tmp = new String[syns.length + 1];
         System.arraycopy(syns, 0, tmp, 0, syns.length);
         tmp[syns.length] = synonym;
         syns = tmp;
      }
      synonyms.put(term, syns);
   }

}
