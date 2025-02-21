package com.github.jacopocarlini.fffp.repository;

import java.util.List;
import java.util.Optional;

import com.github.jacopocarlini.fffp.config.MongoClientManager;
import com.github.jacopocarlini.fffp.entity.Flag;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

public class FlagRepository {
  private final MongoTemplate mongoTemplate;

  public FlagRepository(MongoClientManager mongoClientManager) {
    this.mongoTemplate = mongoClientManager.getMongoTemplate();
  }

  public void deleteByFlagKey(String flagKey) {
    Query query = new Query(Criteria.where("flagKey").is(flagKey));
    mongoTemplate.remove(query, Flag.class);
  }

  public Optional<Flag> findFirstByFlagKey(String flagKey) {
    Query query = new Query(Criteria.where("flagKey").is(flagKey));
    var entity = mongoTemplate.findOne(query, Flag.class);
    return Optional.ofNullable(entity);
  }

  public void save(Flag flag) {
    mongoTemplate.save(flag);
  }

  public List<Flag> findAll() {
    return mongoTemplate.findAll(Flag.class);
  }
}
