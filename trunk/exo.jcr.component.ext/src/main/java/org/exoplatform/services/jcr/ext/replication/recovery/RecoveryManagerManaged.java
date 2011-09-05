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
package org.exoplatform.services.jcr.ext.replication.recovery;

import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.View;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
@Managed
@ManagedDescription("JCR cluster recovery manager")
@NameTemplate({@Property(key = "service", value = "replication"),
   @Property(key = "workspace", value = "{WorkspaceName}")})
public class RecoveryManagerManaged
{

   /** . */
   private RecoveryManager manager;

   public RecoveryManagerManaged(RecoveryManager manager)
   {
      this.manager = manager;
   }

   @Managed
   @ManagedDescription("The workspace name")
   public String getWorkspaceName()
   {
      return manager.getWorkspaceName();
   }

   @Managed
   @ManagedDescription("The workspace name")
   public String getRepositoryName()
   {
      return manager.getRepositoryName();
   }

   @Managed
   @ManagedDescription("The active cluster participants")
   public String[] getActiveParticipants()
   {
      List<String> initedParticipantsClusterList = manager.getInitedParticipantsClusterList();
      return initedParticipantsClusterList.toArray(new String[initedParticipantsClusterList.size()]);
   }

   @Managed
   @ManagedDescription("The cluster participants")
   public String[] getParticipants()
   {
      List<String> initedParticipantsClusterList = manager.getParticipantsClusterList();
      return initedParticipantsClusterList.toArray(new String[initedParticipantsClusterList.size()]);
   }

   @Managed
   @ManagedDescription("Initialization status")
   public boolean isInitializationComplete()
   {
      return manager.isAllInited();
   }

   @Managed
   @ManagedDescription("The node name in the cluster")
   public String getNodeName()
   {
      return manager.getOwnName();
   }

   @Managed
   @ManagedDescription("The cluster name")
   public String getClusterName()
   {
      JChannel juliaChannel = manager.getChannelManager().getChannel();
      return juliaChannel.getClusterName();
   }

   @Managed
   @ManagedDescription("The cluster members")
   public String[] getClusterMembers()
   {
      JChannel juliaChannel = manager.getChannelManager().getChannel();
      View view = juliaChannel.getView();
      List<String> members = new ArrayList<String>();
      for (Address member : view.getMembers())
      {
         members.add(member.toString());
      }
      return members.toArray(new String[members.size()]);
   }
}
