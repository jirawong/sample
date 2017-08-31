/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.th.linksinnovation.mitrphol.compliance.controller;

import co.th.linksinnovation.mitrphol.compliance.model.Accord;
import co.th.linksinnovation.mitrphol.compliance.model.Authority;
import co.th.linksinnovation.mitrphol.compliance.model.Compliance;
import co.th.linksinnovation.mitrphol.compliance.model.JsonViewer;
import co.th.linksinnovation.mitrphol.compliance.model.LegalCategory;
import co.th.linksinnovation.mitrphol.compliance.model.LegalGroup;
import co.th.linksinnovation.mitrphol.compliance.model.UserDetails;
import co.th.linksinnovation.mitrphol.compliance.model.authen.Authenticate;
import co.th.linksinnovation.mitrphol.compliance.repository.AccordRepository;
import co.th.linksinnovation.mitrphol.compliance.repository.LegalcategoryRepository;
import co.th.linksinnovation.mitrphol.compliance.repository.LegalgroupRepository;
import co.th.linksinnovation.mitrphol.compliance.repository.UserDetailsRepository;
import co.th.linksinnovation.mitrphol.compliance.service.RestService;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author jirawong
 */
@RestController
@RequestMapping("/api/legalcategory")
public class LegalcategoryController {

    @Autowired
    private LegalcategoryRepository legalcategoryRepository;
    @Autowired
    private UserDetailsRepository userDetailsRepository;
    @Autowired
    private AccordRepository accordRepository;
    @Autowired
    private RestService restService;

    @GetMapping
    @JsonView(JsonViewer.ComplianceWithCategory.class)
    public List<LegalCategory> get() {
        return legalcategoryRepository.findAll();
    }

    @GetMapping("/{id}")
    @JsonView(JsonViewer.ComplianceWithCategory.class)
    public LegalCategory get(@PathVariable("id") Long id) {
        return legalcategoryRepository.findOne(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        legalcategoryRepository.delete(id);
    }
    
    @PostMapping("/approve")
    @JsonView(JsonViewer.ComplianceWithCategory.class)
    public LegalCategory approve(@RequestBody LegalCategory legalCategory){
        LegalCategory findOne = legalcategoryRepository.findOne(legalCategory.getId());
        findOne.setApproved(Boolean.TRUE);
        return legalcategoryRepository.save(findOne);
    }

    @PostMapping
    @JsonView(JsonViewer.ComplianceWithCategory.class)
    public LegalCategory post(@RequestBody LegalCategory legalCategory) {
        Set<UserDetails> owners = new HashSet<>();
        Set<UserDetails> approvers = new HashSet<>();

        if (!legalCategory.getOwners().isEmpty()) {
            for (UserDetails u : legalCategory.getOwners()) {
                List<UserDetails> userdetails = userDetailsRepository.findByUserId(u.getUserId());
                if (userdetails.isEmpty()) {
                    Authenticate findUserInfo = restService.findUserInfo(u.getUserId());
                    if (findUserInfo.getSuccess().getCode() == 200) {
                        UserDetails ud = new UserDetails();
                        ud.setUsername(findUserInfo.getSuccess().getData().getUserInfo().getUsername());
                        ud.setUserId(findUserInfo.getSuccess().getData().getUserInfo().getId());
                        ud.setNameEn(findUserInfo.getSuccess().getData().getUserInfo().getFullname().getEn());
                        ud.setNameTh(findUserInfo.getSuccess().getData().getUserInfo().getFullname().getTh());
                        ud.setEmail(findUserInfo.getSuccess().getData().getUserInfo().getEmail());
                        ud.setPhoto(findUserInfo.getSuccess().getData().getUserInfo().getPhoto());
                        Authority authority = new Authority();
                        authority.setAuthority("Owner");
                        ud.addAuthority(authority);
                        UserDetails save = userDetailsRepository.save(ud);
                        owners.add(save);
                    }
                } else {
                    owners.add(userdetails.get(0));
                }
            }
        }

        if (!legalCategory.getApprovers().isEmpty()) {
            for (UserDetails u : legalCategory.getApprovers()) {
                List<UserDetails> userdetails = userDetailsRepository.findByUserId(u.getUserId());
                if (userdetails.isEmpty()) {
                    Authenticate findUserInfo = restService.findUserInfo(u.getUserId());
                    if (findUserInfo.getSuccess().getCode() == 200) {
                        UserDetails ud = new UserDetails();
                        ud.setUsername(findUserInfo.getSuccess().getData().getUserInfo().getUsername());
                        ud.setUserId(findUserInfo.getSuccess().getData().getUserInfo().getId());
                        ud.setNameEn(findUserInfo.getSuccess().getData().getUserInfo().getFullname().getEn());
                        ud.setNameTh(findUserInfo.getSuccess().getData().getUserInfo().getFullname().getTh());
                        ud.setEmail(findUserInfo.getSuccess().getData().getUserInfo().getEmail());
                        ud.setPhoto(findUserInfo.getSuccess().getData().getUserInfo().getPhoto());
                        Authority authority = new Authority();
                        authority.setAuthority("Approver");
                        ud.addAuthority(authority);
                        UserDetails save = userDetailsRepository.save(ud);
                        approvers.add(save);
                    }
                } else {
                    approvers.add(userdetails.get(0));
                }
            }
        }

        legalCategory.setOwners(owners);
        legalCategory.setApprovers(approvers);
        legalCategory = legalcategoryRepository.save(legalCategory);

        if (!legalCategory.getCompliances().isEmpty()) {
            for (Compliance c : legalCategory.getCompliances()) {
                Accord ac = accordRepository.findByLegalCategoryAndCompliance(legalCategory, c);
                if (ac == null) {
                    ac = new Accord();
                    ac.setLegalCategory(legalCategory);
                    ac.setCompliance(c);
                    ac = accordRepository.save(ac);
                    legalCategory.getAccords().add(ac);
                }
            }
        }
        
        return legalcategoryRepository.save(legalCategory);
    }
}