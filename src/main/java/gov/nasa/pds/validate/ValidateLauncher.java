// Copyright © 2019, California Institute of Technology ("Caltech").
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

package gov.nasa.pds.validate;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import gov.nasa.pds.tools.label.*;
import gov.nasa.pds.tools.label.validate.DocumentValidator;
import gov.nasa.pds.tools.util.ContextProductReference;
import gov.nasa.pds.tools.util.VersionInfo;
import gov.nasa.pds.tools.util.LabelUtil;
import gov.nasa.pds.tools.util.ReferentialIntegrityUtil;
import gov.nasa.pds.tools.util.XMLExtractor;
import gov.nasa.pds.tools.validate.*;
import gov.nasa.pds.tools.validate.rule.pds4.SchemaValidator;
import gov.nasa.pds.validate.checksum.ChecksumManifest;
import gov.nasa.pds.validate.commandline.options.ConfigKey;
import gov.nasa.pds.validate.commandline.options.Flag;
import gov.nasa.pds.validate.commandline.options.FlagOptions;
import gov.nasa.pds.validate.commandline.options.InvalidOptionException;
import gov.nasa.pds.validate.report.FullReport;
import gov.nasa.pds.validate.report.JSONReport;
import gov.nasa.pds.validate.report.Report;
import gov.nasa.pds.validate.report.XmlReport;
import gov.nasa.pds.validate.target.Target;
import gov.nasa.pds.validate.util.ToolInfo;
import gov.nasa.pds.validate.util.Utility;
import org.apache.commons.cli.*;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.Priority;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.ls.LSInput;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Wrapper class for the Validate Tool. Class handles command-line parsing and
 * querying, in addition to reporting setup.
 *
 * @author mcayanan
 *
 */
public class ValidateLauncher {
    private static Logger LOG = LoggerFactory.getLogger(
        ValidateLauncher.class);

    /** List of targets to validate. */
    private List<URL> targets;

    /** Additional paths to be specified when attempting referential integrity validation (pds4.bundle or pds4.collection rules) */
    private ArrayList<URL> alternateReferentialPaths;

    /** List of regular expressions for the file filter. */
    private List<String> regExps;

    /** A list of user-given schemas to validate against. */
    private List<URL> schemas;

    /** A list of catalog files to use during validation. */
    private List<String> catalogs;

    private List<URL> schematrons;

    /** A report file. */
    private File reportFile;

    /** A flag to enable/disable directory recursion. */
    private boolean traverse;

    /** Update register context products flag */
    private boolean updateRegisteredProducts;

    /** user no register context products flag */
    private boolean nonRegisteredProducts;

    /** Flag to indicated deprecated flag was used **/
    private boolean deprecatedFlagWarning;

    /** The severity level and above to include in the report. */
    private ExceptionType severity;

    /** An object representation of a Validate Tool report. */
    private Report report;

    /** Indicates the report style format. */
    private String reportStyle;

    /**
     * Flag to force the tool to validate against the schema and schematron
     * specified in the given label.
     */
    private boolean force;

    /**
     * A checksum manifest file to use for checksum validation.
     *
     */
    private URL checksumManifest;

    /**
     * Default file filters.
     */
    private String[] DEFAULT_FILE_FILTERS = { "*.xml", "*.XML" };

    private SchemaValidator schemaValidator;

    private SchematronTransformer schematronTransformer;

    private List<Transformer> transformedSchematrons;

    private CachedEntityResolver resolver;
    
    private ValidatorFactory factory;

    /**
     * path to use as the base when looking up file references in a manifest
     * file.
     */
    private URL manifestBasePath;

    /** The validation rule name to use. */
    private String validationRule;

    /** Flag to enable/disable data content validation. */
    private boolean contentValidationFlag;

    /** Flag to enable/disable context reference check. */
    private boolean contextReferenceCheck;

    /** Flag to enable/disable product level validation. */
    private boolean skipProductValidation;

    private long MAX_ERRORS = 100000;

    private long maxErrors;

    private int spotCheckData;

    private boolean allowUnlabeledFiles;

    private File registeredProductsFile;

    private File nonRegisteredProductsFile;

    private Map<String, List<ContextProductReference>> registeredAndNonRegistedProducts;
    
    private boolean validateContext;

    private boolean checkInbetweenFields = false; // Flag to enable checking of values in between fields.

    /**
     * Constructor.
     * 
     * @throws TransformerConfigurationException
     *
     */
    public ValidateLauncher() throws TransformerConfigurationException {
        targets = new ArrayList<URL>();
        regExps = new ArrayList<String>();
        catalogs = new ArrayList<String>();
        schemas = new ArrayList<URL>();
        schematrons = new ArrayList<URL>();
        this.alternateReferentialPaths = new ArrayList<URL>();
        checksumManifest = null;
        manifestBasePath = null;
        reportFile = null;
        traverse = true;
        severity = ExceptionType.WARNING;
        report = null;
        reportStyle = "full";
        force = true;
        regExps.addAll(Arrays.asList(DEFAULT_FILE_FILTERS));
        schemaValidator = new SchemaValidator();
        schematronTransformer = new SchematronTransformer();
        transformedSchematrons = new ArrayList<Transformer>();
        resolver = new CachedEntityResolver();
        contentValidationFlag = true;
        contextReferenceCheck = true;
        skipProductValidation = false;
        maxErrors = MAX_ERRORS;
        spotCheckData = -1;
        allowUnlabeledFiles = false;
        registeredAndNonRegistedProducts = new HashMap<String, List<ContextProductReference>>();
        registeredProductsFile = new File(System.getProperty("resources.home") + File.separator + ToolInfo.getOutputFileName());
        updateRegisteredProducts = false;
        deprecatedFlagWarning = false;
        validateContext = true;
        checkInbetweenFields = false;
        
        this.flushValidators();
    }

    /**
     * Parse the command-line arguments
     *
     * @param args
     *            The command-line arguments
     * @return A class representation of the command-line arguments
     *
     * @throws ParseException
     *             If an error occurred during parsing.
     */
    public CommandLine parse(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        return parser.parse(FlagOptions.getOptions(), args);
    }

