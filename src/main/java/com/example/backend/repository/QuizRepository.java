package com.example.backend.repository;

import com.example.backend.entity.Quiz;
import com.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByUser(User user);

    long countByUser(User user);

    void deleteByUser(User user);

    List<Quiz> findTop3ByUserOrderByCreatedAtDescIdDesc(User user);

}
