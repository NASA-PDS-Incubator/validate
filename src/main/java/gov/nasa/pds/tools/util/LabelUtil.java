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

package gov.nasa.pds.tools.util;

import java.io.File;

import java.net.URI;
import java.net.URL;

import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import gov.nasa.pds.tools.label.ExceptionType;
import gov.nasa.pds.tools.validate.ProblemDefinition;
import gov.nasa.pds.tools.validate.ProblemType;
import gov.nasa.pds.validate.report.Report;
import gov.nasa.pds.tools.validate.ValidationProblem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that keep track of different Information Model (IM) versions found when parsing labels
 * and report a WARNING if multiple versions of the Information Model (IM) are found.
 * 
 */

public class LabelUtil {
  // This class is not meant to be instantiated since its goal is to keep track of different versions of the IM used
  // for the entire run and to report a WARNING if the user desire it.  Since many processes may be accessing these
  // functions and variables, some will be synchronized to protect the data from concurrent access.

  private static final Logger LOG = LoggerFactory.getLogger(LabelUtil.class);

  private static String PDS4_NS = "http://pds.nasa.gov/pds4/pds/v1";
  private static String INFORMATION_MODEL_VERSION = "information_model_version";
  private static String IDENTIFICATION_AREA = "//*:Identification_Area[namespace-uri()='" + PDS4_NS + "']";
  private static String INTERNAL_REFERENCE_AREA = "//*:Reference_List/*:Internal_Reference[namespace-uri()='" + PDS4_NS + "']";

  public static String LIDVID_REFERENCE = "lidvid_reference";
  public static String LID_REFERENCE = "lid_reference";

  // Symbols to parse references from Context_Area tag.
  public static String CONTEXT_AREA_TARGET_IDENTIFICATION_REFERENCE = "//*:Target_Identification/*:Internal_Reference[namespace-uri()='" + PDS4_NS + "']";
  public static String CONTEXT_AREA_OBSERVATION_SYSTEM_COMPONENT_REFERENCE = "//*:Observing_System/*:Observing_System_Component/*:Internal_Reference[namespace-uri()='" + PDS4_NS + "']";
  public static String CONTEXT_AREA_INVESTIGATION_AREA_REFERENCE = "//*:Investigation_Area/*:Internal_Reference[namespace-uri()='" + PDS4_NS + "']";

  private static String LOGICAL_IDENTIFIER_TAG = "logical_identifier";
  private static String VERSION_ID_TAG = "version_id";

  private static URL contextValue = null;
  private static String location = null;
  
  private static XPathFactory xPathFactory = new net.sf.saxon.xpath.XPathFactoryImpl();

  private static ArrayList<String> informationModelVersions = new ArrayList<>();
  private static Report report              = null;
  private static boolean bundleLabelSetFlag = false;
  private static String bundleLocation      = null;
  private static String launcherURIName     = null;

  /**
   * Reduce Information Model Versions List to just one if it contains more than one elements
   * @param  None
   * @return None
   */
  public static synchronized void reduceInformationModelVersions() {
      if (informationModelVersions.size() > 1) {
          String firstElement = informationModelVersions.get(0);
          LabelUtil.informationModelVersions.clear();
          LabelUtil.informationModelVersions.add(firstElement);
      }
      LOG.debug("LabelUtil:reduceInformationModelVersions");
  }

  /**
   * Reset Information Model Versions List to zero size.
   * @param  None
   * @return None
   */
  public static synchronized void hardResetInformationModelVersions() {
      if (informationModelVersions.size() > 0) {
          LabelUtil.informationModelVersions.clear();
      }
      LOG.debug("LabelUtil:hardResetInformationModelVersions");
  }

  /**
   * Reset all list(s) and variables back to their default states.
   * @param  None
   * @return None
   */
  public static synchronized void reset() {
      LabelUtil.informationModelVersions.clear();
      LabelUtil.report             = null;
      LabelUtil.bundleLabelSetFlag = false;
      LabelUtil.bundleLocation     = null;
      LabelUtil.launcherURIName    = null;
      LOG.debug("LabelUtil:reset()");
  }

