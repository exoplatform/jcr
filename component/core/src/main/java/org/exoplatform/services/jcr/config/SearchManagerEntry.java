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

import org.exoplatform.services.log.Log;

import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex;
import org.exoplatform.services.log.ExoLogger;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: SearchManagerEntry.java 12111 2008-03-19 16:27:42Z serg $
 */
public class SearchManagerEntry
{
   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger("jcr.SearchManagerEntry");

   /**
    * Name of the default query implementation class.
    */
   private static final String DEFAULT_QUERY_IMPL_CLASS = QueryImpl.class.getName();

   /**
    * Name of the default query implementation class.
    */
   private static final String DEFAULT_QUERY_HANDLER_CLASS = SearchIndex.class.getName();

   public QueryHandlerEntry queryHandler;

   private String queryClass = DEFAULT_QUERY_IMPL_CLASS;

   private String queryHandlerClass = DEFAULT_QUERY_HANDLER_CLASS;

   private String rootNodeId;

   private String excludeNodeId;

   public String getQueryClass()
   {
      return queryClass;
   }

   public void setQueryClass(String queryClass)
   {
      this.queryClass = queryClass;
   }

   public QueryHandlerEntry getQueryHandler()
   {
      return queryHandler;
   }

   public void setQueryHandler(QueryHandlerEntry queryHandler)
   {
      this.queryHandler = queryHandler;
   }

   public String getRootNodeId()
   {
      return rootNodeId;
   }

   public String getExcludeNodeId()
   {
      return excludeNodeId;
   }

   public void setRootNodeId(String rootNodeId)
   {
      this.rootNodeId = rootNodeId;
   }

   public void setExcludeNodeId(String excludeNodeId)
   {
      this.excludeNodeId = excludeNodeId;
   }

   /*
    * public String getPreparedQueryClass() { return preparedQueryClass; }
    */

   /*
    * public void setPreparedQueryClass(String preparedQueryClass) { this.preparedQueryClass =
    * preparedQueryClass; }
    */

   /**
    * Returns name of default query handler class.
    * 
    * @return String - Default query handler class name.
    */
   public static String getDefaultQueryHandlerClass()
   {
      return DEFAULT_QUERY_HANDLER_CLASS;
   }

   public String getQueryHandlerClass()
   {
      return queryHandlerClass;
   }

   public void setQueryHandlerClass(String queryHandlerClass)
   {
      this.queryHandlerClass = queryHandlerClass;
   }
}
