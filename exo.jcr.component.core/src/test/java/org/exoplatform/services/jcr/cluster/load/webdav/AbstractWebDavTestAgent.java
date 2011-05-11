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
package org.exoplatform.services.jcr.cluster.load.webdav;

import org.exoplatform.services.jcr.cluster.JCRWebdavConnection;
import org.exoplatform.services.jcr.cluster.load.AbstractTestAgent;
import org.exoplatform.services.jcr.cluster.load.NodeInfo;
import org.exoplatform.services.jcr.cluster.load.ResultCollector;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
public abstract class AbstractWebDavTestAgent extends AbstractTestAgent
{
   /**
    * WebDav realm
    */
   private static final String WEBDAV_REALM = "eXo REST services";

   /**
    * WebDav path
    */
   private static final String WEBDAV_DEFAULT_PATH = "/rest/jcr/repository/production/";

   /**
    * @param nodesPath
    * @param responceResults
    * @param startSignal
    * @param READ_VALUE
    * @param random
    */
   public AbstractWebDavTestAgent(List<NodeInfo> nodesPath, ResultCollector resultCollector,
      CountDownLatch startSignal, int READ_VALUE, Random random, boolean isReadThread)
   {
      super(nodesPath, resultCollector, startSignal, READ_VALUE, random, isReadThread);
   }

   protected JCRWebdavConnection getNewConnection()
   {
      return new JCRWebdavConnection("192.168.0.129", 80, "root", "exo", WEBDAV_REALM, WEBDAV_DEFAULT_PATH);
   }
}
