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

package org.exoplatform.services.jcr.impl.jndi;

import org.exoplatform.services.jcr.JcrImplBaseTest;

import javax.jcr.Repository;
import javax.naming.InitialContext;

/**
 * Created by The eXo Platform SAS.
 * 
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: RepositoryBindingTest.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class RepositoryBindingTest extends JcrImplBaseTest
{

   public void setUp() throws Exception
   {
      super.setUp();
   }

   /**
    * Prerequisites: there should be entry in configuration.xml like: <component-plugin>
    * <name>bind.datasource</name> <set-method>addPlugin</set-method>
    * <type>org.exoplatform.services.naming.BindReferencePlugin</type> <init-params> <value-param>
    * <name>bind-name</name> <value>repo</value> </value-param> <value-param> <name>class-name</name>
    * <value>javax.jcr.Repository</value> </value-param> <value-param> <name>factory</name>
    * <value>org.exoplatform.services.jcr.impl.jndi.BindableRepositoryFactory</value> </value-param>
    * <properties-param> <name>ref-addresses</name> <description>ref-addresses</description>
    * <property name="repositoryName" value="db1"/> <!-- property name="containerConfig" value=""/
    * --> </properties-param> </init-params> </component-plugin>
    * 
    * 
    * @throws Exception
    */
   public void testIfConfiguredRepositoryBound() throws Exception
   {

      InitialContext ctx = new InitialContext();
      Repository rep = (Repository)ctx.lookup("repo");
      assertNotNull(rep);
   }
}
