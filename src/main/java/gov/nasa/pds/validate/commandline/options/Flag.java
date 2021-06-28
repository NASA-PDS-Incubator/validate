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

import java.util.List;

/**
 * Class that holds the command-line option flags.
 *
 * @author mcayanan
 *
 */
public enum Flag {
    ALTERNATE_FILE_PATHS(null, "alternate_file_paths", "path", String.class, "This flag will allow for additional paths to be specified when attempting referential integrity validation (pds4.bundle or pds4.collection rules).  FOR DEVELOPMENT PURPOSES ONLY"),

    BASE_PATH("B", "base-path", "path", String.class,
            "Specify a path " + "for the tool to use in order to properly resolve relative file "
                    + "references found in a checksum manifest file."),

    CATALOG("C", "catalog", "catalog files", String.class, true, "Specify catalog files to use during validation."),

    /**
     * Flag to specify a configuration file to configure the tool behavior.
     */
    CONFIG("c", "config", "file", String.class, "Specify a configuration " + "file to set the tool behavior."),

    /**
     * Flag to specify file patterns to look for when validating a target
     * directory.
     */
    MAX_ERRORS("E", "max-errors", "value", short.class, "Specify the max " + "number of errors that the tool will report on before gracefully " + "exiting a validation run. Default is 100,000."),

    /**
     * Flag to specify file patterns to look for when validating a target
     * directory.
     */
    REGEXP("e", "regexp", "patterns", String.class, true, "Specify file patterns " + "to look for when validating a directory. Each pattern should " + "be surrounded by quotes. Default is to look for files ending with " + "a '.xml' or '.XML' file extension."),
    
    /**
     * DEPRECATED: Flag to force the tool to perform validation against the schema and
     * schematron specified in a given label.
     */
    FORCE("f", "force", "DEPRECATED: Tool performs validation against the schema and schematron specified in a given label by default. " +
               "Use -x and/or -S flag(s) to validate with the core PDS or user-specified schema and schematron."),

    /**
     * Flag to display the help.
     */
    HELP("h", "help", "Display usage."),
    
    /**
     * Flag that disables recursion when traversing a target directory.
     */
    LOCAL("L", "local", "Validate files only in the target directory rather " + "than recursively traversing down the subdirectories."),

    /**
     * Flag to specify one or more checksum manifest files in order to perform
     * checksum validation.
     */
    CHECKSUM_MANIFEST("M", "checksum-manifest", "file", String.class, "Specify a checksum manifest file to perform checksum validation " + "against the targets being validated."),
    
    /**
     * DEPRECATED: Flag to specify a model version to use during validation.
     */
    MODEL("m", "model-version", "version", String.class, "DEPRECATED: Tool performs validation against the schema and schematron specified in a given label by default. " +
                                                         "Use -x and/or -S flag(s) to validate with the core PDS or user-specified schema and schematron."),

    /** Flag to disable data content validation. */
    NO_DATA(null, "no-data-check", "DEPRECATED: This option has been renamed to --skip-content-validation to be more consistent with other argument naming."),

    /** Flag to disable data content validation. */
    SKIP_CONTENT_VALIDATION("D", "skip-content-validation", "Disable product content validation."),

    /** Flag to disable context reference check. */
    SKIP_CONTEXT_REFERENCE_CHECK("T", "skip-context-reference-check", "Disable context reference check."),

    /**
     * Flag to specify a report file name.
     */
    REPORT("r", "report-file", "file name", String.class, "Specify the " + "report file name. Default is standard out."),

    /**
     * Flag to specify a list of schematron files to use during validation.
     */
    SCHEMATRON("S", "schematron", "schematron files", String.class, true, "Specify schematron files."),

    /**
     * Flag to specify the report style.
     *
     */
    STYLE("s", "report-style", "full|json|xml", String.class, "Specify the level of detail for the reporting. Valid values are " + "'full' for a full view, 'json' for a json view, and 'xml' for an " + "XML view. Default is to see a full report if this flag is not " + "specified"),

    /**
     * Flag to explicitly specify the targets to validate.
     */
    TARGET("t", "target", "files,dirs", String.class, true, "Explicitly specify " + "the targets (files, directories) to validate. Targets can be " + "specified implicitly as well. " + "(example: validate product.xml)"),

    /**
     * Displays the tool version.
     */
    VERSION("V", "version", "Display application version."),

    /**
     * Flag to specify the severity level and above to include in the report.
     */
    VERBOSE("v", "verbose", "1|2|3", short.class, "Specify the severity " + "level and above to include in the human-readable report: " + "(1=Info, 2=Warning, 3=Error). Default is Warning and above. "),

