package com.devcine.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate dob;

    @Column(name = "id_card")
    private String idCard;

    @Column(name = "membership_tier")
    private String membershipTier;

    @Column(name = "loyalty_points")
    private Integer loyaltyPoints = 0;
}
