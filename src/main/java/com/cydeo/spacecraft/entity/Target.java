package com.cydeo.spacecraft.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
@Setter
@Getter
@Data
public class Target extends BaseEntity {
    private Integer health;
    private Integer armor;
    private Integer shootPower;

    @ManyToOne
    @JoinColumn
    @JsonIgnore
    private Game game;
}
