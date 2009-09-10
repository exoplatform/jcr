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

package org.exoplatform.services.jcr.impl.core.query.lucene;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.exoplatform.services.jcr.impl.core.query.BaseQueryTest;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS Author : Sergey Karpenko <sergey.karpenko@exoplatform.com.ua>
 * 
 * @version $Id: TestIndexRules.java 11908 2008-03-13 16:00:12Z ksm $
 */

public class TestIndexRules extends BaseQueryTest
{

   public final String fName = "FileName";

   private Node rootNode;

   public void setUp() throws Exception
   {
      super.setUp();
      rootNode = root.addNode("indrootparent");
      rootNode.setProperty("priority", "low");
      root.save();
   }

   /**
    * <index-rule nodeType="nt:unstructured"> <property>DifText</property> </index-rule>
    */
   /*
    * public void testPropAddConfiguration() throws Exception{ //create Node NodeImpl n =
    * (NodeImpl)root.addNode(fName); n.setProperty("DifText","blabla");
    * n.setProperty("OtherProp","gig"); root.save(); //check IndexReader reader =
    * defaultSearchIndex.getIndexReader(false); IndexSearcher is = new IndexSearcher(reader);
    * TermQuery query = new TermQuery(new Term(FieldNames.FULLTEXT, "blabla")); //_PREFIX+fName Hits
    * result = is.search(query); assertEquals(1,result.length()); query = new TermQuery(new
    * Term(FieldNames.FULLTEXT, "gig")); //_PREFIX+fName result = is.search(query);
    * assertEquals(0,result.length()); }
    */

   /**
    * <index-rule nodeType="nt:unstructured" boost="2.0" condition="@priority = 'high'">
    * <property>Text</property> </index-rule> <index-rule nodeType="nt:unstructured">
    * <property>DifText</property> </index-rule>
    */
   /*
    * public void testConditionConfiguration() throws Exception{ // create Node NodeImpl n =
    * (NodeImpl)root.addNode(fName); n.setProperty("Text","blabla");
    * n.setProperty("priority","high"); n.setProperty("DifText","second"); root.save(); // check
    * IndexReader reader = defaultSearchIndex.getIndexReader(false); IndexSearcher is = new
    * IndexSearcher(reader); TermQuery query = new TermQuery(new Term(FieldNames.FULLTEXT,
    * "blabla")); Hits result = is.search(query); assertEquals(1,result.length()); query = new
    * TermQuery(new Term(FieldNames.FULLTEXT, "second")); result = is.search(query);
    * assertEquals(0,result.length()); }
    */

   /**
    * <index-rule nodeType="nt:unstructured" boost="2.0" condition="@priority = 'high'">
    * <property>Text</property> </index-rule> <index-rule nodeType="nt:unstructured">
    * <property>DifText</property> </index-rule>
    */
   /*
    * public void testConditionConfigurationWrongConditionValue() throws Exception{ Node n =
    * (NodeImpl)root.addNode(fName); n.setProperty("Text","blabla"); n.setProperty("priority","low");
    * n.setProperty("DifText","second"); root.save(); // check IndexReader reader =
    * defaultSearchIndex.getIndexReader(false); IndexSearcher is = new IndexSearcher(reader);
    * TermQuery query = new TermQuery(new Term(FieldNames.FULLTEXT, "blabla")); Hits result =
    * is.search(query); assertEquals(0,result.length()); query = new TermQuery(new
    * Term(FieldNames.FULLTEXT, "second")); result = is.search(query);
    * assertEquals(1,result.length()); }
    */

   public void testParentCondition() throws Exception
   {

      // create Node
      // assertNotNull(rootNode);
      Node n = rootNode.addNode(fName);
      n.setProperty("Text", "blabla");
      n.setProperty("DifText", "second");
      root.save();

      // check
      IndexReader reader = defaultSearchIndex.getIndexReader(false);
      IndexSearcher is = new IndexSearcher(reader);
      TermQuery query = new TermQuery(new Term(FieldNames.FULLTEXT, "blabla"));
      Hits result = is.search(query);
      assertEquals(1, result.length());

      query = new TermQuery(new Term(FieldNames.FULLTEXT, "second"));
      result = is.search(query);
      assertEquals(0, result.length());
   }

   public void testAncestorCondition() throws Exception
   {

      // create Node
      Node rn = rootNode.addNode("hippo");
      Node n = rn.addNode(fName);
      n.setProperty("Text", "blabla");
      n.setProperty("DifText", "second");
      root.save();

      // check
      IndexReader reader = defaultSearchIndex.getIndexReader(false);
      IndexSearcher is = new IndexSearcher(reader);
      TermQuery query = new TermQuery(new Term(FieldNames.FULLTEXT, "blabla"));
      Hits result = is.search(query);
      assertEquals(1, result.length());

      query = new TermQuery(new Term(FieldNames.FULLTEXT, "second"));
      result = is.search(query);
      assertEquals(0, result.length());
   }

   public void testChildCondition() throws Exception
   {

      // create Node
      Node n = root.addNode(fName);
      n.setProperty("Text", "blabla");
      n.setProperty("DifText", "second");
      Node cn = n.addNode("indrootchild");
      cn.setProperty("priority", "gg");
      root.save();

      // check
      IndexReader reader = defaultSearchIndex.getIndexReader(false);
      IndexSearcher is = new IndexSearcher(reader);
      TermQuery query = new TermQuery(new Term(FieldNames.FULLTEXT, "blabla"));
      Hits result = is.search(query);
      assertEquals(1, result.length());

      query = new TermQuery(new Term(FieldNames.FULLTEXT, "second"));
      result = is.search(query);
      assertEquals(0, result.length());
   }

}
