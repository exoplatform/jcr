/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/ .
 */
package org.exoplatform.services.jcr.ext.index.persistent.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer.PortalContainerPostInitTask;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.index.persistent.api.JCRIndexingQueueDAO;

public class JCRIndexingServiceStartTest extends TestCase {

  public void testTheIndexingTasksAreScheduledWithNoIndexUpdateBeforeTheStart() {
    ExoContainerContext containerContext = mock(ExoContainerContext.class);
    when(containerContext.getContainer()).thenReturn(mock(PortalContainer.class));
    List<PortalContainerPostInitTask> initTasks = new ArrayList<>();
    JCRIndexingServiceImpl service = new JCRIndexingServiceImpl(containerContext,
                                                                mock(EntityManagerService.class),
                                                                mock(RepositoryService.class),
                                                                mock(JCRIndexingQueueDAO.class),
                                                                clusterNodeParams()) {
      @Override
      void addInitTask(PortalContainer portalContainer, PortalContainerPostInitTask task) {
        initTasks.add(task);
      }
    };

    service.start();

    assertEquals(1, initTasks.size());
  }

  public void testAnIndexingTaskKeepsItsScheduleAfterAnError() {
    Runnable task = JCRIndexingServiceImpl.keepingItsSchedule("test", () -> {
      throw new LinkageError("test");
    });

    task.run();
  }

  private static InitParams clusterNodeParams() {
    InitParams params = new InitParams();
    ValueParam nodeName = new ValueParam();
    nodeName.setName("cluster.node.name");
    nodeName.setValue("node1");
    params.addParameter(nodeName);
    return params;
  }
}
