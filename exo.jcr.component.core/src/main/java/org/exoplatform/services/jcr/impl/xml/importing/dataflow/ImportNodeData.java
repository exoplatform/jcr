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
package org.exoplatform.services.jcr.impl.xml.importing.dataflow;

import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: ImportNodeData.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class ImportNodeData extends TransientNodeData implements ImportItemData
{
   /**
    * 
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.core.ImportNodeData");

   /**
    * 
    */
   private String baseVersionIdentifier;

   /**
    * 
    */
   private boolean isContainsVersionhistory;

   /**
    * 
    */
   private boolean isMixReferenceable;

   /**
    * 
    */
   private boolean isMixVersionable;

   private boolean newIdentifer;

   /**
    * 
    */
   private final List<NodeTypeData> nodeTypes;

   private String predecessorsIdentifier;

   /**
    * 
    */
   private String versionHistoryIdentifier;

   /**
      * 
      */
   private String exoOwner;

   /**
    * 
    */
   private List<String> exoPrivileges;

   /**
    * @param parent
    * @param name
    * @param index
    */
   public ImportNodeData(ItemData parent, InternalQName name, int index)
   {
      super();
      this.qpath = QPath.makeChildPath(parent.getQPath(), name, index);
      this.parentIdentifier = parent.getIdentifier();
      this.nodeTypes = new ArrayList<NodeTypeData>();
   }

   /**
    * @param path
    * @param identifier
    * @param version
    * @param primaryTypeName
    * @param mixinTypeNames
    * @param orderNum
    * @param parentIdentifier
    * @param acl
    */
   public ImportNodeData(QPath path, String identifier, int version, InternalQName primaryTypeName,
      InternalQName[] mixinTypeNames, int orderNum, String parentIdentifier, AccessControlList acl)
   {
      super(path, identifier, version, primaryTypeName, mixinTypeNames, orderNum, parentIdentifier, acl);
      this.nodeTypes = new ArrayList<NodeTypeData>();
   }

   /**
    * @return the currentNodeTypes
    */
   public void addNodeType(NodeTypeData nt)
   {
      nodeTypes.add(nt);
   }

   /**
    * @return the baseVersionIdentifier
    */
   public String getBaseVersionIdentifier()
   {
      return baseVersionIdentifier;
   }

   //
   // /**
   // * @return the currentNodeTypes
   // */
   // public List<ExtendedNodeType> getCurrentNodeTypes() {
   // return nodeTypes;
   // }
   /**
     * @return the exoOwner
     */
   public String getExoOwner()
   {
      return exoOwner;
   }

   /**
    * @return the exoPrivileges
    */
   public List<String> getExoPrivileges()
   {
      return exoPrivileges;
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.impl.dataflow.TransientNodeData#getMixinTypeNames
    * ()
    */
   @Override
   public InternalQName[] getMixinTypeNames()
   {
      if (mixinTypeNames == null)
         return new InternalQName[0];
      return mixinTypeNames;

   }

   /**
    * @return the nodeTypes
    */
   public List<NodeTypeData> getNodeTypes()
   {
      return nodeTypes;
   }

   public String getPredecessorsIdentifier()
   {
      return predecessorsIdentifier;
   }

   /**
    * @return the versionHistoryIdentifier
    */
   public String getVersionHistoryIdentifier()
   {
      return versionHistoryIdentifier;
   }

   /**
    * @return the isContainsVersionhistory
    */
   public boolean isContainsVersionhistory()
   {
      return isContainsVersionhistory;
   }

   /**
    * @return the isMixReferenceable
    */
   public boolean isMixReferenceable()
   {
      return isMixReferenceable;
   }

   /**
    * @return the isMixVersionable
    */
   public boolean isMixVersionable()
   {
      return isMixVersionable;
   }

   public boolean isNewIdentifer()
   {
      return newIdentifer;
   }

   /**
    * @param baseVersionIdentifier the baseVersionIdentifier to set
    */
   public void setBaseVersionIdentifier(String baseVersionIdentifier)
   {
      this.baseVersionIdentifier = baseVersionIdentifier;
   }

   /**
    * @param isContainsVersionhistory the isContainsVersionhistory to set
    */
   public void setContainsVersionhistory(boolean isContainsVersionhistory)
   {
      this.isContainsVersionhistory = isContainsVersionhistory;
   }

   /**
    * @param exoOwner the exoOwner to set
    */
   public void setExoOwner(String exoOwner)
   {
      this.exoOwner = exoOwner;
   }

   /**
    * @return
    */
   public boolean isExoOwneable()
   {
      return exoOwner != null;
   }

   /**
    * @param exoPrivileges the exoPrivileges to set
    */
   public void setExoPrivileges(List<String> exoPrivileges)
   {
      this.exoPrivileges = exoPrivileges;
   }

   public boolean isExoPrivilegeable()
   {
      return exoPrivileges != null;
   }

   public void setOrderNumber(int orderNum)
   {
      this.orderNum = orderNum;
   }

   public void setMixinTypeNames(InternalQName[] mixinTypeNames)
   {
      this.mixinTypeNames = mixinTypeNames;
   }

   public void setIdentifier(String identifier)
   {
      this.identifier = identifier;
   }

   public void setACL(AccessControlList acl)
   {
      this.acl = acl;
   }

   public void setIsNewIdentifer(boolean newIdentifer)
   {
      this.newIdentifer = newIdentifer;
   }

   /**
    * @param isMixReferenceable the isMixReferenceable to set
    */
   public void setMixReferenceable(boolean isMixReferenceable)
   {
      this.isMixReferenceable = isMixReferenceable;
   }

   /**
    * @param isMixVersionable the isMixVersionable to set
    */
   public void setMixVersionable(boolean isMixVersionable)
   {
      this.isMixVersionable = isMixVersionable;
   }

   /**
    * Set parent identifier
    * 
    * @param identifer
    */
   public void setParentIdentifer(String identifer)
   {
      this.parentIdentifier = identifer;
   }

   public void setPredecessorsIdentifier(String predecessorsIdentifier)
   {
      this.predecessorsIdentifier = predecessorsIdentifier;
   }

   public void setPrimaryTypeName(InternalQName name)
   {
      primaryTypeName = name;
   }

   /**
    * Set path of item
    * 
    * @param path
    */
   public void setQPath(QPath path)
   {
      this.qpath = path;
   }

   /**
    * @param versionHistoryIdentifier the versionHistoryIdentifier to set
    */
   public void setVersionHistoryIdentifier(String versionHistoryIdentifier)
   {
      this.versionHistoryIdentifier = versionHistoryIdentifier;
   }

   public static ImportNodeData createCopy(NodeData source)
   {
      return new ImportNodeData(source.getQPath(), source.getIdentifier(), source.getPersistedVersion(), source
         .getPrimaryTypeName(), source.getMixinTypeNames(), source.getOrderNumber(), source.getParentIdentifier(),
         source.getACL());

   }
}
