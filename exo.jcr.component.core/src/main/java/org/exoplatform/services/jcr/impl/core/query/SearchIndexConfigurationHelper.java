/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.jcr.impl.core.query;

import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.QueryHandlerParams;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex;
import org.exoplatform.services.jcr.util.StringNumberParser;

import java.io.IOException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: SearchIndexConfigurationHelper.java 1053 2009-12-15 09:27:30Z nzamosenchuk $
 * 
 */
public class SearchIndexConfigurationHelper
{
   private final SearchIndex searchIndex;

   /**
    * @param searchIndex
    */
   public SearchIndexConfigurationHelper(SearchIndex searchIndex)
   {
      super();
      this.searchIndex = searchIndex;
   }

   /**
    * Initialize parameters
    * 
    * @param queryHandlerEntry
    * @throws IOException
    * @throws RepositoryConfigurationException
    */
   public void init(QueryHandlerEntry queryHandlerEntry) throws IOException, RepositoryConfigurationException
   {
      for (SimpleParameterEntry parameter : queryHandlerEntry.getParameters())
      {
         setParam(parameter.getName(), parameter.getValue());
      }
   }

   /**
    * @param name
    * @param value
    */
   private void setParam(String name, String value)
   {

      if (QueryHandlerParams.PARAM_AUTO_REPAIR.equals(name))
      {
         searchIndex.setAutoRepair(Boolean.parseBoolean(value));
      }
      else if (QueryHandlerParams.PARAM_BUFFER_SIZE.equals(name))
      {
         searchIndex.setBufferSize(StringNumberParser.parseInt(value));
      }
      else if (QueryHandlerParams.PARAM_CACHE_SIZE.equals(name))
      {
         searchIndex.setCacheSize(StringNumberParser.parseInt(value));
      }
      else if (QueryHandlerParams.PARAM_CONSISTENCY_CHECK_ENABLED.equals(name))
      {
         searchIndex.setEnableConsistencyCheck(Boolean.parseBoolean(value));
      }
      else if (QueryHandlerParams.PARAM_DOCUMENT_ORDER.equals(name))
      {
         searchIndex.setRespectDocumentOrder(Boolean.parseBoolean(value));
      }
      else if (QueryHandlerParams.PARAM_EXCERPTPROVIDER_CLASS.equals(name))
      {
         searchIndex.setExcerptProviderClass(value);
      }
      else if (QueryHandlerParams.PARAM_EXTRACTOR_BACKLOG.equals(name))
      {
         searchIndex.setExtractorBackLogSize(StringNumberParser.parseInt(value));
      }
      else if (QueryHandlerParams.PARAM_EXTRACTOR_POOLSIZE.equals(name))
      {
         searchIndex.setExtractorPoolSize(StringNumberParser.parseInt(value));
      }
      else if (QueryHandlerParams.PARAM_EXTRACTOR_TIMEOUT.equals(name))
      {
         searchIndex.setExtractorTimeout(StringNumberParser.parseLong(value));
      }
      else if (QueryHandlerParams.PARAM_FORCE_CONSISTENCYCHECK.equals(name))
      {
         searchIndex.setForceConsistencyCheck(Boolean.parseBoolean(value));
      }
      else if (QueryHandlerParams.PARAM_ERRORLOG_SIZE.equals(name))
      {
         searchIndex.setErrorLogfileSize(StringNumberParser.parseInt(value));
      }
      else if (QueryHandlerParams.PARAM_INDEXING_CONFIGURATION_PATH.equals(name))
      {
         searchIndex.setIndexingConfiguration(value);
      }
      else if (QueryHandlerParams.PARAM_INDEXING_CONFIGURATION_CLASS.equals(name))
      {
         searchIndex.setIndexingConfigurationClass(value);
      }
      else if (QueryHandlerParams.PARAM_MAX_FIELD_LENGTH.equals(name))
      {
         searchIndex.setMaxFieldLength(StringNumberParser.parseInt(value));
      }
      else if (QueryHandlerParams.PARAM_MAX_MERGE_DOCS.equals(name))
      {
         searchIndex.setMaxMergeDocs(StringNumberParser.parseInt(value));
      }
      else if (QueryHandlerParams.PARAM_MERGE_FACTOR.equals(name))
      {
         searchIndex.setMergeFactor(StringNumberParser.parseInt(value));
      }
      else if (QueryHandlerParams.PARAM_MIN_MERGE_DOCS.equals(name))
      {
         searchIndex.setMinMergeDocs(StringNumberParser.parseInt(value));
      }
      else if (QueryHandlerParams.PARAM_QUERY_CLASS.equals(name))
      {
         searchIndex.setQueryClass(value);
      }
      else if (QueryHandlerParams.PARAM_RESULT_FETCH_SIZE.equals(name))
      {
         searchIndex.setResultFetchSize(StringNumberParser.parseInt(value));
      }
      else if (QueryHandlerParams.PARAM_SPELLCHECKER_CLASS.equals(name))
      {
         searchIndex.setSpellCheckerClass(value);
      }
      else if (QueryHandlerParams.PARAM_SUPPORT_HIGHLIGHTING.equals(name))
      {
         searchIndex.setSupportHighlighting(Boolean.parseBoolean(value));
      }
      else if (QueryHandlerParams.PARAM_SYNONYMPROVIDER_CLASS.equals(name))
      {
         searchIndex.setSynonymProviderClass(value);
      }
      else if (QueryHandlerParams.PARAM_SYNONYMPROVIDER_CONFIG_PATH.equals(name))
      {
         searchIndex.setSynonymProviderConfigPath(value);
      }
      else if (QueryHandlerParams.PARAM_USE_COMPOUNDFILE.equals(name))
      {
         searchIndex.setUseCompoundFile(Boolean.parseBoolean(value));
      }
      else if (QueryHandlerParams.PARAM_VOLATILE_IDLE_TIME.equals(name))
      {
         searchIndex.setVolatileIdleTime(StringNumberParser.parseInt(value));
      }
      else if (QueryHandlerParams.PARAM_MAX_VOLATILE_SIZE.equals(name))
      {
         searchIndex.setMaxVolatileIndexSize(StringNumberParser.parseInt(value));
      }
      else if (QueryHandlerParams.PARAM_MAX_VOLATILE_TIME.equals(name))
      {
         searchIndex.setMaxVolatileTime(StringNumberParser.parseInt(value));
      }
      else if (QueryHandlerParams.PARAM_ANALYZER_CLASS.equals(name))
      {
         searchIndex.setAnalyzer(value);
      }
   }
}
