//Copyright © 2019, California Institute of Technology ("Caltech").
// U.S. Government sponsorship acknowledged.
//
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// • Redistributions of source code must retain the above copyright notice,
//   this list of conditions and the following disclaimer.
// • Redistributions must reproduce the above copyright notice, this list of
//   conditions and the following disclaimer in the documentation and/or other
//   materials provided with the distribution.
// • Neither the name of Caltech nor its operating division, the Jet Propulsion
//   Laboratory, nor the names of its contributors may be used to endorse or
//   promote products derived from this software without specific prior written
//   permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package gov.nasa.pds.validate.commandline.options;

/**
 * An interface that contains the valid property keys for the Validate Tool
 * configuration file.
 *
 * @author mcayanan
 *
 */
public class ConfigKey {
    /** List of file patterns to look for if traversing a target directory. */
    public static final String REGEXP = "validate.regexp";

    /** The report. */
    public static final String REPORT = "validate.report";

    /** A list of targets to validate. */
    public static final String TARGET = "validate.target";

    /** A severity level for the output report. */
    public static final String VERBOSE = "validate.verbose";

    /** A list of schema files to validate against. */
    public static final String SCHEMA = "validate.schema";

    /** A list of schematron files to validate against. */
    public static final String SCHEMATRON = "validate.schematron";

    /** The catalog file to use during validation. */
    public static final String CATALOG = "validate.catalog";

    /** Property to specify one or more checksum manifest files. */
    public static final String CHECKSUM = "validate.checksum";

    /**
     * List of paths to use as the base when looking up file references in a
     * checksum manifest file.
     */
    public static final String BASE_PATH = "validate.basePath";
    
    /** DEPRECATED: The model version to use during validation. */
    public static final String MODEL = "validate.model";

    /** Enables/disables direcotry recursion. */
    public static final String LOCAL = "validate.local";

    /** Configures the report style format. */
    public static final String STYLE = "validate.reportStyle";
    
    /**
     * DEPRECATED: Force the tool to validate against the schema and schematron specified in
     * the label.
     */
    public static final String FORCE = "validate.force";

    /**
     * Property to specify the validation rule type.
     */
    public static final String RULE = "validate.rule";

    /**
     * DEPRECATED: Property to disable data content validation.
     */
    public static final String NO_DATA = "validate.noDataCheck";

    /**
     * Property to disable data content validation.
     */
    public static final String SKIP_CONTENT_VALIDATION = "validate.skipContentValidation";

    /**
     * Property to disable context reference check.
     */
    public static final String SKIP_CONTEXT_REFERENCE_CHECK = "validate.skipContextReferenceCheck";

    /**
     * Property to enable check in between fields validation for Table_Character file.
     */
    public static final String CHECK_INBETWEEN_FIELDS = "validate.checkInbetweenFields";

    /**
     * Property to specify the maximum number of errors to report before
     * terminating a validation run.
     */
    public static final String MAX_ERRORS = "validate.maxErrors";

    /**
     * Property to specify how many lines or records to skip during content
     * validation.
     */
    public static final String SPOT_CHECK_DATA = "validate.spotCheckData";

    /**
     * Property to allow the tool to not report on unlabeled files in a bundle
     * or collection.
     */
    public static final String ALLOW_UNLABELED_FILES = "validate.allowUnlabeledFiles";

    /**
     * Property to download the latest Registered Context Products JSON file and
     * replace the existing file.
     */
    public static final String LATEST_JSON_FILE = "validate.updateContextProducts";
    
    public static final String NONREGPROD_JSON_FILE = "validate.addContextProducts";
    
    /**
     * Property to disable context validation.
     */
    public static final String SKIP_CONTEXT_VALIDATION = "validate.skipContextValidation";
    
    /**
     * Property to specify the file that contains a list of files/directories to validate.
     */
    public static final String TARGET_MANIFEST = "validate.targetManifest";
    
    public static final String SKIP_PRODUCT_VALIDATION = "validate.ignoreProductValidation";
}
