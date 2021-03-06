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
package org.exoplatform.services.jcr.webdav.resource;

import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.webdav.WebDavConst;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * Created by The eXo Platform SARL .<br>
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public class ResourceUtil
{

   /**
    * Constructor.
    */
   private ResourceUtil()
   {
   }

   /**
    * logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.webdav.ResourceUtil");

   /**
    * If the node is file.
    * 
    * @param node node
    * @return true if node is file false if not
    */
   public static boolean isFile(Node node)
   {
      try
      {
         if (!node.isNodeType("nt:file"))
            return false;
         if (!node.getNode("jcr:content").isNodeType("nt:resource"))
            return false;
         return true;
      }
      catch (RepositoryException exc)
      {
         LOG.error(exc.getMessage(), exc);
         return false;
      }
   }

   /**
    * If the node is version.
    * 
    * @param node node
    * @return true if node is version false if not
    */
   public static boolean isVersion(Node node)
   {
      try
      {
         if (node.isNodeType("nt:version"))
            return true;
         return false;
      }
      catch (RepositoryException exc)
      {
         LOG.error(exc.getMessage(), exc);
         return false;
      }
   }

   /**
    * If the node is versionable.
    * 
    * @param node node
    * @return true if node is versionable false if not
    */
   public static boolean isVersioned(Node node)
   {
      try
      {
         if (node.isNodeType("mix:versionable"))
            return true;
         return false;
      }
      catch (RepositoryException exc)
      {
         LOG.error(exc.getMessage(), exc);
         return false;
      }
   }

   public static String generateEntityTag(Node node, String lastModifiedProperty)
      throws UnsupportedRepositoryOperationException, RepositoryException, ParseException
   {
      DateFormat dateFormat = new SimpleDateFormat(WebDavConst.DateFormat.MODIFICATION, Locale.US);
      Date lastModifiedDate = dateFormat.parse(lastModifiedProperty);
      return ((NodeImpl)node).getIdentifier() + lastModifiedDate.getTime();
   }

}