    /**
     * Flag to specify a list of schemas to use during validation.
     */
    SCHEMA("x", "schema", "schema files", String.class, true, "Specify schema files."),

    SPOT_CHECK_DATA(null, "spot-check-data", "num", int.class,
            "Tool only checks every nth record or line and skips the rest during data content validation."),

    ALLOW_UNLABELED_FILES(null, "allow-unlabeled-files",
            "Tells the tool to not check for unlabeled files in a bundle or collection."),

    RULE("R", "rule", "validation rule name", String.class,
            "Specifies the validation rules to apply. (pds4.bundle|pds4.collection|pds4.folder|pds4.label|pds3.volume)."
                    + " Default is to auto-detect based on the contents at the location specified."),
    
    SKIP_PRODUCT_VALIDATION(null, "skip-product-validation", 
    		"Disables product validation when attempting to run pds4.bundle or pds4.collection validation. The software will perform member integrity checks but will not validate individual products or their labels."),

    /**
     * Flag to download the latest Registered Context Products JSON file and
     * replace the existing file.
     */
    LATEST_JSON_FILE("u", "update-context-products", "Update the Context Product information used for validating context product references in labels."),
    
    NONREGPROD_JSON_FILE(null, "add-context-products", "dir/files", String.class, true,
            "Explicitly specify a JSON file (or directory of files) containing additional context product information used for validation. " +
            "WARNING: This should only be used for development purposes. All context products must be registered for validity of a product in an archive."),

    /**
     * flag to temporarily disable context validation. When this flag is
     * enabled, the output logs will throw WARNING messages instead of failing
     * validation. Only be enabled during development
     */
    SKIP_CONTEXT_VALIDATION(null, "skip-context-validation", "Disable context product reference validation. WARNING: This should only be used for development purposes only. All context products must be registered for validity of a product in an archive."),

    // Flag to allow user to check in between fields for non-blank characters for Table_Character validation.
    CHECK_INBETWEEN_FIELDS(null, "strict-field-checks", "Specific to character tables (Table_Character) validation, enable checks to ensure no unexpected alphanumeric characters appear in between in between fields."),

    /**
     * flag to Flag to specify the file that contains a list of files/directories to validate.
     */
    TARGET_MANIFEST(null, "target-manifest", "file", String.class, true, "Specify a manifest file of files/directory paths to validate.");
    
    /** The short name. */
    private final String shortName;

    /** The long name. */
    private final String longName;

    /** The argument name. */
    private final String argName;

    /** The type of argument that the flag accepts. */
    private final Object argType;

    /** A flag that allows multiple argument values. */
    private final boolean allowsMultipleArgs;

    /** A description of the flag. */
    private final String description;

    /**
     * Constructor.
     *
     * @param shortName
     *            The short name.
     * @param longName
     *            The long name.
     * @param description
     *            A description of the flag.
     */
    private Flag(final String shortName, final String longName, final String description) {
        this(shortName, longName, null, null, description);
    }

    /**
     * Constructor for flags that can take arguments.
     *
     * @param shortName
     *            The short name.
     * @param longName
     *            The long name.
     * @param argName
     *            The argument name.
     * @param argType
     *            The argument type.
     * @param description
     *            A description of the flag.
     */
    private Flag(final String shortName, final String longName, final String argName, final Object argType,
            final String description) {
        this(shortName, longName, argName, argType, false, description);
    }

    /**
     * Constructor for flags that can take arguments.
     *
     * @param shortName
     *            The short name.
     * @param longName
     *            The long name.
     * @param argName
     *            The argument name.
     * @param argType
     *            The argument type.
     * @param description
     *            A description of the flag.
     */
    private Flag(final String shortName, final String longName, final String argName, final Object argType,
            final boolean allowsMultipleArgs, final String description) {
        this.shortName = shortName;
        this.longName = longName;
        this.argName = argName;
        this.argType = argType;
        this.allowsMultipleArgs = allowsMultipleArgs;
        this.description = description;
    }

    /**
     * Get the short name of the flag.
     *
     * @return The short name.
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * Get the long name of the flag.
     *
     * @return The long name.
     */
    public String getLongName() {
        return longName;
    }

    /**
     * Get the argument name of the flag.
     *
     * @return The argument name.
     */
    public String getArgName() {
        return argName;
    }

    /**
     * Find out if the flag can handle multiple arguments.
     *
     * @return 'true' if yes.
     */
    public boolean allowsMultipleArgs() {
        return allowsMultipleArgs;
    }

    /**
     * Get the argument type of the flag.
     *
     * @return The argument type.
     */
    public Object getArgType() {
        return argType;
    }

    /**
     * Get the flag description.
     *
     * @return The description.
     */
    public String getDescription() {
        return description;
    }
}
