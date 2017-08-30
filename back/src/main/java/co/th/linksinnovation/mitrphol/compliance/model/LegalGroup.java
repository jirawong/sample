/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.th.linksinnovation.mitrphol.compliance.model;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import lombok.Data;

/**
 *
 * @author jirawong
 */
@Data
@Entity
public class LegalGroup {
    @Id
    @GeneratedValue
    private Long id;
    private String buName;
    @ManyToMany
    private List<UserDetails> coordinates;
    @ManyToMany
    private List<Compliance> compliances;
}
