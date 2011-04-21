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
package org.exoplatform.services.jcr.ext.resource.representation;

import org.exoplatform.services.jcr.ext.resource.NodeRepresentation;
import org.exoplatform.services.jcr.ext.resource.NodeRepresentationFactory;
import org.exoplatform.services.jcr.ext.resource.NodeRepresentationService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
public class NtFileNodeRepresentationFactory implements NodeRepresentationFactory
{

   /**
    * Logger.
    */
   private static Log LOG = ExoLogger.getLogger("exo.jcr.component.ext.NtFileNodeRepresentationFactory");

   protected NodeRepresentationService nodeRepresentationService;

   /**
    * @param nodeRepresentationService
    */
   public NtFileNodeRepresentationFactory(NodeRepresentationService nodeRepresentationService)
   {
      this.nodeRepresentationService = nodeRepresentationService;
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.ext.resource.NodeRepresentationFactory#createNodeRepresentation(
    * javax.jcr.Node, java.lang.String)
    */
   public NodeRepresentation createNodeRepresentation(Node node, String mediaTypeHint)
   {

      try
      {

         NodeRepresentation content =
            nodeRepresentationService.getNodeRepresentation(node.getNode("jcr:content"), mediaTypeHint);

         // return nodeRepresentationService.getNodeRepresentation(node.getNode("jcr:content"),
         // mediaTypeHint);
         return new NtFileNodeRepresentation(node, content);

      }
      catch (RepositoryException e)
      {
         LOG.error(e.getLocalizedMessage(), e);
      }
      return null;
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.ext.resource.NodeRepresentationFactory#getNodeType()
    */
   public String getNodeType()
   {
      return "nt:file";
   }

}
