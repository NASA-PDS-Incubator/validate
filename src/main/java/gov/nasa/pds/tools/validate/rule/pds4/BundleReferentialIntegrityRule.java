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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import gov.nasa.pds.tools.label.ExceptionType;
import gov.nasa.pds.tools.util.LabelUtil;
import gov.nasa.pds.tools.util.ReferentialIntegrityUtil;
import gov.nasa.pds.tools.util.Utility;
import gov.nasa.pds.tools.util.XMLExtractor;
import gov.nasa.pds.tools.validate.Identifier;
import gov.nasa.pds.tools.validate.ProblemDefinition;
import gov.nasa.pds.tools.validate.ProblemType;
import gov.nasa.pds.tools.validate.Target;
import gov.nasa.pds.tools.validate.ValidationProblem;
import gov.nasa.pds.tools.validate.rule.AbstractValidationRule;
import gov.nasa.pds.tools.validate.rule.GenericProblems;
import gov.nasa.pds.tools.validate.rule.ValidationTest;
import net.sf.saxon.tree.tiny.TinyNodeImpl;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

/**
 * Validation rule that performs referential integrity checking on
 * a Product_Bundle product label.
 * 
 * @author mcayanan
 *
 */
public class BundleReferentialIntegrityRule extends AbstractValidationRule {
  private static final Logger LOG = LoggerFactory.getLogger(BundleReferentialIntegrityRule.class);

  private static final Pattern BUNDLE_LABEL_PATTERN = 
      Pattern.compile(".*bundle.*\\.xml", Pattern.CASE_INSENSITIVE);

  private static final Pattern COLLECTION_LABEL_PATTERN = 
      Pattern.compile(".*collection.*\\.xml", Pattern.CASE_INSENSITIVE);
  
  private static final String PRODUCT_CLASS =
      "//*[starts-with(name(),'Identification_Area')]/product_class";
  
  /** XPath to grab the Member_Entry tags in a bundle. */
  private static final String BUNDLE_MEMBER_ENTRY = "//Bundle_Member_Entry";
  
  /** The member status XPath in an Inventory file. */
  private static final String MEMBER_STATUS = "member_status";

  /** The LID-VID or LID XPath for an association. */
  private static final String IDENTITY_REFERENCE =
      "lidvid_reference | lid_reference";
  
  /**
   * XPath to the logical identifier.
   */
  private static final String LOGICAL_IDENTIFIER =
      "//*[starts-with(name(),'Identification_Area')]/logical_identifier";

  /**
   * XPath to the version id.
   */
  private static final String VERSION_ID =
      "//*[starts-with(name(),'Identification_Area')]/version_id";

  private double totalTimeElapsed = 0.0;
  private ArrayList<String> logicalIdentifiersCumulative = new ArrayList<String>(0);
  private ArrayList<String> lidOrLidVidReferencesCumulative = new ArrayList<String>(0);
  private ArrayList<URL> lidOrLidVidReferencesCumulativeFileNames = new ArrayList<URL>(0); // This array and lidOrLidVidReferencesCumulative should have the same size 
  private String bundleBaseID = null; 
  private HashMap<String, String> lidOrLidvidReferenceToLogicalIdentifierMap =  new HashMap<String, String>(); // A map to allow getting a logical identifier from lid_reference or lidvid_reference.
  
  @Override
  public boolean isApplicable(String location) {
    if (Utility.isDir(location)) {
      return true;
    } else {
      return false;
    }
  }

  @ValidationTest
  public void bundleReferentialIntegrityRule() {

    try {
      List<Target> children = getContext().getCrawler().crawl(getTarget());
      LOG.debug("bundleReferentialIntegrityRule:getTarget() {}",getTarget());
      LOG.debug("bundleReferentialIntegrityRule:children.size():afor_reduced: {}",children.size());

      // Check for bundle(.*)?\.xml file.
      for (Target child : children) {
        Matcher matcher = BUNDLE_LABEL_PATTERN.matcher(
            FilenameUtils.getName(child.toString()));
        //LOG.debug("bundleReferentialIntegrityRule:child.toString() {}",child.toString());
        LOG.debug("bundleReferentialIntegrityRule:FilenameUtils.getName(child.toString()) {}",FilenameUtils.getName(child.toString()));
        if (matcher.matches()) {
          try {
            XMLExtractor extractor = new XMLExtractor(child.getUrl());
            if("Product_Bundle".equals(
                extractor.getValueFromDoc(PRODUCT_CLASS))) {
              String lid = extractor.getValueFromDoc(LOGICAL_IDENTIFIER);
              String vid = extractor.getValueFromDoc(VERSION_ID);
              // For bundles, set a reference to itself.
              getRegistrar().addIdentifierReference(child.getUrl().toString(), 
                  new Identifier(lid, vid));
              getListener().addLocation(child.getUrl().toString());
              getBundleMembers(child.getUrl());
              break;
            }
          } catch (Exception e) {
            //Ignore. This isn't a valid Bundle label, so let's skip it.
          }
        }
      }
    } catch (IOException io) {
      reportError(GenericProblems.UNCAUGHT_EXCEPTION, getTarget(), -1, -1,
          io.getMessage());
    } catch (Exception ex) {
      reportError(GenericProblems.UNCAUGHT_EXCEPTION, getTarget(), -1, -1,
          ex.getMessage());
    }


    // https://github.com/NASA-PDS/validate/issues/308
    // As a user, I want to check that all Internal References are valid references to other PDS4 products within the current validating bundle
    //
    // Report the integrity of the references if they point to any local identifiers in this bundle.
    //

    // Use the refactored functions in ReferentialIntegrityUtil class.
    ReferentialIntegrityUtil.initialize("bundle",getTarget(),getListener(),getContext());
    ReferentialIntegrityUtil.additionalReferentialIntegrityChecks(getTarget());
    ReferentialIntegrityUtil.reportLidOrLidvidReferenceToNonExistLogicalReferences();

    // https://github.com/NASA-PDS/validate/issues/69
    // As a user, I want to validate that all context objects specified in observational products are referenced in the parent bundle/collection Reference_List
    //
    // For every references in the Context_Area, check if it also occur in the bundle/collection Reference_List,
    //  i.e: All context objects specified in observational are referenced in the parent bundle/collection Reference_List 
    //
    // Use the refactored function(s) in ReferentialIntegrityUtil class.

    ReferentialIntegrityUtil.reportContextReferencesUnreferenced();

  }
  