  /**
   * Set the name of the executable of validate.
   * @param launcherURIName The name of the executable of validate.
   * @return None
   */
  public static void setLauncherURIName(String launcherURIName) {
      LabelUtil.launcherURIName = launcherURIName;
  }

  /**
   * Get the name of the executable of validate.
   * @param  None
   * @return The name of the executable of validate. 
   */
  public static String getLauncherURIName() {
      return(LabelUtil.launcherURIName);
  }

  /**
   * Set the report to log status of labels or WARNING messages.
   * @param report The Report object.
   * @return None
   */
  public static void setReport(Report report) {
      // Set a report so any mesages can be recorded.
      LabelUtil.report = report; 
  }


  /**
   * Register the Information Model (IM) version.
   * @param informationModelVersion The version of the IM to register, e.g. 1.12.0.0, 1.10.0.0
   * @return None
   */
  public static synchronized void registerIMVersion(String informationModelVersion) {
      if (!LabelUtil.informationModelVersions.contains(informationModelVersion)) {
          LabelUtil.informationModelVersions.add(informationModelVersion);
      }
      LOG.debug("registerIMVersion:informationModelVersion {}",informationModelVersions);
  }

  /**
   * Returns the list of IMs registered so far.
   * @return informationModelVersions the list of IMs registered: {1.12.0.0, 1.10.0.0}
   */
  public static synchronized ArrayList<String> getInformationModelVersions() {
     return(LabelUtil.informationModelVersions);
  }

  /**
   * Set the location of the label currently processing.
   * @param location informationModelVersion The version of the IM to register, e.g. 1.12.0.0, 1.10.0.0
   * @return informationModelVersions the list of IMs registerd.
   */
  public static synchronized void setLocation(String location) {
    // Set the location of the label being processed.  If the location is bundle, save it in bundleLocation as well. 
    try { 
        // Create a file object 
        File f = new File(location);
  
        // Get the name only of the given file f.
        String nameOnly = f.getName(); 

        // If the label is a bundle, save its name to bundleLocation so it can be used to report the multiple versions of IM.
        if (nameOnly.startsWith("bundle")) {
            LabelUtil.bundleLabelSetFlag = true;
            LabelUtil.bundleLocation     = location;
        }
        LabelUtil.location = location;
        LOG.debug("location,bundleLocation,bundleLabelSetFlag {},{},{}",location,LabelUtil.bundleLocation,LabelUtil.bundleLabelSetFlag);
    } catch (Exception e) { 
        System.err.println("LabelUtil:setLocation " + e.getMessage()); 
    } 
  }

  /**
   * Get the location of the label currently processing.
   * @return location of the label currently processing.
   */
  public static synchronized String getLocation() {
    return(LabelUtil.location);
  }

  /**
   * Get the Information Model (IM) version of the label (as a DOMSource)
   * @param source The content of context as a DOMSource.
   # @param context The location of the label being parsed from 
   * @return informationModelVersion the version of the IM
   */
  public static String getIMVersion(DOMSource source, URL context) {
      String informationModelVersion = null;
      LOG.debug("getIMVersion:MY_SOURCE[{}]",source);
      try {
          NodeList nodeList = (NodeList) xPathFactory.newXPath().evaluate(IDENTIFICATION_AREA,source,XPathConstants.NODESET);
          for (int i = 0; i < nodeList.getLength(); ++i) {
              NodeList childList = ((Element) nodeList.item(i)).getChildNodes();
              for (int j = 0; j < childList.getLength(); ++j) {
                  Node node = childList.item(j);
                  //LOG.debug("node.getTextContent().trim() {}",node.getTextContent().trim());
                  if (node.getNodeName().equals(INFORMATION_MODEL_VERSION)) {
                      informationModelVersion = node.getTextContent().trim();
                  }
              }
         }
     } catch (XPathExpressionException ex) {
         LOG.error("Cannot extract field " + INFORMATION_MODEL_VERSION + " from context " + context.toString());
     }
     LOG.debug("getIMVersion:context,informationModelVersion {},{}",context,informationModelVersion);
     return(informationModelVersion);
  }

