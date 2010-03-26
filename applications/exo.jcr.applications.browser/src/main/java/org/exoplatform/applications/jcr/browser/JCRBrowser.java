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
package org.exoplatform.applications.jcr.browser;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS. <br/>
 * 
 * Date: 27.05.2008 <br/>
 * 
 * JavaBean for JCRBrowser sample application.<br/>
 * 
 * Since JCR 1.11 Browser supports Ext Repository synchronization.<br/>
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: JCRBrowser.java 111 2008-11-11 11:11:11Z peterit $
 */
public class JCRBrowser
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.applications.browser.JCRBrowser");

   protected RepositoryService repositoryService;

   protected ManageableRepository repository;

   protected Session session;

   protected Node node;

   protected List<String> errors = new ArrayList<String>();

   /**
    * Get browser repository.
    * 
    * @return the repository
    */
   public ManageableRepository getRepository()
   {
      return repository;
   }

   /**
    * Set browser repository.
    * 
    * @param repository
    *          the repository to set
    */
   public void setRepository(ManageableRepository repository)
   {
      this.repository = repository;
   }

   /**
    * Get browser JCR session.
    * 
    * @return the session
    */
   public Session getSession()
   {
      return session;
   }

   /**
    * Set browser JCR session.
    * 
    * @param session
    *          the session to set
    * @throws RepositoryException
    */
   public void setSession(Session session) throws RepositoryException
   {
      this.session = session;
      this.node = this.session.getRootNode();
   }

   /**
    * Get browser current node.
    * 
    * @return the node
    */
   public Node getNode()
   {
      return node;
   }

   /**
    * Set browser current node.
    * 
    * @param node
    *          the node to set
    */
   public void setNode(Node node)
   {
      this.node = node;
   }

   public void addError(Throwable error)
   {
      this.errors.add(error.toString());
   }

   public boolean isErrorsFound()
   {
      return this.errors.size() > 0;
   }

   public String[] getErrorsAndClean()
   {
      try
      {
         String[] errs = new String[this.errors.size()];
         this.errors.toArray(errs);
         return errs;
      }
      finally
      {
         this.errors.clear();
      }
   }

   /**
    * @return the repositoryService
    */
   public RepositoryService getRepositoryService()
   {
      return repositoryService;
   }

   /**
    * @param repositoryService
    *          the repositoryService to set
    */
   public void setRepositoryService(RepositoryService repositoryService)
   {
      this.repositoryService = repositoryService;
   }

   /**
    * Tell if Asynchronous Replication service available.
    *
    * @return boolean
    */
   public boolean isAsynchronousReplicationPresent()
   {
      //return getAsynchronousReplication() != null;
      return false;
   }

   /**
    * Tell if Asynchronous Replication service available.
    *
    * @return boolean
    */
   public boolean isAsynchronousReplicationActive()
   {
      return false;
   }

   /**
    * Runs synchronization process if service is present.
    *
    */
   public void runSynchronization()
   {
   }

}
