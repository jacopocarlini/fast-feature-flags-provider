package io.github.jacopocarlini.fffp.repository;

import io.github.jacopocarlini.fffp.config.MongoClientManager;
import io.github.jacopocarlini.fffp.entity.AssignedTarget;
import java.util.Optional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

public class AssignedTargetRepository {
  private final MongoTemplate mongoTemplate;

  public AssignedTargetRepository(MongoClientManager mongoClientManager) {
    this.mongoTemplate = mongoClientManager.getMongoTemplate();
  }

  public void deleteAllByFlagKey(String flagKey) {
    Query query = new Query(Criteria.where("flagKey").is(flagKey));
    mongoTemplate.remove(query, AssignedTarget.class);
  }

  public void save(AssignedTarget assignedTarget) {
    mongoTemplate.save(assignedTarget);
  }

  public Optional<AssignedTarget> findFirstByFlagKeyAndTargetKey(String flagKey, String targetKey) {
    Query query = new Query(Criteria.where("flagKey").is(flagKey).and("targetKey").is(targetKey));
    var entity = mongoTemplate.findOne(query, AssignedTarget.class);
    return Optional.ofNullable(entity);
  }
}
