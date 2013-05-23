/*
 * Copyright (C) 2011 eXo Platform SAS.
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

import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.SessionLifecycleListener;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import javax.transaction.xa.XAResource;

/**
* @author Nikolay Zamosenchuk
* @version $Id: DummySession.xml 34360 2009-07-22 23:58:59Z nzamosenchuk $
*
*/
public class DummySession implements ExtendedSession
{
   private String sessionId;

   public DummySession(String sessionId)
   {
      this.sessionId = sessionId;
   }

   public String getId()
   {
      return sessionId;
   }

   // do nothing   
   public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse,
      boolean exportChildVersionHisotry) throws IOException, PathNotFoundException, RepositoryException
   {
   }

   public LocationFactory getLocationFactory()
   {
      return null;
   }

   public Node getNodeByIdentifier(String identifier) throws ItemNotFoundException, RepositoryException
   {
      return null;
   }

   public long getTimeout()
   {
      return 0;
   }

   public XAResource getXAResource()
   {
      return null;
   }

   public boolean hasExpired()
   {
      return false;
   }

   public void importXML(String parentAbsPath, InputStream in, int uuidBehavior, Map<String, Object> context)
      throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException,
      InvalidSerializedDataException, RepositoryException
   {
   }

   public void registerLifecycleListener(SessionLifecycleListener listener)
   {
   }

   public void setTimeout(long timeout)
   {
   }

   public void addLockToken(String lt)
   {
   }

   public void checkPermission(String absPath, String actions) throws AccessControlException, RepositoryException
   {
   }

   public void exportDocumentView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse)
      throws PathNotFoundException, SAXException, RepositoryException
   {
   }

   public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse)
      throws IOException, PathNotFoundException, RepositoryException
   {
   }

   public void exportSystemView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse)
      throws PathNotFoundException, SAXException, RepositoryException
   {
   }

   public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse)
      throws IOException, PathNotFoundException, RepositoryException
   {
   }

   public Object getAttribute(String name)
   {
      return null;
   }

   public String[] getAttributeNames()
   {
      return null;
   }

   public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) throws PathNotFoundException,
      ConstraintViolationException, VersionException, LockException, RepositoryException
   {
      return null;
   }

   public Item getItem(String absPath) throws PathNotFoundException, RepositoryException
   {
      return null;
   }

   public String[] getLockTokens()
   {
      return null;
   }

   public String getNamespacePrefix(String uri) throws NamespaceException, RepositoryException
   {
      return null;
   }

   public String[] getNamespacePrefixes() throws RepositoryException
   {
      return null;
   }

   public String getNamespaceURI(String prefix) throws NamespaceException, RepositoryException
   {
      return null;
   }

   public Node getNodeByUUID(String uuid) throws ItemNotFoundException, RepositoryException
   {
      return null;
   }

   public Repository getRepository()
   {
      return null;
   }

   public Node getRootNode() throws RepositoryException
   {
      return null;
   }

   public String getUserID()
   {
      return null;
   }

   public ValueFactory getValueFactory() throws UnsupportedRepositoryOperationException, RepositoryException
   {
      return null;
   }

   public Workspace getWorkspace()
   {
      return null;
   }

   public boolean hasPendingChanges() throws RepositoryException
   {
      return false;
   }

   public Session impersonate(Credentials credentials) throws LoginException, RepositoryException
   {
      return null;
   }

   public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws IOException,
      PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException,
      InvalidSerializedDataException, LockException, RepositoryException
   {
   }

   public boolean isLive()
   {
      return false;
   }

   public boolean itemExists(String absPath) throws RepositoryException
   {
      return false;
   }

   public void logout()
   {
   }

   public void move(String srcAbsPath, String destAbsPath) throws ItemExistsException, PathNotFoundException,
      VersionException, ConstraintViolationException, LockException, RepositoryException
   {
   }

   public void refresh(boolean keepChanges) throws RepositoryException
   {
   }

   public void removeLockToken(String lt)
   {
   }

   public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException,
      InvalidItemStateException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException
   {
   }

   public void setNamespacePrefix(String prefix, String uri) throws NamespaceException, RepositoryException
   {
   }

   public void move(String srcAbsPath, String destAbsPath, boolean triggerEventsForDescendants)
      throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException,
      RepositoryException
   {

   }
}
