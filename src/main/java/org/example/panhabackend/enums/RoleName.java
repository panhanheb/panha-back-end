package org.example.panhabackend.enums;

/** Security roles. Stored in the roles table with the ROLE_ prefix expected by Spring Security. */
public enum RoleName {
    ROLE_ADMIN,
    ROLE_TEACHER,
    ROLE_STUDENT
}
