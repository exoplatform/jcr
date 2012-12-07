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
package org.exoplatform.services.jcr.storage;

import java.util.Calendar;

import javax.jcr.RepositoryException;

import org.exoplatform.commons.utils.PrivilegedSystemHelper;

/**
 * Created by The eXo Platform SAS.<br>
 * 
 * Serves repository workspace persistent storage. Acts as factory for WorkspaceStorageConnection
 * objects, the implementation should support thread safety for openConnection() method;
 * 
 * @author Gennady Azarenkov
 * @version $Id: WorkspaceDataContainer.java 11907 2008-03-13 15:36:21Z ksm $
 */

public interface WorkspaceDataContainer extends DataContainer
{

   // configuration params
   public static final String TRIGGER_EVENTS_FOR_DESCENDENTS_ON_RENAME = "trigger-events-for-descendents-on-rename";

   public static final String TRIGGER_EVENTS_FOR_DESCENDANTS_ON_RENAME = "trigger-events-for-descendants-on-rename";

   public static final boolean TRIGGER_EVENTS_FOR_DESCENDANTS_ON_RENAME_DEFAULT = true;

   public static final String LAZY_NODE_ITERATOR_PAGE_SIZE = "lazy-node-iterator-page-size";

   public static final int LAZY_NODE_ITERATOR_PAGE_SIZE_DEFAULT = 100;

   public static final int LAZY_NODE_ITERATOR_PAGE_SIZE_MIN = 20;

   public final static String CONTAINER_NAME = "containerName";

   public final static String MAXBUFFERSIZE_PROP = "max-buffer-size";

   public final static String SWAPDIR_PROP = "swap-directory";

   public final static int DEF_MAXBUFFERSIZE = 1024 * 200; // 200k

   public final static String DEF_SWAPDIR = PrivilegedSystemHelper.getProperty("java.io.tmpdir");

   public final static String CHECK_SNS_NEW_CONNECTION = "check-sns-new-connection";

   public final static String ACL_BF_FALSE_PROPBABILITY = "acl-bloomfilter-false-positive-probability";

   public final static String ACL_BF_ELEMENTS_NUMBER = "acl-bloomfilter-elements-number";

   /**
    * [G.A] do we need it here or in WorkspaceDataManager better??
    * 
    * @return current time as for this container env
    */
   Calendar getCurrentTime();

   /**
    * isSame.
    *
    * @param another
    * @return
    */
   boolean isSame(WorkspaceDataContainer another);

   /**
    * @return the new connection to workspace storage normally implementation of this method should
    *         be synchronized
    */
   WorkspaceStorageConnection openConnection() throws RepositoryException;

   /**
    * Open connection and marked it as READ-ONLY if <code>readOnly</code> is true. <br/>
    * EXPERIMENTAL! Use it with care.
    * 
    * @param readOnly
    *          boolean, if true the Connection will be marked as READ-ONLY
    * 
    * @return the new connection to workspace storage normally implementation of this method should
    *         be synchronized
    */
   WorkspaceStorageConnection openConnection(boolean readOnly) throws RepositoryException;

   /**
     * @return the connection to workspace storage, if it possible the connection will use same
     *         physical resource (already obtained) as original connection, otherwise same behaviour
     *         will be used as for openConnection().
     * 
     *         normally implementation of this method should be synchronized
     */
   WorkspaceStorageConnection reuseConnection(WorkspaceStorageConnection original) throws RepositoryException;

   /**
    * @return the value of 'check-sns-new-connection' parameter 
    */
   boolean isCheckSNSNewConnection();

}
