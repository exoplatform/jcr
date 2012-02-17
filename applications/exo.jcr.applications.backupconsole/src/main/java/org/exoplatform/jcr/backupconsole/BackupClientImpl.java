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
package org.exoplatform.jcr.backupconsole;

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.RepositoryServiceConfiguration;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.ext.backup.BackupJob;
import org.exoplatform.services.jcr.ext.backup.BackupManager;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChain;
import org.exoplatform.services.jcr.ext.backup.impl.JobRepositoryRestore;
import org.exoplatform.services.jcr.ext.backup.impl.JobWorkspaceRestore;
import org.exoplatform.services.jcr.ext.backup.server.HTTPBackupAgent;
import org.exoplatform.services.jcr.ext.backup.server.bean.BackupConfigBean;
import org.exoplatform.services.jcr.ext.backup.server.bean.response.BackupServiceInfoBean;
import org.exoplatform.services.jcr.ext.backup.server.bean.response.DetailedInfo;
import org.exoplatform.services.jcr.ext.backup.server.bean.response.ShortInfo;
import org.exoplatform.services.jcr.ext.backup.server.bean.response.ShortInfoList;
import org.exoplatform.services.jcr.ext.repository.RestRepositoryService;
import org.exoplatform.ws.frameworks.json.JsonHandler;
import org.exoplatform.ws.frameworks.json.JsonParser;
import org.exoplatform.ws.frameworks.json.impl.BeanBuilder;
import org.exoplatform.ws.frameworks.json.impl.JsonDefaultHandler;
import org.exoplatform.ws.frameworks.json.impl.JsonException;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;
import org.exoplatform.ws.frameworks.json.impl.JsonParserImpl;
import org.exoplatform.ws.frameworks.json.value.JsonValue;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.ws.rs.core.Response;

/**
 * Created by The eXo Platform SAS. <br/>Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: BackupClientImpl.java 111 2008-11-11 11:11:11Z serg $
 */
