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
package gov.nasa.pds.tools.validate;

/**
 * Defines the types of problems that can be reported by validation rules.
 */
public enum ProblemType {

    MISSING_REQUIRED_RESOURCE("error.validation.missing_required_file"),

    FILE_NAMING_PROBLEM("error.validation.file_naming_problem"),

    MISSING_REFERENCED_FILE("error.label.missing_file"),

    INVALID_LABEL("error.validation.invalid_label"),

    EMPTY_FOLDER("error.validation.empty_folder"),

    UNKNOWN_VALUE("error.validation.unknown_value"),

    TABLE_DEFINITION_PROBLEM("error.validation.table_definition_problem"),

    TABLE_DEFINITION_MISMATCH("error.validation.table_definition_mismatch"),

    INVALID_FIELD_VALUE("error.validation.invalid_field_value"),

    INVALID_CHARACTER_STREAM("error.validation.invalid_character_stream"),

    DUPLICATE_IDENTIFIER("error.label.duplicate_identifier"),

    CONTEXT_REFERENCE_NOT_FOUND("error.label.context_ref_not_found"),

    OUT_OF_MEMORY("error.validation.out_of_memory"),

    INTERNAL_ERROR("error.validation.internal_error"),

    CHECKSUM_MISMATCH("error.label.checksum_mismatch"),

    MISSING_CHECKSUM("error.label.missing_checksum"),

    FILESIZE_MISMATCH("error.label.filesize_mismatch"),

    MISSING_FILESIZE("error.label.missing_filesize"),

    SCHEMA_ERROR("error.label.schema"),

    SCHEMATRON_ERROR("error.label.schematron"),

    LABEL_UNRESOLVABLE_RESOURCE("error.label.unresolvable_resource"),

    MISSING_SCHEMA("error.label.missing_schema"),

    BAD_EXTENSION("error.label.bad_extension"),

    FILE_NAME_TOO_LONG("error.file.name_too_long"),

    FILE_NAME_HAS_INVALID_CHARS("error.file.name_has_invalid_characters"),

    UNALLOWED_FILE_NAME("error.file.unallowed_name"),

    UNALLOWED_BASE_NAME("error.file.unallowed_base_name"),

    DIR_NAME_TOO_LONG("error.directory.name_too_long"),

    DIR_NAME_HAS_INVALID_CHARS("error.directory.name_has_invalid_characters"),

    UNALLOWED_BUNDLE_SUBDIR_NAME("error.sub_directory.unallowed_name"),

    UNALLOWED_DIRECTORY_NAME("error.directory.unallowed_name"),

    INVALID_COLLECTION_NAME("error.bundle.invalid_collection_name"),

    UNEXPECTED_FILE_IN_BUNDLE_ROOT("error.bundle.invalid_file_in_root_directory"),

    INVALID_MEMBER_STATUS("error.inventory_table.invalid_member_status"),

    // Referential Integrity Checking messages

    DUPLICATE_VERSIONS("error.integrity.duplicate_versions"),

    DUPLICATE_MEMBERS("error.integrity.duplicate_members"),

    MISSING_PARENT_PREFIX("error.integrity.missing_parent_prefix"),

    MISSING_VERSION("error.integrity.missing_version"),

    // Catalog related messages

    CATALOG_UNRESOLVABLE_RESOURCE("error.catalog.unresolvable_resource"),

    CATALOG_UNRESOLVABLE_SCHEMA("error.catalog.unresolvable_schema"),

    CATALOG_UNRESOLVABLE_SCHEMATRON("error.catalog.unresolvable_schematron"),

    // Array Content messages

    ARRAY_INTERNAL_ERROR("error.array.internal_error"),

    ARRAY_DATA_FILE_READ_ERROR("error.array.bad_file_read"),

    ARRAY_VALUE_OUT_OF_DATA_TYPE_RANGE("error.array.value_out_of_data_type_range"),

    ARRAY_VALUE_OUT_OF_MIN_MAX_RANGE("error.array.value_out_of_min_max_range"),

    ARRAY_VALUE_OUT_OF_SCALED_MIN_MAX_RANGE("error.array.value_out_of_scaled_min_max_range"),

    ARRAY_VALUE_IS_SPECIAL_CONSTANT("info.array.is_special_constant"),

    // Table Content messages

    TABLE_INTERNAL_ERROR("error.table.internal_error"),

    TABLE_FILE_READ_ERROR("error.table.bad_file_read"),

    MISSING_CRLF("error.table.missing_CRLF"),

    MISSING_LF("error.table.missing_LF"),

    RECORD_LENGTH_MISMATCH("error.table.record_length_mismatch"),

    RECORDS_MISMATCH("error.table.records_mismatch"),

