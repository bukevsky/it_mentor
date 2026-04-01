package com.example.it.mentor.entity;

import com.example.it.mentor.entity.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Enum labels")
class EnumLabelTest {

    // ── EmploymentType ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("EmploymentType")
    class EmploymentTypeTest {

        @ParameterizedTest
        @EnumSource(EmploymentType.class)
        @DisplayName("каждая константа имеет непустой label")
        void everyConstant_hasNonBlankLabel(EmploymentType type) {
            assertThat(type.getLabel()).isNotBlank();
        }

        @Test
        @DisplayName("содержит ровно 6 значений согласно ТЗ")
        void containsExpectedConstants() {
            assertThat(EmploymentType.values())
                    .extracting(EmploymentType::name)
                    .containsExactly("PRACTICE", "INTERNSHIP", "PART_TIME", "FULL_TIME", "PROJECT", "OTHER");
        }

        @Test
        @DisplayName("labels соответствуют ТЗ")
        void labels_matchSpec() {
            assertThat(EmploymentType.PRACTICE.getLabel()).isEqualTo("Практика");
            assertThat(EmploymentType.INTERNSHIP.getLabel()).isEqualTo("Стажировка");
            assertThat(EmploymentType.PART_TIME.getLabel()).isEqualTo("Part-time");
            assertThat(EmploymentType.FULL_TIME.getLabel()).isEqualTo("Full-time");
            assertThat(EmploymentType.PROJECT.getLabel()).isEqualTo("Проектная");
            assertThat(EmploymentType.OTHER.getLabel()).isEqualTo("Другое");
        }
    }

    // ── SkillLevel ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("SkillLevel")
    class SkillLevelTest {

        @ParameterizedTest
        @EnumSource(SkillLevel.class)
        @DisplayName("каждая константа имеет непустой label")
        void everyConstant_hasNonBlankLabel(SkillLevel level) {
            assertThat(level.getLabel()).isNotBlank();
        }

        @Test
        @DisplayName("содержит ровно 3 значения согласно ТЗ (убраны JUNIOR/MIDDLE/SENIOR)")
        void containsExpectedConstants() {
            assertThat(SkillLevel.values())
                    .extracting(SkillLevel::name)
                    .containsExactly("BEGINNER", "INTERMEDIATE", "CONFIDENT");
        }

        @Test
        @DisplayName("labels соответствуют ТЗ")
        void labels_matchSpec() {
            assertThat(SkillLevel.BEGINNER.getLabel()).isEqualTo("Начальный");
            assertThat(SkillLevel.INTERMEDIATE.getLabel()).isEqualTo("Средний");
            assertThat(SkillLevel.CONFIDENT.getLabel()).isEqualTo("Уверенный");
        }
    }

    // ── WorkFormat ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("WorkFormat")
    class WorkFormatTest {

        @ParameterizedTest
        @EnumSource(WorkFormat.class)
        @DisplayName("каждая константа имеет непустой label")
        void everyConstant_hasNonBlankLabel(WorkFormat format) {
            assertThat(format.getLabel()).isNotBlank();
        }

        @Test
        @DisplayName("labels соответствуют ТЗ")
        void labels_matchSpec() {
            assertThat(WorkFormat.REMOTE.getLabel()).isEqualTo("Удалённо");
            assertThat(WorkFormat.OFFICE.getLabel()).isEqualTo("Офис");
            assertThat(WorkFormat.HYBRID.getLabel()).isEqualTo("Гибрид");
        }
    }

    // ── EducationForm ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("EducationForm")
    class EducationFormTest {

        @ParameterizedTest
        @EnumSource(EducationForm.class)
        @DisplayName("каждая константа имеет непустой label")
        void everyConstant_hasNonBlankLabel(EducationForm form) {
            assertThat(form.getLabel()).isNotBlank();
        }

        @Test
        @DisplayName("labels соответствуют ТЗ")
        void labels_matchSpec() {
            assertThat(EducationForm.FULL_TIME.getLabel()).isEqualTo("Очно");
            assertThat(EducationForm.PART_TIME.getLabel()).isEqualTo("Очно-заочно");
            assertThat(EducationForm.DISTANCE.getLabel()).isEqualTo("Заочно");
        }
    }

    // ── RecruitmentStatus ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("RecruitmentStatus")
    class RecruitmentStatusTest {

        @ParameterizedTest
        @EnumSource(RecruitmentStatus.class)
        @DisplayName("каждая константа имеет непустой label")
        void everyConstant_hasNonBlankLabel(RecruitmentStatus status) {
            assertThat(status.getLabel()).isNotBlank();
        }

        @Test
        @DisplayName("labels соответствуют ТЗ")
        void labels_matchSpec() {
            assertThat(RecruitmentStatus.OPEN.getLabel()).isEqualTo("Набираю");
            assertThat(RecruitmentStatus.PAUSED.getLabel()).isEqualTo("Пауза");
            assertThat(RecruitmentStatus.CLOSED.getLabel()).isEqualTo("Нет мест");
        }
    }

