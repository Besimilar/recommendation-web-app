package db.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import db.DBConnection;
import entity.Item;
import entity.Item.ItemBuilder;
import external.TicketMasterAPI;

public class MySQLConnection implements DBConnection {
  private Connection conn;

  public MySQLConnection() {
    try {
      Class.forName("com.mysql.cj.jdbc.Driver").getConstructor().newInstance();
      conn = DriverManager.getConnection(MySQLDBUtil.URL);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void close() {
    if (conn == null) {
      return;
    }

    // close connection using default close()
    try {
      conn.close();
    } catch (Exception e) {
      // better than system.out.println()
      // see stack trace info
      e.printStackTrace();
    }
  }

  @Override
  public void setFavoriteItems(String userId, List<String> itemIds) {
    if (conn == null) {
      System.out.println("conn is null! in setFavoriteItems()");
      return;
    }

    try {
      String sql = "INSERT IGNORE INTO history (user_id, item_id) VALUES (?, ?)";
      PreparedStatement stmt = conn.prepareStatement(sql);
      for (String itemId : itemIds) {
        stmt.setString(1, userId);
        stmt.setString(2, itemId);
        System.out.println(userId);
        System.out.println(itemId);
        stmt.execute();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void unsetFavoriteItems(String userId, List<String> itemIds) {
    if (conn == null) {
      System.out.println("conn is null! in unsetFavoriteItems()");
      return;
    }

    try {
      String sql = "DELETE FROM history WHERE user_id = ? AND item_id = ?";
      PreparedStatement stmt = conn.prepareStatement(sql);
      for (String itemId : itemIds) {
        stmt.setString(1, userId);
        stmt.setString(2, itemId);
        stmt.execute();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public Set<String> getFavoriteItemIds(String userId) {
    if (conn == null) {
      return new HashSet<>();
    }

    Set<String> favoriteItemIds = new HashSet<>();

    try {
      String sql = "SELECT item_id FROM history where user_id = ?";
      PreparedStatement stmt = conn.prepareStatement(sql);
      stmt.setString(1, userId);
      ResultSet rs = stmt.executeQuery();

      while (rs.next()) {
        String itemId = rs.getString("item_id");
        favoriteItemIds.add(itemId);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return favoriteItemIds;
  }

  @Override
  public Set<Item> getFavoriteItems(String userId) {
    if (conn == null) {
      return new HashSet<>();
    }

    Set<Item> favoriteItems = new HashSet<>();
    Set<String> itemIds = getFavoriteItemIds(userId);

    try {
      String sql = "SELECT * FROM items WHERE item_id = ?";
      PreparedStatement stmt = conn.prepareStatement(sql);
      for (String itemId : itemIds) {
        stmt.setString(1, itemId);
        ResultSet rs = stmt.executeQuery();

        ItemBuilder builder = new ItemBuilder();
        while (rs.next()) {
          builder.setItemId(rs.getString("item_id")).setName(rs.getString("name"))
              .setAddress(rs.getString("address")).setImageUrl(rs.getString("image_url"))
              .setUrl(rs.getString("url")).setCategories(getCategories(itemId))
              .setDistance(rs.getDouble("distance")).setRating(rs.getDouble("rating"));
          favoriteItems.add(builder.build());
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return favoriteItems;
  }

  @Override
  public Set<String> getCategories(String itemId) {
    if (conn == null) {
      return new HashSet<>();
    }

    Set<String> categories = new HashSet<>();

    try {
      String sql = "SELECT category FROM categories WHERE item_id = ?";
      PreparedStatement stmt = conn.prepareStatement(sql);
      stmt.setString(1, itemId);
      ResultSet rs = stmt.executeQuery();

      while (rs.next()) {
        String category = rs.getString("category");
        categories.add(category);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return categories;
  }

  @Override
  public List<Item> searchItems(double lat, double lon, String term) {
    TicketMasterAPI tmAPI = new TicketMasterAPI();
    List<Item> items = tmAPI.search(lat, lon, term);
    for (Item item : items) {
      saveItem(item);
    }
    return items;
  }

  @Override
  public void saveItem(Item item) {
    // We should report something wrong to the front-end
    if (conn == null) {
      return;
    }

    try {
      // SQL injection:
      // SELECT * FROM users WHERE username = '<username>' AND password = '<password>';
      // sql = "SELECT * FROM users WHERE username = '" + username
      // + "' AND password = '" + password + "'";

      // Case 1:
      // username: abcd
      // password: 123456

      // Case 2:
      // username: abcd' OR '1' = '1
      // password: 123456' OR '1' = '1
      // SELECT * FROM users WHERE username = 'abcd' OR '1' = '1' AND password = '123456' OR '1' =
      // '1';

      String sql = "INSERT IGNORE INTO items VALUES (?, ?, ?, ?, ?, ?, ?)";
      PreparedStatement stmt = conn.prepareStatement(sql);
      stmt.setString(1, item.getItemId());
      stmt.setString(2, item.getName());
      stmt.setDouble(3, item.getRating());
      stmt.setString(4, item.getAddress());
      stmt.setString(5, item.getImageUrl());
      stmt.setString(6, item.getUrl());
      stmt.setDouble(7, item.getDistance());
      stmt.execute();

      sql = "INSERT IGNORE INTO categories VALUES (?, ?)";
      stmt = conn.prepareStatement(sql);
      for (String category : item.getCategories()) {
        stmt.setString(1, item.getItemId());
        stmt.setString(2, category);
        stmt.execute();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public String getFullname(String userId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean verifyLogin(String userId, String password) {
    // TODO Auto-generated method stub
    return false;
  }

}
