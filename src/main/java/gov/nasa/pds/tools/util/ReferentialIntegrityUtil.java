// Copyright 2021, by the California Institute of Technology.
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
package gov.nasa.pds.tools.util;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import gov.nasa.pds.tools.label.ExceptionType;
import gov.nasa.pds.tools.util.LabelUtil;
import gov.nasa.pds.tools.util.Utility;
import gov.nasa.pds.tools.validate.Identifier;
import gov.nasa.pds.tools.validate.ProblemDefinition;
import gov.nasa.pds.tools.validate.ProblemType;
import gov.nasa.pds.tools.validate.Target;
import gov.nasa.pds.tools.validate.ProblemListener;
import gov.nasa.pds.tools.validate.ValidationProblem;
import gov.nasa.pds.tools.validate.ValidationTarget;
import gov.nasa.pds.tools.validate.rule.GenericProblems;
import gov.nasa.pds.tools.validate.rule.RuleContext;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

/**
 * Util class to provide additional integrity checks on
 * a Product_Bundle or Product_Collection label. 
 *
 */
public class ReferentialIntegrityUtil {
  private static final Logger LOG = LoggerFactory.getLogger(ReferentialIntegrityUtil.class);
  private static final Pattern BUNDLE_LABEL_PATTERN = 
      Pattern.compile(".*bundle.*\\.xml", Pattern.CASE_INSENSITIVE);

  private static final Pattern COLLECTION_LABEL_PATTERN = 
      Pattern.compile(".*collection.*\\.xml", Pattern.CASE_INSENSITIVE);
  
  private static final String PRODUCT_CLASS =
      "//*[starts-with(name(),'Identification_Area')]/product_class";

  private static final String MESSAGE_TO_DISABLE_WARNINGS = " (Disable with --skip-context-reference-check flag)";
  
  private static ArrayList<URL> urlsParsedCumulative = new ArrayList<URL>(0);
  private static ArrayList<String> logicalIdentifiersCumulative = new ArrayList<String>(0);
  private static ArrayList<String> lidOrLidVidReferencesCumulative = new ArrayList<String>(0);
  private static HashMap<String,HashSetReferenceInfo> contextReferencesCumulative = new HashMap<String,HashSetReferenceInfo>(0); // Collect all references defined in "Context_Area" tag from all labels.
  private static HashMap<String,HashSet> bundleOrCollectionReferenceMap = new HashMap<String,HashSet>(); // Collect all references defined in "Reference_List" tag from a label that is a bundle.
  private static HashMap<String,HashSetReferenceInfo> bundleReferenceMap = new HashMap<String,HashSetReferenceInfo>(); // Collect all references defined in "Context_Area" and "Reference_List" tag from a label that is a bundle.
  private static HashMap<String,HashSetReferenceInfo> collectionReferenceMap = new HashMap<String,HashSetReferenceInfo>(); // Collect all references defined in "Context_Area" and "Reference_List" tag from a label that is a collection. 
  private static ArrayList<URL> lidOrLidVidReferencesCumulativeFileNames = new ArrayList<URL>(0); // This array and lidOrLidVidReferencesCumulative should have the same size 
  private static String bundleBaseID = null; 
  private static HashMap<String, String> lidOrLidvidReferenceToLogicalIdentifierMap =  new HashMap<String, String>(); // A map to allow getting a logical identifier from lid_reference or lidvid_reference.
  private static HashMap<String,URL> bundleURLMap = new HashMap<String,URL>();

  private static URL target = null;
  private static ProblemListener problemListener = null;
  private static RuleContext ruleContext = null;
  private static String [] VALID_REFERENCE_TYPES = {"bundle","collection"};

  // The referenceType determines which label the Reference_List is collected from file name that contains BUNDLE_LABEL_PATTERN or COLLECTION_LABEL_PATTERN.
  private static String referenceType = "";   // Possible values are defined in VALID_REFERENCE_TYPES

  private static boolean contextReferenceCheck = true;  // By default, this class will collect all references and check them from the context area.
    // Used in parsing for "_reference" tags.
  private static String[] tagsList = new String[2];

  /**
   * Initialize this class to ready for doing referential checks.
   * @param referenceType the referenceType of the target of the check: 'bundle' or 'collection'
   * @param target the URL of the target of the check 
   * @param problemListener the ProblemListener of the target of the check
   * @param ruleContext the RuleContext of the target of the check 
   * @return None
   */
  public static void initialize(String referenceType,
                                URL target,
                                ProblemListener problemListener,
                                RuleContext ruleContext) {
      LOG.debug("initialize:referenceType,target [{}],{}",referenceType,target);
      ReferentialIntegrityUtil.setReferenceType(referenceType);
      ReferentialIntegrityUtil.setTarget(target);
      ReferentialIntegrityUtil.setListener(problemListener);
      ReferentialIntegrityUtil.setContext(ruleContext);
      ReferentialIntegrityUtil.tagsList[0] = LabelUtil.LIDVID_REFERENCE;
      ReferentialIntegrityUtil.tagsList[1] = LabelUtil.LID_REFERENCE;
  }


  /**
   * Reset this class to its initial state in case running from regression tests.
   * @return None
   */
  public static void reset() {
      // Make sure all composite arrays/maps defined in this class are clear otherwise regression tests will fail,
      // and it will be difficult to figure out why.  The code may work when validate runs from the command line
      // but not in regression test.
      ReferentialIntegrityUtil.logicalIdentifiersCumulative.clear(); 
      ReferentialIntegrityUtil.lidOrLidVidReferencesCumulative.clear();
      ReferentialIntegrityUtil.contextReferencesCumulative.clear();
      ReferentialIntegrityUtil.bundleOrCollectionReferenceMap.clear();
      ReferentialIntegrityUtil.bundleReferenceMap.clear();
      ReferentialIntegrityUtil.collectionReferenceMap.clear();
      ReferentialIntegrityUtil.lidOrLidVidReferencesCumulativeFileNames.clear();
      ReferentialIntegrityUtil.bundleBaseID = null;
      ReferentialIntegrityUtil.lidOrLidvidReferenceToLogicalIdentifierMap.clear();
      ReferentialIntegrityUtil.bundleURLMap.clear();
      ReferentialIntegrityUtil.urlsParsedCumulative.clear();
  }

  /**
   * Set the contextReferenceCheck
   * @return None
   */
  public static void setContextReferenceCheckFlag(boolean contextReferenceCheck) {
      LOG.debug("setContextReferenceCheckFlag:contextReferenceCheck {}",contextReferenceCheck);
      ReferentialIntegrityUtil.contextReferenceCheck = contextReferenceCheck;
  }

  /**
   * Get the contextReferenceCheck.
   * @return The valule of contextReferenceCheck
   */
  public static boolean getContextReferenceCheckFlag() {
      return(ReferentialIntegrityUtil.contextReferenceCheck);
  }

