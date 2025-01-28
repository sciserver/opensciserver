package org.sciserver.springapp.racm.storem.application;

public final class STOREMConstants {
	private STOREMConstants() {}
	public static final String A_FILESERVICE_REGISTER = "registerFileService";

	public static final String CC_FILESERVICE_NAME = "FileService";
	public static final String A_FILESERVICE_EDIT = "edit";
	public static final String A_FILESERVICE_DEFINE_DATAVOLUME = "registerDataVolume";
	public static final String A_FILESERVICE_GRANT = "grant";

	public static final String R_FILESERVICE_ADMIN = "fs_admin";

	public static final String RT_FILESERVICE_ROOTVOLUME = "FileService.RootVolume";
	public static final String A_FILESERVICE_ROOTVOLUME_CREATE = "create";
	public static final String A_FILESERVICE_ROOTVOLUME_GRANT = "grant";

	public static final String RT_FILESERVICE_USERVOLUME = "FileService.UserVolume";
	public static final String A_FILESERVICE_USERVOLUME_READ = "read";
	public static final String A_FILESERVICE_USERVOLUME_WRITE = "write";
	public static final String A_FILESERVICE_USERVOLUME_DELETE = "delete";
	public static final String A_FILESERVICE_USERVOLUME_GRANT = "grant";

	public static final String RT_FILESERVICE_DATAVOLUME = "FileService.DataVolume";
	public static final String A_FILESERVICE_DATAVOLUME_EDIT = "edit";
	public static final String A_FILESERVICE_DATAVOLUME_READ = "read";
	public static final String A_FILESERVICE_DATAVOLUME_WRITE = "write";
	public static final String A_FILESERVICE_DATAVOLUME_DELETE = "delete";
	public static final String A_FILESERVICE_DATAVOLUME_GRANT = "grant";
}
