package org.sciserver.springapp.racm.utils.logging;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sciserver.springapp.loginterceptor.Log;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import sciserver.logging.Logger;
import sciserver.logging.Message;
import sciserver.logging.MessageType;

/**
* Verifies that LogUtils routes messages through the spring-managed Logger bean with the expected
* application name and message type.
*
* <p>LogUtils resolves the Logger via the static ApplicationContext held by Log, so these tests
* populate that static by calling Log.setApplicationContext with a mock context rather than
* starting a Spring context. Both that static and LogUtils.jobmApplicationName are shared state,
* so they are set in setUp and must not be relied on across test methods.
*/
public class LogUtilsTests {
	private static final String LOGGER_APPLICATION_NAME = "RACM";
	private static final String JOBM_APPLICATION_NAME = "JOBM_TEST";
	private static final String DEFAULT_JOBM_APPLICATION_NAME = "JOBM";

	private Logger loggerSpy;

	@Before
	public void setUp() {
		// No sink is enabled on a bare Logger, so SendMessage does no I/O; the spy records the
		// message that LogUtils built and handed over.
		loggerSpy = spy(new Logger());
		loggerSpy.applicationName = LOGGER_APPLICATION_NAME;

		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBean(Logger.class)).thenReturn(loggerSpy);
		new Log().setApplicationContext(context);

		// fillMessageWithUserInfo reads the current request for client IP and task name.
		RequestContextHolder.setRequestAttributes(
				new ServletRequestAttributes(new MockHttpServletRequest()));

		LogUtils.setJobmApplicationName(JOBM_APPLICATION_NAME);
	}

	@After
	public void tearDown() {
		RequestContextHolder.resetRequestAttributes();
		LogUtils.setJobmApplicationName(DEFAULT_JOBM_APPLICATION_NAME);
	}

	private Message captureSentMessage() throws Exception {
		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(loggerSpy).SendMessage(captor.capture());
		return captor.getValue();
	}

	@Test
	public void jobmLogUsesConfiguredJobmApplicationName() throws Exception {
		LogUtils.buildLog().forJOBM()
				.subject("user").verb("submitted").predicate("a job").log();

		Message message = captureSentMessage();
		assertEquals(JOBM_APPLICATION_NAME, message.Application);
		assertEquals(MessageType.JOBM, message.MessageType);
	}

	@Test
	public void racmLogKeepsLoggerApplicationName() throws Exception {
		LogUtils.buildLog()
				.subject("user").verb("created").predicate("a group").log();

		Message message = captureSentMessage();
		assertEquals(LOGGER_APPLICATION_NAME, message.Application);
		assertEquals(MessageType.RACM, message.MessageType);
	}

	@Test
	public void fileServiceLogKeepsLoggerApplicationName() throws Exception {
		LogUtils.buildLog().forFileService()
				.subject("user").verb("created").predicate("a volume").log();

		Message message = captureSentMessage();
		assertEquals(LOGGER_APPLICATION_NAME, message.Application);
		assertEquals(MessageType.FILESERVICE, message.MessageType);
	}

	@Test
	public void jobmErrorLogUsesConfiguredJobmApplicationName() throws Exception {
		LogUtils.buildLog().forJOBM().logError()
				.errorText("job failed")
				.exception(new IllegalStateException("boom"))
				.log();

		Message message = captureSentMessage();
		assertEquals(JOBM_APPLICATION_NAME, message.Application);
		assertEquals(MessageType.ERROR, message.MessageType);
	}

	@Test
	public void racmErrorLogKeepsLoggerApplicationName() throws Exception {
		LogUtils.buildLog().logError()
				.errorText("request failed")
				.exception(new IllegalStateException("boom"))
				.log();

		Message message = captureSentMessage();
		assertEquals(LOGGER_APPLICATION_NAME, message.Application);
		assertEquals(MessageType.ERROR, message.MessageType);
	}

	@Test
	public void loggerFailureDoesNotPropagateToCaller() throws Exception {
		doThrow(new IllegalStateException("rabbitmq unreachable"))
				.when(loggerSpy).SendMessage(any(Message.class));

		// A logging failure must never break the calling request. wrapSendingMessage swallows the
		// exception and reports it through log4j, so this test is expected to emit a
		// "Failed to send message to sciserver logger" ERROR line on the console.
		LogUtils.buildLog()
				.subject("user").verb("created").predicate("a group").log();

		verify(loggerSpy).SendMessage(any(Message.class));
	}
}
