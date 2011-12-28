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
package org.exoplatform.frameworks.jcr.cli;

import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.PropertyImpl;

import java.util.Iterator;

import javax.jcr.Item;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * Created by The eXo Platform SAS
 * 
 * @author Vitaliy Obmanjuk
 * @version $Id: $
 */

public class GetContextInfoCommand extends AbstractCliCommand
{

   public boolean perform(CliAppContext ctx)
   {
      StringBuilder output = new StringBuilder();
      try
      {
         output.append("Context info: \n");
         Item item = ctx.getCurrentItem();
         ItemDefinition itemDefinition;
         if (item.isNode())
         {
            itemDefinition = ((NodeImpl)item).getDefinition();
         }
         else
         {
            itemDefinition = ((PropertyImpl)item).getDefinition();
         }

         output.append("username: ").append(ctx.getUserName()).append("\n");
         output.append("workspace: ").append(ctx.getCurrentWorkspace()).append("\n");
         output.append("item path: ").append(item.getPath()).append("\n");
         output.append("item type: ").append(item.isNode() ? "Node" : "Property").append("\n");
         output.append("item definitions:\n");
         output.append("  name: ").append(itemDefinition.getName()).append("\n");
         output.append("  autocreated:").append(itemDefinition.isAutoCreated()).append("\n");
         output.append("  mandatory:").append(itemDefinition.isMandatory()).append("\n");
         output.append("  protected:").append(itemDefinition.isProtected()).append("\n");
         output.append("  onparentversion:").append(itemDefinition.getOnParentVersion()).append("\n");


         if (item.isNode() == false)
         {
            Property property = (Property)item;
            int propertyType = property.getValue().getType();
            if (propertyType != (PropertyType.BINARY))
            {
               PropertyDefinition propertyDefinition = (PropertyDefinition)itemDefinition;
               if (propertyDefinition.isMultiple() == false)
               {
                  output.append("property value:").append(property.getValue().getString()).append("\n");
               }
               else
               {
                  output.append("property value is multiple\n");
               }
            }
            else
            {
               output.append("can't show property value:\n");
            }
         }
         output.append("parameters:\n");

         Iterator parametersIterator = ctx.getParameters().iterator();
         int i = 0;
         while (parametersIterator.hasNext())
         {
            output.append("  [").append(i).append("] : ").append((String)parametersIterator.next()).append("\n");
            i++;
         }
      }
      catch (Exception e)
      {
         output.append("Can't execute command - ").append(e.getMessage()).append("\n");
      }
      ctx.setOutput(output.toString());
      return false;
   }
}
