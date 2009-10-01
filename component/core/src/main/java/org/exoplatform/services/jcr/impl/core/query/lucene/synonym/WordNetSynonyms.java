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
package org.exoplatform.services.jcr.impl.core.query.lucene.synonym;

import java.io.IOException;
import java.io.InputStream;

import org.apache.lucene.index.memory.SynonymMap;

import org.exoplatform.services.jcr.impl.core.query.lucene.SynonymProvider;

/**
 * <code>WordNetSynonyms</code> implements a {@link SynonymProvider} that is backed by the WordNet
 * prolog file <a href="http://www.cogsci.princeton.edu/2.0/WNprolog-2.0.tar.gz">wn_s.pl</a>.
 */
public class WordNetSynonyms implements SynonymProvider
{

   /**
    * The synonym map or <code>null</code> if an error occurred while reading the prolog file.
    */
   private SynonymMap SYNONYM_MAP;

   /**
    * {@inheritDoc}
    */
   public void initialize(InputStream configuration) throws IOException
   {

      SynonymMap sm = null;
      try
      {
         sm = new SynonymMap(configuration);
      }
      catch (IOException e)
      {
         // ignore
      }
      SYNONYM_MAP = sm;
   }

   /**
    * {@inheritDoc}
    */
   public String[] getSynonyms(String string)
   {
      if (SYNONYM_MAP != null)
      {
         return SYNONYM_MAP.getSynonyms(string.toLowerCase());
      }
      else
      {
         return new String[0];
      }
   }
}
