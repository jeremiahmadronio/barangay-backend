package com.barangay.barangay.auth.service;

import com.barangay.barangay.audit.service.AuditLogService;
import com.barangay.barangay.auth.dto.*;
import com.barangay.barangay.department.model.Department;
import com.barangay.barangay.enumerated.MfaType;
import com.barangay.barangay.enumerated.Severity;
import com.barangay.barangay.enumerated.Status;
import com.barangay.barangay.security.CustomUserDetails;
import com.barangay.barangay.security.JwtService;
import com.barangay.barangay.admin_management.model.User;
import com.barangay.barangay.admin_management.repository.Root_AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final Root_AdminRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditLogService auditLogService;
    private final MfaService mfaService;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;


    @Transactional(noRollbackFor = {BadCredentialsException.class, LockedException.class, DisabledException.class})
    public LoginResponse authenticate(Login request, String ipAddress) {

        User user = userRepository.findBySystemEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("No account found with this email"));

        // --- 1. ACCOUNT LOCK CHECK ---
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
                long minutesLeft = Math.max(1, ChronoUnit.MINUTES.between(LocalDateTime.now(), user.getLockUntil()));

                auditLogService.log(user, null, "Login Authentication", Severity.WARNING,
                        "Attempt on locked account", ipAddress, "Locked until: " + user.getLockUntil(), null, null);

                throw new LockedException("Account is temporarily locked. Try again in " + minutesLeft + " minutes.");
            }
            // Unlock if time expired
            user.setIsLocked(false);
            user.setLockUntil(null);
            userRepository.saveAndFlush(user);
        }

        // --- 2. STATUS CHECK ---
        if (user.getStatus() != Status.ACTIVE) {
            auditLogService.log(user, null, "Login Authentication", Severity.WARNING,
                    "Attempt on inactive account", ipAddress, "Status: " + user.getStatus(), null, null);
            throw new DisabledException("Account is inactive. Contact your administrator.");
        }

        // --- 3. CREDENTIAL VERIFICATION & LOCKING LOGIC ---
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (BadCredentialsException e) {
            LocalDateTime now = LocalDateTime.now();
            int attempts = (user.getFailedAttempts() == null ? 0 : user.getFailedAttempts()) + 1;

            user.setFailedAttempts(attempts);
            user.setLastFailedAttempt(now);

            String lockMsg = null;
            if (attempts >= 5) {
                user.setIsLocked(true);
                lockMsg = "Account locked please contact your administrator)";
            }

            userRepository.saveAndFlush(user);
            auditLogService.log(user, null, "Login Authentication", Severity.WARNING,
                    (lockMsg != null ? lockMsg : "Failed attempt #" + attempts), ipAddress, "Auth failure", null, null);

            if (user.getIsLocked()) throw new LockedException(lockMsg);
            throw new BadCredentialsException("Invalid email or password. Attempt " + attempts + ".");
        }

        user.setFailedAttempts(0);
        user.setIsLocked(false);
        user.setLockUntil(null);
        user.setLastFailedAttempt(null);

        String code = mfaService.generateCode();
        user.setMfaCode(code);
        user.setMfaExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        mfaService.sendMfaEmail(user.getSystemEmail(), code);

        auditLogService.log(user, null, "Login Authentication", Severity.INFO,
                "OTP sent to primary email", ipAddress, null, null, null);

        return new LoginResponse(
                "MFA_REQUIRED",
                user.getId(),
                user.getRole().getRoleName(),
                null,
                null,
                user.isTotpEnabled(),
                user.getSystemBackupEmail() != null
        );
    }


    @Transactional
    public LoginResponse verifyMfa(MfaRequest request, String ipAddress) {
        User user = userRepository.findBySystemEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("No account found with this email: " + request.email()));

        boolean isVerified = false;


        switch (request.type()) {
            case TOTP -> {
                if (!user.isTotpEnabled() || user.getTotpSecret() == null) {
                    throw new BadCredentialsException("Authenticator app is not set up for this account.");
                }
                isVerified = totpService.verifyCode(user.getTotpSecret(), request.code());
            }

            case RECOVERY -> {
                if (user.getRecoveryCodes() != null && user.getRecoveryCodes().contains(request.code())) {
                    user.getRecoveryCodes().remove(request.code());
                    isVerified = true;
                }
            }

            case EMAIL, BACKUP_EMAIL -> {
                isVerified = (user.getMfaCode() != null &&
                        user.getMfaCode().equals(request.code()) &&
                        user.getMfaExpiry().isAfter(LocalDateTime.now()));
            }

            default -> throw new BadCredentialsException("Unsupported MFA Type.");
        }

        if (!isVerified) {
            auditLogService.log(user, null, "Login Authentication", Severity.WARNING,
                    "Failed MFA verification attempt", ipAddress,
                    "Method: " + request.type(), null, "User ID: " + user.getId());

            throw new BadCredentialsException("Invalid or expired verification code. Please try again.");
        }

        user.setMfaCode(null);
        user.setMfaExpiry(null);
        user.setFailedAttempts(0); // I-reset ang failed attempts dahil successful na ang login sequence
        user.setIsLocked(false);
        user.setLockUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // --- 5. CHECK IF NEW ACCOUNT ---
        if (user.isNewAccount()) {
            return new LoginResponse(
                    "CHANGE_PASSWORD_REQUIRED",
                    user.getId(),
                    user.getRole().getRoleName(),
                    null,
                    null,
                    user.isTotpEnabled(),
                    user.getSystemBackupEmail() != null
            );
        }

        // --- 6. JWT GENERATION ---
        Set<String> departments = user.getAllowedDepartments().stream()
                .map(Department::getName)
                .collect(Collectors.toSet());

        HashMap<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().getRoleName());
        extraClaims.put("userId", user.getId());
        extraClaims.put("depts", departments);

        String jwtToken = jwtService.generateToken(extraClaims, new CustomUserDetails(user));

        // Audit Success
        auditLogService.log(user, null, "Login Authentication", Severity.INFO,
                "Successful login via " + request.type(), ipAddress,
                null, null, "Session started");

        // --- 7. FINAL RESPONSE ---
        return new LoginResponse(
                "SUCCESS",
                user.getId(),
                user.getRole().getRoleName(),
                departments,
                jwtToken,
                user.isTotpEnabled(),
                user.getSystemBackupEmail() != null
        );
    }



    @Transactional
    public LoginResponse changePasswordNewAccount(ChangePasswordRequest request, String ipAddress) {
        User user = userRepository.findBySystemEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("No account found with this email: " + request.email()));

        if (!user.isNewAccount()) {
            auditLogService.log(user, null, "SECURITY", Severity.WARNING,
                    "Unauthorized password change attempt on existing account", ipAddress, null, null, null);
            throw new AccessDeniedException("This endpoint is strictly for new account initialization.");
        }

        // 3. Password Match Check
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match. Please re-type your new password.");
        }

        // 4. Update Account Security
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setNewAccount(false);
        user.setLastLoginAt(LocalDateTime.now());
        user.setUsername(request.username());
        userRepository.save(user);

        // 5. Prepare Response Data (Departments & Claims)
        Set<String> departments = user.getAllowedDepartments().stream()
                .map(Department::getName)
                .collect(Collectors.toSet());

        HashMap<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().getRoleName());
        extraClaims.put("userId", user.getId());
        extraClaims.put("depts", departments);

        // 6. Generate JWT Session
        String jwtToken = jwtService.generateToken(extraClaims, new CustomUserDetails(user));

        auditLogService.log(
                user,
                null,
                "Login Authentication",
                Severity.INFO,
                "New user initialization: Password set successfully",
                ipAddress,
                "Account activated",
                "isNewAccount: true -> false",
                "User ID: " + user.getId().toString()
        );

        return new LoginResponse(
                "SUCCESS",
                user.getId(),
                user.getRole().getRoleName(),
                departments,
                jwtToken,
                user.isTotpEnabled(),
                user.getSystemBackupEmail() != null
        );
    }


    @Transactional
    public void initiateBackupEmailSetup(String primaryEmail, String backupEmail,String ipAddress) {
        User user = userRepository.findBySystemEmail(primaryEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String code = mfaService.generateCode();
        user.setMfaCode(code);
        user.setMfaExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        mfaService.sendMfaEmail(backupEmail, code);

        auditLogService.log(user, null, "SECURITY", Severity.INFO,
                "BACKUP_EMAIL_VERIFICATION_SENT", ipAddress, "Code sent to: " + backupEmail, null, null);
    }

    // STEP B: Verify code and finalize backup email
    @Transactional
    public void verifyAndSaveBackupEmail(String primaryEmail, String backupEmail, String code,String ipAddress) {
        User user = userRepository.findBySystemEmail(primaryEmail).orElseThrow();

        if (user.getMfaCode() == null || !user.getMfaCode().equals(code) ||
                user.getMfaExpiry().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Invalid or expired verification code.");
        }

        // Success - Save the backup email
        user.setSystemBackupEmail(backupEmail);
        user.setMfaCode(null); // Clear code
        user.setMfaExpiry(null);
        userRepository.save(user);

        auditLogService.log(user, null, "SECURITY", Severity.INFO,
                "BACKUP_EMAIL_UPDATED", ipAddress, "Backup email set to: " + backupEmail, null, null);
    }

    @Transactional
    public void initiateForgotPassword(String email) {
        User user = userRepository.findBySystemEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account found with this email"));

        if (user.getStatus() != Status.ACTIVE) {
            throw new DisabledException("Account is inactive. Contact your administrator.");
        }

        String code = mfaService.generateCode();
        user.setMfaCode(code);
        user.setMfaExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        mfaService.sendMfaEmail(user.getSystemEmail(), code);
    }

    @Transactional
    public void verifyResetCode(VerifyCodeRequest request) {
        User user = userRepository.findBySystemEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("No account found with this email"));

        if (user.getMfaCode() == null || !user.getMfaCode().equals(request.code())) {
            throw new BadCredentialsException("The verification code is invalid. Please check and try again.");
        }

        if (user.getMfaExpiry() == null || user.getMfaExpiry().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("The verification code has expired. Please request a new one.");
        }
    }

    @Transactional
    public void completePasswordReset(ResetPasswordRequest request, String ipAddress) {
        User user = userRepository.findBySystemEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("No account found with this email"));

        if (user.getMfaCode() == null || !user.getMfaCode().equals(request.code())) {
            throw new BadCredentialsException("Unauthorized reset attempt. Verification failed.");
        }

        if (user.getMfaExpiry() == null || user.getMfaExpiry().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Session expired. Please restart the password reset process.");
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setMfaCode(null);
        user.setMfaExpiry(null);
        user.setNewAccount(false);

        userRepository.save(user);

        auditLogService.log(
                user, null, "SECURITY", Severity.WARNING, "PASSWORD_RESET_SUCCESS",
                ipAddress, "User successfully reset password", null, null
        );
    }
}
