package com.sportstock.ingestion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "athletes", schema = "ingestion")
@Getter
@Setter
@NoArgsConstructor
public class Athlete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "espn_id", nullable = false, unique = true, length = 15)
    private String espnId;

    @Column(name = "espn_uid", length = 50)
    private String espnUid;

    @Column(name = "espn_guid", length = 60)
    private String espnGuid;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "short_name", length = 100)
    private String shortName;

    @Column(length = 200)
    private String slug;

    @Column(precision = 6, scale = 1)
    private BigDecimal weight;

    @Column(precision = 5, scale = 1)
    private BigDecimal height;

    private Integer age;

    @Column(name = "date_of_birth")
    private Instant dateOfBirth;

    @Column(name = "debut_year")
    private Integer debutYear;

    @Column(length = 5)
    private String jersey;

    @Column(name = "position_id", length = 10)
    private String positionId;

    @Column(name = "position_name", length = 50)
    private String positionName;

    @Column(name = "position_abbreviation", length = 10)
    private String positionAbbreviation;

    @Column(name = "position_parent_name", length = 50)
    private String positionParentName;

    @Column(name = "position_parent_abbreviation", length = 10)
    private String positionParentAbbreviation;

    @Column(name = "birth_city", length = 100)
    private String birthCity;

    @Column(name = "birth_state", length = 15)
    private String birthState;

    @Column(name = "birth_country", length = 50)
    private String birthCountry;

    @Column(name = "college_espn_id", length = 20)
    private String collegeEspnId;

    @Column(name = "college_name", length = 100)
    private String collegeName;

    @Column(name = "college_abbreviation", length = 10)
    private String collegeAbbreviation;

    @Column(name = "headshot_url", length = 500)
    private String headshotUrl;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "status_id", length = 5)
    private String statusId;

    @Column(name = "status_name", length = 30)
    private String statusName;

    @Column(name = "status_type", length = 30)
    private String statusType;

    @Column(name = "hand_type", length = 10)
    private String handType;

    @Column(name = "alternate_ids_sdr", length = 20)
    private String alternateIdsSdr;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