    // ── LanguageLevel ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("LanguageLevel")
    class LanguageLevelTest {

        @ParameterizedTest
        @EnumSource(LanguageLevel.class)
        @DisplayName("каждая константа имеет непустой label")
        void everyConstant_hasNonBlankLabel(LanguageLevel level) {
            assertThat(level.getLabel()).isNotBlank();
        }

        @Test
        @DisplayName("NATIVE имеет label 'Родной', остальные — CEFR-коды")
        void labels_matchSpec() {
            assertThat(LanguageLevel.A1.getLabel()).isEqualTo("A1");
            assertThat(LanguageLevel.C2.getLabel()).isEqualTo("C2");
            assertThat(LanguageLevel.NATIVE.getLabel()).isEqualTo("Родной");
        }
    }

    // ── FileType ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("FileType")
    class FileTypeTest {

        @ParameterizedTest
        @EnumSource(FileType.class)
        @DisplayName("каждая константа имеет непустой label")
        void everyConstant_hasNonBlankLabel(FileType type) {
            assertThat(type.getLabel()).isNotBlank();
        }

        @Test
        @DisplayName("labels соответствуют ТЗ")
        void labels_matchSpec() {
            assertThat(FileType.RESUME.getLabel()).isEqualTo("Резюме");
            assertThat(FileType.PORTFOLIO.getLabel()).isEqualTo("Портфолио");
            assertThat(FileType.ATTACHMENT.getLabel()).isEqualTo("Вложение");
        }
    }

    // ── EducationDegree ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("EducationDegree")
    class EducationDegreeTest {

        @ParameterizedTest
        @EnumSource(EducationDegree.class)
        @DisplayName("каждая константа имеет непустой label")
        void everyConstant_hasNonBlankLabel(EducationDegree degree) {
            assertThat(degree.getLabel()).isNotBlank();
        }

        @Test
        @DisplayName("содержит ровно 5 значений согласно ТЗ")
        void containsExpectedConstants() {
            assertThat(EducationDegree.values())
                    .extracting(EducationDegree::name)
                    .containsExactly("BACHELOR", "SPECIALIST", "MASTER", "COURSE", "OTHER");
        }

        @Test
        @DisplayName("labels соответствуют ТЗ")
        void labels_matchSpec() {
            assertThat(EducationDegree.BACHELOR.getLabel()).isEqualTo("Бакалавр");
            assertThat(EducationDegree.MASTER.getLabel()).isEqualTo("Магистр");
            assertThat(EducationDegree.COURSE.getLabel()).isEqualTo("Курсы");
        }
    }

    // ── MentoringType ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("MentoringType")
    class MentoringTypeTest {

        @ParameterizedTest
        @EnumSource(MentoringType.class)
        @DisplayName("каждая константа имеет непустой label")
        void everyConstant_hasNonBlankLabel(MentoringType type) {
            assertThat(type.getLabel()).isNotBlank();
        }

        @Test
        @DisplayName("labels соответствуют ТЗ")
        void labels_matchSpec() {
            assertThat(MentoringType.PRACTICE.getLabel()).isEqualTo("Практика");
            assertThat(MentoringType.INTERNSHIP.getLabel()).isEqualTo("Стажировка");
            assertThat(MentoringType.PROJECT.getLabel()).isEqualTo("Проектное сопровождение");
        }
    }

    // ── MentoringChannel ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("MentoringChannel")
    class MentoringChannelTest {

        @ParameterizedTest
        @EnumSource(MentoringChannel.class)
        @DisplayName("каждая константа имеет непустой label")
        void everyConstant_hasNonBlankLabel(MentoringChannel channel) {
            assertThat(channel.getLabel()).isNotBlank();
        }

        @Test
        @DisplayName("labels соответствуют ТЗ")
        void labels_matchSpec() {
            assertThat(MentoringChannel.CHAT.getLabel()).isEqualTo("Чат");
            assertThat(MentoringChannel.CALLS.getLabel()).isEqualTo("Созвоны");
            assertThat(MentoringChannel.MIXED.getLabel()).isEqualTo("Смешанный");
        }
    }

    // ── MentoringDuration ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("MentoringDuration")
    class MentoringDurationTest {

        @ParameterizedTest
        @EnumSource(MentoringDuration.class)
        @DisplayName("каждая константа имеет непустой label")
        void everyConstant_hasNonBlankLabel(MentoringDuration duration) {
            assertThat(duration.getLabel()).isNotBlank();
        }

        @Test
        @DisplayName("labels соответствуют ТЗ")
        void labels_matchSpec() {
            assertThat(MentoringDuration.ONE_MONTH.getLabel()).isEqualTo("1 месяц");
            assertThat(MentoringDuration.THREE_MONTHS.getLabel()).isEqualTo("3 месяца");
            assertThat(MentoringDuration.FLEXIBLE.getLabel()).isEqualTo("Гибко");
        }
    }
}
