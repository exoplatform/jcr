/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
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
      /*rsync is used as local index recovery strategy*/
      if(SearchIndex.INDEX_RECOVERY_RSYNC_STRATEGY.equals(searchIndex.getIndexRecoveryStrategy()) ||
              SearchIndex.INDEX_RECOVERY_RSYNC_WITH_DELETE_STRATEGY.equals(searchIndex.getIndexRecoveryStrategy()))
      {
         searchIndex.setRsyncConfiguration(new RSyncConfiguration(queryHandlerEntry));
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
      else if (QueryHandlerParams.PARAM_SPELLCHECKER_MORE_POPULAR.equals(name))
      {
         searchIndex.setSpellCheckerMorePopuar(Boolean.parseBoolean(value));
      }
      else if (QueryHandlerParams.PARAM_SPELLCHECKER_DISTANCE.equals(name))
      {
         searchIndex.setSpellCheckerMinDistance(StringNumberParser.parseNumber(value).floatValue());
      }
      else if (QueryHandlerParams.PARAM_REINDEXING_PAGE_SIZE.equals(name))
      {
         searchIndex.setReindexingPageSize(StringNumberParser.parseNumber(value).intValue());
      }
      else if (QueryHandlerParams.PARAM_RDBMS_REINDEXING.equals(name))
      {
         searchIndex.setRDBMSReindexing(Boolean.parseBoolean(value));
      }
      else if (QueryHandlerParams.PARAM_INDEX_RECOVERY_MODE.equals(name))
      {
         searchIndex.setIndexRecoveryMode(value);
      }
      else if (QueryHandlerParams.PARAM_INDEX_RECOVERY_STRATEGY.equals(name))
      {
         searchIndex.setIndexRecoveryStrategy(value);
      }
      else if (QueryHandlerParams.PARAM_ASYNC_REINDEXING.equals(name))
      {
         searchIndex.setAsyncReindexing(Boolean.parseBoolean(value));
      }
      else if (QueryHandlerParams.PARAM_INDEX_RECOVERY_FILTER.equals(name))
      {
         searchIndex.addRecoveryFilterClass(value);
      }
      else if (QueryHandlerParams.PARAM_INDEXING_THREAD_POOL_SIZE.equals(name))
      {
         searchIndex.setIndexingThreadPoolSize(Integer.parseInt(value));
      }
      else if (QueryHandlerParams.PARAM_INDEXING_LOAD_BATCHING_THRESHOLD_DYNAMIC.equals(name))
      {
         searchIndex.setIndexingLoadBatchingThresholdDynamic(Boolean.valueOf(value));
      }
      else if (QueryHandlerParams.PARAM_INDEXING_LOAD_BATCHING_THRESHOLD_TTL.equals(name))
      {
         searchIndex.setIndexingLoadBatchingThresholdTTL(StringNumberParser.parseTime(value));
      }
      else if (QueryHandlerParams.PARAM_INDEXING_LOAD_BATCHING_THRESHOLD_PROPERTY.equals(name))
      {
         searchIndex.setIndexingLoadBatchingThresholdProperty(Integer.parseInt(value));
      }
      else if (QueryHandlerParams.PARAM_INDEXING_LOAD_BATCHING_THRESHOLD_NODE.equals(name))
      {
         searchIndex.setIndexingLoadBatchingThresholdNode(Integer.parseInt(value));
      }
      else
      {
         searchIndex.addOptionalParameter(name, value);
      }
   }
}
