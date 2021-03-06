package io.tackle.pathfinder.model.questionnaire;

import io.tackle.commons.entities.AbstractEntity;
import io.tackle.pathfinder.model.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import java.util.List;

@Entity
@Table(name = "question")
@SQLDelete(sql = "UPDATE question SET deleted = true WHERE id = ?", check = ResultCheckStyle.COUNT)
@Where(clause = "deleted is not true")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question extends AbstractEntity {
    @Column(name="question_order", nullable = false)
    public int order;

    @Basic(optional = false)
    @Enumerated(value = EnumType.STRING)
    public QuestionType type;

    @Basic(optional = false)
    public String name;

    @Column(length = 1000)
    public String description;

    @Column(name="question_text", length = 500, nullable = false)
    public String questionText;

    @ManyToOne
    @JoinColumn(name="category_id", nullable = false)
    public Category category;

    @OneToMany(mappedBy="question")
    public List<SingleOption> singleOptions;
}
