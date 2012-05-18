/***************************************************************************
 * Copyright 2001-2008 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.services.jcr.usecases.query;

import org.exoplatform.services.jcr.impl.core.ItemImpl;
import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

import javax.jcr.Node;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 * Created by The eXo Platform SARL
 * Author : Nhu Dinh Thuan
 *          nhudinhthuan@exoplatform.com
 * Nov 14, 2008  
 */
public class TestQueryWithRowIterator extends BaseUsecasesTest
{

   private String s1 = "\u043f\u00bb\u0457C\u0431\u00bb\u00a7a \u0413\u0491ng \u0414\u2018\u0413\u045ey";//"Của ông đây";

   public void testExcerpt() throws Exception
   {
      String name = "\u043f\u00bb\u0457\u0413\u0491ng";
      Node node1 = root.addNode(name);//, "exo:article"
      node1.setProperty("exo:title", "abc");
      node1.setProperty("exo:text", s1);

      Node node2 = root.addNode("Node2");
      node2.setProperty("exo:title", "Node2");
      node2.setProperty("exo:text", "Tai vi 1 nguoi");

      Node node3 = root.addNode("Node3");
      node3.setProperty("exo:title", "Node3");
      node3.setProperty("exo:text", "Ho ho ha ha");

      session.save();

      Node n = root.getNode(name);
      ItemImpl item = null;
      try
      {
         item = session.getItem("/" + name);
      }
      catch (Exception e)
      {
         // e.printStackTrace();
         fail();
      }

      assertNotNull(n);
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      Query q1 = queryManager.createQuery("select * from nt:base where jcr:path like '/" + name + "'", Query.SQL);
      QueryResult result1 = q1.execute();
      for (RowIterator it = result1.getRows(); it.hasNext();)
      {
         Row row = it.nextRow();
         String jcrPath = row.getValue("jcr:path").getString();

         try
         {
            Node node = (Node)session.getItem(jcrPath);
            assertNotNull(node);
         }
         catch (Exception e)
         {
            //e.printStackTrace();
            fail();
         }
      }
   }
}
