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
package org.exoplatform.services.jcr.usecases.query;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * Created by The eXo Platform SAS Author : Alex Reshetnyak
 * alex.reshetnyak@exoplatform.org.ua reshetnyak.alex@gmail.com 20.07.2007
 * 14:05:20
 * 
 * @version $Id: TestDateBetween.java 20.07.2007 14:05:20 rainfox
 */
public class TestDateBetween extends JcrAPIBaseTest
{

   String date;

   public void setUp() throws Exception
   {
      super.setUp();

      ArrayList<String> dateList = new ArrayList<String>();
      dateList.add("2006-01-19T15:34:15.917+02:00");
      dateList.add("2005-01-19T15:34:15.917+02:00");
      dateList.add("2007-01-19T15:34:15.917+02:00");

      Node rootNode = session.getRootNode();

      for (int i = 0; i < dateList.size(); i++)
      {
         Node cool = rootNode.addNode("nnn" + i, "nt:file");
         Node contentNode = cool.addNode("jcr:content", "nt:resource");
         contentNode.setProperty("jcr:encoding", "UTF-8");
         contentNode.setProperty("jcr:data", "data _________________________________");
         contentNode.setProperty("jcr:mimeType", "text/plain");
         contentNode.setProperty("jcr:lastModified", valueFactory.createValue(dateList.get(i), PropertyType.DATE));
      }

      session.save();

      date = rootNode.getNode("nnn1").getNode("jcr:content").getProperty("jcr:lastModified").getString();
   }

   public void testDateXPath() throws Exception
   {

      String xParhQuery = "//element(*,nt:resource)[@jcr:lastModified >= xs:dateTime('2006-08-19T10:11:38.281+02:00')]";

      QueryManager manager = session.getWorkspace().getQueryManager();
      Query query = manager.createQuery(xParhQuery, Query.XPATH);
      QueryResult queryResult = query.execute();
      NodeIterator iter = queryResult.getNodes();

      assertEquals(1, iter.getSize());
   }

   public void testDateSQL() throws Exception
   {
      StringBuffer sb = new StringBuffer();
      sb.append("select * from nt:resource where ");
      sb.append("( jcr:lastModified >= TIMESTAMP '");
      sb.append("2006-06-04T15:34:15.917+02:00");
      sb.append("' )");
      sb.append(" and ");
      sb.append("( jcr:lastModified <= TIMESTAMP '");
      sb.append("2008-06-04T15:34:15.917+02:00");
      sb.append("' )");

      String sqlQuery = sb.toString();

      QueryManager manager = session.getWorkspace().getQueryManager();
      Query query = manager.createQuery(sqlQuery, Query.SQL);
      QueryResult queryResult = query.execute();
      NodeIterator iter = queryResult.getNodes();

      assertEquals(1, iter.getSize());
   }

   public void testDateBETWEEN_SQL() throws Exception
   {
      StringBuffer sb = new StringBuffer();
      sb.append("select * from nt:resource where jcr:lastModified between ");
      sb.append(" TIMESTAMP '");
      sb.append("2006-06-04T15:34:15.917+02:00");
      sb.append("'");
      sb.append(" and ");
      sb.append("TIMESTAMP '");
      sb.append("2008-06-04T15:34:15.917+02:00");
      sb.append("'");

      String sqlQuery = sb.toString();

      QueryManager manager = session.getWorkspace().getQueryManager();
      Query query = manager.createQuery(sqlQuery, Query.SQL);
      QueryResult queryResult = query.execute();
      NodeIterator iter = queryResult.getNodes();

      assertEquals(1, iter.getSize());
   }

   public void testDate_equals() throws Exception
   {
      StringBuffer sb = new StringBuffer();
      sb.append("select * from nt:resource where jcr:lastModified = ");
      sb.append("TIMESTAMP '");
      // sb.append("2005-01-19T15:34:15.917+02:00");
      sb.append(date);
      sb.append("'");

      String sqlQuery = sb.toString();

      QueryManager manager = session.getWorkspace().getQueryManager();
      Query query = manager.createQuery(sqlQuery, Query.SQL);
      QueryResult queryResult = query.execute();
      NodeIterator iter = queryResult.getNodes();

      assertEquals(1, iter.getSize());
   }

}
