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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class to hold a list of references and the file name those references belong to.
 * It will be used to report which label contains the message.
 *
 */
public class HashSetReferenceInfo {
  private static final Logger LOG = LoggerFactory.getLogger(HashSetReferenceInfo.class);
  ArrayList<String> references = new ArrayList<String>();
  URL parentLabelFilename = null; // The name of the parent label containing these references.
  int numReferencesAdded = 0;

  public HashSetReferenceInfo() {
  }

  public void addReference(String reference, URL parentLabelFilename) {
      this.references.add(reference);
      numReferencesAdded += 1;

      // Only set the name of the parent label file once since all references being set for this class belong to just one file.
      if (this.parentLabelFilename == null) {
          this.parentLabelFilename = parentLabelFilename;
      }
      LOG.debug("addingReference:ADDING_PARENT_URL:numReferencesAdded,parentLabelFilename,reference {},{},{}",numReferencesAdded,parentLabelFilename,reference);
  }

  public boolean doesReferenceExist(String reference) {
      if (this.references.contains(reference)) {
          return(true);
      } else {
          return(false);
      }
  }

  public ArrayList<String> getReferences() {
      return(this.references);
  }

  public URL getParentLabelFilename() {
      return(this.parentLabelFilename);
  }
}