  /**
   * Get the URL of the target of the check.
   * @return the URL of the target of the check 
   */
  public static URL getTarget() {
      return(ReferentialIntegrityUtil.target);
  }

  /**
   * Set the URL of the target of the check.
   * @return None 
   */
  public static void setTarget(URL target) {
      LOG.debug("setTarget:target [{}]",target);
      ReferentialIntegrityUtil.target = target;
  }

  /**
   * Get the ProblemListener of the target of the check.
   * @return the ProblemListener of the target of the check 
   */
  public static ProblemListener getListener() {
      return(ReferentialIntegrityUtil.problemListener);
  }

  /**
   * Set the ProblemListener of the target of the check.
   * @param problemListener The ProblemListener of the target of the check
   * @return None 
   */
  public static void setListener(ProblemListener problemListener) {
      ReferentialIntegrityUtil.problemListener = problemListener;
  }

  /**
   * Get the RuleContext of the target of the check.
   * @return the RuleContext of the target of the check 
   */
  public static RuleContext getContext() {
     return(ReferentialIntegrityUtil.ruleContext);
  }

  /**
   * Get the RuleContext of the target of the check.
   * @parem ruleContext the RuleContext of the target of the chec
   * @return None
   */
  public static void setContext(RuleContext ruleContext) {
     ReferentialIntegrityUtil.ruleContext = ruleContext; 
  }

  /**
   * Get the referenceType of the target of the check.
   * @return the referenceType of the target of the check 
   */
  public static String getReferenceType() {
     return(ReferentialIntegrityUtil.referenceType);
  }

  /**
   * Set the referenceType of the target of the check.
   * @param referenceType the referenceType of the target of the check: 'bundle' or 'collection'
   * @return None 
   */
  public static void setReferenceType(String referenceType) {
      LOG.error("setReferenceType:afor:referenceType,ReferentialIntegrityUtil.referenceType [{}],[{}]",referenceType,ReferentialIntegrityUtil.referenceType);
      if (Arrays.asList(ReferentialIntegrityUtil.VALID_REFERENCE_TYPES).contains(referenceType)) {
          ReferentialIntegrityUtil.referenceType = referenceType; 
      } else {
          LOG.error("setReferenceType: Unrecognized value for referenceType {}, valid types are {}",referenceType,ReferentialIntegrityUtil.VALID_REFERENCE_TYPES);
      }
      LOG.error("setReferenceType:after:referenceType,ReferentialIntegrityUtil.referenceType [{}],[{}]",referenceType,ReferentialIntegrityUtil.referenceType);
  }

  private static boolean doesReferenceContainsVersion(String singleLidOrLidvidReference) {
      if (singleLidOrLidvidReference.contains("::")) {
          return(true);
      } else {
          return(false);
      }
  }

  private static void performReporting(String singleLidOrLidvidReference, boolean referenceIsLidvid, int indexToFilenames) {
      try {
          String message = "";
          URL url = ReferentialIntegrityUtil.lidOrLidVidReferencesCumulativeFileNames.get(indexToFilenames); // The warning message will be for this label.
          if (referenceIsLidvid) {
              message = "A LIDVID reference " + singleLidOrLidvidReference + " is referencing a logical identifier for a product not found in this " + ReferentialIntegrityUtil.getReferenceType();
          } else {
              message = "A LID reference " + singleLidOrLidvidReference + " is referencing a logical identifier for a product not found in this " + ReferentialIntegrityUtil.getReferenceType(); 
          }
          LOG.debug(message);

          // Build the ValidationProblem and add it to the report.
          ValidationProblem p1 = new ValidationProblem(new ProblemDefinition(ExceptionType.WARNING,
                                                                                 ProblemType.GENERAL_INFO, message),url);
          // Append the WARNING message to the report.
          getListener().addProblem(p1);

      } catch (Exception e) {
          e.printStackTrace();
      }
  }

  private static boolean bruteForceCheckForNonExistLogicalReferences(String singleLidOrLidvidReference) {
      // Given a LID or LIDVID reference, check to see if it is in logicalIdentifiersCumulative.
      // Because the reference may not contain the version, we must do a brute-force check since the logicalIdentifiersCumulative contains a version number.
      boolean referenceIsValid = false;

      for (String singleLogicalIdentifier : ReferentialIntegrityUtil.logicalIdentifiersCumulative) {
          LOG.debug("bruteForceCheckForNonExistLogicalReferences:singleLidOrLidvidReference,singleLogicalIdentifier {},{}",singleLidOrLidvidReference,singleLogicalIdentifier);
          if (singleLogicalIdentifier.contains(singleLidOrLidvidReference)) {
              referenceIsValid = true;
              LOG.debug("bruteForceCheckForNonExistLogicalReferences:singleLidOrLidvidReference,singleLogicalIdentifier,REFERENCE_IS_VALID {},{}",singleLidOrLidvidReference,singleLogicalIdentifier);
              break;
          }
      }
      return(referenceIsValid);
  }


