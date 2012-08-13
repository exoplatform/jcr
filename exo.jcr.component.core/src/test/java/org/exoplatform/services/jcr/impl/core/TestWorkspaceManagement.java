/*
 * CopSyright (C) 2009 eXo Platform SAS.
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

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.config.CacheEntry;
import org.exoplatform.services.jcr.config.ContainerEntry;
import org.exoplatform.services.jcr.config.PropertiesParser;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.SystemParametersPersistenceConfigurator;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.jcr.util.SystemParametersTestConfigurationHelper;
import org.exoplatform.services.jcr.util.TesterConfigurationHelper;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: TestWorkspaceManagement.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestWorkspaceManagement extends JcrImplBaseTest
{
   private final TesterConfigurationHelper helper;

   private WorkspaceEntry wsEntry;

   public TestWorkspaceManagement()
   {
      super();
      this.helper = TesterConfigurationHelper.getInstance();
   }

   // single db test only
   public void testAddWorkspaceWithNewDS() throws Exception
   {
      ManageableRepository repository = null;
      try
      {
         repository = helper.createRepository(container, DatabaseStructureType.SINGLE, null);

         try
         {
            WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, "not-existed-ds");
            helper.addWorkspace(repository, wsEntry);
            fail();
         }
         catch (Exception e)
         {
            // ok;
         }
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   public void testAddWorkspaceWithExistingName() throws RepositoryConfigurationException, Exception
   {
      ManageableRepository repository = null;
      try
      {
         String dsName = helper.createDatasource();
         repository = helper.createRepository(container, DatabaseStructureType.SINGLE, dsName);

         try
         {
            WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, dsName);
            wsEntry.setName(repository.getConfiguration().getSystemWorkspaceName());

            helper.addWorkspace(repository, wsEntry);
            fail();
         }
         catch (RepositoryConfigurationException e)
         {
            // ok;
         }
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   public void testAddWorkspaceWithIvalidVs() throws RepositoryConfigurationException, Exception
   {
      ManageableRepository repository = null;
      try
      {
         String dsName = helper.createDatasource();
         repository = helper.createRepository(container, DatabaseStructureType.SINGLE, dsName);

         try
         {
            WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, dsName);

            ValueStorageEntry valueStorageEntry = wsEntry.getContainer().getValueStorages().get(0);

            ArrayList<SimpleParameterEntry> spe = new ArrayList<SimpleParameterEntry>();
            spe.add(new SimpleParameterEntry("path", "/unknown/path"));
            valueStorageEntry.setParameters(spe);

            wsEntry.getContainer().getValueStorages().set(0, valueStorageEntry);

            helper.addWorkspace(repository, wsEntry);
         }
         catch (RepositoryConfigurationException e)
         {
            // ok;
         }
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   public void testCreateWsNoConfig() throws RepositoryConfigurationException, Exception
   {
      ManageableRepository repository = null;
      try
      {
         String dsName = helper.createDatasource();
         repository = helper.createRepository(container, DatabaseStructureType.SINGLE, dsName);

         try
         {
            WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, dsName);
            wsEntry.setContainer(new ContainerEntry(
               "org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer", new ArrayList()));

            helper.addWorkspace(repository, wsEntry);
            fail();
         }
         catch (Exception e)
         {
            // ok;
         }
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   public void testInitNewWS() throws RepositoryConfigurationException, Exception
   {
      ManageableRepository repository = null;
      try
      {
         String dsName = helper.createDatasource();
         repository = helper.createRepository(container, DatabaseStructureType.SINGLE, dsName);

         SessionImpl session = null;
         try
         {
            WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, dsName);
            helper.addWorkspace(repository, wsEntry);

            session = (SessionImpl)repository.login(credentials, wsEntry.getName());
            assertNotNull(session.getRootNode());
         }
         catch (RepositoryException e)
         {
            e.printStackTrace();
            fail();
         }
         finally
         {
            session.logout();
         }
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   public void testMixMultiAndSingleDbWs() throws RepositoryConfigurationException, Exception
   {
      ManageableRepository repository = null;
      try
      {
         String dsName = helper.createDatasource();
         repository = helper.createRepository(container, DatabaseStructureType.SINGLE, dsName);

         try
         {
            WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, dsName);
            helper.addWorkspace(repository, wsEntry);
            fail();
         }
         catch (Exception e)
         {
            // ok;
         }
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   public void testRemoveSystemWorkspace() throws Exception
   {
      ManageableRepository repository = null;
      try
      {
         String dsName = helper.createDatasource();
         repository = helper.createRepository(container, DatabaseStructureType.SINGLE, dsName);

         try
         {
            repository.removeWorkspace(repository.getConfiguration().getSystemWorkspaceName());
            fail();
         }
         catch (RepositoryException e)
         {
         }
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   public void testRemoveWorkspace() throws Exception
   {
      ManageableRepository repository = null;
      try
      {
         String dsName = helper.createDatasource();
         repository = helper.createRepository(container, DatabaseStructureType.SINGLE, dsName);
         WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, dsName);

         helper.addWorkspace(repository, wsEntry);
         assertEquals(2, repository.getWorkspaceNames().length);

         repository.removeWorkspace(wsEntry.getName());
         assertEquals(1, repository.getWorkspaceNames().length);
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   public void testSystemParameters() throws Exception
   {
      checkForcedParameters();
      checkXmlParameters();
      checkDefaultParameters();
   }

   public void checkForcedParameters() throws Exception
   {
      testForcedOrDefaultParameters(true);
   }

   public void checkXmlParameters() throws Exception
   {
      ManageableRepository manageableRepository = null;

      try
      {
         manageableRepository = helper.createRepository(container, DatabaseStructureType.SINGLE, false, false);

         String repositoryName = manageableRepository.getConfiguration().getName();
         String workspacesUniqueName =
            manageableRepository.getConfiguration().getWorkspaceEntries().get(0).getUniqueName();
         String baseParameterName = "test-parameter" + IdGenerator.generate();
         CacheEntry cacheEntry = manageableRepository.getConfiguration().getWorkspaceEntries().get(0).getCache();

         List<String> systemPropertyPrefixes =
            SystemParametersTestConfigurationHelper.generateDefaultPropertiesPrefixes(repositoryName,
               workspacesUniqueName);

         List<String> systemProperties =
            SystemParametersTestConfigurationHelper.generateForcedAndDefaultProperties(systemPropertyPrefixes,
               baseParameterName);

         manageableRepository.getConfiguration().getWorkspaceEntries().get(0).getCache()
            .putParameterValue(baseParameterName, "correctValue");

         for (int i = 0; i < systemProperties.size(); i++)
         {
            System.setProperty(systemProperties.get(i), String.valueOf(i));
         }

         String parameterValue = cacheEntry.getParameterValue(baseParameterName);

         for (String propertyName : systemProperties)
         {
            System.clearProperty(propertyName);
         }

         assertEquals("correctValue", parameterValue);

      }
      finally
      {
         if (manageableRepository != null)
         {
            helper.removeRepository(container, manageableRepository.getConfiguration().getName());
         }
      }
   }

   public void checkDefaultParameters() throws Exception
   {
      testForcedOrDefaultParameters(false);
   }

   private void testForcedOrDefaultParameters(boolean forced) throws Exception
   {
      ManageableRepository manageableRepository = null;

      try
      {
         manageableRepository = helper.createRepository(container, DatabaseStructureType.SINGLE, false, false);
         testProperties("test-parameter" + IdGenerator.generate(), manageableRepository, forced);
      }
      finally
      {
         if (manageableRepository != null)
         {
            helper.removeRepository(container, manageableRepository.getConfiguration().getName());
         }
      }
   }

   private void testProperties(String baseParameterName, ManageableRepository manageableRepository, boolean forced)
      throws Exception
   {

      String parameterValue;
      String currentIterationParameterName;
      String repositoryName = manageableRepository.getConfiguration().getName();
      for (int j = 0; j < 3; j++)
      {
         WorkspaceEntry workspaceEntry = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, null);
         CacheEntry cacheEntry = workspaceEntry.getCache();
         String workspacesUniqueName = repositoryName + "_" + workspaceEntry.getName();

         List<String> systemPropertyPrefixes =
            forced ? SystemParametersTestConfigurationHelper.generateForcedAndDefaultPropertiesPrefixes(repositoryName,
               workspacesUniqueName) : SystemParametersTestConfigurationHelper.generateDefaultPropertiesPrefixes(
               repositoryName, workspacesUniqueName);

         currentIterationParameterName = baseParameterName + j;
         List<String> systemProperties =
            SystemParametersTestConfigurationHelper.generateForcedAndDefaultProperties(systemPropertyPrefixes,
               currentIterationParameterName);

         if (forced)
         {
            cacheEntry.putParameterValue(currentIterationParameterName, "wrongValue");
         }

         for (int i = 0; i < systemProperties.size(); i++)
         {
            setProperty(systemProperties.get(i), String.valueOf(i));
         }

         helper.addWorkspace(manageableRepository, workspaceEntry);

         parameterValue = cacheEntry.getParameterValue(currentIterationParameterName);
         assertEquals("0", parameterValue);

         if (forced)
         {
            cacheEntry.putParameterValue(currentIterationParameterName, "wrongValue");
            parameterValue = cacheEntry.getParameterValue(currentIterationParameterName);
            assertEquals("0", parameterValue);
         }

         for (String propertyName : systemProperties)
         {
            clearProperty(propertyName);
         }
         systemPropertyPrefixes.remove(0);
      }
   }

   public void testParameterPersistanceConfigurator() throws Exception
   {
      ManageableRepository manageableRepository = null;
      try
      {
         manageableRepository = helper.createRepository(container, DatabaseStructureType.SINGLE, false, false);
         CacheEntry cacheEntry = manageableRepository.getConfiguration().getWorkspaceEntries().get(0).getCache();

         testUnmodifiableParameter(manageableRepository, cacheEntry);
         testBeforeInitializeParameterIsSetCorrectly(manageableRepository);
         testBeforeInitializeParameterIsNotSetCorrectly(manageableRepository);
         testBeforeInitializeParameterExceptionIsThrownCorrectly(manageableRepository);
      }
      finally
      {
         if (manageableRepository != null)
         {
            helper.removeRepository(container, manageableRepository.getConfiguration().getName());
         }
      }
   }

   private void testBeforeInitializeParameterIsNotSetCorrectly(ManageableRepository manageableRepository)
      throws Exception, RepositoryConfigurationException, RepositoryException
   {
      String correctParameterValue = "correct-parameter-value";
      String wrongParameterValue = "wrong-parameter-value";
      String parameterName = "test-parameter-II";

      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, null);

      wsEntry.getCache().putParameterValue(parameterName, correctParameterValue);
      assertEquals(correctParameterValue, wsEntry.getCache().getParameterValue(parameterName));

      helper.addWorkspace(manageableRepository, wsEntry);

      assertEquals(correctParameterValue, manageableRepository.getConfiguration().getWorkspaceEntries().get(2)
         .getCache().getParameterValue(parameterName));

      setProperty(PropertiesParser.EXO_JCR_CONFIG + PropertiesParser.FORCE_TYPE + PropertiesParser.WORKSPACE_SCOPE
         + wsEntry.getUniqueName() + "." + "cache" + "." + parameterName, wrongParameterValue);

      assertEquals(correctParameterValue, manageableRepository.getConfiguration().getWorkspaceEntries().get(2)
         .getCache().getParameterValue(parameterName));

      clearProperty(PropertiesParser.EXO_JCR_CONFIG + PropertiesParser.FORCE_TYPE + PropertiesParser.WORKSPACE_SCOPE
         + wsEntry.getUniqueName() + "." + "cache" + "." + parameterName);

   }

   private void testBeforeInitializeParameterExceptionIsThrownCorrectly(ManageableRepository manageableRepository)
      throws Exception, RepositoryConfigurationException, RepositoryException
   {
      String wrongParameterValue = "wrong-parameter-value";
      String parameterName = "enabled";

      WorkspaceEntry workspaceEntry = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, null);

      helper.addWorkspace(manageableRepository, workspaceEntry);

      setProperty(PropertiesParser.EXO_JCR_CONFIG + PropertiesParser.FORCE_TYPE + PropertiesParser.WORKSPACE_SCOPE
         + workspaceEntry.getUniqueName() + "." + "value-storage" + "." + parameterName, wrongParameterValue);

      manageableRepository.removeWorkspace(workspaceEntry.getName());

      try
      {
         helper.addWorkspace(manageableRepository, workspaceEntry);
         fail("Exception should be thrown, because 'before-initialize' parameter is initialized after initialization of workspace");
      }
      catch (Exception e)
      {
         // ok
      }

      manageableRepository.removeWorkspace(workspaceEntry.getName());

      ArrayList<SimpleParameterEntry> newParameters = new ArrayList<SimpleParameterEntry>();
      newParameters.add(new SimpleParameterEntry(parameterName, "false"));

      workspaceEntry.getContainer().getValueStorages().get(0).setParameters(newParameters);

      setProperty(PropertiesParser.EXO_JCR_CONFIG + PropertiesParser.FORCE_TYPE + PropertiesParser.WORKSPACE_SCOPE
         + workspaceEntry.getUniqueName() + "." + "value-storage" + "." + parameterName, "false");

      try
      {
         helper.addWorkspace(manageableRepository, workspaceEntry);
      }
      catch (Exception e)
      {
         fail("Exception should not be thrown, because 'before-initialize' parameter is initialized before initialization of workspace");
      }

      clearProperty(PropertiesParser.EXO_JCR_CONFIG + PropertiesParser.FORCE_TYPE + PropertiesParser.WORKSPACE_SCOPE
         + workspaceEntry.getUniqueName() + "." + "value-storage" + "." + parameterName);
   }

   private void testBeforeInitializeParameterIsSetCorrectly(ManageableRepository manageableRepository)
      throws Exception, RepositoryConfigurationException, RepositoryException
   {
      String correctParameterValue = "correct-parameter-value";
      String parameterName = "enabled";

      WorkspaceEntry workspaceEntry = helper.createWorkspaceEntry(DatabaseStructureType.SINGLE, null);

      setProperty(PropertiesParser.EXO_JCR_CONFIG + PropertiesParser.FORCE_TYPE + PropertiesParser.WORKSPACE_SCOPE
         + manageableRepository.getConfiguration().getName() + "_" + workspaceEntry.getName() + "." + "value-storage"
         + "." + parameterName, correctParameterValue);

      helper.addWorkspace(manageableRepository, workspaceEntry);

      String parameterValueAfterWorkspaceIsInitialized =
         manageableRepository.getConfiguration().getWorkspaceEntries().get(1).getContainer().getValueStorages().get(0)
            .getParameterValue(parameterName);

      assertEquals(correctParameterValue, parameterValueAfterWorkspaceIsInitialized);

      clearProperty(PropertiesParser.EXO_JCR_CONFIG + PropertiesParser.FORCE_TYPE + PropertiesParser.WORKSPACE_SCOPE
         + workspaceEntry.getUniqueName() + "." + "cache" + "." + parameterName);

   }

   private void testUnmodifiableParameter(ManageableRepository manageableRepository, CacheEntry cacheEntry)
      throws RepositoryConfigurationException
   {
      String originalParameterValue = "original-parameter-value";
      String wrongParameterValue = "wrong-parameter-value";
      String parameterName = "test-parameter-I";
      String wsUniqueName = manageableRepository.getConfiguration().getWorkspaceEntries().get(0).getUniqueName();

      cacheEntry.putParameterValue(parameterName, originalParameterValue);

      setProperty(PropertiesParser.EXO_JCR_CONFIG + PropertiesParser.FORCE_TYPE + PropertiesParser.WORKSPACE_SCOPE
         + wsUniqueName + "." + "cache" + "." + parameterName, wrongParameterValue);

      String parameterValueAfterSystemPropertyIsSet = cacheEntry.getParameterValue(parameterName);

      clearProperty(PropertiesParser.EXO_JCR_CONFIG + PropertiesParser.FORCE_TYPE + PropertiesParser.WORKSPACE_SCOPE
         + wsUniqueName + "." + "cache" + "." + parameterName);

      assertEquals(originalParameterValue, parameterValueAfterSystemPropertyIsSet);
   }

   private void setProperty(String propertyName, String propertyValue)
   {
      ((SystemParametersPersistenceConfigurator)container
         .getComponentInstanceOfType(SystemParametersPersistenceConfigurator.class)).getSystemProperties().put(
         propertyName, propertyValue);
   }

   private void clearProperty(String propertyName)
   {
      ((SystemParametersPersistenceConfigurator)container
         .getComponentInstanceOfType(SystemParametersPersistenceConfigurator.class)).getSystemProperties().remove(
         propertyName);
   }
}
