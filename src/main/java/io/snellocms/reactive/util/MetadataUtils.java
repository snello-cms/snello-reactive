package io.snellocms.reactive.util;

import static io.snello.management.AppConstants.*;

public class MetadataUtils {


    public static boolean isReserved(Object table_name) {
        String name = (String) table_name;
        switch (name) {
            case CONDITIONS:
            case DATALISTS:
            case DOCUMENTS:
            case EXTENSIONS:
            case DRAGGABLES:
            case DROPPABLES:
            case FIELD_DEFINITIONS:
            case LINKS:
            case METADATAS:
            case PUBLIC_DATA:
            case ROLES:
            case SELECT_QUERY:
            case USERS:
            case USER_ROLES:
            case CHANGE_PASSWORD_TOKENS:
                return true;
            default:
                return false;
        }
    }
}
