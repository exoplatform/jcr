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

package org.exoplatform.services.jcr.impl.ext.action;

import org.exoplatform.services.command.action.ActionMatcher;
import org.exoplatform.services.command.action.Condition;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.QPath;

import java.util.Arrays;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: SessionEventMatcher.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class SessionEventMatcher implements ActionMatcher
{

   /**
    * Key describe an Event name to be listened to.
    */
   public static final String EVENTTYPE_KEY = "types";

   /**
    * Key describe a workspace
    */
   public static final String WORKSPACE_KEY = "workspaces";

   /**
    * Key describe an Item absolute paths
    */
   public static final String PATH_KEY = "paths";

   /**
    * Key describe an InternalQName[] array of current node NodeType names.
    */
   public static final String NODETYPES_KEY = "nodeTypes";

   /**
    * Current item.
    */
   public static final String CURRENT_ITEM = "currentItem";

   private final int eventTypes;

   private final String[] workspaces;

   private final QPath[] paths;

   private boolean isDeep;

   private final InternalQName[] nodeTypeNames;

   private final String[] ignoredProperties;

   private final NodeTypeDataManager typeDataManager;

   public SessionEventMatcher(int eventTypes, QPath[] paths, boolean isDeep, String[] workspaces,
                              InternalQName[] nodeTypeNames, NodeTypeDataManager typeDataManager, String[] ignoredProperties)
   {
      super();
      this.eventTypes = eventTypes;
      this.paths = paths;
      this.isDeep = isDeep;

      this.nodeTypeNames = nodeTypeNames;
      this.ignoredProperties = ignoredProperties;
      this.workspaces = workspaces;
      this.typeDataManager = typeDataManager;
   }

   public String dump()
   {
      StringBuilder str = new StringBuilder("SessionEventMatcher: ").append(eventTypes).append("\n");

      if (paths != null)
      {
         str.append("Paths (isDeep=").append(isDeep).append("):\n");

         for (QPath p : paths)
         {
            str.append(p.getAsString()).append("\n");
         }
      }

      if (nodeTypeNames != null)
      {
         str.append("Node Types:\n");
         for (InternalQName n : nodeTypeNames)
         {
            str.append(n.getAsString()).append("\n");
         }
      }

      return str.toString();
   }

   public final boolean match(Condition conditions)
   {

      if (conditions.get(EVENTTYPE_KEY) == null || !isEventTypeMatch((Integer)conditions.get(EVENTTYPE_KEY)))
      {
         return false;
      }

      if (!isPathMatch((QPath)conditions.get(PATH_KEY)))
      {
         return false;
      }

      if (nodeTypeNames != null)
      {
         if (!isNodeTypesMatch((InternalQName[])conditions.get(NODETYPES_KEY)))
         {
            return false;
         }
      }

      if (conditions.get(CURRENT_ITEM) != null && ignoredProperties != null && isIgnoredProperties((String) conditions.get(CURRENT_ITEM))) {
         return false;
      }

      if (!isWorkspaceMatch((String)conditions.get(WORKSPACE_KEY)))
      {
         return false;
      }

      return internalMatch(conditions);
   }

   private boolean isEventTypeMatch(int type)
   {
      return (eventTypes & type) > 0;
   }

   private boolean isNodeTypesMatch(InternalQName[] nodeTypes)
   {
      if (this.nodeTypeNames == null || nodeTypes == null)
         return true;
      for (InternalQName nt : nodeTypeNames)
      {
         if (typeDataManager.isNodeType(nt, nodeTypes))
            return true;
         // for (InternalQName searchNt : nodeTypes) {
         // if (nt.equals(searchNt))
         // return true;
         // }
      }
      return false;
   }

   private boolean isPathMatch(QPath itemPath)
   {
      if (this.paths == null || itemPath == null)
         return true;

      for (QPath p : paths)
      {
         if (itemPath.equals(p) || itemPath.isDescendantOf(p, !isDeep))
            return true;
      }

      return false;
   }

   private boolean isWorkspaceMatch(String workspace)
   {
      if (this.workspaces == null || workspace == null)
         return true;
      for (String ws : workspaces)
      {
         if (ws.equals(workspace))
            return true;
      }

      return false;
   }

   private boolean isIgnoredProperties(String propertyName)
   {
      return Arrays.asList(this.ignoredProperties).contains(propertyName);
   }

   protected boolean internalMatch(Condition conditions)
   {
      return true;
   }
}
