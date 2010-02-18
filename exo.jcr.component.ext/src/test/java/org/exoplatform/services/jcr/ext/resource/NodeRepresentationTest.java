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
import org.exoplatform.services.jcr.ext.app.ThreadLocalSessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.resource.representation.NtFileNodeRepresentation;
import org.exoplatform.services.jcr.ext.resource.representation.NtFileNodeRepresentationFactory;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.Collection;

import javax.jcr.Node;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
public class NodeRepresentationTest extends BaseStandaloneTest
{

   private NodeRepresentationService nodeRepresentationService;

   private NtFileNodeRepresentationFactory ntFileNodeRepresentationFactory;

   private Node testRoot;

   public void setUp() throws Exception
   {
      super.setUp();

      // prepare SessionProviderService
      ThreadLocalSessionProviderService sesProv =  
         (ThreadLocalSessionProviderService)container
            .getComponentInstanceOfType(ThreadLocalSessionProviderService.class);
      sesProv.setSessionProvider(null, new SessionProvider(new ConversationState(new Identity(session.getUserID()))));

      nodeRepresentationService =
         (NodeRepresentationService)container.getComponentInstanceOfType(NodeRepresentationService.class);
      assertNotNull(nodeRepresentationService);
      ntFileNodeRepresentationFactory =
         (NtFileNodeRepresentationFactory)container.getComponentInstanceOfType(NtFileNodeRepresentationFactory.class);
      assertNotNull(ntFileNodeRepresentationFactory);

      if (!root.hasNode("NodeRepresentationTest"))
      {
         testRoot = root.addNode("NodeRepresentationTest", "nt:unstructured");
         root.save();
      }
   }

   @Override
   protected void tearDown() throws Exception
   {
      testRoot.remove();
      session.save();
      super.tearDown();
   }

   public void testServiceInitialization() throws Exception
   {
      Collection<String> nts = nodeRepresentationService.getNodeTypes();
      assertTrue(nts.size() > 0);
      assertTrue(nts.contains("nt:file"));
      assertTrue(nts.contains("nt:resource"));
   }

   public void testNtFileNodeRepresentation() throws Exception
   {
      String data = "Test JCR";

      Node file = testRoot.addNode("file", "nt:file");
      Node d = file.addNode("jcr:content", "nt:resource");
      d.setProperty("jcr:mimeType", "text/plain");
      d.setProperty("jcr:lastModified", Calendar.getInstance());
      d.setProperty("jcr:data", new ByteArrayInputStream(data.getBytes()));
      session.save();

      NodeRepresentation nodeRepresentation = nodeRepresentationService.getNodeRepresentation(file, "text/plain");

      assertNotNull(nodeRepresentation);

      assertTrue(nodeRepresentation instanceof NtFileNodeRepresentation);

      // for(String n : nodeRepresentation.getPropertyNames()) {
      // System.out.println(">>>>>>>>>>>>>>>>>>> "+n+" "+nodeRepresentation);
      // }

      assertEquals(3, nodeRepresentation.getPropertyNames().size());

      compareStream(nodeRepresentation.getInputStream(), new ByteArrayInputStream(data.getBytes()));

   }

   public void testSysViewRepresentation() throws Exception
   {
      XMLViewNodeRepresentationRenderer sysView =
         (XMLViewNodeRepresentationRenderer)container
            .getComponentInstanceOfType(XMLViewNodeRepresentationRenderer.class);

      assertNotNull(sysView);

      Response resp =
         sysView.getXML(repositoryService.getDefaultRepository().getConfiguration().getName(), workspace.getName()
            + "/", "doc", null);
      assertEquals(200, resp.getStatus());
      //System.out.println(">>>>>>>>>>>>>> "+resp.getEntity().getClass()+" "++" "+resp.getEntity());
   }

}
