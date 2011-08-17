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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.access.SystemIdentity;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.ItemDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.datamodel.Identifier;
import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.itemfilters.ItemDataFilter;
import org.exoplatform.services.jcr.impl.core.itemfilters.NodeNamePatternFilter;
import org.exoplatform.services.jcr.impl.core.itemfilters.PropertyNamePatternFilter;
import org.exoplatform.services.jcr.impl.core.lock.LockImpl;
import org.exoplatform.services.jcr.impl.core.nodetype.ItemAutocreator;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeDefinitionImpl;
import org.exoplatform.services.jcr.impl.core.version.ItemDataMergeVisitor;
import org.exoplatform.services.jcr.impl.core.version.VersionHistoryImpl;
import org.exoplatform.services.jcr.impl.core.version.VersionImpl;
import org.exoplatform.services.jcr.impl.dataflow.ItemDataRemoveVisitor;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataConvertor;
import org.exoplatform.services.jcr.impl.dataflow.session.SessionChangesLog;
import org.exoplatform.services.jcr.impl.dataflow.session.TransactionableDataManager;
import org.exoplatform.services.jcr.impl.util.EntityCollection;
import org.exoplatform.services.jcr.observation.ExtendedEvent;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RangeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov </a>
 * @version $Id: NodeImpl.java 14520 2008-05-20 13:42:15Z pnedonosko $
 */
public class NodeImpl extends ItemImpl implements ExtendedNode
{

   /**
    * Logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.NodeImpl");

   /**
    * System LocationFactory.
    */
   protected final LocationFactory sysLocFactory;

   /**
    * This Node NodeDefinition data.
    */
   protected NodeDefinitionData definition;

   /**
    * This Node NodeDefinition object.
    */
   protected NodeDefinition nodeDefinition;

   /**
    * NodeImpl constructor.
    * 
    * @param data
    *          Node data
    * @param session    
    *          Session
    * @throws RepositoryException
    *           if error occurs during the Node data loading
    */
   public NodeImpl(NodeData data, SessionImpl session) throws RepositoryException
   {
      super(data, session);
      this.sysLocFactory = session.getSystemLocationFactory();
      loadData(data);
   }

   /**
    * NodeImpl constructor.
    * 
    * @param data
    *          Node data
    * @param parent NodeData Nodes's parent 
    * @param session   
    *          Session
    * @throws RepositoryException
    *           if error occurs during the Node data loading
    */
   public NodeImpl(NodeData data, NodeData parent, SessionImpl session) throws RepositoryException
   {
      super(data, session);
      this.sysLocFactory = session.getSystemLocationFactory();
      loadData(data, parent);
   }

   /**
    * {@inheritDoc}
    */
   public void accept(ItemVisitor visitor) throws RepositoryException
   {
      checkValid();

      visitor.visit(this);
   }

   /**
    * {@inheritDoc}
    */
   public void addMixin(String mixinName) throws NoSuchNodeTypeException, ConstraintViolationException,
      VersionException, LockException, RepositoryException
   {

      checkValid();

      if (LOG.isDebugEnabled())
      {
         LOG.debug("Node.addMixin " + mixinName + " " + getPath());
      }

      InternalQName name = locationFactory.parseJCRName(mixinName).getInternalName();

      // Does the node already has the mixin
      for (InternalQName mixin : nodeData().getMixinTypeNames())
      {
         if (name.equals(mixin))
         {
            // we already have this mixin type
            LOG.warn("Node already of mixin type " + mixinName + " " + getPath());
            return;
         }
      }

      NodeTypeData type = session.getWorkspace().getNodeTypesHolder().getNodeType(name);

      // Mixin or not
      if (type == null || !type.isMixin())
      {
         throw new NoSuchNodeTypeException("Nodetype " + mixinName + " not found or not mixin type.");
      }

      // Validate
      if (session.getWorkspace().getNodeTypesHolder().isNodeType(type.getName(), nodeData().getPrimaryTypeName(),
         nodeData().getMixinTypeNames()))
      {
         throw new ConstraintViolationException("Can not add mixin type " + mixinName + " to " + getPath());
      }

      if (definition.isProtected())
      {
         throw new ConstraintViolationException("Can not add mixin type. Node is protected " + getPath());
      }

      // Check if versionable ancestor is not checked-in
      if (!checkedOut())
      {
         throw new VersionException("Node " + getPath() + " or its nearest ancestor is checked-in");
      }

      // Check locking
      if (!checkLocking())
      {
         throw new LockException("Node " + getPath() + " is locked ");
      }

      doAddMixin(type);
   }

   /**
    * {@inheritDoc}
    */
   public Node addNode(String relPath) throws PathNotFoundException, ConstraintViolationException, RepositoryException,
      VersionException, LockException
   {

      checkValid();
      if (JCRPath.THIS_RELPATH.equals(relPath))
      {
         throw new RepositoryException("Can't add node to the path '" + relPath + "'");
      }

      // Parent can be not the same as this node
      JCRPath itemPath = locationFactory.parseRelPath(relPath);

      // Check if there no final index
      if (itemPath.isIndexSetExplicitly())
      {
         throw new RepositoryException("The relPath provided must not have an index on its final element. "
            + itemPath.getAsString(false));
      }

      ItemImpl parentItem =
         dataManager.getItem(nodeData(), itemPath.makeParentPath().getInternalPath().getEntries(), false,
            ItemType.UNKNOWN);

      if (parentItem == null)
      {
         throw new PathNotFoundException("Parent not found for " + itemPath.getAsString(true));
      }
      if (!parentItem.isNode())
      {
         throw new ConstraintViolationException("Parent item is not a node " + parentItem.getPath());
      }

      NodeImpl parent = (NodeImpl)parentItem;
      InternalQName name = itemPath.getName().getInternalName();

      // find node type
      NodeDefinitionData nodeDef =
         session.getWorkspace().getNodeTypesHolder().getChildNodeDefinition(name, nodeData().getPrimaryTypeName(),
            nodeData().getMixinTypeNames());

      if (nodeDef == null)
      {
         throw new ConstraintViolationException("Can not define node type for " + name.getAsString());
      }
      InternalQName primaryTypeName = nodeDef.getName();

      if (nodeDef.getName().equals(name) || primaryTypeName.equals(Constants.JCR_ANY_NAME))
      {
         primaryTypeName = nodeDef.getDefaultPrimaryType();

         if (primaryTypeName == null)
         {
            throw new ConstraintViolationException("Can not define node type for " + name.getAsString()
               + ". No default primary type defined for child nodes in \""
               + nodeData().getPrimaryTypeName().getAsString()
               + "\" node type and no explicit primary type given to create a child node.");
         }
      }
      // try to make new node
      return doAddNode(parent, name, primaryTypeName, nodeDef);

   }

   /**
    * {@inheritDoc}
    */
   public Node addNode(String relPath, String nodeTypeName) throws ItemExistsException, PathNotFoundException,
      NoSuchNodeTypeException, ConstraintViolationException, RepositoryException, VersionException, LockException
   {

      checkValid();

      if (JCRPath.THIS_RELPATH.equals(relPath))
      {
         throw new RepositoryException("Can't add node to the path '" + relPath + "'");
      }

      // Parent can be not the same as this node
      JCRPath itemPath = locationFactory.parseRelPath(relPath);

      // Check if there no final index
      if (itemPath.isIndexSetExplicitly())
      {
         throw new RepositoryException("The relPath provided must not have an index on its final element. "
            + itemPath.getAsString(false));
      }

      ItemImpl parentItem =
         dataManager.getItem(nodeData(), itemPath.makeParentPath().getInternalPath().getEntries(), false,
            ItemType.UNKNOWN);

      if (parentItem == null)
      {
         throw new PathNotFoundException("Parent not found for " + itemPath.getAsString(true));
      }
      if (!parentItem.isNode())
      {
         throw new ConstraintViolationException("Parent item is not a node " + parentItem.getPath());
      }
      NodeImpl parent = (NodeImpl)parentItem;

      InternalQName name = itemPath.getName().getInternalName();
      InternalQName ptName = locationFactory.parseJCRName(nodeTypeName).getInternalName();

      // try to make new node
      return doAddNode(parent, name, ptName, null);
   }

   /**
    * {@inheritDoc}
    */
   public boolean canAddMixin(String mixinName) throws RepositoryException
   {

      checkValid();

      NodeTypeData type =
         session.getWorkspace().getNodeTypesHolder().getNodeType(
            locationFactory.parseJCRName(mixinName).getInternalName());

      if (type == null)
      {
         throw new NoSuchNodeTypeException("Nodetype not found (mixin) " + mixinName);
      }

      if (session.getWorkspace().getNodeTypesHolder().isNodeType(type.getName(), nodeData().getPrimaryTypeName(),
         nodeData().getMixinTypeNames()))
      {
         return false;
      }

      if (definition.isProtected())
      {
         return false;
      }

      if (!checkedOut())
      {
         return false;
      }

      if (!checkLocking())
      {
         return false;
      }

      return true;
   }

   /**
    * {@inheritDoc}
    */
   public void cancelMerge(Version version) throws VersionException, InvalidItemStateException,
      UnsupportedRepositoryOperationException, RepositoryException
   {
      if (!session.getAccessManager().hasPermission(getACL(),
         new String[]{PermissionType.ADD_NODE, PermissionType.SET_PROPERTY}, session.getUserState().getIdentity()))
      {
         throw new AccessDeniedException("Access denied: cancel merge operation " + getPath() + " for: "
            + session.getUserID() + " item owner " + getACL().getOwner());
      }

      checkValid();

      PlainChangesLog changesLog = new PlainChangesLogImpl(session.getId());

      removeMergeFailed(version, changesLog);

      dataManager.getTransactManager().save(changesLog);
   }

   /**
    * Tell if this node or its nearest versionable ancestor is checked-out.
    * 
    * @return boolean
    * @throws UnsupportedRepositoryOperationException
    *           if Versionable operations is not supported
    * @throws RepositoryException
    *           if error occurs
    */
   public boolean checkedOut() throws UnsupportedRepositoryOperationException, RepositoryException
   {
      // this will also check if item is valid
      NodeData vancestor = getVersionableAncestor();
      if (vancestor != null)
      {
         PropertyData isCheckedOut =
            (PropertyData)dataManager.getItemData(vancestor, new QPathEntry(Constants.JCR_ISCHECKEDOUT, 1),
               ItemType.PROPERTY);
         try
         {
            return ValueDataConvertor.readBoolean(isCheckedOut.getValues().get(0));
         }
         catch (IOException e)
         {
            throw new RepositoryException("Can't read property "
               + locationFactory.createJCRPath(vancestor.getQPath()).getAsString(false) + "/jcr:isCheckedOut. " + e, e);
         }
      }

      return true;
   }

   /**
    * {@inheritDoc}
    */
   public Version checkin() throws VersionException, UnsupportedRepositoryOperationException,
      InvalidItemStateException, RepositoryException
   {

      checkValid();

      if (!session.getAccessManager().hasPermission(getACL(),
         new String[]{PermissionType.ADD_NODE, PermissionType.SET_PROPERTY}, session.getUserState().getIdentity()))
      {
         throw new AccessDeniedException("Access denied: checkin operation " + getPath() + " for: "
            + session.getUserID() + " item owner " + getACL().getOwner());
      }

      if (!this.isNodeType(Constants.MIX_VERSIONABLE))
      {
         throw new UnsupportedRepositoryOperationException(
            "Node.checkin() is not supported for not mix:versionable node ");
      }

      if (!this.checkedOut())
      {
         return (Version)dataManager.getItemByIdentifier(property(Constants.JCR_BASEVERSION).getString(), false);
      }

      if (session.getTransientNodesManager().hasPendingChanges(getInternalPath()))
      {
         throw new InvalidItemStateException("Node has pending changes " + getPath());
      }

      if (hasProperty(Constants.JCR_MERGEFAILED))
      {
         throw new VersionException("Node has jcr:mergeFailed " + getPath());
      }

      if (!checkLocking())
      {
         throw new LockException("Node " + getPath() + " is locked ");
      }

      // the new version identifier
      String verIdentifier = IdGenerator.generate();

      SessionChangesLog changesLog = new SessionChangesLog(session.getId());

      VersionHistoryImpl vh = versionHistory(false);
      vh.addVersion(this.nodeData(), verIdentifier, changesLog);

      changesLog.add(ItemState.createUpdatedState(updatePropertyData(Constants.JCR_ISCHECKEDOUT,
         new TransientValueData(false))));

      changesLog.add(ItemState.createUpdatedState(updatePropertyData(Constants.JCR_BASEVERSION, new TransientValueData(
         new Identifier(verIdentifier)))));

      changesLog.add(ItemState.createUpdatedState(updatePropertyData(Constants.JCR_PREDECESSORS,
         new ArrayList<ValueData>())));

      dataManager.getTransactManager().save(changesLog);

      VersionImpl version = (VersionImpl)dataManager.getItemByIdentifier(verIdentifier, true, false);

      session.getActionHandler().postCheckin(this);
      return version;
   }

