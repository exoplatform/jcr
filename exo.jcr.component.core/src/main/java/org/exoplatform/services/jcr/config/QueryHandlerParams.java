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
package org.exoplatform.services.jcr.config;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author <a href="mailto:gavrikvetal@gmail.com">Vitaliy Gulyy</a>
 * @version $
 */

public interface QueryHandlerParams
{

   // after JCR-445
   public static final String PARAM_AUTO_REPAIR = "auto-repair";

   public static final String PARAM_BUFFER_SIZE = "buffer-size";

   public static final String PARAM_CACHE_SIZE = "cache-size";

   public static final String PARAM_CONSISTENCY_CHECK_ENABLED = "consistency-check-enabled";

   public static final String PARAM_DOCUMENT_ORDER = "document-order";

   public static final String PARAM_EXCERPTPROVIDER_CLASS = "excerptprovider-class";

   public static final String PARAM_EXCLUDED_NODE_IDENTIFERS = "excluded-node-identifers";

   public static final String PARAM_EXTRACTOR_BACKLOG = "extractor-backlog";

   public static final String PARAM_EXTRACTOR_POOLSIZE = "extractor-pool-size";

   public static final String PARAM_EXTRACTOR_TIMEOUT = "extractor-timeout";

   public static final String PARAM_FORCE_CONSISTENCYCHECK = "force-consistencycheck";

   /**
    * ErrorLog file size in Kb.
    */
   public static final String PARAM_ERRORLOG_SIZE = "errorlog-size";

   /**
    * The location of the search index. <p/> Note: This is a <b>mandatory</b> parameter!
    */
   public static final String PARAM_INDEX_DIR = "index-dir";

   public static final String OLD_PARAM_INDEX_DIR = "indexDir";

   public static final String PARAM_INDEXING_CONFIGURATION_PATH = "indexing-configuration-path";

   public static final String PARAM_INDEXING_CONFIGURATION_CLASS = "indexing-configuration-class";

   public static final String PARAM_MAX_FIELD_LENGTH = "max-field-length";

   public static final String PARAM_MAX_MERGE_DOCS = "max-merge-docs";

   public static final String PARAM_MERGE_FACTOR = "merge-factor";

   public static final String PARAM_MIN_MERGE_DOCS = "min-merge-docs";

   public static final String PARAM_QUERY_CLASS = "query-class";

   public static final String PARAM_RESULT_FETCH_SIZE = "result-fetch-size";

   public static final String PARAM_ROOT_NODE_ID = "root-node-id";

   public static final String PARAM_SPELLCHECKER_CLASS = "spellchecker-class";

   public static final String PARAM_SUPPORT_HIGHLIGHTING = "support-highlighting";

   public static final String PARAM_SYNONYMPROVIDER_CLASS = "synonymprovider-class";

   public static final String PARAM_SYNONYMPROVIDER_CONFIG_PATH = "synonymprovider-config-path";

   public static final String PARAM_USE_COMPOUNDFILE = "use-compoundfile";

   public static final String PARAM_VOLATILE_IDLE_TIME = "volatile-idle-time";

   //since https://jira.jboss.org/jira/browse/EXOJCR-17

   public static final String PARAM_UPGRADE_INDEX = "upgrade-index";

   public static final String PARAM_ANALYZER_CLASS = "analyzer";

}
