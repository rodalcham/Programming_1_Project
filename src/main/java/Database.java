import java.sql.*;

public class Database {
    private static final String URL = "jdbc:sqlite:grades.db";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initialize() {
        String sql = "CREATE TABLE IF NOT EXISTS grades (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                     "student TEXT NOT NULL," +
                     "grade TEXT NOT NULL)";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Database initialized.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void insertGrade(String student, String grade) {
        String sql = "INSERT INTO grades (student, grade) VALUES (?, ?)";
        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, student);
            stmt.setString(2, grade);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getGradesAsHtmlTable() {
        StringBuilder sb = new StringBuilder();
        String sql = "SELECT student, grade FROM grades";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            sb.append("<table border='1'>")
              .append("<tr><th>Student</th><th>Grade</th></tr>");
            while (rs.next()) {
                sb.append("<tr>")
                  .append("<td>").append(rs.getString("student")).append("</td>")
                  .append("<td>").append(rs.getString("grade")).append("</td>")
                  .append("</tr>");
            }
            sb.append("</table>");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

	public static String getGradesAsHtmlRows() {
    StringBuilder sb = new StringBuilder();
    String sql = "SELECT id, student, grade FROM grades";
    try (Connection conn = connect();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        int i = 1;
        while (rs.next()) {
            int id = rs.getInt("id");
            sb.append("<tr>")
              .append("<td>").append(i++).append("</td>")
              .append("<td>").append(rs.getString("student")).append("</td>")
              .append("<td><span class='grade-badge'>").append(rs.getString("grade")).append("</span></td>")
              .append("<td><form method='POST' action='/grades/delete'>")
              .append("<input type='hidden' name='id' value='").append(id).append("'/>")
              .append("<button type='submit' class='btn btn-delete'>Delete</button>")
              .append("</form></td>")
              .append("</tr>");
        }
    } catch (SQLException e) { e.printStackTrace(); }
    return sb.toString();
	}

	public static void deleteGrade(int id) {
		String sql = "DELETE FROM grades WHERE id = ?";
		try (Connection conn = connect();
			PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setInt(1, id);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static double calculateMean() {
		String sql = "SELECT grade FROM grades";
		try (Connection conn = connect();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql)) {

			double sum = 0;
			int count = 0;
			while (rs.next()) {
				Double val = parseGrade(rs.getString("grade").trim().toUpperCase());
				if (val != null) { sum += val; count++; }
			}
			return count > 0 ? sum / count : -1;

		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
	}

	private static Double parseGrade(String s) {
    	try { return Double.parseDouble(s.replace("%", "")); }
    	catch (NumberFormatException e) { return null; }
	}
}