    BIT_FIELD_MISMATCH("error.table.bit_field_mismatch"),

    FIELDS_MISMATCH("error.table.fields_mismatch"),

    FIELD_VALUE_OUT_OF_MIN_MAX_RANGE("error.table.field_value_out_of_min_max_range"),

    FIELD_VALUE_TOO_LONG("error.table.field_value_too_long"),

    FIELD_VALUE_DATA_TYPE_MISMATCH("error.table.field_value_data_type_mismatch"),
    
    FIELD_VALUE_OVERLAP("error.table.field_value_overlap"),

    BAD_FIELD_READ("error.table.bad_field_read"),

    FIELD_VALUE_NOT_A_NUMBER("error.table.field_value_not_a_number"),

    FIELD_VALUE_NOT_RIGHT_JUSTIFIED("error.table.field_value_not_right_justified"),

    FIELD_VALUE_NOT_LEFT_JUSTIFIED("error.table.field_value_not_left_justified"),

    FIELD_VALUE_FORMAT_SPECIFIER_MISMATCH("error.table.field_value_format_specifier_mismatch"),

    FIELD_VALUE_FORMAT_PRECISION_MISMATCH("error.table.field_value_format_precision_mismatch"),

    LOCAL_IDENTIFIER_NOT_FOUND("error.label.local_identifier_not_found"),

    // Warning message types

    SCHEMA_WARNING("warning.label.schema"),

    MISSING_SCHEMATRON_SPEC("warning.label.missing_schematron_spec"),

    BAD_SCHEMATYPENS("warning.label.bad_schematypens"),

    MISSING_SCHEMATYPENS("warning.label.missing_schematypens"),

    SCHEMATRON_WARNING("warning.label.schematron"),

    FILE_REFERENCE_CASE_MISMATCH("warning.label.file_ref_case_mismatch"),

    ARRAY_INTERNAL_WARNING("warning.array.internal_warning"),

    MEMBER_NOT_FOUND("warning.integrity.member_not_found"),

    // A warning when a reference is in the Context_Area but not listed in a bundle/collection's Reference_List tag. 
    // Will become an ERROR once it is documented in the Standards Reference
    MISSING_CONTEXT_REFERENCE("warning.integrity.missing_context_reference"),

    UNREFERENCED_MEMBER("warning.integrity.unreferenced_member"),

    UNLABELED_FILE("warning.file.not_referenced_in_label"),

    NON_PDFA_FILE("warning.pdf.file.not_pdfa_compliant"),

    NON_REGISTERED_PRODUCT("warning.product_not_registered"),
    
    CHARS_BETWEEN_FIELDS("warning.table.characters_between_fields"),
    

    // Info message types

    GENERAL_INFO("info.validation.general"),

    CHECKSUM_MATCHES("info.label.checksum_matches"),

    FILESIZE_MATCHES("info.label.filesize_matches"),

    MISSING_CHECKSUM_INFO("info.label.missing_checksum"),

    MISSING_FILESIZE_INFO("info.label.missing_filesize"),

    SCHEMATRON_INFO("info.label.schematron"),

    BLANK_FIELD_VALUE("info.table.blank_field_value"),

    MEMBER_FOUND("info.integrity.member_found"),

    REFERENCED_MEMBER("info.integrity.referenced_member"),

    DUPLICATE_MEMBERS_INFO("info.integrity.duplicate_members"),

    PARENT_PREFIX_FOUND("info.integrity.parent_prefix_found"),
    
    CONTEXT_REFERENCE_FOUND("info.label.context_ref_found"),
    
    CONTEXT_REFERENCE_FOUND_MISMATCH("info.label.context_ref_mismatch"),
    
    CONTEXT_REFERENCE_FOUND_CASE_MISMATCH("info.label.context_ref_case_mismatch"),

    LOCAL_ID_FOUND("info.label.local_identifier_found"),

    // Debug messages (Should only be used for debugging purposes)
    CRLF_DETECTED("debug.table.record_has_CRLF"),

    RECORD_MATCH("debug.table.record_match"),

    GOOD_RECORD_LENGTH("debug.table.good_record_length"),

    FIELD_VALUE_FORMAT_MATCH("debug.table.field_value_format_match"),

    FIELD_VALUE_IN_MIN_MAX_RANGE("debug.table.field_value_in_min_max_range"),

    FIELD_VALUE_DATA_TYPE_MATCH("debug.table.field_value_matches_data_type"),

    BIT_FIELD_MATCH("debug.table.bit_field_match"),
    
    TIMING_METRICS("debug.execution.time");

    private final String key;

    private ProblemType(String key) {
        this.key = key;
    }

    /**
     * Gets the key for mapping the problem type to a UI string.
     *
     * @return the key string
     */
    public String getKey() {
        return key;
    }

}
