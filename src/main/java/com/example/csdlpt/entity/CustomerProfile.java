package com.example.csdlpt.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerProfile {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private CustomerIdentity identity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "main_site_id", nullable = false)
    private Site mainSite;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;
}
