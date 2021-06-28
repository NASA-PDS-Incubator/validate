// Copyright 2006-2018, by the California Institute of Technology.
// ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
// Any commercial use must be negotiated with the Office of Technology Transfer
// at the California Institute of Technology.
//
// This software is subject to U. S. export control laws and regulations
// (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the extent that the software
// is subject to U.S. export control laws and regulations, the recipient has
// the responsibility to obtain export licenses or other export authority as
// may be required before exporting such information to foreign countries or
// providing access to foreign nationals.
//
// $Id$
package gov.nasa.pds.tools.validate.rule.pds4;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.pds.tools.inventory.reader.InventoryEntry;
import gov.nasa.pds.tools.inventory.reader.InventoryReaderException;
import gov.nasa.pds.tools.inventory.reader.InventoryTableReader;
import gov.nasa.pds.tools.label.ExceptionType;
import gov.nasa.pds.tools.util.ReferentialIntegrityUtil;
import gov.nasa.pds.tools.util.Utility;
import gov.nasa.pds.tools.util.XMLExtractor;
import gov.nasa.pds.tools.validate.Identifier;
import gov.nasa.pds.tools.validate.ProblemDefinition;
import gov.nasa.pds.tools.validate.ProblemType;
import gov.nasa.pds.tools.validate.Target;
import gov.nasa.pds.tools.validate.ValidationProblem;
import gov.nasa.pds.tools.validate.crawler.Crawler;
import gov.nasa.pds.tools.validate.rule.AbstractValidationRule;
import gov.nasa.pds.tools.validate.rule.GenericProblems;
import gov.nasa.pds.tools.validate.rule.ValidationTest;

import gov.nasa.pds.validate.constants.Constants;

/**
 * Validation rule that performs referential integrity checking
 * on a Product_Collection product label.
 * 
 * @author mcayanan
 *
 */
public class CollectionReferentialIntegrityRule extends AbstractValidationRule {
  private static final Logger LOG = LoggerFactory.getLogger(CollectionReferentialIntegrityRule.class);
  
  private static final String PRODUCT_CLASS =
      "//*[starts-with(name(),'Identification_Area')]/product_class";
  
  private static final String LOGICAL_IDENTIFIER =
      "//*[starts-with(name(),'Identification_Area')]/logical_identifier";

  private String lid = null;
  private double totalTimeElapsed = 0.0;
  
  @Override
  public boolean isApplicable(String location) {
    if (Utility.isDir(location)) {
      return true;
    } else {
      return false;
    }
  }

  @ValidationTest
  public void collectionReferentialIntegrityRule() {
    Crawler crawler = getContext().getCrawler();
    try {
      IOFileFilter regexFileFilter = new RegexFileFilter(Constants.COLLECTION_LABEL_PATTERN);
      //List<Target> children = crawler.crawl(getTarget(), regexFileFilter);
      // Note: For some strange reason, the crawler goes into an infinite loop using the above call
      //       so we will use an alternate call to get the list of collection files.
      String[] extensions = new String[1];
      extensions[0] = Constants.LABEL_EXTENSION; // Note that the extension does not contain the dot.
      List<Target> children = crawler.crawl(getTarget(), extensions, false, Constants.COLLECTION_NAME_TOKEN);

      LOG.debug("collectionReferentialIntegrityRule:getTarget(),children.size() {},{}",getTarget(),children.size());

      // Check for collection(_.*)?\.xml file.
      for (int i = 0; i < children.size(); i++) {
        Target child = children.get(i);
        if (child.isDir()) {
          children.addAll(crawler.crawl(child.getUrl(), regexFileFilter));
        } else {
          try {
            XMLExtractor extractor = new XMLExtractor(child.getUrl());
            if("Product_Collection".equals(
                extractor.getValueFromDoc(PRODUCT_CLASS))) {
              getListener().addLocation(child.getUrl().toString());
              this.lid = extractor.getValueFromDoc(LOGICAL_IDENTIFIER);
              getCollectionMembers(child.getUrl());
            }
          } catch (Exception e) {
            //Ignore. This isn't a valid Collection label, so let's skip it.
          } 
        }
      }
    } catch (IOException io) {
      reportError(GenericProblems.UNCAUGHT_EXCEPTION, getTarget(), -1, -1, 
          io.getMessage());
    }

    // https://github.com/NASA-PDS/validate/issues/69
    // As a user, I want to validate that all context objects specified in observational products are referenced in the parent bundle/collection Reference_List
    //
    // For every references in the Context_Area, check if it also occur in the bundle/collection Reference_List,
    //  i.e: All context objects specified in observational are referenced in the parent bundle/collection Reference_List 
    //

    ReferentialIntegrityUtil.initialize("collection",getTarget(),getListener(),getContext());
    ReferentialIntegrityUtil.additionalReferentialIntegrityChecks(getTarget());

    // Report any references declared in labels but not referenced in the parent bundle.
    ReferentialIntegrityUtil.reportContextReferencesUnreferenced();

  }
  