  /**
   * Report a WARNING if any LID or LIDVID references does not resolve to at least one element in the list of logical identifiers.
   * @param validationRule The rule of the validation, e.g. pds4.label, pds4.bundle.  This value can be null since a rule is is not required within validate module.
   * @return None
   */
  public static void reportLidOrLidvidReferenceToNonExistLogicalReferences() {
      // After all the local_identifier and lid_reference or lidliv_reference tags are collected, they can be check if they are pointing to local identifier collected.

      LOG.debug("reportLidOrLidvidReferenceToNonExistLogicalReferences:ReferentialIntegrityUtil.lidOrLidVidReferencesCumulative.size() {}",ReferentialIntegrityUtil.lidOrLidVidReferencesCumulativeFileNames.size());
      try {
        int indexToFilenames = 0;
        for (String singleLidOrLidvidReference : ReferentialIntegrityUtil.lidOrLidVidReferencesCumulative) {
             LOG.debug("reportLidOrLidvidReferenceToNonExistLogicalReferences:VALIDATING_REFERENCE:singleLidOrLidvidReference,filename {},{}",singleLidOrLidvidReference,ReferentialIntegrityUtil.lidOrLidVidReferencesCumulativeFileNames.get(indexToFilenames));
            // It is possible that the reference does not contain a version, we must check for existence differently.
            if (!ReferentialIntegrityUtil.doesReferenceContainsVersion(singleLidOrLidvidReference)) {
                if (!ReferentialIntegrityUtil.bruteForceCheckForNonExistLogicalReferences(singleLidOrLidvidReference)) {

                    // We also need to check if the product is actually a product in the bundle.
                    // We should not throw a WARNING if the product does not belong to the bundle.
                    String filename = ReferentialIntegrityUtil.lidOrLidVidReferencesCumulativeFileNames.get(indexToFilenames).toString(); 
                    String logicalIdentifierPerLidReference = ReferentialIntegrityUtil.lidOrLidvidReferenceToLogicalIdentifierMap.get(singleLidOrLidvidReference);

                    LOG.debug("reportLidOrLidvidReferenceToNonExistLogicalReferences:filename,logicalIdentifierPerLidReference {},{}",filename,logicalIdentifierPerLidReference);

                    boolean productBelongToBundleFlag = ReferentialIntegrityUtil.isIdentiferMatchingBundleBaseID(logicalIdentifierPerLidReference);

                    // Only throw a WARNING if the product does belong to this bundle.
                    if (productBelongToBundleFlag == true) {
                        LOG.debug("reportLidOrLidvidReferenceToNonExistLogicalReferences:PRODUCT_IS_IN_BUNDLE:filename,logicalIdentifierPerLidReference {},{}",filename,logicalIdentifierPerLidReference);
                        ReferentialIntegrityUtil.performReporting(singleLidOrLidvidReference, false, indexToFilenames);
                    } else {
                        LOG.debug("reportLidOrLidvidReferenceToNonExistLogicalReferences:PRODUCT_NOT_IN_BUNDLE:filename,logicalIdentifierPerLidReference {},{}",filename,logicalIdentifierPerLidReference);
                    }
                } else {
                    LOG.debug("reportLidOrLidvidReferenceToNonExistLogicalReferences:LID_REFERENCE:singleLidOrLidvidReference {} is in logicalIdentifiersCumulative",singleLidOrLidvidReference);
                }
            } else {
                if (!ReferentialIntegrityUtil.logicalIdentifiersCumulative.contains(singleLidOrLidvidReference)) {

                    // We also need to check if the product is actually a product in the bundle.
                    // We should not throw a WARNING if the product does not belong to the bundle.
                    String filename = ReferentialIntegrityUtil.lidOrLidVidReferencesCumulativeFileNames.get(indexToFilenames).toString();
                    String logicalIdentifierPerLidReference = ReferentialIntegrityUtil.lidOrLidvidReferenceToLogicalIdentifierMap.get(singleLidOrLidvidReference);

                    LOG.debug("reportLidOrLidvidReferenceToNonExistLogicalReferences:filename,logicalIdentifierPerLidReference {},{}",filename,logicalIdentifierPerLidReference);

                    boolean productBelongToBundleFlag = ReferentialIntegrityUtil.isIdentiferMatchingBundleBaseID(logicalIdentifierPerLidReference);

                    // Only throw a WARNING if the product does belong to this bundle.
                    if (productBelongToBundleFlag == true) {
                        LOG.debug("reportLidOrLidvidReferenceToNonExistLogicalReferences:PRODUCT_IS_IN_BUNDLE:filename,logicalIdentifierPerLidReference {},{}",filename,logicalIdentifierPerLidReference);
                        ReferentialIntegrityUtil.performReporting(singleLidOrLidvidReference, true, indexToFilenames);
                    } else {
                        LOG.debug("reportLidOrLidvidReferenceToNonExistLogicalReferences:PRODUCT_NOT_IN_BUNDLE:filename,logicalIdentifierPerLidReference {},{}",filename,logicalIdentifierPerLidReference);
                    }
                } else {
                    LOG.debug("reportLidOrLidvidReferenceToNonExistLogicalReferences:LIDVID_REFERENCE:singleLidOrLidvidReference {} is in logicalIdentifiersCumulative",singleLidOrLidvidReference);
                }
            }
            indexToFilenames += 1;
        } // end for loop
      } catch (Exception e) {
          e.printStackTrace();
      }
  }

  /**
   * Report a WARNING if any references in observational are not referenced in the parent bundle/collection Reference_List.
   * @return None
   */
  public static void reportContextReferencesUnreferenced() {
      // For every references in the Context_Area, check if it also occur in the bundle/collection Reference_List,
      //  i.e: All context objects specified in observational are referenced in the parent bundle/collection Reference_List 
      LOG.debug("reportContextReferencesUnreferenced:referenceType,contextReferenceCheck {},{}",ReferentialIntegrityUtil.getReferenceType(),ReferentialIntegrityUtil.contextReferenceCheck);
      // Only do the reporting if the user did not use the flag.
      if (ReferentialIntegrityUtil.contextReferenceCheck == false) {
          return;
      }

      try {
        int indexToFilenames = 0;

        HashMap<String,HashSetReferenceInfo> hashMapToCheck = null; 
        Iterator iterator = null;
        boolean referenceTypeIsCollection = false;

        // If checking for bundle referential integrity, loop through all the references defined for all collections.
        // If checking for collection referential integrity, loop through all the references defined for all labels (products).

        if (ReferentialIntegrityUtil.getReferenceType().equals("bundle")) {
            iterator = ReferentialIntegrityUtil.collectionReferenceMap.entrySet().iterator(); 
            hashMapToCheck = ReferentialIntegrityUtil.bundleReferenceMap;
        } else if (ReferentialIntegrityUtil.getReferenceType().equals("collection")) {
            iterator = ReferentialIntegrityUtil.contextReferencesCumulative.entrySet().iterator();
            hashMapToCheck = ReferentialIntegrityUtil.collectionReferenceMap;
            referenceTypeIsCollection = true;
        } else {
             LOG.error("Expecting either 'bundle' or 'collection' for referenceType.  Received {}",ReferentialIntegrityUtil.getReferenceType());
        }

        HashSetReferenceInfo hashSetReferenceInfo = null;
        ArrayList<String> references = null;
        Map.Entry mapElement = null;
        String hashKey = null;
        boolean parentReferToChildFlag = false;

        while (iterator.hasNext()) {
            mapElement = (Map.Entry) iterator.next(); 
            hashSetReferenceInfo = (HashSetReferenceInfo) mapElement.getValue();
            references = hashSetReferenceInfo.getReferences();
            hashKey = (String) mapElement.getKey();
            // The parentId is the hashKey from this point on.

            LOG.debug("reportContextReferencesUnreferenced:referenceType,hashKey,preferences.size {},{},{}",ReferentialIntegrityUtil.getReferenceType(),hashKey,references.size());
            LOG.debug("reportContextReferencesUnreferenced:referenceType,hashKey {},{}",ReferentialIntegrityUtil.getReferenceType(),hashKey);

            for (String singleLidOrLidvidReference : references) {
                LOG.debug("reportContextReferencesUnreferenced:INSPECTING_REFERENCE:referenceType,hashKey,singleLidOrLidvidReference {},{},{}",ReferentialIntegrityUtil.referenceType,hashKey,singleLidOrLidvidReference);

                LOG.debug("reportContextReferencesUnreferenced:referenceType,singleLidOrLidvidReference,hashKey {},{},{}",ReferentialIntegrityUtil.getReferenceType(),singleLidOrLidvidReference,hashKey);
                parentReferToChildFlag = ReferentialIntegrityUtil.doesParentReferToChild(hashMapToCheck,singleLidOrLidvidReference,hashKey);
                LOG.debug("reportContextReferencesUnreferenced:referenceType,singleLidOrLidvidReference,hashKey,parentReferToChildFlag {},{},{},{}",
                    ReferentialIntegrityUtil.getReferenceType(),singleLidOrLidvidReference,hashKey,parentReferToChildFlag);

                if (parentReferToChildFlag == false) {
                    LOG.warn("reportContextReferencesUnreferenced:The context reference '{}' could not be found in the parent {} reference list.",singleLidOrLidvidReference,ReferentialIntegrityUtil.getReferenceType());
                    LOG.debug("reportContextReferencesUnreferenced:VALIDATING_REFERENCE_EXISTENCE_FALSE:referenceType,singleLidOrLidvidReference,hashKey {},{},{}",ReferentialIntegrityUtil.getReferenceType(),singleLidOrLidvidReference,hashKey);
                    LOG.debug("reportContextReferencesUnreferenced:VALIDATING_REFERENCE_EXISTENCE_FALSE:referenceType,singleLidOrLidvidReference,contextReferenceCheck {},{},{}",ReferentialIntegrityUtil.getReferenceType(),singleLidOrLidvidReference,ReferentialIntegrityUtil.contextReferenceCheck);
                    getListener().addProblem(new ValidationProblem(new ProblemDefinition(
                        ExceptionType.WARNING,
                        ProblemType.MISSING_CONTEXT_REFERENCE,
                        "The context reference '" + singleLidOrLidvidReference + "' could not be found in "
                            + "the parent " + ReferentialIntegrityUtil.getReferenceType() + " reference list." + ReferentialIntegrityUtil.MESSAGE_TO_DISABLE_WARNINGS), 
                        hashSetReferenceInfo.getParentLabelFilename()));
                } else {
                    LOG.debug("reportContextReferencesUnreferenced:Reference {} does indeed occur in bundle/collection Reference_List",singleLidOrLidvidReference);
                    LOG.debug("reportContextReferencesUnreferenced:VALIDATING_REFERENCE_EXISTENCE_TRUE:referenceType,singleLidOrLidvidReference,hashKey {},{},{}",ReferentialIntegrityUtil.getReferenceType(),singleLidOrLidvidReference,hashKey);
                }

            } // end for loop
        } // end while loop
      } catch (Exception e) {
          e.printStackTrace();
      }
    return;
  }