    /**
     * Query the command-line and process the command-line option flags set.
     *
     * @param line
     *            A CommandLine object containing the flags that were set.
     *
     * @throws Exception
     *             If an error occurred while processing the command-line
     *             options.
     */
    public void query(CommandLine line) throws Exception {
        List<Option> processedOptions = Arrays.asList(line.getOptions());
        List<String> targetList = new ArrayList<String>();
        // Gets the implicit targets
        for (java.util.Iterator<String> i = line.getArgList().iterator(); i.hasNext();) {
            String[] values = i.next().split(",");
            for (int index = 0; index < values.length; index++) {
                targetList.add(values[index].trim());
            }
        }
        for (Option o : processedOptions) {
            LOG.debug("query:o.getOpt() {}",o.getOpt());
            LOG.debug("query:o.getLongOpt() {}",o.getLongOpt());
            if (Flag.HELP.getShortName().equals(o.getOpt())) {
                displayHelp();
                System.exit(0);
            } else if (Flag.VERSION.getShortName().equals(o.getOpt())) {
                displayVersion();
                System.exit(0);
            } else if (Flag.CONFIG.getShortName().equals(o.getOpt())) {
                File c = new File(o.getValue());
                if (c.exists()) {
                    query(c);
                } else {
                    throw new Exception("Configuration file does not exist: " + c);
                }
            } else if (Flag.REPORT.getShortName().equals(o.getOpt())) {
                setReport(new File(o.getValue()));
            } else if (Flag.LOCAL.getShortName().equals(o.getOpt())) {
                setTraverse(false);
            } else if (Flag.CATALOG.getShortName().equals(o.getOpt())) {
                setCatalogs(o.getValuesList());
                setForce(false);
            } else if (Flag.SCHEMA.getShortName().equals(o.getOpt())) {
                setSchemas(o.getValuesList());
                setForce(false);
            } else if (Flag.SCHEMATRON.getShortName().equals(o.getOpt())) {
                setSchematrons(o.getValuesList());
                setForce(false);
            } else if (Flag.TARGET.getShortName().equals(o.getOpt())) {
                targetList.addAll(o.getValuesList());
            } else if (Flag.TARGET_MANIFEST.getLongName().equals(o.getLongOpt())) {
                String fileName = o.getValue();
                File listF = new File(fileName);               
                if (listF.exists()) {
                    List<String> listTgt;
                    try (Stream<String> lines = Files.lines(Paths.get(fileName))) {
                        listTgt = lines.collect(Collectors.toList());
                    }
                    targetList.addAll(listTgt);
                } else {
                    throw new Exception("The file of target list does not exist: " + fileName);
                }                
            }else if (Flag.VERBOSE.getShortName().equals(o.getOpt())) {
                short value = 0;
                try {
                    value = Short.parseShort(o.getValue());
                } catch (IllegalArgumentException a) {
                    throw new InvalidOptionException("Problems parsing severity level " + "value.");
                }
                setSeverity(value);
            } else if (Flag.REGEXP.getShortName().equals(o.getOpt())) {
                setRegExps((List<String>) o.getValuesList());
            } else if (Flag.ALTERNATE_FILE_PATHS.getLongName().equals(o.getLongOpt())) {
                LOG.debug("query:o.getValues() {},{}",o.getValues(),o.getValues().getClass().getSimpleName());
                LOG.debug("query:o.getValuesList() {},{}",o.getValuesList(),o.getValuesList().getClass().getSimpleName());
                this.setAdditionalPaths((List<String>) o.getValuesList());
            } else if (Flag.STYLE.getShortName().equals(o.getOpt())) {
                setReportStyle(o.getValue());
            } else if (Flag.CHECKSUM_MANIFEST.getShortName().equals(o.getOpt())) {
                setChecksumManifest(o.getValue());
            } else if (Flag.BASE_PATH.getShortName().equals(o.getOpt())) {
                setManifestBasePath(o.getValue());
            } else if (Flag.RULE.getShortName().equals(o.getOpt())) {
                setValidationRule(o.getValue());
            } else if (Flag.SKIP_CONTENT_VALIDATION.getShortName().equals(o.getOpt())) { // Updated to handle deprecated flag name
                setContentValidation(false);
            } else if (Flag.SKIP_CONTEXT_REFERENCE_CHECK.getShortName().equals(o.getOpt())) { // Updated to handle deprecated flag name
                setContextReferenceCheck(false);
            } else if (Flag.CHECK_INBETWEEN_FIELDS.getLongName().equals(o.getLongOpt())) {
                setCheckInbetweenFields(true);
            } else if (Flag.NO_DATA.getLongName().equals(o.getLongOpt())) {
                setContentValidation(false);
                deprecatedFlagWarning = true;
            } else if (Flag.SKIP_PRODUCT_VALIDATION.getLongName().equals(o.getLongOpt())) {
            	setSkipProductValidation(true);
            } else if (Flag.MAX_ERRORS.getShortName().equals(o.getOpt())) {
                long value = 0;
                try {
                    value = Long.parseLong(o.getValue());
                } catch (IllegalArgumentException a) {
                    throw new InvalidOptionException("Could not parse value '" + o.getValue() + "': " + a.getMessage());
                }
                setMaxErrors(value);
            } else if (Flag.SPOT_CHECK_DATA.getLongName().equals(o.getLongOpt())) {
                int value = 0;
                try {
                    value = Integer.parseInt(o.getValue());
                } catch (IllegalArgumentException a) {
                    throw new InvalidOptionException("Could not parse value '" + o.getValue() + "': " + a.getMessage());
                }
                setSpotCheckData(value);
            } else if (Flag.ALLOW_UNLABELED_FILES.getLongName().equals(o.getLongOpt())) {
                setAllowUnlabeledFiles(true);
            } else if (Flag.LATEST_JSON_FILE.getLongName().equals(o.getLongOpt())) {
                setUpdateRegisteredProducts(true);
            } else if (Flag.NONREGPROD_JSON_FILE.getLongName().equals(o.getLongOpt())) {
                File nonRegProdJson = new File(o.getValue());
                if (nonRegProdJson.exists()) {
                    nonRegisteredProductsFile = nonRegProdJson;
                } else {
                    throw new Exception(
                            "The user No Registered Product context file does not exist: " + nonRegProdJson);
                }
                setNonRegisteredProducts(true);
            } else if (Flag.SKIP_CONTEXT_VALIDATION.getLongName().equals(o.getLongOpt())) {
                setValidateContext(false);
            }

            /**
             * Deprecated per
             * https://github.com/NASA-PDS/validate/issues/23
             **/
            else if (Flag.MODEL.getShortName().equals(o.getOpt())) {
                deprecatedFlagWarning = true;
            } else if (Flag.FORCE.getShortName().equals(o.getOpt())) {
                setForce(true);
                deprecatedFlagWarning = true;
            }
            /** **/
        }
        if (!targetList.isEmpty()) {
            setTargets(targetList);
        }
        if (force && (!schemas.isEmpty() || !schematrons.isEmpty() || !catalogs.isEmpty())) {
            throw new InvalidOptionException(
                    "Cannot specify user schemas, " + "schematrons, and/or catalog files with the 'force' flag option");
        }
        if (checksumManifest != null) {
            if ((targets.size() > 1) && (manifestBasePath == null)) {
                throw new InvalidOptionException(
                        "Must specify the base path " + "flag option ('-B' flag) when specifying a checksum manifest "
                                + "file and multiple targets.");
            }
        }
        /*
         * if (!filters.isEmpty() && (validationRule != null &&
         * !validationRule.equals("pds4.folder"))) { throw new
         * IllegalArgumentException("Cannot specify a file filter " +
         * "when the validation rule is set to '" + validationRule + "'"); } if
         * (validationRule != null && !(validationRule.equals("pds4.folder") ||
         * validationRule.equals("pds4.label"))) { setRegExps(Arrays.asList(new
         * String[] {"*"})); }
         */
    }

