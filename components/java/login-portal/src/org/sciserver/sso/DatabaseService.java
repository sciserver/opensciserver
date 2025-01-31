package org.sciserver.sso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sciserver.sso.model.ApprovalRequest;
import org.sciserver.sso.model.ApprovalStatus;
import org.sciserver.sso.model.Rule;
import org.sciserver.sso.model.RuleType;
import org.sciserver.sso.model.UserMapping;
import org.springframework.stereotype.Service;

@Service
public class DatabaseService {
	
	public UserMapping getUserMapping(String externalUserId) throws SQLException {
		UserMapping result = null;
		
		try (Connection conn = AppConfig.getInstance().getDataSource().getConnection()) {
			String sql = "SELECT keystone_user_id, keystone_trust_id, external_username FROM user_mapping WHERE external_user_id = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, externalUserId);
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						result = new UserMapping();
						result.setExternalUserId(externalUserId);
						result.setKeystoneUserId(rs.getString("keystone_user_id"));
						result.setKeystoneTrustId(rs.getString("keystone_trust_id"));
						result.setExternalUsername(rs.getString("external_username"));
					}
				}
			}
		}
		
		return result;
	}
	
	public void addUserMapping(UserMapping mapping) throws SQLException {
		try (Connection conn = AppConfig.getInstance().getDataSource().getConnection()) {
			String sql = "INSERT user_mapping (external_user_id, keystone_user_id, keystone_trust_id, external_username) VALUES(?, ?, ?, ?)";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, mapping.getExternalUserId());
				stmt.setString(2, mapping.getKeystoneUserId());
				stmt.setString(3, mapping.getKeystoneTrustId());
				stmt.setString(4, mapping.getExternalUsername());
				stmt.executeUpdate();
			}
		}
	}

	public Map<Long, String> listMappings(String keystoneUserId) throws SQLException {
		Map<Long, String> result = new HashMap<Long, String>();
		try (Connection conn = AppConfig.getInstance().getDataSource().getConnection()) {
			String sql = "SELECT row_id, external_username FROM user_mapping WHERE keystone_user_id = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, keystoneUserId);
				try (ResultSet rs = stmt.executeQuery()) {
					while (rs.next()) {
						result.put(rs.getLong("row_id"), rs.getString("external_username"));
					}
				}
			}
		}
		return result;
	}

	public void unlinkAccount(long id) throws SQLException {
		try (Connection conn = AppConfig.getInstance().getDataSource().getConnection()) {
			String sql = "DELETE FROM user_mapping WHERE row_id = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, id);
				stmt.executeUpdate();
			}
		}
	}
	
	public List<Rule> getRules() throws SQLException {
		List<Rule> result = new ArrayList<Rule>();
		try (Connection conn = AppConfig.getInstance().getDataSource().getConnection()) {
			try (Statement stmt = conn.createStatement()) {
				try (ResultSet rs = stmt.executeQuery("SELECT `id`, `type`, `reg_ex` FROM `rules` ORDER BY `id` ASC")) {
					while (rs.next()) {
						int type = rs.getInt("type");
						String regEx = rs.getString("reg_ex");
						result.add(new Rule(new RuleType(type), regEx));
					}
				}
			}
		}
		return result;
	}
	
	public void setRules(List<Rule> rules) throws SQLException {
		String sql = "INSERT INTO `rules` (`id`, `type`, `reg_ex`) VALUES (?, ?, ?)";
		try (Connection conn = AppConfig.getInstance().getDataSource().getConnection()) {
			conn.setAutoCommit(false);
			try (Statement stmt = conn.createStatement()) {
				stmt.executeUpdate("TRUNCATE `rules`");
			}
			
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				for (int i=0; i<rules.size(); i++) 
				{
					Rule r = rules.get(i);
					stmt.clearParameters();
					stmt.setInt(1, i);
					stmt.setInt(2, r.getType().getIntType());
					stmt.setString(3, r.getRegEx());
					stmt.addBatch();
				}
				stmt.executeBatch();
			}
			conn.commit();
		}
	}
	
	public List<ApprovalRequest> getApprovalRequests() throws SQLException {
		List<ApprovalRequest> result = new ArrayList<ApprovalRequest>();
		try (Connection conn = AppConfig.getInstance().getDataSource().getConnection()) {
			try (Statement stmt = conn.createStatement()) {
				String sql = "SELECT `row_id`, `created_at`, `keystone_user_id`, `name`, `email`, `ip_address`, `extra`, `status` "
						+ "FROM `approval_request` WHERE `status` = 0";
				
				try (ResultSet rs = stmt.executeQuery(sql)) {
					while (rs.next()) {
						Date createdAt = rs.getTimestamp("created_at");
						String keystoneUserId = rs.getString("keystone_user_id");
						String name = rs.getString("name");
						String email = rs.getString("email");
						String ipAddress = rs.getString("ip_address");
						String extra = rs.getString("extra");
						int status = rs.getInt("status");
						result.add(new ApprovalRequest(createdAt, keystoneUserId, name, email, ipAddress, extra, new ApprovalStatus(status)));
					}
				}
			}
		}
		return result;
	}
	
	public ApprovalRequest getApprovalRequest(String userId) throws SQLException {
		ApprovalRequest result = null;
		try (Connection conn = AppConfig.getInstance().getDataSource().getConnection()) {
			String sql = "SELECT `row_id`, `created_at`, `keystone_user_id`, `name`, `email`, `ip_address`, `extra`, `status` "
					+ "FROM `approval_request` WHERE `keystone_user_id` = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, userId);
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						Date createdAt = rs.getTimestamp("created_at");
						String keystoneUserId = rs.getString("keystone_user_id");
						String name = rs.getString("name");
						String email = rs.getString("email");
						String ipAddress = rs.getString("ip_address");
						String extra = rs.getString("extra");
						int status = rs.getInt("status");
						result = new ApprovalRequest(createdAt, keystoneUserId, name, email, ipAddress, extra, new ApprovalStatus(status));
					}
				}
			}
		}
		return result;
	}
	
	public void addApprovalRequest(ApprovalRequest request) throws SQLException {
		String sql = "INSERT INTO `approval_request` (`created_at`, `keystone_user_id`, `name`, `email`, `ip_address`, `status`) "
				+ "VALUES(NOW(), ?, ?, ?, ?, ?)";
		try (Connection conn = AppConfig.getInstance().getDataSource().getConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, request.getKeystoneUserId());
				stmt.setString(2, request.getName());
				stmt.setString(3, request.getEmail());
				stmt.setString(4, request.getIpAddress());
				stmt.setInt(5, request.getStatus().getIntValue());
				stmt.executeUpdate();
			}
		}
	}

	public void setApprovalRequestExtra(String keystoneUserId, String extra) throws SQLException {
		String sql = "UPDATE `approval_request` SET `extra` = ? WHERE `keystone_user_id` = ?";
		try (Connection conn = AppConfig.getInstance().getDataSource().getConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, extra);
				stmt.setString(2, keystoneUserId);
				stmt.executeUpdate();
			}
		}
	}
	
	public void setApprovalRequestStatus(String keystoneUserId, ApprovalStatus status) throws SQLException {
		String sql = "UPDATE `approval_request` SET `status` = ? WHERE `keystone_user_id` = ?";
		try (Connection conn = AppConfig.getInstance().getDataSource().getConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setInt(1, status.getIntValue());
				stmt.setString(2, keystoneUserId);
				stmt.executeUpdate();
			}
		}
	}
	
	
}
