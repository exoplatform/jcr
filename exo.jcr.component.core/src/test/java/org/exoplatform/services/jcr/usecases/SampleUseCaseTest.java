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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.usecases;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: SampleUseCaseTest.java 11907 2008-03-13 15:36:21Z ksm $
 * 
 *          JCR Use Case test sample
 */

public class SampleUseCaseTest extends BaseUsecasesTest
{

   /**
    * Sample test. An example how to make it
    * 
    * @throws Exception
    */
   public void testSomething() throws Exception
   {
      // make sub-root with unique name;
      Node subRootNode = root.addNode("testSomething");

      // make the structure under subRootNode if you need so...
      subRootNode.setProperty("someProperty", "someValue");

      // and save if you need so...
      session.save();

      // and test
      this.assertNotNull(subRootNode);

      // you have to remove and save it at the end of method(recommended) or at the
      // tearDown() as well
      subRootNode.remove();
      session.save();
   }
}