  private static boolean hasReferenceIDAndFilenameComboAdded(String singleLidorLidVidReference, URL filename) {
      boolean referenceIDAndFilenameComboAddedFlag = false;
      // Build the combo of reference and filename together from input parameters.
      // Remove the use of the slash '/' to avoid confusion.   We are merely looking at the combination of the lid_reference (or lidvid_reference) plus filename as strings for comparison.
      String referenceIDAndFilenameComboValue = singleLidorLidVidReference + filename.toString();
      for (int ii=0; ii < ReferentialIntegrityUtil.lidOrLidVidReferencesCumulative.size(); ii++) {
          // Build the combo of reference and filename together from each value in ReferentialIntegrityUtil.lidOrLidVidReferencesCumulative.get and ReferentialIntegrityUtil.lidOrLidVidReferencesCumulativeFileNames.get.
          // Remove the use of the slash '/' to avoid confusion.   We are merely looking at the combination of the lid_reference (or lidvid_reference) plus filename as strings for comparison.
          String singleComboValue = ReferentialIntegrityUtil.lidOrLidVidReferencesCumulative.get(ii) + ReferentialIntegrityUtil.lidOrLidVidReferencesCumulativeFileNames.get(ii);
          LOG.debug("hasReferenceIDAndFilenameComboAdded:referenceIDAndFilenameComboValue,singleComboValue {},{}",referenceIDAndFilenameComboValue,singleComboValue);
          if (referenceIDAndFilenameComboValue.equals(singleComboValue)) {
              // If there is a compare, we have found our answer and will break out of loop.
              referenceIDAndFilenameComboAddedFlag = true;
              break;
          }
      }
      return(referenceIDAndFilenameComboAddedFlag);
  }

  private static boolean isIdentiferMatchingBundleBaseID(String singleLogicalIdentifier) {
      // Given a logical identifier, check if it contains the bundle base identifier.
      // If the bundle base identifier is urn:nasa:pds:kaguya_grs_spectra
      // then urn:nasa:pds:kaguya_grs_spectra:document:kgrs_calibrated_spectra does contain the bundle base identifier. 
      boolean identifierMatchBundleBaseIDFlag = false;
      if (singleLogicalIdentifier != null) {
          if ((ReferentialIntegrityUtil.bundleBaseID != null) && singleLogicalIdentifier.contains(ReferentialIntegrityUtil.bundleBaseID)) {
              identifierMatchBundleBaseIDFlag = true;
          }
      }

      LOG.debug("isIdentiferMatchingBundleBaseID:singleLogicalIdentifier,ReferentialIntegrityUtil.bundleBaseID,identifierMatchBundleBaseIDFlag {},{},{}",singleLogicalIdentifier,ReferentialIntegrityUtil.bundleBaseID,identifierMatchBundleBaseIDFlag);
      return(identifierMatchBundleBaseIDFlag);
  }

  private static String getBundleBaseID(ArrayList<String> logicalIdentifiers, String bundleFilename) {
      // Given a list of logical identifier from a bundle, fetch the bundle base ID
      // urn:nasa:pds:kaguya_grs_spectra:document:kgrs_calibrated_spectra --> urn:nasa:pds:kaguya_grs_spectra
      // urn:nasa:pds:kaguya_grs_spectra                                  --> urn:nasa:pds:kaguya_grs_spectra
      // Note that a bundle should only have one logical_identifier so we pick the first element and print a warning if there are more than one.
      String bundleBaseID = null;
      if (logicalIdentifiers.size() >= 1) {
          if (logicalIdentifiers.size() == 1) {
              // Split the logical id and fetch the first 4 to form the bundleBaseID
              String [] splittedTokens = logicalIdentifiers.get(0).split(":");
              if (splittedTokens.length >= 4) {
                  bundleBaseID = splittedTokens[0] + ":" + splittedTokens[1] + ":" + splittedTokens[2] + ":" + splittedTokens[3];
              } else {
                  LOG.error("getBundleBaseID: Expecting at least 4 tokens from parsing logical identifier {}",logicalIdentifiers.get(0));
              }
          } else {
              LOG.warn("getBundleBaseID: Expecting only 1 logical identifier but received {} from list of identifiers from bundle file {}",logicalIdentifiers.size(),bundleFilename);
          }
      }
      return(bundleBaseID);
  }

