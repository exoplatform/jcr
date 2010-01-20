package org.exoplatform.services.jcr.impl.core.query.lucene;

/**
 * This interface allows you to execute a given action when the state of the {@link IndexUpdateMonitor}
 * changes.
 * 
 * @author <a href="mailto:nicolas.filotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public interface IndexUpdateMonitorListener
{
   /**
    * This method is called when the flag <code>updateInProgress</code> changes of value
    * @param updateInProgress the new value of the flag
    */
   void onUpdateInProgressChange(boolean updateInProgress);
}
