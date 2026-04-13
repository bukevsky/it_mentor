package com.example.it.mentor.repository;

import com.example.it.mentor.dto.mentor.MentorSearchFilter;
import com.example.it.mentor.entity.MentorProfile;
import com.example.it.mentor.entity.MentorSkill;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Утилита построения {@link Specification} для поиска профилей менторов.
 */
public final class MentorProfileSpecification {

    /**
     * Закрытый конструктор utility-класса.
     */
    private MentorProfileSpecification() {}

    /**
     * Строит спецификацию по набору параметров поиска менторов.
     *
     * @param filter фильтр поиска
     * @return спецификация для JPA Criteria API
     */
    public static Specification<MentorProfile> build(MentorSearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.q() != null && !filter.q().isBlank()) {
                String escaped = escapeLike(filter.q().toLowerCase());
                String pattern = "%" + escaped + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern)
                ));
            }

            if (filter.skillIds() != null && !filter.skillIds().isEmpty()) {
                List<Long> skillIds = filter.skillIds();
                Subquery<Long> sub = query.subquery(Long.class);
                Root<MentorSkill> skillRoot = sub.from(MentorSkill.class);
                sub.select(cb.count(skillRoot.get("skill").get("id")))
                        .where(
                                cb.equal(skillRoot.get("mentorProfile").get("id"), root.get("id")),
                                skillRoot.get("skill").get("id").in(skillIds)
                        );
                predicates.add(cb.greaterThanOrEqualTo(sub, (long) skillIds.size()));
            }

            if (filter.cityId() != null) {
                predicates.add(cb.equal(root.get("city").get("id"), filter.cityId()));
            }

            if (filter.recruitmentStatus() != null) {
                predicates.add(cb.equal(root.get("recruitmentStatus"), filter.recruitmentStatus()));
            }

            if (filter.mentoringType() != null) {
                predicates.add(cb.equal(root.get("mentoringType"), filter.mentoringType()));
            }

            if (filter.mentoringChannel() != null) {
                predicates.add(cb.equal(root.get("mentoringChannel"), filter.mentoringChannel()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Экранирует спецсимволы LIKE (%,_,\) в пользовательском вводе.
     *
     * @param input пользовательская строка
     * @return безопасная строка для LIKE-условия
     */
    private static String escapeLike(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
