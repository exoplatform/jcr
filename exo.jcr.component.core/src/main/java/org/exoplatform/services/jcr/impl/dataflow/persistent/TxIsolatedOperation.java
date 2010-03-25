package org.exoplatform.services.jcr.impl.dataflow.persistent;

import org.exoplatform.services.jcr.impl.storage.JCRInvalidItemStateException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * Handles XA transactionaly isolated operation. I.e. Action of this operation will be executed in dedicated global transaction.
 * If another current transaction exists, the one will be suspended and resumed after the execution.
 * At other hand if nested isolated operations perform they will use same (current, active) transaction. 
 */
public abstract class TxIsolatedOperation
{
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.TxIsolatedOperation");

   protected final TransactionManager txManager;

   protected final static ThreadLocal<Transaction> currentIsolated = new ThreadLocal<Transaction>();

   public TxIsolatedOperation(TransactionManager txManager)
   {
      this.txManager = txManager;
   }

   /**
    * Action body for a final implementation. 
    * 
    * @throws RepositoryException
    */
   protected abstract void action() throws RepositoryException;

   protected void beginTx() throws NotSupportedException, SystemException
   {
      txManager.begin(); // start new global tx
   }

   protected void commitTx() throws SecurityException, IllegalStateException, RollbackException,
      HeuristicMixedException, HeuristicRollbackException, SystemException
   {
      txManager.commit(); // commit global tx
   }

   protected void rollbackTx() throws NotSupportedException, SystemException
   {
      txManager.rollback(); // rollback global tx
   }

   /**
    * Apply action in new XA transaction (begin then commit or rollback). 
    * Action will runs in dedicated XA transaction, i.e. only JCR storage stuff will be involved to.
    * But if it's a nested isolated operation it will run the action assuming the same (current, active) transaction.
    * 
    * @throws RepositoryException if error occurs 
    */
   //   * @throws SystemException if XA unexpected error
   //   * @throws NotSupportedException if unsupported XA operation
   //   * @throws HeuristicRollbackException if all relevant updates have been rolled back on commit
   //   * @throws HeuristicMixedException if some relevant updates have been committed and others have been rolled back on commit
   //   * @throws RollbackException rollback performed
   //   * @throws IllegalStateException if thread is not associated with a transaction
   //   * @throws SecurityException if thread is not allowed to commit the transaction   
   protected void txAction() throws RepositoryException
   {
      final boolean actInTx = currentIsolated.get() == null;
      if (actInTx)
      {
         // it's rooted isolated operation
         boolean rollback = true;
         try
         {
            beginTx();

            // remember current isolated transaction for this thread
            Transaction current = txManager.getTransaction();
            if (current != null)
            {
               currentIsolated.set(current);
            }

            action();

            try
            {
               commitTx();
            }
            catch (RollbackException e)
            {
               // Indicate that the transaction has been rolled back rather than committed.
               // TODO throw new RepositoryException(e);
               LOG.error("Transaction has been rolled back", e);
            }
            catch (HeuristicRollbackException e)
            {
               // if all relevant updates have been rolled back on commit
               // TODO throw new RepositoryException(e);
               LOG.error("Relevant updates have been rolled back", e);
            }
            catch (HeuristicMixedException e)
            {
               // if some relevant updates have been committed and others have been rolled back on commit
               // TODO partial commit - got inconsistency. rollback not possible?
               // doRollback();
               // TODO throw new RepositoryException(e);
               LOG.error("Some relevant updates have been committed and others have been rolled back", e);
            }
            catch (IllegalStateException e)
            {
               // if thread is not associated with a transaction
               // TODO can we do rollback if not in the tx thread?
               // doRollback();
               // TODO throw new RepositoryException(e);
               LOG.error("Commit impossible, thread is not associated with the transaction", e);
            }
            catch (SecurityException e)
            {
               // if thread is not allowed to commit the transaction
               // TODO can we do the rollback, will it have a rights?
               // doRollback();               
               // TODO throw new RepositoryException(e);
               LOG.error("Commit impossible, thread is not allowed to commit the transaction", e);
            }
            catch (SystemException e)
            {
               // if XA unexpected error
               // TODO rollback not possible?
               // doRollback();
               // TODO throw new RepositoryException(e);
               LOG.error("Commit impossible dur to unexpected XA error", e);
            }
            finally
            {
               rollback = false;
            }
         }
         catch (NotSupportedException e)
         {
            // if unsupported XA operation: nested transaction
            rollback = false;
            doRollback();
            throw new RepositoryException(e);
         }
         catch (SystemException e)
         {
            // if XA unexpected error on begin or get transaction
            rollback = false;
            doRollback();
            throw new RepositoryException("Unexpected error on begin or get of a transaction", e);
         }
         finally
         {
            if (rollback)
            {
               doRollback();
            }

            // remove current isolated transaction from this thread
            currentIsolated.remove();
         }
      }
      else
      {
         // it's nested isolated operation
         action();
      }
   }

   /**
    * Performs rollback of the action.
    */
   private void doRollback()
   {
      try
      {
         rollbackTx();
      }
      catch (Exception e1)
      {
         LOG.error("Rollback error ", e1);
      }
   }

   /**
    * Apply the action in new XA transaction. Action should run in dedicated XA transaction,
    * i.e. only JCR storage stuff should be involved to.
    */
   public void perform() throws RepositoryException
   {
      try
      {
         // Care about dedicated XA transaction for storage save:
         // suspend current ransaction and create one new for the JCR storage (cache etc.)
         // after the new transaction done we'll resume the current.

         Transaction current = txManager.suspend();
         Throwable actionError = null; // used for resume errors handling
         try
         {
            txAction();
         }
         //         catch (RollbackException e)
         //         {
         //            // Indicate that the transaction has been rolled back rather than committed.
         //            throw new RepositoryException(actionError = e);
         //         }
         catch (JCRInvalidItemStateException e)
         {
            throw new JCRInvalidItemStateException(e.getMessage(), e.getIdentifier(), e.getState(), actionError = e);
         }
         catch (InvalidItemStateException e)
         {
            throw new InvalidItemStateException(actionError = e);
         }
         catch (ItemExistsException e)
         {
            throw new ItemExistsException(actionError = e);
         }
         catch (ReadOnlyWorkspaceException e)
         {
            throw new ReadOnlyWorkspaceException(actionError = e);
         }
         catch (RepositoryException e)
         {
            throw new RepositoryException(actionError = e);
         }
         catch (Throwable e)
         {
            throw new RepositoryException(actionError = e);
         }
         finally
         {
            if (current != null)
            {
               try
               {
                  txManager.resume(current);
               }
               catch (InvalidTransactionException e)
               {
                  if (actionError == null)
                  {
                     throw new RepositoryException("Error of Transaction resume", e);
                  }
                  else
                  {
                     LOG.error("Error of Transaction resume", e);
                  }
               }
               catch (IllegalStateException e)
               {
                  if (actionError == null)
                  {
                     throw new RepositoryException("Error of Transaction resume", e);
                  }
                  else
                  {
                     LOG.error("Error of Transaction resume", e);
                  }
               }
               catch (SystemException e)
               {
                  if (actionError == null)
                  {
                     throw new RepositoryException("Error of Transaction resume", e);
                  }
                  else
                  {
                     LOG.error("Error of Transaction resume", e);
                  }
               }
            }
         }
      }
      catch (SystemException e)
      {
         throw new RepositoryException("Error of Transaction suspend", e);
      }
   }
}
