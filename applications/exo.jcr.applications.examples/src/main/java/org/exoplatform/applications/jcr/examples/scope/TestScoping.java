/*
 * Copyright (C) 2013 eXo Platform SAS.
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
package org.exoplatform.applications.jcr.examples.scope;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.rest.resource.ResourceContainer;

import javax.enterprise.context.ContextNotActiveException;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
@Path("/scopes")
public class TestScoping implements ResourceContainer
{
   private final ExoContainer container;

   @Inject
   public TestScoping(ExoContainerContext ctx)
   {
      this.container = ctx.getContainer();
   }

   @GET
   public Response getIds()
   {
      StringBuilder result = new StringBuilder();
      result.append("<html><body>");
      try
      {
         // We first make sure that we have an active session
         getProvider().getIdSession();
         result.append("<table border=\"1\"><tr><th>Scope</th><th>Second call</th><th>First call</th></tr>");
         result.append("<tr><td>Request</td><td>").append(getProvider().getIdRequest()).append("</td>");
         result.append("<td>").append(getProvider().getIdRequest()).append("</td></tr>");
         result.append("<tr><td>Session</td><td>").append(getProvider().getIdSession()).append("</td>");
         result.append("<td>").append(getProvider().getIdSession()).append("</td></tr>");
         result.append("<tr><td>Application</td><td>").append(getProvider().getIdApplication()).append("</td>");
         result.append("<td>").append(getProvider().getIdApplication()).append("</td></tr>");
         result.append("<tr><td>Dependent</td><td>").append(getProvider().getIdDependent()).append("</td>");
         result.append("<td>").append(getProvider().getIdDependent()).append("</td></tr>");
         result.append("<tr><td>Singleton</td><td>").append(getProvider().getIdSingleton()).append("</td>");
         result.append("<td>").append(getProvider().getIdSingleton()).append("</td></tr>");
         result.append("</table>");
      }
      catch (ContextNotActiveException e)
      {
         result.append("<b>There is no active session, please sign in first and retry.</b>");
      }
      result.append("</body></html>");
      return Response.ok(result.toString(), MediaType.TEXT_HTML).build();
   }

   private RootIdProvider getProvider()
   {
      return container.getComponentInstanceOfType(RootIdProvider.class);
   }
}
