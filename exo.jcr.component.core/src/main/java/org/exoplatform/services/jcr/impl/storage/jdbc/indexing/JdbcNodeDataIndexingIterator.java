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
package org.exoplatform.services.jcr.impl.storage.jdbc.indexing;

import org.exoplatform.services.jcr.datamodel.NodeDataIndexing;
import org.exoplatform.services.jcr.impl.core.query.NodeDataIndexingIterator;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCStorageConnection;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.GenericConnectionFactory;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 1 02 2011
 * 
 * Iterator for fetching NodeData from database with all properties and its values.
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: JdbcIndexingDataIterator.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class JdbcNodeDataIndexingIterator implements NodeDataIndexingIterator
{

   /**
    * Connection factory. Allows to open jdbc storage connection. 
    */
   private final GenericConnectionFactory connFactory;

   /**
    * The amount of the rows which could be retrieved from database for once.
    */
   private final int pageSize;

   /**
    * The current offset in database.
    */
   private int offset = 0;

   /**
    * Logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.JdbcIndexingDataIterator");

   /**
    * The list of nodes to return in next() method.
    */
   private List<NodeDataIndexing> current;

   /**
    * Constructor JdbcIndexingDataIterator.
    * 
    */
   public JdbcNodeDataIndexingIterator(GenericConnectionFactory connFactory, int pageSize) throws RepositoryException
   {
      this.connFactory = connFactory;
      this.pageSize = pageSize;
      this.current = readNext();
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasNext()
   {
      return this.current.size() != 0;
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeDataIndexing> next() throws RepositoryException
   {
      List<NodeDataIndexing> next = this.current;
      this.current = new ArrayList<NodeDataIndexing>();//readNext();

      return next;
   }

   /**
    * Read next nodes from database. 
    * 
    * @return List<NodeDataIndexing> 
    * @throws RepositoryException 
    */
   private List<NodeDataIndexing> readNext() throws RepositoryException
   {
      JDBCStorageConnection conn = (JDBCStorageConnection)connFactory.openConnection();
      try
      {
         return conn.getNodesAndProperties(offset, pageSize);
         // TODO offset + ?
      }
      finally
      {
         conn.close();
      }
   }
}

