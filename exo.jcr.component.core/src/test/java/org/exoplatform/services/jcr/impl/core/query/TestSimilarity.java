/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.core.query;

import org.exoplatform.services.jcr.api.core.query.AbstractIndexingTest;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestSimilarity.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestSimilarity extends AbstractIndexingTest
{

   public void testFindSimilarNodes() throws Exception
   {
      //base node
      Node file = testRootNode.addNode("baseFile", "nt:file");
      Node resource = file.addNode("jcr:content", "nt:resource");
      resource.setProperty("jcr:lastModified", Calendar.getInstance());
      resource.setProperty("jcr:encoding", "UTF-8");
      resource.setProperty("jcr:mimeType", "text/plain");
      resource
         .setProperty(
            "jcr:data",
            "Similarity is determined by looking up terms that are common to nodes. "
               + "There are some conditions that must be met for a term to be considered. This is required to limit the number possibly relevant terms."
               + "Only terms with at least 4 characters are considered."
               + "Only terms that occur at least 2 times in the source node are considered."
               + "Only terms that occur in at least 5 nodes are considered.");
      session.save();

      // target nodes
      Node target1 = testRootNode.addNode("target1", "nt:file");
      Node resource1 = target1.addNode("jcr:content", "nt:resource");
      resource1.setProperty("jcr:lastModified", Calendar.getInstance());
      resource1.setProperty("jcr:encoding", "UTF-8");
      resource1.setProperty("jcr:mimeType", "text/plain");
      resource1.setProperty("jcr:data", "Similarity is determined by looking up terms that are common to nodes.");

      Node target2 = testRootNode.addNode("target2", "nt:file");
      Node resource2 = target2.addNode("jcr:content", "nt:resource");
      resource2.setProperty("jcr:lastModified", Calendar.getInstance());
      resource2.setProperty("jcr:encoding", "UTF-8");
      resource2.setProperty("jcr:mimeType", "text/plain");
      resource2.setProperty("jcr:data", "There is no you know what");

      Node target3 = testRootNode.addNode("target3", "nt:file");
      Node resource3 = target3.addNode("jcr:content", "nt:resource");
      resource3.setProperty("jcr:lastModified", Calendar.getInstance());
      resource3.setProperty("jcr:encoding", "UTF-8");
      resource3.setProperty("jcr:mimeType", "text/plain");
      resource3.setProperty("jcr:data", "Terms occures here terms");

      session.save();

      //Lets find similar nodes - will return base and similar target nodes

      // make SQL query
      QueryManager qman = session.getWorkspace().getQueryManager();

      Query q =
         qman.createQuery("select * from nt:resource where similar(.,'/testroot/baseFile/jcr:content')", Query.SQL);
      QueryResult result = q.execute();
      assertEquals(3, result.getNodes().getSize());
      checkResult(result, new Node[]{resource, resource1, resource3});

      //make XPath query

      Query xq =
         qman.createQuery("//element(*, nt:resource)[rep:similar(., '/testroot/baseFile/jcr:content')]", Query.XPATH);
      QueryResult xres = xq.execute();
      assertEquals(3, xres.getNodes().getSize());
      checkResult(xres, new Node[]{resource, resource1, resource3});

   }

   public void testSimilar() throws RepositoryException
   {
      executeQuery("//*[rep:similar(., '" + testRootNode.getPath() + "')]");
      executeQuery("//*[rep:similar(node, '" + testRootNode.getPath() + "')]");
   }

}
