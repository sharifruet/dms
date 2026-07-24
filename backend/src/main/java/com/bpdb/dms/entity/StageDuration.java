package com.bpdb.dms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stage_durations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageDuration {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "advertise_tender_days")
    private Integer advertiseTender;

    @Column(name = "tender_opening_days")
    private Integer tenderOpening;

    @Column(name = "tender_evaluation_days")
    private Integer tenderEvaluation;

    @Column(name = "approval_to_award_days")
    private Integer approvalToAward;

    @Column(name = "notification_of_award_days")
    private Integer notificationOfAward;

    @Column(name = "signing_of_contract_days")
    private Integer signingOfContract;

    @Column(name = "completion_of_contract_days")
    private Integer completionOfContract;

    @Column(name = "total_time_days")
    private Integer totalTime;
}
