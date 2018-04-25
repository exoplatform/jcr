/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.ext.audit;

import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.jcr.impl.ext.action.SessionActionCatalog;
import org.exoplatform.services.jcr.observation.ExtendedEvent;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

/**
 * Created by The eXo Platform SAS. <br>
 * Date: 12.05.2008 <br>
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: AuditVersionableTest.java 15151 2008-06-03 10:12:37Z ksm $
 */

public class AuditVersionableTest extends BaseStandaloneTest {

  private static final Log     log = ExoLogger.getLogger(AuditVersionableTest.class);

  private Node                 testRoot;

  private AuditService         service;

  private SessionActionCatalog catalog;

  private Session              adminSession;

  public void setUp() throws Exception {
    super.setUp();
    service = (AuditService) container.getComponentInstanceOfType(AuditService.class);
    catalog = (SessionActionCatalog) session.getContainer()
                                            .getComponentInstanceOfType(SessionActionCatalog.class);
    testRoot = root.addNode(AuditServiceTest.ROOT_PATH);
    root.save();
  }

  @Override
  protected void tearDown() throws Exception {
    testRoot.refresh(false);
    testRoot.remove();
    root.save();
    super.tearDown();
  }

  /**
   * Test if adding of mix:versionable to node doesn't add version information to the history.
   * 
   * @throws AccessDeniedException
   * @throws ItemExistsException
   * @throws ConstraintViolationException
   * @throws InvalidItemStateException
   * @throws ReferentialIntegrityException
   * @throws VersionException
   * @throws LockException
   * @throws NoSuchNodeTypeException
   * @throws RepositoryException
   */
  public void testAddMixVersionable() throws AccessDeniedException,
                                     ItemExistsException,
                                     ConstraintViolationException,
                                     InvalidItemStateException,
                                     ReferentialIntegrityException,
                                     VersionException,
                                     LockException,
                                     NoSuchNodeTypeException,
                                     RepositoryException {

    ExtendedNode node = (ExtendedNode) testRoot.addNode("deep");
    session.save();
    node.addMixin("mix:versionable");
    root.save();

    // check audit history
    AuditHistory ah = service.getHistory(node);
    for (AuditRecord ar : ah.getAuditRecords()) {
      String vuuid = ar.getVersion();
      assertNull("Version UUIDs should be null", vuuid);
    }
  }

  /**
   * Test if mix:versionable nodes history records has version related information. We assume that
   * AuditAction has configured for events addProperty,changeProperty,removeProperty,addMixin on
   * node /AuditServiceTest.
   * 
   * @throws AccessDeniedException
   * @throws ItemExistsException
   * @throws ConstraintViolationException
   * @throws InvalidItemStateException
   * @throws ReferentialIntegrityException
   * @throws VersionException
   * @throws LockException
   * @throws NoSuchNodeTypeException
   * @throws RepositoryException
   */
  public void testAddProperty() throws AccessDeniedException,
                               ItemExistsException,
                               ConstraintViolationException,
                               InvalidItemStateException,
                               ReferentialIntegrityException,
                               VersionException,
                               LockException,
                               NoSuchNodeTypeException,
                               RepositoryException {

    ExtendedNode node = (ExtendedNode) testRoot.addNode("deep");
    session.save();
    node.addMixin("mix:versionable");
    root.save();

    // ver.1
    node.checkin();
    final String v1UUID = node.getBaseVersion().getUUID();
    node.checkout();

    final String propName = "prop1";
    final InternalQName propQName = new InternalQName("", propName);

    node.setProperty(propName, "prop #1");
    root.save();

    // ver.2
    node.checkin();
    final String v2UUID = node.getBaseVersion().getUUID();
    node.getVersionHistory().addVersionLabel(node.getBaseVersion().getName(), "ver.1.1", false);
    node.checkout();

    node.setProperty(propName, "prop #1.1");
    // don't save now, but audit will contains records yet

    // check audit history
    AuditHistory ah = service.getHistory(node);
    for (AuditRecord ar : ah.getAuditRecords()) {
      if (ar.getEventType() == ExtendedEvent.PROPERTY_ADDED
          && ar.getPropertyName().equals(propQName)) {
        String vuuid = ar.getVersion();
        String vname = ar.getVersionName();
        assertEquals("Version UUIDs should be equals", v1UUID, vuuid);
      } else if (ar.getEventType() == ExtendedEvent.PROPERTY_CHANGED
          && ar.getPropertyName().equals(propQName)) {
        String vuuid = ar.getVersion();
        String vname = ar.getVersionName();
        assertEquals("Version UUIDs should be equals", v2UUID, vuuid);
      }
    }
  }

  /**
   * Test if versionable node's descendant nodes will have version records in audit history.
   * 
   * @throws Exception
   */
  public void testVersionableAncestor() throws Exception {
    ExtendedNode node = (ExtendedNode) testRoot.addNode("deep");
    session.save();
    node.addMixin("mix:versionable");
    root.save();

    final String propName = "prop1";
    final InternalQName propQName = new InternalQName("", propName);

    // ver.1
    node.checkin();
    final String v1UUID = node.getBaseVersion().getUUID();
    node.checkout();

    // child node, non versionable but under the version control within their parent
    Node node1 = node.addNode("node1");
    // node1.addMixin("exo:auditable");
    node.save();
    node1.setProperty(propName, "prop #1"); // add property
    node1.save();

    // ver.2
    node.checkin();
    final String v2UUID = node.getBaseVersion().getUUID();
    node.checkout();

    // check node1 and v.1
    AuditHistory ah = service.getHistory(node1);
    for (AuditRecord ar : ah.getAuditRecords()) {
      String vuuid = ar.getVersion();
      // Version av = (Version) session.getNodeByUUID(vuuid);
      String vname = ar.getVersionName();
      if (ar.getEventType() == ExtendedEvent.PROPERTY_ADDED
          && ar.getPropertyName().equals(propQName)) {
        assertEquals("Version UUIDs should be equals", v1UUID, vuuid);
      }
    }

    // subnode, non versionable but under the version control within versionable ancestor
    Node node2 = node1.addNode("node2");
    // node2.addMixin("exo:auditable");
    node1.save();
    node2.setProperty(propName, "prop #2"); // add property
    node2.save();

    // ver.3
    node.checkin();
    node.checkout();

    // check node2 and v.2
    ah = service.getHistory(node2);
    for (AuditRecord ar : ah.getAuditRecords()) {
      String vuuid = ar.getVersion();
      String vname = ar.getVersionName();
      if (ar.getEventType() == ExtendedEvent.PROPERTY_ADDED
          && ar.getPropertyName().equals(propQName)) {
        assertEquals("Version UUIDs should be equals", v2UUID, vuuid);
      }
    }

    // check if node1 still has v.1 in history
    ah = service.getHistory(node1);
    for (AuditRecord ar : ah.getAuditRecords()) {
      String vuuid = ar.getVersion();
      String vname = ar.getVersionName();
      if (ar.getEventType() == ExtendedEvent.PROPERTY_ADDED
          && ar.getPropertyName().equals(propQName)) {
        assertEquals("Version UUIDs should be equals", v1UUID, vuuid);
      }
    }
  }
}
