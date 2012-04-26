/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.api.core.query;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.query.BaseQueryTest;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex;

import javax.jcr.Node;
import javax.jcr.query.Query;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 *
 * @author ha_dangviet
 *
 */
public class TestExcerptHighlighting extends BaseQueryTest
{
   public final String testFieldValue =
      "<p><span style=\"font-family: Arial , FreeSans;font-size: 13.0px;line-height: 16.0px;\">library book pen</span></p>"
         + "<p><span style=\"font-family: Arial , FreeSans;font-size: 13.0px;line-height: 16.0px;\">my book</span></p>"
         + "<p><span style=\"font-family: Arial , FreeSans;font-size: 13.0px;line-height: 16.0px;\">your book </span></p";

   Node testRoot = null;

   private SearchManager searchManager;

   private SearchIndex searchIndex;

   private String oldAnalyzerClassName;

   private boolean oldHighlightingSupport;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      ManageableRepository repository = repositoryService.getDefaultRepository();

      searchManager = (SearchManager)repository.getWorkspaceContainer("ws").getComponent(SearchManager.class);
      searchIndex = (SearchIndex)(searchManager.getHandler());

      oldHighlightingSupport = searchIndex.getSupportHighlighting();
      oldAnalyzerClassName = searchIndex.getAnalyzer();
      searchIndex.setSupportHighlighting(true);
      searchIndex.setAnalyzer("org.exoplatform.services.jcr.impl.core.query.MockAnalyzer");

      testRoot = session.getRootNode().addNode("testExcerpt");
      root.save();
   };

   @Override
   public void tearDown() throws Exception
   {
      searchIndex.setAnalyzer(oldAnalyzerClassName);
      searchIndex.setSupportHighlighting(oldHighlightingSupport);
      testRoot.remove();
      session.save();
      super.tearDown();
   }

   public void testExcerptsWithSameFiledsWithGT() throws Exception
   {
      testExcerptWithSpecifiedEndingChar(">");
   }

   public void testExcerptsWithSameFiledsWithDollar() throws Exception
   {
      testExcerptWithSpecifiedEndingChar("$");
   }

   public void testExcerptsWithSameFiledsWithPercent() throws Exception
   {
      testExcerptWithSpecifiedEndingChar("%");
   }

   public void testExcerptsWithSameFiledsWithSharp() throws Exception
   {
      testExcerptWithSpecifiedEndingChar("#");
   }

   public void testExcerptsWithSameFiledsWithAmpersand() throws Exception
   {
      testExcerptWithSpecifiedEndingChar("&");
   }

   public void testExcerptsWithSameFiledsWithAsterisk() throws Exception
   {
      testExcerptWithSpecifiedEndingChar("*");
   }

   public void testExcerptsWithSameFiledsWithQuestionMark() throws Exception
   {
      testExcerptWithSpecifiedEndingChar("?");
   }

   public void testExcerptsWithSameFiledsWithBracket() throws Exception
   {
      testExcerptWithSpecifiedEndingChar(")");
   }

   /**
    * We test if multiple Lucene {@link Document} {@link Field}s with the same name are processed correctly.
    * Special attention is paid to processing field with ending non-term character (i.e. noise character).
    */
   private void testExcerptWithSpecifiedEndingChar(String character) throws Exception
   {
      NodeImpl testNode1 = (NodeImpl)testRoot.addNode("book", "exo:article");
      testNode1.setProperty("exo:title", "book");
      testNode1.setProperty("exo:text", testFieldValue + character);
      testNode1.setProperty("exo:summary", testFieldValue + character);

      session.save();

      String sql = "SELECT excerpt(.) FROM exo:article WHERE contains(., 'book') AND jcr:path LIKE '/testExcerpt/%'";

      RowIterator rows = workspace.getQueryManager().createQuery(sql, Query.SQL).execute().getRows();
      while (rows.hasNext())
      {
         Row row = rows.nextRow();
         assertFalse(row.getValue("rep:excerpt(.)").getString().contains("<strong>ook"));
      }
   }
}
