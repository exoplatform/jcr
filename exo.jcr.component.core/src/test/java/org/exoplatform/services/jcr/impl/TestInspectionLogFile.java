/*
 * Copyright (C) 2011 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.jcr.impl;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;

/**
 * @author <a href="mailto:skarpenko@exoplatform.com">Sergiy Karpenko</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 11 ����. 2011 skarpenko $
 *
 */
public class TestInspectionLogFile extends TestCase
{

   private InspectionReport report;

   public void setUp() throws Exception
   {
      super.setUp();

      report = new InspectionReport("test");
   }

   public void tearDown() throws Exception
   {
      report.close();
      getFileFromReport().delete();

      super.tearDown();
   }

   public void testLogComment() throws Exception
   {
      report.logComment("test message");

      // read file;
      Reader reader = new FileReader(getFileFromReport());
      BufferedReader br = new BufferedReader(reader);
      String s = br.readLine();
      br.close();
      assertEquals("//test message", s);
      assertFalse(report.hasInconsistency());
   }

   public void testLogInspectionDescription() throws Exception
   {
      report.logDescription("description");

      // read file;
      Reader reader = new FileReader(getFileFromReport());
      BufferedReader br = new BufferedReader(reader);
      String s = br.readLine();
      br.close();
      assertEquals("//description", s);
      assertFalse(report.hasInconsistency());
   }

   public void testLogBrokenObjectInfo() throws Exception
   {
      report.logBrokenObjectAndSetInconsistency("broken object descr", "message");

      // read file;
      Reader reader = new FileReader(getFileFromReport());
      BufferedReader br = new BufferedReader(reader);
      String s = br.readLine();
      br.close();

      assertEquals("broken object descr message", s);
      assertTrue(report.hasInconsistency());
   }

   public void testLogException() throws Exception
   {
      Exception e = new Exception("Exception message.");

      report.logExceptionAndSetInconsistency("message", e);

      // read file;
      Reader reader = new FileReader(getFileFromReport());
      BufferedReader br = new BufferedReader(reader);
      String s = br.readLine();
      assertEquals("//message", s);
      s = br.readLine();
      assertEquals("//" + e.getMessage(), s);
      s = br.readLine();
      assertEquals("//" + e.toString(), s);
      br.close();
      assertTrue(report.hasInconsistency());
   }

   private File getFileFromReport()
   {
      return new File(report.getReportPath());
   }

}
