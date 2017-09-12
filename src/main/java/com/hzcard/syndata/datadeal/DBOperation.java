package com.hzcard.syndata.datadeal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.lookup.MapDataSourceLookup;
import org.springframework.stereotype.Service;

@Service
public class DBOperation {

	private final static Logger logger = LoggerFactory.getLogger(DBOperation.class);
	@Autowired
	private MapDataSourceLookup sources;

	@Deprecated
	public void delete(String schema, String tableName, Map<String, Object> map, String keyword) {
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		try {
			conn = sources.getDataSource("target" + schema).getConnection();
			StringBuilder sql = new StringBuilder("delete from " + schema + "." + tableName);
			sql.append(" where ");
			if (keyword == null || keyword.trim().length() == 0)
				sql.append("  id = '" + map.get("id") + "' ");
			else {
				for (String key : keyword.split(","))
					sql.append(key + "='" + map.get(key) + "' and");
				sql.delete(sql.length() - 3, sql.length());
			}

			preparedStatement = conn.prepareStatement(sql.toString());
			int result = preparedStatement.executeUpdate();
			if (result == -1) {
				logger.error("数据库表" + tableName + "删除失败！");
			} else {
				logger.error("数据库表" + tableName + "删除成功！");
			}
		} catch (SQLException e) {
			logger.error(e.getMessage());
		} finally {
			try {
				conn.close();
				preparedStatement.close();
			} catch (SQLException e) {
				logger.error(e.getMessage());
			}
		}
	}

	/**
	 * 
	 * @param schema
	 * @param tableName
	 * @param maps //LinkedHashMap最佳
	 * @param keyword
	 * @throws Exception
	 */
	public void delete(String schema, String tableName, List</*LinkedHash*/Map<String, Object>> maps, String keyword)
			throws Exception {
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		try {
			String[] keywords = null;
			if (keyword != null) {
				keywords = keyword.split(",");
			}
			conn = sources.getDataSource("target" + schema).getConnection();
			StringBuilder sql = new StringBuilder("delete from " + schema + "." + tableName);
			sql.append(" where ");

			if (keywords == null || keywords.length == 0 || (keywords.length == 1 && "id".equals(keywords[0]) )) {
				sql.append("  id = ? ");
			} else {
				for (String key : keywords) {
					sql.append(" " + key + "=? and");
				}
				sql.delete(sql.length() - 3, sql.length());
			}
			if (logger.isInfoEnabled())
				logger.info("delete sql is: {}", sql.toString());
			preparedStatement = conn.prepareStatement(sql.toString());

			for (Map<String, Object> objMap : maps) {
				int i = 1;
				if (keywords == null || keywords.length == 0 || (keywords.length == 1 && "id".equals(keywords[0]) )) {
					preparedStatement.setObject(i, objMap.get("id"));
					i += 1;
				} else {
					for (String key : keywords) {
						preparedStatement.setObject(i, objMap.get(key));
						i += 1;
					}
				}
				preparedStatement.addBatch();
			}

			preparedStatement.executeBatch();

		} catch (SQLException e) {
			logger.error(e.getMessage() + " error table is:" + tableName + ",error data key is:"
					+ maps.get(0).getOrDefault("id", "notId"), e);
			throw e;
		} finally {
			try {
				preparedStatement.close();
				conn.close();
			} catch (SQLException e) {
				logger.error(e.getMessage());
			}
		}

	}

	@Deprecated
	public boolean save(String schema, String tableName, Map<String, Object> map, String keyword) throws Exception {
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		try {
			conn = sources.getDataSource("target" + schema).getConnection();

			StringBuilder strSql = new StringBuilder("insert into " + schema + "." + tableName + "(");
			LinkedHashMap<String, Object> newMap = new LinkedHashMap<String, Object>(map);
			for (String key : newMap.keySet()) {
				strSql.append(key + ",");
			}
			strSql.deleteCharAt(strSql.length() - 1);
			strSql.append(") values(");
			for (int i = 0; i < newMap.size(); i++) {
				strSql.append("?,");
			}
			strSql.deleteCharAt(strSql.length() - 1);
			strSql.append(")");
			if (logger.isInfoEnabled())
				logger.info("insert sql is:" + strSql.toString());
			preparedStatement = conn.prepareStatement(strSql.toString());
			int i = 1;
			for (Object obj : newMap.values()) {
				preparedStatement.setObject(i, obj);
				i++;
			}

			preparedStatement.executeUpdate();
			return true;
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
			if (e.getMessage().indexOf("Duplicate entry") >= 0)
				return update(schema, tableName, map, keyword, true);
			throw e;
		} finally {
			preparedStatement.close();
			conn.close();
		}
	}

