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

package org.exoplatform.services.jcr.impl.core.query;

import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * Created by The eXo Platform SAS Author : Sergey Karpenko <sergey.karpenko@exoplatform.com.ua>
 * 
 * @version $Id: $
 */

public class TestBrokenQuery
   extends BaseQueryTest
{

   public void testSQLQueryCorrect() throws Exception
   {
      try
      {
         QueryManager qman = this.workspace.getQueryManager();
         Query q = qman.createQuery("SELECT * FROM nt:resource", Query.SQL);

         QueryResult res = q.execute();
      }
      catch (InvalidQueryException e)
      {
         fail();
      }
   }

   public void testSQLQueryInCorrect() throws Exception
   {
      try
      {
         QueryManager qman = this.workspace.getQueryManager();
         Query q = qman.createQuery("SELECT * FROM nt:resource", Query.XPATH);
         QueryResult res = q.execute();
         fail();
      }
      catch (InvalidQueryException e)
      {
      }
   }

   public void testXPATHQueryCorrect() throws Exception
   {
      try
      {
         QueryManager qman = this.workspace.getQueryManager();
         Query q = qman.createQuery("*", Query.XPATH);
         QueryResult res = q.execute();
      }
      catch (InvalidQueryException e)
      {
         fail();
      }
   }

   public void testXPATHQueryInCorrect() throws Exception
   {
      try
      {
         QueryManager qman = this.workspace.getQueryManager();
         Query q = qman.createQuery("*", Query.SQL);
         QueryResult res = q.execute();
         fail();
      }
      catch (InvalidQueryException e)
      {
      }
   }

}
