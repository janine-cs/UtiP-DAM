package com.utipdam.mobility.model.repository;

import com.utipdam.mobility.model.entity.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatasetRepository extends JpaRepository<Dataset, UUID> {
    List<Dataset> findAll();

    Optional<Dataset> findById(@Param("id") UUID id);

    Dataset findByDatasetDefinitionIdAndStartDate(@Param("datasetDefinitionId") UUID datasetDefinitionId, @Param("startDate") String startDate);
    @Query("SELECT dt FROM dataset as dt, (SELECT d.datasetDefinition.id as datasetId, MAX(d.startDate) AS startDate " +
            "FROM dataset as d JOIN dataset_definition dd ON d.datasetDefinition.id = dd.id GROUP BY dd.id ORDER BY startDate DESC) as sub " +
            "WHERE dt.datasetDefinition.id = sub.datasetId and dt.startDate = sub.startDate")
    List<Dataset> findAllByOrderByStartDateDesc();

}
