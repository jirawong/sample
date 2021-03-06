/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.th.linksinnovation.mitrphol.compliance.controller;

import co.th.linksinnovation.mitrphol.compliance.model.Category;
import co.th.linksinnovation.mitrphol.compliance.model.Compliance;
import co.th.linksinnovation.mitrphol.compliance.model.EvidenceFile;
import co.th.linksinnovation.mitrphol.compliance.model.LegalDuty;
import co.th.linksinnovation.mitrphol.compliance.model.LegalFile;
import co.th.linksinnovation.mitrphol.compliance.model.LegalType;
import co.th.linksinnovation.mitrphol.compliance.model.LicenseFile;
import co.th.linksinnovation.mitrphol.compliance.model.Status;
import co.th.linksinnovation.mitrphol.compliance.repository.CategoryRepository;
import co.th.linksinnovation.mitrphol.compliance.repository.ComplianceRepository;
import co.th.linksinnovation.mitrphol.compliance.repository.EvidenceFileRepository;
import co.th.linksinnovation.mitrphol.compliance.repository.LegalDutyRepository;
import co.th.linksinnovation.mitrphol.compliance.repository.LegalFileRepository;
import co.th.linksinnovation.mitrphol.compliance.repository.LicenseFileRepository;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Piyawut Chiradejnunt<pchiradejnunt@gmail.com>
 */
@RestController
@RequestMapping("/api")
public class ProgressUploadController {

    private static final int BUFFER_SIZE = 1024 * 100;

    private static final SimpleDateFormat FD = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");

    private static final int CATEGORY_LAST_COLUMN = 7;

    @Autowired
    private ComplianceRepository complianceRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private LegalFileRepository legalFileRepository;
    @Autowired
    private LicenseFileRepository licenseFileRepository;
    @Autowired
    private EvidenceFileRepository evidenceFileRepository;
    @Autowired
    private LegalDutyRepository legalDutyRepository;

    @RequestMapping(value = "/templateupload", method = RequestMethod.PUT)
    public void templateUpload(@RequestBody byte[] file, HttpServletRequest request) throws UnsupportedEncodingException, FileNotFoundException, IOException, ParseException {

        InputStream chunk = new ByteArrayInputStream(file);
        String filename = URLDecoder.decode(request.getHeader("Content-Name"), "UTF-8");
        appendFile(request.getHeader("Content-Start"), chunk, new File("/mnt/data/files/" + request.getHeader("Content-Name") + "-" + filename));

        if (request.getHeader("Content-End") != null && request.getHeader("Content-End").equals(request.getHeader("Content-FileSize"))) {
            try (FileInputStream inputStream = new FileInputStream(new File("/mnt/data/files/" + request.getHeader("Content-Name") + "-" + filename))) {

                Workbook workbook = new XSSFWorkbook(inputStream);
                Sheet firstSheet = workbook.getSheetAt(0);
                Iterator<Row> iterator = firstSheet.iterator();

                while (iterator.hasNext()) {
                    Row nextRow = iterator.next();
                    System.out.println("RowNum: " + nextRow.getRowNum());
                    if (nextRow.getRowNum() == 0) {
                        continue;
                    } else if (getCellValue(nextRow.getCell(16)) == null || getCellValue(nextRow.getCell(16)).equals("")) {
                        break;
                    }

                    final Category category = saveCategory(nextRow, 0, null).get("last");

                    // get compliance
                    final String legalname = getCellValue(nextRow.getCell(8));
                    final Double year = Double.parseDouble(getCellValue(nextRow.getCell(9)));

                    Calendar publicCalendar = Calendar.getInstance();
                    publicCalendar.setTime(FD.parse(getCellValue(nextRow.getCell(10))));
                    publicCalendar.add(Calendar.YEAR, -543);
                    final Date publicDate = publicCalendar.getTime();

                    Calendar effectiveCalendar = Calendar.getInstance();
                    effectiveCalendar.setTime(FD.parse(getCellValue(nextRow.getCell(11))));
                    effectiveCalendar.add(Calendar.YEAR, -543);
                    final Date effectiveDate = effectiveCalendar.getTime();

                    final Status status = Status.valueOf(getCellValue(nextRow.getCell(12)).toUpperCase());

                    final String department = getCellValue(nextRow.getCell(13));
                    final String ministry = getCellValue(nextRow.getCell(14));
                    final String important = getCellValue(nextRow.getCell(15));
                    final String legalDuty = getCellValue(nextRow.getCell(16));
                    final LegalType legalType;
                    if ("ใบอนุญาต".equals(getCellValue(nextRow.getCell(17)))) {
                        legalType = LegalType.LICENSE;
                    } else {
                        legalType = LegalType.EVIDENCE;
                    }
                    final String tag = getCellValue(nextRow.getCell(18));

                    Compliance compliance = new Compliance();
                    compliance.setLegalName(legalname);
                    compliance.setYear(year.intValue());
                    compliance.setPublicDate(publicDate);
                    compliance.setEffectiveDate(effectiveDate);
                    compliance.setStatus(status);
                    compliance.setDepartment(department);
                    compliance.setMinistry(ministry);
                    compliance.setImportant(important);
                    compliance.setTags(tag);

                    compliance.setCategory(category);

                    Compliance c = complianceRepository.findByLegalNameAndCategory(legalname, category);
                    if (c == null) {
                        LegalDuty newLegalDuty = new LegalDuty();
                        newLegalDuty.setName(legalDuty);
                        newLegalDuty.setLegalType(legalType);
                        newLegalDuty.setCompliance(compliance);
                        compliance = complianceRepository.save(compliance);
                        LegalDuty save = legalDutyRepository.save(newLegalDuty);
                        compliance.addLegalDuties(save);
                        complianceRepository.save(compliance);
                    } else {
                        LegalDuty newLegalDuty = new LegalDuty();
                        newLegalDuty.setName(legalDuty);
                        newLegalDuty.setLegalType(legalType);
                        newLegalDuty.setCompliance(c);
                        LegalDuty save = legalDutyRepository.save(newLegalDuty);
                        c.addLegalDuties(save);
                        complianceRepository.save(c);
                    }
                }
                workbook.close();
            }
        }
    }

