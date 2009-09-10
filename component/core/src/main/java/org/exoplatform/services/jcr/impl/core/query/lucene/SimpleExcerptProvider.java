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
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.exoplatform.services.log.Log;
import org.apache.lucene.search.Query;

import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.log.ExoLogger;

/**
 * <code>SimpleExcerptProvider</code> is a <b>very</b> simple excerpt provider. It does not do any
 * highlighting and simply returns up to <code>maxFragmentSize</code> characters of string
 * properties for a given node.
 * 
 * @see #getExcerpt(org.apache.jackrabbit.core.NodeId, int, int)
 */
public class SimpleExcerptProvider implements ExcerptProvider
{

   /**
    * Logger instance for this class
    */
   private static final Log log = ExoLogger.getLogger(SimpleExcerptProvider.class);

   /**
    * The item state manager.
    */
   private ItemDataConsumer ism;

   /**
    * {@inheritDoc}
    */
   public void init(Query query, SearchIndex index) throws IOException
   {
      ism = index.getContext().getItemStateManager();
   }

   /**
    * {@inheritDoc}
    */
   public String getExcerpt(String id, int maxFragments, int maxFragmentSize) throws IOException
   {
      StringBuffer text = new StringBuffer();
      try
      {
         ItemData node = ism.getItemData(id);
         if (node != null && node.isNode())
         {
            String separator = "";
            List<PropertyData> childs = ism.getChildPropertiesData((NodeData)node);
            for (PropertyData property : childs)
            {
               if (property.getType() == PropertyType.STRING)
               {
                  text.append(separator);
                  separator = " ... ";
                  List<ValueData> values = property.getValues();
                  for (int i = 0; i < values.size(); i++)
                  {
                     text.append(new String(values.get(i).getAsByteArray(), Constants.DEFAULT_ENCODING));
                  }
               }
            }
         }
      }
      catch (RepositoryException e)
      {
         // ignore
         log.warn(e.getLocalizedMessage());
      }
      if (text.length() > maxFragmentSize)
      {
         int lastSpace = text.lastIndexOf(" ", maxFragmentSize);
         if (lastSpace != -1)
         {
            text.setLength(lastSpace);
         }
         else
         {
            text.setLength(maxFragmentSize);
         }
         text.append(" ...");
      }
      return "<excerpt><fragment>" + text.toString() + "</fragment></excerpt>";
   }
}
