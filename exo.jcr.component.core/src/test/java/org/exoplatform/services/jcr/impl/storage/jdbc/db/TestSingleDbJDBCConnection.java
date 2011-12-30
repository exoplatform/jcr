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

import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCConnectionTestBase;
import org.exoplatform.services.jcr.impl.storage.jdbc.init.StorageDBInitializer;

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

   private void setUp(String scriptPath, boolean multiDB) throws Exception
   {
      super.setUp();
      new StorageDBInitializer("ws3", getJNDIConnection(), scriptPath, multiDB).init();
   }
   
   @Override
   public void setUp() throws Exception
   {
      setUp("/conf/storage/jcr-sjdbc.sql", false);
      try
      {
         Statement st = getJNDIConnection().createStatement();
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
         st.close();
         jdbcConn = new SingleDbJDBCConnection(getJNDIConnection(), false, "ws3", null, 10, null, null);
         tableType = "S";
      }
      catch (SQLException se)
      {
         fail(se.toString());
      }
   }

   /* (non-Javadoc)
    * @see org.exoplatform.services.jcr.impl.storage.jdbc.JDBCConnectionTestBase#tearDown()
    */
   @Override
   protected void tearDown() throws Exception
   {

      try
      {
         Statement st = getJNDIConnection().createStatement();
         st.executeUpdate("drop table JCR_SREF");
         st.executeUpdate("drop table JCR_SVALUE");
         st.executeUpdate("drop table JCR_SITEM");
         st.close();
      }
      catch (SQLException se)
      {
         fail(se.toString());
      }
      super.tearDown();
   }
}