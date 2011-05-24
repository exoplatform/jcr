Summary

    Status: Check in DefaultChangesFilter if we use the right ids in case of a IOException while updating the index of the parentSearchManager
    CCP Issue: N/A, Product Jira Issue: JCR-1577.
    Complexity: N/A

The Proposal
Problem description

What is the problem to fix?
In the method DefaultChangesFilter.doUpdateIndex(Set<String> removedNodes, Set<String> addedNodes, Set<String> parentRemovedNodes,
Set<String> parentAddedNodes), I see
try
      {
         parentSearchManager.updateIndex(parentRemovedNodes, parentAddedNodes);
      }
      catch (RepositoryException e)
      {
         log.error("Error indexing changes " + e, e);
      }
      catch (IOException e)
      {
         log.error("Error indexing changes " + e, e);
         try
         {
            parentHandler.logErrorChanges(removedNodes, addedNodes);
         }
         catch (IOException ioe)
         {
            log.warn("Exception occure when errorLog writed. Error log is not complete. " + ioe, ioe);
         }
      }

shouldn't it be:
try
      {
         parentSearchManager.updateIndex(parentRemovedNodes, parentAddedNodes);
      }
      catch (RepositoryException e)
      {
         log.error("Error indexing changes " + e, e);
      }
      catch (IOException e)
      {
         log.error("Error indexing changes " + e, e);
         try
         {
            parentHandler.logErrorChanges(parentRemovedNodes, parentAddedNodes);
         }
         catch (IOException ioe)
         {
            log.warn("Exception occure when errorLog writed. Error log is not complete. " + ioe, ioe);
         }
      }
Fix description

How is the problem fixed?

    Fixed by passing correct lists of added and removed nodes on Exception to queryHandler.logErrorChanges(List, List);

Patch information: JCR-1577.patch

Tests to perform

Reproduction test
    none;

Tests performed at DevLevel

    Full set of eXo and TCK test with manual tests in cluster;

Tests performed at QA/Support Level
*

Documentation changes

Documentation changes:
    none;

Configuration changes

Configuration changes:
    none;

Will previous configuration continue to work?
    yes;

Risks and impacts

Can this bug fix have any side effects on current client projects?
    none;

Is there a performance risk/cost?
    no;

Validation (PM/Support/QA)

PM Comment
* PL review: patch validated

Support Comment
* Support review: patch validated

QA Feedbacks
*