  private static String getParentIdFromBundleLogical(String bundleLogicalId) {
      // Given a bundle urn:nasa:pds:kaguya_grs_spectra::1.1, returns
      // the parent id: urn:nasa:pds:kaguya_grs_spectra
      String parentId = null;

      String[] tokens = bundleLogicalId.split(":");
      if (tokens.length >= 4) {
          parentId = tokens[0] + ":" + tokens[1] + ":" +  tokens[2] + ":" + tokens[3];
      } else {
          LOG.error("getParentIdFromBundleLogical: Expecting at least 4 tokens from {}",bundleLogicalId);
      }

      return(parentId);
  }

  private static void addUniqueReferenceToContextReferences(ArrayList<String> contextLidOrLidVidReferences, URL url, String parentId) {
      // Given a list of references, add unique referrences to ReferentialIntegrityUtil.contextReferencesCumulative
      LOG.debug("addUniqueReferenceToContextReferences:contextLidOrLidVidReferences.size {},{}",contextLidOrLidVidReferences.size(),url);
      LOG.debug("addUniqueReferenceToContextReferences:parentId,contextLidOrrLidVidReferences.size {},{},{}",parentId,contextLidOrLidVidReferences.size(),url);

      for (String singleReference : contextLidOrLidVidReferences) {
          // Check if parentId exist yet in contextReferencesCumulative.  If yes, add the new singleReference to the list of references for parentId
          if (ReferentialIntegrityUtil.contextReferencesCumulative.keySet().contains(parentId)) {
              HashSetReferenceInfo setOfReferences = ReferentialIntegrityUtil.contextReferencesCumulative.get(parentId);
              if (setOfReferences.doesReferenceExist(singleReference) == false) {
                  // Add the new reference if setOfReferences does not already contains it.
                  setOfReferences.addReference(singleReference,url);
                  LOG.debug("addUniqueReferenceToContextReferences:ADDING_REFERENCE_TO_PARENT_EXISTING:parentId,singleReference {},{}",parentId,singleReference);
              } else {
                  LOG.debug("addUniqueReferenceToContextReferences:Reference {} already exist in parent set {}",singleReference,parentId);
              }
          } else {
              HashSetReferenceInfo setOfReferences = new HashSetReferenceInfo();
              setOfReferences.addReference(singleReference,url);
              ReferentialIntegrityUtil.contextReferencesCumulative.put(parentId,setOfReferences);
              LOG.debug("addUniqueReferenceToContextReferences:ADDING_REFERENCE_TO_PARENT_NEW:parentId,singleReference {},{}",parentId,singleReference);
          }
          LOG.debug("addUniqueReferenceToContextReferences:parentId,singleReference {},{}",parentId,singleReference);
      }   
  }

  private static void addUniqueReferenceToBundleOrCollectionReferenceList(ArrayList<String> contextLidOrLidVidReferences, URL url, String parentId) {
      // Given a list of references, add unique referrences to ReferentialIntegrityUtil.contextReferencesCumulative
      LOG.debug("addUniqueReferenceToBundleOrCollectionReferenceList:contextLidOrLidVidReferences.size {},{}",contextLidOrLidVidReferences.size(),url);
      LOG.debug("addUniqueReferenceToBundleOrCollectionReferenceList:parentId,contextLidOrLidVidReferences.size {},{},{}",parentId,contextLidOrLidVidReferences.size(),url);

      for (String singleReference : contextLidOrLidVidReferences) {
          // Check if parentId exist yet in contextReferencesCumulative.  If yes, add the new singleReference to the list of references for parentId
          //if (ReferentialIntegrityUtil.bundleOrCollectionReferenceMap.keySet().contains(parentId)) {
          if (ReferentialIntegrityUtil.contextReferencesCumulative.keySet().contains(parentId)) {
              //HashSet setOfReferences = ReferentialIntegrityUtil.bundleOrCollectionReferenceMap.get(parentId);
              HashSetReferenceInfo setOfReferences = ReferentialIntegrityUtil.contextReferencesCumulative.get(parentId);
              if (setOfReferences.doesReferenceExist(singleReference) == false) {
                  // Add the new reference if setOfReferences does not already contains it.
                  setOfReferences.addReference(singleReference,url);
                  LOG.debug("addUniqueReferenceToBundleOrCollectionReferenceList:ADDING_REFERENCE_TO_PARENT_EXISTING:parentId,singleReference {},{}",parentId,singleReference);
              } else {
                  LOG.debug("addUniqueReferenceToBundleOrCollectionReferenceList:Reference {} already exist in parent set {}",singleReference,parentId);
              }
          } else {
              HashSetReferenceInfo setOfReferences = new HashSetReferenceInfo();
              setOfReferences.addReference(singleReference,url);
              //ReferentialIntegrityUtil.bundleOrCollectionReferenceMap.put(parentId,setOfReferences);
              ReferentialIntegrityUtil.contextReferencesCumulative.put(parentId,setOfReferences);
              LOG.debug("addUniqueReferenceToBundleOrCollectionReferenceList:ADDING_REFERENCE_TO_PARENT_NEW:parentId,singleReference {},{}",parentId,singleReference);
          }
          LOG.debug("addUniqueReferenceToBundleOrCollectionReferenceList:parentId,singleReference {},{}",parentId,singleReference);
      }
      LOG.debug("addUniqueReferenceToBundleOrCollectionReferenceList:bundleOrCollectionReferenceMap.size(),url {}",ReferentialIntegrityUtil.bundleOrCollectionReferenceMap.size(),contextLidOrLidVidReferences.size(),url);
  }