    private void getLatestJsonContext() {

        String url = ToolInfo.getSearchURL();
        String endpoint = ToolInfo.getEndpoint();
        String query = ToolInfo.getQuery();

        SolrClient client = new HttpSolrClient.Builder(url).build();
        SolrQuery solrQuery = new SolrQuery(query);
        solrQuery.setRequestHandler("/" + endpoint);
        solrQuery.setStart(0);
        solrQuery.setParam("fl",
                "identifier, " + "version_id, " + "data_product_type, " + "target_name, " + "instrument_name, "
                        + "instrument_host_name, " + "resource_name, " + "investigation_name, " + "target_type, "
                        + "instrument_type, " + "instrument_host_type, " + "resource_type, " + "investigation_type, "
                                + "facility_name, facility_type, airborne_name, airborne_type");

        QueryResponse resp;
        List<ValidationProblem> pList = new ArrayList<ValidationProblem>();
        try {
            resp = client.query(solrQuery);
            SolrDocumentList res = resp.getResults();
            solrQuery.setRows((int) res.getNumFound());
            resp = client.query(solrQuery);
            res = resp.getResults();
            parseJsonObjectWriteTofile(res);

            client.close();
            ValidationProblem p1 = new ValidationProblem(new ProblemDefinition(ExceptionType.INFO,
                    ProblemType.GENERAL_INFO, "Successfully updated registered context products config file. "),
                    new URL(url));
            pList.add(p1);
            ValidationProblem p2 = new ValidationProblem(new ProblemDefinition(ExceptionType.INFO,
                    ProblemType.GENERAL_INFO, res.size() + " registered context products found."),
                    new URL(url));
            pList.add(p2);

        } catch (SolrServerException | IOException ex) {
            try {
                ValidationProblem p = new ValidationProblem(
                        new ProblemDefinition(ExceptionType.ERROR, ProblemType.INTERNAL_ERROR,
                                "Error connecting to Registry to update registered context products config file. Verify internet connection and try again."),
                        new URL(url));
                report.record(new URI(System.getProperty("resources.home") + File.separator + ToolInfo.getOutputFileName()), p);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            report.record(new URI(System.getProperty("resources.home") + File.separator + ToolInfo.getOutputFileName()), pList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseJsonObjectWriteTofile(SolrDocumentList docs) {
        // backup old file
        try {
            copyFile(registeredProductsFile,
                    new File(System.getProperty("resources.home") + File.separator + ToolInfo.getOutputFileName() + ".backup"));
            // System.out.println("back up " +
            // System.getProperty("resources.home") + "/" +
            // ToolInfo.getOutputFileName()
            // + " to " + System.getProperty("resources.home") + "/" +
            // ToolInfo.getOutputFileName() + ".backup");
        } catch (IOException e) {
            e.printStackTrace();
        }

        JsonWriter jsonWriter;
        try {
            jsonWriter = new JsonWriter(
                    new FileWriter(System.getProperty("resources.home") + File.separator + ToolInfo.getOutputFileName()));

            jsonWriter.setIndent("     ");
            jsonWriter.beginObject(); // start Product_Context
            jsonWriter.name("Product_Context");
            jsonWriter.beginArray();
            for (SolrDocument document : docs) {
                String id = (String) document.getFirstValue("identifier");
                String ver = (String) document.getFirstValue("version_id");
                String data_type = (String) document.getFirstValue("data_product_type");
                List<Object> names = (ArrayList<Object>) document.getFieldValues(data_type.toLowerCase() + "_name");
                List<Object> types = (ArrayList<Object>) document.getFieldValues(data_type.toLowerCase() + "_type");
                
                jsonWriter.beginObject(); // start a product
                
                jsonWriter.name("name");
                jsonWriter.beginArray();
                if (names == null) {
                    jsonWriter.value("N/A");
                } else {
                    for (Object n : names) {
                        jsonWriter.value((String) n);
                    }
                }
                jsonWriter.endArray();
                
                jsonWriter.name("type");
                jsonWriter.beginArray();
                if (types == null) {
                    jsonWriter.value("N/A");
                } else {
                    for (Object t : types) {
                        jsonWriter.value((String) t);
                    }
                }
                jsonWriter.endArray();

                jsonWriter.name("lidvid").value(id + "::" + ver);
                jsonWriter.endObject(); // end a product
            }
            jsonWriter.endArray();
            jsonWriter.endObject(); // end Product_Context
            jsonWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // System.out.println("New Registered Products File: " +
        // registeredProductsFile);

    }

    private void copyFile(File source, File dest) throws IOException {
        FileChannel sourceChannel = null;
        FileChannel destChannel = null;
        FileInputStream fileInputStream = new FileInputStream(source);
        FileOutputStream fileOutputStream = new FileOutputStream(dest);

        try {
            sourceChannel = fileInputStream.getChannel();
            destChannel = fileOutputStream.getChannel();
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        } finally {
            if (sourceChannel != null)
                sourceChannel.close();
            if (destChannel != null)
                destChannel.close();
            if (fileInputStream != null)
                fileInputStream.close();
            if (fileOutputStream != null)
                fileOutputStream.close();
        }
    }

    /**
     * Query the configuration file.
     *
     * @param configuration
     *            A configuration file.
     *
     * @throws ConfigurationException
     *             If an error occurred while querying the configuration file.
     */
    @SuppressWarnings("unchecked")
    public void query(File configuration) throws ConfigurationException {
        try {
            Configuration config = null;
            AbstractConfiguration.setDefaultListDelimiter(',');
            config = new PropertiesConfiguration(configuration);
            
            List<String> targetList = new ArrayList<String>();
            if (config.isEmpty()) {
                throw new ConfigurationException("Configuration file is empty: " + configuration);
            }
            if (config.containsKey(ConfigKey.REGEXP)) {
                // Removes quotes surrounding each pattern being specified
                List<String> list = Utility.removeQuotes(config.getList(ConfigKey.REGEXP));
                setRegExps(list);
            }
            if (config.containsKey(ConfigKey.REPORT)) {
                setReport(new File(config.getString(ConfigKey.REPORT)));
            }
            if (config.containsKey(ConfigKey.TARGET)) {
                // Removes quotes surrounding each pattern being specified
                targetList = Utility.removeQuotes(config.getList(ConfigKey.TARGET));
            }
            if (config.containsKey(ConfigKey.VERBOSE)) {
                setSeverity(config.getShort(ConfigKey.VERBOSE));
            }
            if (config.containsKey(ConfigKey.SCHEMA)) {
                // Removes quotes surrounding each pattern being specified
                List<String> list = Utility.removeQuotes(config.getList(ConfigKey.SCHEMA));
                setSchemas(list);
                setForce(false);
            }
            if (config.containsKey(ConfigKey.SCHEMATRON)) {
                // Removes quotes surrounding each pattern being specified
                List<String> list = Utility.removeQuotes(config.getList(ConfigKey.SCHEMATRON));
                setSchematrons(list);
                setForce(false);
            }
            if (config.containsKey(ConfigKey.LOCAL)) {
                if (config.getBoolean(ConfigKey.LOCAL) == true) {
                    setTraverse(false);
                } else {
                    setTraverse(true);
                }
            }
            if (config.containsKey(ConfigKey.STYLE)) {
                setReportStyle(config.getString(ConfigKey.STYLE));
            }
            /** Deprecated per issue-23. Add deprecation warning message **/
            if (config.containsKey(ConfigKey.MODEL)) {
                deprecatedFlagWarning = true;
            }
            if (config.containsKey(ConfigKey.FORCE)) {
                deprecatedFlagWarning = true;
            }
            if (config.containsKey(ConfigKey.CHECKSUM)) {
                setChecksumManifest(config.getString(ConfigKey.CHECKSUM));
            }
            if (config.containsKey(ConfigKey.BASE_PATH)) {
                setManifestBasePath(config.getString(ConfigKey.BASE_PATH));
            }
            if (config.containsKey(ConfigKey.RULE)) {
                setValidationRule(config.getString(ConfigKey.RULE));
            }
            if (config.containsKey(ConfigKey.NO_DATA)) {
                if (config.getBoolean(ConfigKey.NO_DATA) == true) {
                    setContentValidation(false);
                } else {
                    setContentValidation(true);
                }
                deprecatedFlagWarning = true;
            }
            if (config.containsKey(ConfigKey.SKIP_CONTENT_VALIDATION)) {
                if (config.getBoolean(ConfigKey.SKIP_CONTENT_VALIDATION) == true) {
                    setContentValidation(false);
                } else {
                    setContentValidation(true);
                }
            }
            if (config.containsKey(ConfigKey.SKIP_CONTEXT_REFERENCE_CHECK)) {
                if (config.getBoolean(ConfigKey.SKIP_CONTEXT_REFERENCE_CHECK) == true) {
                    setContextReferenceCheck(false);
                } else {
                    setContextReferenceCheck(true);
                }
            }
            if (config.containsKey(ConfigKey.CHECK_INBETWEEN_FIELDS)) {
                if (config.getBoolean(ConfigKey.CHECK_INBETWEEN_FIELDS) == true) {
                    setCheckInbetweenFields(true);
                } else {
                    setCheckInbetweenFields(false);
                }
            }
            if (config.containsKey(ConfigKey.SKIP_PRODUCT_VALIDATION)) {
                setSkipProductValidation(true);
            }
            if (config.containsKey(ConfigKey.MAX_ERRORS)) {
                setMaxErrors(config.getLong(ConfigKey.MAX_ERRORS));
            }
            if (config.containsKey(ConfigKey.SPOT_CHECK_DATA)) {
                setSpotCheckData(config.getInt(ConfigKey.SPOT_CHECK_DATA));
            }
            if (config.containsKey(ConfigKey.ALLOW_UNLABELED_FILES)) {
                setAllowUnlabeledFiles(true);
            }
            if (config.containsKey(ConfigKey.LATEST_JSON_FILE)) {
                setUpdateRegisteredProducts(true);
            }
            if (config.containsKey(ConfigKey.NONREGPROD_JSON_FILE)) {
                nonRegisteredProductsFile = new File(config.getString(ConfigKey.NONREGPROD_JSON_FILE));
                setNonRegisteredProducts(true);
            }
            
            if (config.containsKey(ConfigKey.SKIP_CONTEXT_VALIDATION)) {
                if (config.getBoolean(ConfigKey.SKIP_CONTEXT_VALIDATION) == true) {
                    setValidateContext(false);
                }
            }
            if (config.containsKey(ConfigKey.TARGET_MANIFEST)) {
                String fileName = config.getString(ConfigKey.TARGET_MANIFEST);
                File listF = new File(fileName);               
                if (listF.exists()) {
                    List<String> listTgt;
                    try (Stream<String> lines = Files.lines(Paths.get(fileName))) {
                        listTgt = lines.collect(Collectors.toList());
                    }
                    targetList.addAll(listTgt);
                } else {
                    throw new Exception("The file of target list does not exist: " + fileName);
                }
            }
            
            if (!targetList.isEmpty()) {
                setTargets(targetList);
            }
            
        } catch (Exception e) {
            throw new ConfigurationException(e.getMessage());
        }
    }


    /**
     * Set additional paths to be specified when attempting referential integrity validation (pds4.bundle or pds4.collection rules)
     *
     * @param additionalPaths 
     *            A list of paths.
     * @throws MalformedURLException
     */
    public void setAdditionalPaths(List<String> additionalPaths) throws MalformedURLException {
        // Due to the fact that each element in additionalPaths may be a separated comma, each entry
        // must be further split using comma inside the for loop below.
        LOG.debug("setAdditionalPaths:additionalPaths {},{}",additionalPaths,additionalPaths.size());
        this.alternateReferentialPaths.clear();
        while (alternateReferentialPaths.remove(""))
            ;
        for (String pathEntries: additionalPaths) {
            LOG.debug("setAdditionalPaths:pathEntries {}",pathEntries);
            // The value of pathEntries are comma separated values.
            String[] pathTokens = pathEntries.split(",");
            for (String pathEntry : pathTokens) {
                URL url = null;
                try {
                    url = new URL(pathEntry);
                    LOG.debug("setAdditionalPaths:url {}",url);
                    this.alternateReferentialPaths.add(url);
                } catch (MalformedURLException u) {
                    File file = new File(pathEntry);
                    LOG.debug("setAdditionalPaths:file.toURI().normalize().toURL() {}",file.toURI().normalize().toURL());
                    this.alternateReferentialPaths.add(file.toURI().normalize().toURL());
                }
            }
        }

        LOG.debug("setAdditionalPaths:additionalPaths {}",additionalPaths);
        LOG.debug("setAdditionalPaths:alternateReferentialPaths {},{}",this.alternateReferentialPaths,this.alternateReferentialPaths.size());
    }

    /**
     * Set the target.
     *
     * @param targets
     *            A list of targets.
     * @throws MalformedURLException
     */
    public void setTargets(List<String> targets) throws MalformedURLException {
        LOG.debug("setTargets:afor:this.targets.size() {}",this.targets.size());
        this.targets.clear();
        while (targets.remove(""))
            ;
        for (String t : targets) {
            URL url = null;
            try {
                url = new URL(t);
                LOG.debug("setTargets:ADD_TO_TARGET_URL:url {}",url);
                this.targets.add(url);
            } catch (MalformedURLException u) {
                File file = new File(t);
                LOG.debug("setTargets:ADD_TO_TARGET_FILE:file.toURI().normalize().toURL() {}",file.toURI().normalize().toURL());
                this.targets.add(file.toURI().normalize().toURL());
            }
        }
        LOG.debug("setTargets:after:this.targets.size() {}",this.targets.size());
    }

    /**
     * Set the checksum manifest.
     *
     * @param manifest
     *            A checksum manifest file.
     * @throws MalformedURLException
     */
    public void setChecksumManifest(String manifest) throws MalformedURLException {
        URL url = null;
        try {
            url = new URL(manifest);
            this.checksumManifest = url;
        } catch (MalformedURLException u) {
            File file = new File(manifest);
            this.checksumManifest = file.toURI().normalize().toURL();
        }
    }

    /**
     * Set the base path to look up relative file references in a given checksum
     * manifest file.
     *
     * @param path
     *            A path.
     * @throws MalformedURLException
     */
    public void setManifestBasePath(String path) throws MalformedURLException {
        this.manifestBasePath = Utility.toURL(path);
    }

    /**
     * Set the schemas.
     *
     * @param schemas
     *            A list of schemas.
     * @throws MalformedURLException
     */
    public void setSchemas(List<String> schemas) throws MalformedURLException {
        while (schemas.remove(""))
            ;
        for (String schema : schemas) {
            this.schemas.add(Utility.toURL(schema));
        }
    }

    /**
     * Set the schematrons.
     *
     * @param schematrons
     *            A list of schematrons.
     * @throws MalformedURLException
     */
    public void setSchematrons(List<String> schematrons) throws MalformedURLException {
        while (schematrons.remove(""))
            ;
        for (String schematron : schematrons) {
            this.schematrons.add(Utility.toURL(schematron));
        }
    }

    /**
     * Set the catalogs.
     *
     * @param catalogs
     *            A list of catalogs.
     */
    public void setCatalogs(List<String> catalogs) {
        while (catalogs.remove(""))
            ;
        this.catalogs.addAll(catalogs);
    }

    /**
     * Sets the report file.
     *
     * @param report
     *            A report file.
     */
    public void setReport(File report) {
        this.reportFile = report;
    }

    /**
     * Gets the object representation of the Validation Report.
     *
     * @return The Report object.
     */
    public Report getReport() {
        return report;
    }

    /**
     * Set the output style for the report.
     * 
     * @param style
     *            'sum' for a summary report, 'min' for a minimal report, and
     *            'full' for a full report
     * @throws ApplicationException
     */
    public void setReportStyle(String style) throws Exception {
        if ((style.equalsIgnoreCase("full") == false) && (style.equalsIgnoreCase("json") == false)
                && (style.equalsIgnoreCase("xml") == false)) {
            throw new Exception(
                    "Invalid value entered for 's' flag. Value can only " + "be either 'full', 'json' or 'xml'");
        }
        this.reportStyle = style;
    }

    /**
     * Sets the flag to enable/disable directory recursion.
     *
     * @param value
     *            A boolean value.
     */
    public void setTraverse(boolean value) {
        this.traverse = value;
    }

    /**
     * Sets the severity level for the report.
     *
     * @param level
     *            An interger value.
     */
    public void setSeverity(int level) {
        if (level < 0 || level > 3) {
            throw new IllegalArgumentException("Severity level value can only " + "be 1, 2, or 3");
        }
        if (level == 0) {
            this.severity = ExceptionType.DEBUG;
        } else if (level == 1) {
            this.severity = ExceptionType.INFO;
        } else if (level == 2) {
            this.severity = ExceptionType.WARNING;
        } else if (level == 3) {
            this.severity = ExceptionType.ERROR;
        }
    }

    /**
     * Sets the list of file patterns to look for if traversing a directory.
     *
     * @param patterns
     *            A list of file patterns.
     */
    public void setRegExps(List<String> patterns) {
        this.regExps = patterns;
        while (this.regExps.remove(""))
            ;
    }

    public void setForce(boolean value) {
        this.force = value;
    }

    /**
     * Sets the validation rule name to use.
     * 
     * @param value
     *            the validation rule name
     */
    public void setValidationRule(String value) {
        if ("pds4.bundle".equalsIgnoreCase(value)) {
            this.validationRule = "pds4.bundle";
        } else if ("pds4.collection".equalsIgnoreCase(value)) {
            this.validationRule = "pds4.collection";
        } else if ("pds4.folder".equalsIgnoreCase(value)) {
            this.validationRule = "pds4.folder";
        } else if ("pds4.label".equalsIgnoreCase(value)) {
            this.validationRule = "pds4.label";
        } else if ("pds3.volume".equalsIgnoreCase(value)) {
            this.validationRule = "pds3.volume";
        } else {
            throw new IllegalArgumentException(
                    "Validation rule type value must " + "be one of the following: pds4.bundle, pds4.collection, "
                            + "pds4.folder, pds4.label, or pds3.volume");
        }
    }

    /**
     * Sets the flag that enables/disables data content validation.
     * 
     * @param flag
     *            True or False.
     */
    public void setContentValidation(boolean flag) {
        this.contentValidationFlag = flag;
    }

    public void setContextReferenceCheck(boolean flag) {
        this.contextReferenceCheck = flag;
    }

    public void setCheckInbetweenFields(boolean flag) {
        this.checkInbetweenFields = flag;
    }

    public void setSkipProductValidation(boolean flag) {
    	this.skipProductValidation = flag;
    }
    
    public void setMaxErrors(long value) {
        this.maxErrors = value;
    }

    public void setSpotCheckData(int value) {
        this.spotCheckData = value;
    }

    public void setAllowUnlabeledFiles(boolean flag) {
        this.allowUnlabeledFiles = flag;
    }

    private void setRegisteredProducts() {
        URL url = null;

        Gson gson = new Gson();
        List<ContextProductReference> contextProducts = new ArrayList<ContextProductReference>();
        List<ValidationProblem> pList = new ArrayList<ValidationProblem>();

        try {
            JsonObject json = gson.fromJson(new FileReader(registeredProductsFile), JsonObject.class);
            JsonArray array = json.get("Product_Context").getAsJsonArray();

            for (JsonElement jsonElm : array) {

                JsonObject jsonObj = jsonElm.getAsJsonObject();
                String lidvidString = jsonObj.get("lidvid").getAsString();
                List<String> types = new ArrayList<String>();
                if (!jsonObj.get("type").isJsonNull()) {
                    for (JsonElement e : jsonObj.get("type").getAsJsonArray()) {
                        types.add(e.getAsString());
                    }
                } else {
                    types.add("N/A");
                }
                
                List<String> names = new ArrayList<String>();
                if (!jsonObj.get("name").isJsonNull()) {
                    for (JsonElement e : jsonObj.get("name").getAsJsonArray()) {
                        names.add(e.getAsString());
                    }
                } else {
                    names.add("N/A");
                }

                contextProducts.add(
                        new ContextProductReference(lidvidString.split("::")[0], lidvidString.split("::")[1], types, names));

            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage()
                    + "\nInvalid JSON File: Verify format and values match that in RegisteredProducts File JSON file: "
                    + registeredProductsFile);
            ValidationProblem pW = new ValidationProblem(
                    new ProblemDefinition(ExceptionType.ERROR, ProblemType.INTERNAL_ERROR,
                            e.getMessage()
                                    + "\nInvalid Registered Context Product JSON File: Verify format and values match that in RegisteredProducts File JSON file: "
                                    + registeredProductsFile),
                    url);
            pList.add(pW);
        }
        
        if (nonRegisteredProducts) {
            
            try {
                gson = new Gson();
                JsonObject jsonN = gson.fromJson(new FileReader(nonRegisteredProductsFile), JsonObject.class);
                JsonArray arrayN = jsonN.get("Product_Context").getAsJsonArray();
                ValidationProblem pW = new ValidationProblem(
                        new ProblemDefinition(ExceptionType.WARNING, ProblemType.NON_REGISTERED_PRODUCT,
                                "Non-registered context products should only be used during archive development. All context products must be registered for a valid, released archive bundle. "),
                        url);
                pList.add(pW);

                for (JsonElement jsonElmN : arrayN) {

                    JsonObject jsonObjN = jsonElmN.getAsJsonObject();
                    String lidvidStringN = jsonObjN.get("lidvid").getAsString();
                    List<String> typesN = new ArrayList<String>();
                    if (!jsonObjN.get("type").isJsonNull()) {
                        for (JsonElement e : jsonObjN.get("type").getAsJsonArray()) {
                            typesN.add(e.getAsString());
                        }
                    } else {
                        typesN.add("N/A");
                    }
                    
                    List<String> namesN = new ArrayList<String>();
                    if (!jsonObjN.get("name").isJsonNull()) {
                        for (JsonElement e : jsonObjN.get("name").getAsJsonArray()) {
                            namesN.add(e.getAsString());
                        }
                    } else {
                        namesN.add("N/A");
                    }
                    contextProducts.add(new ContextProductReference(lidvidStringN.split("::")[0], lidvidStringN.split("::")[1], typesN,
                            namesN));
                }
                
                report.record(new URI(ValidateLauncher.class.getName()), pList);
            } catch (Exception e) {
                System.err.println(e.getMessage()
                        + "\nInvalid JSON File: Verify format and values match that in Non RegisteredProducts File JSON file: "
                        + nonRegisteredProductsFile);
            }

        }

        this.registeredAndNonRegistedProducts.put("Product_Context", contextProducts);
        //System.out.println("Total of LIDVID in registeredAndNonRegistedProducts: " + lidvids.size());
    }

    public void setUpdateRegisteredProducts(boolean updateRegisteredProducts) {
        this.updateRegisteredProducts = updateRegisteredProducts;
    }

    public void setNonRegisteredProducts(boolean nonRegisteredProducts) {
        this.nonRegisteredProducts = nonRegisteredProducts;
    }
    
    /**
     * Sets the flag that enables/disables context validation.
     * 
     * @param flag
     *            True or False.
     */
    public void setValidateContext(boolean flag) {
        this.validateContext = flag;
    }

    /**
     * Displays tool usage.
     *
     */
    public void displayHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(80, "validate <target> <options>", null, FlagOptions.getOptions(), null);
    }

    /**
     * Displays the current version and disclaimer notice.
     *
     * @throws IOException
     *             If there was an error that occurred while getting the tool
     *             information.
     */
    public void displayVersion() throws IOException {
        System.err.println("\n" + ToolInfo.getName());
        System.err.println(ToolInfo.getVersion());
        System.err.println("Release Date: " + ToolInfo.getReleaseDate());
        System.err.println(ToolInfo.getCopyright() + "\n");
    }

    /**
     * Setup the report.
     *
     * @throws IOException
     *             If an error occurred while setting up the report.
     */
    public void setupReport() throws IOException {
        if (this.reportStyle.equals("full")) {
            this.report = new FullReport();
        } else if (this.reportStyle.equals("json")) {
            this.report = new JSONReport();
        } else if (this.reportStyle.equals("xml")) {
            this.report = new XmlReport();
        }
        report.setLevel(severity);
        if (reportFile != null) {
            report.setOutput(reportFile);
        }

        if (this.deprecatedFlagWarning) {
            report.enableDeprecatedFlagWarning();
        }

        String version = ToolInfo.getVersion().replaceFirst("Version", "").trim();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = Calendar.getInstance().getTime();
        report.addConfiguration("   Version                       " + version);
        report.addConfiguration("   Date                          " + df.format(date));
        report.addParameter("   Targets                       " + targets);
        if (validationRule != null) {
            report.addParameter("   Rule Type                     " + validationRule);
        }
        if (!schemas.isEmpty()) {
            report.addParameter("   User Specified Schemas        " + schemas);
        }
        if (!catalogs.isEmpty()) {
            report.addParameter("   User Specified Catalogs       " + catalogs);
        }
        if (!schematrons.isEmpty()) {
            report.addParameter("   User Specified Schematrons    " + schematrons);
        }
        report.addParameter("   Severity Level                " + severity.getName());
        report.addParameter("   Recurse Directories           " + traverse);
        if (!regExps.isEmpty()) {
            report.addParameter("   File Filters Used             " + regExps);
        }
        /*
         * Deprecated issue-23 if (force) {
         * report.addParameter("   Force Mode                    on"); } else {
         * report.addParameter("   Force Mode                    off"); }
         */
        if (checksumManifest != null) {
            report.addParameter("   Checksum Manifest File        " + checksumManifest.toString());
            report.addParameter("   Manifest File Base Path       " + manifestBasePath.toString());
        }
        if (contentValidationFlag) {
            report.addParameter("   Data Content Validation       on");
        } else {
            report.addParameter("   Data Content Validation       off");
        }
        
        if (!skipProductValidation) {
        	report.addParameter("   Product Level Validation      on");
        } else {
            report.addParameter("   Product Level Validation      off");
        }
        if (spotCheckData != -1) {
            report.addParameter("   Data Spot Check               " + spotCheckData);
        }
        if (validationRule != null && (validationRule.equalsIgnoreCase("pds4.bundle")
                || validationRule.equalsIgnoreCase("pds4.collection"))) {
            report.addParameter("   Allow Unlabeled Files         " + allowUnlabeledFiles);
        }
        report.addParameter("   Max Errors                    " + maxErrors);
        report.addParameter("   Registered Contexts File      " + registeredProductsFile.toString());
        if (nonRegisteredProductsFile != null)
            report.addParameter("   Non Registered Contexts File  " + nonRegisteredProductsFile.toString());
        report.printHeader();
    }

    /**
     * Performs validation.
     * 
     * @param checksumManifest
     * @return boolean
     *         true - success
     *         false - fail
     * @throws Exception
     */
    public boolean doValidation(Map<URL, String> checksumManifest) throws Exception {
        boolean success = true;
        long t0 = System.currentTimeMillis();
        // Initialize the Factory Class
        List<DocumentValidator> docValidators = new ArrayList<DocumentValidator>();
        factory = ValidatorFactory.getInstance();
        factory.setDocumentValidators(docValidators);

        // Set the name of this executable and the report in LabelUtil so any errors/warnings can be reported.
        LabelUtil.setLauncherURIName(new URI(ValidateLauncher.class.getName()).toString());
        LabelUtil.setReport(report);

        // Explicitly clear out all elements in the list of Information Model Versions.
        // The reason is for regression tests, the list of Information Model Versions grows if a previous run for label may contains
        // one IM version and the current bundle may contains another version, the code will incorrectly gives a WARNING that it has multiple versions.
        LabelUtil.hardResetInformationModelVersions();

        // Due to the util class ReferentialIntegrityUtil being static, the flag contextReferenceCheck must be set manually to skip reference check.
        ReferentialIntegrityUtil.setContextReferenceCheckFlag(this.contextReferenceCheck);

        for (URL target : targets) {
            try {
                LocationValidator validator = factory.newInstance(severity);

                // If the user requested to check in between the fields, set it here in the validator.
                // Note that it is important to perform a set regardless of the value of checkInbetweenFields,
                // otherwise when the code performs a get, it won't have a value in the dictionary to fetch.
                if (this.checkInbetweenFields) {
                    validator.setCheckInbetweenFields(true);
                } else {
                    validator.setCheckInbetweenFields(false);
                }

                //System.out.println("TARGET   : " + target);
                //System.out.println("SEVERITY : " + severity);
                validator.setForce(force);
                validator.setFileFilters(regExps);
                validator.setRecurse(traverse);
                validator.setCheckData(contentValidationFlag);
                validator.setSpotCheckData(spotCheckData);
                validator.setAllowUnlabeledFiles(allowUnlabeledFiles);
                validator.setValidateContext(validateContext);
                validator.setSkipProductValidation(skipProductValidation);
                validator.setRegisteredProducts(this.registeredAndNonRegistedProducts); //this map may include Non registered products  
                if (!checksumManifest.isEmpty()) {
                    validator.setChecksumManifest(checksumManifest);
                }
                validator.setTargetRegistrar(new InMemoryRegistrar());
                ValidationMonitor monitor = new ValidationMonitor(target.toString(), severity);
                monitor.setMaxErrors(maxErrors);

                if (validationRule != null) {
                    validator.setRule(validationRule);
                }

                if (!schemas.isEmpty()) {
                    validator.setSchema(schemas);
                    validator.setCachedEntityResolver(resolver);
                    validator.setCachedLSResourceResolver(schemaValidator.getCachedLSResolver());
                }
                if (!catalogs.isEmpty()) {
                    validator.setCatalogs(catalogs);
                }
                if (!transformedSchematrons.isEmpty()) {
                    validator.setSchematrons(transformedSchematrons);
                }
                if (!this.alternateReferentialPaths.isEmpty()) {
                    validator.setExtraTargetInContext(this.alternateReferentialPaths);
                }

                LOG.debug("ValidateLauncher:doValidation: validator.validate():target {}",target);
                validator.validate(monitor, target);
                monitor.endValidation();

                if (validationRule != null) {
                    // If the rule is pds4.label, clear out the list of Information Model Versions except the first element.
                    if (validationRule.equals("pds4.label")) {
                        LabelUtil.reduceInformationModelVersions();
                    }
                }

                if (monitor.numErrors > 0) { success = false; }
                LOG.debug("ValidateLauncher:doValidation: monitor.numErrors,target,success {},{},{}",monitor.numErrors,target,success);
            } catch (Exception e) {
                ValidationProblem p = null;
                if (e instanceof MissingLabelSchemaException) {
                    MissingLabelSchemaException mse = (MissingLabelSchemaException) e;
                    p = new ValidationProblem(
                            new ProblemDefinition(ExceptionType.WARNING, ProblemType.MISSING_SCHEMA, mse.getMessage()),
                            target);
                    try {
                        report.recordSkip(target.toURI(), p);
                    } catch (URISyntaxException u) {
                        // Ignore. Should not happen!!!
                    }
                } else {
                    if (e instanceof SAXParseException) {
                        SAXParseException se = (SAXParseException) e;
                        p = new ValidationProblem(
                                new ProblemDefinition(ExceptionType.FATAL, ProblemType.SCHEMA_ERROR, se.getMessage()),
                                target, se.getLineNumber(), se.getColumnNumber());
                    } else {
                        p = new ValidationProblem(
                                new ProblemDefinition(ExceptionType.FATAL, ProblemType.INTERNAL_ERROR, e.getMessage()),
                                target);
                    }
                    try {
                        report.record(target.toURI(), p);
                    } catch (URISyntaxException u) {
                        // Ignore. Should not happen!!!
                    }
                }
            }
        }

        // https://github.com/NASA-PDS/validate/issues/210 As a user, I want validate to raise a WARNING when differing versions of IM are used within a bundle
        // Report a WARNING if more than one versions of the Information Model (IM) is used in this run.
        // At this point, all the versions of the IM would have been collected by the class LabelUtil, we merely need to call reportIfMoreThanOneVersion()
        // to request appending any warning messages to the report.

        LabelUtil.reportIfMoreThanOneVersion(validationRule);

        if (severity.isDebugApplicable()) {
            System.out.println("\nDEBUG  [" + ProblemType.TIMING_METRICS.getKey() + "]  " + System.currentTimeMillis() + " :: Validation complete (" + targets.size() + " targets completed in " + (System.currentTimeMillis() - t0) + " ms)\n");
        }

        // Print some WARNING messages if the user specified additional paths for referential integrity checks.
        if (!this.alternateReferentialPaths.isEmpty()) {
            this.printWarningCollocatedData(alternateReferentialPaths);
        }

        // Due to the util class ReferentialIntegrityUtil being static, it need to be reset() if running a regression test.
        ReferentialIntegrityUtil.reset();

        return success;
    }

    /**
     * Print WARNING messages for collocated data.  If data does not exist, print ERROR message.
     * @param alternateReferentialPaths
     *    List of URL of alternate paths to bundle/collection data.
    */
    private void printWarningCollocatedData(ArrayList<URL> alternateReferentialPaths) {
        for (URL url : alternateReferentialPaths) {
          // Do a sanity check if url exist first before attempting to report on collocated data.
          try {
            boolean fileExistFlag = false;
            LOG.debug("printWarningCollocatedData:url.getPath(),(new File(url.getPath()).exists() {},{}",url.getPath(),(new File(url.getPath()).exists()));
            if (new File(url.getPath()).exists()) {
                ValidationProblem p1 = new ValidationProblem(new ProblemDefinition(ExceptionType.WARNING,
                    ProblemType.GENERAL_INFO, "This data should be collocated with the other bundle data"),url);
                report.record(url.toURI(), p1);
            } else {
                ValidationProblem p1 = new ValidationProblem(new ProblemDefinition(ExceptionType.ERROR,
                    ProblemType.GENERAL_INFO, "This provided data directory does not exist"),url);
                report.record(url.toURI(), p1);
            }
          } catch (Exception e) {
            LOG.error("Cannot perform checking on existence of url {}",url);
          }
        }
    }

    /**
     * Transforms a given schematron.
     *
     * @param schematron
     *            A schematron to transform.
     * @param container
     *            Container to hold problems.
     *
     * @return The ISO Schematron transformer associated with the given
     *         schematron.
     *
     * @throws TransformerException
     *             If an error occurred during the transform process.
     */
    private Transformer transformSchematron(URL schematron, ProblemContainer container) {
        Transformer transformer = null;
        try {
            transformer = schematronTransformer.transform(schematron, container);
            return transformer;
        } catch (Exception e) {
            container.addProblem(new ValidationProblem(
                    new ProblemDefinition(ExceptionType.FATAL, ProblemType.SCHEMATRON_ERROR,
                            "Error occurred while processing schematron '" + schematron + "': " + e.getMessage()),
                    schematron));
        }
        return null;
    }

    /**
     * Validates a given schema.
     *
     * @param schema
     *            The URL to the schema.
     *
     * @return 'true' if the schema was valid, 'false' otherwise.
     * @throws Exception
     */
    private boolean validateSchemas(List<URL> schemas) throws Exception {
        boolean isValid = true;
        List<StreamSource> sources = new ArrayList<StreamSource>();
        String locations = "";
        for (URL schema : schemas) {
            LSInput input = schemaValidator.getCachedLSResolver().resolveResource("", "", "", schema.toString(),
                    schema.toString());
            if (input != null) {
                StreamSource streamSource = new StreamSource(input.getByteStream());
                streamSource.setSystemId(schema.toString());
                sources.add(streamSource);
                try {
                    InputSource inputSource = new InputSource(input.getByteStream());
                    inputSource.setSystemId(input.getSystemId());
                    // TODO: Should we log an error if the targetNamespace is
                    // missing?
                    XMLExtractor extractor = new XMLExtractor(inputSource);
                    String namespace = extractor.getTargetNamespace();
                    if (!namespace.isEmpty()) {
                        locations += namespace + " " + schema + "\n";
                    }
                    inputSource.getByteStream().reset();
                } catch (Exception e) {
                    throw new Exception("Error while getting targetNamespace of schema '" + schema.toString() + "': "
                            + e.getMessage());
                }
            } else {
                System.err.println("Error while trying to read in '" + schema.toString() + "'");
                isValid = false;
            }
        }
        schemaValidator.setExternalLocations(locations);
        for (StreamSource source : sources) {
            ProblemContainer problems = schemaValidator.validate(source);
            if (problems.getProblems().size() != 0) {
                isValid = false;
                report.record(new URI(source.getSystemId()), problems.getProblems());
            }
        }
        return isValid;
    }

    /**
     * Print the report footer.
     *
     */
    public void printReportFooter() {
        report.printFooter();
    }
    
    /**
     * Wrapper method for the main class that returns valid exitCode
     * for execution.
     *
     * @param args
     *            list of command-line arguments.
     */
    public int processMain(String[] args) throws Exception {
        boolean success = true;
        long t0 = System.currentTimeMillis();
        try {
            CommandLine cmdLine = parse(args);
            query(cmdLine);

            if (targets.size() == 0 && !updateRegisteredProducts) { // Throw error if no targets are specified
                throw new InvalidOptionException("No files specified for validation. Check your paths and use -t flag to explicitly denote the set of target data.");
            }

            Map<URL, String> checksumManifestMap = new HashMap<URL, String>();
            if (checksumManifest != null) {
                if (manifestBasePath == null) {
                    URL base = null;
                    Target t = Utility.toTarget(targets.get(0));
                    try {
                        if (t.isDir()) {
                            base = t.getUrl();
                        } else {
                            base = t.getUrl().toURI().getPath().endsWith("/") ? t.getUrl().toURI().resolve("..").toURL()
                                    : t.getUrl().toURI().resolve(".").toURL();
                        }
                        manifestBasePath = base;
                    } catch (URISyntaxException ue) {
                        throw new IOException("Error occurred while getting parent of '" + t + "': " + ue.getMessage());
                    }
                }
                ChecksumManifest cm = new ChecksumManifest(manifestBasePath.toString());
                try {
                    checksumManifestMap.putAll(cm.read(checksumManifest));
                } catch (IOException io) {
                    throw new Exception("Error occurred while reading checksum " + "manifest file '"
                            + checksumManifest.toString() + "': " + io.getMessage());
                }
            }
            setupReport();
            
            if (updateRegisteredProducts) {
                // download the latest Registered Context Products JSON file and
                // replace the existing file.
                getLatestJsonContext();
            }

            // Validate schemas and schematrons first before performing label
            // validation
            boolean invalidSchemas = false;
            if (!schemas.isEmpty()) {
                if (!validateSchemas(schemas)) {
                    invalidSchemas = true;
                    success = false;
                }
            }
            boolean invalidSchematron = false;
            if (!schematrons.isEmpty()) {
                for (URL schematron : schematrons) {
                    ProblemContainer container = new ProblemContainer();
                    Transformer transformer = transformSchematron(schematron, container);
                    if (container.getProblems().size() != 0) {
                        report.record(schematron.toURI(), container.getProblems());
                        invalidSchematron = true;
                        success = false;
                    } else {
                        transformedSchematrons.add(transformer);
                    }
                }
            }
            if (!(invalidSchemas) && !(invalidSchematron)) {
                setRegisteredProducts();
                if (!doValidation(checksumManifestMap)) success = false;
            }
            printReportFooter();
            if (severity.isDebugApplicable()) {
                System.out.println("DEBUG  [" + ProblemType.TIMING_METRICS.getKey() + "]  " + System.currentTimeMillis() + " :: processMain() in " + (System.currentTimeMillis() - t0) + " ms\n");
            }
        } catch (Exception e) {
            throw new Exception(e);
        }

        return (success) ? 0 : 1;
    }
    
    /**
     * Flush ValidatorFactory cache (set to null). The ValidatorFactory
     * will cache schemas and other validation data between runs, which can
     * cause issues, specifically when using varying dictionaries and catalog files. 
     */
    public void flushValidators() {
        if (this.factory != null) {
            this.factory.flush();
        }
    }

    /**
     * Main class that launches the Validate Tool.
     *
     * @param args
     *            A list of command-line arguments.
     * @throws TransformerConfigurationException
     */
    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws TransformerConfigurationException {
        long t0 = System.currentTimeMillis();
        System.setProperty("https.protocols", "TLSv1.2");
        if (args.length == 0) {
            System.out.println("\nType 'validate -h' for usage");
            System.exit(0);
        }

        ConsoleAppender ca = new ConsoleAppender(new PatternLayout("%-5p %m%n"));
        ca.setThreshold(Priority.FATAL);
        BasicConfigurator.configure(ca);

        int exitCode = 0;
        try {
            exitCode = new ValidateLauncher().processMain(args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        System.out.println("Completed execution in " + (System.currentTimeMillis()-t0) + " ms\n");
        System.exit(exitCode);
    }

    /**
     * A validation monitor that coalesces exceptions by location and summarizes
     * into the report.
     */
    private class ValidationMonitor implements ValidateProblemHandler {

        private Map<String, ProblemContainer> exceptions = new LinkedHashMap<String, ProblemContainer>();
        private String rootLocation;
        private ExceptionType verbosityLevel;
        private long maxErrors;
        private long numErrors;

        public ValidationMonitor(String rootLocation, ExceptionType severity) {
            this.rootLocation = rootLocation;
            this.verbosityLevel = severity;
            maxErrors = MAX_ERRORS;
            numErrors = 0;
        }

        @Override
        public void addProblem(ValidationProblem problem) {
            if (problem.getProblem().getSeverity().getValue() <= verbosityLevel.getValue()) {
                String location = rootLocation;
                if (problem instanceof ContentProblem) {
                    ContentProblem cp = (ContentProblem) problem;
                    location = cp.getLabel().toString();
                } else {
                    if (problem.getSource() != null) {
                        location = problem.getSource();
                    }
                }
                addLocation(location);
                exceptions.get(location).addProblem(problem);
                if (problem.getProblem().getSeverity() == ExceptionType.ERROR || problem.getProblem().getSeverity() == ExceptionType.FATAL) {
                    numErrors++;
                }
                if (numErrors >= maxErrors) {
                    endValidation();
                    printReportFooter();
                    System.err
                            .println("\n\nERROR: Validation run terminated due to an excessive amount of errors.\n\n");
                    System.exit(1);
                }
            }
        }

        public void printHeader(String title) {
            report.printHeader(title);
        }

        public void record(String location) {
            URI uri = null;
            try {
                uri = new URI(location);
            } catch (URISyntaxException e) {
                // Should not happen - ignore.
            }
            if (exceptions.get(location) != null) {
                report.record(uri, exceptions.get(location).getProblems());
                exceptions.remove(location);
            }
        }

        public void endValidation() {
            for (String location : exceptions.keySet()) {
                URI uri = null;
                try {
                    uri = new URI(location);
                } catch (URISyntaxException e) {
                    // Should not happen - ignore.
                }
                report.record(uri, exceptions.get(location).getProblems());
            }
        }

        public void addLocation(String location) {
            if (!exceptions.containsKey(location)) {
                ProblemContainer container = new ProblemContainer();
                exceptions.put(location, container);
            }
        }

        public void setMaxErrors(long value) {
            this.maxErrors = value;
        }
    }

}
