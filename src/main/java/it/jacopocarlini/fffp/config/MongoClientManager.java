package it.jacopocarlini.fffp.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Slf4j
public class MongoClientManager {
  private MongoClient mongoClient;
  private MongoTemplate mongoTemplate;
  private final Lock lock = new ReentrantLock();
  private String currentConnectionString = null;

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

        // Salva la nuova connection string
        currentConnectionString = newConnectionString;

        log.info("🔄 Connessione aggiornata: " + newConnectionString);
      }
    } finally {
      lock.unlock();
    }
  }

  public void shutdown() {
    if (mongoClient != null) {
      mongoClient.close();
    }
  }

  public MongoTemplate getMongoTemplate() {
    if (mongoTemplate == null) {
      throw new IllegalStateException("❌ Nessuna connessione attiva a MongoDB.");
    }
    return mongoTemplate;
  }
}
