package com.realestate.repository;

import com.realestate.model.User;
import com.realestate.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    Optional<User> findByEmailIgnoreCase(String email);

    @Query("select u from User u join fetch u.role where lower(u.email) = lower(:email)")
    Optional<User> findByEmailIgnoreCaseWithRole(@Param("email") String email);

    List<User> findAllByRole_Name(RoleName roleName);

    long countByRole_Name(RoleName roleName);
}