  public static ArrayList<String> getIdentifiersCommon(DOMSource source, URL context, String[] tagsList, String searchPathName) {
      // Common function to retrieve values either from logical_identifier or lid_reference/lidvid_reference tags.
      // Note that because a node for logical_identifier can have a version id in another tag, they both must be check
      // before combining them together to .
      ArrayList<String> commonIdentifiers = new ArrayList<String>(0);
      LOG.debug("getIdentifiersCommon:MY_SOURCE[{}]",source);

      try {
          // Get to the node containing the searchPathName
          NodeList nodeList = (NodeList) xPathFactory.newXPath().evaluate(searchPathName,source,XPathConstants.NODESET);
          LOG.debug("getIdentifiersCommon:context,nodeList.getLength() {},{}",context,nodeList.getLength());
          LOG.debug("getIdentifiersCommon:context,searchPathName,nodeList.getLength() {},{},{}",context,searchPathName,nodeList.getLength());
          for (int i = 0; i < nodeList.getLength(); ++i) {
              NodeList childList = ((Element) nodeList.item(i)).getChildNodes();
              String singleIdentifier = null;
              String singleVersion = null;
              String singleLidvidValue = null;
              for (int j = 0; j < childList.getLength(); ++j) {
                  Node node = childList.item(j);
                  LOG.debug("node.getTextContent().trim() {}",node.getTextContent().trim());
                  //LOG.debug("node.getTextContent().trim() {}",node.getTextContent().trim());
                  // Because the tagsList is an array, loop through to check for each tag

                  for (int kk = 0; kk < tagsList.length; kk++) {

                      if (node.getNodeName().equals(tagsList[kk]) || node.getNodeName().equals(VERSION_ID_TAG)) {
                          if (node.getNodeName().equals(tagsList[kk])) 
                              singleIdentifier = node.getTextContent().trim();
                          if (node.getNodeName().equals(VERSION_ID_TAG))
                             singleVersion = node.getTextContent().trim();
                      }

                  }
              }
              if (singleIdentifier != null) {
                  if (singleVersion != null) {
                      // Append the version if it is available.
                      singleLidvidValue = singleIdentifier + "::" + singleVersion;
                  } else {
                      singleLidvidValue = singleIdentifier;
                  }
                  commonIdentifiers.add(singleLidvidValue); 
              }
         }
     } catch (XPathExpressionException ex) {
         LOG.error("Cannot extract field(s) {} or {} from context {}",tagsList,VERSION_ID_TAG,context.toString());
     }
     LOG.debug("getIdentifiersCommon:context,commonIdentifiers,searchPathName {},{},{}",context,commonIdentifiers,searchPathName);
     LOG.debug("getIdentifiersCommon:context,commonIdentifiers.size,searchPathName {},{},{}",context,commonIdentifiers.size(),searchPathName);
     return(commonIdentifiers);
  }

  /**
   * Get the LIDVID references in the label  (as a DOMSource)
   * @param source The content of context as a DOMSource.
   # @param context The location of the label being parsed from.
   * @return lidOrLidVidReference The LID or LIDVID referenced in this label.
   */
  public static ArrayList<String> getLidVidReferences(DOMSource source, URL context) {
      LOG.debug("getLidVidReferences:MY_SOURCE[{}]",source);

      String[] tagsList = new String[2];
      tagsList[0] = LIDVID_REFERENCE;
      tagsList[1] = LID_REFERENCE;

      ArrayList<String> lidOrLidVidReferences = LabelUtil.getIdentifiersCommon(source, context, tagsList, INTERNAL_REFERENCE_AREA);

      LOG.debug("getLidVidReferences:context,lidOrLidVidReferences {},{}",context,lidOrLidVidReferences);
      return(lidOrLidVidReferences);
  }

