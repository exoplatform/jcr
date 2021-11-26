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

package org.exoplatform.frameworks.jcr.cli;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS
 * 
 * @author Vitaliy Obmanjuk
 * @version $Id: $
 */

public class GetPropertiesCommand extends AbstractCliCommand
{

   @Override
   public boolean perform(CliAppContext ctx)
   {
      StringBuilder output;
      try
      {
         Node currentNode = (Node)ctx.getCurrentItem();
         PropertyIterator propertyIterator = currentNode.getProperties();
         output = new StringBuilder("Properties:\n");
         while (propertyIterator.hasNext())
         {
            Property property = propertyIterator.nextProperty();
            output.append(property.getName()).append("\n");
         }
      }
      catch (RepositoryException e)
      {
         output = new StringBuilder("Can't execute command - ").append(e.getMessage()).append("\n");
      }
      ctx.setOutput(output.toString());
      return false;
   }
}
