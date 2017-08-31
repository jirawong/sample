/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.th.linksinnovation.mitrphol.compliance.controller;

import co.th.linksinnovation.mitrphol.compliance.model.Accord;
import co.th.linksinnovation.mitrphol.compliance.model.Compliance;
import co.th.linksinnovation.mitrphol.compliance.model.JsonViewer;
import co.th.linksinnovation.mitrphol.compliance.model.LegalCategory;
import co.th.linksinnovation.mitrphol.compliance.repository.AccordRepository;
import co.th.linksinnovation.mitrphol.compliance.repository.ComplianceRepository;
import co.th.linksinnovation.mitrphol.compliance.repository.LegalcategoryRepository;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author jirawong
 */
@RestController
@RequestMapping("/api/accord")
public class AccordController {

    @Autowired
    private AccordRepository accordRepository;
    @Autowired
    private LegalcategoryRepository legalcategoryRepository;
    @Autowired
    private ComplianceRepository complianceRepository;

    @GetMapping("/{legalCategory}/{compliance}")
    @JsonView(JsonViewer.ComplianceWithCategory.class)
    public Accord get(@PathVariable("legalCategory") Long legalCategory,@PathVariable("compliance") Long compliance) {
        LegalCategory lc = legalcategoryRepository.findOne(legalCategory);
        Compliance c = complianceRepository.findOne(compliance);
        return accordRepository.findByLegalCategoryAndCompliance(lc, c);
    }
    
    @PostMapping
    public Accord post(@RequestBody Accord accord){
        Accord ac = accordRepository.save(accord);
        LegalCategory legalCategory = ac.getLegalCategory();
        legalCategory.setApproved(Boolean.FALSE);
        legalcategoryRepository.save(legalCategory);
        return ac;
    }
}