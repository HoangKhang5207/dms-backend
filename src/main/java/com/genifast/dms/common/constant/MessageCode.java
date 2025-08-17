package com.genifast.dms.common.constant;

/**
 * A message has format = {module}.{message_type}.{code} Example:
 * common.error.required
 */
public final class MessageCode {
    private static final String DELIMITER = ".";
    private static final String ERROR_MSG = "error";
    private static final String SUCCESS_MSG = "success";
    private static final String CONFIRM_MSG = "confirm";
    private static final String INFO_MSG = "info";

    private MessageCode() {
    }

    // Common message
    public static class CommonMessage {
        public static final String ERROR_REQUIRED = ERROR_MSG + DELIMITER + "required";
        public static final String ERROR_INVALID_PAYLOAD = ERROR_MSG + DELIMITER + "invalid_payload";
        public static final String ERROR_CONTAIN_CSV_SPECIAL_CHARACTER = ERROR_MSG + DELIMITER
                + "contain_csv_special_char";
        public static final String ERROR_RECORD_NOT_FOUND = getMessageString(ERROR_MSG, "record_not_found");
        public static final String ERROR_RECORD_DUPLICATE = getMessageString(ERROR_MSG, "record_duplicate");
        public static final String ERROR_OCCURRED_DURING_UPDATE = getMessageString(ERROR_MSG, "occurred_during_update");
        public static final String ERROR_SOMETHING_ERROR = getMessageString(ERROR_MSG, "something_error");
        public static final String ERROR_HAVE_NO_PERMISSION = getMessageString(ERROR_MSG, "have_no_permission");
        public static final String INFO_NO_DATA_AVAILABLE = getMessageString(INFO_MSG, "no_data_available");
        public static final String INFO_NO_TABLE_DATA_AVAILABLE = getMessageString(INFO_MSG, "no_table_data_available");
        public static final String SUCCESS_GET_DATA = getMessageString(SUCCESS_MSG, "get_data");
        public static final String SUCCESS_CREATE_DATA = getMessageString(SUCCESS_MSG, "create_data");
        public static final String SUCCESS_UPDATE_DATA = getMessageString(SUCCESS_MSG, "update_data");
        public static final String SUCCESS_DELETE_DATA = getMessageString(SUCCESS_MSG, "delete_data");
        public static final String NOT_XLSX_FILE = getMessageString(SUCCESS_MSG, "not_xlsx_file");
        public static final String INVALID_DATA_FILE = getMessageString(ERROR_MSG, "invalid_data_file");

        public static final String EMPTY_DATA_FILE = getMessageString(INFO_MSG, "empty_data_file");
        public static final String NOT_TEMPLATE_FILE = getMessageString(ERROR_MSG, "not_template_file");
        public static final String END_DATE_BEFORE_START_DATE = getMessageString(ERROR_MSG,
                "end_date_before_start_date");
        public static final String INVALID_STATUS = getMessageString(ERROR_MSG, "invalid_status");

        private static String getMessageString(String type, String messageCode) {
            return String.join(DELIMITER, type, messageCode);
        }
    }
}
