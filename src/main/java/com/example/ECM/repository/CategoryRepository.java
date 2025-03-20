package com.example.ECM.repository;

import com.example.ECM.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Tìm kiếm Category theo tên
    Optional<Category> findByName(String name);

    // Tìm kiếm Category theo id
    Optional<Category> findById(Long id);
}
