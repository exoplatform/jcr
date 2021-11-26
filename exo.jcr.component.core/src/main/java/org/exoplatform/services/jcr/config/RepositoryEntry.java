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

package org.exoplatform.services.jcr.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by The eXo Platform SAS<br>
 *
 * The repository configuration bean
 *
 * @author <a href="mailto:gavrikvetal@gmail.com">Vitaliy Gulyy</a>
 * @LevelAPI Unsupported
 */

public class RepositoryEntry extends RepositoryInfo
{

   protected List<WorkspaceEntry> workspaces;

   public RepositoryEntry()
   {
      workspaces = new ArrayList<WorkspaceEntry>();
   }

   /**
    * Get workspaces.
    * 
    * @return Returns the workspaces.
    */
   public List<WorkspaceEntry> getWorkspaceEntries()
   {
      return workspaces;
   }

   /**
    * Set workspaces.
    * 
    * @param workspaces
    *          the list of WorkspaceEntry-s
    */
   public void setWorkspaceEntries(List<WorkspaceEntry> workspaces)
   {
      this.workspaces = workspaces;
   }

   /**
    * adds workspace entry object
    * @param ws
    *          the WorkspaceEntry
    */
   public void addWorkspace(WorkspaceEntry ws)
   {
      workspaces.add(ws);
   }

   /**
    * Merges the current {@link RepositoryEntry} with the given one. The current {@link RepositoryEntry}
    * has the highest priority thus only absent data will be overrode
    * @param entry the entry to merge with the current {@link RepositoryEntry}
    */
   void merge(RepositoryEntry entry)
   {
      merge((RepositoryInfo)entry);
      List<WorkspaceEntry> workspaceEntries = entry.workspaces;
      if (workspaceEntries == null || workspaceEntries.isEmpty())
      {
         return;
      }
      if (workspaces == null || workspaces.isEmpty())
      {
         this.workspaces = workspaceEntries;
         return;
      }
      Map<String, WorkspaceEntry> mWorkspaceEntries = new LinkedHashMap<String, WorkspaceEntry>();
      for (WorkspaceEntry wkEntry : workspaceEntries)
      {
         mWorkspaceEntries.put(wkEntry.getName(), wkEntry);
      }
      for (WorkspaceEntry wkEntry : workspaces)
      {
         mWorkspaceEntries.put(wkEntry.getName(), wkEntry);
      }
      this.workspaces = new ArrayList<WorkspaceEntry>(mWorkspaceEntries.values());
   }
}
