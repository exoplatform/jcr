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
package org.exoplatform.services.jcr.api.core.query;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;


import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.jcr.impl.core.query.lucene.QueryResultImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LimitAndOffsetTest extends AbstractQueryTest {

    private Node node1;
    private Node node2;
    private Node node3;

    private QueryImpl query;

    protected void setUp() throws Exception {
        super.setUp();

        node1 = testRootNode.addNode("foo");
        node1.setProperty("name", "1");
        node2 = testRootNode.addNode("foo");
        node2.setProperty("name", "2");
        node3 = testRootNode.addNode("foo");
        node3.setProperty("name", "3");

        testRootNode.save();

        query = createXPathQuery("/jcr:root" + testRoot + "/* order by @name");
    }

    protected void tearDown() throws Exception {
        node1 = null;
        node2 = null;
        node3 = null;
        query = null;
        super.tearDown();
    }

    private QueryImpl createXPathQuery(String xpath)
            throws InvalidQueryException, RepositoryException {
        QueryManager queryManager = superuser.getWorkspace().getQueryManager();
        return (QueryImpl) queryManager.createQuery(xpath, Query.XPATH);
    }

    protected void checkResult(QueryResult result, Node[] expectedNodes) throws RepositoryException {
        assertEquals(expectedNodes.length, result.getNodes().getSize());
    }

    public void testLimit() throws Exception {
        query.setLimit(1);
        QueryResult result = query.execute();
        checkResult(result, new Node[] { node1 });

        query.setLimit(2);
        result = query.execute();
        checkResult(result, new Node[] { node1, node2 });

        query.setLimit(3);
        result = query.execute();
        checkResult(result, new Node[] { node1, node2, node3 });
    }

    public void testOffset() throws Exception {
        query.setOffset(0);
        QueryResult result = query.execute();
        checkResult(result, new Node[] { node1, node2, node3 });

        query.setOffset(1);
        result = query.execute();
        checkResult(result, new Node[] { node2, node3 });

        query.setOffset(2);
        result = query.execute();
        checkResult(result, new Node[] { node3 });
    }

    public void testOffsetAndLimit() throws Exception {
        query.setOffset(0);
        query.setLimit(1);
        QueryResult result = query.execute();
        checkResult(result, new Node[] { node1 });

        query.setOffset(1);
        query.setLimit(1);
        result = query.execute();
        checkResult(result, new Node[] { node2 });

        query.setOffset(1);
        query.setLimit(2);
        result = query.execute();
        checkResult(result, new Node[] { node2, node3 });

        query.setOffset(0);
        query.setLimit(2);
        result = query.execute();
        checkResult(result, new Node[] { node1, node2 });

        // Added for JCR-1323
        query.setOffset(0);
        query.setLimit(4);
        result = query.execute();
        checkResult(result, new Node[] { node1, node2, node3 });
    }

    public void testOffsetAndSkip() throws Exception {
        query.setOffset(1);
        QueryResult result = query.execute();
        NodeIterator nodes = result.getNodes();
        nodes.skip(1);
        assertTrue(nodes.nextNode() == node3);
    }

    public void testOffsetAndLimitWithGetSize() throws Exception {
        query.setOffset(1);
        QueryResult result = query.execute();
        NodeIterator nodes = result.getNodes();
        assertEquals(2, nodes.getSize());
        assertEquals(3, ((QueryResultImpl) result).getTotalSize());

        query.setOffset(1);
        query.setLimit(1);
        result = query.execute();
        nodes = result.getNodes();
        assertEquals(1, nodes.getSize());
        assertEquals(3, ((QueryResultImpl) result).getTotalSize());
    }

    public void testOffsetAndLimitWithSetPermissions() throws Exception {

        String query = "Select * from nt:base where jcr:path like '/testroot/node1/%'" +
                " and not jcr:path like '/testroot/node1/%/%'  order by title asc";
        checkResultOffsetAndLimit(query);
    }

    public void testOffsetAndLimitWithSetPermissions1() throws Exception {

        String query = "Select * from nt:base where jcr:path like '/testroot/node1/%'" +
                " and not jcr:path like '/testroot/node1/%/%'";
        checkResultOffsetAndLimit(query);
    }

    private void checkResultOffsetAndLimit(String sqlQuery) throws Exception {
        addTestNodes();
        Session session = superuser.getRepository().login(new CredentialsImpl("mary", "exo".toCharArray()));
        Query query = session.getWorkspace().getQueryManager().createQuery(sqlQuery, Query.SQL);
        ((QueryImpl) query).setOffset(2);
        ((QueryImpl) query).setLimit(2);
        QueryResult queryResult = query.execute();
        assertNotNull(queryResult);
        NodeIterator iter = queryResult.getNodes();

        String[] result = new String[]{"f", "h"};
        int p = 0;
        while (iter.hasNext()) {
            assertEquals(result[p], iter.nextNode().getName());
            p++;
        }
        long size = queryResult.getNodes().getSize();
        assertEquals(2, size);

        session.logout();
    }

    private void addTestNodes() throws Exception {
        Map<String, String[]> per = new HashMap<String, String[]>();
        Map<String, String[]> per1 = new HashMap<String, String[]>();
        per.put("*:/platform/administrators", PermissionType.ALL);
        per.put("*:/platform/users", PermissionType.ALL);
        per1.put("*:/platform/administrators", PermissionType.ALL);
        Node node1 = testRootNode.addNode("node1");

        Node a = node1.addNode("a");
        a.setProperty("title", "a");
        a.addMixin("exo:privilegeable");
        ((ExtendedNode) a).setPermissions(per1);

        Node b = node1.addNode("b");
        b.setProperty("title", "b");
        b.addMixin("exo:privilegeable");
        ((ExtendedNode) b).setPermissions(per);

        Node c = node1.addNode("c");
        c.setProperty("title", "c");
        c.addMixin("exo:privilegeable");
        ((ExtendedNode) c).setPermissions(per1);

        Node d = node1.addNode("d");
        d.setProperty("title", "d");
        d.addMixin("exo:privilegeable");
        ((ExtendedNode) d).setPermissions(per);

        Node e = node1.addNode("e");
        e.setProperty("title", "e");
        e.addMixin("exo:privilegeable");
        ((ExtendedNode) e).setPermissions(per1);

        Node f = node1.addNode("f");
        f.setProperty("title", "f");
        f.addMixin("exo:privilegeable");
        ((ExtendedNode) f).setPermissions(per);

        Node g = node1.addNode("g");
        g.setProperty("title", "g");
        g.addMixin("exo:privilegeable");
        ((ExtendedNode) g).setPermissions(per1);

        Node h = node1.addNode("h");
        h.setProperty("title", "h");
        h.addMixin("exo:privilegeable");
        ((ExtendedNode) h).setPermissions(per);

        Node i = node1.addNode("i");
        i.setProperty("title", "i");
        i.addMixin("exo:privilegeable");
        ((ExtendedNode) i).setPermissions(per1);

        Node j = node1.addNode("j");
        j.setProperty("title", "j");
        j.addMixin("exo:privilegeable");
        ((ExtendedNode) j).setPermissions(per);

        testRootNode.save();
    }

}
