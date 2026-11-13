package com.scasp.enterprise;

import com.scasp.persistence.JdbcRegistrationRepository;
import com.scasp.repository.RegistrationOutcome;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Stateless, container-managed business component (Stage 4).
 * <ul>
 *   <li>Dependency injection: the pooled MySQL DataSource ({@code jdbc/scasp}, configured in Payara) and the
 *       singleton metrics bean are injected by the container.</li>
 *   <li>Container-managed transaction (REQUIRED): the multi-step registration (lock course, check, insert
 *       registration or waiting-list row, insert audit row) runs in one JTA transaction; the JDBC repository
 *       is created in container-managed mode so it never calls commit/rollback itself.</li>
 *   <li>Business-rule failures are thrown as {@link RegistrationRuleException} (rollback=true).</li>
 * </ul>
 * Entity beans are not used: they were removed from Jakarta EE; persistence is the JDBC repository layer
 * (Stage 5) - JPA would be the standard replacement in a larger system.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class RegistrationBean implements RegistrationService {
    private static final Logger LOG = Logger.getLogger(RegistrationBean.class.getName());

    @Resource(lookup = "jdbc/scasp") private DataSource dataSource;
    @EJB private PlatformMetricsBean metrics;
    private JdbcRegistrationRepository repository;

    @PostConstruct void initialise() {
        repository = new JdbcRegistrationRepository(dataSource, true);
        LOG.info("SCASP RegistrationBean instance created");
    }
    @PreDestroy void destroy() { LOG.info("SCASP RegistrationBean instance removed"); }

    @Override public String register(String actor, String studentId, String courseCode) {
        try {
            RegistrationOutcome outcome = repository.register(actor, studentId, courseCode);
            metrics.recordRegistration();
            return outcome.status();
        } catch (IllegalArgumentException e) {
            throw new RegistrationRuleException(e.getMessage(), true);
        } catch (IllegalStateException e) {
            throw new RegistrationRuleException(e.getMessage(), false);
        }
    }

    @Override public Optional<String> withdraw(String actor, String studentId, String courseCode) {
        try {
            Optional<String> promoted = repository.withdraw(actor, studentId, courseCode);
            metrics.recordWithdrawal();
            return promoted;
        } catch (IllegalArgumentException e) {
            throw new RegistrationRuleException(e.getMessage(), true);
        } catch (IllegalStateException e) {
            throw new RegistrationRuleException(e.getMessage(), false);
        }
    }
}
