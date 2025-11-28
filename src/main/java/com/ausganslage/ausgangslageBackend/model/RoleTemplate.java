package com.ausganslage.ausgangslageBackend.model;

import com.ausganslage.ausgangslageBackend.enums.Faction;
import com.ausganslage.ausgangslageBackend.enums.RoleName;
import jakarta.persistence.*;

@Entity
@Table(name = "role_templates")
public class RoleTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private RoleName name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Faction faction;

    @Column(nullable = false)
    private Boolean hasNightPower = false;

    @Column(nullable = false)
    private Integer defaultCount = 0;

    @Column(length = 500)
    private String description;

    public RoleTemplate() {
    }

    public RoleTemplate(Long id, RoleName name, Faction faction, Boolean hasNightPower, Integer defaultCount, String description) {
        this.id = id;
        this.name = name;
        this.faction = faction;
        this.hasNightPower = hasNightPower;
        this.defaultCount = defaultCount;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RoleName getName() {
        return name;
    }

    public void setName(RoleName name) {
        this.name = name;
    }

    public Faction getFaction() {
        return faction;
    }

    public void setFaction(Faction faction) {
        this.faction = faction;
    }

    public Boolean getHasNightPower() {
        return hasNightPower;
    }

    public void setHasNightPower(Boolean hasNightPower) {
        this.hasNightPower = hasNightPower;
    }

    public Integer getDefaultCount() {
        return defaultCount;
    }

    public void setDefaultCount(Integer defaultCount) {
        this.defaultCount = defaultCount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

