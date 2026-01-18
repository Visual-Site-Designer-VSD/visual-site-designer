package dev.mainul35.cms.security.authserver;

import dev.mainul35.cms.security.entity.CmsRole;
import dev.mainul35.cms.security.entity.CmsUser;
import dev.mainul35.cms.security.entity.UserStatus;
import dev.mainul35.cms.security.entity.RoleName;
import dev.mainul35.cms.security.repository.CmsRoleRepository;
import dev.mainul35.cms.security.repository.CmsUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts JWT tokens from VSD Auth Server to Spring Security authentication.
 * Maps auth server roles to CMS roles and loads/creates local CMS user.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthServerJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final CmsUserRepository cmsUserRepository;
    private final CmsRoleRepository cmsRoleRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // Extract user info from JWT claims
        String authUserId = jwt.getClaimAsString("user_id");
        String email = jwt.getSubject();
        String displayName = jwt.getClaimAsString("display_name");
        String avatarUrl = jwt.getClaimAsString("avatar_url");
        Long cmsUserId = jwt.getClaim("cms_user_id") != null ?
                Long.valueOf(jwt.getClaimAsString("cms_user_id")) : null;

        // Get roles from JWT
        Collection<String> jwtRoles = jwt.getClaimAsStringList("roles");
        if (jwtRoles == null) {
            jwtRoles = List.of();
        }

        log.debug("Processing JWT for user: email={}, authUserId={}, roles={}", email, authUserId, jwtRoles);

        // Find or create local CMS user
        CmsUser cmsUser = findOrCreateCmsUser(authUserId, cmsUserId, email, displayName, avatarUrl, jwtRoles);

        // Build authorities from CMS user roles
        Set<SimpleGrantedAuthority> authorities = cmsUser.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(RoleName.valueOf(role.getRoleName()).withRolePrefix()))
                .collect(Collectors.toSet());

        return new UsernamePasswordAuthenticationToken(cmsUser, jwt, authorities);
    }

    private CmsUser findOrCreateCmsUser(
            String authUserId,
            Long cmsUserId,
            String email,
            String displayName,
            String avatarUrl,
            Collection<String> jwtRoles
    ) {
        // Try to find by CMS user ID first (most reliable for existing users)
        if (cmsUserId != null) {
            Optional<CmsUser> existingUser = cmsUserRepository.findById(cmsUserId);
            if (existingUser.isPresent()) {
                CmsUser user = existingUser.get();
                // Link auth server ID if not already linked
                if (user.getAuthServerId() == null && authUserId != null) {
                    user.setAuthServerId(authUserId);
                    cmsUserRepository.save(user);
                }
                updateLastLogin(user);
                return user;
            }
        }

        // Try to find by auth server ID
        if (authUserId != null) {
            Optional<CmsUser> existingUser = cmsUserRepository.findByAuthServerId(authUserId);
            if (existingUser.isPresent()) {
                updateLastLogin(existingUser.get());
                return existingUser.get();
            }
        }

        // Try to find by email
        Optional<CmsUser> existingUser = cmsUserRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            CmsUser user = existingUser.get();
            // Link auth server ID
            if (authUserId != null) {
                user.setAuthServerId(authUserId);
            }
            // Update display name and avatar if provided
            if (displayName != null && !displayName.isEmpty()) {
                user.setFullName(displayName);
            }
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                user.setAvatarUrl(avatarUrl);
            }
            cmsUserRepository.save(user);
            updateLastLogin(user);
            return user;
        }

        // Create new user
        log.info("Creating new CMS user from auth server: email={}, authUserId={}", email, authUserId);

        CmsUser newUser = CmsUser.builder()
                .email(email)
                .username(email.split("@")[0] + "_" + System.currentTimeMillis()) // Unique username
                .fullName(displayName != null ? displayName : email.split("@")[0])
                .avatarUrl(avatarUrl)
                .authServerId(authUserId)
                .isActive(true)
                .isAdmin(jwtRoles.contains(RoleName.ADMIN.withRolePrefix()) || jwtRoles.contains(RoleName.ADMIN.withCmsRolePrefix()))
                .status(UserStatus.APPROVED) // Auto-approve users from auth server
                .emailVerified(true)
                .roles(mapRolesToCmsRoles(jwtRoles))
                .build();

        return cmsUserRepository.save(newUser);
    }

    private Set<CmsRole> mapRolesToCmsRoles(Collection<String> jwtRoles) {
        Set<CmsRole> cmsRoles = new HashSet<>();

        // Get default USER role
        cmsRoleRepository.findByRoleName(RoleName.USER).ifPresent(cmsRoles::add);

        // Map auth server roles to CMS roles
        for (String jwtRole : jwtRoles) {
            String roleName = jwtRole.replace("ROLE_", "");

            // Direct mapping for CMS-specific roles
            if (roleName.startsWith("CMS_")) {
                roleName = roleName.substring(4); // Remove CMS_ prefix
            }

            // Map to CMS roles using enum
            mapRoleNameToCmsRole(roleName, cmsRoles);
        }

        return cmsRoles;
    }

    private void mapRoleNameToCmsRole(String roleName, Set<CmsRole> cmsRoles) {
        // Try to match the role name to a RoleName enum value
        for (RoleName enumRole : RoleName.values()) {
            if (enumRole.name().equals(roleName) || ("CMS_" + enumRole.name()).equals(roleName)) {
                cmsRoleRepository.findByRoleName(enumRole).ifPresent(cmsRoles::add);
                return;
            }
        }
    }

    private void updateLastLogin(CmsUser user) {
        user.setLastLoginAt(Instant.now());
        cmsUserRepository.save(user);
    }
}
