package io.github.jacopocarlini.fffp.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.github.jacopocarlini.fffp.providers.FlagService;
import io.github.jacopocarlini.fffp.repository.AssignedTargetRepository;
import io.github.jacopocarlini.fffp.repository.FlagRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class FlagEngineConfig {

  @Bean
  public MongoDatabaseFactory mongoDatabaseFactory(@Value("#{systemProperties['mongo.connection']}") String connectionString) {
    ConnectionString conn = new ConnectionString(connectionString);

    MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(conn)
            .retryWrites(true)
            .build();

    MongoClient client = MongoClients.create(settings);
    return new SimpleMongoClientDatabaseFactory(client, conn.getDatabase());
  }

  @Bean
  public MongoTemplate mongoTemplate(MongoDatabaseFactory factory) {
    return new MongoTemplate(factory);
  }

  @Bean
  public MongoTransactionManager transactionManager(MongoDatabaseFactory factory) {
    return new MongoTransactionManager(factory);
  }

  @Bean
  public FlagRepository flagRepository(MongoTemplate mongoTemplate) {
    return new FlagRepository(mongoTemplate);
  }

  @Bean
  public FlagService flagService() {
    return new FlagService();
  }

  @Bean
  public AssignedTargetRepository assignedTargetRepository(MongoTemplate mongoTemplate) {
    return new AssignedTargetRepository(mongoTemplate);
  }

}
