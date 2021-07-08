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

//import java.io.File;
//
//import java.net.URI;
//import java.net.URL;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import javax.xml.transform.dom.DOMSource;
//import javax.xml.xpath.XPathConstants;
//import javax.xml.xpath.XPathExpressionException;
//import javax.xml.xpath.XPathFactory;
//
//import org.w3c.dom.Element;
//import org.w3c.dom.Node;
//import org.w3c.dom.NodeList;
//
//import gov.nasa.pds.tools.label.ExceptionType;
//import gov.nasa.pds.tools.validate.ProblemDefinition;
//import gov.nasa.pds.tools.validate.ProblemType;
//import gov.nasa.pds.validate.report.Report;
//import gov.nasa.pds.tools.validate.ValidationProblem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that keep track of different flags from command line to be made available to other validator that cannot easily
 * be passed to.
 * 
 */
public class FlagsUtil {
    // This class is not meant to be instantiated since its goal is to keep track of flags from command line.
    private static Logger LOG = LoggerFactory.getLogger(FlagsUtil.class);

    /** Flag to enable/disable data content validation. */
    private static boolean contentValidationFlag = true;

    /** Flag to enable/disable product level validation. */
    private static boolean skipProductValidation = false;

  /**
   * Initialize flags to their default states
   * @param  None
   * @return None
   */
    public static void initialize() {
        FlagsUtil.contentValidationFlag = true;
        FlagsUtil.skipProductValidation = false;
        LOG.debug("initialize:contentValidationFlag {}",FlagsUtil.contentValidationFlag);
        LOG.debug("initialize:skipProductValidation {}",FlagsUtil.skipProductValidation);
    }

  /**
   * Set the contentValidationFlagvalue
   * @param  flag The boolean value to set contentValidationFlag to 
   * @return None
   */
    public static void setContentValidationFlag(boolean flag) {
        FlagsUtil.contentValidationFlag = flag;
        LOG.debug("setContentValidationFlag: {}",FlagsUtil.contentValidationFlag);
    }

  /**
   * Get the contentValidationFlag value
   * @param  None
   * @return the contentValidationFlag value
   */
    public static boolean getContentValidationFlag() {
        return(FlagsUtil.contentValidationFlag);
    }


  /**
   * Set the skipProductValidation value
   * @param  flag The boolean value to set skipProductValidation to 
   * @return None
   */
    public static void setSkipProductValidation(boolean flag) {
        FlagsUtil.skipProductValidation = flag;
        LOG.debug("setSkipProductValidation: {}",FlagsUtil.skipProductValidation);
    }

  /**
   * Get the skipProductValidation value
   * @param  None
   * @return the skipProductValidation valueNone
   */
    public static boolean getSkipProductValidation() {
        return(FlagsUtil.skipProductValidation);
    }
}
