/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.ext.audit;

import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.util.TraversingItemVisitor;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class RemoveAuditableVisitor extends TraversingItemVisitor {
  /**
   * Class logger.
   */
  private final AuditService auditService;

  private final static Log LOG = ExoLogger.getLogger("exo-jcr-services.RemoveAuditableVisitor");

  public RemoveAuditableVisitor(AuditService auditService) {
    super();
    this.auditService = auditService;
  }

  @Override
  protected void entering(Node node, int arg1) throws RepositoryException {

    if (((NodeImpl) node).isNodeType(AuditService.EXO_AUDITABLE)) {
      if (auditService.hasHistory(node)) {
        auditService.removeHistory(node);
        if (LOG.isDebugEnabled()) {
          LOG.debug("History removed for " + node.getPath());
        }
      }
    }
  }

  @Override
  protected void entering(Property arg0, int arg1) throws RepositoryException {
  }

  @Override
  protected void leaving(Node arg0, int arg1) throws RepositoryException {

  }

  @Override
  protected void leaving(Property arg0, int arg1) throws RepositoryException {
  }
}
