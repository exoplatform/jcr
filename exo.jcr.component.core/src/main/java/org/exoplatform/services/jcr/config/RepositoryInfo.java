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
package org.exoplatform.services.jcr.config;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov </a>
 * @version $Id: $
 * 
 * Short repository info, does not include workspaces collection.
 * Tends to be extended with some workspaces collection info.
 * 
 */

public class RepositoryInfo
{

   protected String name;

   protected String systemWorkspaceName;

   protected String defaultWorkspaceName;

   protected String accessControl;

   protected String securityDomain;

   protected String authenticationPolicy;

   protected long sessionTimeOut;

   protected int lockRemoverMaxThreadCount;

   protected int fileCleanerMaxThreadsCount;

   public RepositoryInfo()
   {

   }

   /**
    * @return the name
    */
   public String getName()
   {
      return name;
   }

   /**
    * @param name
    */
   public void setName(String name)
   {
      this.name = name;
   }

   /**
    * Get system workspace name.
    * 
    * @return Returns the systemWorkspace.
    */
   public String getSystemWorkspaceName()
   {
      return systemWorkspaceName;
   }

   /**
    * Set system workspace name.
    * 
    * @param systemWorkspace
    *          The systemWorkspace to set.
    */
   public void setSystemWorkspaceName(String systemWorkspace)
   {
      this.systemWorkspaceName = systemWorkspace;
   }

   /**
    * Get Access control.
    * 
    * @return Returns the accessControl.
    */
   public String getAccessControl()
   {
      return accessControl;
   }

   /**
    * Set access control.
    * 
    * @param accessControl
    *          The accessControl to set.
    */
   public void setAccessControl(String accessControl)
   {
      this.accessControl = accessControl;
   }

   /**
    * Get security domain.
    * 
    * @return Returns the securityDomain.
    */
   public String getSecurityDomain()
   {
      return securityDomain;
   }

   /**
    * Set security domain.
    * 
    * @param securityDomain
    *          The securityDomain to set.
    */
   public void setSecurityDomain(String securityDomain)
   {
      this.securityDomain = securityDomain;
   }

   /**
    * Get authentication policy.
    * 
    * @return Returns the authenticationPolicy.
    */
   public String getAuthenticationPolicy()
   {
      return authenticationPolicy;
   }

   /**
    * Set authentication policy.
    * 
    * @param authenticationPolicy
    *          The authenticationPolicy to set.
    */
   public void setAuthenticationPolicy(String authenticationPolicy)
   {
      this.authenticationPolicy = authenticationPolicy;
   }

   /**
    * Get default workspace name.
    * 
    * @return Returns the defaultWorkspaceName.
    */
   public String getDefaultWorkspaceName()
   {
      return defaultWorkspaceName;
   }

   /**
    * Set default workspace name.
    * 
    * @param defaultWorkspaceName
    *          The defaultWorkspaceName to set.
    */
   public void setDefaultWorkspaceName(String defaultWorkspaceName)
   {
      this.defaultWorkspaceName = defaultWorkspaceName;
   }

   /**
    * @return session timeout in milliseconds
    */
   public long getSessionTimeOut()
   {
      return sessionTimeOut;
   }

   /**
    * sets session timeout in milliseconds
    * @param sessionTimeOut
    */
   public void setSessionTimeOut(long sessionTimeOut)
   {
      this.sessionTimeOut = sessionTimeOut;
   }

   /**
    * Returns LockRemovers per-repository max threads count.
    * @return LockRemovers per-repository max threads count
    */
   public int getLockRemoverThreadsCount()
   {
      return lockRemoverMaxThreadCount;
   }

   /**
    * Returns FileCleaner per-repository max threads count.
    * @return LockRemovers per-repository max threads count
    */
   public int getFileCleanerThreadsCount()
   {
      return fileCleanerMaxThreadsCount;
   }

   /**
    * Sets LockRemovers per-repository max threads count.
    * @param lockRemoverMaxThreadCount
    */
   public void setLockRemoverThreadsCount(int lockRemoverMaxThreadCount)
   {
      this.lockRemoverMaxThreadCount = lockRemoverMaxThreadCount;
   }

   /**
    * Sets FileCleaner per-repository max threads count.
    * @param fileCleanerMaxThreadsCount
    */
   public void setFileCleanerThreadsCount(int fileCleanerMaxThreadsCount)
   {
      this.fileCleanerMaxThreadsCount = fileCleanerMaxThreadsCount;
   }

   /**
    * Merges the current {@link RepositoryInfo} with the given one. The current {@link RepositoryInfo}
    * has the highest priority thus only absent data will be overrode
    * @param entry the entry to merge with the current {@link RepositoryInfo}
    */
   void merge(RepositoryInfo entry)
   {
      if (systemWorkspaceName == null)
         setSystemWorkspaceName(entry.systemWorkspaceName);
      if (defaultWorkspaceName == null)
         setDefaultWorkspaceName(entry.defaultWorkspaceName);
      if (accessControl == null)
         setAccessControl(entry.accessControl);
      if (securityDomain == null)
         setSecurityDomain(entry.securityDomain);
      if (authenticationPolicy == null)
         setAuthenticationPolicy(entry.authenticationPolicy);
      if (sessionTimeOut == 0)
         setSessionTimeOut(entry.sessionTimeOut);
      if (lockRemoverMaxThreadCount == 0)
         setLockRemoverThreadsCount(entry.lockRemoverMaxThreadCount);
      if (fileCleanerMaxThreadsCount == 0)
         setFileCleanerThreadsCount(entry.fileCleanerMaxThreadsCount);
   }
}
