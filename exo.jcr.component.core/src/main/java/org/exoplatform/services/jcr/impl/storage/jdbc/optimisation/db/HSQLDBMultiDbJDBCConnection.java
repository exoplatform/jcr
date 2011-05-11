/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db;

import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import javax.jcr.InvalidItemStateException;

/**
 * Created by The eXo Platform SAS
 * 
 * 26.08.2009
 * 
 * @author <a href="mailto:dezder@bk.ru">Denis Grebenyuk</a>
 * @version $Id: HSQLDBMultiDbJDBCConnection.java 2084 2010-03-19 07:22:38Z nfilotto $
 */
public class HSQLDBMultiDbJDBCConnection extends MultiDbJDBCConnection
{

   /**
      * HSQLDB Multidatabase JDBC Connection constructor.
      * 
      * @param dbConnection
      *          JDBC connection, shoudl be opened before
      * @param readOnly
      *          boolean if true the dbConnection was marked as READ-ONLY.
      * @param containerName
      *          Workspace Storage Container name (see configuration)
      * @param valueStorageProvider
      *          External Value Storages provider
      * @param maxBufferSize
      *          Maximum buffer size (see configuration)
      * @param swapDirectory
      *          Swap directory File (see configuration)
      * @param swapCleaner
      *          Swap cleaner (internal FileCleaner).
      * @throws SQLException
      * 
      * @see org.exoplatform.services.jcr.impl.util.io.FileCleaner
      */
   public HSQLDBMultiDbJDBCConnection(Connection dbConnection, boolean readOnly, String containerName,
      ValueStoragePluginProvider valueStorageProvider, int maxBufferSize, File swapDirectory, FileCleaner swapCleaner)
      throws SQLException
   {
      super(dbConnection, readOnly, containerName, valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void prepareQueries() throws SQLException
   {

      super.prepareQueries();
      FIND_PROPERTY_BY_NAME =
         "select V.DATA" + " from JCR_MITEM I, JCR_MVALUE V"
            + " where I.PARENT_ID=? and I.I_CLASS=2 and I.NAME=? and I.ID=V.PROPERTY_ID order by V.ORDER_NUM";
      FIND_NODES_BY_PARENTID = "select * from JCR_MITEM" + " where PARENT_ID=? and I_CLASS=1" + " order by N_ORDER_NUM";
      FIND_NODES_COUNT_BY_PARENTID = "select count(ID) from JCR_MITEM" + " where PARENT_ID=? and I_CLASS=1";
      FIND_PROPERTIES_BY_PARENTID = "select * from JCR_MITEM" + " where PARENT_ID=? and I_CLASS=2" + " order by ID";
      FIND_NODES_BY_PARENTID_CQ =
         "select I.*, P.NAME AS PROP_NAME, V.ORDER_NUM, V.DATA"
            + " from JCR_MITEM I, JCR_MITEM P, JCR_MVALUE V"
            + " where I.PARENT_ID=? and I.I_CLASS=1 and (P.PARENT_ID=I.ID and P.I_CLASS=2 and (P.NAME='[http://www.jcp.org/jcr/1.0]primaryType' or P.NAME='[http://www.jcp.org/jcr/1.0]mixinTypes' or P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]owner' or P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]permissions') and V.PROPERTY_ID=P.ID)"
            + " order by I.N_ORDER_NUM, I.ID";
      FIND_PROPERTIES_BY_PARENTID_CQ =
         "select I.ID, I.PARENT_ID, I.NAME, I.VERSION, I.I_CLASS, I.I_INDEX, I.N_ORDER_NUM, I.P_TYPE, I.P_MULTIVALUED,"
            + " V.ORDER_NUM, V.DATA, V.STORAGE_DESC from JCR_MITEM I LEFT OUTER JOIN JCR_MVALUE V ON (V.PROPERTY_ID=I.ID)"
            + " where I.PARENT_ID=? and I.I_CLASS=2 order by I.NAME";      
   }

   /**
    * Use simple queries since it is much faster
    */
   @Override
   protected QPath traverseQPath(String cpid) throws SQLException, InvalidItemStateException, IllegalNameException
   {
      return traverseQPathSQ(cpid);
   }   
}
