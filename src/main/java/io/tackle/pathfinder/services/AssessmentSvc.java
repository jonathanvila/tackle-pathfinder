package io.tackle.pathfinder.services;

import io.tackle.pathfinder.dto.AssessmentDto;
import io.tackle.pathfinder.dto.AssessmentHeaderDto;
import io.tackle.pathfinder.dto.AssessmentStatus;
import io.tackle.pathfinder.mapper.AssessmentMapper;
import io.tackle.pathfinder.model.assessment.*;
import io.tackle.pathfinder.model.questionnaire.Category;
import io.tackle.pathfinder.model.questionnaire.Question;
import io.tackle.pathfinder.model.questionnaire.Questionnaire;
import io.tackle.pathfinder.model.questionnaire.SingleOption;
import lombok.extern.java.Log;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

@ApplicationScoped
@Log
public class AssessmentSvc {
    @Inject
    AssessmentMapper mapper;

    public Optional<AssessmentHeaderDto> getAssessmentHeaderDtoByApplicationId(@NotNull Long applicationId) {
        List<Assessment> assessmentQuery = Assessment.list("application_id", applicationId);
        return assessmentQuery.stream().findFirst().map(e -> mapper.assessmentToAssessmentHeaderDto(e));
    }

    @Transactional
    public AssessmentHeaderDto createAssessment(@NotNull List<Long> applications) {
        long count = Assessment.count("applications", applications);
        log.log(Level.FINE,"Assessments for application_id [ " + applications + "] : " + count);
        if (count == 0) {
            Assessment assessment = new Assessment();
            assessment.applications.addAll(applications.stream().map(e -> AssessmentApplication.builder()
                                                                    .applicationId(e)
                                                                    .assessment(assessment)
                                                                    .build()).collect(Collectors.toList()));
            assessment.status = AssessmentStatus.STARTED;
            assessment.persistAndFlush();

            copyQuestionnaireIntoAssessment(assessment, defaultQuestionnaire());

            return mapper.assessmentToAssessmentHeaderDto(assessment);
        } else {
            throw new BadRequestException();
        }
    }

    @Transactional
    public Assessment copyQuestionnaireIntoAssessment(Assessment assessment, Questionnaire questionnaire) {

        AssessmentQuestionnaire assessQuestionnaire = AssessmentQuestionnaire.builder()
                .name(questionnaire.name)
                .questionnaire(questionnaire)
                .assessment(assessment)
                .languageCode(questionnaire.languageCode)
                .build();
        assessQuestionnaire.persist();

        assessment.assessmentQuestionnaire = assessQuestionnaire;

        for (Category category : questionnaire.categories) {
            AssessmentCategory assessmentCategory = AssessmentCategory.builder()
                    .name(category.name)
                    .order(category.order)
                    .questionnaire(assessment.assessmentQuestionnaire )
                    .build();
            assessmentCategory.persist();

            for (Question question : category.questions) {
                AssessmentQuestion assessmentQuestion = AssessmentQuestion.builder()
                        .category(assessmentCategory)
                        .name(question.name)
                        .order(question.order)
                        .questionText(question.questionText)
                        .type(question.type)
                        .description(question.description)
                        .build();

                assessmentQuestion.persist();

                for (SingleOption so : question.singleOptions) {
                    AssessmentSingleOption singleOption = AssessmentSingleOption.builder()
                        .option(so.option)
                        .order(so.order)
                        .question(assessmentQuestion)
                        .risk(so.risk)
                        .selected(false)
                        .build();

                    singleOption.persist();

                    assessmentQuestion.singleOptions.add(singleOption);
                }
                assessmentCategory.questions.add(assessmentQuestion);
            }
            assessQuestionnaire.categories.add(assessmentCategory);
        }

        return assessment;
    }

    private Questionnaire defaultQuestionnaire() {
        log.log(Level.FINE, "questionnaires : " + Questionnaire.count());
        return Questionnaire.<Questionnaire>streamAll().findFirst().orElseThrow();
    }

    public AssessmentDto getAssessmentDtoByAssessmentId(@NotNull Long assessmentId) {
        log.log(Level.FINE,"Requesting Assessment " + assessmentId);
        Assessment assessment = (Assessment) Assessment.findByIdOptional(assessmentId).orElseThrow(NotFoundException::new);

        return mapper.assessmentToAssessmentDto(assessment);
    }