  private static void addUniqueReferencesToMap(HashMap<String,HashSetReferenceInfo> hashMap, ArrayList<String> contextLidOrLidVidReferences, URL url, String parentId) {
      // Given a list of references, add unique referrences to provided hashMap.
      // The key to hashMap is the logical identifier stored in parentId variable.
      LOG.debug("addUniqueReferencesToMap:contextLidOrLidVidReferences.size {},{}",contextLidOrLidVidReferences.size(),url);
      LOG.debug("addUniqueReferencesToMap:parentId,contextLidOrLidVidReferences.size {},{},{}",parentId,contextLidOrLidVidReferences.size(),url);
      LOG.debug("addUniqueReferencesToMap:referenceType,url,contextLidOrLidVidReferences.size {},{},{}",ReferentialIntegrityUtil.getReferenceType(),url,contextLidOrLidVidReferences.size());

      int numReferencesAdded = 0;
      //int updatedNumReferences = 0;
      for (String singleReference : contextLidOrLidVidReferences) {
          // Check if parentId exist yet in hashMap.  If yes, add the new singleReference to the list of references for parentId
          if (hashMap.keySet().contains(parentId)) {
              HashSetReferenceInfo  setOfReferences = hashMap.get(parentId);
              if (setOfReferences.doesReferenceExist(singleReference) == false) {
                  // Add the new reference if setOfReferences does not already contains it.
                  setOfReferences.addReference(singleReference,url);
                  numReferencesAdded += 1;
                  LOG.debug("addUniqueReferencesToMap:ADDING_REFERENCE_TO_PARENT_EXISTING_REFERENCE_NEW:referenceType,parentId,singleReference {},{},{}",ReferentialIntegrityUtil.getReferenceType(),parentId,singleReference);
              } else {
                  //LOG.debug("addUniqueReferencesToMap:Reference {} already exist in parent set {}",singleReference,parentId);
                  LOG.debug("addUniqueReferencesToMap:ADDING_REFERENCE_TO_PARENT_EXISTING_REFERENCE_EXISTING:referenceType,parentId,singleReference {},{},{}",ReferentialIntegrityUtil.getReferenceType(),parentId,singleReference);
              }
          } else {
              HashSetReferenceInfo setOfReferences = new HashSetReferenceInfo();
              setOfReferences.addReference(singleReference,url);
              numReferencesAdded += 1;
              hashMap.put(parentId,setOfReferences);
              LOG.debug("addUniqueReferencesToMap:ADDING_REFERENCE_TO_PARENT_NEW_REFERENCE_NEW:referenceType,parentId,singleReference {},{},{}",ReferentialIntegrityUtil.getReferenceType(),parentId,singleReference);
          }
          LOG.debug("addUniqueReferencesToMap:parentId,singleReference {},{}",parentId,singleReference);
      }
      LOG.debug("addUniqueReferencesToMap:referenceType,parentId,url,numReferencesAdded {},{},{},{}",ReferentialIntegrityUtil.getReferenceType(),parentId,url,numReferencesAdded);
  }


  private static ArrayList<String> getKeysMatchingParent(HashMap<String,HashSetReferenceInfo> hashMap, String parentId) {

     boolean setsContainsParentId = false;
     String[] parentTokens = parentId.split("::");    // urn:nasa:pds:kaguya_grs_spectra:data_spectra:kgrs_calibrated_spectra_per1
     ArrayList<String> keysToMap = new ArrayList<String>();

     for (Map.Entry mapElement : hashMap.entrySet()) {
            String key = (String)mapElement.getKey(); // urn:nasa:pds:kaguya_grs_spectra:data_spectra::1.0
            String[] keysToken = key.split("::");     // urn:nasa:pds:kaguya_grs_spectra:data_spectra
            // Check if they keys and portion of the parentId are close enough.
            if (keysToken[0].contains(parentTokens[0]) || parentTokens[0].contains(keysToken[0])) {
                setsContainsParentId = true;
                keysToMap.add(key); // These values will be used to fetch the appropriate values from hashMap. 
            }
      } 
      return(keysToMap);
  }

  private static boolean isSetContainingReference(HashMap<String,HashSetReferenceInfo> hashMap, String parentId,  ArrayList<String> keysToMap, String singleReference) {
     boolean setsContainsReference = false;

     ArrayList<String> references = null;
     for (String singleKey : keysToMap) {
            LOG.debug("isSetContainingReference:getReferenceType,parentId,singleReference,singleKey {},{},{}",ReferentialIntegrityUtil.getReferenceType(),parentId,singleReference,singleKey);
            HashSetReferenceInfo hashSetReferenceInfo = (HashSetReferenceInfo) hashMap.get(singleKey);

            references = hashSetReferenceInfo.getReferences();
            LOG.debug("isSetContainingReference:getReferenceType,parentId,singleKey,references {},{},{},{}",ReferentialIntegrityUtil.getReferenceType(),parentId,singleKey,references);
            LOG.debug("isSetContainingReference:getReferenceType,parentId,singleReference,singleKey,references.size {},{},{},{},{}",ReferentialIntegrityUtil.getReferenceType(),parentId,singleReference,singleKey,references.size());
            if (references != null && references.contains(singleReference)) {
                setsContainsReference = true;
                break;
            }
      }
      return(setsContainsReference);
  }

  private static boolean doesParentReferToChild(HashMap<String,HashSetReferenceInfo> hashMap, String singleReference, String parentId) {
     // Given a child reference, check to see if any parent refers to the child reference.
     // The Map used will be determined by the referenceType.
     //     referenceType "bundle" uses bundleReferenceMap
     //     referenceType "collection" uses collectionReferenceMap

     // The keySet may contain values such as urn:nasa:pds:kaguya_grs_spectra:data_spectra::1.0 but
     // the parentId may be urn:nasa:pds:kaguya_grs_spectra:data_spectra:kgrs_calibrated_spectra_per1::1.0
     // so cannot use the contains() function to check.  New function getKeysMatchingParent() has been created to perform that look up.

     boolean parentReferToChildFlag = false;
     ArrayList<String> keysToMap = ReferentialIntegrityUtil.getKeysMatchingParent(hashMap,parentId);

     LOG.debug("doesParentReferToChild:ReferentialIntegrityUtil.getReferenceType,parentId {},{}",ReferentialIntegrityUtil.getReferenceType(),parentId);
     LOG.debug("doesParentReferToChild:ReferentialIntegrityUtil.getReferenceType,hashMap.keySet {},{}",ReferentialIntegrityUtil.getReferenceType(),hashMap.keySet());

     // Check if parentId exist yet in hashMap.  If yes, check if the child belong to the parent.
     if (keysToMap.size() > 0) {
         parentReferToChildFlag = ReferentialIntegrityUtil.isSetContainingReference(hashMap,parentId,keysToMap,singleReference);
         if (parentReferToChildFlag == true) {
             LOG.debug("doesParentReferToChild:CHILD_BELONG_TO_PARENT_TRUE:parentId,singleReference {},{}",parentId,singleReference);
         } else {
             // The child has a last name but the parent with the same last name does not have the child with the first name.
             LOG.debug("doesParentReferToChild:CHILD_BELONG_TO_PARENT_FALSE:singleReference,parentId {},{}",singleReference,parentId);
         }
     } else {
         // The child is an orphan.  No parent.
         LOG.debug("doesParentReferToChild:CHILD_BELONG_TO_PARENT_FALSE:singleReference,parentId {},{}",singleReference,parentId);
     }
     LOG.debug("doesParentReferToChild:ReferentialIntegrityUtil.getReferenceType,singleReference,parentId,parentReferToChildFlag {},{},{},{}",ReferentialIntegrityUtil.getReferenceType(),singleReference,parentId,parentReferToChildFlag);
     return(parentReferToChildFlag);
  }

