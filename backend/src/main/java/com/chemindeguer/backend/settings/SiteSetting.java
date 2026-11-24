package com.chemindeguer.backend.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class SiteSetting {
    @Id
    @Column(length = 60)
    private String name;

    @Column(nullable = false, length = 200)
    private String value;

    public SiteSetting(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
