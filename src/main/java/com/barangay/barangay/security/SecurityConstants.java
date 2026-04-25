package com.barangay.barangay.security;

public class SecurityConstants {

    public static final String ROLE_ROOT_ADMIN = "ROOT_ADMIN";
    public static final String ROLE_ADMIN = "ADMIN";


    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/error",
            "/api/v1/users/{UserId}/lock",
            "/api/v1/users/update-status",



    };


    public static final String[] ADMIN_ENDPOINTS = {
            "/api/v1/user-management/stats",


    };

    public static final String[] ROOT_ADMIN_ENDPOINTS = {
            //audit endpoint
            //departments endpoint
            "/api/v1/departments/options",

            "/api/v1/dashboard/stats",
            "/api/v1/dashboard/activity-overview",
            "/api/v1/dashboard/recent-actions",

            //
            "/api/v1/users/create-admin",
                "/api/v1/users/update-admin",
    };

    public static final String[] ALLOWED_ORIGINS = {
            "http://localhost:5173",
            "http://localhost:3000",
            "https://baranggay-management.vercel.app/"
    };

    public static final String[] ALLOWED_METHODS = {"GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"};
    public static final String[] ALLOWED_HEADERS = {"*"};


    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";

    private SecurityConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}