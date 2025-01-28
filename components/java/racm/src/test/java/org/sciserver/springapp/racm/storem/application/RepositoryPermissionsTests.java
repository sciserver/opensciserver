package org.sciserver.springapp.racm.storem.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_ROOTVOLUME_CREATE;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_ROOTVOLUME_GRANT;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_USERVOLUME_DELETE;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_USERVOLUME_GRANT;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_USERVOLUME_READ;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.A_FILESERVICE_USERVOLUME_WRITE;
import static org.sciserver.springapp.racm.storem.application.STOREMConstants.R_FILESERVICE_ADMIN;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.InvalidTOMException;
import org.ivoa.dm.model.TransientObjectManager;
import org.junit.Before;
import org.junit.Test;
import org.sciserver.racm.storem.model.RegisterNewFileServiceModel;
import org.sciserver.racm.storem.model.RegisterNewRootVolumeModel;
import org.sciserver.racm.storem.model.RegisterNewUserVolumeModel;
import org.sciserver.springapp.racm.login.InsufficientPermissionsException;
import org.sciserver.springapp.racm.storem.application.FileServiceRepository;
import org.sciserver.springapp.racm.storem.application.STOREMAccessControl;
import org.sciserver.springapp.racm.storem.application.StoremMapper;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.sciserver.springapp.racm.utils.VOURPContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.jhu.file.FileService;
import edu.jhu.file.RootVolume;
import edu.jhu.file.UserVolume;
import edu.jhu.rac.Action;
import edu.jhu.rac.Resource;
import edu.jhu.rac.ResourceContext;
import edu.jhu.user.User;

public class RepositoryPermissionsTests {
	private final TransientObjectManager tom = mock(TransientObjectManager.class);
	private final VOURPContext vourpContext = mock(VOURPContext.class);
	private final STOREMAccessControl accessControl = mock(STOREMAccessControl.class);
	private final RACMUtil racmUtil = mock(RACMUtil.class);
	private final StoremMapper mapper = mock(StoremMapper.class);
	private final User user = mock(User.class);
	private UserProfile userProfile;
	private FileServiceRepository repo;

//	@Before
	public void setupMocks() {
		when(user.getTom()).thenReturn(tom);
		when(user.getId()).thenReturn(42L);
		when(user.getUsername()).thenReturn("somename");

		doCallRealMethod().when(racmUtil).assignPrivileges(any(), any(), any());

		userProfile = new UserProfile(user);
		repo = new FileServiceRepository(mapper, accessControl, racmUtil, vourpContext);
	}

