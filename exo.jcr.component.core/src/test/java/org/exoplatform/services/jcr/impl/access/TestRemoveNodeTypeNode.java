/*
 * Copyright (C) 2010 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.access;

import org.exoplatform.services.jcr.BaseStandaloneTest;
import org.exoplatform.services.jcr.core.CredentialsImpl;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;

/**
 * @author <a href="anatoliy.bazko@exoplatform.org">Anatoliy Bazko</a>
 * @version $Id: TestRemoveSysteNode.java 111 2010-11-11 11:11:11Z tolusha $
 * 
 */
public class TestRemoveNodeTypeNode extends BaseStandaloneTest {

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getRepositoryName() {
    return null;
  }

  public void testRemove() throws Exception {
    Repository repository = repositoryService.getRepository("db1tck");
    Credentials credentials = new CredentialsImpl("demo", "exo".toCharArray());
    Session session = repository.login(credentials, "ws");

    Node node = session.getRootNode()
                       .getNode("jcr:system")
                       .getNode("jcr:nodetypes")
                       .getNode("nt:base");
    try {
      node.remove();
      session.save();

      fail("Exception should be thrown.");
    } catch (AccessDeniedException e) {
    }
  }
}
