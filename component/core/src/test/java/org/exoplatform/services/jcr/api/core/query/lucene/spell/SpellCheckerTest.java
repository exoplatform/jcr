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
package org.exoplatform.services.jcr.api.core.query.lucene.spell;

import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 * <code>SpellCheckerTest</code> performs some spell checker tests.
 */
public class SpellCheckerTest extends AbstractJCRTest
{

   // protected void setUp() throws Exception {
   // super.setUp();
   // if (repository == null) {
   // ClassLoader cl = getClass().getClassLoader();
   // // PropertyConfigurator.configure(cl.getResource("log4j.properties"));
   // // InputStream in = cl.getResourceAsStream("repository.xml");
   // // RepositoryConfig config;
   // // try {
   // // config = RepositoryConfig.create(in, "target/repo-home");
   // // } finally {
   // // in.close();
   // // }
   // // repository = new TransientRepository(config);
   // //session = repository.login(new SimpleCredentials("user",
   // "password".toCharArray()));
   // String text = "the quick brown fox jumps over the lazy dog";
   // session.getRootNode().setProperty("prop", text);
   // session.save();
   // // wait a couple of seconds, refresh interval in test config is 5 seconds
   // try {
   // Thread.sleep(5 * 1000);
   // // perform a dummy check
   // performCheck("quick", "quick");
   // // wait again because refresh is done asynchronous
   // Thread.sleep(1 * 1000);
   // } catch (InterruptedException e) {
   // // ignore
   // }
   // }
   // if (session == null) {
   // session = repository.login(new SimpleCredentials("user",
   // "password".toCharArray()));
   // }
   // }
   //
   // protected void tearDown() throws Exception {
   // if (session != null) {
   // session.logout();
   // }
   // session = null;
   // super.tearDown();
   // }

   public void testSpellChecker() throws RepositoryException
   {
      String text = "the quick brown fox jumps over the lazy dog";
      testRootNode.setProperty("prop", text);
      superuser.save();
      // wait a couple of seconds, refresh interval in test config is 5 seconds
      try
      {
         Thread.sleep(5 * 1000);
         // perform a dummy check
         performCheck("quick", "quick");
         // wait again because refresh is done asynchronous
         Thread.sleep(1 * 1000);
      }
      catch (InterruptedException e)
      {
         // ignore
      }
      performCheck("quik", "quick");
   }

   public void testSpellCheckerComplexQuery() throws RepositoryException
   {
      String text = "the quick brown fox jumps over the lazy dog";
      testRootNode.setProperty("prop", text);
      superuser.save();
      // wait a couple of seconds, refresh interval in test config is 5 seconds
      try
      {
         Thread.sleep(5 * 1000);
         // perform a dummy check
         performCheck("quick", "quick");
         // wait again because refresh is done asynchronous
         Thread.sleep(1 * 1000);
      }
      catch (InterruptedException e)
      {
         // ignore
      }
      performCheck("quik OR (-foo bar)", "quick OR (-fox bar)");
   }

   public void testSpellCheckerCorrectWords() throws RepositoryException
   {
      String text = "the quick brown fox jumps over the lazy dog";
      testRootNode.setProperty("prop", text);
      superuser.save();
      // wait a couple of seconds, refresh interval in test config is 5 seconds
      try
      {
         Thread.sleep(5 * 1000);
         // perform a dummy check
         performCheck("quick", "quick");
         // wait again because refresh is done asynchronous
         Thread.sleep(1 * 1000);
      }
      catch (InterruptedException e)
      {
         // ignore
      }
      performCheck("quick", "quick");
   }

   protected void performCheck(String statement, String expected) throws RepositoryException
   {
      QueryManager qm = superuser.getWorkspace().getQueryManager();
      Query query = qm.createQuery("/jcr:root[rep:spellcheck('" + statement + "')]/(rep:spellcheck())", Query.XPATH);
      RowIterator rows = query.execute().getRows();
      assertEquals("no results returned", 1, rows.getSize());
      Row r = rows.nextRow();
      Value v = r.getValue("rep:spellcheck()");
      if (statement.equals(expected))
      {
         assertNull("must not return a suggestion", v);
      }
      else
      {
         assertNotNull("no suggestion returned", v);
         assertEquals("wrong suggestion returned", expected, v.getString());
      }

      query =
         qm.createQuery("select rep:spellcheck() from nt:base where " + "jcr:path = '/' and spellcheck('" + statement
            + "')", Query.SQL);
      rows = query.execute().getRows();
      assertEquals("no results returned", 1, rows.getSize());
      r = rows.nextRow();
      v = r.getValue("rep:spellcheck()");
      if (statement.equals(expected))
      {
         assertNull("must not return a suggestion", v);
      }
      else
      {
         assertNotNull("no suggestion returned", v);
         assertEquals("wrong suggestion returned", expected, v.getString());
      }
   }
}
