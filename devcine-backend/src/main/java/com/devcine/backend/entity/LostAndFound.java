package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lost_and_founds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LostAndFound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id")
    private Cinema cinema;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "found_by_staff")
    private Staff foundByStaff;

    @Column(name = "item_description", columnDefinition = "TEXT")
    private String itemDescription;

    @Column(name = "found_location")
    private String foundLocation;

    private String status;

    @Column(name = "found_at")
    private LocalDateTime foundAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @PrePersist
    protected void onCreate() {
        this.foundAt = LocalDateTime.now();
    }
}
