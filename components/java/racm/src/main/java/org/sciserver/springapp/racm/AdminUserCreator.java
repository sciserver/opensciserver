package org.sciserver.springapp.racm;

import java.time.Duration;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ivoa.dm.model.InvalidTOMException;
import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.springapp.racm.config.JOBMConfig;
import org.sciserver.springapp.racm.config.RACMAdminConfig;
import org.sciserver.springapp.racm.login.LoginPortalAccess;
import org.sciserver.springapp.racm.login.LoginPortalAccess.LoginPortalHTTPException;
import org.sciserver.springapp.racm.ugm.application.UsersAndGroupsManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import edu.jhu.user.User;
import edu.jhu.user.UserVisibility;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

@Component
@ConditionalOnProperty("spring.flyway.enabled")
public class AdminUserCreator implements ApplicationRunner {
	private static final Logger logger = LogManager.getLogger();

	private final LoginPortalAccess loginPortalAccess;
	private final RACMAdminConfig racmAdminConfig;
	private final JOBMConfig jobmConfig;
	private final TransactionTemplate transactionTemplate;
	private final UsersAndGroupsManager usersAndGroupsManager;

	@PersistenceContext
	private EntityManager em;

	public AdminUserCreator(LoginPortalAccess loginPortalAccess, RACMAdminConfig racmAdminConfig,
			PlatformTransactionManager transactionManager, JOBMConfig jobmConfig,
			UsersAndGroupsManager usersAndGroupsManager) {
		this.loginPortalAccess = loginPortalAccess;
		this.racmAdminConfig = racmAdminConfig;
		this.jobmConfig = jobmConfig;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.usersAndGroupsManager = usersAndGroupsManager;
	}

	@Override
	public void run(ApplicationArguments args) {
		createUserAndFillId(
				racmAdminConfig.getUsername(), racmAdminConfig.getPassword(), racmAdminConfig.getEmail());
		createUserAndFillId(
				jobmConfig.getAdminUser(), jobmConfig.getAdminPassword(), jobmConfig.getAdminEmail());
	}

	private void createUserAndFillId(String username, String password, String email) {
		try {
			logger.trace("Checking for existance of user '{}' in login portal", username);
			String userId = getUserIdRobustly(username);
			if (userId == null) {
				logger.info("Did not find user '{}', registering with login portal", username);
				userId = loginPortalAccess
							.createUser(
									username,
									password,
									email)
						.getUserId();
			}
			final String currentUserId = userId;
			transactionTemplate.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					List<User> users = em.createQuery("SELECT u FROM User u WHERE u.username = :user", User.class)
						.setParameter("user", username)
						.getResultList();
					User user;
					if (users.isEmpty()) {
						user = registerUserInRACM(currentUserId, username, email);
					} else {
						user = users.get(0);
					}
					if (!user.getUserId().equals(currentUserId)) {
						logger.info("Setting userid of '{}' to '{}'", username, currentUserId);
						user.setUserId(currentUserId);
					}
				}
			});
		} catch (Exception e) {
			logger.error("Exception while checking or creating service accounts", e);
			throw new IllegalStateException("Exception while checking or creating service accounts", e);
		}
	}

	private String getUserIdRobustly(String username) {
		Retry retry = Retry.of("initialLoginPortal",
				RetryConfig.custom()
					.retryExceptions(LoginPortalHTTPException.class)
					.waitDuration(Duration.ofSeconds(1))
					.maxAttempts(5)
				.build()
				);
		return retry.executeSupplier(() -> loginPortalAccess.userIdForUserName(username));
	}

	private User registerUserInRACM(String keystoneId, String username, String email) {
		TransientObjectManager tom = new TransientObjectManager(em);
		User user = new User(tom);
		user.setUserId(keystoneId);
		user.setUsername(username);
		user.setContactEmail(email);
		user.setVisibility(UserVisibility.SYSTEM);

		usersAndGroupsManager.addUserToPublicGroup(user);

		try {
			tom.persist();
		} catch (InvalidTOMException e) {
			throw new IllegalStateException(e);
		}
		return user;
	}
}