   /**
    * {@inheritDoc}
    */
   public void checkout() throws RepositoryException, UnsupportedRepositoryOperationException
   {

      checkValid();

      if (!session.getAccessManager().hasPermission(getACL(), new String[]{PermissionType.SET_PROPERTY},
         session.getUserState().getIdentity()))
      {
         throw new AccessDeniedException("Access denied: checkout operation " + getPath() + " for: "
            + session.getUserID() + " item owner " + getACL().getOwner());
      }

      if (!this.isNodeType(Constants.MIX_VERSIONABLE))
      {
         throw new UnsupportedRepositoryOperationException(
            "Node.checkout() is not supported for not mix:versionable node ");
      }

      if (!checkLocking())
      {
         throw new LockException("Node " + getPath() + " is locked ");
      }

      if (checkedOut())
      {
         return;
      }

      SessionChangesLog changesLog = new SessionChangesLog(session.getId());

      changesLog.add(ItemState.createUpdatedState(updatePropertyData(Constants.JCR_ISCHECKEDOUT,
         new TransientValueData(true))));

      ValueData baseVersion =
         ((PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_BASEVERSION, 0),
            ItemType.PROPERTY)).getValues().get(0);

      changesLog.add(ItemState.createUpdatedState(updatePropertyData(Constants.JCR_PREDECESSORS, baseVersion)));

      dataManager.getTransactManager().save(changesLog);

