package dev.mainul35.cms.security.authserver;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthServerSettingsRepository extends JpaRepository<AuthServerSettings, Long> {
}
