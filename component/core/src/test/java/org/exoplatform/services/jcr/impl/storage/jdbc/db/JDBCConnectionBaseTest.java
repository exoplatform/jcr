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
package org.exoplatform.services.jcr.impl.storage.jdbc.db;

import java.sql.*;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import junit.framework.TestCase;

public class JDBCConnectionBaseTest
   extends TestCase
{
   final String URL = "jdbc:mysql://mysql.exoua-int/test";

   final String USER = "asistant";

   final String PASSWORD = "asistant";

   public Connection connect = null;

   public MultiDbJDBCConnection mdbc = null;

   public SingleDbJDBCConnection sdbc = null;

   public Statement st = null;

   public ResultSet rs;

   @Override
   public void setUp() throws Exception
   {

      super.setUp();
      DriverManager.registerDriver((Driver) Class.forName("com.mysql.jdbc.Driver").newInstance());
      connect = DriverManager.getConnection(URL, USER, PASSWORD);
      st = connect.createStatement();
      try
      {
         st.executeUpdate("insert into JCR_SITEM values"
                  + "('12345','123456','Sam',20090525,'myContainer',2,1233,5,10,1)");
         st.executeUpdate("insert into JCR_SITEM values"
                  + "('myContainer123','1235','Nick',20090625,'myContainer',1,1233,5,10,1)");
         st.executeUpdate("insert into JCR_SVALUE values" + "(12,'hello',16,'12345','Say')");
         st.executeUpdate("insert into JCR_SVALUE values" + "(127,'love',16,'aa74d2','java')");
         st.executeUpdate("insert into JCR_SREF values" + "('45as1','12345',2)");
         st.executeUpdate("insert into JCR_SREF values" + "('78710','5987',2)");
         sdbc = new SingleDbJDBCConnection(connect, "myContainer", null, 10, null, null);

         st.executeUpdate("insert into JCR_MITEM values" + "('12345','123456','Sam',20090525,2,1233,5,10,1)");
         st.executeUpdate("insert into JCR_MITEM values" + "('123','1235','Nick',20090625,1,1233,5,10,1)");
         st.executeUpdate("insert into JCR_MVALUE values" + "(12,'hello',16,'12345','Say')");
         st.executeUpdate("insert into JCR_MVALUE values" + "(45,'winner',15,'1278','beauty')");
         st.executeUpdate("insert into JCR_MREF values" + "('45re1','00214',2)");
         st.executeUpdate("insert into JCR_MREF values" + "('45as1','12345',2)");
         mdbc = new MultiDbJDBCConnection(connect, "mycontainer", null, 10, null, null);
      }
      catch (SQLException se)
      {
         fail(se.toString());
      }
   }

   @Override
   protected void tearDown() throws Exception
   {

      try
      {
         st.executeUpdate("delete from JCR_SITEM");
         st.executeUpdate("delete from JCR_SVALUE");
         st.executeUpdate("delete from JCR_SREF");
         st.executeUpdate("delete from JCR_MITEM");
         st.executeUpdate("delete from JCR_MVALUE");
         st.executeUpdate("delete from JCR_MREF");

      }
      catch (SQLException se)
      {
         fail(se.toString());
      }
      finally
      {
         st.close();
         connect.close();
      }
      super.tearDown();
   }

   public NodeData giveNode() throws Exception
   {
      InternalQName[] iqn =
      {InternalQName.parse("[]DbJDBCConnectionEditTest")};
      TransientNodeData tnd =
               new TransientNodeData(Constants.JCR_NODETYPES_PATH, "123", 2, Constants.SV_NODE_NAME, iqn, 7, "4512",
                        null);
      return tnd;
   }

   public NodeData setNode() throws Exception
   {
      InternalQName[] iqn =
      {InternalQName.parse("[]DbJDBCConnectionEditTest")};
      TransientNodeData tnd =
               new TransientNodeData(Constants.JCR_NODETYPES_PATH, "245", 9, Constants.SV_NODE_NAME, iqn, 8, "7870",
                        null);
      return tnd;
   }

   public void makeFindDB(String sql) throws Exception
   {
      rs = st.executeQuery(sql);
      rs.next();
   }

   public void ifDeleted(String sql) throws Exception
   {
      rs = st.executeQuery(sql);
   }
}