package io.tackle.pathfinder.model.assessment;

import io.tackle.commons.entities.AbstractEntity;
import io.tackle.pathfinder.model.questionnaire.Questionnaire;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assessment_questionnaire")
@SQLDelete(sql = "UPDATE assessment_questionnaire SET deleted = true WHERE id = ?", check = ResultCheckStyle.COUNT)
@Where(clause = "deleted is not true")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AssessmentQuestionnaire extends AbstractEntity {
    @Column(name="language_code", nullable = false)
    @Builder.Default
    public String languageCode = "EN";

    @Basic(optional = false)
    public String name;

    @OneToOne
    @JoinColumn(name = "assessment_id", referencedColumnName="id", nullable = false)
    public Assessment assessment;

    @OneToMany(mappedBy="questionnaire", cascade = CascadeType.ALL)
    @Builder.Default
    public List<AssessmentCategory> categories=new ArrayList<>();

    @ManyToOne
    @JoinColumn(name="questionnaire_id", referencedColumnName="id", nullable = false)
    public Questionnaire questionnaire;
}
