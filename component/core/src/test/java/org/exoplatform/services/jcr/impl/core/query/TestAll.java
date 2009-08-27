/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.core.query;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: TestAll.java 12102 2008-03-19 13:16:27Z serg $
 */
public class TestAll
   extends TestCase
{

   /**
    * Returns a <code>Test</code> suite that executes all tests inside this package.
    * 
    * @return a <code>Test</code> suite that executes all tests inside this package.
    */
   public static Test suite()
   {
      TestSuite suite = new TestSuite("Search tests");

      // suite.addTestSuite(TestIndexingConfig.class);
      // suite.addTestSuite(TestPermission.class);

      // lucene
      // suite.addTestSuite(TestFileBasedNamespaceMappings.class);
      // suite.addTestSuite(TestNodeIndexer.class);
      // suite.addTestSuite(TestRemapping.class);
      // suite.addTestSuite(TestSearchManagerIndexing.class);
      // suite.addTestSuite(TestSystemSearchManager.class);
      // suite.addTestSuite(TestAggregateRules.class);
      // suite.addTestSuite(TestIndexRules.class);

      return suite;
   }
}