    @Transactional
    private Map<String, Category> saveCategory(Row nextRow, int col, Category parent) {
        System.out.println("col ----> " + col);
        if (parent != null) {
            System.out.println("--------- parent ---->" + parent.getId());
        } else {
            System.out.println("---NULL");
        }
        if (col > CATEGORY_LAST_COLUMN) {
            Map<String, Category> map = new HashMap<>();
            map.put("child", null);
            map.put("last", parent);
            System.out.println("---- inner last -" + parent.getId());
            return map;
        }

        String categoryName = nextRow.getCell(col).getStringCellValue();
        if (categoryName == null || "".equals(categoryName)) {
            return saveCategory(nextRow, col + 1, parent);
        }
        Category category = categoryRepository.findByNameAndParentAndDeletedIsFalse(categoryName, parent);

        if (category == null) {
            category = new Category();
            category.setName(categoryName);
            category.setParent(parent);
            Map<String, Category> map = new HashMap<>();
            map.put("child", categoryRepository.saveAndFlush(category));
            Map<String, Category> saveCategory = saveCategory(nextRow, col + 1, map.get("child"));
            map.put("last", saveCategory.get("last"));
            category.addChild(saveCategory.get("child"));
            return map;
        } else {
            Map<String, Category> map = new HashMap<>();
            map.put("child", categoryRepository.saveAndFlush(category));
            Map<String, Category> saveCategory = saveCategory(nextRow, col + 1, map.get("child"));
            map.put("last", saveCategory.get("last"));
            category.addChild(saveCategory.get("child"));
            return map;
        }
    }

    @RequestMapping(value = "/fileupload", method = RequestMethod.PUT)
    public void pdfUpload(@RequestBody byte[] file, HttpServletRequest request) throws UnsupportedEncodingException {
        InputStream chunk = new ByteArrayInputStream(file);
        String filename = URLDecoder.decode(request.getHeader("Content-Name"), "UTF-8");
        appendFile(request.getHeader("Content-Start"), chunk, new File("/mnt/data/files/" + request.getHeader("Content-Name") + "-" + filename));
    }

    @RequestMapping(value = "/localeupload/{name}", method = RequestMethod.PUT)
    public void localeUpload(@RequestBody byte[] file, @PathVariable String name, HttpServletRequest request)
            throws Exception {
        InputStream chunk = new ByteArrayInputStream(file);
        appendFile(request.getHeader("Content-Start"), chunk, new File("/mnt/locales/" + name + ".json"));

    }

