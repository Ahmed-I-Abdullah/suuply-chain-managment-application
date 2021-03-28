import java.io. * ;
import java.util. * ;
import java.sql. * ;
import java.util.regex.Pattern;

public class DatabaseConnection {
	public final String DBURL;
	public final String USERNAME;
	public final String PASSWORD;
	private Connection databaseConnection;
	private ResultSet queryResults;
	private Statement myStatment;
	private PreparedStatement myPreparedStatment;
	private ArrayList < String > availableTables = new ArrayList < String > ();

	/**
     * Sets the url, username and password of registeration class.
     * @param DBURL url of database.
     * @param USERNAME connesction username.
     * @param PASSWORD connection passowrd.
     */
	DatabaseConnection(String DBURL, String USERNAME, String PASSWORD) {
		this.DBURL = DBURL;
		this.USERNAME = USERNAME;
		this.PASSWORD = PASSWORD;
		initializeConnection();
		populateAvailableTables();
	}

	/**
     * gets the stored database url
     * @return A String containing the url.
     */
	String getDburl() {
		return this.DBURL;
	}

	/**
     * gets the stored connection username
     * @return A String containing the username.
     */
	String getUsername() {
		return this.USERNAME;
	}

	/**
     * gets the stored connection password
     * @return A String containing the password.
     */
	String getPassword() {
		return this.PASSWORD;
	}

	/**
    creates a new connection with the given data base url
    */
	private void initializeConnection() {
		try {
			databaseConnection = DriverManager.getConnection(this.DBURL, this.USERNAME, this.PASSWORD);
		} catch(SQLException e) {
			e.printStackTrace();
		}
	}

	private void populateAvailableTables() {
		try {
			myStatment = databaseConnection.createStatement();
			queryResults = myStatment.executeQuery("Show tables");

			while (queryResults.next()) {
				availableTables.add(queryResults.getString(1));
			}
			myStatment.close();
		} catch(SQLException ex) {
			ex.printStackTrace();
		}
	}

	private String getItemTable(String furnitureItem) {
		String itemTable = "";

		for (int i = 0; i < availableTables.size(); i++) {
			if (Pattern.compile(Pattern.quote(availableTables.get(i)), Pattern.CASE_INSENSITIVE).matcher(furnitureItem).find()) {
				itemTable = availableTables.get(i);
				break;
			}
		}

		return itemTable.trim();
	}

	private String getItemType(String furnitureItem, String itemTable) {
		int stopIndex = furnitureItem.toLowerCase().indexOf(itemTable.toLowerCase());
		String itemType = furnitureItem.substring(0, stopIndex).trim();

		return itemType.trim();
	}

	public List < Map < String,
	Object >> getItemRecords(String furnitureItem) {
		String itemTable = getItemTable(furnitureItem);

		if (itemTable == "") {
			return null;
		}

		String itemType = getItemType(furnitureItem, itemTable);

		try {
			myStatment = databaseConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			queryResults = myStatment.executeQuery("SELECT * FROM " + itemTable + " WHERE Type = '" + itemType + "'");

			if (queryResults.next() == false) {
				myStatment.close();
				return null;
			} else {
				queryResults.previous();
				List < Map < String,
				Object >> queryResultList = new ArrayList < Map < String,
				Object >> ();
				Map < String,
				Object > entry = null;

				ResultSetMetaData metaData = queryResults.getMetaData();
				Integer columnCount = metaData.getColumnCount();

				while (queryResults.next()) {
					entry = new HashMap < String,
					Object > ();
					for (int i = 1; i <= columnCount.intValue(); i++) {
						entry.put(metaData.getColumnName(i), queryResults.getObject(i));
						//queryResultList.add(entry);
					}
					queryResultList.add(entry);
				}
				myStatment.close();
				return queryResultList;
			}
		} catch(SQLException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	public List < Map < String,
	Object >> getPossibleManufacturer(String furnitureItem) {
		String itemTable = getItemTable(furnitureItem);

		if (itemTable == "") {
			return null;
		}

		String itemType = getItemType(furnitureItem, itemTable);

		HashSet < String > possibleManufacturers = new HashSet < String > ();

		String result[] = null;
		try {
			myStatment = databaseConnection.createStatement();
			queryResults = myStatment.executeQuery("SELECT * FROM " + itemTable + " WHERE Type = '" + itemType + "'");

			if (queryResults.next() == false) {
				myStatment.close();
				return null;
			} else {
				possibleManufacturers.add(queryResults.getString("ManuID"));
				while (queryResults.next()) {
					possibleManufacturers.add(queryResults.getString("ManuID"));
				}
				myStatment.close();
				result = new String[possibleManufacturers.size()];
				possibleManufacturers.toArray(result);
				//return result;
			}
		} catch(SQLException ex) {
			ex.printStackTrace();
		}

		try {
			List < Map < String,
			Object >> queryResultList = new ArrayList < Map < String,
			Object >> ();
			Map < String,
			Object > entry = null;

			for (int i = 0; i < result.length; i++) {
				myStatment = databaseConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
				queryResults = myStatment.executeQuery("SELECT * FROM MANUFACTURER WHERE ManuID = '" + result[i] + "'");

				if (queryResults.next() == false) {
					myStatment.close();
					return queryResultList;
				} else {
					ResultSetMetaData metaData = queryResults.getMetaData();
					Integer columnCount = metaData.getColumnCount();

					entry = new HashMap < String,
					Object > ();
					for (int j = 1; j <= columnCount.intValue(); j++) {
						entry.put(metaData.getColumnName(j), queryResults.getObject(j));
					}
					queryResultList.add(entry);
				}
			}
			myStatment.close();
			return queryResultList;
		}
		catch(SQLException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	public void deleteUsedItems(String[] id, String[] furnitureItem) {
		String itemTable;
		for (int i = 0; i < id.length; i++) {
			itemTable = getItemTable(furnitureItem[i]);

			try {
				String query = "DELETE FROM " + itemTable + " WHERE ID = ?";
				myPreparedStatment = databaseConnection.prepareStatement(query);

				myPreparedStatment.setString(1, id[i]);

				int rowCount = myPreparedStatment.executeUpdate();
				myPreparedStatment.close();
			} catch(SQLException ex) {
				ex.printStackTrace();
			}
		}
	}

	public void close() {
		try {
			queryResults.close();
			databaseConnection.close();
		} catch(SQLException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		DatabaseConnection testConnection = new DatabaseConnection("jdbc:mysql://localhost/inventory", "Ahmed", "ensf409");

		List < Map < String,
		Object >> orderResult = testConnection.getItemRecords("mesh chair");
		Iterator < Map < String,
		Object >> resultIterator = orderResult.iterator();

		System.out.println("Possible Items are: ");

		while (resultIterator.hasNext()) {
			Map < String,
			Object > temp = resultIterator.next();
			System.out.println("ID: " + temp.get("ID").toString() + " and Type: " + temp.get("Type").toString());
		}

		List < Map < String,
		Object >> manufacturersResult = testConnection.getPossibleManufacturer("mesh chair");
		Iterator < Map < String,
		Object >> manufacturersResultIterator = manufacturersResult.iterator();
		System.out.println("\n\nPossible Manufacturers are:");
		while (manufacturersResultIterator.hasNext()) {
			Map < String,
			Object > temp = manufacturersResultIterator.next();
			System.out.println("ManuID: " + temp.get("ManuID").toString() + " and Name: " + temp.get("Name").toString());
		}

		testConnection.close();

	}
}
