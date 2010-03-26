/*
 * Copyright (C) 2009 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.resource;

import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;

import javax.jcr.Node;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
public class JcrURLConnectionTest extends BaseStandaloneTest
{

   private static final Log log = ExoLogger.getLogger("exo.jcr.component.ext.JcrURLConnectionTest");

   private String fname;

   private String data;

   private Node testRoot;

   public void setUp() throws Exception
   {
      super.setUp();

      assertNotNull(System.getProperty("java.protocol.handler.pkgs"));

      fname = "" + System.currentTimeMillis();
      data = "Test JCR urls " + fname;
      testRoot = root.addNode("testRoot", "nt:unstructured");

      Node file = testRoot.addNode(fname, "nt:file");
      Node d = file.addNode("jcr:content", "nt:resource");
      d.setProperty("jcr:mimeType", "text/plain");
      d.setProperty("jcr:lastModified", Calendar.getInstance());
      d.setProperty("jcr:data", new ByteArrayInputStream(data.getBytes()));
      session.save();

   }

   public void testURL() throws Exception
   {
      // NOTE don't use this under web container (found problem with tomcat)
      // It looks like ClassLoader problem.
      // Instead use next: new URL(null, spec, UnifiedNodeReference.getURLStreamHandler)
      URL url = new URL("jcr://exo:exo@db1/ws/#/jcr:system/");
      assertEquals("jcr", url.getProtocol());
      assertEquals("exo:exo", url.getUserInfo());
      assertEquals("exo:exo@db1", url.getAuthority());
      assertEquals("db1", url.getHost());
      assertEquals("/ws/", url.getPath());
      assertEquals("/jcr:system/", url.getRef());
   }

   public void testNodeRepresentation() throws Exception
   {
      // there is no node representation for nt:unstructured
      // default must work, by default work document view node representation.
      URL url = new URL("jcr://db1/ws/#/testRoot/");
      JcrURLConnection conn = (JcrURLConnection)url.openConnection();
      conn.setDoOutput(false);
      Node content = (Node)conn.getContent();
      InputStream in = conn.getInputStream();
      assertEquals("text/xml", conn.getContentType());
      assertEquals("testRoot", content.getName());
      assertEquals("/testRoot", content.getPath());
      byte[] b = new byte[0x2000];
      in.read(b);
      in.close();
      log.info(new String(b));
   }

   public void testNtFileNodeRepresentation() throws Exception
   {
      // should be work node representation for nt:file
      URL url = new URL("jcr://db1/ws/#/testRoot/" + fname + "/");
      JcrURLConnection conn = (JcrURLConnection)url.openConnection();
      conn.setDoOutput(false);

      assertEquals("text/plain", conn.getContentType());
      log.info(conn.getLastModified());
      compareStream(conn.getInputStream(), new ByteArrayInputStream(data.getBytes()));

   }

}