    @Transactional
    public AssessmentHeaderDto updateAssessment(@NotNull Long assessmentId, @NotNull @Valid AssessmentDto assessmentDto) {
        Assessment assessment = (Assessment) Assessment.findByIdOptional(assessmentId).orElseThrow(NotFoundException::new);
        AssessmentQuestionnaire assessment_questionnaire = AssessmentQuestionnaire.find("assessment_id=?1", assessmentId).<AssessmentQuestionnaire>firstResultOptional().orElseThrow(BadRequestException::new);

        if (null != assessmentDto.getStatus()) {
            assessment.status = assessmentDto.getStatus();
        }

        if (null != assessmentDto.getStakeholderGroups()) {
            // Delete existing stakeholdergroups not included in current array
            assessment.stakeholdergroups.forEach(stakegroup -> {
                if (!assessmentDto.getStakeholderGroups().contains(stakegroup.stakeholdergroupId)) {
                    log.log(Level.FINE,"Deleted stakegroup : " + stakegroup.stakeholdergroupId);
                    stakegroup.delete();
                }
            });
            // Add not existing stakeholdergroups included in the current array
            assessmentDto.getStakeholderGroups().forEach(e -> {
                log.log(Level.FINE, "Considering Stakeholdergroup : " + e);
                if (assessment.stakeholdergroups.stream().noneMatch(o -> o.stakeholdergroupId == e)) {
                    log.log(Level.FINE,"Adding Stakeholdergroup : " + e);
                    AssessmentStakeholdergroup.builder()
                            .assessment(assessment)
                            .stakeholdergroupId(e)
                            .build().persist();
                }
            });
        }
        if (null != assessmentDto.getStakeholders()) {
            // Delete existing stakeholders not included in current array
            assessment.stakeholders.forEach(stake -> {
                if (!assessmentDto.getStakeholders().contains(stake.stakeholderId)) {
                    log.log(Level.FINE,"Deleted stake : " + stake.stakeholderId);
                    stake.delete();
                }
            });
            // Add not existing stakeholders included in the current array
            assessmentDto.getStakeholders().forEach(e -> {
                log.log(Level.FINE,"Considering Stakeholder : " + e);
                if (assessment.stakeholders.stream().noneMatch(o -> o.stakeholderId == e)) {
                    log.log(Level.FINE,"Adding Stakeholder : " + e);
                    AssessmentStakeholder.builder()
                        .assessment(assessment)
                        .stakeholderId(e)
                        .build().persist();
                }
            });
        }

        if (assessmentDto.getQuestionnaire() != null && assessmentDto.getQuestionnaire().getCategories() != null) {
            assessmentDto.getQuestionnaire().getCategories().forEach(categ -> {
                AssessmentCategory category = AssessmentCategory.find("assessment_questionnaire_id=?1 and id=?2", assessment_questionnaire.id, categ.getId()).<AssessmentCategory>firstResultOptional().orElseThrow(BadRequestException::new);
                if (categ.getComment() != null) {
                category.comment = categ.getComment();
                    log.info("Setting category comment : " + category.comment);
                }

                if (categ.getQuestions() != null) {
                    categ.getQuestions().forEach(que -> {
                        AssessmentQuestion question = AssessmentQuestion.find("assessment_category_id=?1 and id=?2", categ.getId(), que.getId()).<AssessmentQuestion>firstResultOptional().orElseThrow(BadRequestException::new);

                        if (que.getOptions() != null) {
                            que.getOptions().forEach(opt -> {
                                AssessmentSingleOption option = AssessmentSingleOption.find("assessment_question_id=?1 and id=?2", question.id, opt.getId()).<AssessmentSingleOption>firstResultOptional().orElseThrow(BadRequestException::new);
                                if (opt.getChecked() != null) {
                                option.selected = opt.getChecked();
                                    log.log(Level.FINE, "Setting option checked : " + option.selected);
                                }
                            });
                        }
                    });
                }
            });
        }

        return mapper.assessmentToAssessmentHeaderDto(assessment);
    }

    @Transactional
    public void deleteAssessment(@NotNull Long assessmentId) {
        Assessment assessment = (Assessment) Assessment.findByIdOptional(assessmentId).orElseThrow(NotFoundException::new);
        boolean deleted = Assessment.deleteById(assessment.id);
        log.log(Level.FINE, "Deleted assessment : " + assessmentId + " = " + deleted);
        if (!deleted) throw new BadRequestException();
    }

}