    /**
     * Method to ensure this class could in principle be run from Jenkins.<br/>
     */
	@Test
    public void noop() {}
    
    
//	@Test
	public void registerUserVolumePermissions() throws InvalidTOMException {
		RegisterNewUserVolumeModel newUserVolume = new RegisterNewUserVolumeModel(
				"name", "description", "/some/path", "somename");
		FileService fs = mock(FileService.class);

		// for check for duplicate user volume
		when(fs.getResourceContext()).thenReturn(mock(ResourceContext.class));
		when(tom.createQuery(any())).thenReturn(mockedQuery());

		RootVolume rv = mock(RootVolume.class);
		UserVolume uv = mock(UserVolume.class);
		Action writeAction = mock(Action.class);
		Action readAction = mock(Action.class);
		Action deleteAction = mock(Action.class);
		Action grantAction = mock(Action.class);
		when(uv.getResource()).thenReturn(mock(Resource.class));
		when(mapper.createUserVolume(any(), any(), any())).thenReturn(uv);
		when(racmUtil.findAction(uv.getResource(), A_FILESERVICE_USERVOLUME_WRITE)).thenReturn(writeAction);
		when(racmUtil.findAction(uv.getResource(), A_FILESERVICE_USERVOLUME_READ)).thenReturn(readAction);
		when(racmUtil.findAction(uv.getResource(), A_FILESERVICE_USERVOLUME_DELETE)).thenReturn(deleteAction);
		when(racmUtil.findAction(uv.getResource(), A_FILESERVICE_USERVOLUME_GRANT)).thenReturn(grantAction);
		when(accessControl.canCreateUserVolume(userProfile, rv)).thenReturn(true);

		repo.registerUserVolume(userProfile, fs, rv, newUserVolume);

		verify(racmUtil).assignPrivilege(writeAction, uv.getResource(), user);
		verify(racmUtil).assignPrivilege(readAction, uv.getResource(), user);
		verify(racmUtil).assignPrivilege(deleteAction, uv.getResource(), user);
		verify(tom).persist();
	}

//	@Test
	public void registerSharableUserVolumePermissions() throws VOURPException{
		RegisterNewUserVolumeModel newUserVolume = new RegisterNewUserVolumeModel(
				"name", "description", "/some/path", "somename");
		FileService fs = mock(FileService.class);

		// for check for duplicate user volume
		when(fs.getResourceContext()).thenReturn(mock(ResourceContext.class));
		when(tom.createQuery(any())).thenReturn(mockedQuery());

		RootVolume rv = mock(RootVolume.class);
		UserVolume uv = mock(UserVolume.class);
		Action writeAction = mock(Action.class);
		Action readAction = mock(Action.class);
		Action deleteAction = mock(Action.class);
		Action grantAction = mock(Action.class);
		when(rv.getContainsSharedVolumes()).thenReturn(true);
		when(uv.getResource()).thenReturn(mock(Resource.class));
		when(mapper.createUserVolume(any(), any(), any())).thenReturn(uv);
		when(racmUtil.findAction(uv.getResource(), A_FILESERVICE_USERVOLUME_WRITE)).thenReturn(writeAction);
		when(racmUtil.findAction(uv.getResource(), A_FILESERVICE_USERVOLUME_READ)).thenReturn(readAction);
		when(racmUtil.findAction(uv.getResource(), A_FILESERVICE_USERVOLUME_DELETE)).thenReturn(deleteAction);
		when(racmUtil.findAction(uv.getResource(), A_FILESERVICE_USERVOLUME_GRANT)).thenReturn(grantAction);
		when(accessControl.canCreateUserVolume(userProfile, rv)).thenReturn(true);

		repo.registerUserVolume(userProfile, fs, rv, newUserVolume);

		verify(racmUtil).assignPrivilege(writeAction, uv.getResource(), user);
		verify(racmUtil).assignPrivilege(readAction, uv.getResource(), user);
		verify(racmUtil).assignPrivilege(deleteAction, uv.getResource(), user);
		verify(racmUtil).assignPrivilege(grantAction, uv.getResource(), user);
		verify(tom).persist();
	}

//	@Test(expected=InsufficientPermissionsException.class)
	public void registerUseVolumeAsDifferentUser() throws InvalidTOMException{
		RegisterNewUserVolumeModel newUserVolume = new RegisterNewUserVolumeModel(
				"name", "description", "/some/path", "some_other_user_name");
		FileService fs = mock(FileService.class);
		RootVolume rv = mock(RootVolume.class);

		repo.registerUserVolume(userProfile, fs, rv, newUserVolume);
	}

//	@Test
	public void registerRootVolumePermissions() throws InvalidTOMException {
		RegisterNewRootVolumeModel newRootVolume = new RegisterNewRootVolumeModel(
				"root volume",
				"A set of users' data",
				"/path/on/filesystem",
				true);
		FileService fs = mock(FileService.class);
		RootVolume rv = mock(RootVolume.class);
		Action createAction = mock(Action.class);
		Action grantAction = mock(Action.class);
		when(rv.getResource()).thenReturn(mock(Resource.class));
		when(mapper.createRootVolume(any(), any())).thenReturn(rv);
		when(racmUtil.findAction(rv.getResource(), A_FILESERVICE_ROOTVOLUME_CREATE)).thenReturn(createAction);
		when(racmUtil.findAction(rv.getResource(), A_FILESERVICE_ROOTVOLUME_GRANT)).thenReturn(grantAction);
		when(accessControl.canEditFileService(userProfile, fs)).thenReturn(true);

		repo.registerRootVolume(userProfile, fs, newRootVolume);

		verify(racmUtil).assignPrivilege(createAction, rv.getResource(), user);
		verify(racmUtil).assignPrivilege(grantAction, rv.getResource(), user);
		verify(tom).persist();
	}

//	@Test
	public void registerFileServicesPermissions() throws IOException, InvalidTOMException {
		URL filename = getClass().getClassLoader().getResource("storem_json/register_fileservice.json");
		RegisterNewFileServiceModel newFileService = (new ObjectMapper()).readValue(filename, RegisterNewFileServiceModel.class);
		Resource rootResource = mock(Resource.class);
		when(racmUtil.addRootContext(any())).thenReturn(rootResource);
		when(racmUtil.newResource(any())).thenAnswer(x -> mock(Resource.class));
		when(accessControl.canRegisterFileService(userProfile)).thenReturn(true);

		StoremMapper realMapper = new StoremMapper(racmUtil);
		FileService fs = realMapper.createFileService(newFileService, tom);
		when(mapper.createFileService(newFileService, tom)).thenReturn(fs);

		repo.registerFileService(userProfile, newFileService);

		verify(racmUtil).assignRole(
				R_FILESERVICE_ADMIN,
				rootResource,
				user);

		verify(racmUtil, times(2)).assignPrivileges(eq(user), any(),
				eq(A_FILESERVICE_ROOTVOLUME_CREATE),
				eq(A_FILESERVICE_ROOTVOLUME_GRANT));

		verify(tom).persist();
	}

//	@Test
	public void getUserVolumePermissions() {
		repo.getUserVolumeActions(userProfile, "myResourceContextUUID", "SomeData volume name", "username", "~username1");

		verify(accessControl).getAllowedUserVolumeActions(userProfile, "myResourceContextUUID", "SomeData volume name/username/~username1");
	}

//	@Test(expected=InsufficientPermissionsException.class)
	public void registerFileServicesNotAllowed() {
		when(accessControl.canRegisterFileService(userProfile)).thenReturn(false);

		repo.registerFileService(userProfile, null);
	}