  private static void collectAllContextReferences(DOMSource domSource, ArrayList<String> logicalIdentifiers, ArrayList<String> lidOrLidVidReferences,
                                                  boolean labelIsBundleFlag, boolean labelIsCollectionFlag,
                                                  URL url) {
    // https://github.com/NASA-PDS/validate/issues/69  As a user, I want to validate that all context objects specified in observational products are referenced in the parent bundle/collection Reference_List
    // Collect all the context references defined for each label under the "Context_Area" tag.

    ArrayList<String> allContextLidOrLidVidReferencesPerLabel = new ArrayList<String>();
    ArrayList<String> contextLidOrLidVidReferences = new ArrayList<String>();

    contextLidOrLidVidReferences = LabelUtil.getIdentifiersCommon(domSource,url,ReferentialIntegrityUtil.tagsList,LabelUtil.CONTEXT_AREA_INVESTIGATION_AREA_REFERENCE);
    allContextLidOrLidVidReferencesPerLabel.addAll(contextLidOrLidVidReferences);
    if (contextLidOrLidVidReferences.size() > 0) {
        if ((logicalIdentifiers != null) && !logicalIdentifiers.isEmpty()) {
            ReferentialIntegrityUtil.addUniqueReferenceToContextReferences(contextLidOrLidVidReferences,url,logicalIdentifiers.get(0));
        }
    }
    contextLidOrLidVidReferences = LabelUtil.getIdentifiersCommon(domSource,url,ReferentialIntegrityUtil.tagsList,LabelUtil.CONTEXT_AREA_OBSERVATION_SYSTEM_COMPONENT_REFERENCE);
    allContextLidOrLidVidReferencesPerLabel.addAll(contextLidOrLidVidReferences);
    if (contextLidOrLidVidReferences.size() > 0) {
        if ((logicalIdentifiers != null) && !logicalIdentifiers.isEmpty()) {
            ReferentialIntegrityUtil.addUniqueReferenceToContextReferences(contextLidOrLidVidReferences,url,logicalIdentifiers.get(0));
        }
    }
    contextLidOrLidVidReferences = LabelUtil.getIdentifiersCommon(domSource,url,ReferentialIntegrityUtil.tagsList,LabelUtil.CONTEXT_AREA_TARGET_IDENTIFICATION_REFERENCE);
    allContextLidOrLidVidReferencesPerLabel.addAll(contextLidOrLidVidReferences);
    if (contextLidOrLidVidReferences.size() > 0) {
        if ((logicalIdentifiers != null) && !logicalIdentifiers.isEmpty()) {
            ReferentialIntegrityUtil.addUniqueReferenceToContextReferences(contextLidOrLidVidReferences,url,logicalIdentifiers.get(0));
        }
    }

    // If the label is a bundle or collection, all identifiers in Context_Area can be collected in the appropriate map.

    if (labelIsCollectionFlag || labelIsBundleFlag) {
        if ((logicalIdentifiers != null) && !logicalIdentifiers.isEmpty()) {
            // Collection the references for either a bundle or a collection.  Put in them in the appropriate map.
            if (labelIsBundleFlag) {
                // Add all references listed in Context_Area
                ReferentialIntegrityUtil.addUniqueReferencesToMap(ReferentialIntegrityUtil.bundleReferenceMap,
                                                                  allContextLidOrLidVidReferencesPerLabel,url,logicalIdentifiers.get(0));
                // Add all references listed in Reference_List
                ReferentialIntegrityUtil.addUniqueReferencesToMap(ReferentialIntegrityUtil.bundleReferenceMap,
                                                                  lidOrLidVidReferences,url,logicalIdentifiers.get(0));
            } else if (labelIsCollectionFlag) {
                // Add all references listed in Context_Area
                ReferentialIntegrityUtil.addUniqueReferencesToMap(ReferentialIntegrityUtil.collectionReferenceMap,
                                                                  allContextLidOrLidVidReferencesPerLabel,url,logicalIdentifiers.get(0));
                // Add all references listed in Reference_List
                ReferentialIntegrityUtil.addUniqueReferencesToMap(ReferentialIntegrityUtil.collectionReferenceMap,
                                                                  lidOrLidVidReferences,url,logicalIdentifiers.get(0));
            } else { 
                LOG.error("This function does not support referenceType {}",ReferentialIntegrityUtil.getReferenceType());
            }
        }
    } else {
        // Regular label, all references listed in label (lidOrLidVidReferences) gets stored in contextReferencesCumulative.
        // The parent ID in the label is the logical indentifier.
        ReferentialIntegrityUtil.addUniqueReferencesToMap(ReferentialIntegrityUtil.contextReferencesCumulative,
                                                          lidOrLidVidReferences,url,logicalIdentifiers.get(0));
    }

    LOG.debug("collectAllContextReferences:url,contextReferencesCumulative {},{},{}",url,contextReferencesCumulative,contextReferencesCumulative.size());
  }

