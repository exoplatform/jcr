/**
 * Created by The eXo Platform SASL.
 * @author <a href="volodymyr.krasnikov@exoplatform.com.ua">Volodymyr Krasnikov</a>
 * @version $Id: TestNodeMove.java 15:45:44
 */

package org.exoplatform.services.jcr.usecases.query;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

public class TestNodeMove
   extends BaseUsecasesTest
{

   public void testSingleSession() throws Exception
   {
      // create nodes and seek them in single session

      // - /testRootNode
      Node fakeroot = root.addNode("fakeroot", "nt:folder");
      Node subNode_1 = fakeroot.addNode("subnode1", "nt:folder");
      Node subNode_2 = fakeroot.addNode("subnode2", "nt:folder");

      Node srcNode = subNode_1.addNode("target", "nt:folder");

      String src_path = "/fakeroot/subnode1/target";
      String dest_path = "/fakeroot/subnode2/target";

      session.save();

      String sqlQuery = "SELECT * FROM nt:folder WHERE jcr:path LIKE '/fakeroot/subnode1/%' ";
      QueryManager manager = session.getWorkspace().getQueryManager();
      Query query = manager.createQuery(sqlQuery, Query.SQL);
      QueryResult queryResult = query.execute();
      NodeIterator iterator = queryResult.getNodes();

      assertTrue(iterator.getSize() == 1);

      // checks we have a node we need.
      assertEquals(src_path, iterator.nextNode().getPath());

      // move procedure
      session.getWorkspace().move(srcNode.getPath(), dest_path);

      Node test_node = (Node) session.getItem("/fakeroot/subnode2/target");

      sqlQuery = "SELECT * FROM nt:folder WHERE jcr:path LIKE '/fakeroot/subnode2/%' ";
      QueryManager manager2 = session.getWorkspace().getQueryManager();
      Query query2 = manager2.createQuery(sqlQuery, Query.SQL);
      QueryResult queryResult2 = query2.execute();
      NodeIterator n_iterator = queryResult2.getNodes();

      assertTrue(n_iterator.getSize() == 1);
      assertEquals(dest_path, n_iterator.nextNode().getPath());

   }

   public void testDiffSession() throws Exception
   {
      // target node was created in one sessions
      // attempt to seek & move node in other.

      // - /testRootNode
      Node fakeroot = root.addNode("fakeroot", "nt:folder");
      Node subNode_1 = fakeroot.addNode("subnode1", "nt:folder");
      Node subNode_2 = fakeroot.addNode("subnode2", "nt:folder");

      Node srcNode = subNode_1.addNode("target", "nt:folder");

      session.save();
      session.logout();

      session = (SessionImpl) repository.login(credentials, "ws");

      String src_path = "/fakeroot/subnode1/target";
      String dest_path = "/fakeroot/subnode2/target";

      String sqlQuery = "SELECT * FROM nt:folder WHERE jcr:path LIKE '/fakeroot/subnode1/%' ";
      QueryManager manager = session.getWorkspace().getQueryManager();
      Query query = manager.createQuery(sqlQuery, Query.SQL);

      QueryResult queryResult = query.execute();
      NodeIterator iterator = queryResult.getNodes();
      assertTrue(iterator.getSize() == 1);
      // checks we have a node we need.
      assertEquals(src_path, iterator.nextNode().getPath());

      // move procedure

      session.getWorkspace().move(srcNode.getPath(), dest_path);

      Node test_node = (Node) session.getItem("/fakeroot/subnode2/target");

      sqlQuery = "SELECT * FROM nt:folder WHERE jcr:path LIKE '/fakeroot/subnode2/%' ";
      QueryManager manager2 = session.getWorkspace().getQueryManager();
      Query query2 = manager2.createQuery(sqlQuery, Query.SQL);
      QueryResult queryResult2 = query2.execute();
      NodeIterator n_iterator = queryResult2.getNodes();

      assertTrue(n_iterator.getSize() == 1);
      assertEquals(dest_path, n_iterator.nextNode().getPath());

   }

}