      session.getActionHandler().postCheckout(this);
   }

   /**
    * {@inheritDoc}
    */
   public void checkPermission(String actions) throws AccessControlException, RepositoryException
   {

      checkValid();

      if (!session.getAccessManager().hasPermission(getACL(), actions, session.getUserState().getIdentity()))
      {
         throw new AccessControlException("Permission denied " + getPath() + " : " + actions);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void clearACL() throws RepositoryException, AccessControlException
   {
      if (!isNodeType(Constants.EXO_PRIVILEGEABLE))
      {
         throw new AccessControlException("Node is not exo:privilegeable " + getPath());
      }
      // this will also check if item is valid
      checkPermission(PermissionType.CHANGE_PERMISSION);

      List<AccessControlEntry> aces = new ArrayList<AccessControlEntry>();
      for (String perm : PermissionType.ALL)
      {
         AccessControlEntry ace = new AccessControlEntry(SystemIdentity.ANY, perm);
         aces.add(ace);
      }
      AccessControlList acl = new AccessControlList(getACL().getOwner(), aces);

      setACL(acl);
   }

   /**
    * Internal method to add mixin Nodetype to the Node.
    * 
    * @param type
    *          NodeTypeData
    * @throws NoSuchNodeTypeException
    *           if requested Nodetype is not found
    * @throws ConstraintViolationException
    *           if add will brkoes any constrainst
    * @throws VersionException
    *           if this Node (or its ancestor) is checked-in.
    * @throws LockException
    *           if this Node (or its ancestor) is locked
    * @throws RepositoryException
    *           if any other error occurs
    */
   private void doAddMixin(NodeTypeData type) throws NoSuchNodeTypeException, ConstraintViolationException,
      VersionException, LockException, RepositoryException
   {

      // Add both to mixinNodeTypes and to jcr:mixinTypes property

      // Prepare mixin values
      InternalQName[] mixinTypes = nodeData().getMixinTypeNames();
      List<InternalQName> newMixin = new ArrayList<InternalQName>(mixinTypes.length + 1);
      List<ValueData> values = new ArrayList<ValueData>(mixinTypes.length + 1);

      for (int i = 0; i < mixinTypes.length; i++)
      {
         InternalQName cn = mixinTypes[i];
         newMixin.add(cn);
         values.add(new TransientValueData(cn));
      }
      newMixin.add(type.getName());
      values.add(new TransientValueData(type.getName()));

      PropertyData prop =
         (PropertyData)dataManager.getItemData(((NodeData)getData()), new QPathEntry(Constants.JCR_MIXINTYPES, 0),
            ItemType.PROPERTY);
      ItemState state;

      if (prop != null)
      {// there was mixin prop
         prop =
            new TransientPropertyData(prop.getQPath(), prop.getIdentifier(), prop.getPersistedVersion(),
               prop.getType(), prop.getParentIdentifier(), prop.isMultiValued(), values);

         state = ItemState.createUpdatedState(prop);
      }
      else
      {
         prop =
            TransientPropertyData.createPropertyData(this.nodeData(), Constants.JCR_MIXINTYPES, PropertyType.NAME,
               true, values);
         state = ItemState.createAddedState(prop);
      }

      NodeTypeDataManager ntmanager = session.getWorkspace().getNodeTypesHolder();

      // change ACL
      for (PropertyDefinitionData def : ntmanager.getAllPropertyDefinitions(type.getName()))
      {
         if (ntmanager.isNodeType(Constants.EXO_OWNEABLE, new InternalQName[]{type.getName()})
            && def.getName().equals(Constants.EXO_OWNER))
         {
            AccessControlList acl =
               new AccessControlList(session.getUserID(), ((NodeData)data).getACL().getPermissionEntries());
            setACL(acl);
         }
      }

      updateMixin(newMixin);
      dataManager.update(state, false);

      // Should register jcr:mixinTypes and autocreated items if node is not added
      ItemAutocreator itemAutocreator = new ItemAutocreator(ntmanager, valueFactory, dataManager, false);

      PlainChangesLog changes =
         itemAutocreator.makeAutoCreatedItems(nodeData(), type.getName(), dataManager, session.getUserID());

      for (ItemState autoCreatedState : changes.getAllStates())
      {
         dataManager.update(autoCreatedState, false);
      }

      // launch event
      session.getActionHandler().postAddMixin(this, type.getName());

      if (LOG.isDebugEnabled())
      {
         LOG.debug("Node.addMixin Property " + prop.getQPath().getAsString() + " values " + mixinTypes.length);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void doneMerge(Version version) throws VersionException, InvalidItemStateException,
      UnsupportedRepositoryOperationException, RepositoryException
   {

      if (!session.getAccessManager().hasPermission(getACL(),
         new String[]{PermissionType.ADD_NODE, PermissionType.SET_PROPERTY}, session.getUserState().getIdentity()))
      {
         throw new AccessDeniedException("Access denied: done merge operation " + getPath() + " for: "
            + session.getUserID() + " item owner " + getACL().getOwner());
      }

      PlainChangesLog changesLog = new PlainChangesLogImpl(session.getId());

      VersionImpl base = (VersionImpl)getBaseVersion();
      base.addPredecessor(version.getUUID(), changesLog);
      removeMergeFailed(version, changesLog);

      dataManager.getTransactManager().save(changesLog);
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj instanceof NodeImpl)
      {
         NodeImpl otherNode = (NodeImpl)obj;

         if (!otherNode.isValid() || !this.isValid())
         {
            return false;
         }

         try
         {
            if (otherNode.isNodeType("mix:referenceable") && this.isNodeType("mix:referenceable"))
            {
               // by UUID
               // getProperty("jcr:uuid") is more correct, but may decrease
               // performance
               return getInternalIdentifier().equals(otherNode.getInternalIdentifier());
            }
            // by path
            return getLocation().equals(otherNode.getLocation());
         }
         catch (RepositoryException e)
         {
            return false;
         }
      }
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public AccessControlList getACL() throws RepositoryException
   {

      checkValid();

      List<AccessControlEntry> listEntry = new ArrayList<AccessControlEntry>();

      for (AccessControlEntry aEntry : nodeData().getACL().getPermissionEntries())
      {
         listEntry.add(new AccessControlEntry(aEntry.getIdentity(), aEntry.getPermission()));
      }

      return new AccessControlList(nodeData().getACL().getOwner(), listEntry);
   }

   /**
    * {@inheritDoc}
    */
   public Version getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException
   {

      checkValid();

      if (!this.isNodeType(Constants.MIX_VERSIONABLE))
      {
         throw new UnsupportedRepositoryOperationException("Node is not versionable " + getPath());
      }

      PropertyData bvProp =
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_BASEVERSION, 1),
            ItemType.PROPERTY);
      try
      {
         return (Version)dataManager.getItemByIdentifier(ValueDataConvertor.readString(bvProp.getValues().get(0)),
            true, false);
      }
      catch (IOException e)
      {
         throw new RepositoryException("jcr:baseVersion property error " + e, e);
      }
   }

   /**
    * Return Node corresponding to this Node.
    * 
    * @param correspSession
    *          session on corresponding Workspace
    * @return NodeData corresponding Node
    * @throws ItemNotFoundException
    *           if corresponding Node not found
    * @throws AccessDeniedException
    *           if read impossible due to permisions
    * @throws RepositoryException
    *           if any other error occurs
    */
   protected NodeData getCorrespondingNodeData(SessionImpl corrSession) throws ItemNotFoundException,
      AccessDeniedException, RepositoryException
   {

      final QPath myPath = nodeData().getQPath();
      final SessionDataManager corrDataManager = corrSession.getTransientNodesManager();

      if (this.isNodeType(Constants.MIX_REFERENCEABLE))
      {
         NodeData corrNode = (NodeData)corrDataManager.getItemData(getUUID());
         if (corrNode != null)
         {
            return corrNode;
         }
      }
      else
      {
         NodeData ancestor = (NodeData)dataManager.getItemData(Constants.ROOT_UUID);
         for (int i = 1; i < myPath.getDepth(); i++)
         {
            ancestor = (NodeData)dataManager.getItemData(ancestor, myPath.getEntries()[i], ItemType.NODE);
            if (corrSession.getWorkspace().getNodeTypesHolder().isNodeType(Constants.MIX_REFERENCEABLE,
               ancestor.getPrimaryTypeName(), ancestor.getMixinTypeNames()))
            {
               NodeData corrAncestor = (NodeData)corrDataManager.getItemData(ancestor.getIdentifier());
               if (corrAncestor == null)
               {
                  throw new ItemNotFoundException("No corresponding path for ancestor "
                     + ancestor.getQPath().getAsString() + " in " + corrSession.getWorkspace().getName());
               }

               NodeData corrNode =
                  (NodeData)corrDataManager.getItemData(corrAncestor, myPath.getRelPath(myPath.getDepth() - i),
                     ItemType.NODE);
               if (corrNode != null)
               {
                  return corrNode;
               }
            }
         }
      }
      NodeData corrNode = (NodeData)corrDataManager.getItemData(myPath);
      if (corrNode != null)
      {
         return corrNode;
      }

      throw new ItemNotFoundException("No corresponding path for " + getPath() + " in "
         + corrSession.getWorkspace().getName());
   }

   /**
    * {@inheritDoc}
    */
   public String getCorrespondingNodePath(String workspaceName) throws ItemNotFoundException, NoSuchWorkspaceException,
      AccessDeniedException, RepositoryException
   {

      checkValid();

      SessionImpl corrSession =
         ((RepositoryImpl)session.getRepository()).internalLogin(session.getUserState(), workspaceName);
      return corrSession.getLocationFactory().createJCRPath(getCorrespondingNodeData(corrSession).getQPath())
         .getAsString(false);
   }

   /**
    * {@inheritDoc}
    */
   public NodeDefinition getDefinition() throws RepositoryException
   {

      checkValid();

      if (nodeDefinition == null)
      {

         NodeTypeDataManager nodeTypesHolder = session.getWorkspace().getNodeTypesHolder();
         ExtendedNodeTypeManager nodeTypeManager = (ExtendedNodeTypeManager)session.getWorkspace().getNodeTypeManager();

         if (this.isRoot())
         { // root - no parent
            if (nodeDefinition == null)
            {
               NodeType required =
                  nodeTypeManager.getNodeType(locationFactory.createJCRName(Constants.NT_BASE).getAsString());
               InternalQName requiredName = sysLocFactory.parseJCRName(required.getName()).getInternalName();
               NodeDefinitionData ntData =
                  new NodeDefinitionData(null, null, true, true, OnParentVersionAction.ABORT, false,
                     new InternalQName[]{requiredName}, null, true);
               this.nodeDefinition =
                  new NodeDefinitionImpl(ntData, nodeTypesHolder, nodeTypeManager, sysLocFactory, session
                     .getValueFactory(), session.getTransientNodesManager());
            }
         }
         else
         {

            NodeData parent = (NodeData)dataManager.getItemData(getParentIdentifier());

            this.definition =
               nodeTypesHolder.getChildNodeDefinition(getInternalName(), nodeData().getPrimaryTypeName(), parent
                  .getPrimaryTypeName(), parent.getMixinTypeNames());

            if (definition == null)
            {
               throw new ConstraintViolationException("Node definition not found for " + getPath());
            }

            // TODO same functionality in NodeTypeImpl
            InternalQName[] rnames = definition.getRequiredPrimaryTypes();
            NodeType[] rnts = new NodeType[rnames.length];
            for (int j = 0; j < rnames.length; j++)
            {
               rnts[j] = nodeTypeManager.findNodeType(rnames[j]);
            }

            nodeDefinition =
               new NodeDefinitionImpl(definition, nodeTypesHolder, nodeTypeManager, sysLocFactory, session
                  .getValueFactory(), session.getTransientNodesManager());
         }
      }

      return nodeDefinition;
   }

   /**
    * {@inheritDoc}
    */
   public int getIndex() throws RepositoryException
   {

      checkValid();

      return getInternalPath().getIndex();
   }

   /**
    * {@inheritDoc}
    */
   public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException,
      RepositoryException
   {

      checkValid();

      LockImpl lock = session.getLockManager().getLock(this);
      if (lock == null)
      {
         throw new LockException("Lock not found " + getPath());
      }

      return lock;

   }

   /**
    * {@inheritDoc}
    */
   public NodeType[] getMixinNodeTypes() throws RepositoryException
   {

      checkValid();

      // should not be null
      if (nodeData().getMixinTypeNames() == null)
      {
         throw new RepositoryException("Data Container implementation error getMixinTypeNames == null");
      }

      ExtendedNodeTypeManager nodeTypeManager = (ExtendedNodeTypeManager)session.getWorkspace().getNodeTypeManager();
      NodeType[] mixinNodeTypes = new NodeType[nodeData().getMixinTypeNames().length];
      for (int i = 0; i < mixinNodeTypes.length; i++)
      {
         mixinNodeTypes[i] = nodeTypeManager.findNodeType(nodeData().getMixinTypeNames()[i]);
      }

      return mixinNodeTypes;
   }

   /**
    * Return mixin Nodetype names.
    * 
    * @return String[]
    * @throws RepositoryException
    *           if error occurs
    */
   public String[] getMixinTypeNames() throws RepositoryException
   {
      NodeType[] mixinTypes = getMixinNodeTypes();
      String[] mtNames = new String[mixinTypes.length];
      for (int i = 0; i < mtNames.length; i++)
      {
         mtNames[i] = mixinTypes[i].getName();
      }

      return mtNames;
   }

   /**
    * {@inheritDoc}
    */
   public Node getNode(String relPath) throws PathNotFoundException, RepositoryException
   {

      checkValid();

      JCRPath itemPath = locationFactory.parseRelPath(relPath);

      ItemImpl node = dataManager.getItem(nodeData(), itemPath.getInternalPath().getEntries(), true, ItemType.NODE);
      if (node == null || !node.isNode())
      {
         throw new PathNotFoundException("Node not found " + (isRoot() ? "" : getLocation().getAsString(false)) + "/"
            + itemPath.getAsString(false));
      }

      return (NodeImpl)node;
   }

   /**
    * {@inheritDoc}
    */
   public String getIdentifier() throws RepositoryException
   {
      checkValid();

      return this.getInternalIdentifier();
   }

   /**
    * {@inheritDoc}
    */
   public NodeIterator getNodes() throws RepositoryException
   {

      if (SessionImpl.FORCE_USE_GET_NODES_LAZILY)
      {
         if (LOG.isDebugEnabled())
         {
            LOG
               .debug("EXPERIMENTAL! Be aware that \"getNodesLazily()\" feature is used instead of JCR API getNodes()"
                  + " invocation.To disable this simply cleanup system property \"org.exoplatform.jcr.forceUserGetNodesLazily\""
                  + " and restart JCR.");
         }
         return getNodesLazily();
      }

      long start = 0;
      if (LOG.isDebugEnabled())
      {
         start = System.currentTimeMillis();
         LOG.debug("getNodes() >>>>>");
      }

      checkValid();
      try
      {
         List<NodeData> childs = childNodesData();

         if (childs.size() < session.getLazyReadThreshold())
         {
            // full iterator 
            List<NodeImpl> nodes = new ArrayList<NodeImpl>();
            for (int i = 0, length = childs.size(); i < length; i++)
            {
               NodeData child = childs.get(i);
               if (session.getAccessManager().hasPermission(child.getACL(), new String[]{PermissionType.READ},
                  session.getUserState().getIdentity()))
               {
                  NodeImpl item = (NodeImpl)dataManager.readItem(child, nodeData(), true, false);
                  session.getActionHandler().postRead(item);
                  nodes.add(item);
               }
            }
            return new EntityCollection(nodes);
         }
         else
         {
            // lazy iterator
            return new LazyNodeIterator(childs);
         }
      }
      finally
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug("getNodes() <<<<< " + ((System.currentTimeMillis() - start) / 1000d) + "sec");
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public NodeIterator getNodesLazily() throws RepositoryException
   {
      checkValid();
      return new LazyNodeIteratorByPage(dataManager);
   }

   /**
    * {@inheritDoc}
    */
   public NodeIterator getNodes(String namePattern) throws RepositoryException
   {

      long start = 0;
      if (LOG.isDebugEnabled())
      {
         start = System.currentTimeMillis();
         LOG.debug("getNodes(String) >>>>>");
      }

      checkValid();

      try
      {

         NodeNamePatternFilter filter = new NodeNamePatternFilter(namePattern, session);

         List<NodeData> childs = null;
         if (filter.isLookingAllData())
         {
            childs = childNodesData();
         }
         else
         {
            childs = new ArrayList<NodeData>(dataManager.getChildNodesData(nodeData(), filter.getQPathEntryFilters()));
            Collections.sort(childs, new NodeDataOrderComparator());
         }

         if (childs.size() < session.getLazyReadThreshold())
         {
            // full iterator 
            List<NodeImpl> nodes = new ArrayList<NodeImpl>();
            for (int i = 0, length = childs.size(); i < length; i++)
            {
               NodeData child = childs.get(i);
               if (filter.accept(child)
                  && session.getAccessManager().hasPermission(child.getACL(), new String[]{PermissionType.READ},
                     session.getUserState().getIdentity()))
               {
                  NodeImpl item = (NodeImpl)dataManager.readItem(child, nodeData(), true, false);
                  session.getActionHandler().postRead(item);
                  nodes.add(item);
               }
            }
            return new EntityCollection(nodes);
         }
         else
         {
            // lazy iterator
            return new LazyNodeIterator(childs, filter);
         }
      }
      finally
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug("getNodes(String) <<<<< " + ((System.currentTimeMillis() - start) / 1000d) + "sec");
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException
   {

      checkValid();

      NodeTypeDataManager nodeTypeDataManager = session.getWorkspace().getNodeTypesHolder();

      // load nodeDatas
      List<NodeTypeData> nodeTypes = new ArrayList<NodeTypeData>();
      nodeTypes.add(nodeTypeDataManager.getNodeType(nodeData().getPrimaryTypeName()));
      InternalQName[] mixinNames = nodeData().getMixinTypeNames();
      for (int i = 0; i < mixinNames.length; i++)
      {
         nodeTypes.add(nodeTypeDataManager.getNodeType(mixinNames[i]));
      }

      // Searching default
      for (NodeTypeData ntData : nodeTypes)
      {
         if (ntData.getPrimaryItemName() != null)
         {
            Item primaryItem =
               dataManager.getItem(nodeData(), new QPathEntry(ntData.getPrimaryItemName(), 0), true, ItemType.UNKNOWN);
            if (primaryItem != null)
            {
               return primaryItem;
            }

         }
      }

      throw new ItemNotFoundException("Primary item not found for " + getPath());
   }

   /**
    * {@inheritDoc}
    */
   public NodeType getPrimaryNodeType() throws RepositoryException
   {
      checkValid();

      ExtendedNodeTypeManager nodeTypeManager = (ExtendedNodeTypeManager)session.getWorkspace().getNodeTypeManager();
      return nodeTypeManager.findNodeType(nodeData().getPrimaryTypeName());
   }

   /**
    * {@inheritDoc}
    */
   public PropertyIterator getProperties() throws RepositoryException
   {

      long start = 0;
      if (LOG.isDebugEnabled())
      {
         start = System.currentTimeMillis();
         LOG.debug("getProperties() >>>>>");
      }

      checkValid();

      try
      {
         List<PropertyData> childs = childPropertiesData();

         if (session.getAccessManager().hasPermission(nodeData().getACL(), new String[]{PermissionType.READ},
            session.getUserState().getIdentity()))
         {
            if (childs.size() < session.getLazyReadThreshold())
            {
               // full iterator 
               List<PropertyImpl> props = new ArrayList<PropertyImpl>();
               for (int i = 0, length = childs.size(); i < length; i++)
               {
                  PropertyData child = childs.get(i);
                  PropertyImpl item = (PropertyImpl)dataManager.readItem(child, nodeData(), true, false);
                  session.getActionHandler().postRead(item);
                  props.add(item);

               }
               return new EntityCollection(props);
            }
            else
            {
               // lazy iterator
               return new LazyPropertyIterator(childs);
            }
         }
         else
         {
            // return empty
            return new EntityCollection();
         }
      }
      finally
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug("getProperties() <<<<< " + ((System.currentTimeMillis() - start) / 1000d) + "sec");
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public PropertyIterator getProperties(String namePattern) throws RepositoryException
   {
      long start = 0;
      if (LOG.isDebugEnabled())
      {
         start = System.currentTimeMillis();
         LOG.debug("getProperties(String) >>>>>");
      }

      checkValid();

      try
      {
         if (session.getAccessManager().hasPermission(nodeData().getACL(), new String[]{PermissionType.READ},
            session.getUserState().getIdentity()))
         {

            PropertyNamePatternFilter filter = new PropertyNamePatternFilter(namePattern, session);

            List<PropertyData> childs = null;
            if (filter.isLookingAllData())
            {
               childs = childPropertiesData();
            }
            else
            {
               childs =
                  new ArrayList<PropertyData>(dataManager.getChildPropertiesData(nodeData(), filter
                     .getQPathEntryFilters()));
               Collections.sort(childs, new PropertiesDataOrderComparator<PropertyData>());
            }

            if (childs.size() < session.getLazyReadThreshold())
            {
               // full iterator 
               List<PropertyImpl> props = new ArrayList<PropertyImpl>();
               for (int i = 0, length = childs.size(); i < length; i++)
               {
                  PropertyData child = childs.get(i);
                  if (filter.accept(child))
                  {
                     PropertyImpl item = (PropertyImpl)dataManager.readItem(child, nodeData(), true, false);
                     session.getActionHandler().postRead(item);
                     props.add(item);
                  }

               }
               return new EntityCollection(props);
            }
            else
            {
               // lazy iterator
               return new LazyPropertyIterator(childs, filter);
            }
         }
         else
         {
            // return empty
            return new EntityCollection();
         }
      }
      finally
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug("getProperties(String) <<<<< " + ((System.currentTimeMillis() - start) / 1000d) + "sec");
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public Property getProperty(String relPath) throws PathNotFoundException, RepositoryException
   {

      checkValid();

      JCRPath itemPath = locationFactory.parseRelPath(relPath);

      if (LOG.isDebugEnabled())
      {
         LOG.debug("getProperty() " + getLocation().getAsString(false) + " " + relPath);
      }

      ItemImpl prop = dataManager.getItem(nodeData(), itemPath.getInternalPath().getEntries(), true, ItemType.PROPERTY);
      if (prop == null || prop.isNode())
      {
         throw new PathNotFoundException("Property not found " + itemPath.getAsString(false));
      }

      return (Property)prop;
   }

   /**
    * {@inheritDoc}
    */
   public PropertyIterator getReferences() throws RepositoryException
   {

      checkValid();

      return new EntityCollection(dataManager.getReferences(getInternalIdentifier()));
   }

   /**
    * {@inheritDoc}
    */
   public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException
   {

      checkValid();

      if (isNodeType(Constants.MIX_REFERENCEABLE))
      {
         return this.getInternalIdentifier();
      }

      throw new UnsupportedRepositoryOperationException("Node " + getPath() + " is not referenceable");

   }

   /**
    * Get nearest versionable ancestor NodeData. If the node is mix:versionable this NodeData will be
    * returned.
    * 
    * @return NodeData of versionable ancestor or null if no versionable ancestor exists.
    * @throws RepositoryException
    *           if error
    */
   public NodeData getVersionableAncestor() throws RepositoryException
   {
      checkValid();
      NodeData node = nodeData();
      NodeTypeDataManager ntman = session.getWorkspace().getNodeTypesHolder();

      while (node.getParentIdentifier() != null)
      {
         if (ntman.isNodeType(Constants.MIX_VERSIONABLE, node.getPrimaryTypeName(), node.getMixinTypeNames()))
         {
            // mix:versionable has own jcr:isCheckedOut state
            return node;
         }
         else
         {
            // check on deeper ancestor
            NodeData ancestor = (NodeData)dataManager.getItemData(node.getParentIdentifier());
            if (ancestor == null)
            {
               throw new RepositoryException("Parent not found for "
                  + locationFactory.createJCRPath(node.getQPath()).getAsString(false) + ". Parent id "
                  + node.getParentIdentifier());
            }
            else
            {
               node = ancestor;
            }
         }
      }

      return null;
   }

   /**
    * {@inheritDoc}
    */
   public VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException
   {

      checkValid();

      return versionHistory(true);
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasNode(String relPath) throws RepositoryException
   {

      checkValid();

      JCRPath itemPath = locationFactory.parseRelPath(relPath);

      ItemData node = dataManager.getItemData(nodeData(), itemPath.getInternalPath().getEntries(), ItemType.NODE);
      return node != null && node.isNode();
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasNodes() throws RepositoryException
   {

      checkValid();

      return dataManager.getChildNodesCount(nodeData()) > 0;
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasProperties() throws RepositoryException
   {

      checkValid();

      return dataManager.listChildPropertiesData(nodeData()).size() > 0;
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasProperty(String relPath) throws RepositoryException
   {

      checkValid();

      JCRPath itemPath = locationFactory.parseRelPath(relPath);

      ItemData prop = dataManager.getItemData(nodeData(), itemPath.getInternalPath().getEntries(), ItemType.PROPERTY);
      return prop != null && !prop.isNode();
   }

   /**
    * {@inheritDoc}
    */
   public boolean holdsLock() throws RepositoryException
   {

      checkValid();

      return session.getLockManager().holdsLock((NodeData)getData());
   }

   /**
    * {@inheritDoc}
    */
   public boolean isCheckedOut() throws UnsupportedRepositoryOperationException, RepositoryException
   {

      checkValid();

      return checkedOut();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isLocked() throws RepositoryException
   {

      checkValid();
      return session.getLockManager().isLocked((NodeData)this.getData());
   }

   /**
    * {@inheritDoc}
    */
   public boolean isNode()
   {
      return true;
   }

   /**
    * Indicates whether this node is of the specified node type. Returns true if this node is of the
    * specified node type or a subtype of the specified node type. Returns false otherwise. <br/>
    * Nodetype name asked in for mof internal QName. TODO have it private.
    * 
    * @param qName
    *          InternalQName
    * @return boolean
    * @throws RepositoryException
    *           if error occurs
    */
   public boolean isNodeType(InternalQName qName) throws RepositoryException
   {
      checkValid();

      return session.getWorkspace().getNodeTypesHolder().isNodeType(qName, nodeData().getPrimaryTypeName(),
         nodeData().getMixinTypeNames());
   }

   /**
    * {@inheritDoc}
    */
   public boolean isNodeType(String nodeTypeName) throws RepositoryException
   {
      return isNodeType(locationFactory.parseJCRName(nodeTypeName).getInternalName());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void loadData(ItemData data) throws RepositoryException, InvalidItemStateException,
      ConstraintViolationException
   {
      loadData(data, (NodeData)null);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void loadData(ItemData data, NodeData parent) throws RepositoryException, InvalidItemStateException,
      ConstraintViolationException
   {

      if (data.isNode())
      {
         NodeData nodeData = (NodeData)data;

         // TODO do we need this three checks here?
         if (nodeData.getPrimaryTypeName() == null)
         {
            throw new RepositoryException("Load data: NodeData has no primaryTypeName. Null value found. "
               + (nodeData.getQPath() != null ? nodeData.getQPath().getAsString() : "[null path node]") + " "
               + nodeData);
         }

         if (nodeData.getMixinTypeNames() == null)
         {
            throw new RepositoryException("Load data: NodeData has no mixinTypeNames. Null value found. "
               + (nodeData.getQPath() != null ? nodeData.getQPath().getAsString() : "[null path node]"));
         }

         if (nodeData.getACL() == null)
         {
            throw new RepositoryException("ACL is NULL " + nodeData.getQPath().getAsString());
         }

         this.data = nodeData;
         this.qpath = nodeData.getQPath();
         this.location = null;

         initDefinition(parent);
      }
      else
      {
         throw new RepositoryException("Load data failed: Node expected");
      }
   }

   /**
    * Init NodeDefinition.
    * 
    * @param parent NodeData 
    * @throws RepositoryException
    *           if error occurs
    * @throws ConstraintViolationException
    *           if definition not found
    */
   private void initDefinition(NodeData parent) throws RepositoryException, ConstraintViolationException
   {
      if (this.isRoot())
      {
         // root - no parent
         this.definition =
            new NodeDefinitionData(null, null, true, true, OnParentVersionAction.ABORT, true,
               new InternalQName[]{Constants.NT_BASE}, null, false);
         return;
      }

      if (parent == null)
      {
         parent = (NodeData)dataManager.getItemData(getParentIdentifier());
      }

      this.definition =
         session.getWorkspace().getNodeTypesHolder().getChildNodeDefinition(getInternalName(),
            nodeData().getPrimaryTypeName(), parent.getPrimaryTypeName(), parent.getMixinTypeNames());

      if (definition == null)
      {
         throw new ConstraintViolationException("Node definition not found for " + getPath());
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public ItemDefinitionData getItemDefinitionData()
   {
      return definition;
   }

   /**
    * {@inheritDoc}
    */
   public Lock lock(boolean isDeep, boolean isSessionScoped) throws UnsupportedRepositoryOperationException,
      LockException, AccessDeniedException, RepositoryException
   {

      checkValid();

      if (!isNodeType(Constants.MIX_LOCKABLE))
      {
         throw new LockException("Node is not lockable " + getPath());
      }

      // session.checkPermission(getPath(), PermissionType.SET_PROPERTY) is not used because RepositoryException
      // is wrapped into AccessControlException
      if (!session.getAccessManager().hasPermission(getACL(), new String[]{PermissionType.SET_PROPERTY},
         session.getUserState().getIdentity()))
      {
         throw new AccessDeniedException("Access denied: lock operation " + getPath() + " for: " + session.getUserID()
            + " item owner " + getACL().getOwner());
      }

      if (dataManager.hasPendingChanges(getInternalPath()))
      {
         throw new InvalidItemStateException("Node has pending unsaved changes " + getPath());
      }

      Lock newLock = session.getLockManager().addLock(this, isDeep, isSessionScoped, -1);

      PlainChangesLog changesLog =
         new PlainChangesLogImpl(new ArrayList<ItemState>(), session.getId(), ExtendedEvent.LOCK);

      PropertyData propData =
         TransientPropertyData.createPropertyData(nodeData(), Constants.JCR_LOCKOWNER, PropertyType.STRING, false,
            new TransientValueData(session.getUserID()));
      changesLog.add(ItemState.createAddedState(propData));

      propData =
         TransientPropertyData.createPropertyData(nodeData(), Constants.JCR_LOCKISDEEP, PropertyType.BOOLEAN, false,
            new TransientValueData(isDeep));
      changesLog.add(ItemState.createAddedState(propData));

      dataManager.getTransactManager().save(changesLog);

      session.getActionHandler().postLock(this);
      return newLock;

   }

   /**
    * {@inheritDoc}
    */
   public Lock lock(boolean isDeep, long timeOut) throws UnsupportedRepositoryOperationException, LockException,
      AccessDeniedException, RepositoryException
   {
      checkValid();

      if (!isNodeType(Constants.MIX_LOCKABLE))
      {
         throw new LockException("Node is not lockable " + getPath());
      }

      // session.checkPermission(getPath(), PermissionType.SET_PROPERTY) is not used because RepositoryException
      // is wrapped into AccessControlException
      if (!session.getAccessManager().hasPermission(getACL(), new String[]{PermissionType.SET_PROPERTY},
         session.getUserState().getIdentity()))
      {
         throw new AccessDeniedException("Access denied: lock operation " + getPath() + " for: " + session.getUserID()
            + " item owner " + getACL().getOwner());
      }

      if (dataManager.hasPendingChanges(getInternalPath()))
      {
         throw new InvalidItemStateException("Node has pending unsaved changes " + getPath());
      }

      Lock newLock = session.getLockManager().addLock(this, isDeep, false, timeOut);

      PlainChangesLog changesLog =
         new PlainChangesLogImpl(new ArrayList<ItemState>(), session.getId(), ExtendedEvent.LOCK);

      PropertyData propData =
         TransientPropertyData.createPropertyData(nodeData(), Constants.JCR_LOCKOWNER, PropertyType.STRING, false,
            new TransientValueData(session.getUserID()));
      changesLog.add(ItemState.createAddedState(propData));

      propData =
         TransientPropertyData.createPropertyData(nodeData(), Constants.JCR_LOCKISDEEP, PropertyType.BOOLEAN, false,
            new TransientValueData(isDeep));
      changesLog.add(ItemState.createAddedState(propData));

      dataManager.getTransactManager().save(changesLog);

      session.getActionHandler().postLock(this);
      return newLock;

   }

   /**
    * {@inheritDoc}
    */
   public NodeIterator merge(String srcWorkspace, boolean bestEffort) throws UnsupportedRepositoryOperationException,
      NoSuchWorkspaceException, AccessDeniedException, MergeException, RepositoryException, InvalidItemStateException
   {

      checkValid();

      if (!session.getAccessManager().hasPermission(getACL(),
         new String[]{PermissionType.ADD_NODE, PermissionType.SET_PROPERTY}, session.getUserState().getIdentity()))
      {
         throw new AccessDeniedException("Access denied: checkin operation " + getPath() + " for: "
            + session.getUserID() + " item owner " + getACL().getOwner());
      }

      if (session.hasPendingChanges())
      {
         throw new InvalidItemStateException("Session has pending changes ");
      }

      Map<String, String> failed = new HashMap<String, String>();

      // get corresponding node
      SessionImpl corrSession =
         ((RepositoryImpl)session.getRepository()).internalLogin(session.getUserState(), srcWorkspace);

      ItemDataMergeVisitor visitor = new ItemDataMergeVisitor(this.session, corrSession, failed, bestEffort);
      this.nodeData().accept(visitor);

      SessionChangesLog changes = visitor.getMergeChanges();

      EntityCollection failedIter = createMergeFailed(failed, changes);

      if (changes.getSize() > 0)
      {
         dataManager.getTransactManager().save(changes);
      }

      return failedIter;
   }

   /**
    * {@inheritDoc}
    */
   public void orderBefore(String srcName, String destName) throws UnsupportedRepositoryOperationException,
      ConstraintViolationException, ItemNotFoundException, RepositoryException
   {

      checkValid();

      if (!getPrimaryNodeType().hasOrderableChildNodes())
      {
         throw new UnsupportedRepositoryOperationException("Node does not support child ordering "
            + getPrimaryNodeType().getName());
      }

      JCRPath sourcePath = locationFactory.createJCRPath(getLocation(), srcName);
      JCRPath destenationPath = destName != null ? locationFactory.createJCRPath(getLocation(), destName) : null;
      QPath srcPath = sourcePath.getInternalPath();
      QPath destPath = destenationPath != null ? destenationPath.getInternalPath() : null;

      doOrderBefore(srcPath, destPath);
   }

   /**
    * {@inheritDoc}
    */
   public void removeMixin(String mixinName) throws NoSuchNodeTypeException, ConstraintViolationException,
      RepositoryException
   {

      checkValid();

      InternalQName[] mixinTypes = nodeData().getMixinTypeNames();
      InternalQName name = locationFactory.parseJCRName(mixinName).getInternalName();

      // find mixin
      InternalQName removedName = null;
      // Prepare mixin values
      List<InternalQName> newMixin = new ArrayList<InternalQName>();
      List<ValueData> values = new ArrayList<ValueData>();

      for (InternalQName mt : mixinTypes)
      {
         if (mt.equals(name))
         {
            removedName = mt;
         }
         else
         {
            newMixin.add(mt);
            values.add(new TransientValueData(mt));
         }
      }

      // no mixin found
      if (removedName == null)
      {
         throw new NoSuchNodeTypeException("No mixin type found " + mixinName + " for node " + getPath());
      }

      // A ConstraintViolationException will be thrown either
      // immediately or on save if the removal of a mixin is not
      // allowed. Implementations are free to enforce any policy
      // they like with regard to mixin removal and may differ on
      // when this validation is done.

      // Check if versionable ancestor is not checked-in
      if (!checkedOut())
      {
         throw new VersionException("Node " + getPath() + " or its nearest ancestor is checked-in");
      }

      // Check locking
      if (!checkLocking())
      {
         throw new LockException("Node " + getPath() + " is locked ");
      }

      session.getActionHandler().preRemoveMixin(this, name);

      PropertyData propData =
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_MIXINTYPES, 0),
            ItemType.PROPERTY);

      // create new property data with new values
      TransientPropertyData prop =
         new TransientPropertyData(propData.getQPath(), propData.getIdentifier(), propData.getPersistedVersion(),
            propData.getType(), propData.getParentIdentifier(), propData.isMultiValued(), values);

      NodeTypeDataManager ntmanager = session.getWorkspace().getNodeTypesHolder();

      // remove mix:versionable stuff
      if (ntmanager.isNodeType(Constants.MIX_VERSIONABLE, removedName))
      {
         removeVersionable();
      }

      // remove mix:lockable stuff
      if (ntmanager.isNodeType(Constants.MIX_LOCKABLE, removedName))
      {
         removeLockable();
      }

      // Set mixin property and locally
      updateMixin(newMixin);

      // Remove mixin nt definition node/properties from this node
      QPath ancestorToSave = nodeData().getQPath();

      for (PropertyDefinitionData pd : ntmanager.getAllPropertyDefinitions(removedName))
      {
         ItemData p = dataManager.getItemData(nodeData(), new QPathEntry(pd.getName(), 1), ItemType.PROPERTY);
         if (p != null && !p.isNode())
         {
            // remove it
            dataManager.delete(p, ancestorToSave);
         }
      }

      for (NodeDefinitionData nd : ntmanager.getAllChildNodeDefinitions(removedName))
      {
         ItemData n = dataManager.getItemData(nodeData(), new QPathEntry(nd.getName(), 1), ItemType.NODE);
         if (n != null && n.isNode())
         {
            // remove node with subtree
            ItemDataRemoveVisitor remover = new ItemDataRemoveVisitor(dataManager, ancestorToSave);
            n.accept(remover);
            for (ItemState deleted : remover.getRemovedStates())
            {
               dataManager.delete(deleted.getData(), ancestorToSave);
            }
         }
      }

      if (newMixin.size() > 0)
      {
         dataManager.update(new ItemState(prop, ItemState.UPDATED, true, ancestorToSave), false);
      }
      else
      {
         dataManager.delete(prop, ancestorToSave);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void removePermission(String identity) throws RepositoryException, AccessControlException
   {

      checkValid();

      if (!isNodeType(Constants.EXO_PRIVILEGEABLE))
      {
         throw new AccessControlException("Node is not exo:privilegeable " + getPath());
      }

      checkPermission(PermissionType.CHANGE_PERMISSION);

      AccessControlList acl = new AccessControlList(getACL().getOwner(), getACL().getPermissionEntries());
      acl.removePermissions(identity);
      setACL(acl);
      updatePermissions(acl);
   }

   /**
    * {@inheritDoc}
    */
   public void removePermission(String identity, String permission) throws RepositoryException, AccessControlException
   {

      checkValid();

      if (!isNodeType(Constants.EXO_PRIVILEGEABLE))
      {
         throw new AccessControlException("Node is not exo:privilegeable " + getPath());
      }

      checkPermission(PermissionType.CHANGE_PERMISSION);

      AccessControlList acl = new AccessControlList(getACL().getOwner(), getACL().getPermissionEntries());
      acl.removePermissions(identity, permission);
      setACL(acl);
      updatePermissions(acl);
   }

   /**
    * {@inheritDoc}
    */
   public void restore(String versionName, boolean removeExisting) throws VersionException, ItemExistsException,
      UnsupportedRepositoryOperationException, LockException, RepositoryException, InvalidItemStateException
   {

      VersionImpl version = (VersionImpl)versionHistory(false).version(versionName, false);
      restore(version, removeExisting);
   }

   /**
    * {@inheritDoc}
    */
   public void restore(Version version, boolean removeExisting) throws VersionException, ItemExistsException,
      UnsupportedRepositoryOperationException, LockException, RepositoryException, InvalidItemStateException
   {

      checkValid();

      if (!session.getAccessManager().hasPermission(getACL(),
         new String[]{PermissionType.ADD_NODE, PermissionType.SET_PROPERTY}, session.getUserState().getIdentity()))
      {
         throw new AccessDeniedException("Access denied: restore operation " + getPath() + " for: "
            + session.getUserID() + " item owner " + getACL().getOwner());
      }

      if (!this.isNodeType(Constants.MIX_VERSIONABLE))
      {
         throw new UnsupportedRepositoryOperationException("Node is not versionable " + getPath());
      }

      if (session.hasPendingChanges())
      {
         throw new InvalidItemStateException("Session has pending changes ");
      }

      if (((VersionImpl)version).getInternalName().equals(Constants.JCR_ROOTVERSION))
      {
         throw new VersionException("It is illegal to call restore() on jcr:rootVersion");
      }

      if (!versionHistory(false).isVersionBelongToThis(version))
      {
         throw new VersionException("Bad version " + version.getPath());
      }

      // Check locking
      if (!checkLocking())
      {
         throw new LockException("Node " + getPath() + " is locked ");
      }

      NodeData destParent = (NodeData)dataManager.getItemData(nodeData().getParentIdentifier());
      ((VersionImpl)version).restore(this.getSession(), destParent, nodeData().getQPath().getName(), removeExisting);
   }

   /**
    * {@inheritDoc}
    */
   public void restore(Version version, String relPath, boolean removeExisting) throws VersionException,
      ItemExistsException, UnsupportedRepositoryOperationException, LockException, RepositoryException,
      InvalidItemStateException
   {

      if (JCRPath.THIS_RELPATH.equals(relPath))
      {

         // restore at this position
         this.restore(version, removeExisting);
      }
      else
      {

         // restore at relPath
         checkValid();

         if (!session.getAccessManager().hasPermission(getACL(),
            new String[]{PermissionType.ADD_NODE, PermissionType.SET_PROPERTY}, session.getUserState().getIdentity()))
         {
            throw new AccessDeniedException("Access denied: checkin operation " + getPath() + " for: "
               + session.getUserID() + " item owner " + getACL().getOwner());
         }

         if (session.hasPendingChanges())
         {
            throw new InvalidItemStateException("Session has pending changes ");
         }

         if (((VersionImpl)version).getInternalName().equals(Constants.JCR_ROOTVERSION))
         {
            throw new VersionException("It is illegal to call restore() on jcr:rootVersion");
         }

         QPath destPath = locationFactory.parseRelPath(relPath).getInternalPath();
         NodeImpl destParent =
            (NodeImpl)dataManager.getItem(nodeData(), destPath.makeParentPath().getEntries(), false, ItemType.NODE);

         if (destParent == null)
         {
            throw new PathNotFoundException("Parent not found for " + relPath);
         }

         if (!destParent.isNode())
         {
            throw new ConstraintViolationException("Parent item is not a node. Rel path " + relPath);
         }

         NodeImpl destNode =
            (NodeImpl)dataManager.getItem(destParent.nodeData(),
               new QPathEntry(destPath.getName(), destPath.getIndex()), false, ItemType.NODE);

         if (destNode != null)
         {
            // Dest node exists

            if (!destNode.isNode())
            {
               throw new ConstraintViolationException("Item at relPath is not a node " + destNode.getPath());
            }

            if (!destNode.isNodeType(Constants.MIX_VERSIONABLE))
            {
               throw new UnsupportedRepositoryOperationException("Node at relPath is not versionable "
                  + destNode.getPath());
            }

            if (!destNode.versionHistory(false).isVersionBelongToThis(version))
            {
               throw new VersionException("Bad version " + version.getPath());
            }

            // Check locking
            if (!destNode.parent().checkLocking())
            {
               throw new LockException("Node " + destNode.getPath() + " is locked ");
            }
         }
         else
         {
            // Dest node not found
            if (!destParent.checkedOut())
            {
               throw new VersionException("Parent of a node at relPath is versionable and checked-in "
                  + destParent.getPath());
            }
         }

         ((VersionImpl)version).restore(session, destParent.nodeData(), destPath.getName(), removeExisting);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void restoreByLabel(String versionLabel, boolean removeExisting) throws VersionException,
      ItemExistsException, UnsupportedRepositoryOperationException, LockException, RepositoryException,
      InvalidItemStateException
   {

      checkValid();

      VersionImpl version = (VersionImpl)getVersionHistory().getVersionByLabel(versionLabel);
      restore(version, removeExisting);

   }

   /**
    * {@inheritDoc}
    */
   public void setPermission(String identity, String[] permission) throws RepositoryException, AccessControlException
   {

      checkValid();

      if (!isNodeType(Constants.EXO_PRIVILEGEABLE))
      {
         throw new AccessControlException("Node is not exo:privilegeable " + getPath());
      }

      if (identity == null)
      {
         throw new RepositoryException("Identity cannot be null");
      }

      if (permission == null)
      {
         throw new RepositoryException("Permission cannot be null");
      }

      // check if changing permission allowed
      checkPermission(PermissionType.CHANGE_PERMISSION);
      AccessControlList acl = new AccessControlList(getACL().getOwner(), getACL().getPermissionEntries());
      acl.removePermissions(identity);
      acl.addPermissions(identity, permission);
      setACL(acl);
      updatePermissions(acl);
   }

   // //////////////////////// OPTIONAL

   // VERSIONING

   /**
    * {@inheritDoc}
    */
   public void setPermissions(Map permissions) throws RepositoryException, AccessDeniedException,
      AccessControlException
   {

      checkValid();

      if (!isNodeType(Constants.EXO_PRIVILEGEABLE))
      {
         throw new AccessControlException("Node is not exo:privilegeable " + getPath());
      }

      if (permissions.size() == 0)
      {
         throw new RepositoryException("Permission map size cannot be 0");
      }

      checkPermission(PermissionType.CHANGE_PERMISSION);

      List<AccessControlEntry> aces = new ArrayList<AccessControlEntry>();
      for (Iterator<String> i = permissions.keySet().iterator(); i.hasNext();)
      {
         String identity = i.next();
         if (identity == null)
         {
            throw new RepositoryException("Identity cannot be null");
         }

         String[] perm = (String[])permissions.get(identity);
         if (perm == null)
         {
            throw new RepositoryException("Permissions cannot be null");
         }

         for (int j = 0; j < perm.length; j++)
         {
            AccessControlEntry ace = new AccessControlEntry(identity, perm[j]);
            aces.add(ace);
         }
      }

      AccessControlList acl = new AccessControlList(getACL().getOwner(), aces);
      setACL(acl);
      updatePermissions(acl);
   }

   /**
    * {@inheritDoc}
    */
   public Property setProperty(String name, boolean value) throws ValueFormatException, VersionException,
      LockException, ConstraintViolationException, RepositoryException
   {

      checkValid();

      return doUpdateProperty(this, locationFactory.parseJCRName(name).getInternalName(), valueFactory
         .createValue(value), false, PropertyType.UNDEFINED);

   }

   /**
    * {@inheritDoc}
    */
   public Property setProperty(String name, Calendar value) throws ValueFormatException, VersionException,
      LockException, ConstraintViolationException, RepositoryException
   {

      checkValid();

      return doUpdateProperty(this, locationFactory.parseJCRName(name).getInternalName(), valueFactory
         .createValue(value), false, PropertyType.UNDEFINED);

   }

   /**
    * {@inheritDoc}
    */
   public Property setProperty(String name, double value) throws ValueFormatException, VersionException, LockException,
      ConstraintViolationException, RepositoryException
   {

      checkValid();

      return doUpdateProperty(this, locationFactory.parseJCRName(name).getInternalName(), valueFactory
         .createValue(value), false, PropertyType.UNDEFINED);
   }

   /**
    * {@inheritDoc}
    */
   public Property setProperty(String name, InputStream value) throws ValueFormatException, VersionException,
      LockException, ConstraintViolationException, RepositoryException
   {

      checkValid();

      return doUpdateProperty(this, locationFactory.parseJCRName(name).getInternalName(), valueFactory
         .createValue(value), false, PropertyType.UNDEFINED);
   }

   /**
    * {@inheritDoc}
    */
   public Property setProperty(String name, long value) throws ValueFormatException, VersionException, LockException,
      ConstraintViolationException, RepositoryException
   {

      checkValid();

      return doUpdateProperty(this, locationFactory.parseJCRName(name).getInternalName(), valueFactory
         .createValue(value), false, PropertyType.UNDEFINED);

   }

   /**
    * {@inheritDoc}
    */
   public Property setProperty(String name, Node value) throws ValueFormatException, VersionException, LockException,
      ConstraintViolationException, RepositoryException
   {

      checkValid();

      return doUpdateProperty(this, locationFactory.parseJCRName(name).getInternalName(), valueFactory
         .createValue(value), false, PropertyType.UNDEFINED);

   }

   /**
    * {@inheritDoc}
    */
   public Property setProperty(String name, String value) throws ValueFormatException, VersionException, LockException,
      ConstraintViolationException, RepositoryException
   {

      checkValid();

      return doUpdateProperty(this, locationFactory.parseJCRName(name).getInternalName(), valueFactory
         .createValue(value), false, PropertyType.UNDEFINED);

   }

   /**
    * {@inheritDoc}
    */
   public Property setProperty(String name, String value, int type) throws ValueFormatException, VersionException,
      LockException, ConstraintViolationException, RepositoryException
   {

      checkValid();

      return doUpdateProperty(this, locationFactory.parseJCRName(name).getInternalName(), valueFactory.createValue(
         value, type), false, type);
   }

   /**
    * {@inheritDoc}
    */
   public Property setProperty(String name, String[] values) throws ValueFormatException, VersionException,
      LockException, ConstraintViolationException, RepositoryException
   {

      checkValid();

      Value[] val = null;
      if (values != null)
      {
         val = new Value[values.length];
         for (int i = 0; i < values.length; i++)
         {
            val[i] = valueFactory.createValue(values[i]);
         }
      }
      return doUpdateProperty(this, locationFactory.parseJCRName(name).getInternalName(), val, true,
         PropertyType.UNDEFINED);
   }

   /**
    * {@inheritDoc}
    */
   public Property setProperty(String name, String[] values, int type) throws ValueFormatException, VersionException,
      LockException, ConstraintViolationException, RepositoryException
   {

      checkValid();

      Value[] val = null;
      if (values != null)
      {
         val = new Value[values.length];
         for (int i = 0; i < values.length; i++)
         {
            val[i] = valueFactory.createValue(values[i], type);
         }
      }

      return doUpdateProperty(this, locationFactory.parseJCRName(name).getInternalName(), val, true, type);
   }

   /**
    * {@inheritDoc}
    */
   public Property setProperty(String name, Value value) throws ValueFormatException, VersionException, LockException,
      ConstraintViolationException, RepositoryException
   {

      checkValid();

      return doUpdateProperty(this, locationFactory.parseJCRName(name).getInternalName(), value, false,
         PropertyType.UNDEFINED);
   }

   /**
    * {@inheritDoc}
    */
   public Property setProperty(String name, Value value, int type) throws ValueFormatException, VersionException,
      LockException, ConstraintViolationException, RepositoryException
   {

      checkValid();

      return doUpdateProperty(this, locationFactory.parseJCRName(name).getInternalName(), value, false, type);

   }

   /**
    * {@inheritDoc}
    */
   public Property setProperty(String name, Value[] values) throws ValueFormatException, VersionException,
      LockException, ConstraintViolationException, RepositoryException
   {

      checkValid();

      return doUpdateProperty(this, locationFactory.parseJCRName(name).getInternalName(), values, true,
         PropertyType.UNDEFINED);
   }

   /**
    * {@inheritDoc}
    */
   public Property setProperty(String name, Value[] values, int type) throws ValueFormatException, VersionException,
      LockException, ConstraintViolationException, RepositoryException
   {

      checkValid();

      return doUpdateProperty(this, locationFactory.parseJCRName(name).getInternalName(), values, true, type);
   }

   /**
    * {@inheritDoc}
    */
   public void unlock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException,
      RepositoryException
   {

      checkValid();

      if (!session.getLockManager().holdsLock((NodeData)this.getData()))
      {
         throw new LockException("The node not locked " + getPath());
      }

      if (!session.getLockManager().isLockHolder(this))
      {
         throw new LockException("There are no permission to unlock the node " + getPath());
      }

      if (dataManager.hasPendingChanges(getInternalPath()))
      {
         throw new InvalidItemStateException("Node has pending unsaved changes " + getPath());
      }

      // session.checkPermission(getPath(), PermissionType.SET_PROPERTY) is not used because RepositoryException
      // is wrapped into AccessControlException
      if (!session.getAccessManager().hasPermission(getACL(), new String[]{PermissionType.SET_PROPERTY},
         session.getUserState().getIdentity()))
      {
         throw new AccessDeniedException("Access denied: unlock operation " + getPath() + " for: "
            + session.getUserID() + " item owner " + getACL().getOwner());
      }

      doUnlock();

      session.getActionHandler().postUnlock(this);
   }

   /**
    * {@inheritDoc}
    */
   public void update(String srcWorkspaceName) throws NoSuchWorkspaceException, AccessDeniedException,
      InvalidItemStateException, LockException, RepositoryException
   {

      checkValid();

      // Check pending changes
      if (session.hasPendingChanges())
      {
         throw new InvalidItemStateException("Session has pending changes ");
      }

      // Check locking
      if (!checkLocking())
      {
         throw new LockException("Node " + getPath() + " is locked ");
      }

      SessionChangesLog changes = new SessionChangesLog(session.getId());

      String srcPath;
      try
      {
         srcPath = getCorrespondingNodePath(srcWorkspaceName);

         ItemDataRemoveVisitor remover =
            new ItemDataRemoveVisitor(session.getTransientNodesManager(), null, session.getWorkspace()
               .getNodeTypesHolder(), session.getAccessManager(), session.getUserState());
         nodeData().accept(remover);

         changes.addAll(remover.getRemovedStates());
      }
      catch (ItemNotFoundException e)
      {
         LOG.debug("No corresponding node in workspace: " + srcWorkspaceName);
         return;
      }

      TransactionableDataManager pmanager = session.getTransientNodesManager().getTransactManager();

      session.getWorkspace().clone(srcWorkspaceName, srcPath, this.getPath(), true, changes);
      pmanager.save(changes);

      NodeData thisParent = (NodeData)session.getTransientNodesManager().getItemData(getParentIdentifier());
      QPathEntry[] qpath = getInternalPath().getEntries();
      NodeData thisNew = (NodeData)pmanager.getItemData(thisParent, qpath[qpath.length - 1], ItemType.NODE);
      // reload node impl with old uuid to a new one data
      session.getTransientNodesManager().getItemsPool().reload(getInternalIdentifier(), thisNew);
   }

   public void validateChildNode(InternalQName name, InternalQName primaryTypeName) throws ItemExistsException,
      RepositoryException, ConstraintViolationException, VersionException, LockException
   {

      // Check if nodeType exists and not mixin
      NodeTypeDataManager nodeTypeDataManager = session.getWorkspace().getNodeTypesHolder();
      NodeTypeData nodeType = nodeTypeDataManager.getNodeType(primaryTypeName);
      if (nodeType == null)
      {
         throw new NoSuchNodeTypeException("Nodetype not found "
            + sysLocFactory.createJCRName(primaryTypeName).getAsString());
      }

      if (nodeType.isMixin())
      {
         throw new ConstraintViolationException("Add Node failed, "
            + sysLocFactory.createJCRName(primaryTypeName).getAsString() + " is MIXIN type!");
      }

      // Check if new node's node type is allowed by its parent definition

      if (!nodeTypeDataManager.isChildNodePrimaryTypeAllowed(primaryTypeName, nodeData().getPrimaryTypeName(),
         nodeData().getMixinTypeNames()))
      {
         throw new ConstraintViolationException("Can't add node " + sysLocFactory.createJCRName(name).getAsString()
            + " to " + getPath() + " node type " + sysLocFactory.createJCRName(primaryTypeName).getAsString()
            + " is not allowed as child's node type for parent node type ");

      }
      // Check if node is not protected
      NodeDefinitionData childNodeDefinition =
         session.getWorkspace().getNodeTypesHolder().getChildNodeDefinition(name, primaryTypeName,
            nodeData().getPrimaryTypeName(), nodeData().getMixinTypeNames());
      if (childNodeDefinition == null)
      {
         throw new ConstraintViolationException("Can't find child node definition for "
            + sysLocFactory.createJCRName(name).getAsString() + " in " + getPath());
      }

      if (childNodeDefinition.isProtected())
      {
         throw new ConstraintViolationException("Can't add protected node "
            + sysLocFactory.createJCRName(name).getAsString() + " to " + getPath());
      }

      // Check if versionable ancestor is not checked-in
      if (!checkedOut())
      {
         throw new VersionException("Node " + getPath() + " or its nearest ancestor is checked-in");
      }

      if (!checkLocking())
      {
         // Check locking
         throw new LockException("Node " + getPath() + " is locked ");
      }

   }

   /**
    * For internal use. Doesn't check the InvalidItemStateException and may return unpooled
    * VersionHistory object.
    * 
    * @param pool
    *          boolean, true if result should be pooled in Session
    * @return VersionHistoryImpl
    * @throws UnsupportedRepositoryOperationException
    *           if versions is nopt supported
    * @throws RepositoryException
    *           if error occurs
    */
   public VersionHistoryImpl versionHistory(boolean pool) throws UnsupportedRepositoryOperationException,
      RepositoryException
   {

      if (!this.isNodeType(Constants.MIX_VERSIONABLE))
      {
         throw new UnsupportedRepositoryOperationException("Node is not mix:versionable " + getPath());
      }

      PropertyData vhProp =
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_VERSIONHISTORY, 1),
            ItemType.PROPERTY);
      if (vhProp == null)
      {
         throw new UnsupportedRepositoryOperationException("Node does not have jcr:versionHistory " + getPath());
      }

      try
      {
         return (VersionHistoryImpl)dataManager.getItemByIdentifier(new String(vhProp.getValues().get(0)
            .getAsByteArray()), pool, false);
      }
      catch (IOException e)
      {
         throw new RepositoryException("Error of version history ID read " + e, e);
      }
   }

   boolean checkLocking() throws RepositoryException
   {
      //      return (!isLocked() || session.getLockManager().isLockHolder(this) || session.getUserID().equals(
      //         SystemIdentity.SYSTEM));

      return (session.getLockManager().checkLocking(this.nodeData()) || session.getUserID().equals(
         SystemIdentity.SYSTEM));

   }

   protected void doOrderBefore(QPath srcPath, QPath destPath) throws RepositoryException
   {
      if (srcPath.equals(destPath))
      {
         return;
      }

      // check existence
      if (dataManager.getItemData(srcPath) == null)
      {
         throw new ItemNotFoundException(getPath() + " has no child node with name " + srcPath.getName().getAsString());
      }

      if (destPath != null && dataManager.getItemData(destPath) == null)
      {

         throw new ItemNotFoundException(getPath() + " has no child node with name " + destPath.getName().getAsString());
      }

      if (!checkedOut())
      {
         throw new VersionException(" cannot change child node ordering of a checked-in node ");
      }

      if (destPath != null && srcPath.getDepth() != destPath.getDepth())
      {
         throw new ItemNotFoundException("Source and destenation is not relative paths of depth one, "
            + "i.e. is not a childs of same parent node");
      }

      List<NodeData> siblings = new ArrayList<NodeData>(dataManager.getChildNodesData(nodeData()));
      if (siblings.size() < 2)
      {
         throw new UnsupportedRepositoryOperationException("Nothing to order Count of child nodes " + siblings.size());
      }

      Collections.sort(siblings, new NodeDataOrderComparator());

      // calculating source and destination position
      int srcInd = -1, destInd = -1;
      for (int i = 0; i < siblings.size(); i++)
      {
         NodeData nodeData = siblings.get(i);
         if (srcInd == -1)
         {
            if (nodeData.getQPath().getName().equals(srcPath.getName()))
            {
               srcInd = i;
            }
         }
         if (destPath != null && destInd == -1)
         {
            if (nodeData.getQPath().getName().equals(destPath.getName()))
            {
               destInd = i;
               if (srcInd != -1)
               {
                  break;
               }
            }
         }
         else
         {
            if (srcInd != -1)
            {
               break;
            }
         }
      }

      // check if resulting order would be different to current order
      if (destInd == -1)
      {
         if (srcInd == siblings.size() - 1)
         {
            // no change, we're done
            return;
         }
      }
      else
      {
         if ((destInd - srcInd) == 1)
         {
            // no change, we're done
            return;
         }
      }

      // reorder list
      if (destInd == -1)
      {
         siblings.add(siblings.remove(srcInd));
      }
      else
      {
         if (srcInd < destInd)
         {
            siblings.add(destInd, siblings.get(srcInd));
            siblings.remove(srcInd);
         }
         else
         {
            siblings.add(destInd, siblings.remove(srcInd));
         }
      }

      int sameNameIndex = 0;
      List<ItemState> changes = new ArrayList<ItemState>();
      ItemState deleteState = null;
      for (int j = 0; j < siblings.size(); j++)
      {
         NodeData sdata = siblings.get(j);

         // calculating same name index
         if (sdata.getQPath().getName().getAsString().equals(srcPath.getName().getAsString()))
         {
            ++sameNameIndex;
         }

         // skeep unchanged
         if (sdata.getOrderNumber() == j)
         {
            continue;
         }

         NodeData newData = sdata;
         // change same name index
         if (sdata.getQPath().getName().getAsString().equals(srcPath.getName().getAsString())
            && sdata.getQPath().getIndex() != sameNameIndex)
         {
            // update with new index
            QPath siblingPath =
               QPath.makeChildPath(newData.getQPath().makeParentPath(), newData.getQPath().getName(), sameNameIndex);

            newData =
               new TransientNodeData(siblingPath, newData.getIdentifier(), newData.getPersistedVersion(), newData
                  .getPrimaryTypeName(), newData.getMixinTypeNames(), j, newData.getParentIdentifier(), newData
                  .getACL());
         }
         else
         {
            newData =
               new TransientNodeData(newData.getQPath(), newData.getIdentifier(), newData.getPersistedVersion(),
                  newData.getPrimaryTypeName(), newData.getMixinTypeNames(), j, newData.getParentIdentifier(), newData
                     .getACL());
         }

         /*
          * 8.3.7.8 Re-ordering a set of Child Nodes. When an orderBefore(A, B) is
          * performed, an implementation must generate a NODE_REMOVED for node A
          * and a NODE_ADDED for node A. Note that the paths associated with these
          * two events will either differ by the last index number (if the movement
          * of A causes it to be re-ordered with respect to its same-name siblings)
          * or be identical (if A does not have same-name siblings or if the
          * movement of A does not change its order relative to its same-name
          * siblings). Additionally, an implementation should generate appropriate
          * events reflecting the "shifting" over of the node B and any nodes that
          * come after it in the child node ordering. Each such shifted node would
          * also produce a NODE_REMOVED and NODE_ADDED event pair with paths
          * differing at most by a final index.
          */
         if (sdata.getQPath().equals(srcPath))
         {
            deleteState = new ItemState(sdata, ItemState.DELETED, true, null, false, false);
            changes.add(new ItemState(newData, ItemState.UPDATED, true, null, false, true));
         }
         else
         {
            ItemState state = ItemState.createUpdatedState(newData);
            state.eraseEventFire();
            changes.add(state);
         }
      }
      // delete state first
      if (deleteState != null)
      {
         dataManager.getChangesLog().add(deleteState);
      }
      dataManager.getChangesLog().addAll(changes);
   }

   /**
    * Remove mix:lockable properties.
    * 
    * @throws RepositoryException if error occurs
    */
   protected void doUnlock() throws RepositoryException
   {

      PlainChangesLog changesLog =
         new PlainChangesLogImpl(new ArrayList<ItemState>(), session.getId(), ExtendedEvent.UNLOCK);

      ItemData lockOwner =
         dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_LOCKOWNER, 0), ItemType.PROPERTY);

      changesLog.add(ItemState.createDeletedState(lockOwner));

      ItemData lockIsDeep =
         dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_LOCKISDEEP, 0), ItemType.PROPERTY);

      changesLog.add(ItemState.createDeletedState(lockIsDeep));

      dataManager.getTransactManager().save(changesLog);
   }

   protected NodeData nodeData()
   {
      return (NodeData)data;
   }

   protected PropertyImpl property(InternalQName name) throws IllegalPathException, PathNotFoundException,
      RepositoryException
   {
      PropertyImpl prop =
         (PropertyImpl)dataManager.getItem(nodeData(), new QPathEntry(name, 1), false, ItemType.PROPERTY);
      if (prop == null || prop.isNode())
      {
         throw new PathNotFoundException("Property not found " + name);
      }
      return prop;
   }

   protected void removeLockable() throws RepositoryException
   {
      if (session.getLockManager().holdsLock(nodeData()))
      {
         // locked, should be unlocked

         if (!session.getLockManager().isLockHolder(this))
         {
            throw new LockException("There are no permission to unlock the node " + getPath());
         }

         // remove mix:lockable properties (as the node is locked)
         doUnlock();
      }
   }

   // ////////////////// Item implementation ////////////////////

   protected PropertyData updatePropertyData(InternalQName name, List<ValueData> values) throws RepositoryException
   {

      PropertyData existed =
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(name, 0), ItemType.PROPERTY);

      if (existed == null)
      {
         throw new RepositoryException("Property data is not found " + name.getAsString() + " for node "
            + nodeData().getQPath().getAsString());
      }

      if (!existed.isMultiValued())
      {
         throw new ValueFormatException("An existed property is single-valued " + name.getAsString());
      }

      TransientPropertyData tdata =
         new TransientPropertyData(QPath.makeChildPath(getInternalPath(), name), existed.getIdentifier(), existed
            .getPersistedVersion(), existed.getType(), existed.getParentIdentifier(), existed.isMultiValued(), values);

      return tdata;
   }

   protected PropertyData updatePropertyData(InternalQName name, ValueData value) throws RepositoryException
   {

      PropertyData existed =
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(name, 0), ItemType.PROPERTY);

      if (existed == null)
      {
         throw new RepositoryException("Property data is not found " + name.getAsString() + " for node "
            + nodeData().getQPath().getAsString());
      }

      TransientPropertyData tdata =
         new TransientPropertyData(QPath.makeChildPath(getInternalPath(), name), existed.getIdentifier(), existed
            .getPersistedVersion(), existed.getType(), existed.getParentIdentifier(), existed.isMultiValued(), value);
      return tdata;

   }

   /**
    * Return child Properties list.
    * 
    * @return List of child Properties
    * @throws RepositoryException
    *           if error occurs
    * @throws AccessDeniedException
    *           if Nodes cannot be listed due to permissions on this Node
    */
   private List<PropertyData> childPropertiesData() throws RepositoryException, AccessDeniedException
   {

      List<PropertyData> storedProps = new ArrayList<PropertyData>(dataManager.getChildPropertiesData(nodeData()));

      // TODO we should not sort here!
      Collections.sort(storedProps, new PropertiesDataOrderComparator<PropertyData>());

      return storedProps;
   }

   /**
    * Return child Nodes list.
    * 
    * @return List of child Nodes
    * @throws RepositoryException
    *           if error occurs
    * @throws AccessDeniedException
    *           if Nodes cannot be listed due to permissions on this Node
    */
   private List<NodeData> childNodesData() throws RepositoryException, AccessDeniedException
   {
      List<NodeData> storedNodes = new ArrayList<NodeData>(dataManager.getChildNodesData(nodeData()));
      Collections.sort(storedNodes, new NodeDataOrderComparator());
      return storedNodes;
   }

   private EntityCollection createMergeFailed(Map<String, String> failed, SessionChangesLog changes)
      throws RepositoryException
   {

      EntityCollection res = new EntityCollection();
      TransientPropertyData mergeFailed =
         (TransientPropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_MERGEFAILED, 0),
            ItemType.PROPERTY);

      List<ValueData> mergeFailedRefs = new ArrayList<ValueData>();
      int state = 0;
      try
      {
         if (mergeFailed != null)
         {
            for (ValueData mfvd : mergeFailed.getValues())
            {
               mergeFailedRefs.add(new TransientValueData(mfvd.getAsByteArray()));
            }

            mergeFailed =
               new TransientPropertyData(mergeFailed.getQPath(), mergeFailed.getIdentifier(), mergeFailed
                  .getPersistedVersion(), mergeFailed.getType(), mergeFailed.getParentIdentifier(), mergeFailed
                  .isMultiValued(), mergeFailedRefs);

            state = ItemState.UPDATED;
         }
         else
         {
            mergeFailed =
               TransientPropertyData.createPropertyData((NodeData)getData(), Constants.JCR_MERGEFAILED,
                  PropertyType.REFERENCE, true, mergeFailedRefs);
            state = ItemState.ADDED;
         }

         nextFail : for (String identifier : failed.keySet())
         {
            NodeImpl versionable = (NodeImpl)session.getNodeByUUID(identifier);
            res.add(versionable);
            String offendingIdentifier = failed.get(identifier);

            for (ValueData vd : mergeFailedRefs)
            {
               String mfIdentifier = new String(vd.getAsByteArray());
               if (mfIdentifier.equals(offendingIdentifier))
               {
                  // offending version is alredy in jcr:mergeFailed, skip it
                  continue nextFail;
               }
            }

            mergeFailedRefs.add(new TransientValueData(offendingIdentifier));
         }

         changes.add(new ItemState(mergeFailed, state, true, getInternalPath(), true));

         return res;

      }
      catch (IOException e)
      {
         throw new RepositoryException("jcr:mergeFailed read error " + e, e);
      }
   }

   // ----------------------------- ExtendedNode -----------------------------

   private int getNextChildOrderNum() throws RepositoryException
   {
      return dataManager.getLastOrderNumber(nodeData()) + 1;
   }

   /**
    * Calculates next child node index. Is used existed node definition, if no - get one based on node name
    * and node type. 
    */
   private int getNextChildIndex(InternalQName nameToAdd, InternalQName primaryTypeName, NodeData parentNode,
      NodeDefinitionData def) throws RepositoryException, ItemExistsException
   {
      if (def == null)
      {
         def =
            session.getWorkspace().getNodeTypesHolder().getChildNodeDefinition(nameToAdd, primaryTypeName,
               parentNode.getPrimaryTypeName(), parentNode.getMixinTypeNames());
      }

      boolean allowSns = def.isAllowsSameNameSiblings();

      int ind = 1;

      NodeData sibling = (NodeData)dataManager.getItemData(parentNode, new QPathEntry(nameToAdd, ind), ItemType.NODE);
      while (sibling != null)
      {
         if (allowSns)
         {
            ind++;
            sibling = (NodeData)dataManager.getItemData(parentNode, new QPathEntry(nameToAdd, ind), ItemType.NODE);
         }
         else
         {
            throw new ItemExistsException("The node " + nameToAdd + " already exists in " + getPath()
               + " and same name sibling is not allowed ");
         }
      };

      return ind;
   }

   /**
    * Do add node internally. If nodeDef not null it is used in getNextChildIndex() method to
    * avoid double calculation.
    */
   private NodeImpl doAddNode(NodeImpl parentNode, InternalQName name, InternalQName primaryTypeName,
      NodeDefinitionData nodeDef) throws ItemExistsException, RepositoryException, ConstraintViolationException,
      VersionException, LockException
   {
      validateChildNode(name, primaryTypeName);

      // Initialize data
      InternalQName[] mixinTypeNames = new InternalQName[0];
      String identifier = IdGenerator.generate();

      int orderNum = parentNode.getNextChildOrderNum();
      int index = parentNode.getNextChildIndex(name, primaryTypeName, parentNode.nodeData(), nodeDef);

      QPath path = QPath.makeChildPath(parentNode.getInternalPath(), name, index);

      AccessControlList acl = parentNode.getACL();

      // create new nodedata, [PN] fix of use index as persisted version
      NodeData nodeData =
         new TransientNodeData(path, identifier, -1, primaryTypeName, mixinTypeNames, orderNum, parentNode
            .getInternalIdentifier(), acl);

      // Create new Node
      ItemState state = ItemState.createAddedState(nodeData, false);
      NodeImpl node = (NodeImpl)dataManager.update(state, true);

      NodeTypeDataManager ntmanager = session.getWorkspace().getNodeTypesHolder();
      ItemAutocreator itemAutocreator = new ItemAutocreator(ntmanager, valueFactory, dataManager, true);

      PlainChangesLog changes =
         itemAutocreator.makeAutoCreatedItems(node.nodeData(), primaryTypeName, dataManager, session.getUserID());
      for (ItemState autoCreatedState : changes.getAllStates())
      {
         dataManager.updateItemState(autoCreatedState);
      }

      if (LOG.isDebugEnabled())
      {
         LOG.debug("new node : " + node.getPath() + " name: " + " primaryType: " + node.getPrimaryNodeType().getName()
            + " index: " + node.getIndex() + " parent: " + parentNode);
      }

      // launch event
      session.getActionHandler().postAddNode(node);

      return node;
   }

   private boolean hasProperty(InternalQName name)
   {
      try
      {
         ItemData pdata = dataManager.getItemData(nodeData(), new QPathEntry(name, 1), ItemType.PROPERTY);

         if (pdata != null && !pdata.isNode())
         {
            return true;
         }
      }
      catch (RepositoryException e)
      {
      }
      return false;
   }

   private void removeMergeFailed(Version version, PlainChangesLog changesLog) throws RepositoryException
   {

      PropertyData mergeFailed =
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.JCR_MERGEFAILED, 0),
            ItemType.PROPERTY);

      if (mergeFailed == null)
      {
         return;
      }

      List<ValueData> mf = new ArrayList<ValueData>();
      for (ValueData mfvd : mergeFailed.getValues())
      {
         try
         {
            byte[] mfb = mfvd.getAsByteArray();
            if (!version.getUUID().equals(new String(mfb)))
            {
               mf.add(new TransientValueData(mfb));
            }
         }
         catch (IOException e)
         {
            throw new RepositoryException("Remove jcr:mergeFailed error " + e, e);
         }
      }

      if (mf.size() > 0)
      {
         PropertyData mergeFailedRef =
            TransientPropertyData.createPropertyData(nodeData(), Constants.JCR_MERGEFAILED, PropertyType.REFERENCE,
               true, mf);
         changesLog.add(ItemState.createUpdatedState(mergeFailedRef));
      }
      else
      {
         // Once the last reference in jcr:mergeFailed has been either moved
         // to jcr:predecessors (with doneMerge) or just removed from
         // jcr:mergeFailed (with cancelMerge) the jcr:mergeFailed
         // property is automatically remove
         changesLog.add(ItemState.createDeletedState(new TransientPropertyData(mergeFailed.getQPath(), mergeFailed
            .getIdentifier(), mergeFailed.getPersistedVersion(), mergeFailed.getType(), mergeFailed
            .getParentIdentifier(), mergeFailed.isMultiValued(), mergeFailed.getValues()), true));
      }
   }

   private void setACL(AccessControlList acl)
   {
      NodeData nodeData = (NodeData)data;
      data =
         new TransientNodeData(nodeData.getQPath(), nodeData.getIdentifier(), nodeData.getPersistedVersion(), nodeData
            .getPrimaryTypeName(), nodeData.getMixinTypeNames(), nodeData.getOrderNumber(), nodeData
            .getParentIdentifier(), acl);
   }

   private void updateMixin(List<InternalQName> newMixin) throws RepositoryException
   {
      InternalQName[] mixins = new InternalQName[newMixin.size()];
      newMixin.toArray(mixins);

      NodeData nodeData = (NodeData)data;

      data =
         new TransientNodeData(nodeData.getQPath(), nodeData.getIdentifier(), nodeData.getPersistedVersion(), nodeData
            .getPrimaryTypeName(), mixins, nodeData.getOrderNumber(), nodeData.getParentIdentifier(), nodeData.getACL());

      //      ((TransientNodeData)data).setMixinTypeNames(mixins);

      dataManager.update(new ItemState(data, ItemState.MIXIN_CHANGED, false, null), false);
   }

   // ----------------------------- Object -----------------------------

   private void updatePermissions(AccessControlList acl) throws RepositoryException
   {
      List<ValueData> permValues = new ArrayList<ValueData>();

      List<AccessControlEntry> aces = acl.getPermissionEntries(); // new
      for (AccessControlEntry ace : aces)
      {
         permValues.add(new TransientValueData(ace));
      }

      PropertyData permProp =
         (PropertyData)dataManager.getItemData(nodeData(), new QPathEntry(Constants.EXO_PERMISSIONS, 0),
            ItemType.PROPERTY);

      permProp =
         new TransientPropertyData(permProp.getQPath(), permProp.getIdentifier(), permProp.getPersistedVersion(),
            permProp.getType(), permProp.getParentIdentifier(), permProp.isMultiValued(), permValues);

      dataManager.update(new ItemState(data, ItemState.MIXIN_CHANGED, false, null, true), false);
      dataManager.update(ItemState.createUpdatedState(permProp, true), false);

   }

   private static class NodeDataOrderComparator implements Comparator<NodeData>
   {
      public int compare(NodeData n1, NodeData n2)
      {
         return n1.getOrderNumber() - n2.getOrderNumber();
      }
   }

   // ===================== helpers =====================

   private static class PropertiesDataOrderComparator<P extends PropertyData> implements Comparator<P>
   {
      public int compare(P p1, P p2)
      {
         int r = 0;
         try
         {
            InternalQName qname1 = p1.getQPath().getName();
            InternalQName qname2 = p2.getQPath().getName();
            if (qname1.equals(Constants.JCR_PRIMARYTYPE))
            {
               r = Integer.MIN_VALUE;
            }
            else if (qname2.equals(Constants.JCR_PRIMARYTYPE))
            {
               r = Integer.MAX_VALUE;
            }
            else if (qname1.equals(Constants.JCR_MIXINTYPES))
            {
               r = Integer.MIN_VALUE + 1;
            }
            else if (qname2.equals(Constants.JCR_MIXINTYPES))
            {
               r = Integer.MAX_VALUE - 1;
            }
            else if (qname1.equals(Constants.JCR_UUID))
            {
               r = Integer.MIN_VALUE + 2;
            }
            else if (qname2.equals(Constants.JCR_UUID))
            {
               r = Integer.MAX_VALUE - 2;
            }
            else
            {
               r = qname1.getAsString().compareTo(qname2.getAsString());
            }
         }
         catch (Exception e)
         {
            LOG.error("PropertiesDataOrderComparator error: " + e, e);
         }
         return r;
      }
   }

   protected abstract class LazyItemsIterator implements RangeIterator
   {

      protected final ItemDataFilter filter;

      protected Iterator<? extends ItemData> iter;

      protected int size = -1;

      protected ItemImpl next;

      protected int pos = 0;

      LazyItemsIterator(List<? extends ItemData> items, ItemDataFilter filter) throws RepositoryException
      {
         this.iter = items.iterator();
         this.filter = filter;
         fetchNext();
      }

      protected void fetchNext() throws RepositoryException
      {
         if (iter.hasNext())
         {
            ItemData item = iter.next();

            // check read conditions 
            if (canRead(item))
            {
               next = session.getTransientNodesManager().readItem(item, nodeData(), true, false);
            }
            else
            {
               // try next
               next = null;
               fetchNext();
            }
         }
         else
         {
            next = null;
         }
      }

      protected boolean canRead(ItemData item)
      {
         return (filter != null ? filter.accept(item) : true)
            && session.getAccessManager().hasPermission(
               item.isNode() ? ((NodeData)item).getACL() : nodeData().getACL(), new String[]{PermissionType.READ},
               session.getUserState().getIdentity());
      }

      public ItemImpl nextItem()
      {
         if (next != null)
         {
            try
            {
               ItemImpl i = next;
               fetchNext();
               pos++;

               // fire action post-READ
               session.getActionHandler().postRead(i);
               return i;
            }
            catch (RepositoryException e)
            {
               LOG.error(e);
               throw new NoSuchElementException(e.toString());
            }
         }

         throw new NoSuchElementException();
      }

      /**
       * {@inheritDoc}
       */
      public boolean hasNext()
      {
         return next != null;
      }

      /**
       * {@inheritDoc}
       */
      public Object next()
      {
         return nextItem();
      }

      /**
       * {@inheritDoc}
       */
      public void skip(long skipNum)
      {
         trySkip(skipNum, true);
      }

      /**
       * Tries to skip some elements.
       * 
       * @param throwException
       *          indicates to throw an NoSuchElementException if can't skip defined count of elements
       * @return how many elememnts remained to skip
       */
      protected long trySkip(long skipNum, boolean throwException)
      {
         pos += skipNum;
         while (skipNum-- > 1)
         {
            try
            {
               iter.next();
            }
            catch (NoSuchElementException e)
            {
               if (throwException)
               {
                  throw e;
               }
               return skipNum;
            }
         }

         try
         {
            fetchNext();
         }
         catch (RepositoryException e)
         {
            LOG.error(e);
            throw new NoSuchElementException(e.toString());
         }

         return 0;
      }

      /**
       * {@inheritDoc}
       */
      public long getSize()
      {
         if (size == -1)
         {
            // calc size
            int sz = pos + (next != null ? 1 : 0);
            if (iter.hasNext())
            {
               List<ItemData> itemsLeft = new ArrayList<ItemData>();
               do
               {
                  ItemData item = iter.next();
                  if (canRead(item))
                  {
                     itemsLeft.add(item);
                     sz++;
                  }
               }
               while (iter.hasNext());

               iter = itemsLeft.iterator();
            }
            size = sz;
         }

         return size;
      }

      /**
       * {@inheritDoc}
       */
      public long getPosition()
      {
         return pos;
      }

      /**
       * {@inheritDoc}
       */
      public void remove()
      {
         LOG.warn("Remove not supported");
      }
   }

   protected class LazyNodeIteratorByPage implements RangeIterator, NodeIterator
   {
      private final SessionDataManager dataManager;

      private int limit = session.getLazyNodeIteratorPageSize();

      private int fromOrderNum = 0;

      private boolean hasNext = true;

      private int pos = 0;

      private int size = -1;

      private LazyNodeIterator lazyNodeItetator = new LazyNodeIterator(new ArrayList<NodeData>());

      LazyNodeIteratorByPage(SessionDataManager dataManager) throws RepositoryException
      {
         this.dataManager = dataManager;
      }

      /**
       * {@inheritDoc}
       */
      public Node nextNode()
      {
         return (Node)next();
      }

      /**
       * {@inheritDoc}
       */
      public boolean hasNext()
      {
         boolean hasNext = lazyNodeItetator.hasNext();

         if (!hasNext)
         {
            if (this.hasNext)
            {
               try
               {
                  readNextPage(0);
                  return hasNext();
               }
               catch (NoSuchElementException e)
               {
                  return false;
               }
            }
            else
            {
               return false;
            }
         }
         else
         {
            return true;
         }
      }

      /**
       * {@inheritDoc}
       */
      public Object next()
      {
         try
         {
            return nextItem();
         }
         catch (NoSuchElementException e)
         {
            if (hasNext)
            {
               try
               {
                  readNextPage(0);
                  return next();
               }
               catch (NoSuchElementException e1)
               {
                  throw e1;
               }
            }
            else
            {
               throw e;
            }
         }
      }

      /**
       * {@inheritDoc}
       */
      public void remove()
      {
         LOG.warn("remove() method is not supported in LazyNodeIteratorByPage");
      }

      /**
       * {@inheritDoc}
       */
      public void skip(long skipNum)
      {
         pos += skipNum;
         long leftToSkip = lazyNodeItetator.trySkip(skipNum, false);
         if (leftToSkip != 0)
         {
            readNextPage(leftToSkip + 1);
         }
      }

      /**
       * {@inheritDoc}
       */
      public long getSize()
      {
         if (size < 0)
         {
            try
            {
               size = dataManager.getChildNodesCount(nodeData());
            }
            catch (RepositoryException e)
            {
               LOG.error("Can't determmine number of child nodes.", e);
            }
         }
         return size;
      }

      public ItemImpl nextItem()
      {
         ItemImpl item = lazyNodeItetator.nextItem();
         pos++;

         return item;
      }

      /**
       * {@inheritDoc}
       */
      public long getPosition()
      {
         return pos;
      }

      private void readNextPage(long skip) throws NoSuchElementException
      {
         List<NodeData> storedNodes = new ArrayList<NodeData>();
         try
         {
            hasNext = dataManager.getChildNodesDataByPage(nodeData(), fromOrderNum, limit, storedNodes);
         }
         catch (RepositoryException e)
         {
            LOG.error("There are no more elements in iterator", e);
            throw new NoSuchElementException(e.toString());
         }

         Collections.sort(storedNodes, new NodeDataOrderComparator());

         int size = storedNodes.size();

         fromOrderNum = size == 0 ? fromOrderNum + limit : storedNodes.get(size - 1).getOrderNumber() + 1;

         // skip some nodes
         if (size != 0)
         {
            while (skip > 0 && storedNodes.size() > 0)
            {
               storedNodes.remove(0);
               skip--;
            }
         }

         if (skip != 0)
         {
            if (hasNext)
            {
               readNextPage(skip);
            }
            else
            {
               throw new NoSuchElementException("There are no more elements in iterator");
            }
         }
         else
         {
            try
            {
               lazyNodeItetator = new LazyNodeIterator(storedNodes);
            }
            catch (RepositoryException e)
            {
               if (hasNext)
               {
                  readNextPage(skip);
               }
               else
               {
                  LOG.error("There are no more elements in iterator", e);
                  throw new NoSuchElementException(e.toString());
               }
            }
         }
      }
   }

   protected class LazyNodeIterator extends LazyItemsIterator implements NodeIterator
   {
      LazyNodeIterator(List<? extends ItemData> nodes) throws RepositoryException
      {
         super(nodes, null);
      }

      LazyNodeIterator(List<? extends ItemData> nodes, ItemDataFilter filter) throws RepositoryException
      {
         super(nodes, filter);
      }

      /**
       * {@inheritDoc}
       */
      public Node nextNode()
      {
         return (Node)nextItem();
      }
   }

   protected class LazyPropertyIterator extends LazyItemsIterator implements PropertyIterator
   {
      LazyPropertyIterator(List<? extends ItemData> props) throws RepositoryException
      {
         super(props, null);
      }

      LazyPropertyIterator(List<? extends ItemData> props, ItemDataFilter filter) throws RepositoryException
      {
         super(props, filter);
      }

      /**
       * {@inheritDoc}
       */
      public Property nextProperty()
      {
         return (Property)nextItem();
      }
   }

   @Override
   public String toString()
   {
      // toString can be invoked on invalid node, so need to ensure no exceptions thrown by skipping them 
      String primaryType;
      String mixinTypes;
      String acl;
      boolean locked;
      boolean checkedOut;
      // get primary type name
      try
      {
         primaryType = getPrimaryNodeType().getName();
      }
      catch (RepositoryException e)
      {
         primaryType = "Error requesting primary type";
      }
      // get mixins
      try
      {
         mixinTypes = Arrays.toString(getMixinTypeNames());
      }
      catch (RepositoryException e)
      {
         mixinTypes = "Error requesting mixin types";
      }
      // get ACL
      try
      {
         AccessControlList list = getACL();
         acl = list == null ? "-;" : list.dump();
         acl = acl.replaceAll("\\n", "; ");
      }
      catch (RepositoryException e)
      {
         acl = "Error requesting ACL";
      }
      // check if locked
      try
      {
         locked = isLocked();
      }
      catch (RepositoryException e)
      {
         // not valid node 
         locked = false;
      }
      // check if checkedOut
      try
      {
         checkedOut = isCheckedOut();
      }
      catch (RepositoryException e)
      {
         // not valid, or versioning not supported
         checkedOut = false;
      }
      return String
         .format(
            "Node {\n id: %s;\n path: %s;\n primary-type: %s;\n mixin-types: %s;\n acl: %s\n locked: %b;\n checked-out: %b \n}",
            data == null ? "not valid node" : data.getIdentifier(), qpath == null ? "undefined" : qpath.getAsString(),
            primaryType, mixinTypes, acl, locked, checkedOut);
   }
}