  private void getBundleMembers(URL bundle) {
    LOG.info("getBundleMembers:BEGIN_PROCESSING_BUNDLE:bundle {}",bundle);
    long startTime = System.currentTimeMillis();
    try {
      XMLExtractor extractor = new XMLExtractor(bundle);
      List<TinyNodeImpl> nodes = extractor.getNodesFromDoc(
          BUNDLE_MEMBER_ENTRY);
      for (TinyNodeImpl node : nodes) {
        String reference = extractor.getValueFromItem(
            IDENTITY_REFERENCE,
            node);
        String memberStatus = extractor.getValueFromItem(
            MEMBER_STATUS,
            node);
        Identifier id = parseIdentifier(reference);
        LOG.debug("getBundleMembers:reference,memberStatus,id {},{},{}",reference,memberStatus,id);
        List<Map.Entry<Identifier, String>> matchingMembers = 
            new ArrayList<Map.Entry<Identifier, String>>();
        for (Map.Entry<Identifier, String> idEntry : 
          getRegistrar().getIdentifierDefinitions().entrySet()) {
          LOG.debug("getBundleMembers:reference,memberStatus,id,idEntry.getKey()) {},{},{},{}",reference,memberStatus,id,idEntry.getKey());
          if (id.equals(idEntry.getKey())) {
            matchingMembers.add(idEntry);
            LOG.debug("getBundleMembers:ADDING_IDENTRY:reference,memberStatus,id,idEntry.getKey()) {},{},{},{}",reference,memberStatus,id,idEntry.getKey());
          }
        }
        if (matchingMembers.isEmpty() && 
            "Primary".equalsIgnoreCase(memberStatus)) {
          LOG.debug("getBundleMembers:MATCHING_MEMBER_ID_IS_EMPTY {}",id);
          getListener().addProblem(new ValidationProblem(new ProblemDefinition(
              ExceptionType.WARNING,
              ProblemType.MEMBER_NOT_FOUND,
              "The member '" + id + "' could not be found in "
                  + "any product within the given target."), 
              bundle));
        } else if (matchingMembers.size() == 1) {
          String parentLid = extractor.getValueFromDoc(LOGICAL_IDENTIFIER);
          super.verifyLidPrefix(id.getLid(), parentLid, memberStatus, bundle);
          getListener().addProblem(new ValidationProblem(new ProblemDefinition(
              ExceptionType.INFO,
              ProblemType.MEMBER_FOUND,
              "The member '" + id + "' is identified in "
                  + "the following product: "
                  + matchingMembers.get(0).getValue()), 
              bundle));
        } else if (matchingMembers.size() > 1) {
          String parentLid = extractor.getValueFromDoc(LOGICAL_IDENTIFIER);
          super.verifyLidPrefix(id.getLid(), parentLid, memberStatus, bundle);
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
                            + "': " + matchingIds.get(matchingId).toString()),
                    bundle));
                foundDuplicates = true;
              }
            }
            if (!foundDuplicates) {
              List<String> targets = new ArrayList<String>();
              for (Map.Entry<Identifier, String> m : matchingMembers) {
                targets.add(m.getValue());
              }
              getListener().addProblem(new ValidationProblem(
                  new ProblemDefinition(
                      ExceptionType.INFO,
                      ProblemType.DUPLICATE_MEMBERS_INFO,
                      "The member '" + id + "' is identified "
                          + "in multiple proudcts: " + targets.toString()),
                  bundle));
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
                        + "in multiple proudcts: " + targets.toString()),
                bundle));                
          }
        }
        getRegistrar().addIdentifierReference(bundle.toString(), id);
      }
    } catch (Exception e) {
      reportError(GenericProblems.UNCAUGHT_EXCEPTION, bundle, -1, -1, 
          e.getMessage());
    }
    long finishTime = System.currentTimeMillis();
    long timeElapsed = finishTime - startTime;
    totalTimeElapsed += timeElapsed;

    LOG.info("getBundleMembers:END_PROCESSING_BUNDLE:bundle,totalTimeElapsed {},{}",bundle,totalTimeElapsed/1000.0);
  }
  
  private Identifier parseIdentifier(String identifier) {
    if (identifier.indexOf("::") != -1) {
      return new Identifier(identifier.split("::")[0],
          identifier.split("::")[1]);
    } else {
      return new Identifier(identifier.split("::")[0]);
    }
  }
  
  private Map<String, List<String>> findMatchingIds(List<Map.Entry<Identifier, 
      String>> products) {
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
