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
package org.exoplatform.services.jcr.impl.storage.jdbc.db;

import org.exoplatform.services.jcr.impl.dataflow.SpoolConfig;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCConnectionTestBase;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by The eXo Platform SAS
 * 
 * 01.07.2009
 * 
 * @author <a href="mailto:dezder@bk.ru">Denis Grebenyuk</a>
 * @version $Id:$ 
 */
public class TestSingleDbJDBCConnection extends JDBCConnectionTestBase
{

   private void setUp(String scriptPath, DatabaseStructureType dbStructureType) throws Exception
   {
      super.setUp();
      JDBCDataContainerConfig containerConfig = new JDBCDataContainerConfig();
      containerConfig.containerName = "ws3";
      containerConfig.initScriptPath = scriptPath;
      containerConfig.dbStructureType = dbStructureType;
      new DBInitializer(getJNDIConnection(), containerConfig).init();
   }

   @Override
   public void setUp() throws Exception
   {
      setUp("/conf/storage/jcr-sjdbc.sql", DatabaseStructureType.SINGLE);
      Connection con = null;
      Statement st = null;
      try
      {
         con = getJNDIConnection();
         st = con.createStatement();
         st.executeUpdate("insert into JCR_SITEM values" + "('A','A','test1',20090525,'ws3',2,1233,1,10,5)");
         st.executeUpdate("insert into JCR_SITEM values" + "('B','A','test2',20090625,'ws3',1,1233,5,10,4)");
         st.executeUpdate("insert into JCR_SITEM values" + "('C','B','test3',20090825,'ws3',1,1233,5,10,2)");
         st.executeUpdate("insert into JCR_SITEM values" + "('ws3B','A','test4',2009525,'ws3',2,1233,1,10,5)");
         st.executeUpdate("insert into JCR_SVALUE(DATA,ORDER_NUM,PROPERTY_ID,STORAGE_DESC) values"
            + "('0xfa',16,'A','testConn')");
         st.executeUpdate("insert into JCR_SVALUE(DATA,ORDER_NUM,PROPERTY_ID,STORAGE_DESC) values"
            + "('0xce',16,'B','testConn2')");
         st.executeUpdate("insert into JCR_SREF values" + "('D','A',2)");
         st.executeUpdate("insert into JCR_SREF values" + "('E','B',2)");
         JDBCDataContainerConfig jdbcDataContainerConfig = new JDBCDataContainerConfig();
         jdbcDataContainerConfig.containerName = "ws3";
         jdbcDataContainerConfig.spoolConfig = SpoolConfig.getDefaultSpoolConfig();
         jdbcDataContainerConfig.spoolConfig.maxBufferSize = 10;
         jdbcDataContainerConfig.dbStructureType = DatabaseStructureType.SINGLE;
         jdbcConn = new SingleDbJDBCConnection(getJNDIConnection(), false, jdbcDataContainerConfig);
         tableType = "S";
      }
      catch (SQLException se)
      {
         fail(se.toString());
      }
      finally
      {
         if (st != null)
            st.close();
         if (con != null)
         {
            con.commit();
            con.close();
         }
      }
   }

   /* (non-Javadoc)
    * @see org.exoplatform.services.jcr.impl.storage.jdbc.JDBCConnectionTestBase#tearDown()
    */
   @Override
   protected void tearDown() throws Exception
   {
      super.tearDown();
      Connection con = null;
      Statement st = null;
      try
      {
         con = getJNDIConnection();
         st = con.createStatement();
         st.executeUpdate("drop table JCR_SREF");
         st.executeUpdate("drop table JCR_SVALUE");
         st.executeUpdate("drop table JCR_SITEM");
      }
      catch (SQLException se)
      {
         fail(se.toString());
      }
      finally
      {
         if (st != null)
            st.close();
         if (con != null)
         {
            con.commit();
            con.close();
         }
      }
   }
}