public class BackupClientImpl
   implements BackupClient
{

   /**
    * Block size.
    */
   private static final int BLOCK_SIZE = 1024;

   /**
    * Client transport.
    */
   private ClientTransport transport;

   /**
    * The URL path.
    */
   private final String path;

   /**
    * Form authentication parameters.
    */
   private FormAuthentication formAuthentication;;

   /**
    * Constructor.
    * 
    * @param transport ClientTransport implementation.
    * @param urlPath url path.
    */
   public BackupClientImpl(ClientTransport transport, String urlPath)
   {
      this.transport = transport;

      if (urlPath == null)
         path = "/rest";
      else
         path = urlPath;
   }

   /**
    * Constructor.
    * 
    * @param transport ClientTransport implementation.
    * @param login user login.
    * @param pass user password.
    * @param urlPath url path.
    */
   public BackupClientImpl(ClientTransport transport, String login, String pass, String urlPath)
   {
      this.transport = transport;

      if (urlPath == null)
         path = "/rest";
      else
         path = urlPath;
   }

   /**
    * Constructor.
    * 
    * @param transport ClientTransport implementation.
    * @param formAuthentication form authentication.
    * @param urlPath url path.
    */
   public BackupClientImpl(ClientTransport transport, FormAuthentication formAuthentication, String urlPath)
   {
      this(transport, urlPath);
      this.formAuthentication = formAuthentication;
   }

   /**
    * {@inheritDoc}
    */
   public String startBackUp(String repositoryName, String workspaceName, String backupDir) throws IOException,
            BackupExecuteException
   {
      if (workspaceName != null)
      {
         String sURL =
                  path + HTTPBackupAgent.Constants.BASE_URL + HTTPBackupAgent.Constants.OperationType.START_BACKUP
                           + "/" + repositoryName + "/" + workspaceName;

         BackupConfigBean bean = new BackupConfigBean(BackupManager.FULL_BACKUP_ONLY, backupDir);

         JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
         JsonValue json;
         try
         {
            json = generatorImpl.createJsonObject(bean);
         }
         catch (JsonException e)
         {
            throw new BackupExecuteException("Can not get json from  : " + bean.getClass().toString(), e);
         }

         BackupAgentResponse response = transport.executePOST(sURL, json.toString());

         if (response.getStatus() == Response.Status.OK.getStatusCode())
         {
            return "\nSuccessful : \n" + "\tstatus code = " + response.getStatus() + "\n";
         }
         else
         {
            return failureProcessing(response);
         }
      }
      else
      {
         String sURL =
                  path + HTTPBackupAgent.Constants.BASE_URL
                           + HTTPBackupAgent.Constants.OperationType.START_BACKUP_REPOSITORY + "/" + repositoryName;

         BackupConfigBean bean = new BackupConfigBean(BackupManager.FULL_BACKUP_ONLY, backupDir);

         JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
         JsonValue json;
         try
         {
            json = generatorImpl.createJsonObject(bean);
         }
         catch (JsonException e)
         {
            throw new BackupExecuteException("Can not get json from  : " + bean.getClass().toString(), e);
         }

         BackupAgentResponse response = transport.executePOST(sURL, json.toString());

         if (response.getStatus() == Response.Status.OK.getStatusCode())
         {
            return "\nSuccessful : \n" + "\tstatus code = " + response.getStatus() + "\n";
         }
         else
         {
            return failureProcessing(response);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public String startIncrementalBackUp(String repositoryName, String workspaceName, String backupDir, long incr)
            throws IOException, BackupExecuteException
   {
      if (workspaceName != null)
      {
         String sURL =
                  path + HTTPBackupAgent.Constants.BASE_URL + HTTPBackupAgent.Constants.OperationType.START_BACKUP
                           + "/" + repositoryName + "/" + workspaceName;

         BackupConfigBean bean = new BackupConfigBean(BackupManager.FULL_AND_INCREMENTAL, backupDir, incr);

         JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
         JsonValue json;
         try
         {
            json = generatorImpl.createJsonObject(bean);
         }
         catch (JsonException e)
         {
            throw new BackupExecuteException("Can not get json from  : " + bean.getClass().toString(), e);
         }

         BackupAgentResponse response = transport.executePOST(sURL, json.toString());

         if (response.getStatus() == Response.Status.OK.getStatusCode())
         {
            return "\nSuccessful : \n" + "\tstatus code = " + response.getStatus() + "\n";
         }
         else
         {
            return failureProcessing(response);
         }
      }
      else
      {
         String sURL =
                  path + HTTPBackupAgent.Constants.BASE_URL
                           + HTTPBackupAgent.Constants.OperationType.START_BACKUP_REPOSITORY + "/" + repositoryName;

         BackupConfigBean bean = new BackupConfigBean(BackupManager.FULL_AND_INCREMENTAL, backupDir, incr);

         JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
         JsonValue json;
         try
         {
            json = generatorImpl.createJsonObject(bean);
         }
         catch (JsonException e)
         {
            throw new BackupExecuteException("Can not get json from  : " + bean.getClass().toString(), e);
         }

         BackupAgentResponse response = transport.executePOST(sURL, json.toString());

         if (response.getStatus() == Response.Status.OK.getStatusCode())
         {
            return "\nSuccessful : \n" + "\tstatus code = " + response.getStatus() + "\n";
         }
         else
         {
            return failureProcessing(response);
         }
      }

   }

   /**
    * {@inheritDoc}
    */
   public String status(String backupId) throws IOException, BackupExecuteException
   {

      String sURL =
               path + HTTPBackupAgent.Constants.BASE_URL
                        + HTTPBackupAgent.Constants.OperationType.CURRENT_OR_COMPLETED_BACKUP_REPOSITORY_INFO + "/"
                        + backupId;

      BackupAgentResponse response = transport.executeGET(sURL);

      if (response.getStatus() != Response.Status.OK.getStatusCode())
      {

         sURL =
                  path + HTTPBackupAgent.Constants.BASE_URL
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_OR_COMPLETED_BACKUP_INFO + "/" + backupId;

         response = transport.executeGET(sURL);
      }

      if (response.getStatus() == Response.Status.OK.getStatusCode())
      {
         DetailedInfo info;
         try
         {
            info = (DetailedInfo) getObject(DetailedInfo.class, response.getResponseData());
         }
         catch (Exception e)
         {
            throw new IllegalStateException("Can not get DetailedInfo from responce.", e);
         }

         if (info.getType() == DetailedInfo.COMPLETED)
         {
            StringBuilder result = new StringBuilder("\nThe completed (ready to restore) backup information : \n");

            BackupConfigBean configBean = info.getBackupConfig();

            result.append("\t\tbackup id               : ").append(info.getBackupId()).append("\n");
            result.append("\t\tbackup folder           : ").append(configBean.getBackupDir()).append("\n");
            result.append("\t\trepository name         : ").append(info.getRepositoryName()).append("\n");
            result.append(info.getWorkspaceName().equals("") ? "" : "\t\tworkspace name          : "
               + info.getWorkspaceName() + "\n");
            result.append("\t\tbackup type             : ");
            result.append(configBean.getBackupType() == BackupManager.FULL_AND_INCREMENTAL ? "full + incremental"
               : "full only");
            result.append("\n");
            result.append("\t\tstarted time            : ").append(info.getStartedTime()).append("\n");
            result.append(info.getFinishedTime().equals("") ? "\n" : "\t\tfinished time           : "
               + info.getFinishedTime() + "\n\n");

            return result.toString();
         }
         else
         {
            StringBuilder result = new StringBuilder("\nThe current backup information : \n"); 
            BackupConfigBean configBean = info.getBackupConfig();

            result.append("\t\tbackup id                : ").append(info.getBackupId()).append("\n");
            result.append("\t\tbackup folder            : ").append(configBean.getBackupDir()).append("\n");
            result.append("\t\trepository name          : ").append(info.getRepositoryName()).append("\n");
            result.append(info.getWorkspaceName().equals("") ? "" : "\t\tworkspace name           : "
               + info.getWorkspaceName() + "\n");
            result.append("\t\tbackup type              : ");
            result.append(configBean.getBackupType() == BackupManager.FULL_AND_INCREMENTAL ? "full + incremental"
               : "full only");
            result.append("\n");
            result.append("\t\tfull backup state        : ");
            result.append(info.getWorkspaceName().equals("") ? getRepositoryBackupToFullState(info.getState())
               : getState(info.getState()));
            result.append("\n");
            result.append(info.getBackupType() == BackupManager.FULL_BACKUP_ONLY ? ""
               : "\t\tincremental backup state : " + "working" + "\n");
            result.append("\t\tstarted time             : ").append(info.getStartedTime()).append("\n");
            result.append(info.getFinishedTime().equals("") ? "\n" : "\t\tfinished time            : "
               + info.getFinishedTime() + "\n\n");
            
            return result.toString();
         }
      }
      else
      {
         return failureProcessing(response);
      }
   }

   /**
    * {@inheritDoc}
    */
   public String stop(String backupId) throws IOException, BackupExecuteException
   {
      // first try to find current repository backup
      String sURL =
               path + HTTPBackupAgent.Constants.BASE_URL
                        + HTTPBackupAgent.Constants.OperationType.CURRENT_BACKUPS_REPOSITORY_INFO;

      BackupAgentResponse repositoryResponse = transport.executeGET(sURL);

      if (repositoryResponse.getStatus() == Response.Status.OK.getStatusCode())
      {
         ShortInfoList repositoryInfoList;
         try
         {
            repositoryInfoList = (ShortInfoList) getObject(ShortInfoList.class, repositoryResponse.getResponseData());
         }
         catch (Exception e)
         {
            throw new IllegalStateException("Can not get ShortInfoList from responce.", e);
         }

         for (ShortInfo info : repositoryInfoList.getBackups())
         {
            if (info.getBackupId().equals(backupId))
            {
               // repository backup
               sURL =
                        path + HTTPBackupAgent.Constants.BASE_URL
                                 + HTTPBackupAgent.Constants.OperationType.STOP_BACKUP_REPOSITORY + "/" + backupId;
               BackupAgentResponse response = transport.executeGET(sURL);

               if (response.getStatus() == Response.Status.OK.getStatusCode())
               {
                  return "\nSuccessful : \n" + "\tstatus code = " + response.getStatus() + "\n";
               }
               else
               {
                  return failureProcessing(response);
               }
            }
         }
      }

      // then try to find current workspace backup
      sURL = path + HTTPBackupAgent.Constants.BASE_URL + HTTPBackupAgent.Constants.OperationType.CURRENT_BACKUPS_INFO;

      BackupAgentResponse workspaceResponse = transport.executeGET(sURL);
      if (workspaceResponse.getStatus() == Response.Status.OK.getStatusCode())
      {
         ShortInfoList workspaceInfoList;
         try
         {
            workspaceInfoList = (ShortInfoList) getObject(ShortInfoList.class, workspaceResponse.getResponseData());
         }
         catch (Exception e)
         {
            throw new IllegalStateException("Can not get ShortInfoList from responce.", e);
         }

         for (ShortInfo info : workspaceInfoList.getBackups())
         {
            if (info.getBackupId().equals(backupId))
            {
               // workspace backup
               sURL =
                        path + HTTPBackupAgent.Constants.BASE_URL + HTTPBackupAgent.Constants.OperationType.STOP_BACKUP
                                 + "/" + backupId;

               BackupAgentResponse response = transport.executeGET(sURL);

               if (response.getStatus() == Response.Status.OK.getStatusCode())
               {
                  return "\nSuccessful : \n" + "\tstatus code = " + response.getStatus() + "\n";
               }
               else
               {
                  return failureProcessing(response);
               }
            }
         }
      }

      return "\nFailure :\n" + "\tmessage      : There are no active backup with id  " + backupId;
   }

   /**
    * {@inheritDoc}
    */
   public String restore(String repositoryName, String workspaceName, String backupId, InputStream config,
            String backupSetPath, boolean removeExists)
            throws IOException, BackupExecuteException
   {
      BackupAgentResponse response = null;
      String sURL = null;

      String backupSetPathEncoded = null;

      if (backupSetPath != null)
      {
         backupSetPathEncoded = URLEncoder.encode(backupSetPath, "UTF-8");
      }

      if (workspaceName != null)
      {
         if (config != null)
         {
            if (backupId != null )
            {
               sURL =
                        path + HTTPBackupAgent.Constants.BASE_URL + HTTPBackupAgent.Constants.OperationType.RESTORE + "/"
                                 + repositoryName + "/" + backupId + "/" + removeExists;
            }
            else if (backupSetPath != null)
            {
               sURL =
                        path + HTTPBackupAgent.Constants.BASE_URL
                                 + HTTPBackupAgent.Constants.OperationType.RESTORE_BACKUP_SET + "/"
                                 + repositoryName
                                 + "/" + removeExists + "?backup-set-path=" + backupSetPathEncoded;
            }

            WorkspaceEntry wEntry = null;
            try
            {
               wEntry = getWorkspaceEntry(config, repositoryName, workspaceName);
            }
            catch (Throwable e)
            {
               throw new BackupExecuteException("Can not get WorkspaceEntry for workspace '" + workspaceName
                        + "' from config.", e);
            }

            JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
            JsonValue json;

            try
            {
               json = generatorImpl.createJsonObject(wEntry);
            }
            catch (JsonException e)
            {
               throw new BackupExecuteException("Can not get json from  : " + wEntry.getClass().toString(), e);
            }

            response = transport.executePOST(sURL, json.toString());
         }
      }
      else if (repositoryName != null)
      {
         if (config != null)
         {
            if (backupId != null)
            {
               sURL =
                        path + HTTPBackupAgent.Constants.BASE_URL
                                 + HTTPBackupAgent.Constants.OperationType.RESTORE_REPOSITORY + "/" + backupId + "/"
                                 + removeExists;
            }
            else if (backupSetPath != null)
            {
               sURL =
                        path + HTTPBackupAgent.Constants.BASE_URL
                                 + HTTPBackupAgent.Constants.OperationType.RESTORE_REPOSITORY_BACKUP_SET + "/"
                                 + removeExists + "?backup-set-path=" + backupSetPathEncoded;
            }
   
            RepositoryEntry wEntry = null;
            try
            {
               wEntry = getRepositoryEntry(config, repositoryName);
            }
            catch (Throwable e)
            {
               throw new BackupExecuteException("Can not get RepositoryEntry for repository '" + repositoryName
                        + "' from config.", e);
            }
   
            JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
            JsonValue json;
   
            try
            {
               json = generatorImpl.createJsonObject(wEntry);
            }
            catch (JsonException e)
            {
               throw new BackupExecuteException("Can not get json from  : " + wEntry.getClass().toString(), e);
            }
   
            response = transport.executePOST(sURL, json.toString());
         } 
      }
      else
      {
         if (backupId != null)
         {

            //check is repository or workspace backup
            boolean isRepository = true;

            String lsURL =
                     path + HTTPBackupAgent.Constants.BASE_URL
                              + HTTPBackupAgent.Constants.OperationType.CURRENT_OR_COMPLETED_BACKUP_REPOSITORY_INFO
                              + "/" + backupId;

            BackupAgentResponse lResponse = transport.executeGET(lsURL);

            if (lResponse.getStatus() != Response.Status.OK.getStatusCode())
            {

               sURL =
                        path + HTTPBackupAgent.Constants.BASE_URL
                                 + HTTPBackupAgent.Constants.OperationType.CURRENT_OR_COMPLETED_BACKUP_INFO + "/"
                                 + backupId;

               lResponse = transport.executeGET(sURL);

               if (lResponse.getStatus() == Response.Status.OK.getStatusCode())
               {
                  isRepository = false;
               }
               else
               {
                  return failureProcessing(lResponse);
               }
            }

            if (isRepository)
            {
               sURL =
                        path + HTTPBackupAgent.Constants.BASE_URL
                                 + HTTPBackupAgent.Constants.OperationType.RESTORE_REPOSITORY + "/" + backupId + "/"
                                 + removeExists;
            }
            else
            {
               sURL =
                        path + HTTPBackupAgent.Constants.BASE_URL + HTTPBackupAgent.Constants.OperationType.RESTORE
                                 + "/" + backupId + "/" + removeExists;
            }

         }
         else if (backupSetPath != null)
         {
            sURL =
                     path + HTTPBackupAgent.Constants.BASE_URL
                              + HTTPBackupAgent.Constants.OperationType.RESTORE_BACKUP_SET + "/" + removeExists
                              + "?backup-set-path=" + backupSetPathEncoded;
         }

         response = transport.executeGET(sURL);
      }

      if (response.getStatus() == Response.Status.OK.getStatusCode())
      {
         return "\nSuccessful : \n" + "\tstatus code = " + response.getStatus() + "\n";
      }
      else
      {
         return failureProcessing(response);
      }
   }

   /**
    * {@inheritDoc}
    */
   public String drop(boolean forceClose, String repositoryName, String workspaceName) throws IOException,
            BackupExecuteException
   {
      if (workspaceName != null)
      {
         String sURL =
                  path + HTTPBackupAgent.Constants.BASE_URL + HTTPBackupAgent.Constants.OperationType.DROP_WORKSPACE
                           + "/" + repositoryName + "/" + workspaceName + "/" + forceClose;

         BackupAgentResponse response = transport.executeGET(sURL);

         if (response.getStatus() == Response.Status.OK.getStatusCode())
         {
            return "\nSuccessful : \n" + "\tstatus code = " + response.getStatus() + "\n";
         }
         else
         {
            return failureProcessing(response);
         }
      }
      else
      {
         String sURL =
                  path + RestRepositoryService.Constants.BASE_URL
                           + RestRepositoryService.Constants.OperationType.REMOVE_REPOSITORY + "/" + repositoryName
                           + "/" + forceClose;

         BackupAgentResponse response = transport.executeGET(sURL);

         if (response.getStatus() == Response.Status.OK.getStatusCode())
         {
            return "\nSuccessful : \n" + "\tstatus code = " + response.getStatus() + "\n";
         }
         else
         {
            return failureProcessing(response);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public String info() throws IOException, BackupExecuteException
   {
      String sURL =
               path + HTTPBackupAgent.Constants.BASE_URL + HTTPBackupAgent.Constants.OperationType.BACKUP_SERVICE_INFO;
      BackupAgentResponse response = transport.executeGET(sURL);

      if (response.getStatus() == Response.Status.OK.getStatusCode())
      {
         BackupServiceInfoBean info;
         try
         {
            info = (BackupServiceInfoBean) getObject(BackupServiceInfoBean.class, response.getResponseData());
         }
         catch (Exception e)
         {
            throw new IllegalStateException("Can not get BackupServiceInfoBean from responce.", e);
         }

         String result =
                  "\nThe backup service information : \n" + "\tfull backup type               : "
                           + info.getFullBackupType() + "\n" + "\tincremental backup type        : "
                           + info.getIncrementalBackupType() + "\n" + "\tbackup log folder              : "
                           + info.getBackupLogDir() + "\n" + "\tdefault incremental job period : "
                           + info.getDefaultIncrementalJobPeriod() + "\n\n";

         return result;
      }
      else
      {
         return failureProcessing(response);
      }
   }

   /**
    * {@inheritDoc}
    */
   public String list() throws IOException, BackupExecuteException
   {

      String sURL =
               path + HTTPBackupAgent.Constants.BASE_URL
                        + HTTPBackupAgent.Constants.OperationType.CURRENT_BACKUPS_REPOSITORY_INFO;
      BackupAgentResponse repositoryResponse = transport.executeGET(sURL);

      sURL = path + HTTPBackupAgent.Constants.BASE_URL + HTTPBackupAgent.Constants.OperationType.CURRENT_BACKUPS_INFO;
      BackupAgentResponse workspaceResponse = transport.executeGET(sURL);

      if ((repositoryResponse.getStatus() == Response.Status.OK.getStatusCode())
               && (workspaceResponse.getStatus() == Response.Status.OK.getStatusCode()))
      {
         ShortInfoList repositoryInfoList;
         try
         {
            repositoryInfoList = (ShortInfoList) getObject(ShortInfoList.class, repositoryResponse.getResponseData());
         }
         catch (Exception e)
         {
            throw new IllegalStateException("Can not get ShortInfoList from responce.", e);
         }

         ShortInfoList workspaceInfoList;
         try
         {
            workspaceInfoList = (ShortInfoList) getObject(ShortInfoList.class, workspaceResponse.getResponseData());
         }
         catch (Exception e)
         {
            throw new IllegalStateException("Can not get ShortInfoList from responce.", e);
         }

         StringBuilder result = new StringBuilder("\nThe current backups information : \n");

         if ((repositoryInfoList.getBackups().size() == 0) && (workspaceInfoList.getBackups().size() == 0))
         {
            result.append("\tNo active backups.\n\n");
         }

         int count = 1;
         for (ShortInfo shortInfo : repositoryInfoList.getBackups())
         {
            result.append("\t").append(count).append(") Repository backup with id ").append(shortInfo.getBackupId())
               .append(" :\n");
            result.append("\t\trepository name            : ").append(shortInfo.getRepositoryName()).append("\n");
            result.append("\t\tbackup type                : ");
            result.append(shortInfo.getBackupType() == BackupManager.FULL_AND_INCREMENTAL ? "full + incremental"
               : "full only");
            result.append("\n");
            result.append("\t\tfull backups state         : ")
               .append(getRepositoryBackupToFullState(shortInfo.getState())).append("\n");
            result.append(shortInfo.getBackupType() == BackupManager.FULL_BACKUP_ONLY ? ""
               : "\t\tincremental backups state  : " + "working" + "\n");
            result.append("\t\tstarted time               : ").append(shortInfo.getStartedTime()).append("\n");
            result.append(shortInfo.getFinishedTime().equals("") ? "" : "\t\tfinished time              : "
               + shortInfo.getFinishedTime() + "\n");
            
            count++;
         }

         for (ShortInfo shortInfo : workspaceInfoList.getBackups())
         {
            result.append("\t").append(count).append(") Workspace backup with id ").append(shortInfo.getBackupId())
               .append(" :\n");

            result.append("\t\trepository name            : ").append(shortInfo.getRepositoryName()).append("\n");
            result.append("\t\tworkspace name             : ").append(shortInfo.getWorkspaceName()).append("\n");

            result.append("\t\tbackup type                : ");
            result.append(
               shortInfo.getBackupType() == BackupManager.FULL_AND_INCREMENTAL ? "full + incremental" : "full only")
               .append("\n");

            result.append("\t\tfull backup state          : ").append(getState(shortInfo.getState())).append("\n");
            result.append(shortInfo.getBackupType() == BackupManager.FULL_BACKUP_ONLY ? ""
               : "\t\tincremental backup state   : " + "working\n");
            result.append("\t\tstarted time               : ").append(shortInfo.getStartedTime()).append("\n");
            result.append(shortInfo.getFinishedTime().equals("") ? "" : "\t\tfinished time              : "
               + shortInfo.getFinishedTime() + "\n");

            count++;
         }

         return result.toString();
      }
      else
      {
         return failureProcessing(workspaceResponse);
      }
   }

   /**
    * {@inheritDoc}
    */
   public String listCompleted() throws IOException, BackupExecuteException
   {
      String sURL =
               path + HTTPBackupAgent.Constants.BASE_URL
                        + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_REPOSITORY_INFO;
      BackupAgentResponse repositoryResponse = transport.executeGET(sURL);

      sURL = path + HTTPBackupAgent.Constants.BASE_URL + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_INFO;
      BackupAgentResponse workspaceResponse = transport.executeGET(sURL);

      if ((workspaceResponse.getStatus() == Response.Status.OK.getStatusCode())
               && (repositoryResponse.getStatus() == Response.Status.OK.getStatusCode()))
      {

         ShortInfoList repositoryInfoList;
         try
         {
            repositoryInfoList = (ShortInfoList) getObject(ShortInfoList.class, repositoryResponse.getResponseData());
         }
         catch (Exception e)
         {
            throw new IllegalStateException("Can not get ShortInfoList from responce.", e);
         }

         ShortInfoList workspaceInfoList;
         try
         {
            workspaceInfoList = (ShortInfoList) getObject(ShortInfoList.class, workspaceResponse.getResponseData());
         }
         catch (Exception e)
         {
            throw new RuntimeException("Can not get ShortInfoList from responce.", e);
         }

         StringBuilder result = new StringBuilder("\nThe completed (ready to restore) backups information : \n");

         if ((repositoryInfoList.getBackups().size() == 0) && (workspaceInfoList.getBackups().size() == 0))
         {
            result.append("\tNo completed backups.\n\n");
         }

         int count = 1;
         for (ShortInfo shortInfo : repositoryInfoList.getBackups())
         {
            result.append("\t").append(count).append(") Repository backup with id ").append(shortInfo.getBackupId())
               .append(" :\n");
            result.append("\t\trepository name           : ").append(shortInfo.getRepositoryName()).append("\n");

            result.append("\t\tbackup type               : ");
            result.append(
               shortInfo.getBackupType() == BackupManager.FULL_AND_INCREMENTAL ? "full + incremental" : "full only")
               .append("\n");
            result.append("\t\tstarted time              : ").append(shortInfo.getStartedTime()).append("\n");
            result.append(shortInfo.getFinishedTime().equals("") ? "\n"
               : "\t\tfinished time             : " + shortInfo.getFinishedTime() + "\n");

            count++;
         }

         for (ShortInfo shortInfo : workspaceInfoList.getBackups())
         {
            result.append("\t").append(count).append(") Workspace backup with id ").append(shortInfo.getBackupId())
               .append(" :\n");
            result.append("\t\trepository name           : ").append(shortInfo.getRepositoryName()).append("\n");
            result.append("\t\tworkspace name            : ").append(shortInfo.getWorkspaceName()).append("\n");

            result.append("\t\tbackup type               : ");
            result.append(
               shortInfo.getBackupType() == BackupManager.FULL_AND_INCREMENTAL ? "full + incremental" : "full only")
               .append("\n");
            result.append("\t\tstarted time              : ").append(shortInfo.getStartedTime()).append("\n");
            result.append(shortInfo.getFinishedTime().equals("") ? "\n"
               : "\t\tfinished time             : " + shortInfo.getFinishedTime() + "\n");

            count++;
         }

         return result.toString();
      }
      else
      {
         return failureProcessing(workspaceResponse);
      }
   }

   /**
    * {@inheritDoc}
    */
   public String restores(String repositoryName, String workspaceName) throws IOException, BackupExecuteException
   {
      if (workspaceName != null)
      {
         String sURL =
                  path + HTTPBackupAgent.Constants.BASE_URL
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_WS + "/" + repositoryName
                           + "/" + workspaceName;
         BackupAgentResponse response = transport.executeGET(sURL);

         if (response.getStatus() == Response.Status.OK.getStatusCode())
         {
            DetailedInfo info;
            try
            {
               info = (DetailedInfo) getObject(DetailedInfo.class, response.getResponseData());
            }
            catch (Exception e)
            {
               throw new IllegalStateException("Can not get DetailedInfo from responce.", e);
            }

            StringBuilder result = new StringBuilder("\nThe current restores information : \n");

            result.append("\tWorkspace restore with id ").append(info.getBackupId()).append(":\n");

            BackupConfigBean configBean = info.getBackupConfig();

            result.append("\t\tbackup folder           : ").append(configBean.getBackupDir()).append("\n");
            result.append("\t\trepository name         : ").append(info.getRepositoryName()).append("\n");
            result.append("\t\tworkspace name          : ").append(info.getWorkspaceName()).append("\n");
            result.append("\t\tbackup type             : ");
            result.append(
               configBean.getBackupType() == BackupManager.FULL_AND_INCREMENTAL ? "full + incremental" : "full only")
               .append("\n");
            result.append("\t\trestore state           : ").append(getRestoreState(info.getState())).append("\n");
            result.append("\t\tstarted time            : ").append(info.getStartedTime()).append("\n");
            result.append(info.getFinishedTime().equals("") ? "\n"
               : "\t\tfinished time           : " + info.getFinishedTime() + "\n\n");

            return result.toString();
         }
         else
         {
            return failureProcessing(response);
         }
      }
      else
      {
         String sURL =
                  path + HTTPBackupAgent.Constants.BASE_URL
                           + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_REPOSITORY + "/"
                           + repositoryName;

         BackupAgentResponse response = transport.executeGET(sURL);

         if (response.getStatus() == Response.Status.OK.getStatusCode())
         {
            DetailedInfo info;
            try
            {
               info = (DetailedInfo) getObject(DetailedInfo.class, response.getResponseData());
            }
            catch (Exception e)
            {
               throw new IllegalStateException("Can not get DetailedInfo from responce.", e);
            }

            StringBuilder result = new StringBuilder("\nThe current restores information : \n");

            result.append("\tRepository restore with id ").append(info.getBackupId()).append(":\n");

            BackupConfigBean configBean = info.getBackupConfig();

            result.append("\t\tbackup folder           : ").append(configBean.getBackupDir()).append("\n");
            result.append("\t\trepository name         : ").append(info.getRepositoryName()).append("\n");
            result.append("\t\tbackup type             : ");
            result.append(
               configBean.getBackupType() == BackupManager.FULL_AND_INCREMENTAL ? "full + incremental" : "full only")
               .append("\n");
            result.append("\t\trestore state           : ").append(getRepositoryRestoreState(info.getState()))
               .append("\n");
            result.append("\t\tstarted time            : ").append(info.getStartedTime()).append("\n");
            result.append(info.getFinishedTime().equals("") ? "\n"
               : "\t\tfinished time           : " + info.getFinishedTime() + "\n\n");
            
            return result.toString();
         }
         else
         {
            return failureProcessing(response);
         }
      }
   }

   /**
    * Will be created the Object from JSON binary data.
    *
    * @param cl 
    *          Class
    * @param data
    *          binary data (JSON)
    * @return Object
    * @throws Exception
    *           will be generated Exception
    */
   private Object getObject(Class cl, byte[] data) throws Exception
   {
      JsonHandler jsonHandler = new JsonDefaultHandler();
      JsonParser jsonParser = new JsonParserImpl();
      InputStream inputStream = new ByteArrayInputStream(data);
      jsonParser.parse(inputStream, jsonHandler);
      JsonValue jsonValue = jsonHandler.getJsonObject();

      return new BeanBuilder().createObject(cl, jsonValue);
   }

   private String getRestoreState(int restoreState)
   {
      String state = "unknown sate of restore";

      switch (restoreState)
      {
         case JobWorkspaceRestore.RESTORE_INITIALIZED :
            state = "initialized";
            break;

         case JobWorkspaceRestore.RESTORE_STARTED :
            state = "started";
            break;

         case JobWorkspaceRestore.RESTORE_SUCCESSFUL :
            state = "successful";
            break;

         case JobWorkspaceRestore.RESTORE_FAIL :
            state = "fail";
            break;

         default :
            break;
      }

      return state;
   }

   private String getRepositoryRestoreState(int restoreState)
   {
      String state = "unknown sate of restore";

      switch (restoreState)
      {
         case JobRepositoryRestore.REPOSITORY_RESTORE_INITIALIZED :
            state = "initialized";
            break;

         case JobRepositoryRestore.REPOSITORY_RESTORE_STARTED :
            state = "started";
            break;

         case JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL :
            state = "successful";
            break;

         case JobRepositoryRestore.REPOSITORY_RESTORE_FAIL :
            state = "fail";
            break;

         default :
            break;
      }

      return state;
   }

   /**
    * getState.
    * 
    * @param state
    *          value of state
    * @return String sate
    */
   private String getState(int state)
   {
      String st = "";
      switch (state)
      {

         case BackupJob.FINISHED :
            st = "finished";
            break;

         case BackupJob.WORKING :
            st = "working";
            break;

         case BackupJob.WAITING :
            st = "waiting";
            break;

         case BackupJob.STARTING :
            st = "starting";
            break;
         default :
            break;
      }

      return st;
   }

   /**
    * getState.
    * 
    * @param state
    *          value of state
    * @return String sate
    */
   private String getRepositoryBackupToFullState(int state)
   {
      String st = "";
      switch (state)
      {

         case RepositoryBackupChain.FINISHED :
            st = "finished";
            break;

         case RepositoryBackupChain.WORKING :
            st = "working";
            break;

         case RepositoryBackupChain.INITIALIZED :
            st = "initialized";
            break;

         case RepositoryBackupChain.FULL_BACKUP_FINISHED_INCREMENTAL_BACKUP_WORKING :
            st = "finished";
            break;
         default :
            break;
      }

      return st;
   }

   /**
    * failureProcessing.
    *
    * @param data
    *          response data
    * @return String
    *           result
    * @throws BackupExecuteException
    *           will be generated BackupExecuteException  
    */
   private String failureProcessing(BackupAgentResponse response) throws BackupExecuteException
   {
      try
      {
         String result =
                  "\nFailure :\n" + "\tstatus code : " + response.getStatus() + "\n" + "\tmessage      : "
                           + new String(response.getResponseData(), "UTF-8") + "\n\n";

         return result;
      }
      catch (UnsupportedEncodingException e)
      {
         throw new BackupExecuteException("Can not encoded the responce : " + e.getMessage(), e);
      }
   }

   /**
    * getRepositoryEntry.
    *
    * @param wEntryStream
    *          InputStream, the workspace configuration
    * @return RepositoryEntry
    *           return the workspace entry
    * @throws FileNotFoundException
    *           will be generated the FileNotFoundException 
    * @throws JiBXException
    *           will be generated the JiBXException 
    * @throws RepositoryConfigurationException
    *           will be generated the RepositoryConfigurationException 
    */
   private RepositoryEntry getRepositoryEntry(InputStream wEntryStream, String repositoryName)
            throws FileNotFoundException, JiBXException, RepositoryConfigurationException
   {
      IBindingFactory factory = BindingDirectory.getFactory(RepositoryServiceConfiguration.class);
      IUnmarshallingContext uctx = factory.createUnmarshallingContext();
      RepositoryServiceConfiguration conf = (RepositoryServiceConfiguration) uctx.unmarshalDocument(wEntryStream, null);
      RepositoryEntry rEntry = conf.getRepositoryConfiguration(repositoryName);

      return rEntry;
   }

   /**
    * getWorkspaceEntry.
    *
    * @param wEntryStream
    *          InputStream, the workspace configuration
    * @param workspaceName
    *          String, the workspace name 
    * @return WorkspaceEntry
    *           return the workspace entry
    * @throws FileNotFoundException
    *           will be generated the FileNotFoundException 
    * @throws JiBXException
    *           will be generated the JiBXException 
    * @throws RepositoryConfigurationException
    *           will be generated the RepositoryConfigurationException 
    */
   private WorkspaceEntry getWorkspaceEntry(InputStream wEntryStream, String repositoryName, String workspaceName)
            throws FileNotFoundException, JiBXException, RepositoryConfigurationException
   {
      RepositoryEntry rEntry = getRepositoryEntry(wEntryStream, repositoryName);

      WorkspaceEntry wsEntry = null;

      for (WorkspaceEntry wEntry : rEntry.getWorkspaceEntries())
         if (wEntry.getName().equals(workspaceName))
            wsEntry = wEntry;

      if (wsEntry == null)
      {
         throw new IllegalStateException("Can not find the workspace '" + workspaceName + "' in configuration.");
      }

      return wsEntry;
   }

}
