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
@Table(name = "stage_timelines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageTimeline {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "advertise_tender")
    private String advertiseTender;

    @Column(name = "tender_opening")
    private String tenderOpening;

    @Column(name = "tender_evaluation")
    private String tenderEvaluation;

    @Column(name = "approval_to_award")
    private String approvalToAward;

    @Column(name = "notification_of_award")
    private String notificationOfAward;

    @Column(name = "signing_of_contract")
    private String signingOfContract;

    @Column(name = "completion_of_contract")
    private String completionOfContract;

    @Column(name = "total_time")
    private String totalTime;
}
