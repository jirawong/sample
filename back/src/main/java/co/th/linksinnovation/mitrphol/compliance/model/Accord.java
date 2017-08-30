/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.th.linksinnovation.mitrphol.compliance.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import lombok.Data;

/**
 *
 * @author jirawong
 */
@Data
@Entity
public class Accord {
    @Id
    @GeneratedValue
    private Long id;
    @Column(length = 4000)
    private String remark;
    @Enumerated(EnumType.STRING)
    private Accorded accorded;
    @ManyToOne
    @JsonBackReference
    private LegalCategory legalCategory;
    @ManyToOne
    private Compliance compliance;
}