  private void getCollectionMembers(URL collection) {
    LOG.info("getCollectionMembers: BEGIN_PROCESSING_COLLECTION:collection {}",collection);
    long startTime = System.currentTimeMillis();
    try {
      int numOfCollectionMembers = 0;

      InventoryTableReader reader = new InventoryTableReader(collection);
      for (InventoryEntry entry = new InventoryEntry(); entry != null;) {
        if (!entry.isEmpty()) {
            numOfCollectionMembers++;
          String identifier = entry.getIdentifier();

          LOG.debug("getCollectionMembers:getTarget,collection,numOfCollectionMembers {},{},{}",getTarget(),collection,numOfCollectionMembers);

          LOG.debug("getCollectionMembers: numOfCollectionMembers {}",numOfCollectionMembers);
          LOG.debug("getCollectionMembers: identifier,getTarget(),collection {},{},{}",identifier,getTarget(),collection);
          if (!identifier.equals("")) {
            //Check for a LID or LIDVID
            Identifier id = parseIdentifier(identifier);
            LOG.debug("getCollectionMembers: id {}",id);
            LOG.debug("getCollectionMembers: id,id.hasVersion(),id.getVersion() {},{},{}",id,id.hasVersion(),id.getVersion());
            LOG.debug("getCollectionMembers: id,id.hasVersion(),collection {},{},{}",id,id.hasVersion(),collection);

            // https://github.com/NASA-PDS/validate/issues/230
            // New requirement: The 'P' entry must be a LIDVID (logical identifier and version id separated by '::').
            // Report as error if not a LIDVID.
            if ("P".equalsIgnoreCase(entry.getMemberStatus())) {
                // If identifier has no version, it is not a LIDVID and should be flagged as an error with the new type ProblemType.MISSING_VERSION.
                if (!id.hasVersion()) {
                    getListener().addProblem(new ValidationProblem(
                        new ProblemDefinition(ExceptionType.ERROR,
                            ProblemType.MISSING_VERSION,
                            "The primary member '" + id + "' should include the version number"),
                            collection));
                }
            }

            List<Map.Entry<Identifier, String>> matchingMembers = 
                new ArrayList<Map.Entry<Identifier, String>>();
            for (Map.Entry<Identifier, String> idEntry : 
              getRegistrar().getIdentifierDefinitions().entrySet()) {
              if (id.equals(idEntry.getKey())) {
                matchingMembers.add(idEntry);
              }
            }
            LOG.debug("getCollectionMembers: id,matchingMembers.size() {},{}",id,matchingMembers.size());
            LOG.debug("getCollectionMembers: id,matchingMembers.isEmpty(),entry.getMemberStatus() {},{},{}",id,matchingMembers.isEmpty(),entry.getMemberStatus());
            if (matchingMembers.isEmpty() && 
                "P".equalsIgnoreCase(entry.getMemberStatus())) {
              getListener().addProblem(new ValidationProblem(
                  new ProblemDefinition(ExceptionType.WARNING,
                      ProblemType.MEMBER_NOT_FOUND,
                      "The member '" + id + "' could not be found in "
                          + "any product within the given target."), 
                      collection));
            } else if (matchingMembers.size() == 1) {
              super.verifyLidPrefix(id.getLid(), this.lid, entry.getMemberStatus(),
            	      collection);
              getListener().addProblem(new ValidationProblem(
                  new ProblemDefinition(ExceptionType.INFO,
                      ProblemType.MEMBER_FOUND,
                      "The member '" + id + "' is identified in "
                          + "the following product: "
                          + matchingMembers.get(0).getValue()), 
                      collection));
              //LOG.debug("getCollectionMembers: id {} SUCCESS",id);
              //LOG.debug("getCollectionMembers: id {} SUCCESS: {}",id,"The member '" + id + "' is identified in "
              //                                                + "the following product: "
              //                                                + matchingMembers.get(0).getValue());
            } else if (matchingMembers.size() > 1) {
              super.verifyLidPrefix(id.getLid(), this.lid, entry.getMemberStatus(),
            	      collection);
              ExceptionType exceptionType = ExceptionType.ERROR;
              if (!id.hasVersion()) {
                Map<String, List<String>> matchingIds = 
                    findMatchingIds(matchingMembers);
                boolean foundDuplicates = false;
                for (String matchingId : matchingIds.keySet()) {
                  if (matchingIds.get(matchingId).size() > 1) {
                    getListener().addProblem(new ValidationProblem(
                        new ProblemDefinition(exceptionType,
                            ProblemType.DUPLICATE_VERSIONS,
                            "The member '" + id + "' is identified "
                                + "in multiple products, but with the same "
                                + "version id '" + matchingId.split("::")[1]
                                + "': "
                                + matchingIds.get(matchingId).toString()),
                       collection));
                    foundDuplicates = true;
                  }
                }
                if (!foundDuplicates) {
                  List<String> targets = new ArrayList<String>();
                  for (Map.Entry<Identifier, String> m : matchingMembers) {
                    targets.add(m.getValue());
                  }
                  getListener().addProblem(new ValidationProblem(
                      new ProblemDefinition(ExceptionType.INFO,
                          ProblemType.DUPLICATE_MEMBERS_INFO,
                          "The member '" + id + "' is identified "
                              + "in multiple products: " + targets.toString()),
                      collection));
                }
              } else {
                List<String> targets = new ArrayList<String>();
                for (Map.Entry<Identifier, String> m : matchingMembers) {
                  targets.add(m.getValue());
                }
                getListener().addProblem(new ValidationProblem(
                    new ProblemDefinition(exceptionType,
                        ProblemType.DUPLICATE_MEMBERS,
                        "The member '" + id + "' is identified "
                            + "in multiple products: " + targets.toString()),
                    collection));
              }
            }
            getRegistrar().addIdentifierReference(collection.toString(), id);
          }
        }
        entry = reader.getNext();
      }
      int records = reader.getNumRecords();
      LOG.debug("getCollectionMembers: collection,numOfCollectionMembers,records {},{},{}",collection,numOfCollectionMembers,records);

      if (numOfCollectionMembers>0 && records>0 && numOfCollectionMembers!=records) {
    	  String message = "Number of records read is not equal "
                  + "to the defined number of records in the collection (expected "
                  + records + ", got " + numOfCollectionMembers + ").";
    	  getListener().addProblem(new ValidationProblem(
                  new ProblemDefinition(ExceptionType.ERROR,
                      ProblemType.RECORDS_MISMATCH,
                      message),
                  collection));
      }
      long finishTime = System.currentTimeMillis();
      long timeElapsed = finishTime - startTime;
      totalTimeElapsed += timeElapsed;

      LOG.info("getCollectionMembers: END_PROCESSING_COLLECTION:collection,totalTimeElapsed,numOfCollectionMembers {},{},{}",collection,totalTimeElapsed,numOfCollectionMembers);
    } catch (InventoryReaderException e) {
      reportError(GenericProblems.UNCAUGHT_EXCEPTION, collection, -1, -1,
          e.getMessage());
    }
  }
  
  private Identifier parseIdentifier(String identifier) {
    // Even though the below identifier contains errors (blanks around '::' and too many digits)
    // and may be caught by other validation rules, this function need to remove any leading or trailing blanks from
    // tokens after the split() function:
    //
    //     [urn:nasa:pds:cocirs_c2h4abund:data_derived:c2h4_abund_profiles :: 1.2.3.4]`

    if (identifier.indexOf("::") != -1) {
      return new Identifier(identifier.split("::")[0].trim(),
          identifier.split("::")[1].trim());
    } else {
      return new Identifier(identifier.split("::")[0].trim());
    }
  }
  
  private Map<String, List<String>> findMatchingIds(
      List<Map.Entry<Identifier, String>> products) {
    Map<String, List<String>> results = new HashMap<String, List<String>>();
    for (Map.Entry<Identifier, String> product : products) {
      if (results.get(product.getKey().toString()) != null) {
        List<String> targets = results.get(product.getKey().toString());
        targets.add(product.getValue());
      } else {
        List<String> targets = new ArrayList<String>();
        targets.add(product.getValue());
        results.put(product.getKey().toString(), targets);
      }
    }
    return results;
  }
  
}
