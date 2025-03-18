package com.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.userservice.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
	User findByEmail(String email);

}