  /**
   * Perform additional referential integrity check beside the normal check. 
   * For all references in all labels, check if they refer to a logical identifier that is valid and is in this bundle/collection.
   * @param crawlTarget  The URL of the target to validate for.
   * @return None
   */
  public static void additionalReferentialIntegrityChecks(URL crawlTarget) {
    URL url = null;
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = null;
    Document xml = null;
    DOMSource domSource = null;

    boolean labelIsCollectionFlag = false;
    boolean labelIsBundleFlag = false;
    String parentId = null;

    try {
      List<Target> children = getContext().getCrawler().crawl(crawlTarget,true);  // Get also the directories.
      LOG.debug("additionalReferentialIntegrityChecks:crawlTarget {}",crawlTarget);
      LOG.debug("additionalReferentialIntegrityChecks:crawlTarget,children.size():afor_reduced: {},{}",crawlTarget,children.size());

      db = dbf.newDocumentBuilder();

      for (Target child : children) {
        LOG.debug("additionalReferentialIntegrityChecks:FilenameUtils.getName(child.toString()) {}",FilenameUtils.getName(child.toString()));

        // Regardless of what kinds of file it is, parse it to find all the local_identifier and lid_reference or lidvid_reference tags.
        url = child.getUrl();

        if (url.toString().endsWith(".xml")) {

            // Check this URL has been parsed before.  If yes, skip this file.
            if (ReferentialIntegrityUtil.urlsParsedCumulative.contains(url)) {
                LOG.info("SKIPPING_URL_TRUE:referenceType,url {},{}",ReferentialIntegrityUtil.getReferenceType(),url);
                continue;
            } else {
                LOG.info("SKIPPING_URL_FALSE:referenceType,url {},{}",ReferentialIntegrityUtil.getReferenceType(),url);
            }
            labelIsCollectionFlag = false;
            labelIsBundleFlag = false;

            // Check to see if the label is collection or a bundle (instead of regular label).
            Matcher matcherBundleCollection = BUNDLE_LABEL_PATTERN.matcher(FilenameUtils.getName(child.toString()));
            if (matcherBundleCollection.matches()) {
                labelIsBundleFlag = true;
            }
            matcherBundleCollection = COLLECTION_LABEL_PATTERN.matcher(FilenameUtils.getName(child.toString()));
            if (matcherBundleCollection.matches()) {
                labelIsCollectionFlag = true;
            }

            xml = db.parse(url.openStream());
            domSource = new DOMSource(xml);
            ArrayList<String> lidOrLidVidReferences = LabelUtil.getLidVidReferences(domSource,url);
            ArrayList<String> logicalIdentifiers    = LabelUtil.getLogicalIdentifiers(domSource,url);

            LOG.debug("additionalReferentialIntegrityChecks:url,lidOrLidVidReferences {},{}",url,lidOrLidVidReferences.size());
            LOG.debug("additionalReferentialIntegrityChecks:url,logicalIdentifiers {},{}",url,logicalIdentifiers.size());

            if ((logicalIdentifiers != null) && !logicalIdentifiers.isEmpty()) {
                ReferentialIntegrityUtil.logicalIdentifiersCumulative.addAll(logicalIdentifiers);

                // If the label is a bundle, parse the logical identifier for the base ID.
                if (labelIsBundleFlag) {
                   ReferentialIntegrityUtil.bundleBaseID = ReferentialIntegrityUtil.getBundleBaseID(logicalIdentifiers, child.toString());
                    parentId = ReferentialIntegrityUtil.getParentIdFromBundleLogical(logicalIdentifiers.get(0));
                    ReferentialIntegrityUtil.bundleURLMap.put(parentId,url); // Save the bundle URL so any warning/error can be reported for that bundle.
                }
            }

            if ((lidOrLidVidReferences != null) && !lidOrLidVidReferences.isEmpty()) {
                for (int ii=0; ii < lidOrLidVidReferences.size(); ii++) {
                    // Do not add duplicate references by checking for existence of the combination reference id in ReferentialIntegrityUtil.lidOrLidVidReferencesCumulative
                    // and the file name has not already been added already in ReferentialIntegrityUtil.lidOrLidVidReferencesCumulativeFileNames list.
                    // Note that because the reference id can be the same, the combination of the id plus the file name will make it unique. 
                    if (ReferentialIntegrityUtil.hasReferenceIDAndFilenameComboAdded(lidOrLidVidReferences.get(ii),url) == false) {

                        ReferentialIntegrityUtil.lidOrLidVidReferencesCumulative.add(lidOrLidVidReferences.get(ii));
                        ReferentialIntegrityUtil.lidOrLidVidReferencesCumulativeFileNames.add(url);  // Save the file name as well so it can be referred to.

                        LOG.debug("additionalReferentialIntegrityChecks:ADDING_REFERENCE {}",lidOrLidVidReferences.get(ii),lidOrLidVidReferencesCumulative.size());
                    }

                    // Every lid_reference or lidvid_reference is connected to a logical identifier.
                    // Save that in a Map so a logical identifier can be retrieved from a lid_reference or lidvid_reference as key.
                    if ((logicalIdentifiers != null) && !logicalIdentifiers.isEmpty()) {
                        ReferentialIntegrityUtil.lidOrLidvidReferenceToLogicalIdentifierMap.put(lidOrLidVidReferences.get(ii), logicalIdentifiers.get(0));
                    } else {
                        LOG.error("Expecting the logicalIdentifiers array to be non-empty for label {}",url);
                    }
                } // end for loop
            }


            // https://github.com/NASA-PDS/validate/issues/69  As a user, I want to validate that all context objects specified in observational products are referenced in the parent bundle/collection Reference_List
            // Collect all the context references defined for each label under the "Context_Area" tag.
            if (ReferentialIntegrityUtil.contextReferenceCheck == true) {
                  ReferentialIntegrityUtil.collectAllContextReferences(domSource,logicalIdentifiers,lidOrLidVidReferences,
                                                                       labelIsBundleFlag, labelIsCollectionFlag,
                                                                       url);
            }

        } else {
            LOG.debug("additionalReferentialIntegrityChecks:NON_XML:url {}",url);
            if (Utility.isDir(url.toString())) {
                // If the url is a directory, make a recursive call to this same function.
                ReferentialIntegrityUtil.additionalReferentialIntegrityChecks(url);
            }

        }

        // Add this url so we won't be parsing it again.
        ReferentialIntegrityUtil.urlsParsedCumulative.add(url);

      } // end for (Target child : children)
    } catch (IOException io) {
      reportError(GenericProblems.UNCAUGHT_EXCEPTION, getTarget(), -1, -1,
          io.getMessage());
    } catch (Exception ex) {
      reportError(GenericProblems.UNCAUGHT_EXCEPTION, getTarget(), -1, -1,
          ex.getMessage());
    }

    LOG.debug("additionalReferentialIntegrityChecks:referenceType,crawlTarget,logicalIdentifiersCumulative.size() {},{},{}",ReferentialIntegrityUtil.referenceType,crawlTarget,ReferentialIntegrityUtil.logicalIdentifiersCumulative.size());
    LOG.debug("additionalReferentialIntegrityChecks:referenceType,crawlTarget,lidOrLidVidReferencesCumulative.size() {},{},{}",ReferentialIntegrityUtil.referenceType,crawlTarget,ReferentialIntegrityUtil.lidOrLidVidReferencesCumulative.size());
    LOG.debug("additionalReferentialIntegrityChecks:referenceType,crawlTarget,lidOrLidVidReferencesCumulativeFilenames.size() {},{},{}",ReferentialIntegrityUtil.referenceType,crawlTarget,ReferentialIntegrityUtil.lidOrLidVidReferencesCumulativeFileNames.size());
    LOG.debug("additionalReferentialIntegrityChecks:referenceType,crawlTarget,bundleOrCollectionReferenceMap {},{},{},{}",ReferentialIntegrityUtil.referenceType,crawlTarget,bundleOrCollectionReferenceMap,bundleOrCollectionReferenceMap.size());
  }

  /**
   * Reports an error to the validation listener.
   *
   * @param defn the problem definition
   * @param targetFile the validation target file containing the problem
   * @param lineNumber the line number, or -1 if no line number applies
   * @param columnNumber the column number, or -1 if no column number applies
   */
  protected static void reportError(
          ProblemDefinition defn,
          URL targetUrl,
          int lineNumber,
          int columnNumber
  ) {
    ValidationProblem problem = new ValidationProblem(defn, new ValidationTarget(targetUrl), lineNumber, columnNumber, defn.getMessage());
    problemListener.addProblem(problem);
  }

  /**
   * Reports an error to the validation listener with a custom message.
   *
   * @param defn the problem definition
   * @param target the validation target containing the problem
   * @param lineNumber the line number, or -1 if no line number applies
   * @param columnNumber the column number, or -1 if no column number applies
   * @param message the error message to report
   */
  protected static void reportError(
          ProblemDefinition defn,
          URL target,
          int lineNumber,
          int columnNumber,
          String message
  ) {
    ValidationProblem problem = new ValidationProblem(defn, new ValidationTarget(target), lineNumber, columnNumber, message);
    problemListener.addProblem(problem);
  }
}
