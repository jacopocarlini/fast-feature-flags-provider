package io.github.jacopocarlini.fffp.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Manages the MongoDB client and provides a MongoTemplate for database interactions. Ensures
 * thread-safe operations when updating the connection settings.
 */
public class MongoClientManager {
  private final Lock lock = new ReentrantLock();
  private MongoClient mongoClient;
  private MongoTemplate mongoTemplate;
  private String currentConnectionString = null;

  /**
   * Updates the MongoDB connection. Closes the current connection if it exists and establishes a
   * new one with the provided connection string.
   *
   * @param newConnectionString the new MongoDB connection string
   * @throws IllegalStateException if the MongoDB database name is not specified in the connection
   *     string
   */
  public void updateConnection(String newConnectionString) {
    lock.lock();
    try {
      if (newConnectionString.equals(currentConnectionString)) {
        return;
      }

      if (mongoClient != null) {
        mongoClient.close();
      }

      ConnectionString connectionString = new ConnectionString(newConnectionString);
      MongoClientSettings settings =
          MongoClientSettings.builder().applyConnectionString(connectionString).build();
      mongoClient = MongoClients.create(settings);

      String database = connectionString.getDatabase();
      if (database != null) {
        mongoTemplate = new MongoTemplate(mongoClient, database);

        currentConnectionString = newConnectionString;
      }
    } finally {
      lock.unlock();
    }
  }

  /** Shuts down the MongoClient, closing any open connections. */
  public void shutdown() {
    if (mongoClient != null) {
      mongoClient.close();
    }
  }

  /**
   * Retrieves the current MongoTemplate for database operations.
   *
   * @return the MongoTemplate
   * @throws IllegalStateException if there is no active connection to MongoDB
   */
  public MongoTemplate getMongoTemplate() {
    if (mongoTemplate == null) {
      throw new IllegalStateException("❌ No active connection to MongoDB.");
    }
    return mongoTemplate;
  }
}
