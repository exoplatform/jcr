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

import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Created by The eXo Platform SAS Author : Sergey Karpenko <sergey.karpenko@exoplatform.com.ua>
 * 
 * @version $Id: TestFileBasedNamespaceMappings.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class TestFileBasedNamespaceMappings extends TestCase
{

   File test_mapp;

   public void testFileBasedNamespaceMappings() throws Exception
   {

      FileBasedNamespaceMappings namereg = new FileBasedNamespaceMappings(test_mapp);

      // LocationFactory resolver = new LocationFactory(namereg);

      assertEquals("", namereg.getNamespaceURIByPrefix(""));
      assertEquals("http://www.jcp.org/jcr/1.0", namereg.getNamespaceURIByPrefix("jcr"));
      assertEquals("http://www.w3.org/2004/10/xpath-functions", namereg.getNamespaceURIByPrefix("fn"));

      assertEquals("xs", namereg.getNamespacePrefixByURI("http://www.w3.org/2001/XMLSchema"));
      assertEquals("mix", namereg.getNamespacePrefixByURI("http://www.jcp.org/jcr/mix/1.0"));
   }

   @Override
   public void setUp() throws Exception
   {
      test_mapp = PrivilegedFileHelper.createTempFile("temp", "mapping");

      // Fill the namespace mappings file by prefix uri pairs

      Properties props = new Properties();

      props.setProperty("", "");
      props.setProperty("jcr", "http://www.jcp.org/jcr/1.0");
      props.setProperty("nt", "http://www.jcp.org/jcr/nt/1.0");
      props.setProperty("mix", "http://www.jcp.org/jcr/mix/1.0");
      props.setProperty("xml", "http://www.w3.org/XML/1998/namespace");
      props.setProperty("sv", "http://www.jcp.org/jcr/sv/1.0");
      props.setProperty("exo", "http://www.exoplatform.com/jcr/exo/1.0");
      props.setProperty("xs", "http://www.w3.org/2001/XMLSchema");
      props.setProperty("fn", "http://www.w3.org/2004/10/xpath-functions");

      props.store(PrivilegedFileHelper.fileOutputStream(test_mapp), "");

      props.clear();
      PrivilegedFileHelper.deleteOnExit(test_mapp);
   }

   @Override
   protected void tearDown() throws Exception
   {
      test_mapp.delete();
   }

}
