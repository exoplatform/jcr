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

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.query.DefaultQueryNodeFactory;
import org.exoplatform.services.jcr.impl.core.query.QueryRootNode;
import org.exoplatform.services.jcr.impl.core.query.xpath.XPathQueryBuilder;

import java.util.Arrays;

public class PathQueryNodeTest extends AbstractJCRTest
{

   private static final DefaultQueryNodeFactory QUERY_NODE_FACTORY =
      new DefaultQueryNodeFactory(Arrays.asList(new InternalQName[]{Constants.NT_NODETYPE}));

   // private static final NameResolver JCR_RESOLVER = new DefaultNamePathResolver(new
   // NamespaceResolver() {
   //
   // public String getPrefix(String uri) {
   // throw new UnsupportedOperationException();
   // }
   //        
   // public String getURI(String prefix) {
   // if (Name.NS_JCR_PREFIX.equals(prefix))
   // return Name.NS_JCR_URI;
   // if (Name.NS_NT_PREFIX.equals(prefix))
   // return Name.NS_NT_URI;
   // return "";
   // }
   // });

   public void testNeedsSystemTree() throws Exception
   {
      LocationFactory JCR_RESOLVER = ((SessionImpl)testRootNode.getSession()).getLocationFactory();
      QueryRootNode queryRootNode = XPathQueryBuilder.createQuery("/jcr:root/*", JCR_RESOLVER, QUERY_NODE_FACTORY);
      assertTrue(queryRootNode.needsSystemTree());

      queryRootNode = XPathQueryBuilder.createQuery("/jcr:root/test/*", JCR_RESOLVER, QUERY_NODE_FACTORY);
      assertFalse(queryRootNode.needsSystemTree());

      queryRootNode = XPathQueryBuilder.createQuery("*", JCR_RESOLVER, QUERY_NODE_FACTORY);
      assertTrue(queryRootNode.needsSystemTree());

      queryRootNode = XPathQueryBuilder.createQuery("jcr:system/*", JCR_RESOLVER, QUERY_NODE_FACTORY);
      assertTrue(queryRootNode.needsSystemTree());

      queryRootNode = XPathQueryBuilder.createQuery("test//*", JCR_RESOLVER, QUERY_NODE_FACTORY);
      assertFalse(queryRootNode.needsSystemTree());

      queryRootNode = XPathQueryBuilder.createQuery("//test/*", JCR_RESOLVER, QUERY_NODE_FACTORY);
      assertTrue(queryRootNode.needsSystemTree());
   }

   public void testNeedsSystemTreeForAllNodesByNodeType() throws Exception
   {
      LocationFactory JCR_RESOLVER = ((SessionImpl)testRootNode.getSession()).getLocationFactory();
      QueryRootNode queryRootNode =
         XPathQueryBuilder.createQuery("//element(*, nt:resource)", JCR_RESOLVER, QUERY_NODE_FACTORY);
      assertFalse(queryRootNode.needsSystemTree());

      queryRootNode =
         XPathQueryBuilder
            .createQuery("//element(*, nt:resource)[@jcr:test = 'foo']", JCR_RESOLVER, QUERY_NODE_FACTORY);
      assertFalse(queryRootNode.needsSystemTree());

      queryRootNode = XPathQueryBuilder.createQuery("//element(*, nt:nodeType)", JCR_RESOLVER, QUERY_NODE_FACTORY);
      assertTrue(queryRootNode.needsSystemTree());
   }
}