  /**
   * Get the local identifiers the label (as a DOMSource)
   * @param source The content of context as a DOMSource.
   # @param context The location of the label being parsed from.
   * @return logicalIdentifiers A list of logical identifiers in this label.
   */
  public static ArrayList<String> getLogicalIdentifiers(DOMSource source, URL context) {
      LOG.debug("getLogicalIdentifiers:MY_SOURCE[{}]",source);

      String[] tagsList = new String[1];
      tagsList[0] = LOGICAL_IDENTIFIER_TAG;

      ArrayList<String> logicalIdentifiers = LabelUtil.getIdentifiersCommon(source, context, tagsList, IDENTIFICATION_AREA);

      LOG.debug("getLogicalIdentifiers:context,logicalIdentifiers {},{}",context,logicalIdentifiers);
      return(logicalIdentifiers);
  }

  /**
   * Report a WARNING if the number of unique IM versions are more than one.
   * @param validationRule The rule of the validation, e.g. pds4.label, pds4.bundle.  This value can be null since a rule is is not required within validate module.
   * @return None 
   */
  public static synchronized void reportIfMoreThanOneVersion(String validationRule) {
      // After all the labels have been validated, fetch all the information_model_version encountered.
      ArrayList<String> informationModelVersions = LabelUtil.getInformationModelVersions();

      String versions = "";
      int numDifferentVersions = 0;
      for(String str : informationModelVersions) {
          versions += str + ", ";
          numDifferentVersions += 1;
      }
      if (versions.equals("")) {
          // This means that the function reportIfMoreThanOneVersion() is called too early and no labels has been parsed yet.
          LOG.warn("reportIfMoreThanOneVersion:There has been no versions added to informationModelVersions set yet.");
          // Clear out all list(s) and reset variables back to their default states.
          LabelUtil.reset();
          return;
      }

      versions = versions.substring(0, versions.length() - 2);
      LOG.debug("reportIfMoreThanOneVersion:target,informationModelVersions.size(),MY_VERSIONS {},{},{}",location,informationModelVersions.size(),versions);
      LOG.debug("reportIfMoreThanOneVersion:informationModelVersions {}",informationModelVersions);

      try {
          if (numDifferentVersions > 1) {
              String message = "";
              URL url = null; // The warning message will be for the entire run so no need to set specific url.
              String forRuleStr = "";
              String forBundleStr = "";
              if (validationRule != null) { 
                  // Some run has no rule so cannot print it.
                  forRuleStr = " for rule " + validationRule;
              }
              if (LabelUtil.bundleLabelSetFlag) {
                  // Print specific bundle name to identify a location name with an actual bundle.
                  forBundleStr = " for bundle " + LabelUtil.bundleLocation;
              }
              message = "Multiple versions (" + Integer.toString(numDifferentVersions) + ") of Information Model (IM) found in this run: [" + versions + "]" + forRuleStr + forBundleStr;
              LOG.debug(message);

              // Build the ValidationProblem and add it to the report.
              ValidationProblem p1 = new ValidationProblem(new ProblemDefinition(ExceptionType.WARNING,
                                                                                 ProblemType.GENERAL_INFO, message),url);
              LOG.debug("reportIfMoreThanOneVersion:bundleLabelSetFlag,url {},{}",LabelUtil.bundleLabelSetFlag,url);

              // Append the WARNING message to the report.
              // Note: The value of the url is important as it is a key to the map of the report.  Each url (or label) has status and messages attached to it.
              //LabelUtil.report.record(url.toURI(), p1);
              LabelUtil.report.record(new URI(LabelUtil.launcherURIName), p1);
              //LabelUtil.report.record(url.toURI(), p1);

              // Clear out all list(s) and reset variables back to their default states.
              LabelUtil.reset();
          }
      } catch (Exception e) {
          e.printStackTrace();
      }
  }
}
