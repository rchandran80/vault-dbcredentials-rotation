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
import org.springframework.vault.core.lease.SecretLeaseEventPublisher;
import org.springframework.vault.core.lease.event.*;

import javax.annotation.PostConstruct;
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
            logger.info("Is Lease Renewable :- "+event.getLease().isRenewable());
            logger.info("$$$$$$$$$$$  $$$$$$$$$$$$$$$$$$ \n");

            try {
                if (event instanceof SecretLeaseExpiredEvent && RENEW == event.getSource().getMode()) {
                    logger.info("**SecretLeaseExpiredEvent** Reached Max Lease Ttl for Lease: " +event.getLease().getLeaseId()+ " , Cannot renew lease anymore, rotate credentials");
                    logger.info("** Rotating credentials..\n");
                    leaseContainer.requestRotatingSecret(path);
                    leaseContainer.removeLeaseErrorListener(SecretLeaseEventPublisher.LoggingErrorListener.INSTANCE);
                } else if (event instanceof SecretLeaseCreatedEvent && ROTATE == event.getSource().getMode()) { //NOTE: Condition also evaluates for SecretLeaseRotatedEvent, subclass of SecretLeaseCreatedEvent, so no need for explicit check.
                    logger.info("**SecretLeaseCreatedEvent or SecretLeaseRotatedEvent ** New Lease Created ** " + event.getLease().getLeaseId()+ " with Lease duration: "+event.getLease().getLeaseDuration().getSeconds()+"\n");
                    Map<String, Object> secrets = ((SecretLeaseCreatedEvent) event).getSecrets();
                    secretsProcessing(secrets);
                    try{
                        logger.info("** Revoking lease now..\n");
                        leaseContainer.destroy(); //Triggers events for lease revocation
                    }catch (Exception e) {
                        logger.error("Error occured while revoking lease ** "+e.getMessage());
                    }
                } else if (event instanceof AfterSecretLeaseRenewedEvent && RENEW == event.getSource().getMode() ||
                            event instanceof AfterSecretLeaseRenewedEvent && ROTATE == event.getSource().getMode()){
                    logger.info("**AfterSecretLeaseRenewedEvent** Lease renewed for Lease ID: -"+event.getLease().getLeaseId()+ " with Lease duration: "+event.getLease().getLeaseDuration().getSeconds()+"\n");
                } else if (event instanceof BeforeSecretLeaseRevocationEvent) {
                    logger.info("%&%&^&%& BeforeSecretLeaseRevocationEvent *^*^*^* ON Lease ID "+event.getLease().getLeaseId()+"\n");
                } else if (event instanceof AfterSecretLeaseRevocationEvent) {
                    logger.info("*&*&*&*&AfterSecretLeaseRevocationEvent generated*&*^^&^& ON Lease ID "+event.getLease().getLeaseId()+"\n");
                } else if (event instanceof SecretLeaseErrorEvent) {
                    logger.info("######SecretLeaseErrorEvent generated###### Error occurred during secret retrieval or lease management!!! \n");
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