package com.example.springbootvaultdemo;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.vault.VaultException;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.core.lease.event.*;

import javax.annotation.PostConstruct;
import java.util.EventObject;
import java.util.Map;

import static org.springframework.vault.core.lease.domain.RequestedSecret.Mode.RENEW;
import static org.springframework.vault.core.lease.domain.RequestedSecret.Mode.ROTATE;

@Configuration
@Component
public class VaultConfiguration {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SecretLeaseContainer leaseContainer = null;

    @Autowired
    private HikariDataSource hikariDataSource;

    @Value("${spring.cloud.vault.database.role}")
    private String databaseRole;

    @Value("${spring.cloud.vault.config.lifecycle.min-renewal}")
    protected String minRenewal;

    @Value("${spring.cloud.vault.config.lifecycle.expiry-threshold}")
    protected String expiryThreshold;

    @Value("${spring.cloud.vault.config.lifecycle.enabled}")
    protected boolean isEnabled;

    @PostConstruct
    private void postConstruct() {
        String path = "database/creds/" + databaseRole;
        leaseContainer.addLeaseListener(event -> {
            if (!path.equals(event.getSource().getPath())) {
                return;
            }

            //logger.info(""+'\n');
            logger.info("$$$$$$$$$$$  $$$$$$$$$$$$$$$$$$");
            logger.info("Event instance of :- "+event.getClass().getName());
            logger.info("Mode :- "+event.getSource().getMode());
            logger.info("Event Path :-"+event.getSource().getPath());
            //logger.info("Is Lease Renewable :- "+event.getLease().isRenewable());
            logger.info("Lease Id :-"+event.getLease().getLeaseId());
            logger.info("Lease Duration :-"+event.getLease().getLeaseDuration().getSeconds());
            logger.info("$$$$$$$$$$$  $$$$$$$$$$$$$$$$$$ \n");

            logger.info("----------------------------------");
            logger.info("Print Lease container properties *");
            logger.info("Min renewal = "+leaseContainer.getMinRenewal());
            logger.info("Min renewal in seconds = "+leaseContainer.getMinRenewalSeconds());
            logger.info("Exp Threshold = "+leaseContainer.getExpiryThreshold());
            logger.info("Exp Threshold in seconds = "+leaseContainer.getExpiryThresholdSeconds());
            logger.info("----------------------------------\n");



            try {
                if (event instanceof SecretLeaseExpiredEvent && RENEW == event.getSource().getMode()) {
                    logger.info("** LEASE EXPIRED Cannot renew lease anymore, rotate credentials");
                    leaseContainer.requestRotatingSecret(path);
                } else if (event instanceof SecretLeaseCreatedEvent && ROTATE == event.getSource().getMode() ||
                            event instanceof SecretLeaseRotatedEvent && ROTATE == event.getSource().getMode()) {
                    logger.info("** New Lease Created or rotated **");
                    Map<String, Object> secrets = ((SecretLeaseCreatedEvent) event).getSecrets();
                    secretsProcessing(secrets);
                } else if (event instanceof AfterSecretLeaseRenewedEvent && RENEW == event.getSource().getMode()){
                    logger.info("** LEASE RENEWED **, LEASE ID: -"+event.getLease().getLeaseId());
                    logger.info("** NEW LEASE DURATION ** :- "+event.getLease().getLeaseDuration());
                    //logger.info(""+'\n');
                }
            } catch (VaultException e) {
                logger.error("Lease renewal exception from Vault, check and reconfigure TTL settings", e);
            }
        });
    }

    private void secretsProcessing(Map<String, Object> secrets){
        String username = (String) secrets.get("username");
        String password = (String) secrets.get("password");

        logger.info("Vault generated username :- " + username);
        logger.info("Dynamic generated password :- " + password);

        //logger.info("XXXXX New username = {}", username);
        hikariDataSource.getHikariConfigMXBean().setUsername(username);
        hikariDataSource.getHikariConfigMXBean().setPassword(password);
        logger.info("Soft evicting db connections...");
        hikariDataSource.getHikariPoolMXBean().softEvictConnections();
        logger.info("-------------- -----------------");
    }
}
