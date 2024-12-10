package com.curateme.clacobatchserver.entity;

import com.curateme.clacobatchserver.global.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "concert")
public class Concert extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "concert_id")
    private String mt20id;

    @Column(name = "concert_name")
    private String prfnm;

    @Column(name = "start_date")
    private String prfpdfrom;

    @Column(name = "end_date")
    private String prfpdto;

    @Column(name = "facility_name")
    private String fcltynm;

    @Column(name = "poster")
    private String poster;

    @Column(name = "area")
    private String area;

    @Column(name = "genre")
    private String genrenm;

    @Column(name = "openrun")
    private String openrun;

    @Column(name = "status")
    private String prfstate;

    @Column(name = "cast")
    private String prfcast;

    @Column(name = "crew")
    private String prfcrew;

    @Column(name = "runtime")
    private String prfruntime;

    @Column(name = "age")
    private String prfage;

    @Column(name = "company_name")
    private String entrpsnm;

    @Column(name = "company_namep")
    private String entrpsnmP;

    @Column(name = "company_namea")
    private String entrpsnmA;

    @Column(name = "company_nameh")
    private String entrpsnmH;

    @Column(name = "company_names")
    private String entrpsnmS;

    @Column(name = "seat_guidance")
    private String pcseguidance;

    @Column(name = "visit")
    private String visit;

    @Column(name = "child")
    private String child;

    @Column(name = "daehakro")
    private String daehakro;

    @Column(name = "festival")
    private String festival;

    @Column(name = "musical_license")
    private String musicallicense;

    @Column(name = "musical_create")
    private String musicalcreate;

    @Column(name = "update_date")
    private String updatedate;

    @Column(name = "schedule_guidance", length = 1000)
    private String dtguidance;

    @Column(name = "introduction")
    private String styurl;

    @Column(name = "summary", length = 1500)
    private String summary;

    @ElementCollection
    @CollectionTable(name = "concert_category", joinColumns = @JoinColumn(name = "concert_id"))
    @MapKeyColumn(name = "category")
    @Column(name = "score")
    private Map<String, Double> categories;
    public void setConcertDetails(String mt20id, String prfnm, String prfpdfrom, String prfpdto,
        String fcltynm, String poster, String area, String genrenm,
        String openrun, String prfstate) {
        this.mt20id = mt20id;
        this.prfnm = prfnm;
        this.prfpdfrom = prfpdfrom;
        this.prfpdto = prfpdto;
        this.fcltynm = fcltynm;
        this.poster = poster;
        this.area = area;
        this.genrenm = genrenm;
        this.openrun = openrun;
        this.prfstate = prfstate;
    }

    public void setAdditionalConcertDetails(String prfcast, String prfcrew, String prfruntime, String prfage,
        String entrpsnm, String entrpsnmP, String entrpsnmA, String entrpsnmH,
        String entrpsnmS, String pcseguidance, String visit, String child,
        String daehakro, String festival, String musicallicense,
        String musicalcreate, String updatedate, String dtguidance) {
        this.prfcast = prfcast;
        this.prfcrew = prfcrew;
        this.prfruntime = prfruntime;
        this.prfage = prfage;
        this.entrpsnm = entrpsnm;
        this.entrpsnmP = entrpsnmP;
        this.entrpsnmA = entrpsnmA;
        this.entrpsnmH = entrpsnmH;
        this.entrpsnmS = entrpsnmS;
        this.pcseguidance = pcseguidance;
        this.visit = visit;
        this.child = child;
        this.daehakro = daehakro;
        this.festival = festival;
        this.musicallicense = musicallicense;
        this.musicalcreate = musicalcreate;
        this.updatedate = updatedate;
        this.dtguidance = dtguidance;
    }

    public void setStyurl(String styurl){
        this.styurl =styurl;
    }

    public void setCategories(Map<String, Double> categories) {
        this.categories = categories;
    }

    public void setSummary(String summary) {this.summary = summary; }
}
