package io.tackle.pathfinder.model.assessment;

import io.tackle.commons.entities.AbstractEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;

@Entity
@Table(name = "assessment_application")
@SQLDelete(sql = "UPDATE assessment_application SET deleted = true WHERE id = ?", check = ResultCheckStyle.COUNT)
@Where(clause = "deleted is not true")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentApplication extends AbstractEntity {
    public Long applicationId;

    @ManyToOne
    @JoinColumn(name="assessment_id", referencedColumnName="id")
    public Assessment assessment;
}
