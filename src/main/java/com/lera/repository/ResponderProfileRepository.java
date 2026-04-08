package com.lera.repository;

import com.lera.model.Availability;
import com.lera.model.ResponderProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResponderProfileRepository extends JpaRepository<ResponderProfile, String> {
    Optional<ResponderProfile> findByUserId(String userId);
    List<ResponderProfile> findByAvailability(Availability availability);
}
