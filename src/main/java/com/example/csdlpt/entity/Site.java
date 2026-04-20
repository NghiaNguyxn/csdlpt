package com.example.csdlpt.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "site")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "site_code", nullable = false, unique = true, length = 10)
    private String siteCode;

    @Column(name = "site_name", length = 100)
    private String siteName;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String description;
}