    @RequestMapping(value = "/legalupload", method = RequestMethod.PUT)
    public LegalFile legalFileUpload(@RequestBody byte[] file, HttpServletRequest request) throws UnsupportedEncodingException, IOException, Exception {
        InputStream chunk = new ByteArrayInputStream(file);
        String filename = URLDecoder.decode(request.getHeader("Content-Name"), "UTF-8");
        appendFile(request.getHeader("Content-Start"), chunk, new File("/mnt/data/tmp/" + filename));
        if (request.getHeader("Content-End") != null && request.getHeader("Content-End").equals(request.getHeader("Content-FileSize"))) {
            String uuid = UUID.randomUUID().toString();
            move("/mnt/data/tmp/" + filename, "/mnt/data/files/" + uuid);
            LegalFile legalFile = new LegalFile();
            legalFile.setName(filename);
            legalFile.setUuid(uuid);
            return legalFileRepository.save(legalFile);
        } else {
            return null;
        }
    }

    @RequestMapping(value = "/licenseupload", method = RequestMethod.PUT)
    public LicenseFile licenseFileUpload(@RequestBody byte[] file, HttpServletRequest request) throws UnsupportedEncodingException, IOException, Exception {
        InputStream chunk = new ByteArrayInputStream(file);
        String filename = URLDecoder.decode(request.getHeader("Content-Name"), "UTF-8");
        appendFile(request.getHeader("Content-Start"), chunk, new File("/mnt/data/tmp/" + filename));
        if (request.getHeader("Content-End") != null && request.getHeader("Content-End").equals(request.getHeader("Content-FileSize"))) {
            String uuid = UUID.randomUUID().toString();
            move("/mnt/data/tmp/" + filename, "/mnt/data/files/" + uuid);
            LicenseFile licenseFile = new LicenseFile();
            licenseFile.setName(filename);
            licenseFile.setUuid(uuid);
            return licenseFileRepository.save(licenseFile);
        } else {
            return null;
        }
    }

    @RequestMapping(value = "/evidenceupload", method = RequestMethod.PUT)
    public EvidenceFile evidenceFileUpload(@RequestBody byte[] file, HttpServletRequest request) throws UnsupportedEncodingException, IOException, Exception {
        InputStream chunk = new ByteArrayInputStream(file);
        String filename = URLDecoder.decode(request.getHeader("Content-Name"), "UTF-8");
        appendFile(request.getHeader("Content-Start"), chunk, new File("/mnt/data/tmp/" + filename));
        if (request.getHeader("Content-End") != null && request.getHeader("Content-End").equals(request.getHeader("Content-FileSize"))) {
            String uuid = UUID.randomUUID().toString();
            move("/mnt/data/tmp/" + filename, "/mnt/data/files/" + uuid);
            EvidenceFile evidenceFile = new EvidenceFile();
            evidenceFile.setName(filename);
            evidenceFile.setUuid(uuid);
            return evidenceFileRepository.save(evidenceFile);
        } else {
            return null;
        }
    }

    private void appendFile(String start, InputStream in, File dest) {
        OutputStream out = null;

        try {
            if (dest.exists()) {
                if (start.equals("0")) {
                    if (dest.delete()) {
                        out = new BufferedOutputStream(new FileOutputStream(dest), BUFFER_SIZE);
                    }
                }
                out = new BufferedOutputStream(new FileOutputStream(dest, true), BUFFER_SIZE);
            } else {
                out = new BufferedOutputStream(new FileOutputStream(dest), BUFFER_SIZE);
            }
            in = new BufferedInputStream(in, BUFFER_SIZE);

            int len = 0;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        } catch (Exception ex) {
            System.out.println(ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
                System.out.println(ex);
            }
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        final CellType type = cell.getCellTypeEnum();
        switch (type) {
            case BLANK:
                return "";
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case NUMERIC:
                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                    return String.valueOf(cell.getDateCellValue());
                }
                return String.valueOf(cell.getNumericCellValue());
            default:
                // String
                return cell.getStringCellValue();
        }

    }
    
    private void move(String src, String dest) throws Exception{
        File srcFile = new File(src);
        File desFile = new File(dest);
        FileCopyUtils.copy(srcFile, desFile);
        srcFile.delete();
    }
}
