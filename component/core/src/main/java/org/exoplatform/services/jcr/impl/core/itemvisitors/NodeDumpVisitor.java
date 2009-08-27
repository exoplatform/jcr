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
package org.exoplatform.services.jcr.impl.core.itemvisitors;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.util.TraversingItemVisitor;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: NodeDumpVisitor.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class NodeDumpVisitor
   extends TraversingItemVisitor
{

   private String dumpStr = "";

   protected void entering(Property property, int level) throws RepositoryException
   {
      dumpStr += " " + property.getPath() + "\n";
   }

   protected void entering(Node node, int level) throws RepositoryException
   {
      dumpStr += node.getPath() + "\n";
   }

   protected void leaving(Property property, int level) throws RepositoryException
   {
   }

   protected void leaving(Node node, int level) throws RepositoryException
   {
   }

   public String getDump()
   {
      return dumpStr;
   }

}
