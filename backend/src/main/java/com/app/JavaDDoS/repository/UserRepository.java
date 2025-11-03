package com.app.JavaDDoS.repository;

import com.app.JavaDDoS.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
    User findByUsername(String username);
}