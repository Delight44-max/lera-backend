package com.lera.repository;

import com.lera.model.Emergency;
import com.lera.model.EmergencyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyRepository extends JpaRepository<Emergency, String> {


    Optional<Emergency> findFirstByCitizenIdAndStatusIn(
        String citizenId, List<EmergencyStatus> statuses);


    Optional<Emergency> findFirstByResponderIdAndStatusIn(
        String responderId, List<EmergencyStatus> statuses);


    List<Emergency> findByCitizenIdOrderByCreatedAtDesc(String citizenId);


    List<Emergency> findByResponderIdOrderByCreatedAtDesc(String responderId);


    List<Emergency> findByStatusAndResponderIsNullOrderByCreatedAtAsc(EmergencyStatus status);

    @Query("SELECT e FROM Emergency e LEFT JOIN FETCH e.citizen LEFT JOIN FETCH e.responder WHERE e.id = :id")
    Optional<Emergency> findByIdWithUsers(@Param("id") String id);
}