	/**
	 * 
	 * @param schema
	 * @param tableName
	 * @param maps //LinkedHashMap最佳
	 * @param keyword
	 * @return
	 * @throws Exception
	 */
	public boolean save(String schema, String tableName, List</*LinkedHash*/Map<String, Object>> maps, String keyword)
			throws Exception {
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		try {
			conn = sources.getDataSource("target" + schema).getConnection();
			final StringBuilder strSql = new StringBuilder("insert into " + schema + "." + tableName + "(");

			Map<String, Object> firstRow = maps.iterator().next();
			
			firstRow.keySet().forEach(key -> strSql.append(key).append(","));//按keySet的顺序拼接列名
			strSql.deleteCharAt(strSql.length() - 1);
			strSql.append(") values(");

			for (int i = 0; i < firstRow.size(); i++) {
				strSql.append("?,");
			}
			strSql.deleteCharAt(strSql.length() - 1);
			strSql.append(")");

			if (logger.isInfoEnabled())
				logger.info("insert sql is: {}", strSql.toString());
			
			preparedStatement = conn.prepareStatement(strSql.toString());

			for (Map<String, Object> objMap : maps) {
				int i = 1;
				Set<String> keyset = objMap.keySet();//必须按keySet的顺序setObject
				for (String key : keyset) {
					preparedStatement.setObject(i, objMap.get(key));
					i += 1;
				}
				preparedStatement.addBatch();
			}

			preparedStatement.executeBatch();

			return true;
		} catch (SQLException e) {
//			logger.error(e.getMessage(), e);
			if (e.getMessage().indexOf("Duplicate entry") >= 0)
				logger.error(e.getMessage() + " error table is:" + tableName + ",error data key is:"
						+ maps.get(0).getOrDefault("id", "notId"), e);
			throw e;
		} finally {
			if (preparedStatement != null)
				preparedStatement.close();
			if (conn != null)
				conn.close();
		}
	}

	@Deprecated
	public boolean update(String schema, String tableName, Map<String, Object> map, String keyword) throws Exception {
		return update(schema, tableName, map, keyword, false);
	}

	@Deprecated
	public boolean update(String schema, String tableName, Map<String, Object> map, String keyword, boolean fromSaveOps)
			throws Exception {
		Connection conn = null;
		PreparedStatement preparedStatement = null;

		try {
			conn = sources.getDataSource("target" + schema).getConnection();
			StringBuilder strSql = new StringBuilder("update " + schema + "." + tableName + " set ");

			LinkedHashMap<String, Object> newMap = new LinkedHashMap<String, Object>(map);

			for (String key : newMap.keySet()) {
				strSql.append(key + " = ?,");
			}
			strSql.deleteCharAt(strSql.toString().trim().length() - 1);
			strSql.append(" where ");
			if (keyword == null || keyword.trim().length() == 0)
				strSql.append(" id = '" + newMap.get("id") + "'");
			else {
				for (String key : keyword.split(","))
					strSql.append(key + "='" + newMap.get(key) + "' and");
				strSql.delete(strSql.length() - 3, strSql.length());
			}

			preparedStatement = conn.prepareStatement(strSql.toString());

			int i = 1;
			for (Object obj : newMap.values()) {
				preparedStatement.setObject(i, obj);
				i++;
			}
			int n = preparedStatement.executeUpdate();
			if (n == 0) {
				if (fromSaveOps) {
					return true;
				}
				return save(schema, tableName, map, keyword);
			} else
				return true;

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw e;
		} finally {
			if (preparedStatement != null)
				preparedStatement.close();
			if (conn != null)
				conn.close();
		}
	}

	/**
	 * 
	 * @param schema
	 * @param tableName
	 * @param maps //LinkedHashMap最佳
	 * @param keyword
	 * @return
	 * @throws Exception
	 */
	public boolean update(String schema, String tableName, List</*LinkedHash*/Map<String, Object>> maps, String keyword)
			throws Exception {
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		try {
			String[] keywordx = null;
			if (keyword != null) {
				keywordx = keyword.split(",");
			}
			final String[] keywords = keywordx;
			
			conn = sources.getDataSource("target" + schema).getConnection();
			StringBuilder strSql = new StringBuilder("update " + schema + "." + tableName + " set ");

			Map<String, Object> firstRow = maps.iterator().next();
			
			//按keySet的顺序拼接列名
			firstRow.keySet().forEach(key -> {
				if (!ArrayUtils.contains(keywords, key))
					strSql.append(key + " = ?,");
			});

			strSql.deleteCharAt(strSql.toString().trim().length() - 1);
			strSql.append(" where ");

			for (String key : keywords) {
				strSql.append(" " + key + "=? and");
			}
			strSql.delete(strSql.length() - 3, strSql.length());
			if (logger.isInfoEnabled())
				logger.info("update sql is: {}", strSql.toString());
			preparedStatement = conn.prepareStatement(strSql.toString());

			for (Map<String, Object> objMap : maps) {
				int i = 1;
				Set<String> keyset = objMap.keySet();//必须按keySet的顺序setObject
				for (String key : keyset) {
					preparedStatement.setObject(i, objMap.get(key));
					i += 1;
				}

				for (String key : keywords) {
					preparedStatement.setObject(i, objMap.get(key));
					i += 1;
				}

				preparedStatement.addBatch();
			}

			preparedStatement.executeBatch();
			return true;
		} catch (Exception e) {
			logger.error(e.getMessage() + " error table is:" + tableName + ",error data key is:"
					+ (maps.get(0).getOrDefault("id", "notId")), e);
			throw e;
		} finally {
			if (preparedStatement != null)
				preparedStatement.close();
			if (conn != null)
				conn.close();
		}
	}

}
