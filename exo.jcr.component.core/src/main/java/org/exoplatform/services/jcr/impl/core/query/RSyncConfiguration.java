package org.exoplatform.services.jcr.impl.core.query;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;

import java.io.File;
import java.io.IOException;

/**
 * RSync configuration class is used to manage RSync server parameters.
 */
public class RSyncConfiguration
{
    /*RSYNC server configuration PARAM names*/
    public static final String PARAM_RSYNC_ENTRY_NAME = "rsync-entry-name";

    public static final String PARAM_RSYNC_ENTRY_PATH = "rsync-entry-path";

    public static final String PARAM_RSYNC_PORT = "rsync-port";

    public static final int PARAM_RSYNC_PORT_DEFAULT = 873;

    public static final String PARAM_RSYNC_USER = "rsync-user";

    public static final String PARAM_RSYNC_PASSWORD = "rsync-password";

    public static final String PARAM_RSYNC_OFFLINE = "rsync-offline";

    /*rsync server entry name*/
    private String rsyncEntryName;

    /*rsync server entry path*/
    private String rsyncEntryPath;

    /*rsync server user name*/
    private String rsyncUserName;

    /*rsync server password name*/

    private String rsyncPassword;

    /*rsync server port number*/
    private  int rsyncPort;

    /*rsync command on offline mode*/
    private  boolean rsyncOffline;

    public RSyncConfiguration(QueryHandlerEntry config) throws RepositoryConfigurationException
    {
        // read RSYNC configuration
        rsyncEntryName = config.getParameterValue(PARAM_RSYNC_ENTRY_NAME, null);
        rsyncEntryPath = config.getParameterValue(PARAM_RSYNC_ENTRY_PATH, null);
        if(StringUtils.isEmpty(rsyncEntryName) || StringUtils.isEmpty(rsyncEntryPath))
        {
            throw new RepositoryConfigurationException("rsync-entry-path or rsync-entry-name path cannot be empty.");
        }
        rsyncUserName = config.getParameterValue(PARAM_RSYNC_USER, null);
        rsyncPassword = config.getParameterValue(PARAM_RSYNC_PASSWORD, null);
        rsyncPort = config.getParameterInteger(PARAM_RSYNC_PORT, PARAM_RSYNC_PORT_DEFAULT);
        rsyncOffline = config.getParameterBoolean(PARAM_RSYNC_OFFLINE, true);
    }

    public String getRsyncEntryName()
    {
        return rsyncEntryName;
    }

    public String getRsyncEntryPath()
    {
        return rsyncEntryPath;
    }

    public String getRsyncUserName()
    {
        return rsyncUserName;
    }

    public String getRsyncPassword()
    {
        return rsyncPassword;
    }

    public int getRsyncPort()
    {
        return rsyncPort;
    }

    public boolean isRsyncOffline()
    {
        return rsyncOffline;
    }

    public String generateRsyncSource(String path) throws RepositoryConfigurationException
    {
        String absoluteRsyncEntryPath;
        String urlFormatString;
        String indexPath;
        try
        {
            indexPath = new File(path).getCanonicalPath();
            absoluteRsyncEntryPath = new File(rsyncEntryPath).getCanonicalPath();
        }
        catch (IOException e)
        {
            throw new RepositoryConfigurationException("Index path or rsyncEntry path is invalid.", e);
        }

        if (indexPath.startsWith(absoluteRsyncEntryPath))
        {
            // in relation to RSync Server Entry
            // i.e. absolute index path is /var/portal/data/index/repo1/ws2
            // i.e. RSync Server Entry is "index" pointing to /var/portal/data/index
            // then relative path is repo1/ws2
            // and whole url is "rsync://<addr>:<port>/<entryName>/repo1/ws2"
            String relativeIndexPath = indexPath.substring(absoluteRsyncEntryPath.length());

            // if client is Windows OS, need to replace all '\' with '/' used in url
            if (File.separatorChar == '\\')
            {
                relativeIndexPath = relativeIndexPath.replace(File.separatorChar, '/');
            }
            // generate ready-to-use formatter string with address variable
            urlFormatString = "rsync://%s:" + rsyncPort + "/" + rsyncEntryName + relativeIndexPath + "/";
        }
        else
        {
            throw new RepositoryConfigurationException(
                    "Invalid RSync configuration. Index must be placed in folder that is a descendant of RSync Server Entry. "
                            + "Current RSync Server Entry Path is : " + absoluteRsyncEntryPath
                            + " but it doesnt hold Index folder, that is : " + indexPath
                            + ". Please fix configuration according to JCR Documentation and restart application.");
        }

        return urlFormatString;
    }
}
