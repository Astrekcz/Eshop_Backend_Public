package org.example.eshopbackend.repository;

import org.example.eshopbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserID(Long userID);
    Optional<User> findByEmail(String userName);
    Optional<User> findByPhoneNumber(String phoneNumber);
   // boolean existsByUserName(String userName);
    boolean existsByEmail(String email);
}
