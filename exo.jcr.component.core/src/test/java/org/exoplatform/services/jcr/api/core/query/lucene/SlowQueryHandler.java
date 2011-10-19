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
package org.exoplatform.services.jcr.api.core.query.lucene;

import org.apache.lucene.search.Query;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.InspectionLog;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.query.AbstractQueryHandler;
import org.exoplatform.services.jcr.impl.core.query.ExecutableQuery;
import org.exoplatform.services.jcr.impl.core.query.QueryHandlerContext;
import org.exoplatform.services.jcr.impl.core.query.lucene.ChangesHolder;
import org.exoplatform.services.jcr.impl.core.query.lucene.QueryHits;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;

/**
 * <code>SlowQueryHandler</code> implements a dummy query handler for testing
 * purpose.
 */
public class SlowQueryHandler extends AbstractQueryHandler
{

   protected void doInit() throws IOException, RepositoryException
   {
      // sleep for 10 seconds then try to read from the item state manager
      // the repository.xml is configured with a 5 second maxIdleTime
      try
      {
         Thread.sleep(10 * 1000);
      }
      catch (InterruptedException e)
      {
         // ignore
      }
      String id = "RANDOM_UUID";
      getContext().getItemStateManager().getItemData(id);
   }

   public void addNode(NodeData node) throws RepositoryException, IOException
   {
   }

   public void deleteNode(String id) throws IOException
   {
   }

   public void close()
   {
   }

   public ExecutableQuery createExecutableQuery(SessionImpl session, SessionDataManager itemMgr, String statement,
      String language) throws InvalidQueryException
   {
      return null;
   }

   public void logErrorChanges(Set<String> removed, Set<String> added) throws IOException
   {
      // TODO Auto-generated method stub

   }

   public void setContext(QueryHandlerContext context)
   {
      // TODO Auto-generated method stub

   }

   /**
    * @see org.exoplatform.services.jcr.impl.core.query.QueryHandler#executeQuery(org.apache.lucene.search.Query)
    */
   public QueryHits executeQuery(Query query) throws IOException
   {
      // TODO Auto-generated method stub
      return null;
   }

   public void apply(ChangesHolder changes) throws RepositoryException, IOException
   {
      // TODO Auto-generated method stub
      
   }

   public ChangesHolder getChanges(Iterator<String> remove, Iterator<NodeData> add)
   {
      // TODO Auto-generated method stub
      return null;
   }

   /**
    * @see org.exoplatform.services.jcr.impl.core.query.QueryHandler#isOnline()
    */
   public boolean isOnline()
   {
      return true;
   }

   /**
    * @see org.exoplatform.services.jcr.impl.core.query.QueryHandler#setOnline(boolean, boolean, boolean)
    */
   public void setOnline(boolean isOnline, boolean allowQuery, boolean dropStaleIndexes) throws IOException
   {
      // TODO Auto-generated method stub
      
   }

   /**
    * @see org.exoplatform.services.jcr.impl.core.query.QueryHandler#checkIndex(org.exoplatform.services.jcr.dataflow.ItemDataConsumer, boolean, InspectionLog)
    */
   @Override
   public void checkIndex(ItemDataConsumer itemStateManager, boolean isSystem, InspectionLog inspectionLog) throws RepositoryException,
      IOException
   {
      // do nothing
   }
}
