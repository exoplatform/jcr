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

import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.Constants;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Gennady Azarenkov
 * @version $Id: AuditService.java 12164 2007-01-22 08:39:22Z geaz $
 */

public interface AuditService {
  public static final String        AUDIT_STORAGE_ID                 = "00exo0jcr0audit0storage0id000000";

  public static final InternalQName EXO_AUDIT                        = new InternalQName(Constants.NS_EXO_URI,
                                                                                         "audit");

  public static final InternalQName EXO_AUDITABLE                    = new InternalQName(Constants.NS_EXO_URI,
                                                                                         "auditable");

  public static final InternalQName EXO_AUDITSTORAGE                 = new InternalQName(Constants.NS_EXO_URI,
                                                                                         "auditStorage");

  public static final InternalQName EXO_AUDITRECORD                  = new InternalQName(Constants.NS_EXO_URI,
                                                                                         "auditRecord");

  public static final InternalQName EXO_AUDITRECORD_USER             = new InternalQName(Constants.NS_EXO_URI,
                                                                                         "user");

  public static final InternalQName EXO_AUDITRECORD_CREATED          = new InternalQName(Constants.NS_EXO_URI,
                                                                                         "created");

  public static final InternalQName EXO_AUDITRECORD_EVENTTYPE        = new InternalQName(Constants.NS_EXO_URI,
                                                                                         "eventType");

  public static final InternalQName EXO_AUDITRECORD_PROPERTYNAME     = new InternalQName(Constants.NS_EXO_URI,
                                                                                         "propertyName");

  public static final InternalQName EXO_AUDITRECORD_AUDITVERSION     = new InternalQName(Constants.NS_EXO_URI,
                                                                                         "auditVersion");

  public static final InternalQName EXO_AUDITRECORD_AUDITVERSIONNAME = new InternalQName(Constants.NS_EXO_URI,
                                                                                         "auditVersionName");

  public static final InternalQName EXO_AUDITRECORD_OLDVALUE         = new InternalQName(Constants.NS_EXO_URI,
                                                                                         "oldValue");

  public static final InternalQName EXO_AUDITRECORD_NEWVALUE         = new InternalQName(Constants.NS_EXO_URI,
                                                                                         "newValue");

  public static final InternalQName EXO_AUDITHISTORY                 = new InternalQName(Constants.NS_EXO_URI,
                                                                                         "auditHistory");

  public static final InternalQName EXO_AUDITHISTORY_TARGETNODE      = new InternalQName(Constants.NS_EXO_URI,
                                                                                         "targetNode");

  public static final InternalQName EXO_AUDITHISTORY_LASTRECORD      = new InternalQName(Constants.NS_EXO_URI,
                                                                                         "lastRecord");

  /**
   * Creates audit history for given node. Throws an exception, if history already present.
   * 
   * @param node
   * @throws RepositoryException
   */
  void createHistory(Node node) throws RepositoryException;

  /**
   * Deletes audit history.
   * 
   * @param node
   * @throws RepositoryException
   */
  void removeHistory(Node node) throws RepositoryException;

  /**
   * Adds new audit record.
   * 
   * @param previousItem
   * @param currentItem
   * @param eventType
   * @throws RepositoryException
   */
   void addRecord(Item previousItem, Item currentItem, int eventType) throws RepositoryException;

  /**
   * Get node audit history.
   * 
   * @param node
   * @return audit history of this item
   * @throws RepositoryException
   * @throws UnsupportedOperationException if item(parent) is not auditable
   */
  AuditHistory getHistory(Node node) throws RepositoryException, UnsupportedOperationException;

  /**
   * Check if node has audit history.
   * 
   * @param node
   * @return true if audit history for this item exists
   */
  boolean hasHistory(Node node);
}
