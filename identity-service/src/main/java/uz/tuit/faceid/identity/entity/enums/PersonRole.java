package uz.tuit.faceid.identity.entity.enums;

import java.util.Arrays;

/**
 * Role of a subject. The access-control service picks an access rule by this
 * value, so the set of codes must stay identical in both services.
 */
public enum PersonRole {

    EMPLOYEE("employee"),
    MANAGER("manager"),
    GUEST("guest"),
    SECURITY("security");

    public static final PersonRole DEFAULT = EMPLOYEE;

    private final String code;

    PersonRole(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /** Unknown or blank input falls back to the default role. */
    public static PersonRole fromCode(String code) {
        if (code == null || code.isBlank()) {
            return DEFAULT;
        }
        return Arrays.stream(values())
                .filter(role -> role.code.equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElse(DEFAULT);
    }
}
