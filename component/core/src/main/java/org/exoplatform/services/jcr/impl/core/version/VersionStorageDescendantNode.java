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
package org.exoplatform.services.jcr.impl.core.version;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: VersionStorageDescendantNode.java 11907 2008-03-13 15:36:21Z ksm $
 */

public abstract class VersionStorageDescendantNode
   extends NodeImpl
{

   // new impl
   public VersionStorageDescendantNode(NodeData data, SessionImpl session) throws PathNotFoundException,
            RepositoryException
   {

      super(data, session);
   }

   public Node addNode(String relPath, String nodeTypeName) throws ConstraintViolationException
   {
      throw new ConstraintViolationException("jcr:versionStorage is protected");
   }

   public Node addNode(String relPath) throws ConstraintViolationException
   {
      throw new ConstraintViolationException("jcr:versionStorage is protected");
   }

   public Property setProperty(String name, boolean value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException
   {
      throw new ConstraintViolationException("jcr:versionStorage is protected");
   }

   public Property setProperty(String name, Calendar value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException
   {
      throw new ConstraintViolationException("jcr:versionStorage is protected");
   }

   public Property setProperty(String name, double value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException
   {
      throw new ConstraintViolationException("jcr:versionStorage is protected");
   }

   public Property setProperty(String name, InputStream value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException
   {
      throw new ConstraintViolationException("jcr:versionStorage is protected");
   }

   public Property setProperty(String name, long value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException
   {
      throw new ConstraintViolationException("jcr:versionStorage is protected");
   }

   public Property setProperty(String name, Node value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException
   {
      throw new ConstraintViolationException("jcr:versionStorage is protected");
   }

   public Property setProperty(String name, String value, int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException
   {
      throw new ConstraintViolationException("jcr:versionStorage is protected");
   }

   public Property setProperty(String name, String value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException
   {
      throw new ConstraintViolationException("jcr:versionStorage is protected");
   }

   public Property setProperty(String name, String[] values, int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException
   {
      throw new ConstraintViolationException("jcr:versionStorage is protected");
   }

   public Property setProperty(String name, String[] values) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException
   {
      throw new ConstraintViolationException("jcr:versionStorage is protected");
   }

   public Property setProperty(String name, Value value, int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException
   {
      throw new ConstraintViolationException("jcr:versionStorage is protected");
   }

   public Property setProperty(String name, Value value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException
   {
      throw new ConstraintViolationException("jcr:versionStorage is protected");
   }

   public Property setProperty(String name, Value[] values, int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException
   {
      throw new ConstraintViolationException("jcr:versionStorage is protected");
   }

   public Property setProperty(String name, Value[] values) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException
   {
      throw new ConstraintViolationException("jcr:versionStorage is protected");
   }

   public void remove() throws RepositoryException, ConstraintViolationException, VersionException, LockException
   {
      throw new ConstraintViolationException("jcr:versionStorage is protected");
   }

   public void addMixin(String mixinName) throws ConstraintViolationException
   {
      throw new ConstraintViolationException("jcr:versionStorage is protected");
   }

   public boolean canAddMixin(String mixinName) throws RepositoryException
   {
      return false;
   }

   public void removeMixin(String mixinName) throws NoSuchNodeTypeException, ConstraintViolationException,
            RepositoryException
   {
      throw new ConstraintViolationException("jcr:versionStorage is protected");
   }

   public void restore(Version version, boolean removeExisting) throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException, RepositoryException, InvalidItemStateException
   {
      throw new UnsupportedRepositoryOperationException("jcr:versionStorage is protected");
   }

   public void restore(String versionName, boolean removeExisting) throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException, RepositoryException, InvalidItemStateException
   {
      throw new UnsupportedRepositoryOperationException("jcr:versionStorage is protected");
   }

   public void restore(Version version, String relPath, boolean removeExisting) throws VersionException,
            ItemExistsException, UnsupportedRepositoryOperationException, LockException, RepositoryException,
            InvalidItemStateException
   {
      throw new ConstraintViolationException("jcr:versionStorage is protected");
   }

   public void update(String srcWorkspaceName) throws NoSuchWorkspaceException, AccessDeniedException,
            InvalidItemStateException, LockException, RepositoryException
   {
      throw new ConstraintViolationException("jcr:versionStorage is protected");
   }

   public NodeIterator merge(String srcWorkspace, boolean bestEffort) throws UnsupportedRepositoryOperationException,
            NoSuchWorkspaceException, AccessDeniedException, MergeException, RepositoryException,
            InvalidItemStateException
   {
      throw new ConstraintViolationException("jcr:versionStorage is protected");
   }

   public void cancelMerge(Version version) throws VersionException, InvalidItemStateException,
            UnsupportedRepositoryOperationException, RepositoryException
   {
      throw new UnsupportedRepositoryOperationException("jcr:versionStorage is protected");
   }

   public void doneMerge(Version version) throws VersionException, InvalidItemStateException,
            UnsupportedRepositoryOperationException, RepositoryException
   {
      throw new UnsupportedRepositoryOperationException("jcr:versionStorage is protected");
   }

}
