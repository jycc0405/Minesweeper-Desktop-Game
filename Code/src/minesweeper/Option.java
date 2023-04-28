package minesweeper;

import java.sql.*;
import java.sql.Statement;

public class Option {

    private int diffOption;
    private  boolean qMark;

    public Option(){loadOption();}

    public void loadOption()
    {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;


        try {
            String dbURL = Game.dbPath;

            connection = DriverManager.getConnection(dbURL);
            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM OPTION");

            resultSet.next();
            diffOption=resultSet.getInt("diffOption");
            qMark=resultSet.getBoolean("qMark");

            // cleanup resources, once after processing
            resultSet.close();
            statement.close();

            // and then finally close connection
            connection.close();

        }
        catch(SQLException sqlex)
        {
            sqlex.printStackTrace();
        }
    }

    public void saveDiffOption(int diffOption)
    {
        this.diffOption=diffOption;
        Connection connection = null;
        PreparedStatement statement = null;


        try {
            String dbURL = Game.dbPath;

            connection = DriverManager.getConnection(dbURL);


            String template = "UPDATE OPTION set diffOption = ?";
            statement = connection.prepareStatement(template);

            statement.setInt(1, diffOption);

            statement.executeUpdate();

            statement.close();
            // and then finally close connection
            connection.close();

        }
        catch(SQLException sqlex)
        {
            sqlex.printStackTrace();
        }
    }

    public void saveqMarkOption(boolean qMark)
    {
        this.qMark=qMark;
        Connection connection = null;
        PreparedStatement statement = null;


        try {
            String dbURL = Game.dbPath;

            connection = DriverManager.getConnection(dbURL);


            String template = "UPDATE OPTION set qMark = ?";
            statement = connection.prepareStatement(template);

            statement.setBoolean(1, qMark);

            statement.executeUpdate();

            statement.close();
            // and then finally close connection
            connection.close();

        }
        catch(SQLException sqlex)
        {
            sqlex.printStackTrace();
        }
    }

    public int getDiffOption(){return diffOption;}
    public boolean getqMark(){return qMark;}
}