	private Query mockedQuery() {
		return new Query() {
			@SuppressWarnings("rawtypes")
			@Override
			public List getResultList() {
				return null;
			}

			@Override
			public Object getSingleResult() {
				return null;
			}

			@Override
			public int executeUpdate() {
				return 0;
			}

			@Override
			public Query setMaxResults(int maxResult) {
				return null;
			}

			@Override
			public int getMaxResults() {
				return 0;
			}

			@Override
			public Query setFirstResult(int startPosition) {
				return this;
			}

			@Override
			public int getFirstResult() {
				return 0;
			}

			@Override
			public Query setHint(String hintName, Object value) {
				return this;
			}

			@Override
			public Map<String, Object> getHints() {
				return null;
			}

			@Override
			public <T> Query setParameter(Parameter<T> param, T value) {
				return this;
			}

			@Override
			public Query setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
				return this;
			}

			@Override
			public Query setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
				return this;
			}

			@Override
			public Query setParameter(String name, Object value) {
				return this;
			}

			@Override
			public Query setParameter(String name, Calendar value, TemporalType temporalType) {
				return this;
			}

			@Override
			public Query setParameter(String name, Date value, TemporalType temporalType) {
				return this;
			}

			@Override
			public Query setParameter(int position, Object value) {
				return this;
			}

			@Override
			public Query setParameter(int position, Calendar value, TemporalType temporalType) {
				return this;
			}

			@Override
			public Query setParameter(int position, Date value, TemporalType temporalType) {
				return this;
			}

			@Override
			public Set<Parameter<?>> getParameters() {
				return null;
			}

			@Override
			public Parameter<?> getParameter(String name) {
				return null;
			}

			@Override
			public <T> Parameter<T> getParameter(String name, Class<T> type) {
				return null;
			}

			@Override
			public Parameter<?> getParameter(int position) {
				return null;
			}

			@Override
			public <T> Parameter<T> getParameter(int position, Class<T> type) {
				return null;
			}

			@Override
			public boolean isBound(Parameter<?> param) {
				return false;
			}

			@Override
			public <T> T getParameterValue(Parameter<T> param) {
				return null;
			}

			@Override
			public Object getParameterValue(String name) {
				return null;
			}

			@Override
			public Object getParameterValue(int position) {
				return null;
			}

			@Override
			public Query setFlushMode(FlushModeType flushMode) {
				return null;
			}

			@Override
			public FlushModeType getFlushMode() {
				return null;
			}

			@Override
			public Query setLockMode(LockModeType lockMode) {
				return null;
			}

			@Override
			public LockModeType getLockMode() {
				return null;
			}

			@Override
			public <T> T unwrap(Class<T> cls) {
				return null;
			}
		};
	}
}
