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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import org.mockito.ArgumentCaptor;

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
    PortalContainer portalContainer = mock(PortalContainer.class);
    when(containerContext.getContainer()).thenReturn(portalContainer);
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

  public void testTheStartSchedulesThreeTasksThatKeepTheirScheduleAfterAnError() throws Exception {
    ExoContainerContext containerContext = mock(ExoContainerContext.class);
    PortalContainer portalContainer = mock(PortalContainer.class);
    when(containerContext.getContainer()).thenReturn(portalContainer);
    EntityManagerService entityManagerService = mock(EntityManagerService.class);
    // The queue consumer catches an Exception only: an Error leaves its body, once
    doThrow(new LinkageError("consumer")).doNothing().when(entityManagerService).startRequest(any());
    RepositoryService repositoryService = mock(RepositoryService.class);
    // At the start, no repository: no index directory. In the persister's run, an Error leaves its body
    when(repositoryService.getCurrentRepository()).thenReturn(null).thenThrow(new LinkageError("persister"));
    ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
    List<PortalContainerPostInitTask> initTasks = new ArrayList<>();
    JCRIndexingServiceImpl service = new JCRIndexingServiceImpl(containerContext,
                                                                entityManagerService,
                                                                repositoryService,
                                                                mock(JCRIndexingQueueDAO.class),
                                                                clusterNodeParams()) {
      @Override
      void addInitTask(PortalContainer container, PortalContainerPostInitTask task) {
        initTasks.add(task);
      }

      @Override
      void scheduleKeepingItsSchedule(ScheduledExecutorService ignored,
                                      String name,
                                      Runnable task,
                                      long periodInSeconds) {
        super.scheduleKeepingItsSchedule(executor, name, task, periodInSeconds);
      }
    };
    service.start();
    assertEquals(1, initTasks.size());

    initTasks.get(0).execute(null, portalContainer);

    ArgumentCaptor<Runnable> scheduled = ArgumentCaptor.forClass(Runnable.class);
    verify(executor, times(3)).scheduleAtFixedRate(scheduled.capture(), eq(0L), anyLong(), eq(TimeUnit.SECONDS));
    // A throwable leaving a task would cancel its later runs: each scheduled task returns instead
    for (Runnable task : scheduled.getAllValues()) {
      task.run();
    }

    // The consumer's failed run left no running flag behind: its next run starts a request again
    clearInvocations(entityManagerService);
    scheduled.getAllValues().get(0).run();
    verify(entityManagerService).startRequest(any());
  }

  public void testAnIndexingTaskKeepsItsScheduleAfterAnError() {
    AtomicBoolean ran = new AtomicBoolean();
    Runnable task = JCRIndexingServiceImpl.keepingItsSchedule("test", () -> {
      ran.set(true);
      throw new LinkageError("test");
    });

    // A throwable leaving the task would cancel its later runs: the wrapper returns normally instead
    task.run();

    assertTrue("The wrapped task must have run", ran.get());
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
